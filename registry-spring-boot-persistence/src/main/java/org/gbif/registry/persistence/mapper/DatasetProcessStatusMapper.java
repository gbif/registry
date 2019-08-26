package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Mapper that perform operations on {@link DatasetProcessStatus} instances.
 */
@Repository
public interface DatasetProcessStatusMapper {

  int count();

  int countAborted();

  int countByDataset(@Param("datasetKey") UUID datasetKey);

  void create(DatasetProcessStatus datasetProcessStatus);

  void update(DatasetProcessStatus datasetProcessStatus);

  DatasetProcessStatus get(@Param("datasetKey") UUID datasetKey, @Param("attempt") int attempt);

  List<DatasetProcessStatus> list(@Nullable @Param("page") Pageable page);

  List<DatasetProcessStatus> listByDataset(@Param("datasetKey") UUID datasetKey, @Nullable @Param("page") Pageable page);

  List<DatasetProcessStatus> listAborted(@Nullable @Param("page") Pageable page);

}
