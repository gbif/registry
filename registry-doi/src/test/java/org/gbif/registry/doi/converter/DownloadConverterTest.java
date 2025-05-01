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

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.occurrence.query.TitleLookupService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xmlunit.matchers.CompareMatcher;

import static org.gbif.registry.doi.converter.DataCiteConverterTestCommon.getXmlMetadataFromFile;
import static org.gbif.registry.doi.converter.DownloadTestDataProvider.prepareDatasetOccurrenceDownloadUsage1;
import static org.gbif.registry.doi.converter.DownloadTestDataProvider.prepareDatasetOccurrenceDownloadUsage2;
import static org.gbif.registry.doi.converter.DownloadTestDataProvider.preparePredicateDownload;
import static org.gbif.registry.doi.converter.DownloadTestDataProvider.prepareSqlDownload;
import static org.gbif.registry.doi.converter.DownloadTestDataProvider.prepareUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DownloadConverterTest {

  @Test
  public void testConvertPredicateDownload() throws Exception {
    // given
    DatasetOccurrenceDownloadUsage du1 = prepareDatasetOccurrenceDownloadUsage1();
    DatasetOccurrenceDownloadUsage du2 = prepareDatasetOccurrenceDownloadUsage2();
    Download download = preparePredicateDownload();
    GbifUser user = prepareUser();
    // mock title lookup API
    TitleLookupService tl = mock(TitleLookupService.class);
    when(tl.getSpeciesName(anyString())).thenReturn("Abies alba Mill.");
    final String expected = getXmlMetadataFromFile("metadata/metadata-predicate-download.xml");

    // when
    DataCiteMetadata metadata =
      DownloadConverter.convert(download, user, Arrays.asList(du1, du2), tl, "http://api.gbif-dev.org/v1");
    String actualXmlMetadata = DataCiteValidator.toXml(download.getDoi(), metadata);

    // then
    assertThat(
      actualXmlMetadata,
      CompareMatcher.isIdenticalTo(expected).normalizeWhitespace().ignoreWhitespace());
    verify(tl, atLeastOnce()).getSpeciesName(anyString());
  }

  @Test
  public void testConvertSqlDownload() throws Exception {
    // given
    DatasetOccurrenceDownloadUsage du1 = prepareDatasetOccurrenceDownloadUsage1();
    DatasetOccurrenceDownloadUsage du2 = prepareDatasetOccurrenceDownloadUsage2();
    Download download = prepareSqlDownload();
    GbifUser user = prepareUser();
    // mock title lookup API
    TitleLookupService tl = mock(TitleLookupService.class);
    final String expected = getXmlMetadataFromFile("metadata/metadata-sql-download.xml");

    // when
    DataCiteMetadata metadata =
      DownloadConverter.convert(download, user, Arrays.asList(du1, du2), tl, "http://api.gbif-dev.org/v1");
    String actualXmlMetadata = DataCiteValidator.toXml(download.getDoi(), metadata);

    // then
    assertThat(
      actualXmlMetadata,
      CompareMatcher.isIdenticalTo(expected).normalizeWhitespace().ignoreWhitespace());
  }

  @Test
  public void testConvertLargePredicateDownload() throws Exception {
    // given
    DatasetOccurrenceDownloadUsage du = prepareDatasetOccurrenceDownloadUsage1();
    List<DatasetOccurrenceDownloadUsage> manyUsages = new ArrayList<DatasetOccurrenceDownloadUsage>();
    for (int i = 0; i < 1000; i++) {
      manyUsages.add(du);
    }
    Download download = preparePredicateDownload();
    GbifUser user = prepareUser();
    // mock title lookup API
    TitleLookupService tl = mock(TitleLookupService.class);
    when(tl.getSpeciesName(anyString())).thenReturn("Abies alba Mill.");
    final String expected = getXmlMetadataFromFile("metadata/metadata-large-predicate-download.xml");

    // when
    DataCiteMetadata metadata =
      DownloadConverter.convert(download, user, manyUsages, tl, "http://api.gbif-dev.org/v1");
    String actualXmlMetadata = DataCiteValidator.toXml(download.getDoi(), metadata);
    String truncatedXml = DownloadConverter.truncateConstituents(download.getDoi(), actualXmlMetadata);

    // then
    DataCiteValidator.validateMetadata(truncatedXml);
    assertThat(
      truncatedXml,
      CompareMatcher.isIdenticalTo(expected).normalizeWhitespace().ignoreWhitespace());
    verify(tl, atLeastOnce()).getSpeciesName(anyString());
  }
}
