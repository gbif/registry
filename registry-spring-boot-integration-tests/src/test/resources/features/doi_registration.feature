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
    When register DOI of type "<type>" for entity with key "<key>"
    Then response status should be 201

    Scenarios:
      | type         | key                                  |
      | DATASET      | 17d12c7f-12a6-4910-aca2-11b6247d937d |
      | DOWNLOAD     | 0000251-150304104939900              |
      | DATA_PACKAGE |                                      |
