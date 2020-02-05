package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.registry.doi.DoiPersistenceService;
import org.gbif.registry.doi.DoiType;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * MyBatis mapper to store DOIs and their status in the registry db.
 */
@Repository
public interface DoiMapper extends DoiPersistenceService {

  @Override
  DoiData get(@Param("doi") DOI doi);

  @Override
  DoiType getType(@Param("doi") DOI doi);

  @Override
  List<Map<String, Object>> list(
      @Nullable @Param("status") DoiStatus status,
      @Nullable @Param("type") DoiType type,
      @Nullable @Param("page") Pageable page);

  @Override
  String getMetadata(@Param("doi") DOI doi);

  @Override
  void create(@Param("doi") DOI doi, @Param("type") DoiType type);

  @Override
  void update(@Param("doi") DOI doi, @Param("status") DoiData status, @Param("xml") String xml);

  @Override
  void delete(@Param("doi") DOI doi);
}
