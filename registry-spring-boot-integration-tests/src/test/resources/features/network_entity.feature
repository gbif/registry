@NetworkEntity
Feature: Network entity functionality

  @NetworkEntityCRUD
  Scenario Outline: CRUD <entity>
    When create new <entity>
    Then response status should be 201
    When get <entity> by key
    Then response status should be 200
    And modification and creation dates are present
    And <entity> is not marked as deleted
    And created <entity> reflects the original one

    When update <entity> with new title "New Title"
    Then response status should be 200
    When get <entity> by key
    Then response status should be 200
    And title is new "New Title"
    And modification date was updated
    And modification date is after the creation date

    When delete <entity> by key
    Then response status should be 200
    When get <entity> by key
    Then response status should be 200
    And deleted <entity> reflects the original one

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
