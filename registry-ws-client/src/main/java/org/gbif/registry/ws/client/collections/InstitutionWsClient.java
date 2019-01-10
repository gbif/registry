package org.gbif.registry.ws.client.collections;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.InstitutionService;
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

public class InstitutionWsClient extends BaseExtendableCollectionEntityClient<Institution>
  implements InstitutionService {

  private static final GenericType<PagingResponse<Institution>> PAGING_INSTITUTION =
    new GenericType<PagingResponse<Institution>>() {};

  /**
   * @param resource   the base url to the underlying webservice
   * @param authFilter optional authentication filter, can be null
   */
  @Inject
  protected InstitutionWsClient(
    @RegistryWs WebResource resource, @Nullable ClientFilter authFilter
  ) {
    super(Institution.class, resource.path("grbio/institution"), authFilter, PAGING_INSTITUTION);
  }

  @Override
  public PagingResponse<Institution> list(
    @Nullable String query, @Nullable UUID contactKey, @Nullable Pageable pageable
  ) {
    return get(PAGING_INSTITUTION,
               null,
               QueryParamBuilder.create("q", query).queryParam("contact", contactKey).build(),
               pageable);
  }

  @Override
  public List<KeyCodeNameResult> suggest(@Nullable String q) {
    MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
    queryParams.putSingle("q", q);
    return get(LIST_KEY_CODE_NAME, null, queryParams, null, "suggest");
  }
}
