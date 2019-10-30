package org.gbif.registry.ws.model;

import java.util.UUID;

/**
 * Simple container class to hold challenge code related parameters.
 */
public class AuthenticationDataParameters {

  private String password;
  private UUID challengeCode;

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public UUID getChallengeCode() {
    return challengeCode;
  }

  public void setChallengeCode(UUID challengeCode) {
    this.challengeCode = challengeCode;
  }
}
