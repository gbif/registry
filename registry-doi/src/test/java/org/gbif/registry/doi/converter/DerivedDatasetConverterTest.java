package org.gbif.registry.doi.converter;

import org.gbif.api.model.common.DOI;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;
import org.junit.Test;
import org.xmlunit.matchers.CompareMatcher;

import java.util.ArrayList;
import java.util.List;

import static org.gbif.registry.doi.converter.DataCiteConverterTestCommon.getXmlMetadataFromFile;
import static org.junit.Assert.assertThat;

public class DerivedDatasetConverterTest {

  @Test
  public void testConvertDerivedDataset() throws Exception {
    // given
    DerivedDataset derivedDataset = DerivedDatasetTestDataProvider.prepareDerivedDataset();
    List<DerivedDatasetUsage> relatedDatasets = new ArrayList<>();
    DerivedDatasetUsage derivedDatasetUsage1 =
        DerivedDatasetTestDataProvider.prepareDerivedDatasetUsage(derivedDataset.getDoi(), new DOI("10.21373/abcdef"), 2L);
    DerivedDatasetUsage derivedDatasetUsage2 =
        DerivedDatasetTestDataProvider.prepareDerivedDatasetUsage(derivedDataset.getDoi(), new DOI("10.21373/fedcba"), null);

    relatedDatasets.add(derivedDatasetUsage1);
    relatedDatasets.add(derivedDatasetUsage2);

    String expectedMetadataXml = getXmlMetadataFromFile("metadata/metadata-derived-dataset.xml");

    // when
    DataCiteMetadata actualMetadata = DerivedDatasetConverter.convert(derivedDataset, relatedDatasets);
    String actualMetadataXml = DataCiteValidator.toXml(derivedDataset.getDoi(), actualMetadata);

    // then
    assertThat(
        actualMetadataXml,
        CompareMatcher.isIdenticalTo(expectedMetadataXml).ignoreWhitespace().normalizeWhitespace());
  }
}
