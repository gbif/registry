package org.gbif.registry.persistence.mapper;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasetValidationMapper {

  void createOrUpdate(
      @Param("datasetKey") UUID datasetKey,
      @Param("attempt") int attempt,
      @Param("report") String report);

  String get(
      @Param("datasetKey") UUID datasetKey,
      @Param("attempt") int attempt);

  String getLatest(@Param("datasetKey") UUID datasetKey);
}
