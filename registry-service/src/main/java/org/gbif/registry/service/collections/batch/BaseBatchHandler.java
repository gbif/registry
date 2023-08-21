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

import org.gbif.api.model.collections.Batch;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.registry.persistence.mapper.collections.BatchMapper;
import org.gbif.registry.service.collections.batch.model.ContactsParserResult;
import org.gbif.registry.service.collections.batch.model.ParsedData;
import org.gbif.registry.service.collections.batch.model.ParserResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.annotations.VisibleForTesting;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.CODE;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.ERRORS;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.KEY;
import static org.gbif.registry.service.collections.batch.FileParser.parseContacts;
import static org.gbif.registry.service.collections.batch.FileParser.parseEntities;

@Slf4j
public abstract class BaseBatchHandler<T extends CollectionEntity> implements BatchHandler {

  private final BatchMapper batchMapper;
  private final CollectionEntityService<T> entityService;
  private final Path resultDirPath;
  private final CollectionEntityType entityType;
  private final Class<T> clazz;

  BaseBatchHandler(
      BatchMapper batchMapper,
      CollectionEntityService<T> entityService,
      String resultDirPath,
      CollectionEntityType entityType,
      Class<T> clazz) {
    this.batchMapper = batchMapper;
    this.entityService = entityService;
    this.resultDirPath = Paths.get(resultDirPath);
    this.entityType = entityType;
    this.clazz = clazz;
  }

