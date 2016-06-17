package org.gbif.registry.cli;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.common.messaging.api.messages.ChangeDoiMessage;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.DoiExistsException;
import org.gbif.doi.service.DoiService;
import org.gbif.registry.persistence.mapper.DoiMapper;

import java.net.URI;
import javax.annotation.Nullable;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DoiUpdateListenerTest {

  class MockImpl implements DoiService {

    private DOI lastRegisteredDOI = null;

    public DOI getLastRegisteredDOI() {
      return lastRegisteredDOI;
    }

    @Nullable
    @Override
    public DoiData resolve(DOI doi) throws DoiException {
      throw new DoiException("Not implemented yet");
    }

    @Override
    public boolean exists(DOI doi) throws DoiException {
      throw new DoiException("Not implemented yet");
    }

    @Override
    public String getMetadata(DOI doi) throws DoiException {
      throw new DoiException("Not implemented yet");
    }

    @Override
    public void reserve(DOI doi, String metadata) throws DoiException {
      throw new DoiException("Not implemented yet");
    }

    @Override
    public void reserve(DOI doi, DataCiteMetadata metadata) throws DoiExistsException, DoiException {
      throw new DoiException("Not implemented yet");
    }

    @Override
    public void register(DOI doi, URI target, String metadata) throws DoiException {
      this.lastRegisteredDOI = doi;
    }

    @Override
    public void register(DOI doi, URI target, DataCiteMetadata metadata) throws DoiExistsException, DoiException {
      this.lastRegisteredDOI = doi;
    }

    @Override
    public boolean delete(DOI doi) throws DoiException {
      throw new DoiException("Not implemented yet");
    }

    @Override
    public void update(DOI doi, String metadata) throws DoiException {
      throw new DoiException("Not implemented yet");
    }

    @Override
    public void update(DOI doi, DataCiteMetadata metadata) throws DoiException {
      throw new DoiException("Not implemented yet");
    }

    @Override
    public void update(DOI doi, URI target) throws DoiException {
      throw new DoiException("Not implemented yet");
    }
  }

  @Test
  public void testHandleMessage() throws Exception {
    DOI testDOI = new DOI("10.5072/1234");
    DoiMapper doiMapper = mock(DoiMapper.class);
    when(doiMapper.get(any(DOI.class))).thenReturn(new DoiData(DoiStatus.NEW, null));

    MockImpl mockService = new MockImpl();
    DoiUpdateListener listener = new DoiUpdateListener(mockService, doiMapper, 10);
    ChangeDoiMessage msg = new ChangeDoiMessage(DoiStatus.REGISTERED,
            testDOI, "", URI.create("http:??gbif.org"));
    listener.handleMessage(msg);
    assertEquals(testDOI, mockService.getLastRegisteredDOI());
  }

}