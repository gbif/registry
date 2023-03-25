package org.gbif.registry.service.collections.batch;

import com.google.common.base.Preconditions;

import org.gbif.api.model.collections.Batch;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.registry.persistence.mapper.BatchMapper;
import org.gbif.registry.service.collections.batch.FileFields.ContactFields;
import org.gbif.registry.service.collections.batch.FileParser.ParsingData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.SneakyThrows;

import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.CODE;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.ERRORS;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.KEY;
import static org.gbif.registry.service.collections.batch.FileParser.parseContacts;
import static org.gbif.registry.service.collections.batch.FileParser.parseEntities;

public abstract class BaseBatchHandler<T extends CollectionEntity> {

  private final BatchMapper batchMapper;
  private final CollectionEntityService<T> entityService;
  private final CollectionEntityType entityType;
  private final Class<T> clazz;

  BaseBatchHandler(
      BatchMapper batchMapper,
      CollectionEntityService<T> entityService,
      CollectionEntityType entityType,
      Class<T> clazz) {
    this.batchMapper = batchMapper;
    this.entityService = entityService;
    this.entityType = entityType;
    this.clazz = clazz;
  }

  public int handleBatchAsync(
    Path entitiesPath, Path contactsPath, ExportFormat format, boolean update) {
    Objects.requireNonNull(entitiesPath);
    Objects.requireNonNull(contactsPath);
    Objects.requireNonNull(format);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Preconditions.checkArgument(
      authentication != null && authentication.getName() != null, "Authentication is required");

    // create entry in DB
    Batch batch = new Batch();
    batch.setCreatedBy(authentication.getName());
    batch.setEntityType(entityType);

    // async import
    if (update) {
      batch.setOperation(Batch.Operation.UPDATE);
      CompletableFuture.runAsync(() -> updateBatch(entitiesPath, contactsPath, format, batch));
    } else {
      batch.setOperation(Batch.Operation.CREATE);
      CompletableFuture.runAsync(() -> importBatch(entitiesPath, contactsPath, format, batch));
    }

    batchMapper.create(batch);

    return batch.getKey();
  }

  @SneakyThrows
  void importBatch(Path entitiesPath, Path contactsPath, ExportFormat format, Batch batch) {

    FileParser.ParsingResult<T> parsingResult =
        parseEntities(
            entitiesPath,
            format,
            this::createEntityFromValues,
            CollectionEntity::getCode,
            entityType);

    if (!parsingResult.duplicates.isEmpty()) {
      batch.setSuccessful(false);
      batch
          .getFileErrors()
          .add(
              "Duplicate "
                  + entityType.name().toLowerCase()
                  + " codes: "
                  + String.join(",", parsingResult.duplicates));
      batchMapper.update(batch);
      return;
    }

    FileParser.ParsingContactsResult<Contact> contactsParsed =
        parseContacts(
            contactsPath,
            parsingResult.parsingData,
            format,
            ContactFields.getEntityCode(entityType));

    for (ParsingData<T> parsedEntity : parsingResult.parsingData.values()) {
      T entity = parsedEntity.entity;

      List<UUID> existingEntities = findEntity(entity.getCode(), entity.getIdentifiers());
      if (!existingEntities.isEmpty()) {
        parsedEntity.errors.add(
            "Other "
                + entityType.name().toUpperCase()
                + "s already exist with the same code or id: "
                + existingEntities.stream().map(UUID::toString).collect(Collectors.joining()));
        continue;
      }

      // create entity
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      String userName = authentication.getName();
      entity.setCreatedBy(userName);

      UUID key = null;
      try {
        key = entityService.create(entity);
        entity.setKey(key);
      } catch (Exception ex) {
        parsedEntity.errors.add(
            "Couldn't create " + entityType.name().toLowerCase() + ": " + ex.getMessage());
      }

      if (key == null) {
        continue;
      }

      // add identifiers
      entity.getIdentifiers().forEach(i -> addIdentifier(entity.getKey(), i, parsedEntity.errors));

      // add contacts
      for (ParsingData<Contact> parsedContact :
          contactsParsed.contactsByEntity.get(entity.getCode())) {
        try {
          entityService.addContactPerson(key, parsedContact.entity);
        } catch (Exception ex) {
          parsedContact.errors.add("Couldn't add contact: " + ex.getMessage());
        }
      }
    }

    // csv
    Path resultPath =
        createResultFile(entitiesPath, contactsPath, format, parsingResult, contactsParsed, CODE);

    // update batch
    batch.setSuccessful(true);
    batch.getFileErrors().addAll(parsingResult.fileErrors);
    batch.getFileErrors().addAll(contactsParsed.fileErrors);

    if (!contactsParsed.duplicates.isEmpty()) {
      batch
          .getFileErrors()
          .add(
              "Duplicate contact keys: "
                  + contactsParsed.duplicates.stream()
                      .map(Object::toString)
                      .collect(Collectors.joining(",")));
    }

    batch.setResultFilePath(resultPath.toFile().getAbsolutePath());
    batchMapper.update(batch);
  }

