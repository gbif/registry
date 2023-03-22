package org.gbif.registry.service.collections.batch;

import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.service.collections.batch.FileFields.InstitutionFields;
import org.gbif.registry.service.collections.batch.FileParser.ParsingData;
import org.gbif.registry.service.collections.batch.FileParser.ParsingResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Strings;

import lombok.SneakyThrows;

import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.CODE;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.ERRORS;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.KEY;
import static org.gbif.registry.service.collections.batch.FileFields.ContactFields;
import static org.gbif.registry.service.collections.batch.FileFields.ContactFields.INSTITUTION_CODE;
import static org.gbif.registry.service.collections.batch.FileParser.parseContacts;
import static org.gbif.registry.service.collections.batch.FileParser.parseInstitutions;

// TODO: interface in gbif-api
public class DefaultBatchHandler {

  private final InstitutionService institutionService;

  @Autowired
  public DefaultBatchHandler(InstitutionService institutionService) {
    this.institutionService = institutionService;
  }

  public long handleBatch(
      Path institutionsPath, Path contactsPath, ExportFormat format, boolean update) {

    // TODO: create entry in DB

    // async import
    if (update) {

    } else {

    }

    // generate csv and save in DB

    // TODO:
    return 0;
  }

  @SneakyThrows
  void importInstitutionsBatch(Path institutionsPath, Path contactsPath, ExportFormat format) {

    ParsingResult<Institution> institutionsParsingResult =
        parseInstitutions(institutionsPath, format, Institution::getCode);

    List<String> duplicateCodes =
        institutionsParsingResult.parsingData.values().stream()
            .filter(institutions -> institutions.size() > 1)
            .map(
                institutions ->
                    institutions.stream()
                        .map(v -> v.entity.getCode())
                        .collect(Collectors.joining()))
            .collect(Collectors.toList());
    if (!duplicateCodes.isEmpty()) {
      // TODO: Error and stop the process
    }

    ParsingResult<Contact> contactsParsed =
        parseContacts(
            contactsPath, institutionsParsingResult.parsingData, format, INSTITUTION_CODE);

    for (ParsingData<Institution> parsedInstitution :
        institutionsParsingResult.parsingData.values().stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList())) {
      Institution institution = parsedInstitution.entity;
      if (existsInstitution(institution.getCode(), institution.getIdentifiers())) {
        parsedInstitution.errors.add("Institution already exists");
        continue;
      }

      // create institution
      UUID key = null;
      try {
        key = institutionService.create(institution);
        institution.setKey(key);
      } catch (Exception ex) {
        parsedInstitution.errors.add("Couldn't create institution: " + ex.getMessage());
      }

      if (key == null) {
        continue;
      }

      // add identifiers
      for (Identifier identifier : institution.getIdentifiers()) {
        try {
          institutionService.addIdentifier(key, identifier);
        } catch (Exception ex) {
          parsedInstitution.errors.add("Couldn't add identifier: " + ex.getMessage());
        }
      }

      // add contacts
      for (ParsingData<Contact> parsedContact :
          contactsParsed.parsingData.get(institution.getCode())) {
        try {
          institutionService.addContactPerson(key, parsedContact.entity);
        } catch (Exception ex) {
          parsedContact.errors.add("Couldn't add contact: " + ex.getMessage());
        }
      }
    }

