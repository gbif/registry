package org.gbif.api.service.common;

public class LoggedUserWithJwt {

  private LoggedUser loggedUser;
  private String jwt;

  // needed for JSON deserialization
  public LoggedUserWithJwt() { }

  public LoggedUserWithJwt(LoggedUser loggedUser, String jwt) {
    this.loggedUser = loggedUser;
    this.jwt = jwt;
  }

  public LoggedUser getLoggedUser() {
    return loggedUser;
  }

  public String getJwt() {
    return jwt;
  }
}
