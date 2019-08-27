package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mapper that perform operations on dataset usages in occurrence downloads.
 */
@Repository
public interface DatasetOccurrenceDownloadMapper {

  List<DatasetOccurrenceDownloadUsage> listByDataset(@Param("datasetKey") UUID datasetKey,
                                                     @Nullable @Param("page") Pageable page);

  int countByDataset(@Param("datasetKey") UUID datasetKey);

  /**
   * Note that the Download objects within the DatasetOccurrenceDownloadUsage are not retrieved, to avoid massive
   * repetition, and high memory use for complex queries.
   */
  List<DatasetOccurrenceDownloadUsage> listByDownload(@Param("downloadKey") String downloadKey,
                                                      @Nullable @Param("page") Pageable page);

  void createUsages(@Param("downloadKey") String downloadKey, @Param("citationMap") Map<UUID,Long> downloadDataset);
}
