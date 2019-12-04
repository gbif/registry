@UserManagement
Feature: User management functionality

  Background:
    Given user "user_reset_password"
    And user "user_update_password"

  @UserCRUD
  Scenario: Create, confirm, login and then delete user. Use valid APP role
    When create new user "user_14" with password "welcome" by APP role "gbif.app.it"
    Then response status should be 201
    And user "user_14" reflects the original one
    When login with user "user_14" and password "welcome"
    Then response status should be 401
    When confirm user's challenge code with APP role "gbif.app.it"
    Then response status should be 201
    And response of confirmation is valid
    When login with user "user_14" and password "welcome"
    Then response status should be 200
    When delete user "user_14"
    Then response status should be 204
    When login with user "user_14" and password "welcome"
    Then response status should be 401

  Scenario: Create user. Use invalid APP role
    When create new user "user_14" with password "welcome" by APP role "gbif.app.it2"
    Then response status should be 403

  Scenario: Create user which already exist fails with Unprocessable Entity 422
    When create existing user "USER" with password "welcome" by APP role "gbif.app.it"
    Then response status should be 422
    And create user response contains error information "USER_ALREADY_EXIST"

  Scenario: Create user with too short password fails with Unprocessable Entity 422
    When create new user "new_user" with password "1" by APP role "gbif.app.it"
    Then response status should be 422
    And create user response contains error information "PASSWORD_LENGTH_VIOLATION"

  Scenario: Get user
    When get user by "USER"
    Then response status should be 200
    And returned user "USER" is valid

  Scenario: Get use by system settings
    When get user by system settings "100_tacos=100$" by admin
    Then response status should be 200
    And returned user "ADMIN" is valid
    When get user by system settings "100_tacos=100$" by APP role "gbif.app.it"
    Then response status should be 200
    And returned user "ADMIN" is valid

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
    When update user "ADMIN" with new <property> "<newValue>" by APP role "gbif.app.it"
    Then response status should be <status>
    And <property> of user "ADMIN" was updated with new value "<newValue>"

    Scenarios:
      | property  | newValue | status |
      | firstName | Joseph   | 204    |
      | lastName  | Smith    | 204    |

  Scenario Outline: Update user with valid values by admin
    Given user which is admin with credentials "ADMIN" and "welcome"
    When update user "ADMIN" with new <property> "<newValue>" by admin "ADMIN"
    Then response status should be <status>
    And <property> of user "ADMIN" was updated with new value "<newValue>"

    Scenarios:
      | property  | newValue | status |
      | firstName | James    | 204    |
      | lastName  | White    | 204    |

  Scenario: Update user with wrong email by APP role
    When update user "ADMIN" with new email "user@mailinator.com" by APP role "gbif.app.it"
    Then response status should be 422
    And response should be "EMAIL_ALREADY_IN_USE"

  Scenario: Editor rights
    Given user which is admin with credentials "ADMIN" and "welcome"
    And user which is user with credentials "USER" and "welcome"
    When "ADMIN" adds a right "8b207f7a-fd9c-4992-8193-ca56948fa679" to the user "USER"
    Then response status should be 201
    When "ADMIN" gets user "USER" rights
    Then response status should be 200
    And response is "8b207f7a-fd9c-4992-8193-ca56948fa679"
    When "USER" gets user "USER" rights
    Then response status should be 200
    And response is "8b207f7a-fd9c-4992-8193-ca56948fa679"
    When "ADMIN" deletes user "USER" right "8b207f7a-fd9c-4992-8193-ca56948fa679"
    Then response status should be 204
    When "ADMIN" gets user "USER" rights
    Then response status should be 200
    And response is ""

  Scenario Outline: Editor rights create errors: <comment>
    Given user which is <role> with credentials "<performer>" and "<password>"
    When "<performer>" adds a right "8b207f7a-fd9c-4992-8193-ca56948fa679" to the user "<username>"
    Then response status should be <status>

    Scenarios:
      | username        | performer | password | role  | status | comment                                   |
      | notexistinguser | ADMIN     | welcome  | admin | 404    | User does not exist                       |
      | USER            | USER      | welcome  | user  | 403    | Not an admin user                         |
      | USER            | ADMIN     | welcome  | admin | 201    | Create one in order to fail the next step |
      | USER            | ADMIN     | welcome  | admin | 409    | Right already exists                      |

  Scenario Outline: Editor rights delete errors: <comment>
    Given user which is <role> with credentials "<performer>" and "<password>"
    When "<performer>" deletes user "<username>" right "<right>"
    Then response status should be <status>

    Scenarios:
      | username        | right                                | performer | password | role  | status | comment              |
      | USER            | e323a550-ad60-408d-88d2-cc1356fc10fb | ADMIN     | welcome  | admin | 404    | Right does not exist |
      | notexistinguser | 8b207f7a-fd9c-4992-8193-ca56948fa679 | ADMIN     | welcome  | admin | 404    | User does not exist  |
      | USER            | 8b207f7a-fd9c-4992-8193-ca56948fa679 | USER      | welcome  | user  | 403    | Not an admin user    |

  Scenario: List roles should have 10 roles
    When perform list roles
    Then response status should be 200
    And roles response should be
      | USER | REGISTRY_ADMIN | REGISTRY_EDITOR | DATA_REPO_USER | COL_ADMIN | COL_EDITOR | VOCABULARY_ADMIN | VOCABULARY_EDITOR | GRSCICOLL_ADMIN | GRSCICOLL_EDITOR |

  Scenario: Search users
    When perform search user with query "user"
    Then response status should be 200
    And search users response are
      | key | userName             | firstName | lastName  | email                         | roles |
      | 2   | USER                 | John      | User      | user@mailinator.com           | USER  |
      | 6   | user_reset_password  | Tim       | Robertson | user_reset_password@gbif.org  | USER  |
      | 7   | user_update_password | Tim       | Robertson | user_update_password@gbif.org | USER  |

