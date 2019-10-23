@NetworkEntity
Feature: Network entity functionality

  @NetworkEntityCRUD
  Scenario Outline: CRUD <entity>
    When create new <entity>
    Then response status should be 201
    When get <entity> by key
    Then response status should be 200
    When update <entity> with new title "New Title"
    Then response status should be 200
    When get <entity> by key
    Then response status should be 200
    And title is new "New Title"
    And modification date was updated
    And modification date is after the creation date

    Examples:
      | entity       |
      | installation |
      | network      |
      | node         |
      | organization |
