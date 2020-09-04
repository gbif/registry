package org.gbif.registry.service;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.domain.ws.Citation;
import org.gbif.registry.domain.ws.CitationCreationRequest;

import java.util.UUID;

public interface RegistryCitationService {

  String getCitationText(DOI citationDoi);

  Citation create(CitationCreationRequest data);

  Citation get(DOI citationDoi);

  PagingResponse<Citation> getDatasetCitations(UUID datasetKey, Pageable page);

  PagingResponse<Dataset> getCitationDatasets(DOI datasetDoi, Pageable pageable);
}
