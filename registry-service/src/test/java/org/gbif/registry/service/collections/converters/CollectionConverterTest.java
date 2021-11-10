package org.gbif.registry.service.collections.converters;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.MasterSourceType;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.eml.TaxonomicCoverage;
import org.gbif.api.model.registry.eml.TaxonomicCoverages;
import org.gbif.api.model.registry.eml.geospatial.GeospatialCoverage;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.PreservationMethodType;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the {@link CollectionConverter}. */
public class CollectionConverterTest {

  @Test
  public void convertFromDatasetTest() {
    Dataset dataset = new Dataset();
    dataset.setTitle("title");
    dataset.setDescription("description");
    dataset.setHomepage(URI.create("http://test.com"));
    dataset.setDoi(new DOI("10.1594/pangaea.94668"));

    // dataset collections
    org.gbif.api.model.registry.eml.Collection datasetColl1 =
        new org.gbif.api.model.registry.eml.Collection();
    datasetColl1.setName("coll1");
    datasetColl1.setSpecimenPreservationMethod(PreservationMethodType.ALCOHOL);

    org.gbif.api.model.registry.eml.Collection datasetColl2 =
        new org.gbif.api.model.registry.eml.Collection();
    datasetColl2.setName("coll2");
    datasetColl2.setSpecimenPreservationMethod(PreservationMethodType.DEEP_FROZEN);

    dataset.getCollections().add(datasetColl1);
    dataset.getCollections().add(datasetColl2);

    // taxonomic coverages
    TaxonomicCoverages taxonomicCoverages1 = new TaxonomicCoverages();
    taxonomicCoverages1.setDescription("Taxon Coverage Description");
    dataset.getTaxonomicCoverages().add(taxonomicCoverages1);

    TaxonomicCoverage taxonomicCoverage1 = new TaxonomicCoverage();
    taxonomicCoverage1.setCommonName("Common name");
    taxonomicCoverage1.setScientificName("Scientific name");
    taxonomicCoverages1.addCoverages(taxonomicCoverage1);

    TaxonomicCoverage taxonomicCoverage2 = new TaxonomicCoverage();
    taxonomicCoverage2.setScientificName("Scientific name 2");
    taxonomicCoverages1.addCoverages(taxonomicCoverage2);

    TaxonomicCoverages taxonomicCoverages2 = new TaxonomicCoverages();
    dataset.getTaxonomicCoverages().add(taxonomicCoverages2);
    TaxonomicCoverage taxonomicCoverage3 = new TaxonomicCoverage();
    taxonomicCoverage3.setCommonName("Common name");
    taxonomicCoverages2.addCoverages(taxonomicCoverage3);

    // geographic coverages
    GeospatialCoverage geospatialCoverage = new GeospatialCoverage();
    geospatialCoverage.setDescription("geo coverage");
    dataset.getGeographicCoverages().add(geospatialCoverage);

    // contacts
    Contact datasetContact1 = new Contact();
    datasetContact1.setFirstName("first name");
    datasetContact1.setLastName("last name");
    datasetContact1.setPrimary(true);
    datasetContact1.setUserId(Collections.singletonList("http://orcid.org/0000-0003-1662-7791"));
    datasetContact1.setPosition(Collections.singletonList("position"));
    datasetContact1.setEmail(Collections.singletonList("aa@test.com"));
    datasetContact1.setPhone(Collections.singletonList("12345"));
    datasetContact1.setAddress(Collections.singletonList("adrr"));
    datasetContact1.setCity("city1");
    datasetContact1.setProvince("province");
    datasetContact1.setCountry(Country.AFGHANISTAN);
    datasetContact1.setPostalCode("1234");
    dataset.getContacts().add(datasetContact1);

    Contact datasetContact2 = new Contact();
    datasetContact2.setFirstName("first name 2");
    datasetContact2.setLastName("last name 2");
    datasetContact2.setPrimary(true);
    datasetContact2.setUserId(Collections.singletonList("http://orcid.org/0000-0003-1662-7791"));
    datasetContact2.setPosition(Collections.singletonList("position2"));
    datasetContact2.setEmail(Collections.singletonList("aa@test.com"));
    datasetContact2.setPhone(Collections.singletonList("12345"));
    datasetContact2.setAddress(Collections.singletonList("adrr2"));
    datasetContact2.setCity("city2");
    datasetContact2.setProvince("province");
    datasetContact2.setCountry(Country.AFGHANISTAN);
    datasetContact2.setPostalCode("1234");
    dataset.getContacts().add(datasetContact2);

    // publishing organization
    Organization organization = new Organization();
    organization.setAddress(Arrays.asList("addr1", "addr2"));
    organization.setCity("city");
    organization.setProvince("prov");
    organization.setPostalCode("1234");
    organization.setCountry(Country.AFGHANISTAN);

    String collectionCode = "CODE";
    Collection collectionConverted =
        CollectionConverter.convertFromDataset(dataset, organization, collectionCode);

    assertEquals(collectionCode, collectionConverted.getCode());
    assertEquals(MasterSourceType.GBIF_REGISTRY, collectionConverted.getMasterSource());
    assertEquals(dataset.getTitle(), collectionConverted.getName());
    assertEquals(dataset.getDescription(), collectionConverted.getDescription());
    assertEquals(dataset.getHomepage(), collectionConverted.getHomepage());
    assertTrue(collectionConverted.isActive());
    assertEquals(
        dataset.getCollections().size(), collectionConverted.getPreservationTypes().size());
    assertEquals(
        dataset.getCollections().size(), collectionConverted.getIncorporatedCollections().size());
    assertTrue(
        collectionConverted
            .getTaxonomicCoverage()
            .startsWith(dataset.getTaxonomicCoverages().get(0).getDescription()));
    assertTrue(
        collectionConverted
            .getGeography()
            .startsWith(dataset.getGeographicCoverages().get(0).getDescription()));
    assertTrue(
        collectionConverted.getIdentifiers().stream()
            .anyMatch(i -> i.getIdentifier().equals(dataset.getDoi().getDoiName())));

    // assert organization fields
    assertNotNull(collectionConverted.getAddress());
    assertTrue(
        collectionConverted.getAddress().getAddress().startsWith(organization.getAddress().get(0)));
    assertEquals(organization.getCity(), collectionConverted.getAddress().getCity());
    assertEquals(organization.getProvince(), collectionConverted.getAddress().getProvince());
    assertEquals(organization.getPostalCode(), collectionConverted.getAddress().getPostalCode());
    assertEquals(organization.getCountry(), collectionConverted.getAddress().getCountry());

    // assert contacts
    assertEquals(dataset.getContacts().size(), collectionConverted.getContactPersons().size());
    collectionConverted
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
}
