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
package org.gbif.registry.cli.util;

import org.gbif.registry.cli.common.DbConfiguration;
import org.gbif.utils.file.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public final class RegistryCliUtils {

  private RegistryCliUtils() {}

  /** Prepare JDBC Connection. */
  public static Connection prepareConnection(DbConfiguration dbConfiguration) throws Exception {
    return prepareConnection(
        dbConfiguration.serverName,
        dbConfiguration.databaseName,
        dbConfiguration.user,
        dbConfiguration.password);
  }

  /** Prepare JDBC Connection. */
  public static Connection prepareConnection(
      String host, String databaseName, String user, String password) throws Exception {
    return DriverManager.getConnection(
        String.format("jdbc:postgresql://%s/%s", host, databaseName), user, password);
  }

  /** Read data from file and put it into String. */
  public static String getFileData(String filename) throws Exception {
    //noinspection ConstantConditions
    byte[] bytes =
        Files.readAllBytes(
            Paths.get(ClassLoader.getSystemClassLoader().getResource(filename).getFile()));

    return new String(bytes);
  }

  /** Load yaml config into Configuration class. */
  public static <T> T loadConfig(String configFile, Class<T> type) {
    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      InputStream is = FileUtils.classpathStream(configFile);
      T cfg = mapper.readValue(is, type);
      System.out.println(cfg);
      return cfg;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
