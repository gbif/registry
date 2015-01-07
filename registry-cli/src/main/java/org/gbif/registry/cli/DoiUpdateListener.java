package org.gbif.registry.cli;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.common.messaging.AbstractMessageCallback;
import org.gbif.common.messaging.api.messages.ChangeDoiMessage;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.DoiHttpException;
import org.gbif.doi.service.DoiService;
import org.gbif.registry.persistence.mapper.DoiMapper;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Message callback implementation to take DOI updates and send them to DataCite. Updates the status of the DOI in
 * the registry database.
 */
public class DoiUpdateListener extends AbstractMessageCallback<ChangeDoiMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(DoiUpdateListener.class);
  private static final Marker DOI_SMTP = MarkerFactory.getMarker("DOI_SMTP");

  private final DoiService doiService;
  private final DoiMapper doiMapper;

  public DoiUpdateListener(DoiService doiService, DoiMapper doiMapper) {
    this.doiService = doiService;
    this.doiMapper = doiMapper;
  }

  @Override
  public void handleMessage(ChangeDoiMessage msg) {
    LOG.debug("Handling change DOI to {} message for {}", msg.getStatus(), msg.getDoi());
    final DoiData currState = doiMapper.get(msg.getDoi());
    if (currState == null) {
      // this is bad, we should have an entry for the DOI in our registry table!
      LOG.warn("Skipping unknown GBIF DOI {}", msg.getDoi());
      return;
    }

    while (true) {
      try {
        switch (msg.getStatus()) {
          case REGISTERED:
            register(msg.getDoi(), msg.getTarget(), msg.getMetadata(), currState);
            break;
          case RESERVED:
            reserve(msg.getDoi(), msg.getMetadata(), currState);
            break;
          case DELETED:
            delete(msg.getDoi(), currState);
            break;
          default:
            LOG.warn("Cannot update {} to illegal state {}.", msg.getDoi(), msg.getStatus());
            break;
        }
        break;
      } catch (DoiException e) {
        LOG.warn(DOI_SMTP, "DOI exception updating {} to {}. Trying again in 5 minutes", msg.getDoi(), msg.getStatus(),
          e);
        try {
          Thread.sleep(TimeUnit.MINUTES.toMillis(5));
        } catch (InterruptedException e1) {
          LOG.info("Interrupted eternal retry");
          break;
        }
      }
    }
  }

  private void reserve(DOI doi, String xml, DoiData currState) throws DoiException {
    doiService.reserve(doi, xml);
    DoiData newState = new DoiData(DoiStatus.RESERVED, currState.getTarget());
    doiMapper.update(doi, newState, xml);
  }

  private void delete(DOI doi, DoiData currState) throws DoiException {
    if (currState.getStatus() == null) {
      doiMapper.delete(doi);
    } else {
      try {
        boolean fullDeleted = doiService.delete(doi);
        if (fullDeleted) {
          doiMapper.delete(doi);
        } else {
          DoiData newState = new DoiData(DoiStatus.DELETED, currState.getTarget());
          doiMapper.update(doi, newState, null);
        }
      } catch (DoiHttpException e) {
        // in case of a 404 swallow
        if (e.getStatus() == 404) {
          LOG.warn(DOI_SMTP, "Trying to delete DOI {} failed because it doesn't exist in DataCite; deleting locally",
            doi, e);
          doiMapper.delete(doi);
        } else {
          throw e;
        }
      }
    }
  }

  private void register(DOI doi, URI target, String xml, DoiData currState) throws DoiException {
    doiService.register(doi, target, xml);
    DoiData newState = new DoiData(DoiStatus.REGISTERED, target);
    doiMapper.update(doi, newState, xml);
  }
}
