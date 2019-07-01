package org.gbif.api.service.common;

import com.google.common.collect.Maps;
import org.gbif.api.model.common.GbifUser;

import java.util.Map;

/**
 * Immutable user representation returned by the WebService.
 * Contains only what we need to expose via WebService response.
 */
public class LoggedUser {

  private String userName;
  private String firstName;
  private String lastName;
  private String email;
  private final Map<String, String> settings = Maps.newHashMap();

  public static LoggedUser from(GbifUser user){
    if(user == null) {
      return null;
    }
    return new LoggedUser(user);
  }

  /**
   * Only used for json deserialization
   */
  public LoggedUser() {

  }

  private LoggedUser(GbifUser user) {
    userName = user.getUserName();
    firstName = user.getFirstName();
    lastName = user.getLastName();
    email = user.getEmail();
    settings.putAll(user.getSettings());
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
}
