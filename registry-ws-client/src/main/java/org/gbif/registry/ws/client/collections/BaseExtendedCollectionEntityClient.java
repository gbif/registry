package org.gbif.registry.ws.client.collections;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.ContactService;

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
public abstract class BaseExtendedCollectionEntityClient<
        T extends CollectionEntity & Taggable & MachineTaggable & Identifiable & Contactable>
    extends BaseClient<T> implements ContactService {

  protected static final GenericType<List<Person>> LIST_PERSON = new GenericType<List<Person>>() {};
  protected static final GenericType<List<KeyCodeNameResult>> LIST_KEY_CODE_NAME =
    new GenericType<List<KeyCodeNameResult>>() {};

  protected BaseExtendedCollectionEntityClient(
      Class resourceClass,
      WebResource resource,
      @Nullable ClientFilter authFilter,
      GenericType pagingType) {
    super(resourceClass, resource, authFilter, pagingType);
  }

  @Override
  public List<Person> listContacts(@NotNull UUID key) {
    return get(LIST_PERSON, null, null, (Pageable) null, String.valueOf(key), "contact");
  }

  @Override
  public void addContact(@NotNull UUID entityKey, @NotNull UUID contactKey) {
    post(contactKey, String.valueOf(entityKey), "contact");
  }

  @Override
  public void removeContact(@NotNull UUID entityKey, @NotNull UUID contactKey) {
    delete(String.valueOf(entityKey), "contact", String.valueOf(contactKey));
  }
}
