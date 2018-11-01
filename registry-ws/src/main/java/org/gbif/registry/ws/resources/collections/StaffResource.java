package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.Staff;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.service.collections.StaffService;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.StaffMapper;

import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.bval.guice.Validate;
import org.mybatis.guice.transactional.Transactional;

import static com.google.common.base.Preconditions.checkArgument;

@Singleton
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("staff")
public class StaffResource extends BaseCrudResource<Staff> implements StaffService {

  private final StaffMapper staffMapper;
  private final AddressMapper addressMapper;

  @Inject
  public StaffResource(StaffMapper staffMapper, AddressMapper addressMapper) {
    super(staffMapper);
    this.staffMapper = staffMapper;
    this.addressMapper = addressMapper;
  }

  @GET
  public PagingResponse<Staff> list(
      @Nullable @QueryParam("q") String query,
      @Nullable @QueryParam("institution") UUID institutionKey,
      @Nullable @QueryParam("collection") UUID collectionKey,
      @Nullable @Context Pageable page) {
    if (institutionKey != null) {
      return listByInstitution(institutionKey, page);
    } else if (collectionKey != null) {
      return listByCollection(collectionKey, page);
    } else if (!Strings.isNullOrEmpty(query)) {
      return search(query, page);
    } else {
      return list(page);
    }
  }

  @Override
  public PagingResponse<Staff> listByInstitution(@NotNull UUID institutionKey, @Nullable Pageable pageable) {
    pageable = pageable == null ? new PagingRequest() : pageable;
    long total = staffMapper.countByInstitution(institutionKey);

    return new PagingResponse<>(
        pageable.getOffset(),
        pageable.getLimit(),
        total,
        staffMapper.listStaffByInstitution(institutionKey, pageable));
  }

  @Override
  public PagingResponse<Staff> listByCollection(@NotNull UUID collectionKey, @Nullable Pageable pageable) {
    pageable = pageable == null ? new PagingRequest() : pageable;
    long total = staffMapper.countByCollection(collectionKey);
    return new PagingResponse<>(
        pageable.getOffset(),
        pageable.getLimit(),
        total,
        staffMapper.listStaffByCollection(collectionKey, pageable));
  }

  @Transactional
  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public UUID create(@Valid @NotNull Staff staff) {
    checkArgument(staff.getKey() == null, "Unable to create an entity which already has a key");

    if (staff.getMailingAddress() != null) {
      checkArgument(
          staff.getMailingAddress().getKey() == null,
          "Unable to create an address which already has a key");
      addressMapper.create(staff.getMailingAddress());
    }

    staff.setKey(UUID.randomUUID());
    staffMapper.create(staff);

    return staff.getKey();
  }
}
