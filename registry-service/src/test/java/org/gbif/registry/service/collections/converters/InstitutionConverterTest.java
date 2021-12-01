package org.gbif.registry.service.collections.converters;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.MasterSourceType;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.Country;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InstitutionConverterTest {

  @Test
  public void convertFromOrganizationTest() {
    Organization organization = createOrganization();

    String institutionCode = "CODE";
    Institution convertedInstitution =
        InstitutionConverter.convertFromOrganization(organization, institutionCode);

    assertEquals(institutionCode, convertedInstitution.getCode());
    assertConvertedInstitution(organization, convertedInstitution);
  }

  @Test
  public void convertExistingInstitutionFromOrganizationTest() {
    Organization organization = createOrganization();

    String institutionCode = "CODE";
    String originalInstitutionName = "fooo";

    Institution institution = new Institution();
    institution.setCode(institutionCode);
    institution.setName(originalInstitutionName);
    institution.setNumberSpecimens(124);

    Institution convertedInstitution =
        InstitutionConverter.convertFromOrganization(organization, institution);

    assertEquals(institutionCode, convertedInstitution.getCode());
    assertNotEquals(originalInstitutionName, convertedInstitution.getName());
    assertEquals(institution.getNumberSpecimens(), convertedInstitution.getNumberSpecimens());
    assertConvertedInstitution(organization, convertedInstitution);
  }

  private void assertConvertedInstitution(
      Organization organization, Institution convertedInstitution) {
    assertEquals(organization.getTitle(), convertedInstitution.getName());
    assertEquals(organization.getDescription(), convertedInstitution.getDescription());
    assertEquals(organization.getHomepage().get(0), convertedInstitution.getHomepage());
    assertTrue(convertedInstitution.isActive());

    assertNotNull(convertedInstitution.getAddress());
    assertTrue(
        convertedInstitution
            .getAddress()
            .getAddress()
            .startsWith(organization.getAddress().get(0)));
    assertEquals(organization.getCity(), convertedInstitution.getAddress().getCity());
    assertEquals(organization.getProvince(), convertedInstitution.getAddress().getProvince());
    assertEquals(organization.getPostalCode(), convertedInstitution.getAddress().getPostalCode());
    assertEquals(organization.getCountry(), convertedInstitution.getAddress().getCountry());

    // assert contacts
    assertEquals(
        organization.getContacts().size(), convertedInstitution.getContactPersons().size());
    convertedInstitution
        .getContactPersons()
        .forEach(
            c -> {
              assertNotNull(c.getFirstName());
              assertNotNull(c.getLastName());
              assertEquals(1, c.getUserIds().size());
              assertNotNull(c.getPosition());
              assertNotNull(c.getEmail());
              assertNotNull(c.getPhone());
              assertNotNull(c.getAddress());
              assertNotNull(c.getCity());
              assertNotNull(c.getProvince());
              assertNotNull(c.getCountry());
              assertNotNull(c.getPostalCode());
            });
  }

  @NotNull
  private Organization createOrganization() {
    Organization organization = new Organization();
    organization.setTitle("title");
    organization.setDescription("description");
    organization.setHomepage(Collections.singletonList(URI.create("http://test.com")));
    organization.setPhone(Collections.singletonList("1234"));
    organization.setEmail(Collections.singletonList("aa@aa.com"));
    organization.setLatitude(BigDecimal.ONE);
    organization.setLongitude(BigDecimal.ONE);
    organization.setLogoUrl(URI.create("http://aa.com"));
    organization.setAddress(Arrays.asList("addr1", "addr2"));
    organization.setCity("city");
    organization.setProvince("prov");
    organization.setPostalCode("1234");
    organization.setCountry(Country.AFGHANISTAN);

    // contacts
    Contact orgContact1 = new Contact();
    orgContact1.setFirstName("first name");
    orgContact1.setLastName("last name");
    orgContact1.setPrimary(true);
    orgContact1.setUserId(Collections.singletonList("http://orcid.org/0000-0003-1662-7791"));
    orgContact1.setPosition(Collections.singletonList("position"));
    orgContact1.setEmail(Collections.singletonList("aa@test.com"));
    orgContact1.setPhone(Collections.singletonList("12345"));
    orgContact1.setAddress(Collections.singletonList("adrr"));
    orgContact1.setCity("city1");
    orgContact1.setProvince("province");
    orgContact1.setCountry(Country.AFGHANISTAN);
    orgContact1.setPostalCode("1234");
    organization.getContacts().add(orgContact1);

    Contact orgContact2 = new Contact();
    orgContact2.setFirstName("first name 2");
    orgContact2.setLastName("last name 2");
    orgContact2.setPrimary(true);
    orgContact2.setUserId(Collections.singletonList("http://orcid.org/0000-0003-1662-7791"));
    orgContact2.setPosition(Collections.singletonList("position2"));
    orgContact2.setEmail(Collections.singletonList("aa@test.com"));
    orgContact2.setPhone(Collections.singletonList("12345"));
    orgContact2.setAddress(Collections.singletonList("adrr2"));
    orgContact2.setCity("city2");
    orgContact2.setProvince("province");
    orgContact2.setCountry(Country.AFGHANISTAN);
    orgContact2.setPostalCode("1234");
    organization.getContacts().add(orgContact2);
    return organization;
  }
}
