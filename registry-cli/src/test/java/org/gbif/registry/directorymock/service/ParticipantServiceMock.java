package org.gbif.registry.directorymock.service;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.directory.Participant;
import org.gbif.api.service.directory.ParticipantService;

import java.util.List;

/**
 *
 */
public class ParticipantServiceMock implements ParticipantService {

  private List<Participant> participants;

  public void setParticipants(List<Participant> participants){
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
  public void update(Participant participant) {

  }

  @Override
  public void delete(Integer integer) {

  }

  @Override
  public PagingResponse<Participant> list(Pageable pageable) {
    return new PagingResponse<Participant>(new PagingRequest(), (long)participants.size(), participants);
  }
}
