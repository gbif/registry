package org.gbif.ws.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "appkeys")
public class AppkeysConfiguration {

  private String file;

  private List<String> whitelist;

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public List<String> getWhitelist() {
    return whitelist;
  }

  public void setWhitelist(List<String> whitelist) {
    this.whitelist = whitelist;
  }
}
