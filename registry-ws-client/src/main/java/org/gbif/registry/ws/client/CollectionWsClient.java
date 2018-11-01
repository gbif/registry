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

public class CollectionWsClient extends BaseWsGetClient<Collection, UUID>
    implements CollectionService {

  /**
   * @param resource the base url to the underlying webservice
   * @param authFilter optional authentication filter, can be null
   */
  @Inject
  protected CollectionWsClient(
      @RegistryWs WebResource resource, @Nullable ClientFilter authFilter) {
    super(Collection.class, resource.path("collection"), authFilter);
  }

  @Override
  public UUID create(@NotNull Collection collection) {
    return post(UUID.class, collection, "/");
  }

  @Override
  public void delete(@NotNull UUID uuid) {
    delete(String.valueOf(uuid));
  }

  @Override
  public PagingResponse<Collection> list(@Nullable Pageable pageable) {
    return get(GenericTypes.PAGING_COLLECTION, null, null, pageable);
  }

  @Override
  public PagingResponse<Collection> listByInstitution(
    UUID institutionKey, @Nullable Pageable pageable
  ) {
    return get(
      GenericTypes.PAGING_COLLECTION,
      null,
      QueryParamBuilder.create("institution", institutionKey).build(),
      pageable);
  }

  @Override
  public PagingResponse<Collection> search(String query, @Nullable Pageable pageable) {
    return get(
        GenericTypes.PAGING_COLLECTION,
        null,
        QueryParamBuilder.create("q", query).build(),
        pageable);
  }

  @Override
  public void update(@NotNull Collection collection) {
    put(collection, String.valueOf(collection.getKey()));
  }

  @Override
  public List<Staff> listContacts(@NotNull UUID uuid) {
    return get(GenericTypes.LIST_STAFF, null, null, (Pageable) null, String.valueOf(uuid), "contact");
  }

  @Override
  public void addContact(@NotNull UUID uuid, @NotNull UUID staffKey) {
    post(staffKey, String.valueOf(uuid), "contact", String.valueOf(staffKey));
  }

  @Override
  public void removeContact(@NotNull UUID uuid, @NotNull UUID staffKey) {
    delete(String.valueOf(uuid), "contact", String.valueOf(staffKey));
  }

  @Override
  public int addIdentifier(@NotNull UUID uuid, @NotNull Identifier identifier) {
    return post(Integer.class, identifier, String.valueOf(uuid), "identifier");
  }

  @Override
  public void deleteIdentifier(@NotNull UUID uuid, int identifierKey) {
    delete(String.valueOf(uuid), "identifier", String.valueOf(identifierKey));
  }

  @Override
  public List<Identifier> listIdentifiers(@NotNull UUID uuid) {
    return get(
        GenericTypes.LIST_IDENTIFIER, null, null, (Pageable) null, String.valueOf(uuid), "identifier");
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull String value) {
    return addTag(uuid, new Tag(value));
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull Tag tag) {
    return post(Integer.class, tag, String.valueOf(uuid), "tag");
  }

  @Override
  public void deleteTag(@NotNull UUID uuid, int tagKey) {
    delete(String.valueOf(uuid), "tag", String.valueOf(tagKey));
  }

  @Override
  public List<Tag> listTags(@NotNull UUID uuid, @Nullable String s) {
    return get(GenericTypes.LIST_TAG, null, null, (Pageable) null, String.valueOf(uuid), "tag");
  }
}
