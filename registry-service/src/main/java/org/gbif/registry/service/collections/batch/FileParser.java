package org.gbif.registry.service.collections.batch;

import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.vocabulary.collections.Discipline;
import org.gbif.api.vocabulary.collections.InstitutionGovernance;
import org.gbif.api.vocabulary.collections.InstitutionType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.base.Strings;

import lombok.Builder;
import lombok.SneakyThrows;

import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.ACTIVE;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.ADDRESS;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.ALT_CODES;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.API_URL;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.CATALOG_URL;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.CITY;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.CODE;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.COUNTRY;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.DESCRIPTION;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.EMAIL;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.HOMEPAGE;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.IDENTIFIERS;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.KEY;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.MAIL_ADDRESS;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.MAIL_CITY;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.MAIL_COUNTRY;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.MAIL_POSTAL_CODE;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.MAIL_PROVINCE;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.NAME;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.NUMBER_SPECIMENS;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.PHONE;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.POSTAL_CODE;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.PROVINCE;
import static org.gbif.registry.service.collections.batch.FileFields.ContactFields.FAX;
import static org.gbif.registry.service.collections.batch.FileFields.ContactFields.FIRST_NAME;
import static org.gbif.registry.service.collections.batch.FileFields.ContactFields.INSTITUTION_CODE;
import static org.gbif.registry.service.collections.batch.FileFields.ContactFields.LAST_NAME;
import static org.gbif.registry.service.collections.batch.FileFields.ContactFields.POSITION;
import static org.gbif.registry.service.collections.batch.FileFields.InstitutionFields.DISCIPLINES;
import static org.gbif.registry.service.collections.batch.FileFields.InstitutionFields.FOUNDING_DATE;
import static org.gbif.registry.service.collections.batch.FileFields.InstitutionFields.GEOGRAPHIC_DESCRIPTION;
import static org.gbif.registry.service.collections.batch.FileFields.InstitutionFields.INSTITUTIONAL_GOVERNANCE;
import static org.gbif.registry.service.collections.batch.FileFields.InstitutionFields.LATITUDE;
import static org.gbif.registry.service.collections.batch.FileFields.InstitutionFields.LOGO_URL;
import static org.gbif.registry.service.collections.batch.FileFields.InstitutionFields.LONGITUDE;
import static org.gbif.registry.service.collections.batch.FileFields.InstitutionFields.TAXONOMIC_DESCRIPTION;
import static org.gbif.registry.service.collections.batch.FileFields.isContactField;
import static org.gbif.registry.service.collections.batch.FileFields.isInstitutionField;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseAddress;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseAlternativeCodes;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseBigDecimal;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseBoolean;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseCountry;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseEnum;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseIdentifiers;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseInteger;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseListValues;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseStringList;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseUUID;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseUri;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseUserIds;

public class FileParser {

  @SneakyThrows
  static ParsingResult<Institution> parseInstitutions(
      Path institutionsPath, ExportFormat format, Function<Institution, String> keyExtractor) {

    Map<String, List<ParsingData<Institution>>> institutionsDataMap = new HashMap<>();
    List<String> fileErrors = new ArrayList<>();
    Map<String, Integer> headersIndex = new HashMap<>();
    try (BufferedReader br = new BufferedReader(new FileReader(institutionsPath.toFile()))) {
      // extract headers
      String[] headers = br.readLine().split(format.getDelimiter().toString());
      for (int i = 0; i < headers.length; i++) {
        if (isInstitutionField(headers[i])) {
          headersIndex.put(headers[i].toUpperCase(), i);
        } else {
          fileErrors.add("Unknown column: " + headers[i]);
        }
      }

      br.lines()
          .forEach(
              line -> {
                String[] values = line.split(format.getDelimiter().toString());
                ParsingData<Institution> institutionData =
                    createInstitutionFromValues(values, headersIndex);

                String key = keyExtractor.apply(institutionData.entity);

                if (key == null) {
                  institutionData.errors.add("No key or code found");
                } else {
                  institutionsDataMap
                      .computeIfAbsent(key, v -> new ArrayList<>())
                      .add(institutionData);
                }
              });
    }

    return ParsingResult.<Institution>builder()
        .parsingData(institutionsDataMap)
        .fileHeadersIndex(headersIndex)
        .fileErrors(fileErrors)
        .build();
  }

