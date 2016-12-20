package org.gbif.registry.search.dataset.checklist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A configuration for the checklist bank database providing JDBC connections from Properties.
 */
@SuppressWarnings("PublicField")
public class ClbConnection {

  private static final String PROPERTY_PREFIX = "checklistbank.db.dataSource.";

  private final Properties props;

  public ClbConnection(Properties props) {
    this.props = props;
  }

  /**
   * @return a new simple postgres jdbc connection
   */
  public Connection connect() throws SQLException {
    String url = "jdbc:postgresql://" + p("serverName") + "/" + p("databaseName");
    return DriverManager.getConnection(url, p("user"), p("password"));
  }

  private String p(String name) {
    return props.getProperty(PROPERTY_PREFIX+name, "");
  }

}
