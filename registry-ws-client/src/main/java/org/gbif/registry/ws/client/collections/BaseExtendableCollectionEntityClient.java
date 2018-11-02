package org.gbif.registry.ws.client.collections;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Staff;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.TagService;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

/**
 * Base ws client for {@link CollectionEntity} that are also {@link Taggable}, {@link Identifiable}
 * and {@link Contactable}.
 */
public abstract class BaseExtendableCollectionEntityClient<T extends CollectionEntity & Taggable & Identifiable & Contactable>
    extends BaseCrudClient<T> implements TagService, IdentifierService, ContactService {

  protected static final GenericType<List<Staff>> LIST_STAFF = new GenericType<List<Staff>>() {};
  protected static final GenericType<List<Tag>> LIST_TAG = new GenericType<List<Tag>>() {};
  protected static final GenericType<List<Identifier>> LIST_IDENTIFIER =
      new GenericType<List<Identifier>>() {};

  protected BaseExtendableCollectionEntityClient(
      Class resourceClass,
      WebResource resource,
      @Nullable ClientFilter authFilter,
      GenericType pagingType) {
    super(resourceClass, resource, authFilter, pagingType);
  }

  @Override
  public List<Staff> listContacts(@NotNull UUID key) {
    return get(LIST_STAFF, null, null, (Pageable) null, String.valueOf(key), "contact");
  }

  @Override
  public void addContact(@NotNull UUID entityKey, @NotNull UUID contactKey) {
    post(contactKey, String.valueOf(entityKey), "contact");
  }

  @Override
  public void removeContact(@NotNull UUID entityKey, @NotNull UUID contactKey) {
    delete(String.valueOf(entityKey), "contact", String.valueOf(contactKey));
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
}
