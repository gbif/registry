package org.gbif.registry.ws.it;

import org.gbif.api.model.common.DOI;
import org.gbif.registry.domain.ws.Citation;
import org.gbif.registry.domain.ws.CitationCreationRequest;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.resources.CitationResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;

import java.net.URI;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CitationIT extends BaseItTest {

  private final CitationResource citationResource;

  @Autowired
  public CitationIT(
      CitationResource citationResource,
      @Nullable SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer) {
    super(simplePrincipalProvider, esServer);
    this.citationResource = citationResource;
  }

  @Test
  public void createCitation() {
    CitationCreationRequest requestData = new CitationCreationRequest();
    requestData.setTitle("Let's test citation");
    requestData.setOriginalDownloadDOI(new DOI("10.21373/55555"));
    requestData.setCreator("it");
    requestData.setTarget(URI.create("https://www.gbif.org"));
    requestData.setRelatedDatasets(Arrays.asList(UUID.randomUUID().toString(), new DOI("10.21373/44444").toString()));

    Citation citation = citationResource.createCitation(requestData);

    Citation actual = citationResource.getCitation(citation.getDoi().getPrefix(), citation.getDoi().getSuffix());

    assertNotNull(actual);
    assertEquals(citation.getDoi(), actual.getDoi());
    assertEquals(requestData.getOriginalDownloadDOI(), actual.getOriginalDownloadDOI());
    assertEquals(requestData.getTarget(), actual.getTarget());
    assertEquals(requestData.getTitle(), actual.getTitle());
    assertNotNull(actual.getCreated());
    assertNotNull(actual.getCreatedBy());
    assertNotNull(actual.getModified());
    assertNotNull(actual.getModifiedBy());
  }
}
