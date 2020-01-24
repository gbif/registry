package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.collections.InstitutionType;
import org.gbif.registry.collections.sync.ih.IHHttpClient;
import org.gbif.registry.collections.sync.ih.IHInstitution;
import org.gbif.registry.collections.sync.ih.IHStaff;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Tests the {@link EntityConverter}. */
public class EntityConverterTest {

  private static final String IRN_TEST = "1";
  private static final EntityConverter entityConverter =
      EntityConverter.builder()
          .countries(Arrays.asList("U.K.", "U.S.A.", "United Kingdom", "United States"))
          .creationUser("test-user")
          .build();

  @Test
  public void institutionConversionFromExistingTest() {
    // Existing
    Institution existing = new Institution();
    existing.setCode("UARK");
    existing.setName("University of Arkansas OLD");
    existing.setType(InstitutionType.OTHER_INSTITUTIONAL_TYPE);
    existing.setLatitude(new BigDecimal("36.0424"));
    existing.setLongitude(new BigDecimal("-94.1624"));
    existing.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, Utils.encodeIRN(IRN_TEST)));

    Address address = new Address();
    address.setCity("foo");
    address.setProvince("Arkansas");
    address.setCountry(Country.UNITED_STATES);
    existing.setMailingAddress(address);

    // IH Institution
    IHInstitution ih = getIhInstitution();

    // Expected
    Institution expected = new Institution();
    expected.setCode("UARK");
    expected.setName("University of Arkansas");
    expected.setType(InstitutionType.OTHER_INSTITUTIONAL_TYPE);
    expected.setIndexHerbariorumRecord(true);
    expected.setLatitude(BigDecimal.valueOf(30d));
    expected.setLongitude(BigDecimal.valueOf(-80d));
    expected.setNumberSpecimens(1000);
    expected.setEmail(Arrays.asList("uark@uark.com", "uark2@uark.com"));
    expected.setPhone(Arrays.asList("123", "456", "789"));
    expected.setHomepage(URI.create("http://www.a.com"));
    expected.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, Utils.encodeIRN(IRN_TEST)));
    expected.setMailingAddress(getExpectedMailingAddress());
    expected.setAddress(getExpectedAddress());

    // When
    Institution converted = entityConverter.convertToInstitution(ih, existing);

    // Expect
    assertTrue(converted.lenientEquals(expected));
  }

  @Test
  public void institutionConversionTest() {
    // IH Institution
    IHInstitution ih = getIhInstitution();

    // Expected
    Institution expected = new Institution();
    expected.setCode("UARK");
    expected.setName("University of Arkansas");
    expected.setIndexHerbariorumRecord(true);
    expected.setLatitude(BigDecimal.valueOf(30d));
    expected.setLongitude(BigDecimal.valueOf(-80d));
    expected.setNumberSpecimens(1000);
    expected.setEmail(Arrays.asList("uark@uark.com", "uark2@uark.com"));
    expected.setPhone(Arrays.asList("123", "456", "789"));
    expected.setHomepage(URI.create("http://www.a.com"));
    expected.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, Utils.encodeIRN(IRN_TEST)));
    expected.setMailingAddress(getExpectedMailingAddress());
    expected.setAddress(getExpectedAddress());

    // When
    Institution converted = entityConverter.convertToInstitution(ih);

    // Expect
    assertTrue(converted.lenientEquals(expected));
  }

  @Test
  public void collectionConversionFromExistingTest() {
    // Existing
    Collection existing = new Collection();
    existing.setCode("code");
    existing.setName("old name");
    existing.setEmail(Collections.singletonList("bb@bb.com"));
    existing.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, Utils.encodeIRN(IRN_TEST)));

    Address address = new Address();
    address.setCity("foo");
    address.setProvince("Arkansas");
    address.setCountry(Country.AFGHANISTAN);
    existing.setMailingAddress(address);

    // IH Institution
    IHInstitution ih = getIhInstitution();

    // Expected
    Collection expected = new Collection();
    expected.setCode("UARK");
    expected.setName("University of Arkansas");
    expected.setIndexHerbariorumRecord(true);
    expected.setEmail(Arrays.asList("uark@uark.com", "uark2@uark.com"));
    expected.setPhone(Arrays.asList("123", "456", "789"));
    expected.setHomepage(URI.create("http://www.a.com"));
    expected.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, Utils.encodeIRN(IRN_TEST)));
    expected.setMailingAddress(getExpectedMailingAddress());
    expected.setAddress(getExpectedAddress());
    expected.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, Utils.encodeIRN(IRN_TEST)));

    // When
    Collection converted = entityConverter.convertToCollection(ih, existing);

    // Expect
    assertTrue(converted.lenientEquals(expected));
  }

  @Test
  public void personConversionFromExistingTest() {
    // Existing
    Person existing = new Person();
    existing.setFirstName("John Wayne");
    existing.setEmail("wayne@test.com");
    existing.setPosition("Dummy");
    existing.setFax("0000");

    Address address = new Address();
    address.setCity("foo");
    address.setProvince("Arkansas");
    address.setCountry(Country.AFGHANISTAN);
    existing.setMailingAddress(address);

    // IH Staff
    IHStaff ihStaff = getIhStaff();

    // Expected
    Person expected = new Person();
    expected.setFirstName("First M.");
    expected.setLastName("Last");
    expected.setEmail("a@a.com");
    expected.setPhone("123");
    expected.setFax("987");
    expected.setPosition("Collections Manager");
    expected.setMailingAddress(getExpectedAddress());
    expected.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, Utils.encodeIRN(IRN_TEST)));

    // When
    Person converted = entityConverter.convertToPerson(ihStaff, existing);

    // Expect
    assertTrue(converted.lenientEquals(expected));
  }

  @Test
  public void personConversionTest() {
    // IH Staff
    IHStaff ihStaff = getIhStaff();

    // Expected
    Person expected = new Person();
    expected.setFirstName("First M.");
    expected.setLastName("Last");
    expected.setEmail("a@a.com");
    expected.setPhone("123");
    expected.setFax("987");
    expected.setPosition("Collections Manager");
    expected.setMailingAddress(getExpectedAddress());
    expected.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, Utils.encodeIRN(IRN_TEST)));

    // When
    Person converted = entityConverter.convertToPerson(ihStaff);

    // Expect
    assertTrue(converted.lenientEquals(expected));
  }

  @Ignore("Manual test")
  @Test
  public void testCountryMapping() {
    IHHttpClient ihHttpClient = IHHttpClient.create("http://sweetgum.nybg.org/science/api/v1/");
    List<String> countries = ihHttpClient.getCountries();

    Map<String, Country> mappings = EntityConverter.mapCountries(countries);

    assertEquals(countries.size(), mappings.size());
  }

  private IHStaff getIhStaff() {
    IHStaff s = new IHStaff();
    s.setIrn(IRN_TEST);
    s.setCode("UARK");
    s.setLastName("Last");
    s.setMiddleName("M.");
    s.setFirstName("First");
    s.setPosition("Collections Manager");

    IHStaff.Address address = new IHStaff.Address();
    address.setStreet("street");
    address.setCity("FAYETTEVILLE");
    address.setState("state");
    address.setCountry("U.S.A.");
    address.setZipCode("zip");
    s.setAddress(address);

    IHStaff.Contact contact = new IHStaff.Contact();
    contact.setEmail("a@a.com\nb@b.com");
    contact.setPhone("123,456\n789");
    contact.setFax("987;654");
    s.setContact(contact);

    return s;
  }

  private IHInstitution getIhInstitution() {
    // IH Institution
    IHInstitution ih = new IHInstitution();
    ih.setIrn(IRN_TEST);
    ih.setCode("UARK");
    ih.setOrganization("University of Arkansas");
    ih.setSpecimenTotal(1000);

    IHInstitution.Address ihAddress = new IHInstitution.Address();
    ihAddress.setPhysicalStreet("street");
    ihAddress.setPhysicalState("state");
    ihAddress.setPhysicalZipCode("zip");
    ihAddress.setPhysicalCity("FAYETTEVILLE");
    ihAddress.setPhysicalCountry("United States");
    ihAddress.setPostalCity("FAYETTEVILLE");
    ihAddress.setPostalCountry("U.K.");
    ih.setAddress(ihAddress);

    IHInstitution.Location location = new IHInstitution.Location();
    location.setLat(30d);
    location.setLon(-80d);
    ih.setLocation(location);

    IHInstitution.Contact contact = new IHInstitution.Contact();
    contact.setEmail("uark@uark.com\nuark2@uark.com");
    contact.setPhone("123,456\n789");
    contact.setWebUrl("http://www. a.com;http://www.b.com");
    ih.setContact(contact);
    return ih;
  }

  private Address getExpectedMailingAddress() {
    Address expectedMailingAddress = new Address();
    expectedMailingAddress.setCity("FAYETTEVILLE");
    expectedMailingAddress.setCountry(Country.UNITED_KINGDOM);
    return expectedMailingAddress;
  }

  private Address getExpectedAddress() {
    Address expectedAddress = new Address();
    expectedAddress.setAddress("street");
    expectedAddress.setProvince("state");
    expectedAddress.setPostalCode("zip");
    expectedAddress.setCity("FAYETTEVILLE");
    expectedAddress.setCountry(Country.UNITED_STATES);
    return expectedAddress;
  }
}
