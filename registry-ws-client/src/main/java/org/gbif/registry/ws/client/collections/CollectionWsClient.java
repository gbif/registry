package org.gbif.registry.ws.client;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Staff;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.ws.client.guice.RegistryWs;
import org.gbif.ws.client.BaseWsGetClient;
import org.gbif.ws.client.QueryParamBuilder;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.google.inject.Inject;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

public class CollectionWsClient extends BaseExtendableColletionEntityClient<Collection>
    implements CollectionService {

  /**
   * @param resource the base url to the underlying webservice
   * @param authFilter optional authentication filter, can be null
   */
  @Inject
  protected CollectionWsClient(
      @RegistryWs WebResource resource, @Nullable ClientFilter authFilter) {
    super(
        Collection.class, resource.path("collection"), authFilter, GenericTypes.PAGING_COLLECTION);
  }

  @Override
  public PagingResponse<Collection> listByInstitution(
      UUID institutionKey, @Nullable Pageable pageable) {
    return get(
        GenericTypes.PAGING_COLLECTION,
        null,
        QueryParamBuilder.create("institution", institutionKey).build(),
        pageable);
  }
}
