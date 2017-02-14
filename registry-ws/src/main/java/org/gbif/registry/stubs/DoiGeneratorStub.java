package org.gbif.registry.stubs;

import org.gbif.api.model.common.DOI;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.registry.doi.generator.DoiGenerator;

import java.util.UUID;

/**
 * Stub class used to simplify Guice binding, e.g. when this class must be bound but isn't actually used.
 */
public class DoiGeneratorStub implements DoiGenerator {

  @Override
  public DOI newDatasetDOI() {
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
  public void failed(DOI doi, InvalidMetadataException e) {

  }

  @Override
  public void registerDataset(DOI doi, DataCiteMetadata metadata, UUID datasetKey) throws InvalidMetadataException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerDownload(DOI doi, DataCiteMetadata metadata, String downloadKey) throws InvalidMetadataException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerDataPackage(DOI doi, DataCiteMetadata metadata) throws InvalidMetadataException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(DOI doi) {
    throw new UnsupportedOperationException();
  }
}
