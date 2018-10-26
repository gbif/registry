package org.gbif.registry.ws.resources;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifier;
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
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;

import com.google.inject.Inject;
import org.mybatis.guice.transactional.Transactional;

import static com.google.common.base.Preconditions.checkArgument;

// TODO: validations, trim and other annotations. Security and transactional too

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

  // TODO: create another method for the ws since the signature changes

  @Trim
  @Transactional
  @Override
  public UUID create(@NotNull Institution institution) {
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

  // TODO: ws
  @Transactional
  @Override
  public void delete(@NotNull UUID uuid) {
    // TODO: modifiedby
    institutionMapper.delete(uuid);
  }

  // TODO: ws
  @NullToNotFound
  @Override
  public Institution get(@NotNull UUID uuid) {
    return institutionMapper.get(uuid);
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
      pageable.getOffset(), pageable.getLimit(), total, institutionMapper.search(query, pageable));
  }

  @Transactional
  @Override
  public void update(@NotNull Institution institution) {
    Institution institutionOld = get(institution.getKey());

    if (institutionOld.getDeleted() != null) {
      throw new IllegalArgumentException("Can't update a deleted entity");
    }

    if (institution.getDeleted() != null) {
      throw new IllegalArgumentException("Can't delete an entity when updating");
    }

    institutionMapper.update(institution);
  }

  @Override
  public int addIdentifier(@NotNull UUID uuid, @NotNull Identifier identifier) {
    return WithMyBatis.addIdentifier(identifierMapper, institutionMapper, uuid, identifier);
  }

  @Override
  public void deleteIdentifier(@NotNull UUID uuid, int identifierKey) {
    WithMyBatis.deleteIdentifier(institutionMapper, uuid, identifierKey);
  }

  @Override
  public List<Identifier> listIdentifiers(@NotNull UUID uuid) {
    return WithMyBatis.listIdentifiers(institutionMapper, uuid);
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

  @Override
  public void deleteTag(@NotNull UUID uuid, int tagKey) {
    WithMyBatis.deleteTag(institutionMapper, uuid, tagKey);
  }

  @Override
  public List<Tag> listTags(@NotNull UUID uuid, @Nullable String owner) {
    return WithMyBatis.listTags(institutionMapper, uuid, owner);
  }
}
