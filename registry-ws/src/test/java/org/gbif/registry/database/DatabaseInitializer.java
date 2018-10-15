/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.database;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import com.google.common.base.Throwables;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Rule that will truncate the tables ready for a new test. It is expected to do this before each test by using the
 * following:
 *
 * <pre>
 * @Rule
 * public DatabaseInitializer = new DatabaseInitializer(getDatasource()); // developer required to provide datasource
 * </pre>
 */
public class DatabaseInitializer extends ExternalResource {

  private static final Logger LOG = LoggerFactory.getLogger(DatabaseInitializer.class);
  private final DataSource dataSource;

  public DatabaseInitializer(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  protected void before() throws Throwable {
    LOG.info("Truncating registry tables");
    Connection connection = dataSource.getConnection();
    try {
      connection.setAutoCommit(false);
      connection.prepareStatement("DELETE FROM contact").execute();
      connection.prepareStatement("DELETE FROM endpoint").execute();
      connection.prepareStatement("DELETE FROM tag").execute();
      connection.prepareStatement("DELETE FROM identifier").execute();
      connection.prepareStatement("DELETE FROM comment").execute();
      connection.prepareStatement("DELETE FROM node_identifier").execute();
      connection.prepareStatement("DELETE FROM node_machine_tag").execute();
      connection.prepareStatement("DELETE FROM node_tag").execute();
      connection.prepareStatement("DELETE FROM node_comment").execute();
      connection.prepareStatement("DELETE FROM organization_contact").execute();
      connection.prepareStatement("DELETE FROM organization_endpoint").execute();
      connection.prepareStatement("DELETE FROM organization_machine_tag").execute();
      connection.prepareStatement("DELETE FROM organization_tag").execute();
      connection.prepareStatement("DELETE FROM organization_identifier").execute();
      connection.prepareStatement("DELETE FROM organization_comment").execute();
      connection.prepareStatement("DELETE FROM installation_contact").execute();
      connection.prepareStatement("DELETE FROM installation_endpoint").execute();
      connection.prepareStatement("DELETE FROM installation_machine_tag").execute();
      connection.prepareStatement("DELETE FROM installation_tag").execute();
      connection.prepareStatement("DELETE FROM installation_comment").execute();
      connection.prepareStatement("DELETE FROM dataset_contact").execute();
      connection.prepareStatement("DELETE FROM dataset_endpoint").execute();
      connection.prepareStatement("DELETE FROM dataset_machine_tag").execute();
      connection.prepareStatement("DELETE FROM dataset_tag").execute();
      connection.prepareStatement("DELETE FROM dataset_identifier").execute();
      connection.prepareStatement("DELETE FROM dataset_comment").execute();
      connection.prepareStatement("DELETE FROM network_contact").execute();
      connection.prepareStatement("DELETE FROM network_endpoint").execute();
      connection.prepareStatement("DELETE FROM network_machine_tag").execute();
      connection.prepareStatement("DELETE FROM network_tag").execute();
      connection.prepareStatement("DELETE FROM network_comment").execute();
      connection.prepareStatement("DELETE FROM machine_tag").execute();
      connection.prepareStatement("DELETE FROM metadata").execute();
      connection.prepareStatement("DELETE FROM editor_rights").execute();
      connection.prepareStatement("DELETE FROM network").execute();
      connection.prepareStatement("DELETE FROM dataset").execute();
      connection.prepareStatement("DELETE FROM installation").execute();
      connection.prepareStatement("DELETE FROM organization").execute();
      connection.prepareStatement("DELETE FROM node").execute();
      connection.prepareStatement("DELETE FROM public.user").execute();
      connection.prepareStatement("DELETE FROM challenge_code").execute();
      connection.prepareStatement("TRUNCATE gbif_doi").execute();
      connection.commit();

    } catch (SQLException e) {
      Throwables.propagate(e);
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
    LOG.info("Registry tables truncated");
  }
}
