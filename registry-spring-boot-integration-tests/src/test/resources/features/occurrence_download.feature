@OccurrenceDownload
Feature: Occurrence Download functionality

  Scenario: create and get equals predicate download
    Given equals predicate download
    When create download by admin "registry_admin"
    Then response status should be 201
    And extract doi from download
    When get download
    Then response status should be 200
    And equals predicate download assertions passed
      | license     | predicateType | predicateKey | predicateValue | sendNotification | format | status    | downloadLink | size | totalRecords | numberDatasets |
      | UNSPECIFIED | equals        | TAXON_KEY    | 212            | true             | DWCA   | PREPARING | testUrl      | 0    | 0            | 0              |
    When get download by doi
    Then response status should be 200
    And equals predicate download assertions passed
      | license     | predicateType | predicateKey | predicateValue | sendNotification | format | status    | downloadLink | size | totalRecords | numberDatasets |
      | UNSPECIFIED | equals        | TAXON_KEY    | 212            | true             | DWCA   | PREPARING | testUrl      | 0    | 0            | 0              |

  Scenario: create and get null predicate download
    Given null predicate download
    When create download by admin "registry_admin"
    Then response status should be 201
    And extract doi from download
    When get download
    Then response status should be 200
    And null predicate download assertions passed
      | license     | sendNotification | format | status    | downloadLink | size | totalRecords | numberDatasets |
      | UNSPECIFIED | true             | DWCA   | PREPARING | testUrl      | 0    | 0            | 0              |

  Scenario: create and get sql download
    Given sql download
    When create download by admin "registry_admin"
    Then response status should be 201
    And extract doi from download
    When get download
    Then response status should be 200
    And sql download assertions passed
      | license     | sql                               | sendNotification | format | status    | downloadLink | size | totalRecords | numberDatasets |
      | UNSPECIFIED | SELECT datasetKey FROM occurrence | true             | SQL    | PREPARING | testUrl      | 0    | 0            | 0              |

  Scenario: create and get null sql download
    Given sql download
    When create download by admin "registry_admin"
    Then response status should be 201
    And extract doi from download
    When get download
    Then response status should be 200
    And null sql download assertions passed
      | license     | sendNotification | format | status    | downloadLink | size | totalRecords | numberDatasets |
      | UNSPECIFIED | true             | SQL    | PREPARING | testUrl      | 0    | 0            | 0              |

  Scenario: only admin is allowed to create occurrence download
    Given equals predicate download
    When create download by user "registry_user"
    Then response status should be 403

  Scenario: create occurrence download with invalid parameters is not allowed
    Given equals predicate download
    And download with invalid parameters
      | key  | request | status | created    | modified   |
      | null | null    | null   | 13-12-2019 | 13-12-2019 |
    When create download by admin "registry_admin"
    Then response status should be 422
    And download creation error response is
      | created                                      | key                                          | modified                                      | request                                          | status                                          |
      | Validation of [created] failed: must be null | Validation of [key] failed: must not be null | Validation of [modified] failed: must be null | Validation of [request] failed: must not be null | Validation of [status] failed: must not be null |
