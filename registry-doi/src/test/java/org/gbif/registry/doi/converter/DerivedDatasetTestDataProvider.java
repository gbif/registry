package org.gbif.registry.doi.converter;

import org.gbif.api.model.common.DOI;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

public class DerivedDatasetTestDataProvider {

  public static DerivedDataset prepareDerivedDataset() {
    DerivedDataset derivedDataset = new DerivedDataset();
    derivedDataset.setOriginalDownloadDOI(new DOI("10.21373/dl.abcdef"));
    derivedDataset.setCitation("Derived dataset GBIF.org");
    derivedDataset.setCreated(new Date());
    derivedDataset.setCreatedBy("test");
    derivedDataset.setModified(new Date());
    derivedDataset.setModifiedBy("test");
    derivedDataset.setDoi(new DOI("10.21373/dd.abcdef"));
    derivedDataset.setRegistrationDate(new Date());
    derivedDataset.setTarget(URI.create("gbif.org"));
    derivedDataset.setTitle("Derived dataset title");

    return derivedDataset;
  }

  public static DerivedDatasetUsage prepareDerivedDatasetUsage(DOI derivedDatasetDoi, DOI datasetDoi, Long count) {
    DerivedDatasetUsage derivedDatasetUsage = new DerivedDatasetUsage();
    derivedDatasetUsage.setDatasetDoi(datasetDoi);
    derivedDatasetUsage.setDatasetKey(UUID.randomUUID());
    derivedDatasetUsage.setDatasetTitle("Related dataset");
    derivedDatasetUsage.setDerivedDatasetDoi(derivedDatasetDoi);
    derivedDatasetUsage.setNumberRecords(count);

    return derivedDatasetUsage;
  }
}
