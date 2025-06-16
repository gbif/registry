package org.gbif.registry.cli.vocabularysynchronizer;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.gbif.common.messaging.config.MessagingConfiguration;
import org.gbif.registry.cli.common.DbConfiguration;

import java.util.Set;

@Data
public class VocabularySynchronizerConfiguration {

  @ParametersDelegate @Valid @NotNull
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @Parameter(
      names = "--queue-name",
      description = "The name of the queue to listen to for vocabulary released messages")
  @NotNull
  public String queueName = "vocabulary-released";

  @Parameter(names = "--pool-size", description = "The size of the thread pool to listen to messages")
  public int poolSize = 1;

  @Setter
  @Getter
  @ParametersDelegate @Valid @NotNull
  private DbConfiguration dbConfig;

  @Setter
  @Getter
  @JsonProperty("apiRootUrl")
  @NotNull
  public String apiRootUrl;

  @JsonProperty("vocabulariesToProcess")
  @NotNull
  public Set<String> vocabulariesToProcess;

  @Override
  public String toString() {
    return "VocabularySynchronizerConfiguration{" +
        "messaging=" + messaging +
        ", queueName='" + queueName + '\'' +
        ", poolSize=" + poolSize +
        ", dbConfig=" + dbConfig +
        ", apiRootUrl='" + apiRootUrl + '\'' +
        ", vocabulariesToProcess=" + vocabulariesToProcess +
        '}';
  }
}
 