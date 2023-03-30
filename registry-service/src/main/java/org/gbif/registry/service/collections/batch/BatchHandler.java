package org.gbif.registry.service.collections.batch;

import org.gbif.api.model.collections.Batch;
import org.gbif.api.model.common.export.ExportFormat;

import java.nio.file.Path;

public interface BatchHandler {

  void importBatch(
      Path entitiesPath, Path contactsPath, ExportFormat format, Batch batch, String userName);

  void updateBatch(
      Path entitiesPath, Path contactsPath, ExportFormat format, Batch batch, String userName);
}
