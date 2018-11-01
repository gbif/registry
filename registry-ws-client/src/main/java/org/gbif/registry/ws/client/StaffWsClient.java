package org.gbif.registry.ws.client;

import org.gbif.api.model.collections.Staff;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.StaffService;
import org.gbif.registry.ws.client.guice.RegistryWs;
import org.gbif.ws.client.BaseWsGetClient;
import org.gbif.ws.client.QueryParamBuilder;

import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.google.inject.Inject;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

public class StaffWsClient extends BaseWsGetClient<Staff, UUID> implements StaffService {

  @Inject
  protected StaffWsClient(@RegistryWs WebResource resource, @Nullable ClientFilter authFilter) {
    super(Staff.class, resource.path("staff"), authFilter);
  }

  @Override
  public PagingResponse<Staff> listByInstitution(UUID institutionKey, @Nullable Pageable pageable) {
    return get(
        GenericTypes.PAGING_STAFF,
        null,
        QueryParamBuilder.create("institution", institutionKey).build(),
        pageable);
  }

  @Override
  public PagingResponse<Staff> listByCollection(UUID collectionKey, @Nullable Pageable pageable) {
    return get(
        GenericTypes.PAGING_STAFF,
        null,
        QueryParamBuilder.create("collection", collectionKey).build(),
        pageable);
  }

  @Override
  public UUID create(@NotNull Staff staff) {
    return post(UUID.class, staff, "/");
  }

  @Override
  public void delete(@NotNull UUID uuid) {
    delete(String.valueOf(uuid));
  }

  @Override
  public PagingResponse<Staff> list(@Nullable Pageable pageable) {
    return get(GenericTypes.PAGING_STAFF, null, null, pageable);
  }

  @Override
  public PagingResponse<Staff> search(String query, @Nullable Pageable pageable) {
    return get(
        GenericTypes.PAGING_STAFF, null, QueryParamBuilder.create("q", query).build(), pageable);
  }

  @Override
  public void update(@NotNull Staff staff) {
    put(staff, String.valueOf(staff.getKey()));
  }
}
