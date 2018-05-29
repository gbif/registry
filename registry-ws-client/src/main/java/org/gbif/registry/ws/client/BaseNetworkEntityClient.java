package org.gbif.registry.ws.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.registry.NetworkEntityService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.gbif.ws.client.BaseWsGetClient;
import org.gbif.ws.client.QueryParamBuilder;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Preconditions;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.codehaus.jackson.map.ObjectMapper;

public class BaseNetworkEntityClient<T extends NetworkEntity> extends BaseWsGetClient<T, UUID>
  implements NetworkEntityService<T> {

  private final GenericType<PagingResponse<T>> pagingType;
  private final ObjectMapper mapper = new ObjectMapper();

  public BaseNetworkEntityClient(Class<T> resourceClass, WebResource resource, @Nullable ClientFilter authFilter,
    GenericType<PagingResponse<T>> pagingType) {
    super(resourceClass, resource, authFilter);
    this.pagingType = pagingType;
  }

  @Override
  public UUID create(T entity) {
    return post(UUID.class, entity, "/");
  }

  @Override
  public void delete(UUID key) {
    delete(key.toString());
  }

  @Override
  public PagingResponse<T> list(Pageable page) {
    return get(pagingType, null, null, page);
  }

  @Override
  public void update(T entity) {
    put(entity, entity.getKey().toString());
  }

  @Override
  public T get(UUID key) {
    return get(key.toString());
  }

  @Override
  public Map<UUID, String> getTitles(Collection<UUID> collection) {
    return post(GenericTypes.TITLES_MAP_TYPE, collection, "titles");
  }

  @Override
  public PagingResponse<T> search(String query, Pageable page) {
    return get(pagingType, (Locale) null, QueryParamBuilder.create("q", query).build(), page);
  }

  @Override
  public int addTag(UUID targetEntityKey, Tag tag) {
    // post the value to .../uuid/tag and expect an int back
    return post(Integer.class, tag, targetEntityKey.toString(), "tag");
  }

  @Override
  public int addTag(UUID targetEntityKey, String value) {
    return addTag(targetEntityKey, new Tag(value));
  }

  @Override
  public void deleteTag(UUID taggedEntityKey, int tagKey) {
    delete(taggedEntityKey.toString(), "tag", String.valueOf(tagKey));
  }

  @Override
  public List<Tag> listTags(UUID taggedEntityKey, String owner) {
    return get(GenericTypes.LIST_TAG, null, null, // TODO add owner here
      (Pageable) null, taggedEntityKey.toString(), "tag");
  }

  @Override
  public int addContact(UUID targetEntityKey, Contact contact) {
    // post the contact to .../uuid/contact and expect an int back
    return post(Integer.class, contact, targetEntityKey.toString(), "contact");
  }

  @Override
  public void updateContact(UUID targetEntityKey, Contact contact) {
    Preconditions.checkNotNull(contact.getKey(), "Contact key is required to update the contact");
    Preconditions.checkNotNull(targetEntityKey, "The target entity is required to update the contact");
    // put the contact to .../uuid/contact/id
    put(contact, targetEntityKey.toString(), "contact", contact.getKey().toString());
  }

  @Override
  public void deleteContact(UUID targetEntityKey, int contactKey) {
    delete(targetEntityKey.toString(), "contact", String.valueOf(contactKey));
  }

  @Override
  public List<Contact> listContacts(UUID targetEntityKey) {
    return get(GenericTypes.LIST_CONTACT, null, null,
      // TODO: type on contact?
      (Pageable) null, targetEntityKey.toString(), "contact");
  }

  @Override
  public int addEndpoint(UUID targetEntityKey, Endpoint endpoint) {
    return post(Integer.class, endpoint, targetEntityKey.toString(), "endpoint");
  }

  @Override
  public void deleteEndpoint(UUID targetEntityKey, int endpointKey) {
    delete(targetEntityKey.toString(), "endpoint", String.valueOf(endpointKey));
  }

  @Override
  public List<Endpoint> listEndpoints(UUID targetEntityKey) {
    return get(GenericTypes.LIST_ENDPOINT, null, null,
      // TODO: endpoint type
      (Pageable) null, targetEntityKey.toString(), "endpoint");
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
//    int tagId = post(Integer.class, machineTag, targetEntityKey.toString(), "machineTag");
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

  @Override
  public PagingResponse<T> listByMachineTag(String namespace, @Nullable String name, @Nullable String value, @Nullable Pageable page) {
    return get(pagingType, null, QueryParamBuilder.create("machineTagNamespace", namespace, "machineTagName", name, "machineTagValue", value).build(), page);
  }

  @Override
  public int addComment(UUID targetEntityKey, Comment comment) {
    return post(Integer.class, comment, targetEntityKey.toString(), "comment");
  }

  @Override
  public void deleteComment(UUID targetEntityKey, int commentKey) {
    delete(targetEntityKey.toString(), "comment", String.valueOf(commentKey));
  }

  @Override
  public List<Comment> listComments(UUID targetEntityKey) {
    return get(GenericTypes.LIST_COMMENT, null, null, (Pageable) null, targetEntityKey.toString(), "comment");
  }

  @Override
  public int addIdentifier(UUID targetEntityKey, Identifier identifier) {
    return post(Integer.class, identifier, targetEntityKey.toString(), "identifier");
  }

  @Override
  public void deleteIdentifier(UUID targetEntityKey, int identifierKey) {
    delete(targetEntityKey.toString(), "identifier", String.valueOf(identifierKey));
  }

  @Override
  public List<Identifier> listIdentifiers(UUID targetEntityKey) {
    return get(GenericTypes.LIST_IDENTIFIER, null, null,
      // TODO: identifier type
      (Pageable) null, targetEntityKey.toString(), "identifier");
  }

  @Override
  public PagingResponse<T> listByIdentifier(IdentifierType type, String identifier, Pageable page) {
    return get(pagingType, null, QueryParamBuilder.create("identifier", identifier, "identifierType", type).build(),
      page);
  }

  @Override
  public PagingResponse<T> listByIdentifier(String identifier, Pageable page) {
    return get(pagingType, null, QueryParamBuilder.create("identifier", identifier).build(),
      page);
  }
}
