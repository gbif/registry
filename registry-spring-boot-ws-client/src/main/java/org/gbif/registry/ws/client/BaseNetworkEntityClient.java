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
import org.gbif.registry.ws.client.retrofit.BaseNetworkEntityRetrofitClient;
import retrofit2.Response;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.gbif.registry.ws.client.SyncCall.syncCallWithResponse;

// TODO: 25/09/2019 implements these methods (see registry's BaseNetworkEntityClient)
public class BaseNetworkEntityClient<T extends NetworkEntity> implements NetworkEntityService<T> {

  private final BaseNetworkEntityRetrofitClient<T> client;

  public BaseNetworkEntityClient(BaseNetworkEntityRetrofitClient<T> client) {
    this.client = client;
  }

  @Override
  public UUID create(@NotNull T t) {
    final Response<UUID> response = syncCallWithResponse(client.create(t));
    return response.body();
  }

  @Override
  public void delete(@NotNull UUID uuid) {
    syncCallWithResponse(client.delete(uuid));
  }

  @Override
  public T get(@NotNull UUID uuid) {
    final Response<T> response = syncCallWithResponse(client.get(uuid));
    return response.body();
  }

  @Override
  public Map<UUID, String> getTitles(Collection<UUID> collection) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public PagingResponse<T> list(@Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public PagingResponse<T> search(String s, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public PagingResponse<T> listByIdentifier(IdentifierType identifierType, String s, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public PagingResponse<T> listByIdentifier(String s, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public PagingResponse<T> listByMachineTag(String s, @Nullable String s1, @Nullable String s2, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void update(@NotNull T t) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public int addComment(@NotNull UUID uuid, @NotNull Comment comment) {
    final Response<Integer> response = syncCallWithResponse(client.addComment(uuid, comment));
    return response.body();
  }

  @Override
  public void deleteComment(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public List<Comment> listComments(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public int addContact(@NotNull UUID uuid, @NotNull Contact contact) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void deleteContact(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public List<Contact> listContacts(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void updateContact(@NotNull UUID uuid, @NotNull Contact contact) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public int addEndpoint(@NotNull UUID uuid, @NotNull Endpoint endpoint) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void deleteEndpoint(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public List<Endpoint> listEndpoints(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public int addIdentifier(@NotNull UUID uuid, @NotNull Identifier identifier) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void deleteIdentifier(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public List<Identifier> listIdentifiers(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull MachineTag machineTag) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull TagName tagName, @NotNull String s) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull String s, @NotNull String s1, @NotNull String s2) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void deleteMachineTag(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull TagNamespace tagNamespace) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull String s) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull TagName tagName) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull String s, @NotNull String s1) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public List<MachineTag> listMachineTags(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull String s) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull Tag tag) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void deleteTag(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public List<Tag> listTags(@NotNull UUID uuid, @Nullable String s) {
    throw new UnsupportedOperationException("not implemented");
  }
}
