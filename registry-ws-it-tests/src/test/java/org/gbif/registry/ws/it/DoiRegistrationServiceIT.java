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
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.doi.registration.DoiRegistrationService;
import org.gbif.registry.domain.doi.DoiType;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
public class DoiRegistrationServiceIT  extends BaseItTest {

  private final DoiRegistrationService doiRegistrationService;

  @Autowired
  public DoiRegistrationServiceIT(DoiRegistrationService doiRegistrationService,
                                  SimplePrincipalProvider simplePrincipalProvider) {
    super(simplePrincipalProvider);
    this.doiRegistrationService = doiRegistrationService;
  }

  /** Generates a new DOI. */
  @Test
  public void testGenerate() {
    DOI doi = doiRegistrationService.generate(DoiType.DATA_PACKAGE);
    assertNotNull(doi);
  }

  /** Tests generate and get DOI methods. */
  @Test
  public void testCreateAndGet() {
    DOI doi = doiRegistrationService.generate(DoiType.DATA_PACKAGE);
    DoiData doiData = doiRegistrationService.get(doi.getPrefix(), doi.getSuffix());
    assertNotNull(doi);
    assertNotNull(doiData);
  }

  /** Tests the registration of DOI for a data package. */
  @Test
  public void testRegister() {
    DoiRegistration.Builder builder =
        DoiRegistration.builder()
            .withType(DoiType.DATA_PACKAGE)
            .withUser(TestConstants.TEST_ADMIN)
            .withMetadata(testMetadata());
    DOI doi = doiRegistrationService.register(builder.build());
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