  private static ParsingData<Institution> createInstitutionFromValues(
      String[] values, Map<String, Integer> headersIndex) {
    List<String> errors = new ArrayList<>();
    Institution institution = new Institution();
    handleParserResult(parseUUID(values[headersIndex.get(KEY)]), institution::setKey, errors);
    institution.setCode(values[headersIndex.get(CODE)]);
    institution.setName(values[headersIndex.get(NAME)]);
    institution.setDescription(values[headersIndex.get(DESCRIPTION)]);
    parseStringList(values[headersIndex.get(EMAIL)]).ifPresent(institution::setEmail);
    parseStringList(values[headersIndex.get(PHONE)]).ifPresent(institution::setPhone);
    handleParserResult(
        parseAlternativeCodes(values[headersIndex.get(ALT_CODES)]),
        institution::setAlternativeCodes,
        errors);
    parseStringList(values[headersIndex.get(FileFields.InstitutionFields.ADDITIONAL_NAMES)])
        .ifPresent(institution::setAdditionalNames);
    Optional.ofNullable(values[headersIndex.get(FileFields.InstitutionFields.INSTITUTION_TYPE)])
        .map(InstitutionType::valueOf)
        .ifPresent(institution::setType);
    handleParserResult(
        parseBoolean(values[headersIndex.get(ACTIVE)]), institution::setActive, errors);
    handleParserResult(
        parseUri(values[headersIndex.get(HOMEPAGE)]), institution::setHomepage, errors);
    handleParserResult(
        parseUri(values[headersIndex.get(CATALOG_URL)]), institution::setCatalogUrl, errors);
    handleParserResult(parseUri(values[headersIndex.get(API_URL)]), institution::setApiUrl, errors);
    handleParserResult(
        parseUri(values[headersIndex.get(LOGO_URL)]), institution::setLogoUrl, errors);
    handleParserResult(
        parseEnum(
            values[headersIndex.get(INSTITUTIONAL_GOVERNANCE)], InstitutionGovernance::valueOf),
        institution::setInstitutionalGovernance,
        errors);
    handleParserResult(
        parseListValues(
            values[headersIndex.get(DISCIPLINES)], s -> parseEnum(s, Discipline::valueOf)),
        institution::setDisciplines,
        errors);
    handleParserResult(
        parseBigDecimal(values[headersIndex.get(LATITUDE)]), institution::setLatitude, errors);
    handleParserResult(
        parseBigDecimal(values[headersIndex.get(LONGITUDE)]), institution::setLongitude, errors);
    handleParserResult(
        parseAddress(
            values[headersIndex.get(ADDRESS)],
            values[headersIndex.get(CITY)],
            values[headersIndex.get(PROVINCE)],
            values[headersIndex.get(POSTAL_CODE)],
            values[headersIndex.get(COUNTRY)]),
        institution::setAddress,
        errors);
    handleParserResult(
        parseAddress(
            values[headersIndex.get(MAIL_ADDRESS)],
            values[headersIndex.get(MAIL_CITY)],
            values[headersIndex.get(MAIL_PROVINCE)],
            values[headersIndex.get(MAIL_POSTAL_CODE)],
            values[headersIndex.get(MAIL_COUNTRY)]),
        institution::setMailingAddress,
        errors);
    handleParserResult(
        parseInteger(values[headersIndex.get(FOUNDING_DATE)]),
        institution::setFoundingDate,
        errors);
    institution.setGeographicDescription(values[headersIndex.get(GEOGRAPHIC_DESCRIPTION)]);
    institution.setTaxonomicDescription(values[headersIndex.get(TAXONOMIC_DESCRIPTION)]);
    handleParserResult(
        parseInteger(values[headersIndex.get(NUMBER_SPECIMENS)]),
        institution::setNumberSpecimens,
        errors);
    handleParserResult(
        parseIdentifiers(values[headersIndex.get(IDENTIFIERS)]),
        institution::setIdentifiers,
        errors);

    return ParsingData.<Institution>builder().entity(institution).errors(errors).build();
  }

