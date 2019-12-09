@DoiRegistration
Feature: DOI functionality

  Scenario Outline: generate <type> DOI
    When generate new DOI of type "<type>"
    Then response status should be 201
    And DOI is returned

    Scenarios:
      | type         |
      | DATASET      |
      | DOWNLOAD     |
      | DATA_PACKAGE |

  @DoiCRUD
  Scenario Outline: register <type> DOI
    When register DOI of type "<type>" for entity with key "<key>" and metadata parameters
      | title | New DOI title |
    Then response status should be 201
    And DOI is returned

    When get DOI
    Then response status should be 200

    When update DOI of type "<type>" for entity with key "<key>" and metadata parameters
      | title | Updated DOI title |
    Then response status should be 200

    When delete DOI of type "<type>"
    Then response status should be 200

    Scenarios:
      | type         | key                                  |
      | DATASET      | 17d12c7f-12a6-4910-aca2-11b6247d937d |
      | DOWNLOAD     | 0000251-150304104939900              |
      | DATA_PACKAGE |                                      |

  Scenario: create existing registered DOI should be Bad Request 400
    Given existing DOI "10.21373/test_registered_doi" with status "REGISTERED"
    When register DOI "10.21373/test_registered_doi" of type "DATA_PACKAGE" for entity with key "" and metadata parameters
      | title | New DOI title |
    Then response status should be 400

  Scenario: update existing deleted DOI should be Bad Request 400
    Given existing DOI "10.21373/test_deleted_doi" with status "DELETED"
    When update DOI "10.21373/test_deleted_doi" of type "DATA_PACKAGE" for entity with key "" and metadata parameters
      | title | Updated DOI title |
    Then response status should be 400
