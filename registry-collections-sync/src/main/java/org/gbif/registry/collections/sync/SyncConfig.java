package org.gbif.registry.collections.sync;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

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
  private NotificationConfig notification;
  private boolean saveResultsToFile;
  private boolean dryRun;
  private boolean sendNotifications;

  @Getter
  @Setter
  public static class NotificationConfig {
    private String githubWsUrl;
    private String githubUser;
    private String githubPassword;
    private String ihPortalUrl;
    private String registryPortalUrl;
    private List<String> ghIssuesAssignees;
  }

  public static Optional<SyncConfig> fromFileName(String configFileName) {
    if (Strings.isNullOrEmpty(configFileName)) {
      log.error("No config file provided");
      return Optional.empty();
    }

    File configFile = Paths.get(configFileName).toFile();
    SyncConfig config;
    try {
      config = YAML_READER.readValue(configFile);
    } catch (IOException e) {
      log.error("Couldn't load config from file {}", configFileName, e);
      return Optional.empty();
    }

    if (config == null) {
      return Optional.empty();
    }

    // do some checks for required fields
    if (Strings.isNullOrEmpty(config.getRegistryWsUrl())
        || Strings.isNullOrEmpty(config.getIhWsUrl())) {
      throw new IllegalArgumentException("Registry and IH WS URLs are required");
    }

    if (!config.isDryRun()
        && (Strings.isNullOrEmpty(config.getRegistryWsUser())
            || Strings.isNullOrEmpty(config.getRegistryWsPassword()))) {
      throw new IllegalArgumentException(
          "Registry WS credentials are required if we are not doing a dry run");
    }

    if (config.isSendNotifications()) {
      if (config.getNotification() == null) {
        throw new IllegalArgumentException("Notification config is required");
      }

      if (!config.getNotification().getGithubWsUrl().endsWith("/")) {
        throw new IllegalArgumentException("Github API URL must finish with a /.");
      }

      if (Strings.isNullOrEmpty(config.getNotification().getGithubUser())
          || Strings.isNullOrEmpty(config.getNotification().getGithubPassword())) {
        throw new IllegalArgumentException(
            "Github credentials are required if we are not ignoring conflicts.");
      }

      if (Strings.isNullOrEmpty(config.getNotification().getRegistryPortalUrl())
          || Strings.isNullOrEmpty(config.getNotification().getIhPortalUrl())) {
        throw new IllegalArgumentException("Portal URLs are required");
      }
    }

    return Optional.of(config);
  }
}
