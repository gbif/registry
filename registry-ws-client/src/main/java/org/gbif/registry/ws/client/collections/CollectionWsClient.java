package org.gbif.registry.ws.client.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.ws.client.guice.RegistryWs;
import org.gbif.ws.client.QueryParamBuilder;

import java.util.UUID;
import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

public class CollectionWsClient extends BaseExtendableCollectionEntityClient<Collection>
    implements CollectionService {

  private static final GenericType<PagingResponse<Collection>> PAGING_COLLECTION =
      new GenericType<PagingResponse<Collection>>() {};

  /**
   * @param resource the base url to the underlying webservice
   * @param authFilter optional authentication filter, can be null
   */
  @Inject
  protected CollectionWsClient(
      @RegistryWs WebResource resource, @Nullable ClientFilter authFilter) {
    super(Collection.class, resource.path("collection"), authFilter, PAGING_COLLECTION);
  }

  @Override
  public PagingResponse<Collection> listByInstitution(
      UUID institutionKey, @Nullable Pageable pageable) {
    return get(
        PAGING_COLLECTION,
        null,
        QueryParamBuilder.create("institution", institutionKey).build(),
        pageable);
  }
}
