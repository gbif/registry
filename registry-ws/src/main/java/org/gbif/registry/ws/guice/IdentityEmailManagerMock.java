package org.gbif.registry.ws.guice;

import org.gbif.api.model.common.User;
import org.gbif.identity.email.IdentityEmailManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mock implementation of {@link IdentityEmailManager} that keeps information in variable instead of
 * sending real emails.
 */
public class IdentityEmailManagerMock implements IdentityEmailManager {

  private final Map<String, UUID> emailToChallengeCode = new HashMap<>();

  @Override
  public void generateAndSendUserCreated(User user, UUID challengeCode) {
    emailToChallengeCode.put(user.getEmail(), challengeCode);
  }

  @Override
  public void generateAndSendPasswordReset(User user, UUID challengeCode) {
    emailToChallengeCode.put(user.getEmail(), challengeCode);
  }

  public UUID getChallengeCode(String email){
    return emailToChallengeCode.get(email);
  }

  public Map<String, UUID> getAllChallengeCode() {
    return new HashMap<>(emailToChallengeCode);
  }

}
