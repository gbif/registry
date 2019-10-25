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

  @NetworkEntityContacts
  Scenario Outline: <entity> contacts
    When create new <entity>
    Then response status should be 201
    When list <entity> contacts
    Then response status should be 200
    And <entity> contacts list should be empty
    When add contact to <entity>
    Then response status should be 201

    Scenarios:
      | entity       |
      | installation |
#      | dataset      |
      | network      |
#      | node         |
      | organization |

