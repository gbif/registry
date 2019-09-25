package org.gbif.registry.ws.client.retrofit;

import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Tag;
import retrofit2.Call;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BaseNetworkEntityRetrofitClient<T extends NetworkEntity> {

  default Call<UUID> create(T entity) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Void> delete(UUID key) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<T> get(UUID key) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Map<UUID, String>> getTitles(Collection<UUID> collection) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Void> update(UUID key, T entity) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Integer> addComment(UUID targetEntityKey, Comment comment) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Void> deleteComment(UUID targetEntityKey, int commentKey) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<List<Comment>> listComments(UUID targetEntityKey) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Integer> addMachineTag(UUID targetEntityKey, MachineTag machineTag) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Void> deleteMachineTag(UUID targetEntityKey, int machineTagKey) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Void> deleteMachineTags(UUID targetEntityKey, String namespace) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Void> deleteMachineTags(UUID targetEntityKey,
                                       String namespace,
                                       String name) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<List<MachineTag>> listMachineTags(UUID targetEntityKey) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Integer> addTag(UUID targetEntityKey, Tag tag) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Void> deleteTag(UUID taggedEntityKey, int tagKey) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<List<Tag>> listTags(UUID taggedEntityKey, String owner) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Integer> addContact(UUID targetEntityKey, Contact contact) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Void> updateContact(UUID targetEntityKey, int contactKey, Contact contact) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Void> deleteContact(UUID targetEntityKey, int contactKey) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<List<Contact>> listContacts(UUID targetEntityKey) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Integer> addEndpoint(UUID targetEntityKey, Endpoint endpoint) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Void> deleteEndpoint(UUID targetEntityKey, int endpointKey) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<List<Endpoint>> listEndpoints(UUID targetEntityKey) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Integer> addIdentifier(UUID targetEntityKey, Identifier identifier) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<Void> deleteIdentifier(UUID targetEntityKey, int identifierKey) {
    throw new UnsupportedOperationException("not implemented");
  }

  default Call<List<Identifier>> listIdentifiers(UUID targetEntityKey) {
    throw new UnsupportedOperationException("not implemented");
  }
}
