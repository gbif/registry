@Institution
Feature: Institution functionality

  Background:
    Given 2 addresses
      | key | address               | city          |
      | 1   | Universitetsparken 15 | Copenhagen    |
      | 2   | Roskildevej 32        | Frederiksberg |
    Given 2 contacts
      | key                                  | firstName | lastName |
      | 9ed62e2a-9288-4516-ad3d-2ffc5e019cb7 | John      | Doe      |
      | e5f16af3-6fb7-4cb0-826b-733d1a4a5e36 | Joseph    | Doe      |
    Given 3 institutions
      | key                                  | code | name               | addressKey | contactKey                           | deleted |
      | b40143bf-c810-4e67-b998-2936cef72bb3 | II   | First institution  | 1          | 9ed62e2a-9288-4516-ad3d-2ffc5e019cb7 | false   |
      | 4c7db8e8-23ac-4392-824f-82e17dd19cc8 | II2  | Second institution | 2          | e5f16af3-6fb7-4cb0-826b-733d1a4a5e36 | false   |
      | 0082dba6-9669-414e-925e-183d7a136554 | code | deleted            |            |                                      | true    |

  @InstitutionCRUD
  Scenario: CRUD institution
    Given new institution
      | code | name            | description       | homepage     |
      | CODE | New institution | dummy description | http://dummy |
    And new institution address
      | address               | city       | province | postalCode | country |
      | Universitetsparken 15 | Copenhagen | Capital  | 2100       | DENMARK |
    And new institution mailing address
      | address               | city       | province | postalCode | country |
      | Universitetsparken 15 | Copenhagen | Capital  | 2100       | DENMARK |
    And new institution tags
      | value    |
      | tagValue |
    And new institution identifiers
      | identifierType | identifier                           |
      | UUID           | c4930a2c-3d3a-4d47-b65b-8347c42bf0a3 |
    When create institution "New institution" using admin "registry_admin"
    Then response status should be 201
    And institution key is present in response
    When get institution by key
    Then response status should be 200

    When update institution "New institution" using admin "registry_admin"
      | description | new dummy description |
    Then response status should be 200
    When get institution by key
    Then response status should be 200

    When delete institution "New institution" using admin "registry_admin"
    Then response status should be 200
    When get institution by key
    Then response status should be 200


  Scenario: create institution without privileges will cause Forbidden 403
    Given new arbitrary valid institution "New institution"
    When create institution "New institution" using user "registry_user"
    Then response status should be 403

  Scenario: update institution without privileges will cause Forbidden 403
    Given institution "First institution" with key "b40143bf-c810-4e67-b998-2936cef72bb3"
    When update institution "First institution" using user "registry_user"
      | description | new dummy description |
    Then response status should be 403

  Scenario: delete institution without privileges will cause Forbidden 403
    Given institution "First institution" with key "b40143bf-c810-4e67-b998-2936cef72bb3"
    When delete institution "First institution" using user "registry_user"
    Then response status should be 403


  Scenario Outline: suggest institutions
    When call suggest institutions with query "<query>"
    Then response status should be 200
    And <number> institution(s) should be suggested

    Scenarios:
      | query       | number | type |
      | institution | 2      | name |
      | II          | 2      | code |
      | II2         | 1      | code |
      | First       | 1      | name |

  Scenario Outline: list institutions by query
    When list institutions by query "<query>"
    Then response status should be 200
    And <number> institution(s) in response

    Scenarios:
      | query  | number |
      |        | 2      |
      | Copenh | 1      |
      | wrong  | 0      |

  Scenario Outline: list institutions by contact
    When list institutions by contact "<contact>"
    Then response status should be 200
    And <number> institution(s) in response

    Scenarios:
      | contact                              | number | comment                  |
      | 9ed62e2a-9288-4516-ad3d-2ffc5e019cb7 | 1      | John Doe's contact key   |
      | e5f16af3-6fb7-4cb0-826b-733d1a4a5e36 | 1      | Joseph Doe's contact key |
      | 0082dba6-9669-414e-925e-183d7a136554 | 0      | Arbitrary UUID           |

  Scenario: list deleted institutions
    When list deleted institutions
    Then response status should be 200
    And 1 institution(s) in response
