/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.occurrence.query.TitleLookupService;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.xmlunit.matchers.CompareMatcher;

import com.google.common.io.Resources;

import static org.gbif.registry.doi.converter.DataCiteConverterTestCommon.getXmlMetadataFromFile;
import static org.gbif.registry.doi.converter.DownloadTestDataProvider.prepareDatasetOccurrenceDownloadUsage1;
import static org.gbif.registry.doi.converter.DownloadTestDataProvider.prepareDatasetOccurrenceDownloadUsage2;
import static org.gbif.registry.doi.converter.DownloadTestDataProvider.prepareDownload;
import static org.gbif.registry.doi.converter.DownloadTestDataProvider.prepareUser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DownloadConverterTest {

  @Test
  public void testConvertDownload() throws Exception {
    // given
    DatasetOccurrenceDownloadUsage du1 = prepareDatasetOccurrenceDownloadUsage1();
    DatasetOccurrenceDownloadUsage du2 = prepareDatasetOccurrenceDownloadUsage2();
    Download download = prepareDownload();
    GbifUser user = prepareUser();
    // mock title lookup API
    TitleLookupService tl = mock(TitleLookupService.class);
    when(tl.getSpeciesName(anyString())).thenReturn("Abies alba Mill.");
    final String expected = getXmlMetadataFromFile("metadata/metadata-download.xml");

    // when
    DataCiteMetadata metadata =
        DownloadConverter.convert(download, user, Arrays.asList(du1, du2), tl);
    String actualXmlMetadata = DataCiteValidator.toXml(download.getDoi(), metadata);

    // then
    assertThat(
        actualXmlMetadata,
        CompareMatcher.isIdenticalTo(expected).normalizeWhitespace().ignoreWhitespace());
    verify(tl, atLeastOnce()).getSpeciesName(anyString());
  }

  @Test
  public void testTruncateDescription() throws Exception {
    // given
    DOI doi = new DOI("10.15468/dl.v8zc57");
    String sourceXml =
        Resources.toString(
            Resources.getResource("metadata/datacite-large.xml"), StandardCharsets.UTF_8);

    // when
    String truncatedXml =
        DownloadConverter.truncateDescription(doi, sourceXml, URI.create("http://gbif.org"));

    // then
    DataCiteValidator.validateMetadata(truncatedXml);
    assertTrue(truncatedXml.contains("for full list of all constituents"));
    assertFalse(truncatedXml.contains("University of Ghent"));
    assertTrue(truncatedXml.contains("10.15468/siye1z"));
    assertEquals(3690, truncatedXml.length());
  }

  @Test
  public void testTruncateConstituents() throws Exception {
    // given
    DOI doi = new DOI("10.15468/dl.v8zc57");
    String sourceXml =
        Resources.toString(
            Resources.getResource("metadata/datacite-large.xml"), StandardCharsets.UTF_8);

    // when
    String truncatedXml =
        DownloadConverter.truncateConstituents(doi, sourceXml, URI.create("http://gbif.org"));

    // then
    DataCiteValidator.validateMetadata(truncatedXml);
    assertTrue(truncatedXml.contains("for full list of all constituents"));
    assertFalse(truncatedXml.contains("University of Ghent"));
    assertFalse(truncatedXml.contains("10.15468/siye1z"));
    assertEquals(2352, truncatedXml.length());
  }
}
