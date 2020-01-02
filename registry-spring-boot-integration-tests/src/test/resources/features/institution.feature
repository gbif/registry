@Institution
Feature: Institution functionality

  Scenario Outline: Institution suggest
    When call suggest institutions with query "<query>" by <type>
    Then response status should be 200
    And <number> institution(s) should be suggested

    Scenarios:
      | query       | number | type |
      | institution | 2      | name |
      | II          | 2      | code |
      | II2         | 1      | code |
      | name2       | 1      | name |
