package org.gbif.registry.ws.client.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.ws.client.guice.RegistryWs;
import org.gbif.ws.client.QueryParamBuilder;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.ws.rs.core.MultivaluedMap;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class CollectionWsClient extends BaseExtendableCollectionEntityClient<Collection> implements CollectionService {

  private static final GenericType<PagingResponse<Collection>> PAGING_COLLECTION =
    new GenericType<PagingResponse<Collection>>() {};

  /**
   * @param resource   the base url to the underlying webservice
   * @param authFilter optional authentication filter, can be null
   */
  @Inject
  protected CollectionWsClient(
    @RegistryWs WebResource resource, @Nullable ClientFilter authFilter
  ) {
    super(Collection.class, resource.path("grscicoll/collection"), authFilter, PAGING_COLLECTION);
  }

  @Override
  public PagingResponse<Collection> list(
      @Nullable String query,
      @Nullable UUID institutionKey,
      @Nullable UUID contactKey,
      @Nullable String code,
      @Nullable String name,
      @Nullable Pageable pageable) {
    return get(PAGING_COLLECTION,
               null,
               QueryParamBuilder.create("institution", institutionKey)
                 .queryParam("contact", contactKey)
                 .queryParam("q", query)
                 .queryParam("code", code)
                 .queryParam("name", name)
                 .build(),
               pageable);
  }


  @Override
  public PagingResponse<Collection> listDeleted(@Nullable Pageable pageable) {
    return get(PAGING_COLLECTION, pageable, "deleted");
  }

  @Override
  public List<KeyCodeNameResult> suggest(@Nullable String q) {
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    queryParams.putSingle("q", q);
    return get(LIST_KEY_CODE_NAME, null, queryParams, null, "suggest");
  }

}