  @SneakyThrows
  static ParsingResult<Contact> parseContacts(
      Path contactsPath,
      Map<String, List<ParsingData<Institution>>> institutionsMap,
      ExportFormat format,
      String keyColum) {
    Map<String, Integer> columnsIndex = new HashMap<>();

    Map<String, List<ParsingData<Contact>>> contactsByInstitutionCode = new HashMap<>();
    List<String> fileErrors = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new FileReader(contactsPath.toFile()))) {
      String[] headers = br.readLine().split(format.getDelimiter().toString());
      for (int i = 0; i < headers.length; i++) {
        if (isContactField(headers[i])) {
          columnsIndex.put(headers[i].toUpperCase(), i);
        } else {
          fileErrors.add("Unknown column: " + headers[i]);
        }
      }

      br.lines()
          .forEach(
              line -> {
                String[] values = line.split(format.getDelimiter().toString());

                ParsingData<Contact> parsedContact = createContactFromValues(values, columnsIndex);
                String institutionCode = values[columnsIndex.get(INSTITUTION_CODE)];
                // it can either be the entity code (new entities) or the entity key(entity update)
                String keyColumn = values[columnsIndex.get(keyColum)];

                if (Strings.isNullOrEmpty(keyColumn) || !institutionsMap.containsKey(keyColumn)) {
                  parsedContact.errors.add("Invalid entity key or code");
                  return;
                }

                contactsByInstitutionCode
                    .computeIfAbsent(institutionCode, v -> new ArrayList<>())
                    .add(parsedContact);
              });
    }

    return ParsingResult.<Contact>builder()
        .parsingData(contactsByInstitutionCode)
        .fileHeadersIndex(columnsIndex)
        .fileErrors(fileErrors)
        .build();
  }

  private static ParsingData<Contact> createContactFromValues(
      String[] values, Map<String, Integer> headersIndex) {

    List<String> errors = new ArrayList<>();
    Contact contact = new Contact();
    handleParserResult(
        parseInteger(values[headersIndex.get(FileFields.ContactFields.KEY)]),
        contact::setKey,
        errors);
    contact.setFirstName(values[headersIndex.get(FIRST_NAME)]);
    contact.setLastName(values[headersIndex.get(LAST_NAME)]);
    parseStringList(values[headersIndex.get(POSITION)]).ifPresent(contact::setPosition);
    parseStringList(values[headersIndex.get(FileFields.ContactFields.PHONE)])
        .ifPresent(contact::setPhone);
    parseStringList(values[headersIndex.get(FAX)]).ifPresent(contact::setFax);
    parseStringList(values[headersIndex.get(FileFields.ContactFields.EMAIL)])
        .ifPresent(contact::setEmail);
    parseStringList(values[headersIndex.get(FileFields.ContactFields.ADDRESS)])
        .ifPresent(contact::setAddress);
    contact.setCity(values[headersIndex.get(FileFields.ContactFields.CITY)]);
    contact.setProvince(values[headersIndex.get(FileFields.ContactFields.PROVINCE)]);
    handleParserResult(
        parseCountry(values[headersIndex.get(FileFields.ContactFields.COUNTRY)]),
        contact::setCountry,
        errors);
    contact.setPostalCode(values[headersIndex.get(FileFields.ContactFields.POSTAL_CODE)]);
    handleParserResult(
        parseBoolean(values[headersIndex.get(FileFields.ContactFields.PRIMARY)]),
        contact::setPrimary,
        errors);
    parseStringList(values[headersIndex.get(FileFields.ContactFields.TAXONOMIC_EXPERTISE)])
        .ifPresent(contact::setTaxonomicExpertise);
    contact.setNotes(values[headersIndex.get(FileFields.ContactFields.NOTES)]);
    handleParserResult(
        parseUserIds(values[headersIndex.get(FileFields.ContactFields.USER_IDS)]),
        contact::setUserIds,
        errors);

    return ParsingData.<Contact>builder().entity(contact).errors(errors).build();
  }

  private static <T> void handleParserResult(
    FileParsingUtils.ParserResult<T> parserResult, Consumer<T> setter, List<String> errors) {
    parserResult.getResult().ifPresent(setter);
    errors.addAll(parserResult.getErrors());
  }

  @Builder
  static class ParsingResult<T> {
    Map<String, List<ParsingData<T>>> parsingData;
    List<String> fileErrors;
    Map<String, Integer> fileHeadersIndex;
  }

  @Builder
  static class ParsingData<T> {
    T entity;
    List<String> errors;
  }
}
