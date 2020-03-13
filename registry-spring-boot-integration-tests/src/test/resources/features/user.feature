@User
Feature: User functionality

  Background:
    Given user "user_12"
    And user "user_13"

  Scenario Outline: Login with no credentials <method>
    When login "<method>" with no credentials
    Then response status should be <status>

    Scenarios:
      | method | status |
      | GET    | 401    |
      | POST   | 401    |

  Scenario Outline: Login with valid credentials <method> <login>/<password>
    When login "<method>" with valid credentials login "<login>" and password "<password>"
    Then response status should be <status>
    And user "<login>" is logged in
      | userName | firstName | lastName  | email            |
      | user_12  | Tim       | Robertson | user_12@gbif.org |
    And JWT is present in the response

    Scenarios:
      | method | login            | password | status |
      | GET    | user_12          | welcome  | 200    |
      | GET    | user_12@gbif.org | welcome  | 200    |
      | POST   | user_12          | welcome  | 201    |
      | POST   | user_12@gbif.org | welcome  | 201    |

  Scenario: Change password
    When change password for user "user_13" from "welcome" to "123456"
    Then response status should be 204
    When login "user_13" with old password "welcome"
    Then response status should be 401
    When login "user_13" with new password "123456"
    Then response status should be 200

  Scenario: Change password too short fails with Unprocessable Entity 422
    When change password for user "user_12" from "welcome" to "1"
    Then response status should be 422
    And change password response contains error information "PASSWORD_LENGTH_VIOLATION"

  Scenario Outline: Login by <state> APP role
    Given <state> request with <body> body and sign
    When <state> login by APP role
    Then response status should be <status>
    And <state> request verifications passed

    Scenarios:
      | state   | body    | status |
      | invalid | absent  | 403    |
      | valid   | present | 201    |

  Scenario: Get current user for existing user "user_12" should be successful
    When perform whoami request for user "user_12" with password "welcome"
    Then response status should be 201
    And user "user_12" is logged in
      | userName | firstName | lastName  | email            |
      | user_12  | Tim       | Robertson | user_12@gbif.org |
