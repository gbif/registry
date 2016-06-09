package org.gbif.registry.cli;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.common.messaging.AbstractMessageCallback;
import org.gbif.common.messaging.api.messages.ChangeDoiMessage;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.DoiExistsException;
import org.gbif.doi.service.DoiHttpException;
import org.gbif.doi.service.DoiService;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.registry.persistence.mapper.DoiMapper;
import org.gbif.registry.doi.DataCiteConverter;

import java.net.URI;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Message callback implementation to take DOI updates and send them to DataCite.
 * Updates the status of the DOI in the registry database.
 */
public class DoiUpdateListener extends AbstractMessageCallback<ChangeDoiMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(DoiUpdateListener.class);
  private static final Marker DOI_SMTP = MarkerFactory.getMarker("DOI_SMTP");
  private static final int MAX_RETRY = 4;
  private final long timeToRetryInMs;

  private final DoiService doiService;
  private final DoiMapper doiMapper;

  public DoiUpdateListener(DoiService doiService, DoiMapper doiMapper, long timeToRetryInMs) {
    this.doiService = doiService;
    this.doiMapper = doiMapper;
    this.timeToRetryInMs = timeToRetryInMs;
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

    boolean descriptionTruncated = false;
    for (int retry = 1; retry < MAX_RETRY; retry++) {
      try {
        switch (msg.getStatus()) {
          case REGISTERED:
            registerOrUpdate(msg.getDoi(), msg.getTarget(), msg.getMetadata(), currState);
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

      } catch (DoiExistsException e) {
        writeFailedStatus(msg.getDoi(), msg.getTarget(), msg.getMetadata());
        LOG.warn(DOI_SMTP, "DOI {} existed already when trying to change status to {}. Ignore", msg.getDoi(), msg.getStatus(), e);
        break;

      } catch (DoiHttpException e) {
        writeFailedStatus(msg.getDoi(), msg.getTarget(), msg.getMetadata());
        if (HttpStatus.SC_REQUEST_TOO_LONG == e.getStatus()) {
          LOG.warn(DOI_SMTP, "Metadata of length {} is exceeding max datacite limit in attempt #{} "
                          + "while updating {} to {} with target {}. "
                          + "Trying again {}",
                  msg.getMetadata().length(), retry, msg.getDoi(), msg.getStatus(), msg.getTarget(),
                  descriptionTruncated ? "without constituent information" : "with truncated description", e);
          try {
            String truncatedXml;
            if (descriptionTruncated) {
              LOG.warn("Truncating all constituent relations as last resort from metadata for DOI {}", msg.getDoi());
              truncatedXml = DataCiteConverter.truncateConstituents(msg.getDoi(), msg.getMetadata(), msg.getTarget());
            } else {
              LOG.debug("Original metadata for DOI {}:\n\n{}", msg.getDoi(), msg.getMetadata());
              truncatedXml = DataCiteConverter.truncateDescription(msg.getDoi(), msg.getMetadata(), msg.getTarget());
              descriptionTruncated = true;
            }
            msg = new ChangeDoiMessage(msg.getStatus(), msg.getDoi(), truncatedXml, msg.getTarget());
          } catch (InvalidMetadataException e1) {
            LOG.warn("Failed to deserialize xml metadata for DOI {}", msg.getDoi(), e1);
          }
        } else {
          LOG.warn(DOI_SMTP, "DOI http {} exception updating {} to {} with target {}. Attempt #{}",
                  e.getStatus(), msg.getDoi(), msg.getStatus(), msg.getTarget(), retry, e);
          sleep();
        }

      } catch (DoiException e) {
        writeFailedStatus(msg.getDoi(), msg.getTarget(), msg.getMetadata());
        LOG.warn(DOI_SMTP, "DOI exception updating {} to {} with target {}. Attempt #{}",
                msg.getDoi(), msg.getStatus(), msg.getTarget(), retry, e);
        sleep();
      }
    }
  }

  private void sleep() {
    try {
      Thread.sleep(timeToRetryInMs);
    } catch (InterruptedException e1) {
      LOG.info("Interrupted retries");
    }
  }

  private void writeFailedStatus(DOI doi, URI target, String xml) {
    doiMapper.update(doi, new DoiData(DoiStatus.FAILED, target), xml);
  }

  private void reserve(DOI doi, String xml, DoiData currState) throws DoiException {
    doiService.reserve(doi, xml);
    LOG.info("Reserved doi {}", doi);
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
          LOG.info("Deleted doi {}", doi);
        } else {
          DoiData newState = new DoiData(DoiStatus.DELETED, currState.getTarget());
          doiMapper.update(doi, newState, null);
          LOG.info("Marked registered doi {} as deleted", doi);
        }
      } catch (DoiHttpException e) {
        // in case of a 404 swallow
        if (HttpStatus.SC_NOT_FOUND == e.getStatus()) {
          LOG.warn(DOI_SMTP, "Trying to delete DOI {} failed because it doesn't exist in DataCite; deleting locally",
                  doi, e);
          doiMapper.delete(doi);
        } else {
          throw e;
        }
      }
    }
  }

  /**
   * Register or Update the DOI with the DOI Service (Datacite).
   *
   * @param currState current state of the DOI in the database
   *
   * @throws DoiException
   */
  private void registerOrUpdate(DOI doi, URI target, String xml, DoiData currState) throws DoiException {
    final DoiStatus doiStatus = currState.getStatus();
    boolean registered = true;
    LOG.info("registerOrUpdate DOI {} with state {}", doi, currState.getStatus());
    switch (doiStatus) {
      case REGISTERED:
        // the DOI was already registered, so we only need to update the target url if changed and the metadata
        // TODO review this part, is it useful?
        if (!target.equals(currState.getTarget())) {
          doiService.update(doi, target);
        }
        doiService.update(doi, xml);
        LOG.info("Updated doi {} with target {}", doi, target);
        break;
      case NEW:
      case RESERVED:
        doiService.register(doi, target, xml);
        LOG.info("Registered doi {} with target {}", doi, target);
        break;
      case FAILED:
        registered = retryRegisterOrUpdate(doi, target, xml);
        break;
      default:
        LOG.warn("Can't register or update the DOI {} with state {}", doi, doiStatus);
    }
    // store the new state in our registry
    if(registered) {
      doiMapper.update(doi, new DoiData(DoiStatus.REGISTERED, target), xml);
    }
  }

  /**
   * Retry to Register or Update a DOI flagged as "FAILED" in the database.
   * Do not use this method to fix a RESERVED DOI that should be updated (will be rejected).
   * As opposed to registerOrUpdate, this method will ask the doiService for the status of
   * the DOI since when the status is FAILED we loose the 'real' status before the failure.
   * If the DOI doesn't exist on the DOI Service (e.g. Datacite) it will register it.
   * If the DOI already exist it will try an update.
   * If any error occurs it will be logged and the will method with false.
   *
   * @throws DoiException
   * @return true if this method is able to retry the registration/update, false otherwise
   */
  private boolean retryRegisterOrUpdate(DOI doi, URI target, String xml) throws DoiException {
    // Check if the DOI is known by the DOI service. Known means RESERVED or REGISTERED.
    if (doiService.exists(doi)) {
      //check the latest status from the DoiService
      DoiData doiServiceData = doiService.resolve(doi);
      // for the moment we only deal with REGISTERED status
      if (DoiStatus.REGISTERED == doiServiceData.getStatus()) {
        doiService.update(doi, xml);
        LOG.info("Updated doi {} with target {}", doi, target);
      } else {
        LOG.info("Failed to update doi {} with target {}. Only doi with state REGISTERED can be retried. Datacite status: {}. ", doi, target, doiServiceData.getStatus());
        return false;
      }
    } else {
      doiService.register(doi, target, xml);
      LOG.info("Registered doi {} with target {}", doi, target);
    }
    return true;
  }

}
