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
package org.gbif.registry.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.base.Throwables;

import lombok.Data;

/**
 * A Rule that will truncate the tables ready for a new test. It is expected to do this before each
 * test by using the following:
 *
 * <pre>
 * @RegisterExtension TestCaseDatabaseInitializer = TestCaseDatabaseInitializer(tables);
 * </pre>
 */
@Data
public class TestCaseDatabaseInitializer implements BeforeEachCallback {

  private static final Logger LOG = LoggerFactory.getLogger(TestCaseDatabaseInitializer.class);

  private List<String> tables =
      Arrays.asList(
          "contact",
          "endpoint",
          "tag",
          "identifier",
          "comment",
          "node_identifier",
          "node_identifier",
          "node_identifier",
          "node_machine_tag",
          "node_tag",
          "node_comment",
          "occurrence_download",
          "organization_contact",
          "organization_endpoint",
          "organization_machine_tag",
          "organization_tag",
          "organization_identifier",
          "organization_comment",
          "installation_contact",
          "installation_endpoint",
          "installation_machine_tag",
          "installation_tag",
          "installation_comment",
          "dataset_contact",
          "dataset_endpoint",
          "dataset_machine_tag",
          "dataset_tag",
          "dataset_identifier",
          "dataset_comment",
          "dataset_network",
          "network_contact",
          "network_endpoint",
          "network_machine_tag",
          "network_tag",
          "network_comment",
          "machine_tag, metadata",
          "editor_rights",
          "network",
          "dataset",
          "installation",
          "organization, node",
          "collection_identifier",
          "collection_tag",
          "institution_identifier",
          "institution_tag",
          "institution_occurrence_mapping",
          "collection_occurrence_mapping",
          "collection",
          "institution",
          "address",
          "gbif_doi",
          "pipeline_step",
          "pipeline_process",
          "pipeline_execution",
          "derived_dataset",
          "change_suggestion",
          "grscicoll_audit_log",
          "collection_contact",
          "institution_collection_contact",
          "collection_collection_contact",
          "collections_batch",
          "collection_descriptor_group",
          "collection_descriptor",
          "collection_descriptor_verbatim",
          "descriptor_change_suggestion");

  public TestCaseDatabaseInitializer() {}

  public TestCaseDatabaseInitializer(String... tables) {
    if (tables.length > 0) {
      this.tables = Arrays.asList(tables);
    }
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    truncateTables(
        SpringExtension.getApplicationContext(extensionContext).getBean(DataSource.class));
  }

  private void truncateTables(DataSource dataSource) throws Exception {
    LOG.info("Truncating registry tables");
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      List<String> tablesNotChallengeCode = new ArrayList<>(tables);
      if (tables.contains("public.user")) {
        tablesNotChallengeCode.remove("public.user");
        connection.prepareStatement("DELETE FROM public.user WHERE key >= 0").execute();
        if (tables.contains("challenge_code")) {
          tablesNotChallengeCode.remove("challenge_code");
          // Only challenge codes created for organization are deleted
          connection
              .prepareStatement(
                  "DELETE FROM challenge_code WHERE key IN "
                      + "(SELECT challenge_code_key FROM organization"
                      + " WHERE challenge_code_key IS NOT NULL)")
              .execute();
        }
      }

      if (!tablesNotChallengeCode.isEmpty()) {
        connection
            .prepareStatement("TRUNCATE " + String.join(",", tablesNotChallengeCode) + " CASCADE")
            .execute();
      }

      connection.commit();

      /*
       * These tables aren't truncated, so we probably aren't testing them properly.
       * – crawl_history
       * – dataset_occurrence_download
       * – download_statistics
       * – download_user_statistics
       * – downloaded_records_statistics
       * – endpoint_machine_tag
       * – metasync_history
       * – namespace_rights
       * - country_rights
       * – network_identifier
       * – node_contact
       * – node_endpoint
       * – occurrence_download
       */

    } catch (SQLException e) {
      Throwables.propagate(e);
    }
    LOG.info("Registry tables truncated");
  }
}
