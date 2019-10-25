@UserManagement
Feature: User management functionality

  Scenario: Create a user
    When create a new user
    Then response status should be 201
    And created user reflects the original one
    When login with new user
    Then response status should be 401
    When confirm user's challenge code
    Then response status should be 201
    And response of confirmation is valid
    When login with new user
    Then response status should be 200
