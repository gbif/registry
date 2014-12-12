package org.gbif.registry.cli.configuration;

import org.gbif.registry.persistence.guice.RegistryMyBatisModule;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Properties;
import java.util.Set;
import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A configuration for the registry database connection pool
 * as used by the mybatis layer. Knows how to insert a service guice module.
 */
@SuppressWarnings("PublicField")
public class DbConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(DbConfiguration.class);
  private static final String PROPERTY_PREFIX = "registry.db.";
  private static final Set<String> DATASOURCE_SET = Sets.newHashSet("serverName", "databaseName", "user", "password");

  @NotNull
  @Parameter(names = "--db-host")
  public String serverName;

  @NotNull
  @Parameter(names = "--db-db")
  public String databaseName;

  @NotNull
  @Parameter(names = "--db-user")
  public String user;

  @NotNull
  @Parameter(names = "--db-password", password = true)
  public String password;

  @Parameter(names = "--db-maximumPoolSize")
  public int maximumPoolSize = 3;

  @Parameter(names = "--db-connectionTimeout")
  public int connectionTimeout = 3000;

  public RegistryMyBatisModule createMyBatisModule() {
    Properties props = new Properties();
    props.put(PROPERTY_PREFIX + "dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
    for (Field field : DbConfiguration.class.getDeclaredFields()) {
      if (!field.isSynthetic() && Modifier.isPublic(field.getModifiers())) {
        try {
          if (DATASOURCE_SET.contains(field.getName())) {
            props.put(PROPERTY_PREFIX + "dataSource." + field.getName(), String.valueOf(field.get(this)));
          } else {
            props.put(PROPERTY_PREFIX + field.getName(), String.valueOf(field.get(this)));
          }
        } catch (IllegalAccessException e) {
          // cant happen, we check for public access
          throw new RuntimeException(e);
        }
      }
    }
    LOG.info("Connecting to registry db {} on {} with user {}", databaseName, serverName, user);
    return new RegistryMyBatisModule(props);
  }

}
