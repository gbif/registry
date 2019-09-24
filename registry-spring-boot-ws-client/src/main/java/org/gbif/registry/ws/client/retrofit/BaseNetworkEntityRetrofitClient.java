package org.gbif.registry.ws.client.retrofit;

import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Tag;
import retrofit2.Call;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BaseNetworkEntityRetrofitClient<T> {

  Call<UUID> create(T entity);

  Call<Void> delete(UUID key);

  Call<T> get(UUID key);

  Call<Map<UUID, String>> getTitles(Collection<UUID> collection);

  Call<Void> update(UUID key, T entity);

  Call<Integer> addComment(UUID targetEntityKey, Comment comment);

  Call<Void> deleteComment(UUID targetEntityKey, int commentKey);

  Call<List<Comment>> listComments(UUID targetEntityKey);

  Call<Integer> addMachineTag(UUID targetEntityKey, MachineTag machineTag);

  Call<Void> deleteMachineTag(UUID targetEntityKey, int machineTagKey);

  Call<Void> deleteMachineTags(UUID targetEntityKey, String namespace);

  Call<Void> deleteMachineTags(UUID targetEntityKey,
                               String namespace,
                               String name);

  Call<List<MachineTag>> listMachineTags(UUID targetEntityKey);

  Call<Integer> addTag(UUID targetEntityKey, Tag tag);

  Call<Void> deleteTag(UUID taggedEntityKey, int tagKey);

  Call<List<Tag>> listTags(UUID taggedEntityKey, String owner);

  Call<Integer> addContact(UUID targetEntityKey, Contact contact);

  Call<Void> updateContact(UUID targetEntityKey, int contactKey, Contact contact);

  Call<Void> deleteContact(UUID targetEntityKey, int contactKey);

  Call<List<Contact>> listContacts(UUID targetEntityKey);

  Call<Integer> addEndpoint(UUID targetEntityKey, Endpoint endpoint);

  Call<Void> deleteEndpoint(UUID targetEntityKey, int endpointKey);

  Call<List<Endpoint>> listEndpoints(UUID targetEntityKey);

  Call<Integer> addIdentifier(UUID targetEntityKey, Identifier identifier);

  Call<Void> deleteIdentifier(UUID targetEntityKey, int identifierKey);

  Call<List<Identifier>> listIdentifiers(UUID targetEntityKey);
}
