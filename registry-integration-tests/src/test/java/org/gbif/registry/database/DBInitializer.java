package org.gbif.registry.database;

import java.sql.Connection;

public interface DBInitializer {
  void init(Connection connection);
}
