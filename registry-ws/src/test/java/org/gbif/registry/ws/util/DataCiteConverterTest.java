package org.gbif.registry.ws.util;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.User;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadRequest;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.eml.geospatial.BoundingBox;
import org.gbif.api.model.registry.eml.geospatial.GeospatialCoverage;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Language;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.occurrence.query.TitleLookup;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.beust.jcommander.internal.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataCiteConverterTest {

  @Test
  public void testGetYear() throws Exception {
    Date d = new Date(1418340702253l);
    assertEquals("2014", DataCiteConverter.getYear(d));
    assertNull(DataCiteConverter.getYear(null));
  }

  @Test
  public void testConvert() throws Exception {
    Organization publisher = new Organization();
    publisher.setTitle("X-Publisher");
    publisher.setKey(UUID.randomUUID());

    final DOI doi = new DOI("10.1234/5678");
    Dataset d = new Dataset();
    d.setKey(UUID.randomUUID());
    d.setType(DatasetType.OCCURRENCE);
    d.setTitle("my title");
    d.setCreated(new Date());
    d.setModified(new Date());
    d.setCreatedBy("Markus");
    d.setLanguage(Language.NORWEGIAN);
    d.setDataLanguage(Language.NORWEGIAN);

    DataCiteMetadata m = convertAndValidate(doi, d, publisher);
    assertEquals("my title", m.getTitles().getTitle().get(0).getValue());
    assertEquals("Markus", m.getCreators().getCreator().get(0).getCreatorName());
    assertEquals(doi.getDoiName(), m.getIdentifier().getValue());

    d.setDoi(doi);
    d.setDescription("bla bla bla bla bla bla bla bla - I talk too much");
    List<GeospatialCoverage> geos = Lists.newArrayList();
    d.setGeographicCoverages(geos);
    GeospatialCoverage g = new GeospatialCoverage();
    geos.add(g);
    g.setBoundingBox(new BoundingBox(1,2,3,4));

    m = convertAndValidate(doi, d, publisher);
    assertEquals("my title", m.getTitles().getTitle().get(0).getValue());
    assertEquals("Markus", m.getCreators().getCreator().get(0).getCreatorName());
    assertEquals("10.1234/5678", m.getIdentifier().getValue());
    assertEquals(Lists.<Double>newArrayList(1d,3d,2d,4d), m.getGeoLocations().getGeoLocation().get(0).getGeoLocationBox());
    assertEquals(d.getDescription(), m.getDescriptions().getDescription().get(0).getContent().get(0));
  }


  private DataCiteMetadata convertAndValidate(DOI doi, Dataset d, Organization publisher) throws InvalidMetadataException {
    DataCiteMetadata m = DataCiteConverter.convert(d, publisher);
    DataCiteValidator.toXml(doi, m);
    return m;
  }

  @Test
  public void testConvertDownload() throws Exception{
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
    download.setCreated(new Date());
    download.setDoi(new DOI("10.1234/5678"));
    download.setKey("1");
    download.setModified(new Date());
    download.setNumberDatasets(1l);
    download.setSize(100);
    download.setStatus(Download.Status.SUCCEEDED);
    download.setTotalRecords(10);
    DownloadRequest downloadRequest = new DownloadRequest();
    downloadRequest.setCreator("dev@gbif.org");
    download.setRequest(downloadRequest);

    User user = new User();
    user.setUserName("peta");
    user.setEmail("aha@music.com");
    user.setFirstName("Pete");
    user.setEmail("Doherty");

    // mock title lookup API
    TitleLookup tl = mock(TitleLookup.class);
    when(tl.getDatasetTitle(anyString())).thenReturn("PonTaurus");
    when(tl.getSpeciesName(anyString())).thenReturn("Abies alba Mill.");

    DataCiteMetadata metadata = DataCiteConverter.convert(download, user, Lists.newArrayList(du1, du2), tl);
    String xml = DataCiteValidator.toXml(download.getDoi(), metadata);
    System.out.println(xml);
    assertTrue(xml.contains(du1.getDatasetDOI().getDoiName()));
    assertTrue(xml.contains(du2.getDatasetDOI().getDoiName()));
    assertTrue(xml.contains(String.valueOf(du1.getNumberRecords())));
    assertTrue(xml.contains(String.valueOf(du2.getNumberRecords())));
  }

  @Test
  public void ttt() {
    long l = 321l;
    String q = "HALLO";
    System.out.print(String.format("A dataset containing %s species occurrences available in GBIF matching the query: %s",l,q));
  }
}
