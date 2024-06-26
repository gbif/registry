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
package org.gbif.registry.ws.it.collections.service.batch;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Batch;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.service.collections.BatchService;
import org.gbif.api.service.collections.CollectionEntityService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.service.collections.batch.FileFields;
import org.gbif.registry.service.collections.batch.FileParsingUtils;
import org.gbif.registry.ws.it.collections.service.BaseServiceIT;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class BaseBatchServiceIT<T extends CollectionEntity> extends BaseServiceIT {

  private final CollectionEntityService<T> entityService;
  private final BatchService batchService;
  private final CollectionEntityType entityType;

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  @Autowired
  public BaseBatchServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      CollectionEntityService<T> entityService,
      BatchService batchService,
      CollectionEntityType entityType) {
    super(simplePrincipalProvider);
    this.entityService = entityService;
    this.batchService = batchService;
    this.entityType = entityType;
  }

  @Test
  public void handleBatchTest() throws IOException, SQLException {
    Resource entitiesFile = getEntitiesResource();
    Resource contactsFile = getContactsResource();

    T existing = newInstance();
    existing.setKey(UUID.fromString("0b453274-d25e-4644-b860-327d5c54f173"));
    existing.setCode("c2");
    existing.setName("n2");
    existing.setDescription("descr2");
    existing.setEmail(Collections.singletonList("email1@test.com"));
    // this one is not included in the csv so it's removed in the batch update
    existing.setAlternativeCodes(Collections.singletonList(new AlternativeCode("foo", "boo")));

    // create entities
    persistDBEntities(existing);

    assertEquals(1, listAllEntities().size());

    int key =
        batchService.handleBatch(
            StreamUtils.copyToByteArray(entitiesFile.getInputStream()),
            StreamUtils.copyToByteArray(contactsFile.getInputStream()),
            ExportFormat.CSV);

    Batch batch = batchService.get(key);
    assertTrue(batch.getErrors().isEmpty());
    assertEquals(entityType, batch.getEntityType());
    assertEquals(Batch.State.FINISHED, batch.getState());

    List<T> entities = listAllEntities();
    assertEquals(2, entities.size());
    assertEquals(1, entities.stream().filter(e -> e.getContactPersons().size() == 1).count());
    assertEquals(1, entities.stream().filter(e -> e.getContactPersons().size() == 2).count());
    assertEquals(
        1,
        entities.stream()
            .filter(e -> e.getAddress() != null && e.getAddress().getCountry() == Country.SPAIN)
            .count());

    T updated = entityService.get(existing.getKey());
    assertEquals("descr,22", updated.getDescription());
    assertEquals(2, updated.getEmail().size());
    assertTrue(updated.isActive());
    assertEquals(1, updated.getAlternativeCodes().size());
    assertEquals("c22", updated.getAlternativeCodes().get(0).getCode());
    assertEquals("another code", updated.getAlternativeCodes().get(0).getDescription());
    assertEquals(License.CC0_1_0, updated.getFeaturedImageLicense());

    assertAddressContactsAndIdentifiers(updated);
    assertTrue(Files.exists(Paths.get(batch.getResultFilePath())));

    Files.delete(Paths.get(batch.getResultFilePath()));
  }

  @Test
  public void importBatchWithoutPermissionsTest() throws IOException, SQLException {
    Resource entitiesFile = getEntitiesResource();
    Resource contactsFile = getContactsResource();

    resetSecurityContext("test", UserRole.GRSCICOLL_MEDIATOR);

    T existing = newInstance();
    existing.setKey(UUID.fromString("0b453274-d25e-4644-b860-327d5c54f173"));
    existing.setCode("c2");
    existing.setName("n2");
    existing.setDescription("descr2");
    existing.setEmail(Collections.singletonList("email1@test.com"));
    // this one is not included in the csv so it's removed in the batch update
    existing.setAlternativeCodes(Collections.singletonList(new AlternativeCode("foo", "boo")));

    // create entities
    persistDBEntities(existing);

    assertEquals(1, listAllEntities().size());

    int key =
        batchService.handleBatch(
            StreamUtils.copyToByteArray(entitiesFile.getInputStream()),
            StreamUtils.copyToByteArray(contactsFile.getInputStream()),
            ExportFormat.CSV);

    Batch batch = batchService.get(key);
    assertTrue(batch.getErrors().isEmpty());
    assertEquals(entityType, batch.getEntityType());
    assertEquals(Batch.State.FINISHED, batch.getState());
    assertEquals(1, listAllEntities().size());

    T updated = entityService.get(existing.getKey());
    assertEquals(existing.getDescription(), updated.getDescription());
    assertEquals(existing.getEmail().size(), updated.getEmail().size());
    assertFalse(updated.isActive());
    assertEquals(1, updated.getAlternativeCodes().size());
    assertEquals(existing.getAlternativeCodes().get(0), updated.getAlternativeCodes().get(0));

    assertTrue(Files.exists(Paths.get(batch.getResultFilePath())));

    assertTrue(batch.getResultFilePath().contains("batchResult-" + batch.getKey() + "-"));
    List<Path> unzippedFiles =
        ZipUtils.unzip(Paths.get(batch.getResultFilePath()), "src/test/resources/collections");
    assertEquals(2, unzippedFiles.size());

    // csv options
    CSVParser csvParser =
        new CSVParserBuilder().withSeparator(ExportFormat.CSV.getDelimiter()).build();

    for (Path unzipped : unzippedFiles) {
      boolean isEntitiesFile = unzipped.getFileName().toString().contains("result-");
      try (CSVReader csvReader =
          new CSVReaderBuilder(new BufferedReader(new FileReader(unzipped.toFile())))
              .withCSVParser(csvParser)
              .build()) {
        List<String> headers = Arrays.asList(csvReader.readNextSilently());
        assertTrue(headers.contains(FileFields.CommonFields.KEY));
        assertTrue(headers.contains(FileFields.CommonFields.ERRORS));

        String[] values;
        while ((values = csvReader.readNextSilently()) != null) {
          values = FileParsingUtils.normalizeValues(headers.size(), values);
          String codeValue =
              headers.contains(FileFields.CommonFields.CODE)
                  ? values[headers.indexOf(FileFields.CommonFields.CODE)]
                  : null;
          String keyValue = values[headers.indexOf(FileFields.CommonFields.KEY)];
          if (existing.getCode().equals(codeValue)) {
            assertEquals(
                existing.getKey().toString(), values[headers.indexOf(FileFields.CommonFields.KEY)]);
          }

          if (isEntitiesFile) {
            assertNotNull(values[headers.indexOf(FileFields.CommonFields.ERRORS)]);
            assertTrue(
                values[headers.indexOf(FileFields.CommonFields.ERRORS)].contains(
                    " not allowed to "));
          } else {
            if (keyValue != null && !keyValue.isEmpty()) {
              assertEquals("-1", keyValue);
            }
          }
        }
      }
    }

    Files.delete(Paths.get(batch.getResultFilePath()));
  }

  private void assertAddressContactsAndIdentifiers(T entity) {
    assertEquals(2, entity.getIdentifiers().size());
    assertTrue(entity.getIdentifiers().stream().noneMatch(i -> i.getIdentifier().equals("foo")));

    assertEquals(2, entity.getContactPersons().size());
    assertTrue(
        entity.getContactPersons().stream().noneMatch(c -> c.getFirstName().equals("name-2")));
    Contact updatedContact = entity.getContactPersons().get(0);
    assertEquals("lastn2", updatedContact.getLastName());
    assertEquals(2, updatedContact.getEmail().size());
    assertEquals(2, updatedContact.getUserIds().size());
    Contact newContact = entity.getContactPersons().get(1);
    assertEquals("name2", newContact.getFirstName());
    assertEquals("lastn22", newContact.getLastName());
    assertEquals(0, newContact.getEmail().size());
    assertEquals(0, newContact.getUserIds().size());

    assertNull(entity.getAddress().getCountry());
    assertEquals("Avilés", entity.getAddress().getCity());
    assertEquals("234", entity.getAddress().getPostalCode());
    assertEquals("foo123", entity.getMailingAddress().getAddress());
  }

  @Test
  public void duplicateCodesTest() throws IOException {
    Resource duplicatesFile = new ClassPathResource("collections/duplicate_codes.csv");
    Resource contactsFile = new ClassPathResource("collections/institutions_contacts.csv");

    assertEquals(0, listAllEntities().size());

    int key =
        batchService.handleBatch(
            StreamUtils.copyToByteArray(duplicatesFile.getInputStream()),
            StreamUtils.copyToByteArray(contactsFile.getInputStream()),
            ExportFormat.CSV);

    List<T> entities = listAllEntities();
    assertEquals(0, entities.size());

    Batch batch = batchService.get(key);
    // 2 errors: unknown column and duplicate codes
    assertEquals(1, batch.getErrors().size());
    assertEquals(entityType, batch.getEntityType());
    assertEquals(Batch.State.FAILED, batch.getState());
    assertNull(batch.getResultFilePath());
  }

  @Test
  public void unknownColumnsTest() throws IOException {
    Resource unknownColumnFile = new ClassPathResource("collections/unknown_column.csv");
    Resource contactsFile = new ClassPathResource("collections/institutions_contacts.csv");

    assertEquals(0, listAllEntities().size());

    int key =
        batchService.handleBatch(
            StreamUtils.copyToByteArray(unknownColumnFile.getInputStream()),
            StreamUtils.copyToByteArray(contactsFile.getInputStream()),
            ExportFormat.CSV);

    List<T> entities = listAllEntities();
    assertEquals(1, entities.size());

    Batch batch = batchService.get(key);
    // 2 errors: unknown column and duplicate codes
    assertEquals(1, batch.getErrors().size());
    assertEquals(entityType, batch.getEntityType());
    assertEquals(Batch.State.FINISHED, batch.getState());

    Files.delete(Paths.get(batch.getResultFilePath()));
  }

  @Test
  public void wrongDelimiterTest() throws IOException {
    Resource unknownColumnFile = new ClassPathResource("collections/wrong_delimiter.csv");
    Resource contactsFile = new ClassPathResource("collections/institutions_contacts.csv");

    assertEquals(0, listAllEntities().size());

    int key =
        batchService.handleBatch(
            StreamUtils.copyToByteArray(unknownColumnFile.getInputStream()),
            StreamUtils.copyToByteArray(contactsFile.getInputStream()),
            ExportFormat.CSV);

    List<T> entities = listAllEntities();
    assertEquals(0, entities.size());

    Batch batch = batchService.get(key);
    // 2 errors: unknown columns in entities file plus no entities found
    assertEquals(2, batch.getErrors().size());
    assertEquals(entityType, batch.getEntityType());
    assertEquals(Batch.State.FAILED, batch.getState());
    assertNull(batch.getResultFilePath());
  }

  private void persistDBEntities(T entity) throws SQLException {
    Connection connection = PG_CONTAINER.createConnection("");

    Address address = new Address();
    address.setCountry(Country.SPAIN);
    address.setCity("Oviedo");
    entity.setAddress(address);

    connection
        .prepareStatement(
            "INSERT INTO address(key,city,country) VALUES(-1,'"
                + entity.getAddress().getCity()
                + "','"
                + entity.getAddress().getCountry().getIso2LetterCode()
                + "')")
        .executeUpdate();
    connection
        .prepareStatement(
            "INSERT INTO "
                + entityType.name().toLowerCase()
                + "(key,code,name,description,alternative_codes,email,address_key,created_by,modified_by) VALUES('"
                + entity.getKey()
                + "','"
                + entity.getCode()
                + "','"
                + entity.getName()
                + "','"
                + entity.getDescription()
                + "','"
                + entity.getAlternativeCodes().get(0).getCode()
                + " => "
                + entity.getAlternativeCodes().get(0).getDescription()
                + "','{\""
                + entity.getEmail()
                + "\"}',-1,'test','test')")
        .executeUpdate();

    Contact contact1 = new Contact();
    contact1.setKey(-1);
    contact1.setFirstName("name1");
    contact1.setLastName("lastn1");
    contact1.setPosition(Collections.singletonList("tester"));

    // this one is not included in the csv so it's removed in the batch update
    Contact contact2 = new Contact();
    contact2.setKey(-2);
    contact2.setFirstName("name-2");
    contact2.setLastName("lastn-2");
    contact2.setPosition(Collections.singletonList("tester"));

    // this one is not included in the csv so it's removed in the batch update
    Identifier identifier1 = new Identifier();
    identifier1.setType(IdentifierType.IH_IRN);
    identifier1.setIdentifier("foo");

    for (Contact contact : Arrays.asList(contact1, contact2)) {
      connection
          .prepareStatement(
              "INSERT INTO collection_contact(key,first_name,last_name,position,created_by,modified_by) VALUES('"
                  + contact.getKey()
                  + "','"
                  + contact.getFirstName()
                  + "','"
                  + contact.getLastName()
                  + "','{\""
                  + contact.getPosition().get(0)
                  + "\"}','test','test')")
          .executeUpdate();
      connection
          .prepareStatement(
              "INSERT INTO "
                  + entityType.name().toLowerCase()
                  + "_collection_contact("
                  + entityType.name().toLowerCase()
                  + "_key,collection_contact_key) VALUES('"
                  + entity.getKey()
                  + "','"
                  + contact.getKey()
                  + "')")
          .executeUpdate();
    }

    int key = 1;
    for (Identifier identifier : Arrays.asList(identifier1)) {
      connection
          .prepareStatement(
              "INSERT INTO identifier(key,type,identifier,created_by) VALUES("
                  + key
                  + ",'"
                  + identifier.getType()
                  + "','"
                  + identifier.getIdentifier()
                  + "','test')")
          .executeUpdate();
      connection
          .prepareStatement(
              "INSERT INTO "
                  + entityType.name().toLowerCase()
                  + "_identifier("
                  + entityType.name().toLowerCase()
                  + "_key,identifier_key) VALUES('"
                  + entity.getKey()
                  + "',"
                  + key
                  + ")")
          .executeUpdate();
      connection.close();
      key++;
    }
  }

  private ClassPathResource getContactsResource() {
    return new ClassPathResource(
        "collections/" + entityType.name().toLowerCase() + "s_contacts.csv");
  }

  private Resource getEntitiesResource() {
    Resource institutionsFile =
        new ClassPathResource("collections/" + entityType.name().toLowerCase() + "s.csv");
    return institutionsFile;
  }

  abstract List<T> listAllEntities();

  abstract T newInstance();

  abstract void assertUpdatedEntity(T updated);
}
