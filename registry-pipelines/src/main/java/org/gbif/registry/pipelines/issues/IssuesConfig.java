package org.gbif.registry.pipelines.issues;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotEmpty;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "pipelines.issues")
public class IssuesConfig {
  @NotEmpty private String githubWsUrl;
  @NotEmpty private String githubUser;
  @NotEmpty private String githubPassword;
  @NotEmpty public String hdfsSiteConfig;
  @NotEmpty public String hdfsPrefix = "hdfs://ha-nn";
}
