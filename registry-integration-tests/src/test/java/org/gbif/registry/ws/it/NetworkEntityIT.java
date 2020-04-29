/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.it;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Contactable;
import org.gbif.api.model.registry.Endpointable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.registry.CommentService;
import org.gbif.api.service.registry.ContactService;
import org.gbif.api.service.registry.EndpointService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.MachineTagService;
import org.gbif.api.service.registry.NetworkEntityService;
import org.gbif.api.service.registry.TagService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.security.AccessControlException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.ConstraintViolationException;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static org.gbif.registry.ws.it.LenientAssert.assertLenientEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A generic test for all network entities that implement all interfaces required by the
 * BaseNetworkEntityResource.
 */
public abstract class NetworkEntityIT<
        T extends
            NetworkEntity & Contactable & Taggable & MachineTaggable & Commentable & Endpointable
                & Identifiable & LenientEquals<T>>
    extends BaseItTest {

  private final NetworkEntityService<T> service; // under test

  private final ContactService contactService;
  private final EndpointService endpointService;
  private final MachineTagService machineTagService;
  private final TagService tagService;
  private final CommentService commentService;
  private final IdentifierService identifierService;
  private final TestDataFactory testDataFactory;

  public NetworkEntityIT(
      NetworkEntityService<T> service,
      @Nullable SimplePrincipalProvider simplePrincipalProvider,
      TestDataFactory testDataFactory,
      EsManageServer esServer) {
    super(simplePrincipalProvider, esServer);
    this.service = service;
    // not so nice, but we know what we deal with in the tests
    // and this bundles most basic tests into one base test class without copy paste redundancy
    contactService = service;
    endpointService = service;
    machineTagService = service;
    tagService = service;
    commentService = service;
    identifierService = service;
    this.testDataFactory = testDataFactory;
  }

  public TestDataFactory getTestDataFactory() {
    return testDataFactory;
  }

  @Test
  public void createWithKey() {
    T e = newEntity();
    e.setKey(UUID.randomUUID()); // illegal to provide a key
    assertThrows(ConstraintViolationException.class, () -> service.create(e));
  }

  @Test
  public void testCreate() {
    create(newEntity(), 1);
    create(newEntity(), 2);
  }

  @Disabled("Use during development.")
  @Test
  public void testCreateAsEditor() {
    // Create as admin.
    T entity = create(newEntity(), 1);

    if (getSimplePrincipalProvider() == null) {
      return;
    } else {
      this.getSimplePrincipalProvider().setPrincipal(TestConstants.TEST_EDITOR);
    }

    // Grant appropriate rights to the normal user.
    // This is very ugly, but there aren't APIs for editor rights.
    T anotherEntity = null;
    try {
      anotherEntity = duplicateForCreateAsEditorTest(entity);
      anotherEntity.setModified(null);
      anotherEntity.setCreated(null);
      anotherEntity.setKey(null);

      Connection c = database.getTestDatabase().getConnection();
      PreparedStatement ps = c.prepareStatement("INSERT INTO editor_rights VALUES(?, ?)");
      ps.setString(1, TestConstants.TEST_EDITOR);
      ps.setObject(2, keyForCreateAsEditorTest(entity));
      ps.execute();
      ps.close();
      c.close();

    } catch (Exception e) {
    }

    // Create as the editor user
    create(anotherEntity, 2);
  }

  protected abstract T duplicateForCreateAsEditorTest(T entity) throws Exception;

  protected abstract UUID keyForCreateAsEditorTest(T entity);

  @Test
  public void testTitles() {
    Map<UUID, String> titles = Maps.newHashMap();

    assertEquals(titles, service.getTitles(titles.keySet()));

    for (int i = 1; i < 8; i++) {
      T ent = newEntity();
      ent = create(ent, i);
      titles.put(ent.getKey(), ent.getTitle());
    }
    assertEquals(titles, service.getTitles(titles.keySet()));

    titles.remove(titles.keySet().iterator().next());
    assertEquals(titles, service.getTitles(titles.keySet()));
  }

  /**
   * Create an entity using a principal provider that will fail authorization. The principal
   * provider with name "heinz" won't authorize, because there is no user "heinz" in the test
   * identity database with role administrator.
   */
  @Test
  public void testCreateBadRole() {
    // SimplePrincipalProvider configured for web service client tests only
    if (getSimplePrincipalProvider() != null) {
      getSimplePrincipalProvider().setPrincipal("heinz");
      try {
        create(newEntity(), 1);
      } catch (Exception e) {
        assertTrue(e instanceof AccessControlException);
      }
    }
  }

  @Test
  public void testUpdate() {
    T n1 = create(newEntity(), 1);
    n1.setTitle("New title");
    service.update(n1);
    NetworkEntity n2 = service.get(n1.getKey());
    assertEquals("New title", n2.getTitle(), "Organization's title was to be updated");
    assertNotNull(n1.getModified());
    assertNotNull(n2.getModified());
    assertNotNull(n1.getCreated());
    assertTrue(n2.getModified().after(n1.getModified()), "Modification date was to be changed");
    assertTrue(
        n2.getModified().after(n1.getCreated()),
        "Modification date must be after the creation date");
    assertEquals(
        1,
        service.list(new PagingRequest()).getResults().size(),
        "List service does not reflect the number of created entities");
  }

  @Test
  public void testUpdateFailingValidation() {
    T n1 = create(newEntity(), 1);
    n1.setTitle("A"); // should fail as it is too short
    assertThrows(ConstraintViolationException.class, () -> service.update(n1));
  }

  @Test
  public void testDelete() {
    NetworkEntity n1 = create(newEntity(), 1);
    NetworkEntity n2 = create(newEntity(), 2);
    service.delete(n1.getKey());
    T n4 = service.get(n1.getKey()); // one can get a deleted entity
    n1.setDeleted(n4.getDeleted());
    n1.setModified(n4.getModified());
    assertEquals(n1, n4, "Persisted does not reflect original after a deletion");
    // check that one cannot see the deleted entity in a list
    assertEquals(
        1,
        service.list(new PagingRequest()).getResults().size(),
        "List service does not reflect the number of created entities");
    assertEquals(
        n2,
        service.list(new PagingRequest()).getResults().get(0),
        "Following a delete, the wrong entity is returned in list results");
  }

  public void testDoubleDelete() {
    NetworkEntity n1 = create(newEntity(), 1);
    service.delete(n1.getKey());
    service.delete(n1.getKey()); // should just do nothing silently
  }

  /**
   * Creates 5 entities, and then pages over them using differing paging strategies, confirming the
   * correct number of records are returned for each strategy.
   */
  @Test
  public void testPaging() {
    for (int i = 1; i <= 5; i++) {
      create(newEntity(), i);
    }

    // the expected number of records returned when paging at different page sizes
    int[][] expectedPages =
        new int[][] {
          {1, 1, 1, 1, 1, 0}, // page size of 1
          {2, 2, 1, 0}, // page size of 2
          {3, 2, 0}, // page size of 3
          {4, 1, 0}, // page size of 4
          {5, 0}, // page size of 5
          {5, 0}, // page size of 6
        };

    // test the various paging strategies (e.g. page size of 1,2,3 etc to verify they behave as
    // outlined above)
    for (int pageSize = 1; pageSize <= expectedPages.length; pageSize++) {
      int offset = 0; // always start at beginning
      for (int page = 0; page < expectedPages[pageSize - 1].length; page++, offset += pageSize) {
        // request the page using the page size and offset
        List<T> results =
            service.list(new PagingRequest(offset, expectedPages[pageSize - 1][page])).getResults();
        // confirm it is the correct number of results as outlined above
        assertEquals(
            expectedPages[pageSize - 1][page],
            results.size(),
            "Paging is not operating as expected when requesting pages of size " + pageSize);
      }
    }
  }

  /** Confirm that the list method and its paging return entities in creation time order. */
  @Test
  public void testPagingOrder() {
    // keeps a list of all uuids created in that creation order
    List<UUID> uuids = Lists.newArrayList();
    for (int i = 1; i <= 5; i++) {
      T d = create(newEntity(), i);
      uuids.add(d.getKey());
    }
    uuids = Lists.reverse(uuids);

    // test the various paging strategies (e.g. page size of 1,2,3 etc to verify they behave as
    // outlined above)
    for (int pageSize = 1; pageSize <= uuids.size(); pageSize++) {
      for (int offset = 0; offset < uuids.size() + 1; offset++) {
        // request a page using the page size and offset
        PagingResponse<T> resp = service.list(new PagingRequest(offset, pageSize));
        // confirm it is the correct number of results as outlined above
        assertEquals(
            Math.min(pageSize, uuids.size() - offset),
            resp.getResults().size(),
            "Paging is not operating as expected when requesting pages of size " + pageSize);
        assertEquals(Long.valueOf(uuids.size()), resp.getCount(), "Count wrong");
        int lastIdx = -1;
        for (T d : resp.getResults()) {
          int idx = uuids.indexOf(d.getKey());
          // make sure the datasets are in the same order as the reversed uuid list
          assertTrue(idx > lastIdx);
          lastIdx = idx;
        }
      }
    }
  }

  /** Simple search test including when the entity is updated. */
  @Test
  public void testSimpleSearch() {
    T n1 = create(newEntity(), 1);
    n1.setTitle("New title foo");
    service.update(n1);

    assertEquals(
        Long.valueOf(1), service.search("New", null).getCount(), "Search should return a hit");
    assertEquals(
        Long.valueOf(1), service.search("TITLE", null).getCount(), "Search should return a hit");
    assertEquals(
        Long.valueOf(0), service.search("NO", null).getCount(), "Search should return a hit");
    assertEquals(
        Long.valueOf(1),
        service.search(" new tit fo", null).getCount(),
        "Search should return a hit");
    assertEquals(
        Long.valueOf(0), service.search("<", null).getCount(), "Search should return no hits");
    assertEquals(
        Long.valueOf(0), service.search("\"<\"", null).getCount(), "Search should return no hits");
    assertEquals(
        Long.valueOf(1), service.search(null, null).getCount(), "Search should return all hits");
    assertEquals(
        Long.valueOf(1), service.search("  ", null).getCount(), "Search should return all hits");

    // Updates should be reflected in search
    n1.setTitle("BINGO");
    service.update(n1);

    assertEquals(
        Long.valueOf(1), service.search("BINGO", null).getCount(), "Search should return a hit");
    assertEquals(
        Long.valueOf(0), service.search("New", null).getCount(), "Search should return no hits");
    assertEquals(
        Long.valueOf(0), service.search("TITILE", null).getCount(), "Search should return no hits");
    assertEquals(
        Long.valueOf(1), service.search("bi ", null).getCount(), "Search should return a hit");
  }

  /** Ensures the simple search pages as expected. */
  @Test
  public void testSimpleSearchPaging() {
    for (int i = 1; i <= 5; i++) {
      T n1 = newEntity();
      n1.setTitle("Bingo");
      create(n1, i);
    }

    assertEquals(
        Long.valueOf(5), service.search("Bingo", null).getCount(), "Search should return a hit");
    // first page 3 results
    assertEquals(
        3,
        service.search("Bingo", new PagingRequest(0, 3)).getResults().size(),
        "Search should return the requested number of records");
    // second page should bring the last 2
    assertEquals(
        2,
        service.search("Bingo", new PagingRequest(3, 3)).getResults().size(),
        "Search should return the requested number of records");
    // there are no results after 5
    assertTrue(
        service.search("Bingo", new PagingRequest(5, 3)).getResults().isEmpty(),
        "Search should return the requested number of records");
  }

  @Test
  public void testContacts() {
    T entity = create(newEntity(), 1);
    ContactTests.testAddDeleteUpdate(contactService, entity, testDataFactory);
  }

  @Test
  public void testEndpoints() {
    T entity = create(newEntity(), 1);
    EndpointTests.testAddDelete(endpointService, entity, testDataFactory);
  }

  @Test
  public void testMachineTags() {
    T entity = create(newEntity(), 1);
    MachineTagTests.testAddDelete(machineTagService, entity, testDataFactory);
  }

  @Test
  @Disabled("Only for clients")
  public void testMachineTagsNotAllowedToCreateClient() {
    // only for client tests
    if (getSimplePrincipalProvider() != null) {
      getSimplePrincipalProvider().setPrincipal("notExisting");
      T entity = create(newEntity(), 1);
      assertThrows(
          AccessControlException.class,
          () -> MachineTagTests.testAddDelete(machineTagService, entity, testDataFactory));
    } else {
      throw new AccessControlException("");
    }
  }

  @Test
  @Disabled("only for cliets")
  public void testMachineTagsMissingNamespaceRights() {
    // only for client tests
    if (getSimplePrincipalProvider() != null) {
      getSimplePrincipalProvider().setPrincipal("editor");
      T entity = create(newEntity(), 1);
      assertThrows(
          AccessControlException.class,
          () -> MachineTagTests.testAddDelete(machineTagService, entity, testDataFactory));
    } else {
      throw new AccessControlException("");
    }
  }

  @Test
  @Disabled("Only for clients")
  public void testMachineTagsNotAllowedToDeleteClient() {
    // only for client tests
    if (getSimplePrincipalProvider() != null) {
      T entity = create(newEntity(), 1);
      // add machine tags
      service.addMachineTag(entity.getKey(), testDataFactory.newMachineTag());
      service.addMachineTag(entity.getKey(), testDataFactory.newMachineTag());
      List<MachineTag> machineTags = service.listMachineTags(entity.getKey());
      assertNotNull(machineTags);
      assertEquals(2, machineTags.size(), "2 machine tags have been added");

      // test forbidden deletion
      getSimplePrincipalProvider().setPrincipal("editor");
      setSecurityPrincipal(getSimplePrincipalProvider(), UserRole.EDITOR);
      assertThrows(
          AccessControlException.class,
          () -> service.deleteMachineTag(entity.getKey(), machineTags.get(0).getKey()));
    } else {
      throw new AccessControlException("Only client calls are tested");
    }
  }

  private MachineTag newMachineTag(T owner, String namespace, String name, String value) {
    MachineTag machineTag = new MachineTag(namespace, name, value);
    machineTag.setCreatedBy(owner.getCreatedBy());
    return machineTag;
  }

  @Test
  public void testMachineTagSearch() {
    T entity1 = create(newEntity(), 1);
    T entity2 = create(newEntity(), 2);

    machineTagService.addMachineTag(
        entity1.getKey(), newMachineTag(entity1, "test.gbif.org", "network-entity", "one"));
    machineTagService.addMachineTag(
        entity1.getKey(), newMachineTag(entity1, "test.gbif.org", "network-entity", "two"));
    machineTagService.addMachineTag(
        entity1.getKey(), newMachineTag(entity1, "test.gbif.org", "network-entity", "three"));
    machineTagService.addMachineTag(
        entity2.getKey(), newMachineTag(entity2, "test.gbif.org", "network-entity", "four"));

    PagingResponse<T> res;

    res = service.listByMachineTag("test.gbif.org", "network-entity", "one", null);
    assertEquals(Long.valueOf(1), res.getCount());
    assertEquals(1, res.getResults().size());
    res = service.listByMachineTag("test.gbif.org", "network-entity", null, null);
    assertEquals(Long.valueOf(2), res.getCount());
    assertEquals(2, res.getResults().size());
    res = service.listByMachineTag("test.gbif.org", null, null, null);
    assertEquals(Long.valueOf(2), res.getCount());
    assertEquals(2, res.getResults().size());

    res = service.listByMachineTag("test.gbif.org", "network-entity", "five", null);
    System.out.println("Results " + res.getResults());
    System.out.println("Count " + res.getCount());
    System.err.println("Results " + res.getResults());
    System.err.println("Count " + res.getCount());
    assertEquals(Long.valueOf(0), res.getCount());

    res = service.listByMachineTag("test.gbif.org", "nothing", null, null);
    assertEquals(Long.valueOf(0), res.getCount());

    res = service.listByMachineTag("not-in-test.gbif.org", null, null, null);
    assertEquals(Long.valueOf(0), res.getCount());

    res = service.listByMachineTag("test.gbif.org", null, null, new PagingRequest(0, 1));
    assertEquals(Long.valueOf(2), res.getCount());
    assertEquals(1, res.getResults().size());
    res = service.listByMachineTag("test.gbif.org", null, null, new PagingRequest(1, 1));
    assertEquals(Long.valueOf(2), res.getCount());
    assertEquals(1, res.getResults().size());
    res = service.listByMachineTag("test.gbif.org", null, null, new PagingRequest(2, 1));
    assertEquals(Long.valueOf(2), res.getCount());
    assertEquals(0, res.getResults().size());
  }

  @Test
  public void testTags() {
    T entity = create(newEntity(), 1);
    TagTests.testAddDelete(tagService, entity);
    entity = create(newEntity(), 2);
    TagTests.testTagErroneousDelete(tagService, entity);
  }

  @Test
  public void testComments() {
    T entity = create(newEntity(), 1);
    CommentTests.testAddDelete(commentService, entity, testDataFactory);
  }

  // Check that simple search covers contacts which throws IllegalStateException for Nodes
  @Test
  public void testSimpleSearchContact() {
    ContactTests.testSimpleSearch(contactService, service, create(newEntity(), 1), testDataFactory);
  }

  @Test
  public void testIdentifierCRUD() {
    T entity = create(newEntity(), 1);
    IdentifierTests.testAddDelete(identifierService, service, entity, testDataFactory);
  }

  private Identifier newTestIdentifier(T owner, IdentifierType type, String identifierValue) {
    Identifier identifier = new Identifier(type, identifierValue);
    identifier.setCreatedBy(owner.getCreatedBy());
    return identifier;
  }

  @Test
  public void testIdentifierSearch() {
    T entity1 = create(newEntity(), 1);
    T entity2 = create(newEntity(), 2);

    identifierService.addIdentifier(
        entity1.getKey(), newTestIdentifier(entity1, IdentifierType.DOI, "doi:1"));
    identifierService.addIdentifier(
        entity1.getKey(), newTestIdentifier(entity1, IdentifierType.DOI, "doi:1"));
    identifierService.addIdentifier(
        entity1.getKey(), newTestIdentifier(entity2, IdentifierType.DOI, "doi:2"));
    identifierService.addIdentifier(
        entity2.getKey(), newTestIdentifier(entity2, IdentifierType.DOI, "doi:2"));

    PagingResponse<T> res = service.listByIdentifier(IdentifierType.DOI, "doi:2", null);
    assertEquals(Long.valueOf(2), res.getCount());
    assertEquals(2, res.getResults().size());
    res = service.listByIdentifier("doi:2", null);
    assertEquals(Long.valueOf(2), res.getCount());
    assertEquals(2, res.getResults().size());

    res = service.listByIdentifier(IdentifierType.DOI, "doi:1", null);
    assertEquals(Long.valueOf(1), res.getCount());
    assertEquals(1, res.getResults().size());

    res = service.listByIdentifier(IdentifierType.DOI, "doi:XXX", null);
    assertEquals(Long.valueOf(0), res.getCount());

    res = service.listByIdentifier(IdentifierType.GBIF_PORTAL, "doi:1", null);
    assertEquals(Long.valueOf(0), res.getCount());

    res = service.listByIdentifier(IdentifierType.DOI, "doi:2", new PagingRequest(0, 1));
    assertEquals(Long.valueOf(2), res.getCount());
    assertEquals(1, res.getResults().size());
    res = service.listByIdentifier(IdentifierType.DOI, "doi:2", new PagingRequest(1, 1));
    assertEquals(Long.valueOf(2), res.getCount());
    assertEquals(1, res.getResults().size());
    res = service.listByIdentifier(IdentifierType.DOI, "doi:2", new PagingRequest(2, 1));
    assertEquals(Long.valueOf(2), res.getCount());
    assertEquals(0, res.getResults().size());
  }

  /** @return a new example instance */
  protected abstract T newEntity();

  // Repeatable entity creation with verification tests
  protected T create(T orig, int expectedCount) {
    return create(orig, expectedCount, null);
  }

  /**
   * Repeatable entity creation with verification tests + support for processed properties.
   *
   * @param processedProperties expected values of properties that are processed so they would not
   *     match the original
   */
  protected T create(T orig, int expectedCount, Map<String, Object> processedProperties) {
    try {
      @SuppressWarnings("unchecked")
      T entity = (T) BeanUtils.cloneBean(orig);
      Preconditions.checkNotNull(entity, "Cannot create a non existing entity");
      UUID key = service.create(entity);
      entity.setKey(key);
      T written = service.get(key);
      assertNotNull(written.getCreated());
      assertNotNull(written.getModified());
      assertNull(written.getDeleted());

      if (processedProperties != null) {
        String writtenProperty;
        for (String prop : processedProperties.keySet()) {
          writtenProperty = BeanUtils.getProperty(written, prop);
          // check that the value of the process property is what we expect
          assertEquals(processedProperties.get(prop), writtenProperty);
          // copy property to the entity so it will pass the assertLenientEquals
          BeanUtils.setProperty(entity, prop, writtenProperty);
        }
      }

      assertLenientEquals("Persisted does not reflect original", entity, written);
      assertEquals(
          expectedCount,
          service.list(new PagingRequest()).getResults().size(),
          "List service does not reflect the number of created entities");
      return written;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected NetworkEntityService<T> getService() {
    return service;
  }

  protected void assertResultsOfSize(PagingResponse<T> results, int size) {
    assertNotNull(
        results, "PagingResponse (itself) is null; hint: no paging response should EVER do this");
    assertNotNull(results.getResults(), "PagingResponse results are null");
    assertEquals(
        size, results.getResults().size(), "Unexpected result size for current test state");
  }

  public static void setSecurityPrincipal(
      SimplePrincipalProvider simplePrincipalProvider, UserRole userRole) {
    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
    SecurityContextHolder.setContext(ctx);

    ctx.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            simplePrincipalProvider.get().getName(),
            "",
            Collections.singleton(new SimpleGrantedAuthority(userRole.name()))));
  }
}
