@DoiRegistration
Feature: DOI functionality

  Scenario Outline: generate <type> DOI
    When generate new DOI of type "<type>" by admin
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

  Scenario: register existing registered DOI should be Bad Request 400
    Given existing DOI "10.21373/test_registered_doi" with status "REGISTERED"
    When register DOI "10.21373/test_registered_doi" of type "DATA_PACKAGE" for entity with key "" and metadata parameters by admin
      | title | New DOI title |
    Then response status should be 400

  Scenario: register existing new DOI should update DOI instead
    Given existing DOI "10.21373/test_new_doi" with status "NEW"
    When register DOI "10.21373/test_new_doi" of type "DATA_PACKAGE" for entity with key "" and metadata parameters by admin
      | title | New DOI title |
    Then response status should be 201

  Scenario: update existing deleted DOI should be Bad Request 400
    Given existing DOI "10.21373/test_deleted_doi" with status "DELETED"
    When update DOI "10.21373/test_deleted_doi" of type "DATA_PACKAGE" for entity with key "" and metadata parameters by admin
      | title | Updated DOI title |
    Then response status should be 400

  Scenario: update existing DOI with DOI present in request
    Given existing DOI "10.21373/test_registered_doi" with status "REGISTERED"
    When update DOI "10.21373/test_registered_doi" of type "DATA_PACKAGE" for entity with key "" and metadata parameters by admin
      | title | Updated DOI title |
    Then response status should be 200

  Scenario: get not existing DOI should be Not Found 404
    When get DOI "10.21373/not_existing"
    Then response status should be 404

  Scenario: register or update by unauthorized user should be 401 Unauthorized
    When generate new DOI of type "DATA_PACKAGE" by unauthorized
    Then response status should be 401
    When register DOI "10.21373/test_doi" of type "DATA_PACKAGE" for entity with key "" and metadata parameters by unauthorized
      | title | New DOI title |
    Then response status should be 401
    When update DOI "10.21373/test_doi" of type "DATA_PACKAGE" for entity with key "" and metadata parameters by unauthorized
      | title | Updated DOI title |
    Then response status should be 401
