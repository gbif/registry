package org.gbif.registry.domain.ws;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.UUID;

public class EmailChangeRequest {

  // Must be the same as AbstractGbifUser#EMAIL_PATTERN
  public static final String EMAIL_PATTERN =
      "^[_A-Za-z0-9-+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";

  private String email;
  private UUID challengeCode;

  @NotNull
  @Pattern(regexp = EMAIL_PATTERN)
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @NotNull
  public UUID getChallengeCode() {
    return challengeCode;
  }

  public void setChallengeCode(UUID challengeCode) {
    this.challengeCode = challengeCode;
  }
}
