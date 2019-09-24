package org.gbif.registry.ws.client.retrofit;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Tag;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import javax.annotation.Nullable;
import javax.websocket.server.PathParam;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// TODO: 23/09/2019 specify mapping, path and query params, headers adn stuff
public interface NetworkRetrofitClient extends BaseNetworkEntityRetrofitClient<Network> {

  @GET("network/{key}/constituents")
  Call<PagingResponse<Dataset>> listConstituents(@Path("key") UUID networkKey, @Nullable Pageable page);

  @POST("network/{key}/constituents/{datasetKey}")
  Call<Void> addConstituent(@Path("key") UUID networkKey, @Path("datasetKey") UUID datasetKey);

  @DELETE("network/{key}/constituents/{datasetKey}")
  Call<Void> removeConstituent(@Path("key") UUID networkKey, @Path("datasetKey") UUID datasetKey);

  @POST("network")
  @Override
  Call<UUID> create(@Body Network entity);

  @DELETE("network/{key}")
  @Override
  Call<Void> delete(@Path("key") UUID key);

  @GET("network/{key}")
  @Override
  Call<Network> get(@Path("key") UUID key);

  @POST("network/titles")
  @Override
  Call<Map<UUID, String>> getTitles(@Body Collection<UUID> collection);

  @PUT("network/{key}")
  @Override
  Call<Void> update(@Path("key") UUID key, @Body Network entity);

  @POST("network/{key}/comment")
  @Override
  Call<Integer> addComment(@Path("key") UUID targetEntityKey, @Body Comment comment);

  @DELETE("network/{key}/comment/{commentKey}")
  @Override
  Call<Void> deleteComment(@Path("key") UUID targetEntityKey, @Path("commentKey") int commentKey);

  @GET("network/{key}/comment")
  @Override
  Call<List<Comment>> listComments(@Path("key") UUID targetEntityKey);

  @POST("network/{key}/machineTag")
  @Override
  Call<Integer> addMachineTag(@Path("key") UUID targetEntityKey, @Body MachineTag machineTag);

  // TODO: 24/09/2019 check {machineTagKey: [0-9]+} (now it's duplicates deleteMachineTags) see current resource implementation
  @DELETE("network/{key}/machineTag/{machineTagKey}")
  @Override
  Call<Void> deleteMachineTag(@Path("key") UUID targetEntityKey, @Path("machineTagKey") int machineTagKey);

  @DELETE("network/{key}/machineTag/{namespace}")
  @Override
  Call<Void> deleteMachineTags(@Path("key") UUID targetEntityKey, @Path("namespace") String namespace);

  @DELETE("network/{key}/machineTag/{namespace}/{name}")
  @Override
  Call<Void> deleteMachineTags(@Path("key") UUID targetEntityKey,
                         @Path("namespace") String namespace,
                         @Path("name") String name);

  @GET("network/{key}/machineTag")
  @Override
  Call<List<MachineTag>> listMachineTags(@Path("key") UUID targetEntityKey);

  @POST("network/{key}/tag")
  @Override
  Call<Integer> addTag(@Path("key") UUID targetEntityKey, @Body Tag tag);

  @DELETE("network/{key}/tag/{tagKey}")
  @Override
  Call<Void> deleteTag(@Path("key") UUID taggedEntityKey, @Path("tagKey") int tagKey);

  @GET("network/{key}/tag")
  @Override
  Call<List<Tag>> listTags(@Path("key") UUID taggedEntityKey, @Query("owner") String owner);

  @POST("network/{key}/contact")
  @Override
  Call<Integer> addContact(@Path("key") UUID targetEntityKey, @Body Contact contact);

  @PUT("network/{key}/contact/{contactKey}")
  @Override
  Call<Void> updateContact(@Path("key") UUID targetEntityKey, @Path("contactKey") int contactKey, @Body Contact contact);

  @DELETE("network/{key}/contact/{contactKey}")
  @Override
  Call<Void> deleteContact(@Path("key") UUID targetEntityKey, @Path("contactKey") int contactKey);

  @GET("network/{key}/contact")
  @Override
  Call<List<Contact>> listContacts(@Path("key") UUID targetEntityKey);

  @POST("network/{key}/endpoint")
  @Override
  Call<Integer> addEndpoint(@Path("key") UUID targetEntityKey, @Body Endpoint endpoint);

  @DELETE("network/{key}/endpoint/{endpointKey}")
  @Override
  Call<Void> deleteEndpoint(@Path("key") UUID targetEntityKey, @Path("endpointKey") int endpointKey);

  @GET("network/{key}/endpoint")
  @Override
  Call<List<Endpoint>> listEndpoints(@Path("key") UUID targetEntityKey);

  @POST("network/{key}/identifier")
  @Override
  Call<Integer> addIdentifier(@Path("key") UUID targetEntityKey, @Body Identifier identifier);

  @DELETE("network/{key}/identifier/{identifierKey}")
  @Override
  Call<Void> deleteIdentifier(@Path("key") UUID targetEntityKey, @PathParam("identifierKey") int identifierKey);

  @GET("network/{key}/identifier")
  @Override
  Call<List<Identifier>> listIdentifiers(@Path("key") UUID targetEntityKey);
}
