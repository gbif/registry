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
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.registry.doi.generator.DoiGenerator;

import java.net.URI;
import java.util.UUID;

/**
 * Stub class used to simplify binding, e.g. when this class must be bound but isn't actually used.
 */
public class DoiGeneratorStub implements DoiGenerator {

  @Override
  public DOI newDatasetDOI() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DOI newDerivedDatasetDOI() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DOI newDownloadDOI() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DOI newDataPackageDOI() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isGbif(DOI doi) {
    return false;
  }

  @Override
  public void failed(DOI doi, InvalidMetadataException e) {}

  @Override
  public void registerDataset(DOI doi, DataCiteMetadata metadata, UUID datasetKey)
      throws InvalidMetadataException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerDerivedDataset(DOI doi, DataCiteMetadata metadata, URI target)
      throws InvalidMetadataException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerDownload(DOI doi, DataCiteMetadata metadata, String downloadKey)
      throws InvalidMetadataException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerDataPackage(DOI doi, DataCiteMetadata metadata)
      throws InvalidMetadataException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(DOI doi) {
    throw new UnsupportedOperationException();
  }
}
