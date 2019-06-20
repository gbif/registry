package org.gbif.registry.cli.common;

import com.beust.jcommander.internal.Nullable;
import org.gbif.doi.service.DoiService;
import org.gbif.doi.service.datacite.RestJsonApiDataCiteService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * A configuration for the DataCite service.
 */
@Configuration
@ConfigurationProperties(prefix = "datacite")
@Validated
public class DataCiteConfiguration {
  @NotNull
  private String username;

  @NotNull
  private String password;

  @Nullable
  private URI api;

  private int threads = 10;

  private int timeout = 20000;

  @Bean
  public DoiService doiService() {
    return new RestJsonApiDataCiteService(api.toString(), username, password);
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public URI getApi() {
    return api;
  }

  public void setApi(URI api) {
    this.api = api;
  }

  public int getThreads() {
    return threads;
  }

  public void setThreads(int threads) {
    this.threads = threads;
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }
}