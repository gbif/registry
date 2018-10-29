package org.gbif.registry.ws.resources;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Staff;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.AddressMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.InstitutionMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.ws.guice.Trim;
import org.gbif.ws.server.interceptor.NullToNotFound;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.groups.Default;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.bval.guice.Validate;
import org.mybatis.guice.transactional.Transactional;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.EDITOR_ROLE;

import static com.google.common.base.Preconditions.checkArgument;

@Singleton
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("institution")
public class InstitutionResource implements InstitutionService {

  private final InstitutionMapper institutionMapper;
  private final AddressMapper addressMapper;
  private final IdentifierMapper identifierMapper;
  private final TagMapper tagMapper;

  @Inject
  public InstitutionResource(
      InstitutionMapper institutionMapper,
      AddressMapper addressMapper,
      IdentifierMapper identifierMapper,
      TagMapper tagMapper) {
    this.institutionMapper = institutionMapper;
    this.addressMapper = addressMapper;
    this.identifierMapper = identifierMapper;
    this.tagMapper = tagMapper;
  }

  @POST
  @Trim
  @NullToNotFound
  @Validate
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public UUID create(@NotNull Institution entity, @Context SecurityContext security) {
    entity.setCreatedBy(security.getUserPrincipal().getName());
    entity.setModifiedBy(security.getUserPrincipal().getName());
    return create(entity);
  }

  @Transactional
  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public UUID create(@Valid Institution institution) {
    checkArgument(
        institution.getKey() == null, "Unable to create an institutio which already has a key");

    if (institution.getAddress() != null) {
      checkArgument(
          institution.getAddress().getKey() == null,
          "Unable to create an address which already has a key");
      addressMapper.create(institution.getAddress());
    }

    if (institution.getMailingAddress() != null) {
      checkArgument(
          institution.getMailingAddress().getKey() == null,
          "Unable to create an address which already has a key");
      addressMapper.create(institution.getMailingAddress());
    }

    if (institution.getTags() != null && !institution.getTags().isEmpty()) {
      for (Tag tag : institution.getTags()) {
        checkArgument(tag.getKey() == null, "Unable to create a tag which already has a key");
        tagMapper.createTag(tag);
      }
    }

    if (institution.getIdentifiers() != null && !institution.getIdentifiers().isEmpty()) {
      for (Identifier identifier : institution.getIdentifiers()) {
        checkArgument(
            identifier.getKey() == null, "Unable to create an identifier which already has a key");
        identifierMapper.createIdentifier(identifier);
      }
    }

    institution.setKey(UUID.randomUUID());
    institutionMapper.create(institution);

    return institution.getKey();
  }

  @DELETE
  @Path("{key}")
  @NullToNotFound
  @Validate
  @Transactional
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public void delete(@PathParam("key") @NotNull UUID key, @Context SecurityContext security) {
    Institution institutionToDelete = get(key);
    institutionToDelete.setModifiedBy(security.getUserPrincipal().getName());
    update(institutionToDelete);

    delete(key);
  }

  @Transactional
  @Validate
  @Override
  public void delete(@NotNull UUID uuid) {
    institutionMapper.delete(uuid);
  }

  @GET
  @Path("{key}")
  @Nullable
  @NullToNotFound
  @Validate(validateReturnedValue = true)
  @Override
  public Institution get(@PathParam("key") @NotNull UUID uuid) {
    return institutionMapper.get(uuid);
  }

  @GET
  public PagingResponse<Institution> list(
      @Nullable @QueryParam("q") String query, @Nullable @Context Pageable page) {
    return Strings.isNullOrEmpty(query) ? list(page) : list(query, page);
  }

  @Override
  public PagingResponse<Institution> list(@Nullable Pageable pageable) {
    pageable = pageable == null ? new PagingRequest() : pageable;
    long total = institutionMapper.count();

    return new PagingResponse<>(
        pageable.getOffset(), pageable.getLimit(), total, institutionMapper.list(pageable));
  }

  @Override
  public PagingResponse<Institution> search(String query, @Nullable Pageable pageable) {
    if (pageable == null) {
      pageable = new PagingRequest();
    }
    long total = institutionMapper.countWithFilter(query);

    return new PagingResponse<>(
        pageable.getOffset(),
        pageable.getLimit(),
        total,
        institutionMapper.search(query, pageable));
  }

