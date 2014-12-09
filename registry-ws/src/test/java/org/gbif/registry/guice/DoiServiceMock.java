package org.gbif.registry.guice;

import org.gbif.api.model.common.DOI;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.DoiService;
import org.gbif.doi.service.DoiStatus;

import java.net.URI;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.RandomUtils;

/**
 * Class that implements a mock doi service that verifies the presence of mandatory fields.
 */
public class DoiServiceMock implements DoiService {
  public static final URI MOCK_TARGET = URI.create("http://www.gbif.org");

  @Nullable
  @Override
  public DoiStatus resolve(DOI doi) throws DoiException {
    return new DoiStatus(DoiStatus.Status.REGISTERED, MOCK_TARGET);
  }

  @Override
  public void reserve(DOI doi, DataCiteMetadata metadata) throws DoiException {
    requireMandatoryFields(metadata);
  }

  @Override
  public DOI reserveRandom(String prefix, String shoulder, int length, DataCiteMetadata metadata) throws DoiException {
    requireMandatoryFields(metadata);
    // dont do nothing
    return new DOI(prefix, shoulder + RandomUtils.nextInt(0, 10*length));
  }

  @Override
  public void register(DOI doi, URI target, DataCiteMetadata metadata) throws DoiException {
    requireMandatoryFields(metadata);
    Preconditions.checkNotNull(target);
  }

  @Override
  public boolean delete(DOI doi) throws DoiException {
    // dont do nothing
    return false;
  }

  @Override
  public void update(DOI doi, DataCiteMetadata metadata) throws DoiException {
    requireMandatoryFields(metadata);
  }

  @Override
  public void update(DOI doi, URI target) throws DoiException {
    Preconditions.checkNotNull(target);
  }

  private void requireMandatoryFields(DataCiteMetadata m) {
    Preconditions.checkArgument(!m.getCreators().getCreator().isEmpty(), "Creator required");
    Preconditions.checkArgument(!m.getTitles().getTitle().isEmpty(), "Title required");
    Preconditions.checkNotNull(m.getPublicationYear(), "Publication year required");
    Preconditions.checkNotNull(m.getPublisher(), "Publisher required");
  }
}
