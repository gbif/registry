@DoiRegistration
Feature: DOI registration functionality

  Scenario Outline: generate <type> DOI
    When generate new DOI of type "<type>"
    Then response status should be 201
    And DOI is returned

    Scenarios:
      | type         |
      | DATASET      |
      | DOWNLOAD     |
      | DATA_PACKAGE |

  Scenario Outline: register <type> DOI
    When register DOI of type "<type>"
    Then response status should be 201

    Scenarios:
      | type         |
      | DATASET      |
      | DOWNLOAD     |
      | DATA_PACKAGE |
