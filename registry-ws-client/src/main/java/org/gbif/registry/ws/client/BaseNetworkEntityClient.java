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
import org.gbif.ws.client.BaseWsGetClient;
import org.gbif.ws.client.QueryParamBuilder;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.google.common.base.Preconditions;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;

public class BaseNetworkEntityClient<T extends NetworkEntity> extends BaseWsGetClient<T, UUID>
  implements NetworkEntityService<T> {

  private final GenericType<PagingResponse<T>> pagingType;

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
    return post(Integer.class, machineTag, targetEntityKey.toString(), "machineTag");
  }

  @Override
  public int addMachineTag(
    @NotNull UUID targetEntityKey, @NotNull String namespace, @NotNull String name, @NotNull String value) {
    return addMachineTag(targetEntityKey, new MachineTag(namespace, name, value));
  }

  @Override
  public void deleteMachineTag(UUID targetEntityKey, int machineTagKey) {
    delete(targetEntityKey.toString(), "machineTag", String.valueOf(machineTagKey));
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull String namespace) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull String namespace, @NotNull String name) {
    // TODO: Write implementation
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public List<MachineTag> listMachineTags(UUID targetEntityKey) {
    return get(GenericTypes.LIST_MACHINETAG, null, null, (Pageable) null, targetEntityKey.toString(), "machineTag");
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
