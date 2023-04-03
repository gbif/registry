package org.gbif.registry.ws.it.collections.service.batch;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Batch;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.Identifier;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.service.collections.BatchService;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.collections.InstitutionType;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.ws.it.collections.service.BaseServiceIT;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BatchServiceIT extends BaseServiceIT {

  private final InstitutionService institutionService;
  private final CollectionService collectionService;
  private final BatchService batchService;

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  @Autowired
  public BatchServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      InstitutionService institutionService,
      CollectionService collectionService,
      BatchService batchService) {
    super(simplePrincipalProvider);
    this.institutionService = institutionService;
    this.collectionService = collectionService;
    this.batchService = batchService;
  }

  @Test
  public void importInstitutionsBatchTest() throws IOException {
    Resource institutionsFile = new ClassPathResource("collections/institution_import.csv");
    Resource contactsFile = new ClassPathResource("collections/institution_contacts_import.csv");

    assertEquals(0, institutionService.list(InstitutionSearchRequest.builder().build()).getCount());

    int key =
        batchService.handleBatchAsync(
            StreamUtils.copyToByteArray(institutionsFile.getInputStream()),
            StreamUtils.copyToByteArray(contactsFile.getInputStream()),
            ExportFormat.CSV,
            false,
            CollectionEntityType.INSTITUTION);

    Batch batch = batchService.get(key);
    assertTrue(batch.getErrors().isEmpty());
    assertEquals(Batch.Operation.CREATE, batch.getOperation());
    assertEquals(CollectionEntityType.INSTITUTION, batch.getEntityType());
    assertEquals(Batch.State.SUCCESSFUL, batch.getState());

    PagingResponse<Institution> institutions =
        institutionService.list(InstitutionSearchRequest.builder().build());
    assertEquals(1, institutions.getCount());
    assertEquals(1, institutions.getResults().get(0).getContactPersons().size());
    assertEquals(Country.SPAIN, institutions.getResults().get(0).getAddress().getCountry());

    Files.delete(Paths.get(batch.getResultFilePath()));
  }

  @Test
  public void importCollectionsBatchTest() throws IOException {
    Resource collectionsFile = new ClassPathResource("collections/collection_import.csv");
    Resource contactsFile = new ClassPathResource("collections/collection_contacts_import.csv");

    assertEquals(0, collectionService.list(CollectionSearchRequest.builder().build()).getCount());

    int key =
        batchService.handleBatchAsync(
            StreamUtils.copyToByteArray(collectionsFile.getInputStream()),
            StreamUtils.copyToByteArray(contactsFile.getInputStream()),
            ExportFormat.CSV,
            false,
            CollectionEntityType.COLLECTION);

    Batch batch = batchService.get(key);
    assertTrue(batch.getErrors().isEmpty());
    assertEquals(Batch.Operation.CREATE, batch.getOperation());
    assertEquals(CollectionEntityType.COLLECTION, batch.getEntityType());
    assertEquals(Batch.State.SUCCESSFUL, batch.getState());

    PagingResponse<CollectionView> collections =
        collectionService.list(CollectionSearchRequest.builder().build());
    assertEquals(1, collections.getCount());
    assertEquals(1, collections.getResults().get(0).getCollection().getContactPersons().size());

    Files.delete(Paths.get(batch.getResultFilePath()));
  }

  @Test
  public void updateInstitutionsBatchTest() throws SQLException, IOException {
    Institution existing = new Institution();
    existing.setKey(UUID.fromString("0b453274-d25e-4644-b860-327d5c54f173"));
    existing.setCode("c1");
    existing.setName("n1");
    existing.setDescription("descr1");
    existing.setEmail(Collections.singletonList("email1@test.com"));
    // this one is not included in the csv so it's removed in the batch update
    existing.setAlternativeCodes(Collections.singletonList(new AlternativeCode("foo", "boo")));

    // create entities
    persistDBEntities(existing, CollectionEntityType.INSTITUTION, existing.getEmail().get(0));

    Resource institutionsFile = new ClassPathResource("collections/institutions_update.csv");
    Resource contactsFile = new ClassPathResource("collections/institution_contacts_update.csv");

    int key =
        batchService.handleBatchAsync(
            StreamUtils.copyToByteArray(institutionsFile.getInputStream()),
            StreamUtils.copyToByteArray(contactsFile.getInputStream()),
            ExportFormat.CSV,
            true,
            CollectionEntityType.INSTITUTION);

    Batch batch = batchService.get(key);
    assertTrue(batch.getErrors().isEmpty());
    assertEquals(Batch.Operation.UPDATE, batch.getOperation());
    assertEquals(CollectionEntityType.INSTITUTION, batch.getEntityType());
    assertEquals(Batch.State.SUCCESSFUL, batch.getState());

    Institution updated = institutionService.get(existing.getKey());
    assertEquals("descr2", updated.getDescription());
    assertEquals(2, updated.getEmail().size());
    assertTrue(updated.isActive());
    assertEquals(1, updated.getAlternativeCodes().size());
    assertEquals("c11", updated.getAlternativeCodes().get(0).getCode());
    assertEquals("another code", updated.getAlternativeCodes().get(0).getDescription());
    assertEquals(InstitutionType.HERBARIUM, updated.getType());
    assertEquals(2, updated.getDisciplines().size());

    assertAddressContactsAndIdentifiers(updated);

    assertTrue(Files.exists(Paths.get(batch.getResultFilePath())));

    Files.delete(Paths.get(batch.getResultFilePath()));
  }

  @Test
  public void updateCollectionsBatchTest() throws SQLException, IOException {
    Collection existing = new Collection();
    existing.setKey(UUID.fromString("0b453274-d25e-4644-b860-327d5c54f173"));
    existing.setCode("c1");
    existing.setName("n1");
    existing.setDescription("descr1");
    existing.setEmail(Collections.singletonList("email1@test.com"));
    // this one is not included in the csv so it's removed in the batch update
    existing.setAlternativeCodes(Collections.singletonList(new AlternativeCode("foo", "boo")));

    // create entities
    persistDBEntities(existing, CollectionEntityType.COLLECTION, existing.getEmail().get(0));

    Resource institutionsFile = new ClassPathResource("collections/collections_update.csv");
    Resource contactsFile = new ClassPathResource("collections/collection_contacts_update.csv");

    int key =
        batchService.handleBatchAsync(
            StreamUtils.copyToByteArray(institutionsFile.getInputStream()),
            StreamUtils.copyToByteArray(contactsFile.getInputStream()),
            ExportFormat.CSV,
            true,
            CollectionEntityType.COLLECTION);

    Batch batch = batchService.get(key);
    assertTrue(batch.getErrors().isEmpty());
    assertEquals(Batch.Operation.UPDATE, batch.getOperation());
    assertEquals(CollectionEntityType.COLLECTION, batch.getEntityType());
    assertEquals(Batch.State.SUCCESSFUL, batch.getState());

    Collection updated = collectionService.get(existing.getKey());
    assertEquals("descr2", updated.getDescription());
    assertEquals(2, updated.getEmail().size());
    assertTrue(updated.isActive());
    assertEquals(1, updated.getAlternativeCodes().size());
    assertEquals("c11", updated.getAlternativeCodes().get(0).getCode());
    assertEquals("another code", updated.getAlternativeCodes().get(0).getDescription());
    assertEquals(2, updated.getContentTypes().size());

    assertAddressContactsAndIdentifiers(updated);

    assertTrue(Files.exists(Paths.get(batch.getResultFilePath())));

    Files.delete(Paths.get(batch.getResultFilePath()));
  }

  private <T extends Contactable & Identifiable> void assertAddressContactsAndIdentifiers(
      T entity) {
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
    Resource contactsFile = new ClassPathResource("collections/institution_contacts_import.csv");

    assertEquals(0, institutionService.list(InstitutionSearchRequest.builder().build()).getCount());

    int key =
        batchService.handleBatchAsync(
            StreamUtils.copyToByteArray(duplicatesFile.getInputStream()),
            StreamUtils.copyToByteArray(contactsFile.getInputStream()),
            ExportFormat.CSV,
            false,
            CollectionEntityType.INSTITUTION);

    PagingResponse<Institution> institutions =
        institutionService.list(InstitutionSearchRequest.builder().build());
    assertEquals(0, institutions.getCount());

    Batch batch = batchService.get(key);
    // 2 errors: unknown column and duplicate codes
    assertEquals(1, batch.getErrors().size());
    assertEquals(Batch.Operation.CREATE, batch.getOperation());
    assertEquals(CollectionEntityType.INSTITUTION, batch.getEntityType());
    assertEquals(Batch.State.FAILED, batch.getState());
    assertNull(batch.getResultFilePath());
  }

  @Test
  public void unknownColumnsTest() throws IOException {
    Resource unknownColumnFile = new ClassPathResource("collections/unknown_column.csv");
    Resource contactsFile = new ClassPathResource("collections/institution_contacts_import.csv");

    assertEquals(0, institutionService.list(InstitutionSearchRequest.builder().build()).getCount());

    int key =
        batchService.handleBatchAsync(
            StreamUtils.copyToByteArray(unknownColumnFile.getInputStream()),
            StreamUtils.copyToByteArray(contactsFile.getInputStream()),
            ExportFormat.CSV,
            false,
            CollectionEntityType.INSTITUTION);

    PagingResponse<Institution> institutions =
        institutionService.list(InstitutionSearchRequest.builder().build());
    assertEquals(1, institutions.getCount());

    Batch batch = batchService.get(key);
    // 2 errors: unknown column and duplicate codes
    assertEquals(1, batch.getErrors().size());
    assertEquals(Batch.Operation.CREATE, batch.getOperation());
    assertEquals(CollectionEntityType.INSTITUTION, batch.getEntityType());
    assertEquals(Batch.State.SUCCESSFUL, batch.getState());

    Files.delete(Paths.get(batch.getResultFilePath()));
  }

  private static <T extends CollectionEntity & Contactable> void persistDBEntities(
      T entity, CollectionEntityType entityType, String entityEmail) throws SQLException {
    Connection connection = PG_CONTAINER.createConnection("");

    Address address = new Address();
    address.setCountry(Country.SPAIN);
    address.setCity("Oviedo");
    entity.setAddress(address);

    connection
        .prepareStatement(
            "INSERT INTO address(key,city,country) VALUES(1,'"
                + entity.getAddress().getCity()
                + "','"
                + entity.getAddress().getCountry().getIso2LetterCode()
                + "')")
        .executeUpdate();
    connection
        .prepareStatement(
            "INSERT INTO "
                + entityType.name().toLowerCase()
                + "(key,code,name,description,email,address_key,created_by,modified_by) VALUES('"
                + entity.getKey()
                + "','"
                + entity.getCode()
                + "','"
                + entity.getName()
                + "','"
                + entity.getDescription()
                + "','{\""
                + entityEmail
                + "\"}',1,'test','test')")
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
}