  @SneakyThrows
  void updateBatch(Path entitiesPath, Path contactsPath, ExportFormat format, Batch batch) {
    FileParser.ParsingResult<T> parsedEntities =
        parseEntities(
            entitiesPath,
            format,
            this::createEntityFromValues,
            i -> i.getKey().toString(),
            CollectionEntityType.COLLECTION);

    if (!parsedEntities.duplicates.isEmpty()) {
      batch.setSuccessful(false);
      batch
          .getFileErrors()
          .add(
              "Duplicate "
                  + entityType.name().toLowerCase()
                  + " keys: "
                  + String.join(",", parsedEntities.duplicates));
      batchMapper.update(batch);
      return;
    }

    FileParser.ParsingContactsResult<Contact> contactsParsed =
        parseContacts(
            contactsPath,
            parsedEntities.parsingData,
            format,
            ContactFields.getEntityKey(entityType));

    for (ParsingData<T> parsedEntity : parsedEntities.parsingData.values()) {
      T entity = parsedEntity.entity;
      if (!entityService.exists(entity.getKey()) || entity.getKey() == null) {
        parsedEntity.errors.add(entityType.name().toLowerCase() + " doesn't exist");
        continue;
      }

      // update entity
      T mergedEntity =
          mergeEntities(
              entityService.get(entity.getKey()), entity, parsedEntities.fileHeadersIndex);

      try {
        entityService.update(mergedEntity);
      } catch (Exception ex) {
        parsedEntity.errors.add(
            "Couldn't update " + entityType.name().toLowerCase() + ": " + ex.getMessage());
      }

      List<Identifier> existingIdentifiers = entityService.listIdentifiers(entity.getKey());

      // delete identifiers
      for (Identifier existing : existingIdentifiers) {
        if (!containsIdentifier(entity.getIdentifiers(), existing)) {
          try {
            entityService.deleteIdentifier(entity.getKey(), existing.getKey());
          } catch (Exception ex) {
            parsedEntity.errors.add("Couldn't delete identifier: " + ex.getMessage());
          }
        }
      }

      // create new identifiers
      entity.getIdentifiers().stream()
          .filter(i -> !containsIdentifier(existingIdentifiers, i))
          .forEach(i -> addIdentifier(entity.getKey(), i, parsedEntity.errors));

      // delete contacts
      List<Contact> existingContacts = entityService.listContactPersons(entity.getKey());
      java.util.Collection<ParsingData<Contact>> parsingDataContacts =
          contactsParsed.contactsByKey.values();
      for (Contact existing : existingContacts) {
        if (!containsContact(parsingDataContacts, existing)) {
          try {
            entityService.removeContactPerson(entity.getKey(), existing.getKey());
          } catch (Exception ex) {
            parsedEntity.errors.add("Couldn't remove contact: " + ex.getMessage());
          }
        }
      }

      // update contacts
      for (ParsingData<Contact> contact : parsingDataContacts) {
        if (contact.entity.getKey() == null) {
          // create new contact
          try {
            entityService.addContactPerson(entity.getKey(), contact.entity);
          } catch (Exception ex) {
            contact.errors.add("Couldn't add contact: " + ex.getMessage());
          }
        } else {
          try {
            entityService.updateContactPerson(entity.getKey(), contact.entity);
          } catch (Exception ex) {
            contact.errors.add("Couldn't update contact: " + ex.getMessage());
          }
        }
      }
    }

    // csv
    Path resultPath =
        createResultFile(entitiesPath, contactsPath, format, parsedEntities, contactsParsed, CODE);

    // update batch
    batch.setSuccessful(true);
    batch.getFileErrors().addAll(parsedEntities.fileErrors);
    batch.getFileErrors().addAll(contactsParsed.fileErrors);
    batch.setResultFilePath(resultPath.toFile().getAbsolutePath());
    batchMapper.update(batch);
  }

