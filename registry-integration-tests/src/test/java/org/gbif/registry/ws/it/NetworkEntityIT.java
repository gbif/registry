/*
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
import org.gbif.api.service.registry.NetworkEntityService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.database.DatabaseCleaner;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.client.NetworkEntityClient;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.security.AccessControlException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import javax.validation.ValidationException;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import static org.gbif.registry.ws.it.LenientAssert.assertLenientEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

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
  private final NetworkEntityService<T> client; // under test

  private final TestDataFactory testDataFactory;

  @Autowired private DataSource dataSource;

  public NetworkEntityIT(
      NetworkEntityService<T> service,
      int localServerPort,
      KeyStore keyStore,
      Class<? extends NetworkEntityService<T>> cls,
      @Nullable SimplePrincipalProvider simplePrincipalProvider,
      TestDataFactory testDataFactory,
      EsManageServer esServer) {
    super(simplePrincipalProvider, esServer);
    this.service = service;
    this.client = prepareClient(localServerPort, keyStore, cls);
    this.testDataFactory = testDataFactory;
  }

  public TestDataFactory getTestDataFactory() {
    return testDataFactory;
  }

  public NetworkEntityService<T> getService(ServiceType param) {
    switch (param) {
      case CLIENT:
        return client;
      case RESOURCE:
        return service;
      default:
        throw new IllegalStateException("Must be resource or client");
    }
  }

  @Execution(CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void createWithKey(ServiceType serviceType) {
    NetworkEntityService<T> service = getService(serviceType);
    T e = newEntity(serviceType);
    e.setKey(UUID.randomUUID()); // illegal to provide a key
    assertThrows(ValidationException.class, () -> service.create(e));
  }

  @Execution(CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreate(ServiceType serviceType) {
    create(newEntity(serviceType), serviceType);
    create(newEntity(serviceType), serviceType);
  }

  @Disabled("Use during development.")
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreateAsEditor(ServiceType serviceType) throws Exception {
    // Create as admin.
    T entity = create(newEntity(serviceType), serviceType);

    if (getSimplePrincipalProvider() == null) {
      return;
    } else {
      this.getSimplePrincipalProvider().setPrincipal(TestConstants.TEST_EDITOR);
    }

    // Grant appropriate rights to the normal user.
    // This is very ugly, but there aren't APIs for editor rights.
    T anotherEntity = null;
    anotherEntity = duplicateForCreateAsEditorTest(entity);
    anotherEntity.setModified(null);
    anotherEntity.setCreated(null);
    anotherEntity.setKey(null);

    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement("INSERT INTO editor_rights VALUES(?, ?)")) {
      ps.setString(1, TestConstants.TEST_EDITOR);
      ps.setObject(2, keyForCreateAsEditorTest(entity));
      ps.execute();
    }

    // Create as the editor user
    create(anotherEntity, serviceType);
  }

  protected abstract T duplicateForCreateAsEditorTest(T entity) throws Exception;

  protected abstract UUID keyForCreateAsEditorTest(T entity);

  @Execution(CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testTitles(ServiceType serviceType) {
    NetworkEntityService<T> service = getService(serviceType);
    Map<UUID, String> titles = new HashMap<>();

    assertEquals(titles, service.getTitles(titles.keySet()));

    for (int i = 1; i < 8; i++) {
      T ent = newEntity(serviceType);
      ent = create(ent, serviceType);
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
  @Execution(CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreateBadRole(ServiceType serviceType) {
    // SimplePrincipalProvider configured for web service client tests only
    if (getSimplePrincipalProvider() != null) {
      getSimplePrincipalProvider().setPrincipal("heinz");
      try {
        create(newEntity(serviceType), serviceType);
      } catch (Exception e) {
        assertTrue(e instanceof AccessControlException);
      }
    }
  }

  @Execution(CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testUpdate(ServiceType serviceType) {
    NetworkEntityService<T> service = getService(serviceType);
    T n1 = create(newEntity(serviceType), serviceType);
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
    List<T> results = service.list(new PagingRequest()).getResults();
    assertEquals(
        1,
        results.stream().filter(r -> r.getKey().equals(n1.getKey())).count(),
        "List service does not reflect the number of created entities");
  }

  @Execution(CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testUpdateFailingValidation(ServiceType serviceType) {
    NetworkEntityService<T> service = getService(serviceType);
    T n1 = create(newEntity(serviceType), serviceType);
    n1.setTitle("A"); // should fail as it is too short
    assertThrows(ValidationException.class, () -> service.update(n1));
    service.delete(n1.getKey());
  }

  @Execution(CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testDelete(ServiceType serviceType) {
    NetworkEntityService<T> service = getService(serviceType);
    NetworkEntity n1 = create(newEntity(serviceType), serviceType);
    NetworkEntity n2 = create(newEntity(serviceType), serviceType);
    service.delete(n1.getKey());
    T n4 = service.get(n1.getKey()); // one can get a deleted entity
    n1.setDeleted(n4.getDeleted());
    n1.setModified(n4.getModified());
    assertEquals(n1, n4, "Persisted does not reflect original after a deletion");
    // check that one cannot see the deleted entity in a list
    List<T> results = service.list(new PagingRequest()).getResults();
    assertEquals(
        1,
        results.stream().filter(r -> r.getKey().equals(n2.getKey())).count(),
        "List service does not reflect the number of created entities");
    assertEquals(
        0,
        results.stream().filter(r -> r.getKey().equals(n1.getKey())).count(),
        "Following a delete, the wrong entity is returned in list results");
  }

  @Execution(CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testContacts(ServiceType serviceType) {
    NetworkEntityService<T> service = getService(serviceType);
    T entity = create(newEntity(serviceType), serviceType);
    ContactTests.testAddDeleteUpdate(service, entity, testDataFactory);
    service.delete(entity.getKey());
  }

  @Execution(CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testEndpoints(ServiceType serviceType) {
    NetworkEntityService<T> service = getService(serviceType);
    T entity = create(newEntity(serviceType), serviceType);
    EndpointTests.testAddDelete(service, entity, testDataFactory);
    service.delete(entity.getKey());
  }

  @Execution(CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testMachineTags(ServiceType serviceType) {
    NetworkEntityService<T> service = getService(serviceType);
    T entity = create(newEntity(serviceType), serviceType);
    MachineTagTests.testAddDelete(service, entity, testDataFactory);
  }

  @Disabled("client should throw AccessControlException")
  @ParameterizedTest
  @EnumSource(
      value = ServiceType.class,
      names = {"CLIENT"})
  public void testMachineTagsNotAllowedToCreateClient() {
    ServiceType serviceType = ServiceType.RESOURCE;
    NetworkEntityService<T> service = getService(serviceType);
    // only for client tests
    if (getSimplePrincipalProvider() != null) {
      getSimplePrincipalProvider().setPrincipal("notExisting");
      T entity = create(newEntity(serviceType), serviceType);
      assertThrows(
          AccessControlException.class,
          () -> MachineTagTests.testAddDelete(service, entity, testDataFactory));
    } else {
      throw new AccessControlException("");
    }
  }

  @Disabled("client should throw AccessControlException")
  @ParameterizedTest
  @EnumSource(
      value = ServiceType.class,
      names = {"CLIENT"})
  public void testMachineTagsMissingNamespaceRights() {
    ServiceType serviceType = ServiceType.RESOURCE;
    NetworkEntityService<T> service = getService(serviceType);
    // only for client tests
    if (getSimplePrincipalProvider() != null) {
      getSimplePrincipalProvider().setPrincipal("editor");
      T entity = create(newEntity(serviceType), ServiceType.CLIENT);
      assertThrows(
          AccessControlException.class,
          () -> MachineTagTests.testAddDelete(service, entity, testDataFactory));
    } else {
      throw new AccessControlException("");
    }
  }

  @Disabled("client should throw AccessControlException")
  @ParameterizedTest
  @EnumSource(
      value = ServiceType.class,
      names = {"CLIENT"})
  public void testMachineTagsNotAllowedToDeleteClient() {
    ServiceType serviceType = ServiceType.RESOURCE;
    NetworkEntityService<T> service = getService(serviceType);
    // only for client tests
    if (getSimplePrincipalProvider() != null) {
      T entity = create(newEntity(serviceType), ServiceType.CLIENT);
      // add machine tags
      service.addMachineTag(entity.getKey(), testDataFactory.newMachineTag());
      service.addMachineTag(entity.getKey(), testDataFactory.newMachineTag());
      List<MachineTag> machineTags = service.listMachineTags(entity.getKey());
      assertNotNull(machineTags);
      assertEquals(2, machineTags.size(), "2 machine tags have been added");

      // test forbidden deletion
      getSimplePrincipalProvider().setPrincipal("editor");
      setSecurityPrincipal(getSimplePrincipalProvider(), UserRole.REGISTRY_EDITOR);
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

  @Execution(CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testTags(ServiceType serviceType) {
    NetworkEntityService<T> service = getService(serviceType);
    T entity = create(newEntity(serviceType), serviceType);
    TagTests.testAddDelete(service, entity);
    entity = create(newEntity(serviceType), serviceType);
    TagTests.testTagErroneousDelete(service, entity);
  }

  @Execution(CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testComments(ServiceType serviceType) {
    NetworkEntityService<T> service = getService(serviceType);
    T entity = create(newEntity(serviceType), serviceType);
    CommentTests.testAddDelete(service, entity, testDataFactory);
  }

  @Execution(CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testIdentifierCRUD(ServiceType serviceType) {
    NetworkEntityService<T> service = getService(serviceType);
    T entity = create(newEntity(serviceType), serviceType);
    IdentifierTests.testAddDelete(service, service, entity, testDataFactory);
  }

  private Identifier newTestIdentifier(T owner, IdentifierType type, String identifierValue) {
    Identifier identifier = new Identifier(type, identifierValue);
    identifier.setCreatedBy(owner.getCreatedBy());
    return identifier;
  }

  @Execution(CONCURRENT)
  public void updateEntityKeyMismatchTest() {
    ServiceType serviceType = ServiceType.CLIENT;
    NetworkEntityClient<T> crudClient = (NetworkEntityClient<T>) client;
    T entity = create(newEntity(serviceType), serviceType);

    assertThrows(
        IllegalArgumentException.class, () -> crudClient.updateResource(UUID.randomUUID(), entity));
  }

  /** @return a new example instance */
  protected abstract T newEntity(ServiceType serviceType);

  // Repeatable entity creation with verification tests
  protected T create(T orig, ServiceType serviceType) {
    return create(orig, serviceType, null);
  }

  /**
   * Repeatable entity creation with verification tests + support for processed properties.
   *
   * @param processedProperties expected values of properties that are processed so they would not
   *     match the original
   */
  protected T create(T orig, ServiceType serviceType, Map<String, Object> processedProperties) {
    try {
      NetworkEntityService<T> service = getService(serviceType);
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

      assertLenientEquals(
          String.format(
              "Persisted does not reflect original\nPersisted: %s\nOriginal:  %s", written, entity),
          entity,
          written);
      List<T> results = service.list(new PagingRequest()).getResults();
      assertEquals(
          1,
          results.stream().filter(r -> r.getKey().equals(key)).count(),
          "List service does not reflect the number of created entities");
      return written;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  protected void assertResultsOfSize(PagingResponse<?> results, int size) {
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

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("registry.datasource.url", PG_CONTAINER::getJdbcUrl);
    registry.add("registry.datasource.username", PG_CONTAINER::getUsername);
    registry.add("registry.datasource.password", PG_CONTAINER::getPassword);
    registry.add("elasticsearch.mock", () -> "false");
  }
}
