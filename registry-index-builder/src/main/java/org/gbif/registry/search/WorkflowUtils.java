package org.gbif.registry.search;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Properties;

import com.google.common.io.Files;

public class WorkflowUtils {

  public static Properties loadProperties(String[] args) throws IOException {
    if (args.length == 0) {
      throw new IllegalArgumentException("Path to property file required");
    }
    // Creates the injector
    Properties properties;
    try (Reader reader = Files.newReader(new File(args[0]), Charset.defaultCharset())) {
      properties = new Properties();
      properties.load(reader);
    }
    return properties;
  }
}
