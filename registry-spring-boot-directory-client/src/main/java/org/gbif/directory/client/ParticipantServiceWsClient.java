/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.directory.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.directory.Participant;
import org.gbif.api.service.directory.ParticipantService;
import org.gbif.directory.client.retrofit.JacksonObjectMapper;
import org.gbif.directory.client.retrofit.RetrofitFactory;
import org.gbif.directory.client.retrofit.directory.ParticipantServiceRetrofitClient;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import lombok.SneakyThrows;
import retrofit2.Retrofit;

public class ParticipantServiceWsClient implements ParticipantService {

  private final ParticipantServiceRetrofitClient retrofitClient;

  public ParticipantServiceWsClient(ParticipantServiceRetrofitClient retrofitClient) {
    this.retrofitClient = retrofitClient;
  }

  @Override
  @SneakyThrows
  public Participant create(@NotNull Participant participant) {
    return retrofitClient.create(participant).execute().body();
  }

  @Override
  @SneakyThrows
  public Participant get(@NotNull Integer id) {
    return retrofitClient.get(id).execute().body();
  }

  @Override
  @SneakyThrows
  public void update(@NotNull Participant entity) {
    retrofitClient.update(entity.getId(), entity).execute();
  }

  @Override
  @SneakyThrows
  public void delete(@NotNull Integer id) {
    retrofitClient.delete(id).execute();
  }

  @Override
  @SneakyThrows
  public PagingResponse<Participant> list(@Nullable String query, @Nullable Pageable page) {
    return retrofitClient.list(query, page).execute().body();
  }

  public static void main(String[] args) {
    Retrofit retrofit =
        RetrofitFactory.create("http://api.gbif-dev.org/v1/directory/", JacksonObjectMapper.get());
    ParticipantServiceWsClient wsClient =
        new ParticipantServiceWsClient(retrofit.create(ParticipantServiceRetrofitClient.class));
    PagingResponse<Participant> participants = wsClient.list(null, new PagingRequest(0, 50));
    System.out.println(participants);
  }
}
