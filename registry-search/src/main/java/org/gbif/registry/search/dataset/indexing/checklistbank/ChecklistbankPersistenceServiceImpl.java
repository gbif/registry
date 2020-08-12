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
package org.gbif.registry.search.dataset.indexing.checklistbank;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/** A builder that will clear and build a new dataset index by paging over the given service. */
@Slf4j
@Component
public class ChecklistbankPersistenceServiceImpl implements ChecklistbankPersistenceService {

  private static final String SQL =
      "SELECT array_agg(nub_fk) as keys " + "FROM nub_rel " + "WHERE dataset_key = '%s'";

  private final DataSource dataSource;

  @Autowired
  public ChecklistbankPersistenceServiceImpl(@Qualifier("clb_datasource") DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /** Pages over all datasets and adds them to SOLR. */
  @Override
  public Integer[] getTaxonKeys(String datasetKey) {

    try (Connection conn = dataSource.getConnection()) {
      log.info("Updating all checklists for dataset {}", datasetKey);

      // use streaming cursor for large result sets
      conn.setAutoCommit(false);
      try (Statement st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
        st.setFetchSize(2);

        try (ResultSet rs = st.executeQuery(String.format(SQL, datasetKey))) {
          if (rs.next()) {
            try {
              Array result = rs.getArray("keys");
              if (Objects.nonNull(result)) {
                Integer[] taxonKeys = (Integer[]) result.getArray();
                log.info("Dataset [{}] has [{}] different taxon keys", datasetKey, taxonKeys.length);
                return taxonKeys;
              } else {
                return new Integer[0];
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Failed to index taxon keys for dataset {}", datasetKey, e);
      throw new RuntimeException(e);
    }
    return new Integer[0];
  }
}
