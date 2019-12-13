@OccurrenceDownload
Feature: Occurrence Download functionality

  Scenario: create and get equals predicate download
    Given equals predicate download
    When create download
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
    When create download
    Then response status should be 201
    And extract doi from download
    When get download
    Then response status should be 200
    And null predicate download assertions passed
      | license     | sendNotification | format | status    | downloadLink | size | totalRecords | numberDatasets |
      | UNSPECIFIED | true             | DWCA   | PREPARING | testUrl      | 0    | 0            | 0              |

  Scenario: create and get sql download
    Given sql download
    When create download
    Then response status should be 201
    And extract doi from download
    When get download
    Then response status should be 200
    And sql download assertions passed
      | license     | sql                               | sendNotification | format | status    | downloadLink | size | totalRecords | numberDatasets |
      | UNSPECIFIED | SELECT datasetKey FROM occurrence | true             | SQL    | PREPARING | testUrl      | 0    | 0            | 0              |

  Scenario: create and get null sql download
    Given sql download
    When create download
    Then response status should be 201
    And extract doi from download
    When get download
    Then response status should be 200
    And null sql download assertions passed
      | license     | sendNotification | format | status    | downloadLink | size | totalRecords | numberDatasets |
      | UNSPECIFIED | true             | SQL    | PREPARING | testUrl      | 0    | 0            | 0              |
