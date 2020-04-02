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
package org.gbif.registry.ws;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.ObjectFactory;
import org.gbif.doi.metadata.datacite.ResourceType;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.doi.registration.DoiRegistrationService;
import org.gbif.registry.domain.doi.DoiType;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;

import static org.junit.Assert.assertNotNull;

/**
 * This is parameterized to run the same test routines for the following DOI Registration elements:
 *
 * <ol>
 *   <li>The persistence layer
 *   <li>The WS service layer
 *   <li>The WS service client layer
 * </ol>
 */
@RunWith(Parameterized.class)
public class DoiRegistrationServiceIT {

  @RegisterExtension
  static PreparedDbExtension database =
      EmbeddedPostgresExtension.preparedDatabase(
          LiquibasePreparer.forClasspathLocation("liquibase/master.xml"));

  // Tests user
  private static String TEST_ADMIN_USER = "admin";

  @RegisterExtension
  public final DatabaseInitializer databaseRule =
      new DatabaseInitializer(database.getTestDatabase());

  private final DoiRegistrationService doiRegistrationService;

  @Autowired
  public DoiRegistrationServiceIT(
      DoiRegistrationService doiRegistrationService,
      SimplePrincipalProvider simplePrincipalProvider) {
    this.doiRegistrationService = doiRegistrationService;
  }

  /** Generates a new DOI. */
  @Test
  public void testGenerate() {
    DOI doi = doiRegistrationService.generate(DoiType.DATA_PACKAGE);
    assertNotNull(doi);
  }

  /** Tests the generate and get DOI methods. */
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
            .withUser(TEST_ADMIN_USER)
            .withMetadata(testMetadata());
    DOI doi = doiRegistrationService.register(builder.build());
    assertNotNull(doi);
  }

  /** Create a test DataCiteMetadata instance. */
  public String testMetadata() {
    try {
      ObjectFactory of = new ObjectFactory();
      DataCiteMetadata res = of.createDataCiteMetadata();

      DataCiteMetadata.Creators creators = of.createDataCiteMetadataCreators();
      DataCiteMetadata.Creators.Creator creator = of.createDataCiteMetadataCreatorsCreator();
      DataCiteMetadata.Creators.Creator.CreatorName name =
          of.createDataCiteMetadataCreatorsCreatorCreatorName();
      name.setValue(TEST_ADMIN_USER);
      creator.setCreatorName(name);
      creators.getCreator().add(creator);
      res.setCreators(creators);

      DataCiteMetadata.Titles titles = of.createDataCiteMetadataTitles();
      DataCiteMetadata.Titles.Title title = of.createDataCiteMetadataTitlesTitle();
      title.setValue("TEST Tile");
      titles.getTitle().add(title);
      res.setTitles(titles);

      res.setPublicationYear("2017");
      DataCiteMetadata.Publisher publisher = of.createDataCiteMetadataPublisher();
      publisher.setValue(TEST_ADMIN_USER);
      res.setPublisher(publisher);
      DataCiteMetadata.ResourceType resourceType = of.createDataCiteMetadataResourceType();
      resourceType.setResourceTypeGeneral(ResourceType.DATASET);
      res.setResourceType(resourceType);
      return DataCiteValidator.toXml(new DOI(DOI.TEST_PREFIX, "1"), res);
    } catch (InvalidMetadataException ex) {
      throw new RuntimeException(ex);
    }
  }
}
