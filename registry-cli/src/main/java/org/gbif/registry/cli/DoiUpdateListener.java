package org.gbif.registry.cli;

import org.gbif.api.model.common.DOI;
import org.gbif.common.messaging.AbstractMessageCallback;
import org.gbif.common.messaging.api.messages.ChangeDoiMessage;
import org.gbif.doi.service.DoiService;
import org.gbif.registry.persistence.mapper.DoiMapper;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message callback implementation to take DOI updates and send them to DataCite. Updates the status of the DOI in
 * the registry database.
 */
public class DoiUpdateListener extends AbstractMessageCallback<ChangeDoiMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(DoiUpdateListener.class);

  private final DoiService doiService;
  private final DoiMapper doiMapper;

  public DoiUpdateListener(DoiService doiService, DoiMapper doiMapper) {
    this.doiService = doiService;
    this.doiMapper = doiMapper;
  }

  @Override
  public void handleMessage(ChangeDoiMessage msg) {
    LOG.debug("Got change doi message");
    switch (msg.getStatus()) {
      case REGISTERED:
      case RESERVED:
      case DELETED:
    }
  }

  private void register(DOI doi, URI target, String xml) {

  }
}
