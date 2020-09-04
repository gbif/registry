package org.gbif.registry.doi;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.registry.persistence.mapper.DoiMapper;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class DoiDirectManagingServiceImpl implements DoiDirectManagingService {

  private final DoiMapper doiMapper;

  public DoiDirectManagingServiceImpl(DoiMapper doiMapper) {
    this.doiMapper = doiMapper;
  }

  @Override
  public void failed(DOI doi, InvalidMetadataException e) {
    // Updates the doi table to FAILED status and uses the error stacktrace as the xml for
    // debugging.
    doiMapper.update(doi, new DoiData(DoiStatus.FAILED, null), ExceptionUtils.getStackTrace(e));
  }

  @Override
  public void update(DOI doi, DataCiteMetadata metadata, URI target) {
    try {
      DoiData doiData = new DoiData(DoiStatus.NEW, target);
      String xmlMetadata = DataCiteValidator.toXml(doi, metadata);
      doiMapper.update(doi, doiData, xmlMetadata);
    } catch (InvalidMetadataException e) {
      failed(doi, e);
    }
  }
}
