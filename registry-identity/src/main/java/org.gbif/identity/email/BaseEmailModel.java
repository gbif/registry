package org.gbif.identity.email;

import java.net.URL;

/**
 *
 */
public class BaseEmailModel {

  private final String name;
  private final URL challengeUrl;

  public BaseEmailModel(String name, URL challengeUrl) {
    this.name = name;
    this.challengeUrl = challengeUrl;
  }

  public String getName() {
    return name;
  }

  public URL getChallengeUrl() {
    return challengeUrl;
  }
}
