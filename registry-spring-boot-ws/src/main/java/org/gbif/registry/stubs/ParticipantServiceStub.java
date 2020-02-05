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
package org.gbif.registry.stubs;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.directory.Participant;
import org.gbif.api.service.directory.ParticipantService;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Service;

@Service
public class ParticipantServiceStub implements ParticipantService {

  @Override
  public Participant create(@NotNull Participant participant) {
    throw new UnsupportedOperationException(
        "ParticipantService#create not implemented yet (directory-ws needed)");
  }

  @Override
  public Participant get(@NotNull Integer integer) {
    throw new UnsupportedOperationException(
        "ParticipantService#get not implemented yet (directory-ws needed)");
  }

  @Override
  public void update(@NotNull Participant participant) {
    throw new UnsupportedOperationException(
        "ParticipantService#update not implemented yet (directory-ws needed)");
  }

  @Override
  public void delete(@NotNull Integer integer) {
    throw new UnsupportedOperationException(
        "ParticipantService#delete not implemented yet (directory-ws needed)");
  }

  @Override
  public PagingResponse<Participant> list(@Nullable String s, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException(
        "ParticipantService#list not implemented yet (directory-ws needed)");
  }
}
