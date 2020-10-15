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
    derivedDataset.setSourceUrl(URI.create("gbif.org"));
    derivedDataset.setTitle("Derived dataset title");

    return derivedDataset;
  }

  public static DerivedDatasetUsage prepareDerivedDatasetUsage(
      DOI derivedDatasetDoi, DOI datasetDoi, Long count) {
    DerivedDatasetUsage derivedDatasetUsage = new DerivedDatasetUsage();
    derivedDatasetUsage.setDatasetDoi(datasetDoi);
    derivedDatasetUsage.setDatasetKey(UUID.randomUUID());
    derivedDatasetUsage.setDatasetTitle("Related dataset");
    derivedDatasetUsage.setDerivedDatasetDoi(derivedDatasetDoi);
    derivedDatasetUsage.setNumberRecords(count);

    return derivedDatasetUsage;
  }
}
