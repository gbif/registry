@UserManagement
Feature: User management functionality

  Scenario: Create, confirm and login with new user. Use valid APP role
    When create a new user "user_14" with APP role "gbif.app.it"
    Then response status should be 201
    And user "user_14" reflects the original one
    When login with user "user_14"
    Then response status should be 401
    When confirm user's challenge code with APP role "gbif.app.it"
    Then response status should be 201
    And response of confirmation is valid
    When login with user "user_14"
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
    When login with user "user_reset_password"
    Then response status should be 200
