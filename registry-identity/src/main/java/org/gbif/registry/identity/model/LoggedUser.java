/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.identity.model;

import org.gbif.api.model.common.GbifUser;

import java.util.HashMap;
import java.util.Map;

/**
 * User representation returned by the WebService. Contains only what we need to expose via
 * WebService response.
 */
public class LoggedUser {

  private String userName;
  private String firstName;
  private String lastName;
  private String email;
  private final Map<String, String> settings = new HashMap<>();

  public static LoggedUser from(GbifUser user) {
    if (user == null) {
      return null;
    }
    return new LoggedUser(user);
  }

  /** Only used for json deserialization */
  public LoggedUser() {}

  protected LoggedUser(GbifUser user) {
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
