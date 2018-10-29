package org.gbif.registry.ws.client;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Staff;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.ws.client.BaseWsGetClient;
import org.gbif.ws.client.QueryParamBuilder;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

public class InstitutionWsClient extends BaseWsGetClient<Institution, UUID>
    implements InstitutionService {

  /**
   * @param resourceClass the main entity class
   * @param resource the base url to the underlying webservice
   * @param authFilter optional authentication filter, can be null
   */
  protected InstitutionWsClient(
      Class<Institution> resourceClass, WebResource resource, @Nullable ClientFilter authFilter) {
    super(resourceClass, resource.path("institution"), authFilter);
  }

  @Override
  public UUID create(@NotNull Institution institution) {
    return post(UUID.class, institution, "/");
  }

  @Override
  public void delete(@NotNull UUID uuid) {
    delete(uuid.toString());
  }

  @Override
  public Institution get(@NotNull UUID uuid) {
    return get(uuid.toString());
  }

  @Override
  public PagingResponse<Institution> list(@Nullable Pageable pageable) {
    return get(GenericTypes.PAGING_INSTITUTION, null, null, pageable);
  }

  @Override
  public PagingResponse<Institution> search(String query, @Nullable Pageable pageable) {
    return get(
        GenericTypes.PAGING_INSTITUTION,
        null,
        QueryParamBuilder.create("q", query).build(),
        pageable);
  }

  @Override
  public void update(@NotNull Institution institution) {
    put(institution, institution.getKey().toString());
  }

  @Override
  public List<Staff> listContacts(@NotNull UUID uuid) {
    return get(GenericTypes.LIST_STAFF, null, null, (Pageable) null, uuid.toString(), "contact");
  }

  @Override
  public void addContact(@NotNull UUID uuid, @NotNull UUID staffKey) {
    post(UUID.class, staffKey, uuid.toString(), "contact");
  }

  @Override
  public void removeContact(@NotNull UUID uuid, @NotNull UUID staffKey) {
    delete(uuid.toString(), "contact", staffKey.toString());
  }

  @Override
  public int addIdentifier(@NotNull UUID uuid, @NotNull Identifier identifier) {
    return post(Integer.class, identifier, uuid.toString(), "identifier");
  }

  @Override
  public void deleteIdentifier(@NotNull UUID uuid, int identifierKey) {
    delete(uuid.toString(), "identifier", String.valueOf(identifierKey));
  }

  @Override
  public List<Identifier> listIdentifiers(@NotNull UUID uuid) {
    return get(
        GenericTypes.LIST_IDENTIFIER, null, null, (Pageable) null, uuid.toString(), "identifier");
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull String value) {
    return addTag(uuid, new Tag(value));
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull Tag tag) {
    return post(Integer.class, tag, uuid.toString(), "tag");
  }

  @Override
  public void deleteTag(@NotNull UUID uuid, int tagKey) {
    delete(uuid.toString(), "tag", String.valueOf(tagKey));
  }

  @Override
  public List<Tag> listTags(@NotNull UUID uuid, @Nullable String s) {
    return get(GenericTypes.LIST_TAG, null, null, (Pageable) null, uuid.toString(), "tag");
  }
}
