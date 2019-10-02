package org.gbif.registry.doi.converter;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadFormat;
import org.gbif.api.model.occurrence.PredicateDownloadRequest;
import org.gbif.api.model.occurrence.predicate.EqualsPredicate;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.occurrence.query.TitleLookup;
import org.gbif.registry.doi.converter.DownloadConverter;
import org.gbif.utils.file.FileUtils;
import org.junit.Test;
import org.xmlunit.matchers.CompareMatcher;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DownloadConverterTest {

  @Test
  public void testConvertDownload() throws Exception {
    DatasetOccurrenceDownloadUsage du1 = new DatasetOccurrenceDownloadUsage();
    du1.setDatasetKey(UUID.randomUUID());
    du1.setDatasetTitle("my title");
    du1.setDatasetDOI(new DOI("10.1234/5679"));
    du1.setNumberRecords(101);

    DatasetOccurrenceDownloadUsage du2 = new DatasetOccurrenceDownloadUsage();
    du2.setDatasetKey(UUID.randomUUID());
    du2.setDatasetTitle("my title #2");
    du2.setDatasetDOI(new DOI("10.1234/klimbim"));
    du2.setNumberRecords(2002);

    Download download = new Download();
    download.setCreated(new Date(1569967363000L));
    download.setModified(new Date(1570053763000L));
    download.setDoi(new DOI("10.1234/5678"));
    download.setKey("1");
    download.setNumberDatasets(1L);
    download.setSize(100);
    download.setStatus(Download.Status.SUCCEEDED);
    download.setTotalRecords(10);
    PredicateDownloadRequest downloadRequest = new PredicateDownloadRequest();
    downloadRequest.setCreator("dev@gbif.org");
    downloadRequest.setPredicate(new EqualsPredicate(OccurrenceSearchParameter.TAXON_KEY, "3"));
    downloadRequest.setFormat(DownloadFormat.DWCA);
    download.setRequest(downloadRequest);

    GbifUser user = new GbifUser();
    user.setUserName("occdownload.gbif.org");
    user.setEmail("occdownload@devlist.gbif.org");
    user.setFirstName(null);
    user.setLastName("GBIF.org");

    // mock title lookup API
    TitleLookup tl = mock(TitleLookup.class);
    when(tl.getDatasetTitle(anyString())).thenReturn("PonTaurus");
    when(tl.getSpeciesName(anyString())).thenReturn("Abies alba Mill.");

    DataCiteMetadata metadata = DownloadConverter.convert(download, user, Lists.newArrayList(du1, du2), tl);
    String xml = DataCiteValidator.toXml(download.getDoi(), metadata);

    final DataCiteMetadata expectedMetadata = DataCiteValidator.fromXml(FileUtils.classpathStream("metadata/metadata-download.xml"));
    final String expected = DataCiteValidator.toXml(expectedMetadata, true);

    assertThat(xml, CompareMatcher.isIdenticalTo(expected).normalizeWhitespace().ignoreWhitespace());
  }

  @Test
  public void testTruncateDesription() throws Exception {
    DOI doi = new DOI("10.15468/dl.v8zc57");
    String xml = Resources.toString(Resources.getResource("metadata/datacite-large.xml"), Charsets.UTF_8);
    String xml2 = DownloadConverter.truncateDescription(doi, xml, URI.create("http://gbif.org"));
    //System.out.println(xml2);
    DataCiteValidator.validateMetadata(xml2);
    assertTrue(xml2.contains("for full list of all constituents"));
    assertFalse(xml2.contains("University of Ghent"));
    assertTrue(xml2.contains("10.15468/siye1z"));
    assertEquals(3690, xml2.length());
  }

  @Test
  public void testTruncateConstituents() throws Exception {
    DOI doi = new DOI("10.15468/dl.v8zc57");
    String xml = Resources.toString(Resources.getResource("metadata/datacite-large.xml"), Charsets.UTF_8);
    String xml2 = DownloadConverter.truncateConstituents(doi, xml, URI.create("http://gbif.org"));
    DataCiteValidator.validateMetadata(xml2);
    assertTrue(xml2.contains("for full list of all constituents"));
    assertFalse(xml2.contains("University of Ghent"));
    assertFalse(xml2.contains("10.15468/siye1z"));
    assertEquals(2352, xml2.length());
  }
}
