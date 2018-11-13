package org.gbif.registry.collections;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.registry.ws.resources.collections.InstitutionResource;
import org.gbif.registry.ws.resources.collections.PersonResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.net.URI;
import java.util.Arrays;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.gbif.registry.guice.RegistryTestModules.webservice;
import static org.gbif.registry.guice.RegistryTestModules.webserviceClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class InstitutionIT extends BaseCollectionTest<Institution> {

  private static final String CODE = "code";
  private static final String NAME = "name";
  private static final String DESCRIPTION = "dummy description";
  private static final URI HOMEPAGE = URI.create("http://dummy");
  private static final String CODE_UPDATED = "code2";
  private static final String NAME_UPDATED = "name2";
  private static final String DESCRIPTION_UPDATED = "dummy description updated";
  private static final String ADDITIONAL_NAME = "additional name";

  private final InstitutionService institutionService;
  private final PersonService personService;

  @Parameters
  public static Iterable<Object[]> data() {
    final Injector client = webserviceClient();
    final Injector webservice = webservice();
    return ImmutableList.<Object[]>of(
        new Object[] {
          webservice.getInstance(InstitutionResource.class),
          webservice.getInstance(PersonResource.class),
          null
        },
        new Object[] {
          client.getInstance(InstitutionService.class),
          client.getInstance(PersonService.class),
          client.getInstance(SimplePrincipalProvider.class)
        });
  }

  public InstitutionIT(
      InstitutionService institutionService,
      PersonService personService,
      @Nullable SimplePrincipalProvider pp) {
    super(
        institutionService,
        institutionService,
        institutionService,
        institutionService,
        personService,
        pp);
    this.institutionService = institutionService;
    this.personService = personService;
  }

  @Test
  public void listWithoutParametersTest() {
    Institution institution1 = newEntity();
    UUID key1 = institutionService.create(institution1);

    Institution institution2 = newEntity();
    UUID key2 = institutionService.create(institution2);

    Institution institution3 = newEntity();
    UUID key3 = institutionService.create(institution3);

    PagingResponse<Institution> response = institutionService.list(null, null, PAGE.apply(5, 0L));
    assertEquals(3, response.getResults().size());

    institutionService.delete(key3);

    response = institutionService.list(null, null, PAGE.apply(5, 0L));
    assertEquals(2, response.getResults().size());

    response = institutionService.list(null, null, PAGE.apply(1, 0L));
    assertEquals(1, response.getResults().size());

    response = institutionService.list(null, null, PAGE.apply(0, 0L));
    assertEquals(0, response.getResults().size());
  }

  @Test
  public void listQueryTest() {
    Institution institution1 = newEntity();
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    institution1.setAddress(address);
    UUID key1 = institutionService.create(institution1);

    // add contact
    Person person1 = new Person();
    person1.setFirstName("first name");
    UUID personKey = personService.create(person1);
    institutionService.addContact(key1, personKey);

    Institution institution2 = newEntity();
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    institution2.setAddress(address2);
    UUID key2 = institutionService.create(institution2);

    Pageable page = PAGE.apply(5, 0L);
    PagingResponse<Institution> response = institutionService.list("dummy", null, page);
    assertEquals(2, response.getResults().size());

    response = institutionService.list("city", null, page);
    assertEquals(1, response.getResults().size());
    assertEquals(key1, response.getResults().get(0).getKey());

    response = institutionService.list("city2", null, page);
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());

    assertEquals(0, institutionService.list("c", null, page).getResults().size());

    // person search
    assertEquals(1, institutionService.list("first name", null, page).getResults().size());
    institutionService.removeContact(key1, personKey);
    assertEquals(0, institutionService.list("first name", null, page).getResults().size());

    institutionService.delete(key2);
    assertEquals(0, institutionService.list("city2", null, page).getResults().size());
  }

  @Test
  public void listByContactTest() {
    // persons
    Person person1 = new Person();
    person1.setFirstName("first name");
    UUID personKey1 = personService.create(person1);

    Person person2 = new Person();
    person2.setFirstName("first name2");
    UUID personKey2 = personService.create(person2);

    // institutions
    Institution institution1 = newEntity();
    UUID instutionKey1 = institutionService.create(institution1);

    Institution institution2 = newEntity();
    UUID instutionKey2 = institutionService.create(institution2);

    // add contacts
    institutionService.addContact(instutionKey1, personKey1);
    institutionService.addContact(instutionKey1, personKey2);
    institutionService.addContact(instutionKey2, personKey2);

    assertEquals(1, institutionService.list(null, personKey1, PAGE.apply(5, 0L)).getResults().size());
    assertEquals(2, institutionService.list(null, personKey2, PAGE.apply(5, 0L)).getResults().size());
    assertEquals(0, institutionService.list(null, UUID.randomUUID(), PAGE.apply(5, 0L)).getResults().size());

    institutionService.removeContact(instutionKey1, personKey2);
    assertEquals(1, institutionService.list(null, personKey2, PAGE.apply(5, 0L)).getResults().size());
  }

  @Override
  protected Institution newEntity() {
    Institution institution = new Institution();
    institution.setCode(CODE);
    institution.setName(NAME);
    institution.setDescription(DESCRIPTION);
    institution.setHomepage(HOMEPAGE);
    return institution;
  }

  @Override
  protected void assertNewEntity(Institution institution) {
    assertEquals(CODE, institution.getCode());
    assertEquals(NAME, institution.getName());
    assertEquals(DESCRIPTION, institution.getDescription());
    assertEquals(HOMEPAGE, institution.getHomepage());
    assertNull(institution.getAdditionalNames());
  }

  @Override
  protected Institution updateEntity(Institution institution) {
    institution.setCode(CODE_UPDATED);
    institution.setName(NAME_UPDATED);
    institution.setDescription(DESCRIPTION_UPDATED);
    institution.setAdditionalNames(Arrays.asList(ADDITIONAL_NAME));
    return institution;
  }

  @Override
  protected void assertUpdatedEntity(Institution entity) {
    assertEquals(CODE_UPDATED, entity.getCode());
    assertEquals(NAME_UPDATED, entity.getName());
    assertEquals(DESCRIPTION_UPDATED, entity.getDescription());
    assertEquals(1, entity.getAdditionalNames().size());
  }

  @Override
  protected Institution newInvalidEntity() {
    return new Institution();
  }
}
