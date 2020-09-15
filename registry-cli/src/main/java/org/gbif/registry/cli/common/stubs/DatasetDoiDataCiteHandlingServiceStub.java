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
package org.gbif.registry.cli.common.stubs;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.registry.doi.DatasetDoiDataCiteHandlingService;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Stub class used to simplify binding, e.g. when this class must be bound but isn't actually used.
 */
public class DatasetDoiDataCiteHandlingServiceStub implements DatasetDoiDataCiteHandlingService {

  @Override
  public void datasetChanged(Dataset dataset, @Nullable DOI previousDoi) {}

  @Override
  public void scheduleDatasetRegistration(DOI doi, DataCiteMetadata metadata, UUID datasetKey) {}

  @Override
  public void scheduleDerivedDatasetRegistration(
      DOI doi, DataCiteMetadata metadata, URI target, Date registrationDate) {}

  @Override
  public void scheduleDerivedDatasetUpdating(DOI doi, DataCiteMetadata metadata, URI target) {}
}
