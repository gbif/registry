package org.gbif.registry.doi.converter;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.NameIdentifier;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.junit.Test;
import org.xmlunit.matchers.CompareMatcher;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.gbif.registry.doi.converter.DataCiteConverterTestCommon.getXmlMetadataFromFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class DatasetConverterTest {

  @Test
  public void testConvertDataset() throws Exception {
    // given
    DOI doi = new DOI("10.1234/21373");
    Organization publisher = preparePublisher();
    Dataset dataset = DatasetTestDataProvider.prepareFullDataset(doi);
    String expectedMetadataXml = getXmlMetadataFromFile("metadata/metadata-dataset.xml");

    // when
    DataCiteMetadata actualMetadata = DatasetConverter.convert(dataset, publisher);
    String actualMetadataXml = DataCiteValidator.toXml(doi, actualMetadata);

    // then
    assertThat(actualMetadataXml, CompareMatcher.isIdenticalTo(expectedMetadataXml).ignoreWhitespace().normalizeWhitespace());
  }

  @Test
  public void testDatasetLicense() {
    // given
    Organization publisher = preparePublisher();
    DOI doi = new DOI("10.1234/5679");
    Dataset dataset = DatasetTestDataProvider.prepareSimpleDataset(doi);

    // when
    DataCiteMetadata metadata = DatasetConverter.convert(dataset, publisher);

    // then
    assertEquals("Copyright ©", metadata.getRightsList().getRights().get(0).getValue());
  }

  @Test
  public void testUserIdToNameIdentifier() {
    // given
    List<String> userIds = Collections.singletonList("http://orcid.org/0000-0000-0000-0001");

    // when
    NameIdentifier creatorNid = DatasetConverter.userIdToNameIdentifier(userIds);

    // then
    assertEquals("http://orcid.org/", creatorNid.getSchemeURI());
    assertEquals("0000-0000-0000-0001", creatorNid.getValue());
  }

  private Organization preparePublisher() {
    final Organization publisher = new Organization();
    publisher.setTitle("X-Publisher");
    publisher.setKey(UUID.randomUUID());

    return publisher;
  }
}
