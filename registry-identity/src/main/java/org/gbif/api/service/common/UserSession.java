package org.gbif.api.service.common;

import org.gbif.api.model.common.User;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * Immutable user representation returned by the WebService.
 * Contains only what we need to expose via WebService response.
 */
public class UserSession {

  private String userName;
  private String firstName;
  private String lastName;
  private String email;
  private Map<String, String> settings = Maps.newHashMap();

  private String session;

  public static UserSession from(User user, String session){
    if(user == null) {
      return null;
    }
    return new UserSession(user, session);
  }

  public static UserSession from(User user){
    return from(user, null);
  }

  /**
   * Only used for json deserialization
   */
  public UserSession() {

  }

  private UserSession(User user, String session) {
    userName = user.getUserName();
    firstName = user.getFirstName();
    lastName = user.getLastName();
    email = user.getEmail();
    settings.putAll(user.getSettings());
    this.session = session;
  }

  public String getUserName() {
    return userName;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getEmail() {
    return email;
  }

  public Map<String, String> getSettings() {
    return settings;
  }

  public String getSession() {
    return session;
  }
}
