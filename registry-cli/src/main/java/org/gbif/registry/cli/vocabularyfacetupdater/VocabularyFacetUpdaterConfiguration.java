package org.gbif.registry.cli.vocabularyfacetupdater;

import lombok.Data;

import lombok.Getter;

import lombok.Setter;

import org.gbif.common.messaging.config.MessagingConfiguration;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Set;

import org.gbif.registry.cli.common.DbConfiguration;

@Data
public class VocabularyFacetUpdaterConfiguration {

  @ParametersDelegate @Valid @NotNull
  public MessagingConfiguration messaging = new MessagingConfiguration();

  @Parameter(
      names = "--queue-name",
      description = "The name of the queue to listen to for vocabulary released messages")
  @NotNull
  public String queueName = "vocabulary-released";

  @Parameter(names = "--pool-size", description = "The size of the thread pool to listen to messages")
  @Min(1)
  public int poolSize = 1;

  @Setter
  @Getter
  @ParametersDelegate @Valid @NotNull private DbConfiguration dbConfig;

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
    return "VocabularyFacetUpdaterConfiguration{" +
        "messaging=" + messaging +
        ", queueName='" + queueName + '\'' +
        ", poolSize=" + poolSize +
        ", dbConfig=" + dbConfig +
        ", apiRootUrl='" + apiRootUrl + '\'' +
        ", vocabulariesToProcess=" + vocabulariesToProcess +
        '}';
  }
}
