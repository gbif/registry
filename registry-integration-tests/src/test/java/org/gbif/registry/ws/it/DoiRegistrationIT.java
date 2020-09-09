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

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator.CreatorName;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Publisher;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles.Title;
import org.gbif.doi.metadata.datacite.ResourceType;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.registry.doi.DoiInteractionService;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.domain.doi.DoiType;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.client.DoiInteractionClient;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.registry.ws.it.fixtures.UserTestFixture;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This is parameterized to run the same test routines for the following DOI Registration elements:
 *
 * <ol>
 *   <li>The persistence layer
 *   <li>The WS service layer
 *   <li>The WS service client layer
 * </ol>
 */
public class DoiRegistrationIT extends BaseItTest {

  private final DoiInteractionService doiRegistrationResource;
  private final DoiInteractionService doiRegistrationClient;

  @Autowired
  public DoiRegistrationIT(
      DoiInteractionService doiRegistrationResource,
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer,
      @LocalServerPort int localServerPort,
      KeyStore keyStore,
      UserTestFixture userTestFixture) {
    super(simplePrincipalProvider, esServer);
    this.doiRegistrationResource = doiRegistrationResource;
    this.doiRegistrationClient =
        prepareClient(localServerPort, keyStore, DoiInteractionClient.class);
  }

  /** Generates a new DOI. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testGenerate(ServiceType serviceType) {
    DoiInteractionService service =
        getService(serviceType, doiRegistrationResource, doiRegistrationClient);
    DOI doi = service.generate(DoiType.DATA_PACKAGE);
    assertNotNull(doi);
  }

  /** Tests generate and get DOI methods. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreateAndGet(ServiceType serviceType) {
    DoiInteractionService service =
        getService(serviceType, doiRegistrationResource, doiRegistrationClient);
    DOI doi = service.generate(DoiType.DATA_PACKAGE);
    DoiData doiData = service.get(doi.getPrefix(), doi.getSuffix());
    assertNotNull(doi);
    assertNotNull(doiData);
  }

  /** Tests the registration of DOI for a data package. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testRegister(ServiceType serviceType) {
    DoiInteractionService service =
        getService(serviceType, doiRegistrationResource, doiRegistrationClient);
    DoiRegistration doiRegistration =
        DoiRegistration.builder()
            .withType(DoiType.DATA_PACKAGE)
            .withUser(TestConstants.TEST_ADMIN)
            .withMetadata(testMetadata())
            .build();
    DOI doi = service.register(doiRegistration);
    assertNotNull(doi);
  }

  /** Create a test DataCiteMetadata instance. */
  public String testMetadata() {
    try {
      DataCiteMetadata metadata =
          DataCiteMetadata.builder()
              .withCreators(
                  Creators.builder()
                      .addCreator(
                          Creator.builder()
                              .withCreatorName(
                                  CreatorName.builder().withValue(TestConstants.TEST_ADMIN).build())
                              .build())
                      .build())
              .withTitles(
                  Titles.builder().addTitle(Title.builder().withValue("TEST Tile").build()).build())
              .withPublicationYear("2017")
              .withPublisher(Publisher.builder().withValue(TestConstants.TEST_ADMIN).build())
              .withResourceType(
                  DataCiteMetadata.ResourceType.builder()
                      .withResourceTypeGeneral(ResourceType.DATASET)
                      .build())
              .build();

      return DataCiteValidator.toXml(new DOI(DOI.TEST_PREFIX, "1"), metadata);
    } catch (InvalidMetadataException ex) {
      throw new RuntimeException(ex);
    }
  }
}
