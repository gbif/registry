package org.gbif.registry.service.collections.batch;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.vocabulary.collections.AccessionStatus;
import org.gbif.api.vocabulary.collections.CollectionContentType;
import org.gbif.api.vocabulary.collections.Discipline;
import org.gbif.api.vocabulary.collections.InstitutionGovernance;
import org.gbif.api.vocabulary.collections.InstitutionType;
import org.gbif.api.vocabulary.collections.PreservationType;
import org.gbif.registry.service.collections.batch.model.ContactsParserResult;
import org.gbif.registry.service.collections.batch.model.EntitiesParserResult;
import org.gbif.registry.service.collections.batch.model.ParsedData;
import org.gbif.registry.service.collections.batch.model.ParserResult;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
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
import static org.gbif.registry.service.collections.batch.FileFields.isEntityField;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseAddress;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseAlternativeCodes;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseBigDecimal;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseBoolean;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseCollectionsSummary;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseCountry;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseEnum;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseIdentifiers;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseInteger;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseListValues;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseStringList;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseUUID;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseUri;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.parseUserIds;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.splitLine;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileParser {

  @SneakyThrows
  static <T extends CollectionEntity> ParserResult<T> parseEntities(
      byte[] entitiesFile,
      ExportFormat format,
      BiFunction<String[], Map<String, Integer>, ParsedData<T>> createEntityFn,
      Function<T, String> keyExtractor,
      CollectionEntityType entityType) {

    Map<String, ParsedData<T>> dataMap = new HashMap<>();
    List<String> fileErrors = new ArrayList<>();
    List<String> duplicateKeys = new ArrayList<>();
    Map<String, Integer> headersIndex = new HashMap<>();
    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(new ByteArrayInputStream(entitiesFile)))) {
      // extract headers
      String[] headers = br.readLine().split(format.getDelimiter().toString());
      for (int i = 0; i < headers.length; i++) {
        if (isEntityField(headers[i], entityType)) {
          headersIndex.put(headers[i].toUpperCase(), i);
        } else {
          fileErrors.add("Unknown " + entityType.name().toLowerCase() + " column: " + headers[i]);
        }
      }

      br.lines()
          .forEach(
              line -> {
                String[] values = splitLine(format, headersIndex.entrySet().size(), line);
                ParsedData<T> data = createEntityFn.apply(values, headersIndex);

                String key = keyExtractor.apply(data.getEntity());

                if (key == null) {
                  data.getErrors().add("No key or code found");
                } else {
                  if (dataMap.containsKey(key)) {
                    duplicateKeys.add(key);
                  }
                  dataMap.put(key, data);
                }
              });
    }

    return EntitiesParserResult.<T>builder()
        .format(format)
        .parsedDataMap(dataMap)
        .duplicates(duplicateKeys)
        .fileHeadersIndex(headersIndex)
        .fileErrors(fileErrors)
        .build();
  }

  static ParsedData<Institution> createInstitutionFromValues(
      String[] values, Map<String, Integer> headersIndex) {
    List<String> errors = new ArrayList<>();
    Institution institution = new Institution();
    handleParserResult(
        parseUUID(extractValue(values, headersIndex.get(KEY))), institution::setKey, errors);
    institution.setCode(extractValue(values, headersIndex.get(CODE)));
    institution.setName(extractValue(values, headersIndex.get(NAME)));
    institution.setDescription(extractValue(values, headersIndex.get(DESCRIPTION)));
    parseStringList(extractValue(values, headersIndex.get(EMAIL))).ifPresent(institution::setEmail);
    parseStringList(extractValue(values, headersIndex.get(PHONE))).ifPresent(institution::setPhone);
    handleParserResult(
        parseAlternativeCodes(extractValue(values, headersIndex.get(ALT_CODES))),
        institution::setAlternativeCodes,
        errors);
    parseStringList(
            extractValue(values, headersIndex.get(FileFields.InstitutionFields.ADDITIONAL_NAMES)))
        .ifPresent(institution::setAdditionalNames);
    Optional.ofNullable(
            extractValue(values, headersIndex.get(FileFields.InstitutionFields.INSTITUTION_TYPE)))
        .map(InstitutionType::valueOf)
        .ifPresent(institution::setType);
    handleParserResult(
        parseBoolean(extractValue(values, headersIndex.get(ACTIVE))),
        institution::setActive,
        errors);
    handleParserResult(
        parseUri(extractValue(values, headersIndex.get(HOMEPAGE))),
        institution::setHomepage,
        errors);
    handleParserResult(
        parseUri(extractValue(values, headersIndex.get(CATALOG_URL))),
        institution::setCatalogUrl,
        errors);
    handleParserResult(
        parseUri(extractValue(values, headersIndex.get(API_URL))), institution::setApiUrl, errors);
    handleParserResult(
        parseUri(extractValue(values, headersIndex.get(LOGO_URL))),
        institution::setLogoUrl,
        errors);
    handleParserResult(
        parseEnum(
            extractValue(values, headersIndex.get(INSTITUTIONAL_GOVERNANCE)),
            InstitutionGovernance::valueOf),
        institution::setInstitutionalGovernance,
        errors);
    handleParserResult(
        parseListValues(
            extractValue(values, headersIndex.get(DISCIPLINES)),
            s -> parseEnum(s, Discipline::valueOf)),
        institution::setDisciplines,
        errors);
    handleParserResult(
        parseBigDecimal(extractValue(values, headersIndex.get(LATITUDE))),
        institution::setLatitude,
        errors);
    handleParserResult(
        parseBigDecimal(extractValue(values, headersIndex.get(LONGITUDE))),
        institution::setLongitude,
        errors);
    handleParserResult(
        parseAddress(
            extractValue(values, headersIndex.get(ADDRESS)),
            extractValue(values, headersIndex.get(CITY)),
            extractValue(values, headersIndex.get(PROVINCE)),
            extractValue(values, headersIndex.get(POSTAL_CODE)),
            extractValue(values, headersIndex.get(COUNTRY))),
        institution::setAddress,
        errors);
    handleParserResult(
        parseAddress(
            extractValue(values, headersIndex.get(MAIL_ADDRESS)),
            extractValue(values, headersIndex.get(MAIL_CITY)),
            extractValue(values, headersIndex.get(MAIL_PROVINCE)),
            extractValue(values, headersIndex.get(MAIL_POSTAL_CODE)),
            extractValue(values, headersIndex.get(MAIL_COUNTRY))),
        institution::setMailingAddress,
        errors);
    handleParserResult(
        parseInteger(extractValue(values, headersIndex.get(FOUNDING_DATE))),
        institution::setFoundingDate,
        errors);
    institution.setGeographicDescription(
        extractValue(values, headersIndex.get(GEOGRAPHIC_DESCRIPTION)));
    institution.setTaxonomicDescription(
        extractValue(values, headersIndex.get(TAXONOMIC_DESCRIPTION)));
    handleParserResult(
        parseInteger(extractValue(values, headersIndex.get(NUMBER_SPECIMENS))),
        institution::setNumberSpecimens,
        errors);
    handleParserResult(
        parseIdentifiers(extractValue(values, headersIndex.get(IDENTIFIERS))),
        institution::setIdentifiers,
        errors);

    return ParsedData.<Institution>builder().entity(institution).errors(errors).build();
  }

  static ParsedData<Collection> createCollectionFromValues(
      String[] values, Map<String, Integer> headersIndex) {
    List<String> errors = new ArrayList<>();
    Collection collection = new Collection();
    handleParserResult(
        parseUUID(extractValue(values, headersIndex.get(KEY))), collection::setKey, errors);
    collection.setCode(extractValue(values, headersIndex.get(CODE)));
    collection.setName(extractValue(values, headersIndex.get(NAME)));
    collection.setDescription(extractValue(values, headersIndex.get(DESCRIPTION)));
    handleParserResult(
        parseListValues(
            extractValue(values, headersIndex.get(FileFields.CollectionFields.CONTENT_TYPES)),
            s -> parseEnum(s, CollectionContentType::valueOf)),
        collection::setContentTypes,
        errors);
    handleParserResult(
        parseBoolean(extractValue(values, headersIndex.get(ACTIVE))),
        collection::setActive,
        errors);
    handleParserResult(
        parseBoolean(
            extractValue(
                values, headersIndex.get(FileFields.CollectionFields.PERSONAL_COLLECTION))),
        collection::setPersonalCollection,
        errors);
    handleParserResult(
        FileParsingUtils.parseDoi(
            extractValue(values, headersIndex.get(FileFields.CollectionFields.DOI))),
        collection::setDoi,
        errors);
    parseStringList(extractValue(values, headersIndex.get(EMAIL))).ifPresent(collection::setEmail);
    parseStringList(extractValue(values, headersIndex.get(PHONE))).ifPresent(collection::setPhone);
    handleParserResult(
        parseUri(extractValue(values, headersIndex.get(HOMEPAGE))),
        collection::setHomepage,
        errors);
    handleParserResult(
        parseUri(extractValue(values, headersIndex.get(CATALOG_URL))),
        collection::setCatalogUrl,
        errors);
    handleParserResult(
        parseUri(extractValue(values, headersIndex.get(API_URL))), collection::setApiUrl, errors);
    handleParserResult(
        parseListValues(
            extractValue(values, headersIndex.get(FileFields.CollectionFields.PRESERVATION_TYPES)),
            s -> parseEnum(s, PreservationType::valueOf)),
        collection::setPreservationTypes,
        errors);
    handleParserResult(
        parseEnum(
            extractValue(values, headersIndex.get(FileFields.CollectionFields.ACCESSION_STATUS)),
            AccessionStatus::valueOf),
        collection::setAccessionStatus,
        errors);
    handleParserResult(
        parseUUID(
            extractValue(values, headersIndex.get(FileFields.CollectionFields.INSTITUTION_KEY))),
        collection::setInstitutionKey,
        errors);
    handleParserResult(
        parseAddress(
            extractValue(values, headersIndex.get(ADDRESS)),
            extractValue(values, headersIndex.get(CITY)),
            extractValue(values, headersIndex.get(PROVINCE)),
            extractValue(values, headersIndex.get(POSTAL_CODE)),
            extractValue(values, headersIndex.get(COUNTRY))),
        collection::setAddress,
        errors);
    handleParserResult(
        parseAddress(
            extractValue(values, headersIndex.get(MAIL_ADDRESS)),
            extractValue(values, headersIndex.get(MAIL_CITY)),
            extractValue(values, headersIndex.get(MAIL_PROVINCE)),
            extractValue(values, headersIndex.get(MAIL_POSTAL_CODE)),
            extractValue(values, headersIndex.get(MAIL_COUNTRY))),
        collection::setMailingAddress,
        errors);
    handleParserResult(
        parseIdentifiers(extractValue(values, headersIndex.get(IDENTIFIERS))),
        collection::setIdentifiers,
        errors);
    handleParserResult(
        parseInteger(extractValue(values, headersIndex.get(NUMBER_SPECIMENS))),
        collection::setNumberSpecimens,
        errors);
    collection.setTaxonomicCoverage(
        extractValue(values, headersIndex.get(FileFields.CollectionFields.TAXONOMIC_COVERAGE)));
    collection.setGeography(
        extractValue(values, headersIndex.get(FileFields.CollectionFields.GEOGRAPHY)));
    collection.setNotes(extractValue(values, headersIndex.get(FileFields.CollectionFields.NOTES)));
    parseStringList(
            extractValue(
                values, headersIndex.get(FileFields.CollectionFields.INCORPORATED_COLLECTIONS)))
        .ifPresent(collection::setIncorporatedCollections);
    parseStringList(
            extractValue(
                values, headersIndex.get(FileFields.CollectionFields.IMPORTANT_COLLECTORS)))
        .ifPresent(collection::setImportantCollectors);
    handleParserResult(
        parseCollectionsSummary(
            extractValue(values, headersIndex.get(FileFields.CollectionFields.COLLECTION_SUMMARY))),
        collection::setCollectionSummary,
        errors);
    handleParserResult(
        parseAlternativeCodes(extractValue(values, headersIndex.get(ALT_CODES))),
        collection::setAlternativeCodes,
        errors);
    collection.setDepartment(
        extractValue(values, headersIndex.get(FileFields.CollectionFields.DEPARTMENT)));
    collection.setDivision(
        extractValue(values, headersIndex.get(FileFields.CollectionFields.DIVISION)));

    return ParsedData.<Collection>builder().entity(collection).errors(errors).build();
  }

  static String extractValue(String[] values, Integer index) {
    return index != null && index < values.length ? values[index] : null;
  }

  @SneakyThrows
  static <T extends CollectionEntity> ContactsParserResult parseContacts(
      byte[] contactsFile,
      Map<String, ParsedData<T>> entitiesMap,
      ExportFormat format,
      String entityKeyColum) {
    Map<String, Integer> columnsIndex = new HashMap<>();

    Map<String, List<ParsedData<Contact>>> contactsByEntityKey = new HashMap<>();
    List<String> fileErrors = new ArrayList<>();
    List<String> duplicateContactKeys = new ArrayList<>();
    Map<String, ParsedData<Contact>> contactsByKey = new HashMap<>();
    try (BufferedReader br =
        new BufferedReader(new InputStreamReader(new ByteArrayInputStream(contactsFile)))) {
      String[] headers = br.readLine().split(format.getDelimiter().toString());
      for (int i = 0; i < headers.length; i++) {
        if (isContactField(headers[i])) {
          columnsIndex.put(headers[i].toUpperCase(), i);
        } else {
          fileErrors.add("Unknown contact column: " + headers[i]);
        }
      }

      br.lines()
          .forEach(
              line -> {
                String[] values = splitLine(format, columnsIndex.entrySet().size(), line);

                ParsedData<Contact> parsedContact = createContactFromValues(values, columnsIndex);
                // it can either be the entity code (new entities) or the entity key(entity update)
                if (!columnsIndex.containsKey(entityKeyColum)) {
                  parsedContact.getErrors().add("There is no column with entity key or code");
                  return;
                }

                String entityKey = values[columnsIndex.get(entityKeyColum)];

                if (Strings.isNullOrEmpty(entityKey) || !entitiesMap.containsKey(entityKey)) {
                  parsedContact.getErrors().add("Invalid entity key or code");
                  return;
                }

                // assign the contact to the entity
                entitiesMap
                    .get(entityKey)
                    .getEntity()
                    .getContactPersons()
                    .add(parsedContact.getEntity());

                // get a key of the contact. For updates it's the contact key but for inital imports
                // we hash all the values of the fields
                String uniqueKey = String.valueOf(getContactUniqueKey(values, columnsIndex));

                if (contactsByKey.containsKey(uniqueKey)) {
                  duplicateContactKeys.add(uniqueKey);
                }
                contactsByKey.put(uniqueKey, parsedContact);
                contactsByEntityKey
                    .computeIfAbsent(entityKey, k -> new ArrayList<>())
                    .add(parsedContact);
              });
    }

    return ContactsParserResult.builder()
        .format(format)
        .contactsByEntity(contactsByEntityKey)
        .contactsByKey(contactsByKey)
        .duplicates(duplicateContactKeys)
        .fileHeadersIndex(columnsIndex)
        .fileErrors(fileErrors)
        .build();
  }

  private static ParsedData<Contact> createContactFromValues(
      String[] values, Map<String, Integer> headersIndex) {
    List<String> errors = new ArrayList<>();
    Contact contact = new Contact();
    handleParserResult(
        parseInteger(extractValue(values, headersIndex.get(KEY))), contact::setKey, errors);
    contact.setFirstName(extractValue(values, headersIndex.get(FIRST_NAME)));
    contact.setLastName(extractValue(values, headersIndex.get(LAST_NAME)));
    parseStringList(extractValue(values, headersIndex.get(POSITION)))
        .ifPresent(contact::setPosition);
    parseStringList(extractValue(values, headersIndex.get(FileFields.ContactFields.PHONE)))
        .ifPresent(contact::setPhone);
    parseStringList(extractValue(values, headersIndex.get(FAX))).ifPresent(contact::setFax);
    parseStringList(extractValue(values, headersIndex.get(FileFields.ContactFields.EMAIL)))
        .ifPresent(contact::setEmail);
    parseStringList(extractValue(values, headersIndex.get(FileFields.ContactFields.ADDRESS)))
        .ifPresent(contact::setAddress);
    contact.setCity(extractValue(values, headersIndex.get(FileFields.ContactFields.CITY)));
    contact.setProvince(extractValue(values, headersIndex.get(FileFields.ContactFields.PROVINCE)));
    handleParserResult(
        parseCountry(extractValue(values, headersIndex.get(FileFields.ContactFields.COUNTRY))),
        contact::setCountry,
        errors);
    contact.setPostalCode(
        extractValue(values, headersIndex.get(FileFields.ContactFields.POSTAL_CODE)));
    handleParserResult(
        parseBoolean(extractValue(values, headersIndex.get(FileFields.ContactFields.PRIMARY))),
        contact::setPrimary,
        errors);
    parseStringList(
            extractValue(values, headersIndex.get(FileFields.ContactFields.TAXONOMIC_EXPERTISE)))
        .ifPresent(contact::setTaxonomicExpertise);
    contact.setNotes(extractValue(values, headersIndex.get(FileFields.ContactFields.NOTES)));
    handleParserResult(
        parseUserIds(extractValue(values, headersIndex.get(FileFields.ContactFields.USER_IDS))),
        contact::setUserIds,
        errors);

    return ParsedData.<Contact>builder().entity(contact).errors(errors).build();
  }

  static int getContactUniqueKey(String[] values, Map<String, Integer> headersIndex) {
    String rawKey = extractValue(values, headersIndex.get(KEY));
    FileParsingUtils.ParserResult<Integer> keyParsedResult = parseInteger(rawKey);
    if (keyParsedResult.getResult().isPresent()) {
      return keyParsedResult.getResult().get();
    }

    List<String> rawValues = new ArrayList<>();
    Optional.ofNullable(extractValue(values, headersIndex.get(FIRST_NAME)))
        .ifPresent(rawValues::add);
    Optional.ofNullable(extractValue(values, headersIndex.get(LAST_NAME)))
        .ifPresent(rawValues::add);
    Optional.ofNullable(extractValue(values, headersIndex.get(POSITION))).ifPresent(rawValues::add);
    Optional.ofNullable(extractValue(values, headersIndex.get(FileFields.ContactFields.PHONE)))
        .ifPresent(rawValues::add);
    Optional.ofNullable(extractValue(values, headersIndex.get(FAX))).ifPresent(rawValues::add);
    Optional.ofNullable(extractValue(values, headersIndex.get(FileFields.ContactFields.EMAIL)))
        .ifPresent(rawValues::add);
    Optional.ofNullable(extractValue(values, headersIndex.get(FileFields.ContactFields.CITY)))
        .ifPresent(rawValues::add);
    Optional.ofNullable(extractValue(values, headersIndex.get(FileFields.ContactFields.PROVINCE)))
        .ifPresent(rawValues::add);
    Optional.ofNullable(extractValue(values, headersIndex.get(FileFields.ContactFields.COUNTRY)))
        .ifPresent(rawValues::add);
    Optional.ofNullable(
            extractValue(values, headersIndex.get(FileFields.ContactFields.POSTAL_CODE)))
        .ifPresent(rawValues::add);
    Optional.ofNullable(extractValue(values, headersIndex.get(FileFields.ContactFields.PRIMARY)))
        .ifPresent(rawValues::add);
    Optional.ofNullable(
            extractValue(values, headersIndex.get(FileFields.ContactFields.TAXONOMIC_EXPERTISE)))
        .ifPresent(rawValues::add);
    Optional.ofNullable(extractValue(values, headersIndex.get(FileFields.ContactFields.NOTES)))
        .ifPresent(rawValues::add);
    Optional.ofNullable(extractValue(values, headersIndex.get(FileFields.ContactFields.USER_IDS)))
        .ifPresent(rawValues::add);

    return Objects.hash(rawValues);
  }

  private static <T> void handleParserResult(
      FileParsingUtils.ParserResult<T> parserResult, Consumer<T> setter, List<String> errors) {
    parserResult.getResult().ifPresent(setter);
    Optional.ofNullable(parserResult.getErrors()).ifPresent(errors::addAll);
  }
}
