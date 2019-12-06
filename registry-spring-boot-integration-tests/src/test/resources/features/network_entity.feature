@NetworkEntity
Feature: Network entity functionality

  @NetworkEntityCreateExceptions
  Scenario Outline: Regular user or editor without rights can't create <entity>
    When create new <entity> by user "registry_user" and password "welcome"
    Then response status should be 403
    When create new <entity> by editor "registry_editor" and password "welcome"
    Then response status should be 403

    Scenarios:
      | entity       |
      | installation |
      | dataset      |
      | network      |
      | node         |
      | organization |

  @NetworkEntityUpdateExceptions
  Scenario Outline: Regular user or editor without rights can't update <entity>
    When update <entity> by user "registry_user" and password "welcome"
      | title | New Title |
    Then response status should be 403
    When update <entity> by editor "registry_editor" and password "welcome"
      | title | New Title |
    Then response status should be 403

    Scenarios:
      | entity       |
      | installation |
      | dataset      |
      | network      |
      | node         |
      | organization |

  # nodes do not support this functionality
  @NetworkEntityContacts
  Scenario Outline: <entity> contacts
    When create new <entity>
    Then response status should be 201
    And <entity> key is present in response
    When list <entity> contacts
    Then response status should be 200
    And <entity> contacts list should contain 0 contacts

    When add first contact to <entity>
    Then response status should be 201
    When add second contact to <entity>
    Then response status should be 201
    When list <entity> contacts
    Then response status should be 200
    And <entity> contacts list should contain 2 contacts
    And only second contact is primary

    When delete <entity> contact
    Then response status should be 200
    When list <entity> contacts
    Then response status should be 200
    And <entity> contacts list should contain 1 contacts
    And <entity> contact reflects the original one

    Scenarios:
      | entity       |
      | installation |
      | dataset      |
      | network      |
      | organization |

  @NetworkEntityEndpoints
  Scenario Outline: <entity> endpoints
    When create new <entity>
    Then response status should be 201
    And <entity> key is present in response
    When list <entity> endpoints
    Then response status should be 200
    And <entity> endpoints list should contain 0 endpoints

    When add first endpoint to <entity>
    Then response status should be 201
    When add second endpoint to <entity>
    Then response status should be 201
    When list <entity> endpoints
    Then response status should be 200
    And <entity> endpoints list should contain 2 endpoints

    Scenarios:
      | entity       |
      | installation |
      | node         |
      | dataset      |
      | network      |
      | organization |

  @NetworkEntityComments
  Scenario Outline: <entity> comments
    When create new <entity>
    Then response status should be 201
    And <entity> key is present in response
    When list <entity> comments
    Then response status should be 200
    And <entity> comments list should contain 0 comments

    When add first comment to <entity>
    Then response status should be 201
    When add second comment to <entity>
    Then response status should be 201
    When list <entity> comments
    Then response status should be 200
    And <entity> comments list should contain 2 comments

    Scenarios:
      | entity       |
      | installation |
      | node         |
      | dataset      |
      | network      |
      | organization |

  @NetworkEntityMachineTags
  Scenario Outline: <entity> machine tags
    When create new <entity>
    Then response status should be 201
    And <entity> key is present in response
    When list <entity> machine tags
    Then response status should be 200
    And <entity> machine tags list should contain 0 machine tags

    When add first machine tag to <entity>
    Then response status should be 201
    When add second machine tag to <entity>
    Then response status should be 201
    When list <entity> machine tags
    Then response status should be 200
    And <entity> machine tags list should contain 2 machine tags

    Scenarios:
      | entity       |
      | installation |
      | node         |
      | dataset      |
      | network      |
      | organization |

  @NetworkEntityTags
  Scenario Outline: <entity> tags
    When create new <entity>
    Then response status should be 201
    And <entity> key is present in response
    When list <entity> tags
    Then response status should be 200
    And <entity> tags list should contain 0 tags

    When add first tag to <entity>
    Then response status should be 201
    When add second tag to <entity>
    Then response status should be 201
    When list <entity> tags
    Then response status should be 200
    And <entity> tags list should contain 2 tags

    Scenarios:
      | entity       |
      | installation |
      | node         |
      | dataset      |
      | network      |
      | organization |
