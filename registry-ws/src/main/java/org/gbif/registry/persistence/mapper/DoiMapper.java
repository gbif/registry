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

/**
 * MyBatis mapper to store DOIs and their status in the registry db.
 */
public interface DoiMapper extends DoiPersistenceService {
  DoiData get (@Param("doi") DOI doi);
  List<Map<String, Object>> list (@Nullable @Param("status") DoiStatus status, @Nullable @Param("type") DoiType type,
                                  @Nullable @Param("page") Pageable page);
  String getMetadata(@Param("doi") DOI doi);
  void create (@Param("doi") DOI doi, @Param("type") DoiType type);
  void update (@Param("doi") DOI doi, @Param("status") DoiData status, @Param("xml") String xml);
  void delete (@Param("doi") DOI doi);
}