  @DELETE
  @Path("{key}")
  @NullToNotFound
  @Validate
  @Transactional
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public void update(@PathParam("key") @NotNull UUID key, @Context SecurityContext security) {
    Institution institutionToDelete = get(key);
    institutionToDelete.setModifiedBy(security.getUserPrincipal().getName());
    update(institutionToDelete);
  }

  @Transactional
  @Validate
  @Override
  public void update(@NotNull @Valid Institution institution) {
    Institution institutionOld = get(institution.getKey());

    if (institutionOld.getDeleted() != null) {
      throw new IllegalArgumentException("Can't update a deleted entity");
    }

    if (institution.getDeleted() != null) {
      throw new IllegalArgumentException("Can't delete an entity when updating");
    }

    institutionMapper.update(institution);
  }

  @GET
  @Path("{key}/contact")
  @Nullable
  @NullToNotFound
  @Validate(validateReturnedValue = true)
  @Override
  public List<Staff> listContacts(@PathParam("key") @NotNull UUID uuid) {
    return institutionMapper.listContacts(uuid);
  }

  @POST
  @Path("{key}/contact/{staffKey}")
  @Validate
  @Transactional
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void addContact(
      @PathParam("key") @NotNull UUID institutionKey,
      @PathParam("staffKey") @NotNull UUID staffKey) {
    institutionMapper.addContact(institutionKey, staffKey);
  }

  @DELETE
  @Path("{key}/contact/{staffKey}")
  @Validate
  @Transactional
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void removeContact(
      @PathParam("key") @NotNull UUID institutionKey,
      @PathParam("staffKey") @NotNull UUID staffKey) {
    institutionMapper.removeContact(institutionKey, staffKey);
  }

  @POST
  @Path("{key}/identifier")
  @Validate(groups = {PrePersist.class, Default.class})
  @Trim
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public int addIdentifier(
      @PathParam("key") @NotNull UUID institutionKey,
      @Valid @NotNull Identifier identifier,
      @Context SecurityContext security) {
    identifier.setCreatedBy(security.getUserPrincipal().getName());
    return addIdentifier(institutionKey, identifier);
  }

  @Override
  public int addIdentifier(@NotNull UUID uuid, @NotNull Identifier identifier) {
    return WithMyBatis.addIdentifier(identifierMapper, institutionMapper, uuid, identifier);
  }

  @DELETE
  @Path("{key}/identifier/{identifierKey}")
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  @Transactional
  @Override
  public void deleteIdentifier(
      @PathParam("key") @NotNull UUID institutionKey,
      @PathParam("identifierKey") @NotNull int identifierKey) {
    WithMyBatis.deleteIdentifier(institutionMapper, institutionKey, identifierKey);
  }

  @GET
  @Path("{key}/identifier")
  @Nullable
  @NullToNotFound
  @Validate(validateReturnedValue = true)
  @Override
  public List<Identifier> listIdentifiers(@PathParam("key") @NotNull UUID uuid) {
    return WithMyBatis.listIdentifiers(institutionMapper, uuid);
  }

  @POST
  @Path("{key}/tag")
  @Validate(groups = {PrePersist.class, Default.class})
  @Trim
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public int addTag(
      @PathParam("key") @NotNull UUID institutionKey,
      @Valid @NotNull Tag tag,
      @Context SecurityContext security) {
    tag.setCreatedBy(security.getUserPrincipal().getName());
    return addTag(institutionKey, tag);
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull String value) {
    Tag tag = new Tag();
    tag.setValue(value);
    return addTag(uuid, tag);
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull Tag tag) {
    return WithMyBatis.addTag(tagMapper, institutionMapper, uuid, tag);
  }

  @DELETE
  @Path("{key}/tag/{tagKey}")
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  @Transactional
  @Override
  public void deleteTag(
      @PathParam("key") @NotNull UUID institutionKey, @PathParam("tagKey") int tagKey) {
    WithMyBatis.deleteTag(institutionMapper, institutionKey, tagKey);
  }

  @GET
  @Path("{key}/tag")
  @Nullable
  @NullToNotFound
  @Validate(validateReturnedValue = true)
  @Override
  public List<Tag> listTags(@NotNull UUID uuid, @QueryParam("owner") @Nullable String owner) {
    return WithMyBatis.listTags(institutionMapper, uuid, owner);
  }
}
