package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.collections.InstitutionType;
import org.gbif.registry.collections.sync.ih.IHInstitution;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/** Tests the {@link EntityConverter}. */
public class EntityConverterTest {

  private static final String IRN_TEST = "1";
  private static final List<String> COUNTRIES =
      Arrays.asList("U.K.", "U.S.A.", "United Kingdom", "United States");

  @Test
  public void institutionConversionTest() {
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
    IHInstitution ih = new IHInstitution();
    ih.setIrn(IRN_TEST);
    ih.setCode("UARK");
    ih.setOrganization("University of Arkansas");
    ih.setSpecimenTotal(1000);

    IHInstitution.Address ihAddress = new IHInstitution.Address();
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
    contact.setEmail("uark@uark.com");
    ih.setContact(contact);

    // Expected
    Institution expected = new Institution();
    expected.setCode("UARK");
    expected.setName("University of Arkansas");
    expected.setType(InstitutionType.OTHER_INSTITUTIONAL_TYPE);
    expected.setIndexHerbariorumRecord(true);
    expected.setLatitude(BigDecimal.valueOf(30d));
    expected.setLongitude(BigDecimal.valueOf(-80d));
    expected.setNumberSpecimens(1000);
    expected.setEmail(Collections.singletonList("uark@uark.com"));
    expected.getIdentifiers().add(new Identifier(IdentifierType.IH_IRN, Utils.encodeIRN(IRN_TEST)));

    Address expectedMailingAddress = new Address();
    expectedMailingAddress.setCity("FAYETTEVILLE");
    expectedMailingAddress.setCountry(Country.UNITED_KINGDOM);
    expected.setMailingAddress(expectedMailingAddress);

    Address expectedAddress = new Address();
    expectedAddress.setCity("FAYETTEVILLE");
    expectedAddress.setCountry(Country.UNITED_STATES);
    expected.setAddress(expectedAddress);

    // When
    Institution converted = EntityConverter.from(COUNTRIES).convertToInstitution(ih, existing);

    // Expect
    assertTrue(converted.lenientEquals(expected));
  }
}
