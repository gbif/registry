package org.gbif.registry.collections.sync;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class SyncConfig {

  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
  private static final ObjectReader YAML_READER = YAML_MAPPER.readerFor(SyncConfig.class);

  private String registryWsUrl;
  private String registryWsUser;
  private String registryWsPassword;
  private String ihWsUrl;
  private String githubWsUrl;
  private String githubUser;
  private String githubPassword;
  private boolean saveResultsToFile;
  private boolean dryRun;
  private boolean ignoreConflicts;
  private List<String> ghIssuesAssignees;

  public static Optional<SyncConfig> getConfig(String[] args) throws IOException {
    // parse args
    Params params = new Params();
    JCommander.newBuilder().addObject(params).build().parse(args);

    String configFileName = params.confPath;
    if (Strings.isNullOrEmpty(configFileName)) {
      log.error("No config file provided");
      return Optional.empty();
    }

    File configFile = new File(configFileName);
    return Optional.ofNullable(YAML_READER.readValue(configFile));
  }

  private static class Params {
    @Parameter(names = {"--config", "-c"})
    private String confPath;
  }
}
