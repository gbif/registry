package org.gbif.api.model.common;

import org.gbif.api.vocabulary.UserRole;

import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * An abstract GBIF user account.
 */
public abstract class AbstractUser {
  protected static final String EMAIL_PATTERN =
          "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

  protected String userName;
  protected String firstName;
  protected String lastName;
  protected String email;
  protected Set<UserRole> roles = Sets.newHashSet();
  // Note: Settings was introduced in the system developed to replace Drupal
  protected Map<String, String> settings = Maps.newHashMap();

  @NotNull
  @Pattern(regexp = EMAIL_PATTERN)
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * The unique, immutable drupal user account name.
   * This name should be used for referring to a user.
   * The account name is made of ASCII lower case alphanumerics, underscore, dash or dots and is in particular void
   * of whitespace.
   */
  @NotNull
  @Pattern(regexp = "^[a-z0-9_.-]+$")
  @Size(min = 3, max = 64)
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  /**
   * @return the first name of a person
   */
  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  /**
   * @return the last name of the user
   */
  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  /**
   * @return the first and last name of the user concatenated with a space
   */
  @JsonIgnore
  public String getName() {
    return firstName + " " + lastName;
  }

  @NotNull
  public Set<UserRole> getRoles() {
    return roles;
  }

  public void setRoles(Set<UserRole> roles) {
    this.roles = roles;
  }

  public void addRole(UserRole role) {
    roles.add(role);
  }

  /**
   * Checks if the user has the given user role.
   * @param role
   *
   * @return true if the user has the requested role
   */
  public boolean hasRole(UserRole role) {
    if (role != null && roles.contains(role)) {
      return true;
    }
    return false;
  }

  public boolean isAdmin() {
    return roles.contains(UserRole.ADMIN);
  }


  /**
   * Gets the settings which may be empty but never null.
   * @return
   */
  @NotNull
  public Map<String, String> getSettings() {
    return settings;
  }

  /**
   * Sets the settings object, setting an empty map if null is provided.
   */
  public void setSettings(Map<String, String> settings) {
    // safeguard against misuse to avoid NPE
    this.settings = settings == null ? Maps.<String,String>newHashMap() : settings;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AbstractUser)) {
      return false;
    }

    AbstractUser that = (AbstractUser) obj;
    return Objects.equal(this.userName, that.userName)
            && Objects.equal(this.firstName, that.firstName)
            && Objects.equal(this.lastName, that.lastName)
            && Objects.equal(this.email, that.email)
            && Objects.equal(this.roles, that.roles)
            && Objects.equal(this.settings, that.settings);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(userName, firstName, lastName, email, roles, settings);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
            .add("accountName", userName)
            .add("firstName", firstName)
            .add("lastName", lastName)
            .add("email", email)
            .add("roles", roles)
            .add("settings", settings)
            .toString();
  }

}
