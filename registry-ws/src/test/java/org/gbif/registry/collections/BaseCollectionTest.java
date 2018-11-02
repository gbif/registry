package org.gbif.registry.collections;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Staff;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.service.collections.StaffService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.TagService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Base class to tests the main operations of {@link CollectionEntity} that are also {@link
 * Taggable}, {@link Identifiable} and {@link Contactable}.
 *
 * <p>It inherits from {@link CrudTest} to test the CRUD operations.
 */
public abstract class BaseCollectionTest<T extends CollectionEntity & Taggable & Identifiable & Contactable>
    extends CrudTest<T> {

  private final CrudService<T> crudService;
  private final SimplePrincipalProvider pp;
  private final ContactService contactService;
  private final TagService tagService;
  private final IdentifierService identifierService;
  private final StaffService staffService;

  public BaseCollectionTest(
      CrudService<T> crudService,
      ContactService contactService,
      TagService tagService,
      IdentifierService identifierService,
      StaffService staffService,
      @Nullable SimplePrincipalProvider pp) {
    super(crudService, pp);
    this.crudService = crudService;
    this.contactService = contactService;
    this.tagService = tagService;
    this.identifierService = identifierService;
    this.staffService = staffService;
    this.pp = pp;
  }

  @Test
  public void createFullTest() {
    T entity = newEntity();

    Address address = new Address();
    address.setAddress("address");
    address.setCountry(Country.AFGHANISTAN);
    address.setCity("city");
    entity.setAddress(address);

    Address mailingAddress = new Address();
    mailingAddress.setAddress("mailing");
    entity.setMailingAddress(mailingAddress);

    Tag tag = new Tag();
    tag.setValue("value");
    entity.setTags(Arrays.asList(tag));

    Identifier identifier = new Identifier();
    identifier.setIdentifier("id");
    identifier.setType(IdentifierType.LSID);
    entity.setIdentifiers(Arrays.asList(identifier));

    UUID key = crudService.create(entity);
    T entitySaved = crudService.get(key);

    assertNotNull(entitySaved.getAddress());
    assertEquals("address", entitySaved.getAddress().getAddress());
    assertEquals(Country.AFGHANISTAN, entitySaved.getAddress().getCountry());
    assertNotNull(entitySaved.getMailingAddress());
    assertEquals("mailing", entitySaved.getMailingAddress().getAddress());
    assertEquals(1, entitySaved.getTags().size());
    assertEquals("value", entitySaved.getTags().get(0).getValue());
    assertEquals(1, entitySaved.getIdentifiers().size());
    assertEquals("id", entitySaved.getIdentifiers().get(0).getIdentifier());
    assertEquals(IdentifierType.LSID, entitySaved.getIdentifiers().get(0).getType());
  }

  @Test
  public void searchTest() {
    T entity1 = newEntity();
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    entity1.setAddress(address);
    UUID key1 = crudService.create(entity1);

    T entity2 = newEntity();
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    entity2.setAddress(address2);
    UUID key2 = crudService.create(entity2);

    Pageable page = PAGE.apply(5, 0L);
    PagingResponse<T> response = crudService.search("dummy", page);
    assertEquals(2, response.getResults().size());

    response = crudService.search("city", page);
    assertEquals(1, response.getResults().size());
    assertEquals(key1, response.getResults().get(0).getKey());

    response = crudService.search("city2", page);
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());

    assertEquals(0, crudService.search("c", page).getResults().size());

    crudService.delete(key2);
    assertEquals(0, crudService.search("city2", page).getResults().size());
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

  @Test
  public void contactsTest() {
    // entities
    T entity1 = newEntity();
    UUID entityKey1 = crudService.create(entity1);

    T entity2 = newEntity();
    UUID entityKey2 = crudService.create(entity2);

    T entity3 = newEntity();
    UUID entityKey3 = crudService.create(entity3);

    // contacts
    Staff staff1 = new Staff();
    staff1.setFirstName("name1");
    UUID staffKey1 = staffService.create(staff1);

    Staff staff2 = new Staff();
    staff2.setFirstName("name2");
    UUID staffKey2 = staffService.create(staff2);

    // add contacts
    contactService.addContact(entityKey1, staffKey1);
    contactService.addContact(entityKey1, staffKey2);
    contactService.addContact(entityKey2, staffKey2);

    // list contacts
    List<Staff> contactsEntity1 = contactService.listContacts(entityKey1);
    assertEquals(2, contactsEntity1.size());

    List<Staff> contactsEntity2 = contactService.listContacts(entityKey2);
    assertEquals(1, contactsEntity2.size());
    assertEquals("name2", contactsEntity2.get(0).getFirstName());

    assertEquals(0, contactService.listContacts(entityKey3).size());

    // remove contacts
    contactService.removeContact(entityKey1, staffKey2);
    contactsEntity1 = contactService.listContacts(entityKey1);
    assertEquals(1, contactsEntity1.size());
    assertEquals("name1", contactsEntity1.get(0).getFirstName());

    contactService.removeContact(entityKey2, staffKey2);
    assertEquals(0, contactService.listContacts(entityKey2).size());
  }
}
