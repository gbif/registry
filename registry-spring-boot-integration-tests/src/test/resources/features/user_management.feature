@UserManagement
Feature: User management functionality

  Scenario: Create, confirm and login with new user. Use valid APP role
    When create a new user "user_14" with APP role "gbif.app.it"
    Then response status should be 201
    And user "user_14" reflects the original one
    When login with user "user_14" and password "welcome"
    Then response status should be 401
    When confirm user's challenge code with APP role "gbif.app.it"
    Then response status should be 201
    And response of confirmation is valid
    When login with user "user_14" and password "welcome"
    Then response status should be 200

  Scenario: Create a user. Use invalid APP role
    When create a new user "user_14" with APP role "gbif.app.it2"
    Then response status should be 403

  Scenario: Get user
    When get user by "justuser"
    Then response status should be 200
    And returned user "justuser" is valid

  Scenario: Get use by system settings
    When get user by system settings "100_tacos=100$" by admin
    Then response status should be 200
    And returned user "justadmin" is valid
    When get user by system settings "100_tacos=100$" by APP role "gbif.app.it"
    Then response status should be 200
    And returned user "justadmin" is valid

  Scenario: Reset password for user
    When reset password for user "user_reset_password" by APP role "gbif.app.it"
    Then response status should be 204
    And challenge code for user "user_reset_password" is present now
    When login with user "user_reset_password" and password "welcome"
    Then response status should be 200

  Scenario: Update password for user
    When update password to "newpass" for user "user_update_password" by APP role "gbif.app.it" with wrong challenge code
    Then response status should be 401
    When reset password for user "user_update_password" by APP role "gbif.app.it"
    Then response status should be 204
    And challenge code for user "user_update_password" is present now
    And challenge code is valid for user "user_update_password" by APP role "gbif.app.it"
    When update password to "newpass" for user "user_update_password" by APP role "gbif.app.it" with valid challenge code
    Then response status should be 201
    When login with user "user_update_password" and password "newpass"
    Then response status should be 200

  Scenario Outline: Update user with valid values by APP role
    When update user "justadmin" with new <property> "<newValue>" by APP role "gbif.app.it"
    Then response status should be <status>
    And <property> of user "justadmin" was updated with new value "<newValue>"

    Scenarios:
      | property  | newValue          | status |
      | firstName | Joseph            | 204    |
      | lastName  | Smith             | 204    |

  Scenario: Update user with wrong email by APP role
    When update user "justadmin" with new email "justuser@gbif.org" by APP role "gbif.app.it"
    Then response status should be 422
    And response should be "EMAIL_ALREADY_IN_USE"
