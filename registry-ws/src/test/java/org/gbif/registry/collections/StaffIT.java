package org.gbif.registry.collections;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Staff;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.StaffService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.ws.resources.collections.CollectionResource;
import org.gbif.registry.ws.resources.collections.InstitutionResource;
import org.gbif.registry.ws.resources.collections.StaffResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

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
import static org.junit.Assert.assertNotNull;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class StaffIT extends CrudTest<Staff> {

  private static final String FIRST_NAME = "first name";
  private static final String LAST_NAME = "last name";
  private static final String POSITION = "position";
  private static final String PHONE = "134235435";
  private static final String EMAIL = "dummy@dummy.com";

  private static final String FIRST_NAME_UPDATED = "first name updated";
  private static final String POSITION_UPDATED = "new position";
  private static final String PHONE_UPDATED = "134235433";

  private final StaffService staffService;
  private final InstitutionService institutionService;
  private final CollectionService collectionService;

  @Parameters
  public static Iterable<Object[]> data() {
    final Injector client = webserviceClient();
    final Injector webservice = webservice();
    return ImmutableList.<Object[]>of(
        new Object[] {
          webservice.getInstance(StaffResource.class),
          webservice.getInstance(InstitutionResource.class),
          webservice.getInstance(CollectionResource.class),
          null
        },
        new Object[] {
          client.getInstance(StaffService.class),
          client.getInstance(InstitutionService.class),
          client.getInstance(CollectionService.class),
          client.getInstance(SimplePrincipalProvider.class)
        });
  }

  public StaffIT(
      StaffService staffService,
      InstitutionService institutionService,
      CollectionService collectionService,
      @Nullable SimplePrincipalProvider pp) {
    super(staffService, pp);
    this.staffService = staffService;
    this.institutionService = institutionService;
    this.collectionService = collectionService;
  }

  @Test
  public void createWithAddressTest() {
    Staff staff = newEntity();

    Address mailingAddress = new Address();
    mailingAddress.setAddress("mailing");
    mailingAddress.setCity("city");
    mailingAddress.setCountry(Country.AFGHANISTAN);
    staff.setMailingAddress(mailingAddress);

    UUID key = staffService.create(staff);
    Staff staffSaved = staffService.get(key);

    assertNewEntity(staff);
    assertNotNull(staffSaved.getMailingAddress());
    assertEquals("mailing", staffSaved.getMailingAddress().getAddress());
    assertEquals("city", staffSaved.getMailingAddress().getCity());
    assertEquals(Country.AFGHANISTAN, staffSaved.getMailingAddress().getCountry());
  }

  @Test
  public void searchTest() {
    Staff staff1 = newEntity();
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    staff1.setMailingAddress(address);
    UUID key1 = staffService.create(staff1);

    Staff staff2 = newEntity();
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    staff2.setMailingAddress(address2);
    UUID key2 = staffService.create(staff2);

    Pageable page = PAGE.apply(5, 0L);
    PagingResponse<Staff> response = staffService.search("dummy", page);
    assertEquals(2, response.getResults().size());

    response = staffService.search("city", page);
    assertEquals(1, response.getResults().size());
    assertEquals(key1, response.getResults().get(0).getKey());

    response = staffService.search("city2", page);
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());

    assertEquals(0, staffService.search("c", page).getResults().size());
  }

  @Test
  public void listByInstitutionTest() {
    // institutions
    Institution institution1 = new Institution();
    institution1.setCode("code1");
    institution1.setName("name1");
    UUID institutionKey1 = institutionService.create(institution1);

    Institution institution2 = new Institution();
    institution2.setCode("code2");
    institution2.setName("name2");
    UUID institutionKey2 = institutionService.create(institution2);

    // staff
    Staff staff1 = newEntity();
    staff1.setInstitutionKey(institutionKey1);
    UUID key1 = staffService.create(staff1);

    Staff staff2 = newEntity();
    staff2.setInstitutionKey(institutionKey1);
    UUID key2 = staffService.create(staff2);

    Staff staff3 = newEntity();
    staff3.setInstitutionKey(institutionKey2);
    UUID key3 = staffService.create(staff3);

    PagingResponse<Staff> response =
        staffService.listByInstitution(institutionKey1, PAGE.apply(5, 0L));
    assertEquals(2, response.getResults().size());

    response = staffService.listByInstitution(institutionKey2, PAGE.apply(2, 0L));
    assertEquals(1, response.getResults().size());

    response = staffService.listByInstitution(UUID.randomUUID(), PAGE.apply(2, 0L));
    assertEquals(0, response.getResults().size());
  }

  @Test
  public void listByCollectionTest() {
    // collections
    Collection collection1 = new Collection();
    collection1.setCode("code1");
    collection1.setName("name1");
    UUID collectionKey1 = collectionService.create(collection1);

    Collection collection2 = new Collection();
    collection2.setCode("code2");
    collection2.setName("name2");
    UUID collectionKey2 = collectionService.create(collection2);

    // staff
    Staff staff1 = newEntity();
    staff1.setCollectionKey(collectionKey1);
    UUID key1 = staffService.create(staff1);

    Staff staff2 = newEntity();
    staff2.setCollectionKey(collectionKey1);
    UUID key2 = staffService.create(staff2);

    Staff staff3 = newEntity();
    staff3.setCollectionKey(collectionKey2);
    UUID key3 = staffService.create(staff3);

    PagingResponse<Staff> response =
      staffService.listByCollection(collectionKey1, PAGE.apply(5, 0L));
    assertEquals(2, response.getResults().size());

    response = staffService.listByCollection(collectionKey2, PAGE.apply(2, 0L));
    assertEquals(1, response.getResults().size());

    response = staffService.listByCollection(UUID.randomUUID(), PAGE.apply(2, 0L));
    assertEquals(0, response.getResults().size());
  }

  @Override
  protected Staff newEntity() {
    Staff staff = new Staff();
    staff.setFirstName(FIRST_NAME);
    staff.setLastName(LAST_NAME);
    staff.setPosition(POSITION);
    staff.setPhone(PHONE);
    staff.setEmail(EMAIL);
    return staff;
  }

  @Override
  protected void assertNewEntity(Staff staff) {
    assertEquals(FIRST_NAME, staff.getFirstName());
    assertEquals(LAST_NAME, staff.getLastName());
    assertEquals(POSITION, staff.getPosition());
    assertEquals(PHONE, staff.getPhone());
    assertEquals(EMAIL, staff.getEmail());
  }

  @Override
  protected Staff updateEntity(Staff staff) {
    staff.setFirstName(FIRST_NAME_UPDATED);
    staff.setPosition(POSITION_UPDATED);
    staff.setPhone(PHONE_UPDATED);
    return staff;
  }

  @Override
  protected void assertUpdatedEntity(Staff staff) {
    assertEquals(FIRST_NAME_UPDATED, staff.getFirstName());
    assertEquals(LAST_NAME, staff.getLastName());
    assertEquals(POSITION_UPDATED, staff.getPosition());
    assertEquals(PHONE_UPDATED, staff.getPhone());
    assertEquals(EMAIL, staff.getEmail());
  }
}
