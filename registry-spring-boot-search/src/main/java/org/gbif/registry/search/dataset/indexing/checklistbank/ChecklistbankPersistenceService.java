package org.gbif.registry.search.dataset.indexing.checklistbank;

import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * A builder that will clear and build a new dataset index by paging over the given service.
 */
@Slf4j
@Component
public class ChecklistbankPersistenceService {

  private static final String SQL = "SELECT array_agg(nub_fk) as keys " +
                                    "FROM nub_rel " +
                                    "WHERE dataset_key = '%s'";

  @Autowired
  @Qualifier("clb_datasource")
  private DataSource dataSource;
  /**
   * Pages over all datasets and adds them to SOLR.
   */
  public Integer[] getTaxonKeys(String datasetKey) {

    try (Connection conn = dataSource.getConnection()) {
      log.info("Updating all checklists for dataset {}", datasetKey);

      // use streaming cursor for large result sets
      conn.setAutoCommit(false);
      Statement st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
      st.setFetchSize(2);

      ResultSet rs = st.executeQuery(String.format(SQL, datasetKey));
      if (rs.next()) {
        try {
          Array result = rs.getArray("keys");
          if(Objects.nonNull(result)) {
            return (Integer[])result.getArray();
          } else {
            return new Integer[0];
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
      rs.close();
    } catch (Exception e) {
      log.error("Failed to index taxon keys for dataset {}", datasetKey, e);
      throw new RuntimeException(e);
    }
    return new Integer[0];
  }

}
