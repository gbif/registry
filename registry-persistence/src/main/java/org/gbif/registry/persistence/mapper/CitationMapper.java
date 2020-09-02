package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.domain.ws.Citation;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.UUID;

@Repository
public interface CitationMapper {

  void create(@Param("citation") Citation citation);

  Citation get(@Param("doi") DOI doi);

  void addDatasetCitation(@Param("datasetKeyOrDoi") String datasetKeyOrDoi, @Param("citationDoi") DOI citationDoi);

  PagingResponse<Citation> listByDataset(
      @Param("datasetKey") UUID datasetKey, @Nullable @Param("page") Pageable page);

  PagingResponse<Dataset> listByCitation(
      @Param("citationDoi") DOI citationDoi, @Nullable @Param("page") Pageable page);
}
