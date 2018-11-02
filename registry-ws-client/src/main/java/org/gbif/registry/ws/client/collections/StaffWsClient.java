package org.gbif.registry.ws.client.collections;

import org.gbif.api.model.collections.Staff;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.StaffService;
import org.gbif.registry.ws.client.guice.RegistryWs;
import org.gbif.ws.client.QueryParamBuilder;

import java.util.UUID;
import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

public class StaffWsClient extends BaseCrudClient<Staff> implements StaffService {

  private static final GenericType<PagingResponse<Staff>> PAGING_STAFF =
      new GenericType<PagingResponse<Staff>>() {};

  @Inject
  protected StaffWsClient(@RegistryWs WebResource resource, @Nullable ClientFilter authFilter) {
    super(Staff.class, resource.path("staff"), authFilter, PAGING_STAFF);
  }

  @Override
  public PagingResponse<Staff> listByInstitution(UUID institutionKey, @Nullable Pageable pageable) {
    return get(
        PAGING_STAFF,
        null,
        QueryParamBuilder.create("institution", institutionKey).build(),
        pageable);
  }

  @Override
  public PagingResponse<Staff> listByCollection(UUID collectionKey, @Nullable Pageable pageable) {
    return get(
        PAGING_STAFF,
        null,
        QueryParamBuilder.create("collection", collectionKey).build(),
        pageable);
  }
}
