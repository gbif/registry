package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.common.DOI;
import org.gbif.doi.service.DoiStatus;
import org.gbif.registry.doi.DoiType;

import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper to store DOIs and their status in the registry db.
 */
public interface DoiMapper {
  void create (@Param("doi") DOI doi, @Param("type") DoiType type);
  void update (@Param("doi") DOI doi, @Param("status") DoiStatus status, @Param("xml") String xml);
  DoiStatus get (@Param("doi") DOI doi);
}
