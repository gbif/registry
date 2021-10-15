/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.doi.converter;

import org.gbif.api.model.common.DOI;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xmlunit.matchers.CompareMatcher;

import static org.gbif.registry.doi.converter.DataCiteConverterTestCommon.getXmlMetadataFromFile;
import static org.hamcrest.MatcherAssert.assertThat;

public class DerivedDatasetConverterTest {

  @Test
  public void testConvertDerivedDataset() throws Exception {
    // given
    DerivedDataset derivedDataset = DerivedDatasetTestDataProvider.prepareDerivedDataset();
    List<DerivedDatasetUsage> relatedDatasets = new ArrayList<>();
    DerivedDatasetUsage derivedDatasetUsage1 =
        DerivedDatasetTestDataProvider.prepareDerivedDatasetUsage(
            derivedDataset.getDoi(), new DOI("10.21373/abcdef"), 2L);
    DerivedDatasetUsage derivedDatasetUsage2 =
        DerivedDatasetTestDataProvider.prepareDerivedDatasetUsage(
            derivedDataset.getDoi(), new DOI("10.21373/fedcba"), null);

    relatedDatasets.add(derivedDatasetUsage1);
    relatedDatasets.add(derivedDatasetUsage2);

    String expectedMetadataXml =
        getXmlMetadataFromFile("metadata/metadata-derived-dataset.xml")
            .replace(
                "<publicationYear>2020</publicationYear>",
                "<publicationYear>" + LocalDate.now().getYear() + "</publicationYear>");

    // when
    DataCiteMetadata actualMetadata =
        DerivedDatasetConverter.convert(derivedDataset, relatedDatasets);
    String actualMetadataXml = DataCiteValidator.toXml(derivedDataset.getDoi(), actualMetadata);

    // then
    assertThat(
        actualMetadataXml,
        CompareMatcher.isIdenticalTo(expectedMetadataXml).ignoreWhitespace().normalizeWhitespace());
  }
}
