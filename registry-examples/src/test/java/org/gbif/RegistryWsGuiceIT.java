package org.gbif;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;

import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

import static org.gbif.config.guice.RegistryWsClientFactoryGuice.webserviceClient;
import static org.gbif.config.guice.RegistryWsClientFactoryGuice.webserviceClientReadOnly;

import static org.junit.Assert.assertEquals;

public class RegistryWsGuiceIT {

  /**
   * Retrieve dataset Pontaurus with key "b3e760d8-8dcc-468c-a23d-8e9772affe59". It is assumed dataset Pontaurus will
   * always exist in every Registry database.
   */
  @Test
  public void testGetDataset() {
    DatasetService ds = webserviceClientReadOnly().getInstance(DatasetService.class);
    Dataset dataset = ds.get(UUID.fromString("8575f23e-f762-11e1-a439-00145eb45e9a"));
    assertEquals("PonTaurus collection", dataset.getTitle());
  }

  /**
   * Update dataset user has permission to edit.
   */
  @Ignore("Ignored because Dataset with key 38b0f0f5-cac1-48e6-be85-b9f8cdd5ca93 may not always exist")
  public void testUpdateDataset() {
    DatasetService ds = webserviceClient().getInstance(DatasetService.class);
    Dataset dataset = ds.get(UUID.fromString("38b0f0f5-cac1-48e6-be85-b9f8cdd5ca93"));
    dataset.setRights("CC0");
    ds.update(dataset);
  }
}
