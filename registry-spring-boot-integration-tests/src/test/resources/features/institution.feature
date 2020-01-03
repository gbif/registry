@Institution
Feature: Institution functionality

  Background:
    Given 2 institutions
      | key                                  | code | name              | deleted |
      | b40143bf-c810-4e67-b998-2936cef72bb3 | II   | Institution name  | false   |
      | 4c7db8e8-23ac-4392-824f-82e17dd19cc8 | II2  | Institution name2 | false   |
      | 0082dba6-9669-414e-925e-183d7a136554 | code | deleted           | true    |

  Scenario Outline: suggest institutions
    When call suggest institutions with query "<query>"
    Then response status should be 200
    And <number> institution(s) should be suggested

    Scenarios:
      | query       | number | type |
      | institution | 2      | name |
      | II          | 2      | code |
      | II2         | 1      | code |
      | name2       | 1      | name |

  Scenario: list institutions without parameters
    When list institutions
    Then response status should be 200
    And 2 institution(s) in response

  Scenario: list deleted institutions
    When list deleted institutions
    Then response status should be 200
    And 1 institution(s) in response
