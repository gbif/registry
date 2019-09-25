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

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// TODO: 25/09/2019 implements these methods (see registry's BaseNetworkEntityClient)
public class BaseNetworkEntityClient<T extends NetworkEntity> implements NetworkEntityService<T> {

  @Override
  public UUID create(@NotNull T t) {
    return null;
  }

  @Override
  public void delete(@NotNull UUID uuid) {

  }

  @Override
  public T get(@NotNull UUID uuid) {
    return null;
  }

  @Override
  public Map<UUID, String> getTitles(Collection<UUID> collection) {
    return null;
  }

  @Override
  public PagingResponse<T> list(@Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<T> search(String s, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<T> listByIdentifier(IdentifierType identifierType, String s, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<T> listByIdentifier(String s, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<T> listByMachineTag(String s, @Nullable String s1, @Nullable String s2, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public void update(@NotNull T t) {

  }

  @Override
  public int addComment(@NotNull UUID uuid, @NotNull Comment comment) {
    return 0;
  }

  @Override
  public void deleteComment(@NotNull UUID uuid, int i) {

  }

  @Override
  public List<Comment> listComments(@NotNull UUID uuid) {
    return null;
  }

  @Override
  public int addContact(@NotNull UUID uuid, @NotNull Contact contact) {
    return 0;
  }

  @Override
  public void deleteContact(@NotNull UUID uuid, int i) {

  }

  @Override
  public List<Contact> listContacts(@NotNull UUID uuid) {
    return null;
  }

  @Override
  public void updateContact(@NotNull UUID uuid, @NotNull Contact contact) {

  }

  @Override
  public int addEndpoint(@NotNull UUID uuid, @NotNull Endpoint endpoint) {
    return 0;
  }

  @Override
  public void deleteEndpoint(@NotNull UUID uuid, int i) {

  }

  @Override
  public List<Endpoint> listEndpoints(@NotNull UUID uuid) {
    return null;
  }

  @Override
  public int addIdentifier(@NotNull UUID uuid, @NotNull Identifier identifier) {
    return 0;
  }

  @Override
  public void deleteIdentifier(@NotNull UUID uuid, int i) {

  }

  @Override
  public List<Identifier> listIdentifiers(@NotNull UUID uuid) {
    return null;
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull MachineTag machineTag) {
    return 0;
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull TagName tagName, @NotNull String s) {
    return 0;
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull String s, @NotNull String s1, @NotNull String s2) {
    return 0;
  }

  @Override
  public void deleteMachineTag(@NotNull UUID uuid, int i) {

  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull TagNamespace tagNamespace) {

  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull String s) {

  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull TagName tagName) {

  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull String s, @NotNull String s1) {

  }

  @Override
  public List<MachineTag> listMachineTags(@NotNull UUID uuid) {
    return null;
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull String s) {
    return 0;
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull Tag tag) {
    return 0;
  }

  @Override
  public void deleteTag(@NotNull UUID uuid, int i) {

  }

  @Override
  public List<Tag> listTags(@NotNull UUID uuid, @Nullable String s) {
    return null;
  }
}
