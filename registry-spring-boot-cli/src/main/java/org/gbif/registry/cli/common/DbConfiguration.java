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
package org.gbif.registry.cli.common;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Properties;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.beust.jcommander.Parameter;
import com.google.common.collect.Sets;

/** Holds configuration for the registry or identity database. */
@SuppressWarnings("PublicField")
public class DbConfiguration {

  private static final String PG_DATASOURCE_CLASSNAME = "org.postgresql.ds.PGSimpleDataSource";
  private static final String IDENTITY_DATASOURCE_CLASSNAME =
      "org.postgresql.ds.PGSimpleDataSource";

  private static final String PROPERTY_REGISTRY_PREFIX = "registry.db.";
  private static final String PROPERTY_IDENTITY_PREFIX = "identity.db.";

  private static final String DATASOURCE_DATASOURCE_CLASSNAME_PROP = "dataSourceClassName";
  private static final Set<String> DATASOURCE_SET =
      Sets.newHashSet("serverName", "databaseName", "user", "password");

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

  /**
   * Alias for toProperties(PROPERTY_REGISTRY_PREFIX, PG_DATASOURCE_CLASSNAME)
   *
   * @return
   */
  public Properties toRegistryProperties() {
    return toProperties(PROPERTY_REGISTRY_PREFIX, PG_DATASOURCE_CLASSNAME);
  }

  /**
   * Create a Properties object from the public fields from that class.
   *
   * @param prefix
   * @param dataSourceClassName
   * @return
   */
  public Properties toProperties(String prefix, String dataSourceClassName) {
    Properties props = new Properties();
    props.put(prefix + DATASOURCE_DATASOURCE_CLASSNAME_PROP, dataSourceClassName);
    for (Field field : DbConfiguration.class.getDeclaredFields()) {
      if (!field.isSynthetic() && Modifier.isPublic(field.getModifiers())) {
        try {
          if (DATASOURCE_SET.contains(field.getName())) {
            props.put(prefix + "dataSource." + field.getName(), String.valueOf(field.get(this)));
          } else {
            props.put(prefix + field.getName(), String.valueOf(field.get(this)));
          }
        } catch (IllegalAccessException e) {
          // cant happen, we check for public access
          throw new RuntimeException(e);
        }
      }
    }
    return props;
  }
}