    // TODO: return errors and key for each institution to edit csv?? ver como hago para editar el
    // csv
    Path resultPath =
        createResultFile(institutionsPath, contactsPath, format, institutionsParsingResult, CODE);
  }

  @SneakyThrows
  void updateInstitutionsBatch(Path institutionsPath, Path contactsPath, ExportFormat format) {
    ParsingResult<Institution> parsedInstitutions =
        parseInstitutions(institutionsPath, format, i -> i.getKey().toString());

    List<String> duplicateKeys =
        parsedInstitutions.parsingData.values().stream()
            .filter(institutions -> institutions.size() > 1)
            .map(
                institutions ->
                    institutions.stream()
                        .map(i -> i.entity.getKey().toString())
                        .collect(Collectors.joining()))
            .collect(Collectors.toList());
    if (!duplicateKeys.isEmpty()) {
      // TODO: Error and stop the process
    }

    ParsingResult<Contact> contactsParsed =
        parseContacts(
            contactsPath, parsedInstitutions.parsingData, format, ContactFields.INSTITUTION_KEY);

    for (ParsingData<Institution> parsedInstitution :
        parsedInstitutions.parsingData.values().stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList())) {

      Institution institution = parsedInstitution.entity;
      if (!institutionService.exists(institution.getKey()) || institution.getKey() == null) {
        parsedInstitution.errors.add("Institution doesn't exist");
        continue;
      }

      // update institution
      try {
        institutionService.update(institution);
      } catch (Exception ex) {
        parsedInstitution.errors.add("Couldn't update institution: " + ex.getMessage());
      }

      List<Identifier> existingIdentifiers =
          institutionService.listIdentifiers(institution.getKey());

      // delete identifiers
      for (Identifier existing : existingIdentifiers) {
        if (!containsIdentifier(institution.getIdentifiers(), existing)) {
          try {
            institutionService.deleteIdentifier(institution.getKey(), existing.getKey());
          } catch (Exception ex) {
            parsedInstitution.errors.add("Couldn't delete identifier: " + ex.getMessage());
          }
        }
      }

      // create new identifiers
      for (Identifier identifier : institution.getIdentifiers()) {
        if (!containsIdentifier(existingIdentifiers, identifier)) {
          try {
            institutionService.addIdentifier(institution.getKey(), identifier);
          } catch (Exception ex) {
            parsedInstitution.errors.add("Couldn't add identifier: " + ex.getMessage());
          }
        }
      }

      // delete contacts
      List<Contact> existingContacts = institutionService.listContactPersons(institution.getKey());
      List<ParsingData<Contact>> parsingDataContacts =
          contactsParsed.parsingData.values().stream()
              .flatMap(Collection::stream)
              .collect(Collectors.toList());
      for (Contact existing : existingContacts) {
        if (!containsContact(parsingDataContacts, existing)) {
          try {
            institutionService.removeContactPerson(institution.getKey(), existing.getKey());
          } catch (Exception ex) {
            parsedInstitution.errors.add("Couldn't remove contact: " + ex.getMessage());
          }
        }
      }

      // update contacts
      for (ParsingData<Contact> contact : parsingDataContacts) {
        if (contact.entity.getKey() == null) {
          // create new contact
          try {
            institutionService.addContactPerson(institution.getKey(), contact.entity);
          } catch (Exception ex) {
            contact.errors.add("Couldn't add contact: " + ex.getMessage());
          }
        } else {
          try {
            institutionService.updateContactPerson(institution.getKey(), contact.entity);
          } catch (Exception ex) {
            contact.errors.add("Couldn't update contact: " + ex.getMessage());
          }
        }
      }
    }

    // TODO: return errors and key for each institution to edit csv?? ver como hago para editar el
    // csv
  }

  @SneakyThrows
  private Path createResultFile(
      Path institutionsPath,
      Path contactsPath,
      ExportFormat format,
      ParsingResult<Institution> parsingResult,
      String keyColumn) {
    // TODO: path
    Path resultFile = Files.createFile(Paths.get("result.csv"));

    try (BufferedReader br = new BufferedReader(new FileReader(institutionsPath.toFile()));
        BufferedWriter bw = new BufferedWriter(new FileWriter(resultFile.toFile()))) {

      // write headers in result file.
      List<String> headersResult = new ArrayList<>();
      headersResult.addAll(InstitutionFields.ALL_FIELDS);
      headersResult.addAll(FileFields.CommonFields.ALL_FIELDS);

      String headersLine =
          headersResult.stream().collect(Collectors.joining(format.getDelimiter().toString()));
      headersLine += format.getDelimiter() + ERRORS;
      bw.write(headersLine);
      bw.newLine();

      // skip header line
      br.readLine();

      String line;
      while ((line = br.readLine()) != null) {
        String[] values = line.split(format.getDelimiter().toString());

        String keyValue = values[parsingResult.fileHeadersIndex.get(keyColumn)];
        ParsingData<Institution> parsingData = parsingResult.parsingData.get(keyValue).get(0);

        StringBuilder resultLine = new StringBuilder();
        for (String header : headersResult) {
          if (header.equals(KEY) && !parsingResult.fileHeadersIndex.containsKey(KEY)) {
            // case of initial imports
            resultLine.append(parsingData.entity.getKey());
          } else {
            resultLine.append(values[parsingResult.fileHeadersIndex.get(header)]);
          }
          resultLine.append(format.getDelimiter().toString());
        }

        // add errors
        resultLine.append(String.join(FileParsingUtils.LIST_DELIMITER, parsingData.errors));

        bw.write(resultLine.toString());
        bw.newLine();
      }
    }

    return resultFile;
  }

  private boolean containsIdentifier(List<Identifier> identifierList, Identifier identifier) {
    return identifierList.stream()
        .anyMatch(
            i ->
                i.getIdentifier().equals(identifier.getIdentifier())
                    && i.getType() == identifier.getType());
  }

  private boolean containsContact(List<ParsingData<Contact>> contactList, Contact contact) {
    return contactList.stream()
        .map(d -> d.entity)
        .anyMatch(c -> c.getKey() != null && c.getKey().equals(contact.getKey()));
  }

  private boolean existsInstitution(String code, List<Identifier> identifiers) {
    List<Institution> institutionsFound = new ArrayList<>();
    if (!Strings.isNullOrEmpty(code)) {
      institutionsFound =
          institutionService
              .list(InstitutionSearchRequest.builder().code(code).build())
              .getResults();

      if (institutionsFound.isEmpty()) {
        institutionsFound =
            institutionService
                .list(InstitutionSearchRequest.builder().alternativeCode(code).build())
                .getResults();
      }
    }

    if (institutionsFound.isEmpty() && identifiers != null && !identifiers.isEmpty()) {
      int i = 0;
      while (i < identifiers.size() && institutionsFound.isEmpty()) {
        Identifier identifier = identifiers.get(i);
        institutionsFound =
            institutionService
                .list(
                    InstitutionSearchRequest.builder()
                        .identifier(identifier.getIdentifier())
                        .identifierType(identifier.getType())
                        .build())
                .getResults();

        i++;
      }
    }

    if (institutionsFound.size() > 0) {
      // TODO: add error showing the existing institutions
      return true;
    }

    return false;
  }
}
