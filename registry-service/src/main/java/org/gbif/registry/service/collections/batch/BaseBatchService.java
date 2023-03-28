package org.gbif.registry.service.collections.batch;

import org.gbif.api.model.collections.Batch;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.BatchService;
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.registry.persistence.mapper.collections.BatchMapper;
import org.gbif.registry.service.collections.batch.FileFields.ContactFields;
import org.gbif.registry.service.collections.batch.model.ContactsParserResult;
import org.gbif.registry.service.collections.batch.model.ParsedData;
import org.gbif.registry.service.collections.batch.model.ParserResult;

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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import lombok.SneakyThrows;

import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.CODE;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.ERRORS;
import static org.gbif.registry.service.collections.batch.FileFields.CommonFields.KEY;
import static org.gbif.registry.service.collections.batch.FileParser.parseContacts;
import static org.gbif.registry.service.collections.batch.FileParser.parseEntities;

public abstract class BaseBatchService<T extends CollectionEntity> implements BatchService {

  private final BatchMapper batchMapper;
  private final CollectionEntityService<T> entityService;
  private final Path resultDirPath;
  private final CollectionEntityType entityType;
  private final Class<T> clazz;

  BaseBatchService(
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

  @Override
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

  @Override
  public Batch get(int key) {
    return batchMapper.get(key);
  }

  @SneakyThrows
  void importBatch(Path entitiesPath, Path contactsPath, ExportFormat format, Batch batch) {

    ParserResult<T> parsingResult =
        parseEntities(
            entitiesPath,
            format,
            this::createEntityFromValues,
            CollectionEntity::getCode,
            entityType);

    if (!parsingResult.getDuplicates().isEmpty()) {
      batch.setSuccessful(false);
      batch
          .getFileErrors()
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
            contactsPath,
            parsingResult.getParsedDataMap(),
            format,
            ContactFields.getEntityCode(entityType));

    for (ParsedData<T> parsedEntity : parsingResult.getParsedDataMap().values()) {
      T entity = parsedEntity.getEntity();

      List<UUID> existingEntities = findEntity(entity.getCode(), entity.getIdentifiers());
      if (!existingEntities.isEmpty()) {
        parsedEntity
            .getErrors()
            .add(
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
        parsedEntity
            .getErrors()
            .add("Couldn't create " + entityType.name().toLowerCase() + ": " + ex.getMessage());
      }

      if (key == null) {
        continue;
      }

      // add identifiers
      entity
          .getIdentifiers()
          .forEach(i -> addIdentifier(entity.getKey(), i, parsedEntity.getErrors()));

      // add contacts
      for (ParsedData<Contact> parsedContact :
          contactsParsed.getContactsByEntity().get(entity.getCode())) {
        try {
          entityService.addContactPerson(key, parsedContact.getEntity());
        } catch (Exception ex) {
          parsedContact.getErrors().add("Couldn't add contact: " + ex.getMessage());
        }
      }
    }

    // csv
    Path resultPath =
        createResultFile(entitiesPath, contactsPath, parsingResult, contactsParsed, CODE);

    // update batch
    batch.setSuccessful(true);
    batch.getFileErrors().addAll(parsingResult.getFileErrors());
    batch.getFileErrors().addAll(contactsParsed.getFileErrors());

    if (!contactsParsed.getDuplicates().isEmpty()) {
      batch
          .getFileErrors()
          .add(
              "Duplicate contact keys: "
                  + contactsParsed.getDuplicates().stream()
                      .map(Object::toString)
                      .collect(Collectors.joining(",")));
    }

    batch.setResultFilePath(resultPath.toFile().getAbsolutePath());
    batchMapper.update(batch);
  }

  @SneakyThrows
  void updateBatch(Path entitiesPath, Path contactsPath, ExportFormat format, Batch batch) {
    ParserResult<T> parsedEntities =
        parseEntities(
            entitiesPath,
            format,
            this::createEntityFromValues,
            i -> i.getKey().toString(),
            CollectionEntityType.COLLECTION);

    if (!parsedEntities.getDuplicates().isEmpty()) {
      batch.setSuccessful(false);
      batch
          .getFileErrors()
          .add(
              "Duplicate "
                  + entityType.name().toLowerCase()
                  + " keys: "
                  + String.join(",", parsedEntities.getDuplicates()));
      batchMapper.update(batch);
      return;
    }

    ContactsParserResult contactsParsed =
        parseContacts(
            contactsPath,
            parsedEntities.getParsedDataMap(),
            format,
            ContactFields.getEntityKey(entityType));

    for (ParsedData<T> parsedEntity : parsedEntities.getParsedDataMap().values()) {
      T entity = parsedEntity.getEntity();
      if (!entityService.exists(entity.getKey()) || entity.getKey() == null) {
        parsedEntity.getErrors().add(entityType.name().toLowerCase() + " doesn't exist");
        continue;
      }

      // update entity
      T mergedEntity =
          mergeEntities(
              entityService.get(entity.getKey()),
              entity,
              parsedEntities.getFileHeadersIndex().keySet());

      try {
        entityService.update(mergedEntity);
      } catch (Exception ex) {
        parsedEntity
            .getErrors()
            .add("Couldn't update " + entityType.name().toLowerCase() + ": " + ex.getMessage());
      }

      List<Identifier> existingIdentifiers = entityService.listIdentifiers(entity.getKey());

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

      // create new identifiers
      entity.getIdentifiers().stream()
          .filter(i -> !containsIdentifier(existingIdentifiers, i))
          .forEach(i -> addIdentifier(entity.getKey(), i, parsedEntity.getErrors()));

      // delete contacts
      List<Contact> existingContacts = entityService.listContactPersons(entity.getKey());
      java.util.Collection<ParsedData<Contact>> parsingDataContacts =
          contactsParsed.getParsedDataMap().values();
      for (Contact existing : existingContacts) {
        if (!containsContact(parsingDataContacts, existing)) {
          try {
            entityService.removeContactPerson(entity.getKey(), existing.getKey());
          } catch (Exception ex) {
            parsedEntity.getErrors().add("Couldn't remove contact: " + ex.getMessage());
          }
        }
      }

      // update contacts
      for (ParsedData<Contact> contact : parsingDataContacts) {
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

    // csv
    Path resultPath =
        createResultFile(entitiesPath, contactsPath, parsedEntities, contactsParsed, KEY);

    // update batch
    batch.setSuccessful(true);
    batch.getFileErrors().addAll(parsedEntities.getFileErrors());
    batch.getFileErrors().addAll(contactsParsed.getFileErrors());
    batch.setResultFilePath(resultPath.toFile().getAbsolutePath());
    batchMapper.update(batch);
  }

  @SneakyThrows
  @VisibleForTesting
  public T mergeEntities(T existing, T parsed, Set<String> headers) {
    for (String h : getEntityFields()) {
      if (headers.contains(h)) {
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
      java.util.Collection<ParsedData<Contact>> contactList, Contact contact) {
    return contactList.stream()
        .map(ParsedData::getEntity)
        .anyMatch(c -> c.getKey() != null && c.getKey().equals(contact.getKey()));
  }

  @SneakyThrows
  @VisibleForTesting
  public Path createResultFile(
      Path entitiesPath,
      Path contactsPath,
      ParserResult<T> entityParserResult,
      ContactsParserResult contactsParsed,
      String keyColumn) {

    Path resultFile =
        Files.createTempFile(
            resultDirPath,
            "result-" + System.currentTimeMillis(),
            "." + entityParserResult.getFormat().name().toLowerCase());
    writeResult(
        entitiesPath,
        resultFile,
        getEntityFields(),
        entityParserResult,
        (v, h) -> v[h.get(keyColumn)]);

    List<Path> resultFiles = new ArrayList<>();
    resultFiles.add(resultFile);

    // contacts
    if (contactsPath != null) {
      Path contactsResultFile =
          Files.createTempFile(
              resultDirPath,
              "contacts-" + System.currentTimeMillis(),
              "." + contactsParsed.getFormat().name().toLowerCase());

      writeResult(
          contactsPath,
          contactsResultFile,
          ContactFields.ALL_FIELDS,
          contactsParsed,
          (v, h) -> String.valueOf(FileParser.getContactUniqueKey(v, h)));

      resultFiles.add(contactsResultFile);
    }

    // zip both files
    Path zipFile =
        Files.createFile(
            resultDirPath.resolve(Paths.get("batchResult-" + System.currentTimeMillis() + ".zip")));
    toZipFile(resultFiles, zipFile);

    for (Path path : resultFiles) {
      Files.deleteIfExists(path);
    }

    return zipFile;
  }

  @SneakyThrows
  private <R> void writeResult(
      Path entitiesFile,
      Path resultFile,
      List<String> entityFields,
      ParserResult<R> parserResult,
      BiFunction<String[], Map<String, Integer>, String> keyExtractor) {
    try (BufferedReader br = new BufferedReader(new FileReader(entitiesFile.toFile()));
        BufferedWriter bw = new BufferedWriter(new FileWriter(resultFile.toFile()))) {

      // write headers in result file. We only use the headers used in the batch plus the key and
      // errors
      Set<String> headersResult =
          entityFields.stream()
              .filter(f -> parserResult.getFileHeadersIndex().containsKey(f))
              .collect(Collectors.toSet());
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
        String[] values = line.split(parserResult.getFormat().getDelimiter().toString());

        String keyValue = keyExtractor.apply(values, parserResult.getFileHeadersIndex());
        ParsedData<R> parsingData = parserResult.getParsedDataMap().get(keyValue);

        StringBuilder resultLine = new StringBuilder();
        for (String header : headersResult) {
          if (header.equals(KEY) && !parserResult.getFileHeadersIndex().containsKey(KEY)) {
            // case of initial imports
            resultLine.append(parserResult.getEntityKeyExtractor().apply(parsingData.getEntity()));
          } else {
            resultLine.append(values[parserResult.getFileHeadersIndex().get(header)]);
          }
          resultLine.append(parserResult.getFormat().getDelimiter().toString());
        }

        // add errors
        resultLine.append(String.join(FileParsingUtils.LIST_DELIMITER, parsingData.getErrors()));

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

  abstract List<String> getEntityFields();

  abstract ParsedData<T> createEntityFromValues(String[] values, Map<String, Integer> headersIndex);

  abstract List<UUID> findEntity(String code, List<Identifier> identifiers);
}
