/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.service.collections.batch;

import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.*;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.ADDRESS;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.CITY;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.COUNTRY;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.EMAIL;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.PHONE;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.POSTAL_CODE;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.PROVINCE;
import static org.gbif.registry.service.collections.batch.FileFields.ContactFields.*;
import static org.gbif.registry.service.collections.batch.FileFields.InstitutionFields.*;
import static org.gbif.registry.service.collections.batch.FileFields.isContactField;
import static org.gbif.registry.service.collections.batch.FileFields.isEntityField;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.*;

import com.google.common.base.Strings;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.gbif.api.model.collections.*;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.service.collections.batch.model.ContactsParserResult;
import org.gbif.registry.service.collections.batch.model.EntitiesParserResult;
import org.gbif.registry.service.collections.batch.model.ParsedData;
import org.gbif.registry.service.collections.batch.model.ParserResult;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileParser {

  @SneakyThrows
  static <T extends CollectionEntity> ParserResult<T> parseEntities(
      byte[] entitiesFile,
      ExportFormat format,
      BiFunction<String[], Map<String, Integer>, ParsedData<T>> createEntityFn,
      CollectionEntityType entityType) {

    Map<String, ParsedData<T>> dataMap = new HashMap<>();
    List<String> fileErrors = new ArrayList<>();
    List<String> duplicateKeys = new ArrayList<>();
    Map<String, Integer> headersIndex = new HashMap<>();

    // csv options
    CSVParser csvParser = new CSVParserBuilder().withSeparator(format.getDelimiter()).build();

    try (CSVReader csvReader =
        new CSVReaderBuilder(
                new BufferedReader(new InputStreamReader(new ByteArrayInputStream(entitiesFile))))
            .withCSVParser(csvParser)
            .build()) {
      // extract headers
      String[] headers = csvReader.readNextSilently();
      for (int i = 0; i < headers.length; i++) {
        if (isEntityField(headers[i], entityType)) {
          headersIndex.put(headers[i].toUpperCase(), i);
        } else {
          fileErrors.add(
              "Unknown "
                  + entityType.name().toLowerCase()
                  + " column in entities file: "
                  + headers[i]);
        }
      }

      String[] values;
      while ((values = csvReader.readNextSilently()) != null) {
        if (values.length == 0) {
          continue;
        }

        values = FileParsingUtils.normalizeValues(headersIndex.entrySet().size(), values);

        ParsedData<T> data = createEntityFn.apply(values, headersIndex);
        String entityCode = data.getEntity().getCode();
        if (entityCode == null) {
          data.getErrors().add("No code found");
        } else {
          if (dataMap.containsKey(entityCode)) {
            duplicateKeys.add(entityCode);
          }
          dataMap.put(entityCode, data);
        }
      }
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
    createBaseEntityFromValues(values, headersIndex, institution, errors);
    parseStringList(
            extractValue(values, headersIndex.get(FileFields.InstitutionFields.ADDITIONAL_NAMES)))
        .ifPresent(institution::setAdditionalNames);
    parseStringList(
            extractValue(values, headersIndex.get(FileFields.InstitutionFields.INSTITUTION_TYPE)))
        .ifPresent(institution::setTypes);
    handleParserResult(
        parseUri(extractValue(values, headersIndex.get(HOMEPAGE))),
        institution::setHomepage,
        errors);
    handleParserResult(
        parseListValues(
            extractValue(values, headersIndex.get(CATALOG_URL)), FileParsingUtils::parseUri),
        institution::setCatalogUrls,
        errors);
    handleParserResult(
        parseListValues(
            extractValue(values, headersIndex.get(API_URL)), FileParsingUtils::parseUri),
        institution::setApiUrls,
        errors);
    handleParserResult(
        parseUri(extractValue(values, headersIndex.get(LOGO_URL))),
        institution::setLogoUrl,
        errors);
    parseStringList(extractValue(values, headersIndex.get(INSTITUTIONAL_GOVERNANCE)))
        .ifPresent(institution::setInstitutionalGovernances);
    parseStringList(extractValue(values, headersIndex.get(DISCIPLINES)))
        .ifPresent(institution::setDisciplines);
    handleParserResult(
        parseBigDecimal(extractValue(values, headersIndex.get(LATITUDE))),
        institution::setLatitude,
        errors);
    handleParserResult(
        parseBigDecimal(extractValue(values, headersIndex.get(LONGITUDE))),
        institution::setLongitude,
        errors);
    handleParserResult(
        parseInteger(extractValue(values, headersIndex.get(FOUNDING_DATE))),
        institution::setFoundingDate,
        errors);
    handleParserResult(
        parseInteger(extractValue(values, headersIndex.get(NUMBER_SPECIMENS))),
        institution::setNumberSpecimens,
        errors);

    return ParsedData.<Institution>builder().entity(institution).errors(errors).build();
  }

  static ParsedData<Collection> createCollectionFromValues(
      String[] values, Map<String, Integer> headersIndex) {
    List<String> errors = new ArrayList<>();
    Collection collection = new Collection();
    createBaseEntityFromValues(values, headersIndex, collection, errors);
    parseStringList(
            extractValue(values, headersIndex.get(FileFields.CollectionFields.CONTENT_TYPES)))
        .ifPresent(collection::setContentTypes);
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
    handleParserResult(
        parseUri(extractValue(values, headersIndex.get(HOMEPAGE))),
        collection::setHomepage,
        errors);
    handleParserResult(
        parseListValues(
            extractValue(values, headersIndex.get(CATALOG_URL)), FileParsingUtils::parseUri),
        collection::setCatalogUrls,
        errors);
    handleParserResult(
        parseListValues(
            extractValue(values, headersIndex.get(API_URL)), FileParsingUtils::parseUri),
        collection::setApiUrls,
        errors);
    parseStringList(
            extractValue(values, headersIndex.get(FileFields.CollectionFields.PRESERVATION_TYPES)))
        .ifPresent(collection::setPreservationTypes);
    collection.setAccessionStatus(
        extractValue(values, headersIndex.get(FileFields.CollectionFields.ACCESSION_STATUS)));
    handleParserResult(
        parseUUID(
            extractValue(values, headersIndex.get(FileFields.CollectionFields.INSTITUTION_KEY))),
        collection::setInstitutionKey,
        errors);
    handleParserResult(
        parseInteger(extractValue(values, headersIndex.get(NUMBER_SPECIMENS))),
        collection::setNumberSpecimens,
        errors);
    collection.setTaxonomicCoverage(
        extractValue(values, headersIndex.get(FileFields.CollectionFields.TAXONOMIC_COVERAGE)));
    collection.setGeographicCoverage(
        extractValue(values, headersIndex.get(FileFields.CollectionFields.GEOGRAPHIC_COVERAGE)));
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
    collection.setDepartment(
        extractValue(values, headersIndex.get(FileFields.CollectionFields.DEPARTMENT)));
    collection.setDivision(
        extractValue(values, headersIndex.get(FileFields.CollectionFields.DIVISION)));
    collection.setTemporalCoverage(
        extractValue(values, headersIndex.get(FileFields.CollectionFields.TEMPORAL_COVERAGE)));

    return ParsedData.<Collection>builder().entity(collection).errors(errors).build();
  }

  private static <T extends CollectionEntity> void createBaseEntityFromValues(
      String[] values, Map<String, Integer> headersIndex, T entity, List<String> errors) {
    handleParserResult(
        parseUUID(extractValue(values, headersIndex.get(KEY))), entity::setKey, errors);
    entity.setCode(extractValue(values, headersIndex.get(CODE)));
    entity.setName(extractValue(values, headersIndex.get(NAME)));
    entity.setDescription(extractValue(values, headersIndex.get(DESCRIPTION)));
    parseStringList(extractValue(values, headersIndex.get(EMAIL))).ifPresent(entity::setEmail);
    parseStringList(extractValue(values, headersIndex.get(PHONE))).ifPresent(entity::setPhone);
    handleParserResult(
        parseAlternativeCodes(extractValue(values, headersIndex.get(ALT_CODES))),
        entity::setAlternativeCodes,
        errors);
    handleParserResult(
        parseBoolean(extractValue(values, headersIndex.get(ACTIVE))), entity::setActive, errors);
    handleParserResult(
        parseAddress(
            extractValue(values, headersIndex.get(ADDRESS)),
            extractValue(values, headersIndex.get(CITY)),
            extractValue(values, headersIndex.get(PROVINCE)),
            extractValue(values, headersIndex.get(POSTAL_CODE)),
            extractValue(values, headersIndex.get(COUNTRY))),
        entity::setAddress,
        errors);
    handleParserResult(
        parseAddress(
            extractValue(values, headersIndex.get(MAIL_ADDRESS)),
            extractValue(values, headersIndex.get(MAIL_CITY)),
            extractValue(values, headersIndex.get(MAIL_PROVINCE)),
            extractValue(values, headersIndex.get(MAIL_POSTAL_CODE)),
            extractValue(values, headersIndex.get(MAIL_COUNTRY))),
        entity::setMailingAddress,
        errors);
    handleParserResult(
        parseIdentifiers(extractValue(values, headersIndex.get(IDENTIFIERS))),
        entity::setIdentifiers,
        errors);
    handleParserResult(
        parseUri(extractValue(values, headersIndex.get(FEATURED_IMAGE_URL))),
        entity::setFeaturedImageUrl,
        errors);
    handleParserResult(
        parseEnum(
            extractValue(values, headersIndex.get(FEATURED_IMAGE_LICENSE)),
            v -> License.fromString(v).orElse(null)),
        entity::setFeaturedImageLicense,
        errors);
  }

  static String extractValue(String[] values, Integer index) {
    return index != null && index < values.length ? Strings.emptyToNull(values[index]) : null;
  }

  @SneakyThrows
  static <T extends CollectionEntity> ContactsParserResult parseContacts(
      byte[] contactsFile,
      Map<String, ParsedData<T>> entitiesMap,
      ExportFormat format,
      String entityCodeColum) {
    Map<String, Integer> columnsIndex = new HashMap<>();

    Map<String, List<ParsedData<Contact>>> contactsByEntityKey = new HashMap<>();
    List<String> fileErrors = new ArrayList<>();
    List<String> duplicateContactKeys = new ArrayList<>();
    Map<String, ParsedData<Contact>> contactsByKey = new HashMap<>();

    // csv options
    CSVParser csvParser = new CSVParserBuilder().withSeparator(format.getDelimiter()).build();

    try (CSVReader csvReader =
        new CSVReaderBuilder(
                new BufferedReader(new InputStreamReader(new ByteArrayInputStream(contactsFile))))
            .withCSVParser(csvParser)
            .build()) {
      String[] headers = csvReader.readNextSilently();
      for (int i = 0; i < headers.length; i++) {
        if (isContactField(headers[i])) {
          columnsIndex.put(headers[i].toUpperCase(), i);
        } else {
          fileErrors.add("Unknown contact column in contacts file: " + headers[i]);
        }
      }

      String[] values;
      while ((values = csvReader.readNextSilently()) != null) {
        if (values.length == 0) {
          continue;
        }

        values = FileParsingUtils.normalizeValues(columnsIndex.entrySet().size(), values);

        ParsedData<Contact> parsedContact = createContactFromValues(values, columnsIndex);
        if (!columnsIndex.containsKey(entityCodeColum)) {
          parsedContact.getErrors().add("There is no column with entity code");
          continue;
        }

        String entityCode = values[columnsIndex.get(entityCodeColum)];

        if (Strings.isNullOrEmpty(entityCode) || !entitiesMap.containsKey(entityCode)) {
          parsedContact.getErrors().add("Invalid entity code");
          continue;
        }

        // assign the contact to the entity
        entitiesMap.get(entityCode).getEntity().getContactPersons().add(parsedContact.getEntity());

        // get a key of the contact. For updates it's the contact key but for inital imports
        // we hash all the values of the fields
        String uniqueKey = String.valueOf(getContactUniqueKey(values, columnsIndex));

        if (contactsByKey.containsKey(uniqueKey)) {
          duplicateContactKeys.add(uniqueKey);
        }
        contactsByKey.put(uniqueKey, parsedContact);
        contactsByEntityKey.computeIfAbsent(entityCode, k -> new ArrayList<>()).add(parsedContact);
      }
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
