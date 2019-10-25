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
      | property  | newValue | status |
      | firstName | Joseph   | 204    |
      | lastName  | Smith    | 204    |

  Scenario: Update user with wrong email by APP role
    When update user "justadmin" with new email "justuser@gbif.org" by APP role "gbif.app.it"
    Then response status should be 422
    And response should be "EMAIL_ALREADY_IN_USE"

  Scenario: Editor rights
    Given user which is admin with credentials "justadmin" and "welcome"
    And user which is user with credentials "justuser" and "welcome"
    When "justadmin" adds a right "8b207f7a-fd9c-4992-8193-ca56948fa679" to the user "justuser"
    Then response status should be 201
    When "justadmin" gets user "justuser" rights
    Then response status should be 200
    And response is "8b207f7a-fd9c-4992-8193-ca56948fa679"
    When "justuser" gets user "justuser" rights
    Then response status should be 200
    And response is "8b207f7a-fd9c-4992-8193-ca56948fa679"
    When "justadmin" deletes user "justuser" right "8b207f7a-fd9c-4992-8193-ca56948fa679"
    Then response status should be 204
    When "justadmin" gets user "justuser" rights
    Then response status should be 200
    And response is ""

  Scenario Outline: Editor rights create errors: <comment>
    Given user which is <role> with credentials "<performer>" and "<password>"
    When "<performer>" adds a right "8b207f7a-fd9c-4992-8193-ca56948fa679" to the user "<username>"
    Then response status should be <status>

    Scenarios:
      | username        | performer | password | role  | status | comment                                   |
      | notexistinguser | justadmin | welcome  | admin | 404    | User does not exist                       |
      | justuser        | justuser  | welcome  | user  | 403    | Not an admin user                         |
      | justuser        | justadmin | welcome  | admin | 201    | Create one in order to fail the next step |
      | justuser        | justadmin | welcome  | admin | 409    | Right already exists                      |

  Scenario Outline: Editor rights delete errors: <comment>
    Given user which is <role> with credentials "<performer>" and "<password>"
    When "<performer>" deletes user "<username>" right "<right>"
    Then response status should be <status>

    Scenarios:
      | username        | right                                | performer | password | role  | status | comment              |
      | justuser        | e323a550-ad60-408d-88d2-cc1356fc10fb | justadmin | welcome  | admin | 404    | Right does not exist |
      | notexistinguser | 8b207f7a-fd9c-4992-8193-ca56948fa679 | justadmin | welcome  | admin | 404    | User does not exist  |
      | justuser        | 8b207f7a-fd9c-4992-8193-ca56948fa679 | justuser  | welcome  | user  | 403    | Not an admin user    |
