package org.gbif.registry.cli;

import org.gbif.common.messaging.AbstractMessageCallback;
import org.gbif.doi.service.DoiService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message callback implementation to take DOI updates and send them to DataCite. Updates the status of the DOI in
 * the registry database.
 */
public class DoiUpdateListener extends AbstractMessageCallback<DoiUpdateMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(DoiUpdateListener.class);

  private final DoiService doiService;

  public DoiUpdateListener(DoiService doiService) {
    this.doiService = doiService;
  }

  @Override
  public void handleMessage(DoiUpdateMessage doiUpdateMessage) {
    LOG.debug("Got doi update message");
  }
}
