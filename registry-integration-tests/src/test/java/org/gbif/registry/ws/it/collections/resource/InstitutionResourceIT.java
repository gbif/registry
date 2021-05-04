package org.gbif.registry.ws.it.collections.resource;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.ContactService;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.OccurrenceMappingService;
import org.gbif.api.service.registry.CommentService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.MachineTagService;
import org.gbif.api.service.registry.TagService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.client.collections.InstitutionClient;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class InstitutionResourceIT extends PrimaryCollectionEntityResourceIT<Institution> {

  @MockBean private InstitutionService institutionService;

  @Autowired
  public InstitutionResourceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer,
      @LocalServerPort int localServerPort) {
    super(
        InstitutionClient.class,
        simplePrincipalProvider,
        esServer,
        Institution.class,
        localServerPort);
  }

  @Test
  public void listTest() {
    Institution i1 = testData.newEntity();
    Institution i2 = testData.newEntity();
    List<Institution> institutions = Arrays.asList(i1, i2);

    when(institutionService.list(any(InstitutionSearchRequest.class)))
        .thenReturn(
            new PagingResponse<>(
                new PagingRequest(), Long.valueOf(institutions.size()), institutions));

    PagingResponse<Institution> result = getClient().list(new InstitutionSearchRequest());
    assertEquals(institutions.size(), result.getResults().size());
  }

  @Test
  public void testSuggest() {
    KeyCodeNameResult r1 = new KeyCodeNameResult(UUID.randomUUID(), "c1", "n1");
    KeyCodeNameResult r2 = new KeyCodeNameResult(UUID.randomUUID(), "c2", "n2");
    List<KeyCodeNameResult> results = Arrays.asList(r1, r2);

    when(institutionService.suggest(anyString())).thenReturn(results);
    assertEquals(2, getClient().suggest("foo").size());
  }

  @Test
  public void listDeletedTest() {
    Institution i1 = testData.newEntity();
    i1.setKey(UUID.randomUUID());
    i1.setCode("code1");
    i1.setName("Institution name");

    Institution i2 = testData.newEntity();
    i2.setKey(UUID.randomUUID());
    i2.setCode("code2");
    i2.setName("Institution name2");

    List<Institution> institutions = Arrays.asList(i1, i2);

    when(institutionService.listDeleted(any(Pageable.class)))
        .thenReturn(
            new PagingResponse<>(
                new PagingRequest(), Long.valueOf(institutions.size()), institutions));

    PagingResponse<Institution> result = getClient().listDeleted(new PagingRequest());
    assertEquals(institutions.size(), result.getResults().size());
  }

  // TODO: duplicates, merge, suggestions

  @Override
  protected CrudService<Institution> getMockCrudService() {
    return institutionService;
  }

  @Override
  protected TagService getMockTagService() {
    return institutionService;
  }

  @Override
  protected MachineTagService getMockMachineTagService() {
    return institutionService;
  }

  @Override
  protected IdentifierService getMockIdentifierService() {
    return institutionService;
  }

  @Override
  protected CommentService getMockCommentService() {
    return institutionService;
  }

  @Override
  protected ContactService getMockContactService() {
    return institutionService;
  }

  @Override
  protected OccurrenceMappingService getMockOccurrenceMappingService() {
    return institutionService;
  }

  protected InstitutionClient getClient() {
    return (InstitutionClient) baseClient;
  }
}