  @Async
  @Override
  public void handleBatch(
      byte[] entitiesFile, byte[] contactsFile, ExportFormat format, Batch batch) {
    Objects.requireNonNull(batch.getKey());

    try {
      ParserResult<T> parsingResult =
          parseEntities(entitiesFile, format, this::createEntityFromValues, entityType);
      Optional.ofNullable(parsingResult.getFileErrors())
          .ifPresent(e -> batch.getErrors().addAll(e));

      if (!parsingResult.getDuplicates().isEmpty()) {
        batch.setState(Batch.State.FAILED);
        batch
            .getErrors()
            .add(
                "Duplicate "
                    + entityType.name().toLowerCase()
                    + " codes: "
                    + String.join(",", parsingResult.getDuplicates()));
        batchMapper.update(batch);
        return;
      }

      ContactsParserResult contactsParsed =
          parseContacts(
              contactsFile,
              parsingResult.getParsedDataMap(),
              format,
              FileFields.ContactFields.getEntityCode(entityType));
      Optional.ofNullable(contactsParsed.getFileErrors())
          .ifPresent(e -> batch.getErrors().addAll(e));

      if (!contactsParsed.getDuplicates().isEmpty()) {
        batch
            .getErrors()
            .add(
                "Duplicate contact keys: "
                    + contactsParsed.getDuplicates().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(",")));
      }

      for (ParsedData<T> parsedEntity : parsingResult.getParsedDataMap().values()) {
        T entity = parsedEntity.getEntity();

        boolean succesful = false;
        if (entity.getKey() == null) {
          succesful = createEntity(entity, parsedEntity);
        } else {
          succesful = updateEntity(entity, parsedEntity, parsingResult.getFileHeadersIndex());
        }

        if (!succesful) {
          continue;
        }

        handleIdentifiers(parsedEntity, entity);

        // handle contacts of the entity
        List<ParsedData<Contact>> entityContacts =
            contactsParsed
                .getContactsByEntity()
                .getOrDefault(entity.getCode(), Collections.emptyList());

        if (entityContacts.isEmpty()) {
          continue;
        }

        handleContacts(parsedEntity, entity, entityContacts);
      }

      if (!parsingResult.getParsedDataMap().isEmpty()) {
        // write the results to a new file
        Path resultPath =
            createResultFile(
                entitiesFile, contactsFile, parsingResult, contactsParsed, batch.getKey());
        batch.setState(Batch.State.FINISHED);
        batch.setResultFilePath(resultPath.toFile().getAbsolutePath());
      } else {
        batch.setState(Batch.State.FAILED);
        batch.getErrors().add("No entities found. Check the file delimiter is correct");
      }

      // update batch
      batchMapper.update(batch);
    } catch (Exception ex) {
      batch.getErrors().add("Import failed: " + ex.getMessage());
      batch.setState(Batch.State.FAILED);
      batchMapper.update(batch);
    }
  }

  private void handleContacts(
      ParsedData<T> parsedEntity, T entity, List<ParsedData<Contact>> entityContacts) {
    // delete contacts
    List<Contact> existingContacts = entityService.listContactPersons(entity.getKey());
    if (existingContacts != null && !existingContacts.isEmpty()) {
      for (Contact existing : existingContacts) {
        if (!containsContact(entityContacts, existing)) {
          try {
            entityService.removeContactPerson(entity.getKey(), existing.getKey());
          } catch (Exception ex) {
            parsedEntity.getErrors().add("Couldn't remove contact: " + ex.getMessage());
          }
        }
      }
    }

    // update or create new contacts
    for (ParsedData<Contact> contact : entityContacts) {
      if (contact.getEntity().getKey() == null) {
        // create new contact
        try {
          entityService.addContactPerson(entity.getKey(), contact.getEntity());
        } catch (Exception ex) {
          contact.getErrors().add("Couldn't add contact: " + ex.getMessage());
        }
      } else {
        try {
          entityService.updateContactPerson(entity.getKey(), contact.getEntity());
        } catch (Exception ex) {
          contact.getErrors().add("Couldn't update contact: " + ex.getMessage());
        }
      }
    }
  }

  private void handleIdentifiers(ParsedData<T> parsedEntity, T entity) {
    List<Identifier> existingIdentifiers = entityService.listIdentifiers(entity.getKey());
    if (existingIdentifiers != null && !existingIdentifiers.isEmpty()) {
      // delete identifiers
      for (Identifier existing : existingIdentifiers) {
        if (!containsIdentifier(entity.getIdentifiers(), existing)) {
          try {
            entityService.deleteIdentifier(entity.getKey(), existing.getKey());
          } catch (Exception ex) {
            parsedEntity.getErrors().add("Couldn't delete identifier: " + ex.getMessage());
          }
        }
      }
    }

    // create new identifiers
    entity.getIdentifiers().stream()
        .filter(i -> !containsIdentifier(existingIdentifiers, i))
        .forEach(i -> addIdentifier(entity.getKey(), i, parsedEntity.getErrors()));
  }

  private boolean createEntity(T entity, ParsedData<T> parsedEntity) {
    List<UUID> existingEntities = findEntity(entity.getCode(), entity.getIdentifiers());
    if (!existingEntities.isEmpty()) {
      parsedEntity
          .getErrors()
          .add(
              "Other "
                  + entityType.name().toLowerCase()
                  + "s already exist with the same code or id: "
                  + existingEntities.stream().map(UUID::toString).collect(Collectors.joining())
                  + ". Contacts skipped");
      return false;
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!allowedToCreateEntity(entity, authentication)) {
      parsedEntity
          .getErrors()
          .add(
              "User "
                  + authentication.getName()
                  + " not allowed to create this "
                  + entityType.name().toLowerCase()
                  + ". Contacts skipped");
      return false;
    }

    // create entity
    entity.setCreatedBy(authentication.getName());

    try {
      UUID key = entityService.create(entity);
      entity.setKey(key);
      return true;
    } catch (Exception ex) {
      parsedEntity
          .getErrors()
          .add("Couldn't create " + entityType.name().toLowerCase() + ": " + ex.getMessage());
    }

    return false;
  }

  private boolean updateEntity(
      T entity, ParsedData<T> parsedEntity, Map<String, Integer> fileHeadersIndex)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    if (!entityService.exists(entity.getKey()) || entity.getKey() == null) {
      parsedEntity.getErrors().add(entityType.name().toLowerCase() + " doesn't exist");
      return false;
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!allowedToUpdateEntity(entity, authentication)) {
      parsedEntity
          .getErrors()
          .add(
              "User "
                  + authentication.getName()
                  + " not allowed to update this "
                  + entityType.name().toLowerCase()
                  + ". Contacts skipped");
      return false;
    }

    // update entity
    T mergedEntity =
        mergeEntities(entityService.get(entity.getKey()), entity, fileHeadersIndex.keySet());
    mergedEntity.setModifiedBy(authentication.getName());

    try {
      entityService.update(mergedEntity);
      return true;
    } catch (Exception ex) {
      parsedEntity
          .getErrors()
          .add("Couldn't update " + entityType.name().toLowerCase() + ": " + ex.getMessage());
    }
    return false;
  }

  @VisibleForTesting
  public T mergeEntities(T existing, T parsed, Set<String> headers)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    for (String h : getEntityFields()) {
      if (headers.contains(h)) {

        if (h.startsWith(FileFields.CommonFields.ADDRESS_PREFIX)) {
          if (existing.getAddress() != null && existing.getAddress().getKey() != null) {
            parsed.getAddress().setKey(existing.getAddress().getKey());
          }
          existing.setAddress(parsed.getAddress());
          continue;
        }

        if (h.startsWith(FileFields.CommonFields.MAILING_ADDRESS_PREFIX)) {
          if (existing.getMailingAddress() != null
              && existing.getMailingAddress().getKey() != null) {
            parsed.getMailingAddress().setKey(existing.getMailingAddress().getKey());
          }
          existing.setMailingAddress(parsed.getMailingAddress());
          continue;
        }

        Optional<Field> field =
            Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.getName().equalsIgnoreCase(h.replace("_", "")))
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
    return identifierList != null
        && identifierList.stream()
            .anyMatch(
                i ->
                    i.getIdentifier().equals(identifier.getIdentifier())
                        && i.getType() == identifier.getType());
  }

  private boolean containsContact(
      java.util.Collection<ParsedData<Contact>> contactList, Contact contact) {
    return contactList != null
        && contactList.stream()
            .map(ParsedData::getEntity)
            .filter(c -> c.getKey() != null)
            .anyMatch(c -> c.getKey().equals(contact.getKey()));
  }

  @SneakyThrows
  @VisibleForTesting
  public Path createResultFile(
      byte[] entitiesFile,
      byte[] contactsFile,
      ParserResult<T> entityParserResult,
      ContactsParserResult contactsParsed,
      int batchKey) {

    Path resultFile =
        Files.createTempFile(
            resultDirPath,
            "result-" + System.currentTimeMillis(),
            "." + entityParserResult.getFormat().name().toLowerCase());
    writeResult(
        entitiesFile, resultFile, getEntityFields(), entityParserResult, (v, h) -> v[h.get(CODE)]);

    List<Path> resultFiles = new ArrayList<>();
    resultFiles.add(resultFile);

    // contacts
    if (contactsFile != null && !contactsParsed.getParsedDataMap().isEmpty()) {
      Path contactsResultFile =
          Files.createTempFile(
              resultDirPath,
              "contacts-" + System.currentTimeMillis(),
              "." + contactsParsed.getFormat().name().toLowerCase());

      writeResult(
          contactsFile,
          contactsResultFile,
          FileFields.ContactFields.ALL_FIELDS,
          contactsParsed,
          (v, h) -> String.valueOf(FileParser.getContactUniqueKey(v, h)));

      resultFiles.add(contactsResultFile);
    }

    // zip both files
    Path zipFile =
        Files.createFile(
            resultDirPath.resolve(
                Paths.get("batchResult-" + batchKey + "-" + System.currentTimeMillis() + ".zip")));
    toZipFile(resultFiles, zipFile);

    for (Path path : resultFiles) {
      Files.deleteIfExists(path);
    }

    return zipFile;
  }

  private <R> void writeResult(
      byte[] entitiesFile,
      Path resultFile,
      List<String> entityFields,
      ParserResult<R> parserResult,
      BiFunction<String[], Map<String, Integer>, String> keyExtractor)
      throws IOException {

    // csv options
    CSVParser csvParser =
        new CSVParserBuilder().withSeparator(parserResult.getFormat().getDelimiter()).build();

    try (CSVReader csvReader =
            new CSVReaderBuilder(
                    new BufferedReader(
                        new InputStreamReader(new ByteArrayInputStream(entitiesFile))))
                .withCSVParser(csvParser)
                .build();
        CSVWriter csvWriter =
            new CSVWriter(
                new BufferedWriter(new FileWriter(resultFile.toFile())),
                parserResult.getFormat().getDelimiter(),
                ICSVWriter.DEFAULT_QUOTE_CHARACTER,
                ICSVWriter.DEFAULT_ESCAPE_CHARACTER,
                ICSVWriter.DEFAULT_LINE_END)) {

      // write headers in result file. We only use the headers used in the batch plus the key and
      // errors
      Set<String> sourceHeaders =
          entityFields.stream()
              .filter(f -> parserResult.getFileHeadersIndex().containsKey(f))
              .collect(Collectors.toCollection(LinkedHashSet::new));
      sourceHeaders.add(KEY);

      String[] resultHeaders =
          Arrays.copyOf(sourceHeaders.toArray(new String[0]), sourceHeaders.size() + 1);
      resultHeaders[resultHeaders.length - 1] = ERRORS;
      csvWriter.writeNext(resultHeaders);

      // skip header line
      csvReader.readNextSilently();

      String[] values;
      while ((values = csvReader.readNextSilently()) != null) {
        if (values.length == 0) {
          continue;
        }

        values = FileParsingUtils.normalizeValues(sourceHeaders.size(), values);

        // we get the key of the current entity from the specific column
        String keyValue = keyExtractor.apply(values, parserResult.getFileHeadersIndex());
        // we find the entity in the parsed data to write the generated values during the batch
        // processing such as the key and the errors
        ParsedData<R> parsingData = parserResult.getParsedDataMap().get(keyValue);

        if (parsingData == null) {
          log.warn("No parsed data found for key value {}", keyValue);
          continue;
        }

        String[] resultValues = new String[resultHeaders.length + 1];
        int i = 0;
        for (String sourceHeader : sourceHeaders) {
          if (sourceHeader.equals(KEY)) {
            String key = parserResult.getEntityKeyExtractor().apply(parsingData.getEntity());
            resultValues[i] = key != null ? key : "";
          } else {
            resultValues[i] = values[parserResult.getFileHeadersIndex().get(sourceHeader)];
          }
          i++;
        }

        // add errors
        if (parsingData.getErrors() != null) {
          resultValues[i] = String.join(FileParsingUtils.LIST_DELIMITER, parsingData.getErrors());
        }

        csvWriter.writeNext(resultValues);
      }
    }
  }

  @SneakyThrows
  private static void toZipFile(List<Path> filesToZip, Path targetFile) {
    try (FileOutputStream fos = new FileOutputStream(targetFile.toFile());
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

  abstract boolean allowedToCreateEntity(T entity, Authentication authentication);

  abstract boolean allowedToUpdateEntity(T entity, Authentication authentication);

  abstract List<String> getEntityFields();

  abstract ParsedData<T> createEntityFromValues(String[] values, Map<String, Integer> headersIndex);

  abstract List<UUID> findEntity(String code, List<Identifier> identifiers);
}
