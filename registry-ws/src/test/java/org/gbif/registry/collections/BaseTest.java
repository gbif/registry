package org.gbif.registry.collections;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.*;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.MachineTagService;
import org.gbif.api.service.registry.TagService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import javax.validation.ValidationException;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/** Base class to test the CRUD operations of {@link CollectionEntity}. */
public abstract class BaseTest<
    T extends CollectionEntity & Identifiable & Taggable & MachineTaggable> {

  protected static final BiFunction<Integer, Long, Pageable> PAGE =
      (limit, offset) ->
          new Pageable() {
            @Override
            public int getLimit() {
              return limit;
            }

            @Override
            public long getOffset() {
              return offset;
            }
          };

  private final CrudService<T> crudService;
  private final TagService tagService;
  private final MachineTagService machineTagService;
  private final IdentifierService identifierService;
  private final SimplePrincipalProvider pp;

  @ClassRule
  public static final LiquibaseInitializer liquibaseRule =
      new LiquibaseInitializer(LiquibaseModules.database());

  @ClassRule public static final RegistryServer registryServer = RegistryServer.INSTANCE;

  @Rule
  public final DatabaseInitializer databaseRule =
      new DatabaseInitializer(LiquibaseModules.database());

  public BaseTest(
      CrudService<T> crudService,
      TagService tagService,
      MachineTagService machineTagService,
      IdentifierService identifierService,
      @Nullable SimplePrincipalProvider pp) {
    this.crudService = crudService;
    this.tagService = tagService;
    this.machineTagService = machineTagService;
    this.identifierService = identifierService;
    this.pp = pp;
  }

  @Before
  public void setup() {
    if (pp != null) {
      pp.setPrincipal(TestConstants.TEST_GRSCICOLL_ADMIN);
    }
  }

  protected abstract T newEntity();

  protected abstract void assertNewEntity(T entity);

  protected abstract T updateEntity(T entity);

  protected abstract void assertUpdatedEntity(T entity);

  protected abstract T newInvalidEntity();

  @Test
  public void crudTest() {
    // create
    T entity = newEntity();

    UUID key = crudService.create(entity);
    assertNotNull(key);
    T entitySaved = crudService.get(key);
    assertEquals(key, entitySaved.getKey());
    assertNewEntity(entitySaved);
    assertNotNull(entitySaved.getCreatedBy());
    assertNotNull(entitySaved.getCreated());
    assertNotNull(entitySaved.getModifiedBy());
    assertNotNull(entitySaved.getModified());

    // update
    entity = updateEntity(entitySaved);
    crudService.update(entity);
    entitySaved = crudService.get(key);
    assertUpdatedEntity(entitySaved);

    // delete
    crudService.delete(key);
    entitySaved = crudService.get(key);
    assertNotNull(entitySaved.getDeleted());
  }

  @Test(expected = ValidationException.class)
  public void createInvalidEntityTest() {
    crudService.create(newInvalidEntity());
  }

  @Test
  public void deleteMissingEntityTest() {
    // does nothing
    crudService.delete(UUID.randomUUID());
  }

  @Test(expected = IllegalArgumentException.class)
  public void updateDeletedEntityTest() {
    T entity = newEntity();
    UUID key = crudService.create(entity);
    entity.setKey(key);
    crudService.delete(key);
    entity = crudService.get(key);
    assertNotNull(entity.getDeleted());
    crudService.update(entity);
  }

  @Test
  public void restoreDeletedEntityTest() {
    T entity = newEntity();
    UUID key = crudService.create(entity);
    entity.setKey(key);
    crudService.delete(key);
    entity = crudService.get(key);
    assertNotNull(entity.getDeleted());

    // restore it
    entity.setDeleted(null);
    crudService.update(entity);
    entity = crudService.get(key);
    assertNull(entity.getDeleted());
  }

  @Test(expected = ValidationException.class)
  public void updateInvalidEntityTest() {
    T entity = newEntity();
    UUID key = crudService.create(entity);
    entity = newInvalidEntity();
    entity.setKey(key);
    crudService.update(entity);
  }

  @Test
  public void getMissingEntity() {
    assertNull(crudService.get(UUID.randomUUID()));
  }

  @Test
  public void createTest() {
    T entity = newEntity();

    Address address = new Address();
    address.setAddress("address");
    address.setCountry(Country.AFGHANISTAN);
    address.setCity("city");

    Address mailingAddress = new Address();
    mailingAddress.setAddress("mailing");

    MachineTag machineTag = new MachineTag("ns", "name", "value");
    entity.setMachineTags(Collections.singletonList(machineTag));

    Tag tag = new Tag();
    tag.setValue("value");
    entity.setTags(Collections.singletonList(tag));

    Identifier identifier = new Identifier();
    identifier.setIdentifier("id");
    identifier.setType(IdentifierType.LSID);
    entity.setIdentifiers(Collections.singletonList(identifier));

    UUID key = crudService.create(entity);
    T entitySaved = crudService.get(key);

    assertEquals(1, entitySaved.getMachineTags().size());
    assertEquals("value", entitySaved.getMachineTags().get(0).getValue());
    assertEquals(1, entitySaved.getTags().size());
    assertEquals("value", entitySaved.getTags().get(0).getValue());
    assertEquals(1, entitySaved.getIdentifiers().size());
    assertEquals("id", entitySaved.getIdentifiers().get(0).getIdentifier());
    assertEquals(IdentifierType.LSID, entitySaved.getIdentifiers().get(0).getType());
  }

  @Test
  public void tagsTest() {
    T entity = newEntity();
    UUID key = crudService.create(entity);

    Tag tag = new Tag();
    tag.setValue("value");
    Integer tagKey = tagService.addTag(key, tag);

    List<Tag> tags = tagService.listTags(key, null);
    assertEquals(1, tags.size());
    assertEquals(tagKey, tags.get(0).getKey());
    assertEquals("value", tags.get(0).getValue());

    tagService.deleteTag(key, tagKey);
    assertEquals(0, tagService.listTags(key, null).size());
  }

  @Test
  public void machineTagsTest() {
    T entity = newEntity();
    UUID key = crudService.create(entity);

    MachineTag machineTag = new MachineTag("ns", "name", "value");
    Integer machineTagKey = machineTagService.addMachineTag(key, machineTag);

    List<MachineTag> machineTags = machineTagService.listMachineTags(key);
    assertEquals(1, machineTags.size());
    assertEquals(machineTagKey, machineTags.get(0).getKey());
    assertEquals("value", machineTags.get(0).getValue());

    machineTagService.deleteMachineTag(key, machineTagKey);
    assertEquals(0, machineTagService.listMachineTags(key).size());
  }

  @Test
  public void identifiersTest() {
    T entity = newEntity();
    UUID key = crudService.create(entity);

    Identifier identifier = new Identifier();
    identifier.setIdentifier("identifier");
    identifier.setType(IdentifierType.LSID);

    Integer identifierKey = identifierService.addIdentifier(key, identifier);

    List<Identifier> identifiers = identifierService.listIdentifiers(key);
    assertEquals(1, identifiers.size());
    assertEquals(identifierKey, identifiers.get(0).getKey());
    assertEquals("identifier", identifiers.get(0).getIdentifier());
    assertEquals(IdentifierType.LSID, identifiers.get(0).getType());

    identifierService.deleteIdentifier(key, identifierKey);
    assertEquals(0, identifierService.listIdentifiers(key).size());
  }
}
