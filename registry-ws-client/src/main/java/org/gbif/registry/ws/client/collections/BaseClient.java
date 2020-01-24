package org.gbif.registry.ws.client.collections;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.*;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.MachineTagService;
import org.gbif.api.service.registry.TagService;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.gbif.registry.ws.client.GenericTypes;
import org.gbif.ws.client.BaseWsGetClient;
import org.gbif.ws.client.QueryParamBuilder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.codehaus.jackson.map.ObjectMapper;

/** Base ws client for {@link CollectionEntity}. */
public abstract class BaseClient<
        T extends CollectionEntity & Taggable & MachineTaggable & Identifiable>
    extends BaseWsGetClient<T, UUID>
    implements CrudService<T>, TagService, MachineTagService, IdentifierService {

  protected static final GenericType<List<Tag>> LIST_TAG = new GenericType<List<Tag>>() {};
  protected static final GenericType<List<Identifier>> LIST_IDENTIFIER =
    new GenericType<List<Identifier>>() {};
  private final GenericType<PagingResponse<T>> pagingType;
  private final ObjectMapper mapper = new ObjectMapper();

  protected BaseClient(
      Class<T> resourceClass,
      WebResource resource,
      @Nullable ClientFilter authFilter,
      GenericType<PagingResponse<T>> pagingType) {
    super(resourceClass, resource, authFilter);
    this.pagingType = pagingType;
  }

  @Override
  public UUID create(@NotNull T entity) {
    return post(UUID.class, entity, "/");
  }

  @Override
  public void delete(@NotNull UUID uuid) {
    delete(String.valueOf(uuid));
  }

  @Override
  public void update(@NotNull T entity) {
    put(entity, String.valueOf(entity.getKey()));
  }

  @Override
  public int addIdentifier(@NotNull UUID key, @NotNull Identifier identifier) {
    return post(Integer.class, identifier, String.valueOf(key), "identifier");
  }

  @Override
  public void deleteIdentifier(@NotNull UUID key, int identifierKey) {
    delete(String.valueOf(key), "identifier", String.valueOf(identifierKey));
  }

  @Override
  public List<Identifier> listIdentifiers(@NotNull UUID key) {
    return get(LIST_IDENTIFIER, null, null, (Pageable) null, String.valueOf(key), "identifier");
  }

  @Override
  public int addTag(@NotNull UUID key, @NotNull String value) {
    return addTag(key, new Tag(value));
  }

  @Override
  public int addTag(@NotNull UUID key, @NotNull Tag tag) {
    return post(Integer.class, tag, String.valueOf(key), "tag");
  }

  @Override
  public void deleteTag(@NotNull UUID key, int tagKey) {
    delete(String.valueOf(key), "tag", String.valueOf(tagKey));
  }

  @Override
  public List<Tag> listTags(@NotNull UUID uuid, @Nullable String s) {
    return get(LIST_TAG, null, null, (Pageable) null, String.valueOf(uuid), "tag");
  }

  @Override
  public int addMachineTag(UUID targetEntityKey, MachineTag machineTag) {
    // allow post through varnish (no chunked encoding needed)
    int tagId;
    try {
      tagId = getResource()
        .path(targetEntityKey.toString())
        .path("machineTag")
        .type(MediaType.APPLICATION_JSON)
        .post(Integer.class, mapper.writeValueAsBytes(machineTag));

    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return tagId;
  }

  @Override
  public int addMachineTag(@NotNull UUID targetEntityKey, @NotNull TagName tagName, @NotNull String value) {
    return addMachineTag(targetEntityKey, new MachineTag(tagName, value));
  }

  @Override
  public int addMachineTag(@NotNull UUID targetEntityKey, @NotNull String namespace, @NotNull String name, @NotNull String value) {
    return addMachineTag(targetEntityKey, new MachineTag(namespace, name, value));
  }

  @Override
  public void deleteMachineTag(UUID targetEntityKey, int machineTagKey) {
    delete(targetEntityKey.toString(), "machineTag", String.valueOf(machineTagKey));
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull String namespace) {
    delete(targetEntityKey.toString(), "machineTag", String.valueOf(namespace));
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull TagNamespace tagNamespace) {
    delete(targetEntityKey.toString(), "machineTag", tagNamespace.getNamespace());
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull String namespace, @NotNull String name) {
    delete(targetEntityKey.toString(), "machineTag", String.valueOf(namespace), String.valueOf(name));
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull TagName tagName) {
    delete(targetEntityKey.toString(), "machineTag", tagName.getNamespace().getNamespace(), tagName.getName());
  }

  @Override
  public List<MachineTag> listMachineTags(UUID targetEntityKey) {
    return get(GenericTypes.LIST_MACHINETAG, null, null, (Pageable) null, targetEntityKey.toString(), "machineTag");
  }

  public PagingResponse<T> listByMachineTag(String namespace, @Nullable String name, @Nullable String value, @Nullable Pageable page) {
    return get(pagingType, null, QueryParamBuilder.create("machineTagNamespace", namespace, "machineTagName", name, "machineTagValue", value).build(), page);
  }

}
