package org.gbif.registry.ws.it.collections.resource;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.client.collections.CollectionClient;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;

public class CollectionResourceIT extends BaseResourceTest {

  private final CollectionClient collectionClient;
  @MockBean private CollectionService collectionService;

  @Autowired
  public CollectionResourceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer,
      @LocalServerPort int localServerPort,
      KeyStore keyStore) {
    super(simplePrincipalProvider, esServer);
    this.collectionClient = prepareClient(localServerPort, keyStore, CollectionClient.class);
  }

  @Test
  public void getTest() {
    Collection c1 = new Collection();
    c1.setKey(UUID.randomUUID());
    c1.setCode("c1");
    c1.setName("n1");
    Mockito.when(collectionService.getCollectionView(c1.getKey()))
        .thenReturn(new CollectionView(c1));

    Collection fetched = collectionClient.get(c1.getKey());
    Assertions.assertEquals(c1.getCode(), fetched.getCode());
  }
}
