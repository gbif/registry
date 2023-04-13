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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.CODE;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.ERRORS;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.KEY;
import static org.gbif.registry.service.collections.batch.FileParser.parseContacts;
import static org.gbif.registry.service.collections.batch.FileParser.parseEntities;
import static org.gbif.registry.service.collections.batch.FileParsingUtils.splitLine;

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

        if (entity.getKey() == null) {
          Optional<UUID> key = createEntity(entity, parsedEntity);
          if (!key.isPresent()) {
            continue;
          }
        } else {
          updateEntity(entity, parsedEntity, parsingResult.getFileHeadersIndex());
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

      // csv
      Path resultPath =
          createResultFile(
              entitiesFile, contactsFile, parsingResult, contactsParsed, batch.getKey());

      // update batch
      batch.setState(Batch.State.FINISHED);
      batch.getErrors().addAll(parsingResult.getFileErrors());
      batch.getErrors().addAll(contactsParsed.getFileErrors());

      batch.setResultFilePath(resultPath.toFile().getAbsolutePath());
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

  private Optional<UUID> createEntity(T entity, ParsedData<T> parsedEntity) {
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
      return Optional.empty();
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
      return Optional.empty();
    }

    // create entity
    entity.setCreatedBy(authentication.getName());

    try {
      UUID key = entityService.create(entity);
      entity.setKey(key);
      return Optional.of(key);
    } catch (Exception ex) {
      parsedEntity
          .getErrors()
          .add("Couldn't create " + entityType.name().toLowerCase() + ": " + ex.getMessage());
    }

    return Optional.empty();
  }

  private void updateEntity(
      T entity, ParsedData<T> parsedEntity, Map<String, Integer> fileHeadersIndex)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    if (!entityService.exists(entity.getKey()) || entity.getKey() == null) {
      parsedEntity.getErrors().add(entityType.name().toLowerCase() + " doesn't exist");
      return;
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
      return;
    }

    // update entity
    T mergedEntity =
        mergeEntities(entityService.get(entity.getKey()), entity, fileHeadersIndex.keySet());
    mergedEntity.setModifiedBy(authentication.getName());

    try {
      entityService.update(mergedEntity);
    } catch (Exception ex) {
      parsedEntity
          .getErrors()
          .add("Couldn't update " + entityType.name().toLowerCase() + ": " + ex.getMessage());
    }
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

    try (BufferedReader br =
            new BufferedReader(new InputStreamReader(new ByteArrayInputStream(entitiesFile)));
        BufferedWriter bw = new BufferedWriter(new FileWriter(resultFile.toFile()))) {

      // write headers in result file. We only use the headers used in the batch plus the key and
      // errors
      Set<String> headersResult =
          entityFields.stream()
              .filter(f -> parserResult.getFileHeadersIndex().containsKey(f))
              .collect(Collectors.toCollection(LinkedHashSet::new));
      headersResult.add(KEY);

      String headersLine =
          headersResult.stream()
              .collect(Collectors.joining(parserResult.getFormat().getDelimiter().toString()));
      headersLine += parserResult.getFormat().getDelimiter().toString() + ERRORS;
      bw.write(headersLine);
      bw.newLine();

      // skip header line
      br.readLine();

      String line;
      while ((line = br.readLine()) != null) {
        if (line.isEmpty()) {
          continue;
        }

        String[] values = splitLine(parserResult.getFormat(), headersResult.size(), line);

        // we get the key of the current entity from the specific column
        String keyValue = keyExtractor.apply(values, parserResult.getFileHeadersIndex());
        // we find the entity in the parsed data to write the generated values during the batch
        // processing such as the key and the errors
        ParsedData<R> parsingData = parserResult.getParsedDataMap().get(keyValue);

        if (parsingData == null) {
          log.warn("No parsed data found for key value {}", keyValue);
          continue;
        }

        StringBuilder resultLine = new StringBuilder();
        for (String header : headersResult) {
          if (header.equals(KEY) && !parserResult.getFileHeadersIndex().containsKey(KEY)) {
            // case of initial imports
            Optional.ofNullable(parserResult.getEntityKeyExtractor().apply(parsingData.getEntity()))
                .ifPresent(resultLine::append);
          } else {
            resultLine.append(values[parserResult.getFileHeadersIndex().get(header)]);
          }
          resultLine.append(parserResult.getFormat().getDelimiter().toString());
        }

        // add errors
        Optional.ofNullable(parsingData.getErrors())
            .ifPresent(e -> resultLine.append(String.join(FileParsingUtils.LIST_DELIMITER, e)));

        bw.write(resultLine.toString());
        bw.newLine();
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
