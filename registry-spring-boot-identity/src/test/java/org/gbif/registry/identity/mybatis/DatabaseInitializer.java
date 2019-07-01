package org.gbif.registry.identity.mybatis;

import com.google.common.base.Throwables;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
  protected void before() {
    LOG.info("Truncating identity tables");

    // TODO: 2019-06-28 check this part
    // TODO: 2019-06-28 do we really need autoCommit false?
    // TODO: 2019-06-28 use truncate instead of delete
    try (final Connection connection = dataSource.getConnection();
         final PreparedStatement ps1 = connection.prepareStatement("TRUNCATE public.user");
         final PreparedStatement ps2 = connection.prepareStatement("TRUNCATE editor_rights")) {
      connection.setAutoCommit(false);
      ps1.execute();
      ps2.execute();
      connection.commit();
    } catch (SQLException e) {
      Throwables.propagate(e);
    }

    LOG.info("Identity tables truncated");
  }

  // TODO: 2019-06-28 where is the 'after'?
}

