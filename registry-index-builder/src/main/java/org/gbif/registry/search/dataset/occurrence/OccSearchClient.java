package org.gbif.registry.search.dataset.occurrence;

import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.occurrence.Occurrence;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchRequest;
import org.gbif.api.service.occurrence.OccurrenceSearchService;
import org.gbif.ws.client.BaseWsFacetedSearchClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;

import static org.gbif.ws.paths.OccurrencePaths.OCCURRENCE_PATH;

/**
 * Ws client for {@link OccurrenceSearchService} only exposing the faceted search method.
 * We duplicate part of the regular client class here to avoid circular dependencies.
 */
@Singleton
public class OccSearchClient extends BaseWsFacetedSearchClient<Occurrence, OccurrenceSearchParameter, OccurrenceSearchRequest>
    implements AutoCloseable {

  // Response type.
  private static final GenericType<SearchResponse<Occurrence, OccurrenceSearchParameter>> GENERIC_TYPE =
    new GenericType<SearchResponse<Occurrence, OccurrenceSearchParameter>>() {
    };

  /**
   * @param resource to the occurrence webapp
   */
  @Inject
  public OccSearchClient(WebResource resource) {
    super(resource.path(OCCURRENCE_PATH), GENERIC_TYPE);
  }

  @Override
  public void close() throws Exception {

  }
}
