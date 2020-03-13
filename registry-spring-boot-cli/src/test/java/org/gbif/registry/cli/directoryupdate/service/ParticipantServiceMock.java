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
package org.gbif.registry.cli.directoryupdate.service;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.directory.Participant;
import org.gbif.api.service.directory.ParticipantService;

import java.util.List;

/** */
public class ParticipantServiceMock implements ParticipantService {

  private List<Participant> participants;

  public void setParticipants(List<Participant> participants) {
    this.participants = participants;
  }

  @Override
  public Participant create(Participant participant) {
    return null;
  }

  @Override
  public Participant get(Integer integer) {
    return null;
  }

  @Override
  public void update(Participant participant) {}

  @Override
  public void delete(Integer integer) {}

  @Override
  public PagingResponse<Participant> list(String q, Pageable pageable) {
    return new PagingResponse<Participant>(
        new PagingRequest(), (long) participants.size(), participants);
  }
}