  @SneakyThrows
  private T mergeEntities(T existing, T parsed, Map<String, Integer> headersIndex) {
    for (String h : getEntityFields()) {
      if (headersIndex.containsKey(h)) {
        Optional<Field> field =
            Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.getName().toUpperCase().equals(h))
                .findFirst();

        if (!field.isPresent()) {
          continue;
        }

        String methodName =
            field.get().getName().substring(0, 1).toUpperCase()
                + field.get().getName().substring(1);

        String getterPrefix = field.get().getType().isAssignableFrom(Boolean.TYPE) ? "is" : "get";
        Method getter = clazz.getDeclaredMethod(getterPrefix + methodName);
        Method setter = clazz.getDeclaredMethod("set" + methodName, field.get().getType());

        setter.invoke(existing, getter.invoke(parsed));
      }
    }

    // set user
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userName = authentication.getName();
    existing.setModifiedBy(userName);

    return existing;
  }

  private void addIdentifier(UUID entityKey, Identifier identifier, List<String> errors) {
    try {
      entityService.addIdentifier(entityKey, identifier);
    } catch (Exception ex) {
      errors.add("Couldn't add identifier: " + ex.getMessage());
    }
  }

  private boolean containsIdentifier(List<Identifier> identifierList, Identifier identifier) {
    return identifierList.stream()
        .anyMatch(
            i ->
                i.getIdentifier().equals(identifier.getIdentifier())
                    && i.getType() == identifier.getType());
  }

  private boolean containsContact(
      java.util.Collection<ParsingData<Contact>> contactList, Contact contact) {
    return contactList.stream()
        .map(d -> d.entity)
        .anyMatch(c -> c.getKey() != null && c.getKey().equals(contact.getKey()));
  }

  @SneakyThrows
  private Path createResultFile(
      Path institutionsPath,
      Path contactsPath,
      ExportFormat format,
      FileParser.ParsingResult<T> parsingResult,
      FileParser.ParsingContactsResult<Contact> contactsParsed,
      String keyColumn) {
    // TODO: path
    // TODO: temp file?
    Path resultFile = Files.createFile(Paths.get("result." + format.name().toLowerCase()));

    // institutions
    try (BufferedReader br = new BufferedReader(new FileReader(institutionsPath.toFile()));
        BufferedWriter bw = new BufferedWriter(new FileWriter(resultFile.toFile()))) {

      // write headers in result file.
      List<String> headersResult = new ArrayList<>();
      headersResult.addAll(FileFields.InstitutionFields.ALL_FIELDS);
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
        ParsingData<T> parsingData = parsingResult.parsingData.get(keyValue);

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

    // contacts
    // TODO: temp file?
    Path contactsResultFile =
        Files.createFile(Paths.get("contacts." + format.name().toLowerCase()));
    try (BufferedReader br = new BufferedReader(new FileReader(contactsPath.toFile()));
        BufferedWriter bw = new BufferedWriter(new FileWriter(resultFile.toFile()))) {

      // write headers in result file.
      String headersLine =
          ContactFields.ALL_FIELDS.stream()
              .collect(Collectors.joining(format.getDelimiter().toString()));
      headersLine += format.getDelimiter() + ERRORS;
      bw.write(headersLine);
      bw.newLine();

      // skip header line
      br.readLine();

      String line;
      while ((line = br.readLine()) != null) {
        String[] values = line.split(format.getDelimiter().toString());

        int uniqueKey = FileParser.getContactUniqueKey(values, parsingResult.fileHeadersIndex);
        ParsingData<Contact> parsingData = contactsParsed.contactsByKey.get(uniqueKey);

        StringBuilder resultLine = new StringBuilder();
        for (String header : ContactFields.ALL_FIELDS) {
          if (header.equals(ContactFields.KEY)
              && !parsingResult.fileHeadersIndex.containsKey(ContactFields.KEY)) {
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

    // zip both files
    Path zipFile = Files.createFile(Paths.get("contacts." + format.name().toLowerCase()));
    toZipFile(Arrays.asList(resultFile, contactsResultFile), zipFile);

    return zipFile;
  }

  @SneakyThrows
  private static void toZipFile(List<Path> filesToZip, Path targetFile) {
    try (FileOutputStream fos = new FileOutputStream(targetFile.toFile().getName());
        ZipOutputStream zipOut = new ZipOutputStream(fos)) {
      for (Path file : filesToZip) {
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
          ZipEntry zipEntry = new ZipEntry(file.toFile().getName());
          zipOut.putNextEntry(zipEntry);
          byte[] bytes = new byte[1024];
          int length;
          while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
          }
        }
      }
    }
  }

  abstract List<String> getEntityFields();

  abstract ParsingData<T> createEntityFromValues(
      String[] values, Map<String, Integer> headersIndex);

  abstract List<UUID> findEntity(String code, List<Identifier> identifiers);
}
