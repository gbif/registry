package org.gbif.registry.cli.datasetindex;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;

/**
 *
 */
public class IndexBuilderCommandManual {
  public static void main(String[] args) throws Exception {

    final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    IndexBuilderConfig cfg = mapper.readValue(Resources.getResource("index-builder.yaml"), IndexBuilderConfig.class);;

    IndexBuilderCommand cmd = new IndexBuilderCommand(cfg);
    cmd.doRun();
  }

}