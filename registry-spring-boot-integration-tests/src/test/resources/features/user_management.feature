@UserManagement
Feature: User management functionality

  Scenario: Create, confirm and login with new user. Use valid APP role
    When create a new user with APP role "gbif.app.it"
    Then response status should be 201
    And created user reflects the original one
    When login with new user
    Then response status should be 401
    When confirm user's challenge code with APP role "gbif.app.it"
    Then response status should be 201
    And response of confirmation is valid
    When login with new user
    Then response status should be 200

  Scenario: Create a user. Use invalid APP role
    When create a new user with APP role "gbif.app.it2"
    Then response status should be 403
