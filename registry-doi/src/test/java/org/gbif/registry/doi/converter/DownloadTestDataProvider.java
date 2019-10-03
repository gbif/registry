package org.gbif.registry.doi.converter;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadFormat;
import org.gbif.api.model.occurrence.PredicateDownloadRequest;
import org.gbif.api.model.occurrence.predicate.EqualsPredicate;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;

import java.util.Date;
import java.util.UUID;

public class DownloadTestDataProvider {

  static Download prepareDownload() {
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

    return download;
  }

  static DatasetOccurrenceDownloadUsage prepareDatasetOccurrenceDownloadUsage1() {
    DatasetOccurrenceDownloadUsage du1 = new DatasetOccurrenceDownloadUsage();
    du1.setDatasetKey(UUID.randomUUID());
    du1.setDatasetTitle("my title");
    du1.setDatasetDOI(new DOI("10.1234/5679"));
    du1.setNumberRecords(101);

    return du1;
  }

  static DatasetOccurrenceDownloadUsage prepareDatasetOccurrenceDownloadUsage2() {
    DatasetOccurrenceDownloadUsage du2 = new DatasetOccurrenceDownloadUsage();
    du2.setDatasetKey(UUID.randomUUID());
    du2.setDatasetTitle("my title #2");
    du2.setDatasetDOI(new DOI("10.1234/klimbim"));
    du2.setNumberRecords(2002);

    return du2;
  }

  static GbifUser prepareUser() {
    GbifUser user = new GbifUser();
    user.setUserName("occdownload.gbif.org");
    user.setEmail("occdownload@devlist.gbif.org");
    user.setFirstName(null);
    user.setLastName("GBIF.org");

    return user;
  }
}
