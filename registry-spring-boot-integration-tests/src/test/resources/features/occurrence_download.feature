@OccurrenceDownload
Feature: Occurrence Download functionality

  Scenario: create and get equals predicate download
    Given equals predicate download
    When create download using admin "registry_admin"
    Then response status should be 201
    And extract doi from download
    When get download
    Then response status should be 200
    And predicate download assertions passed
      | license                  | UNSPECIFIED |
      | request.predicate.type   | equals      |
      | request.predicate.key    | TAXON_KEY   |
      | request.predicate.value  | 212         |
      | request.sendNotification | true        |
      | request.format           | DWCA        |
      | status                   | PREPARING   |
      | downloadLink             | testUrl     |
      | size                     | 0           |
      | totalRecords             | 0           |
      | numberDatasets           | 0           |
    When get download by doi
    Then response status should be 200
    And predicate download assertions passed
      | license                  | UNSPECIFIED |
      | request.predicate.type   | equals      |
      | request.predicate.key    | TAXON_KEY   |
      | request.predicate.value  | 212         |
      | request.sendNotification | true        |
      | request.format           | DWCA        |
      | status                   | PREPARING   |
      | downloadLink             | testUrl     |
      | size                     | 0           |
      | totalRecords             | 0           |
      | numberDatasets           | 0           |

  Scenario: create and get null predicate download
    Given null predicate download
    When create download using admin "registry_admin"
    Then response status should be 201
    And extract doi from download
    When get download
    Then response status should be 200
    And predicate download assertions passed
      | license                  | UNSPECIFIED |
      | request.sendNotification | true        |
      | request.format           | DWCA        |
      | status                   | PREPARING   |
      | downloadLink             | testUrl     |
      | size                     | 0           |
      | totalRecords             | 0           |
      | numberDatasets           | 0           |

  Scenario: create and get sql download
    Given sql download
    When create download using admin "registry_admin"
    Then response status should be 201
    And extract doi from download
    When get download
    Then response status should be 200
    And sql download assertions passed
      | license                  | UNSPECIFIED                       |
      | request.sql              | SELECT datasetKey FROM occurrence |
      | request.sendNotification | true                              |
      | request.format           | SQL                               |
      | status                   | PREPARING                         |
      | downloadLink             | testUrl                           |
      | size                     | 0                                 |
      | totalRecords             | 0                                 |
      | numberDatasets           | 0                                 |

  Scenario: create and get null sql download
    Given sql download
    When create download using admin "registry_admin"
    Then response status should be 201
    And extract doi from download
    When get download
    Then response status should be 200
    And sql download assertions passed
      | license                  | UNSPECIFIED |
      | request.sendNotification | true        |
      | request.format           | SQL         |
      | status                   | PREPARING   |
      | downloadLink             | testUrl     |
      | size                     | 0           |
      | totalRecords             | 0           |
      | numberDatasets           | 0           |

  Scenario: only admin is allowed to create occurrence download
    Given equals predicate download
    When create download using user "registry_user"
    Then response status should be 403

  Scenario: create occurrence download with invalid parameters is not allowed
    Given equals predicate download
    And download with invalid parameters
      | key  | request | status | created    | modified   |
      | null | null    | null   | 13-12-2019 | 13-12-2019 |
    When create download using admin "registry_admin"
    Then response status should be 422
    And download creation error response is
      | created                                      | key                                          | modified                                      | request                                          | status                                          |
      | Validation of [created] failed: must be null | Validation of [key] failed: must not be null | Validation of [modified] failed: must be null | Validation of [request] failed: must not be null | Validation of [status] failed: must not be null |

  @OccurrenceDownloadList
  Scenario: list occurrence downloads
    Given 3 predicate downloads
    And 3 sql downloads
    When list downloads using admin "registry_admin"
    Then response status should be 200
    And 6 downloads in occurrence downloads list response

  @OccurrenceDownloadList
  Scenario: list occurrence downloads by status
    Given 3 predicate downloads
    And 3 sql downloads
    When list downloads using admin "registry_admin" with query params
      | status | PREPARING,RUNNING,SUSPENDED |
    Then response status should be 200
    And 5 downloads in occurrence downloads list response

  @OccurrenceDownloadList
  Scenario: list occurrence downloads by user
    Given 3 predicate downloads
    And 3 sql downloads
    When list downloads by user "registry_user" using user "registry_user"
    Then response status should be 200
    And 3 downloads in occurrence downloads list response

  @OccurrenceDownloadList
  Scenario: list occurrence downloads by user and status
    Given 3 predicate downloads
    And 3 sql downloads
    When list downloads by user "registry_user" using user "registry_user" with query params
      | status | PREPARING,RUNNING,SUSPENDED |
    Then response status should be 200
    And 2 downloads in occurrence downloads list response

  @OccurrenceDownloadList
  Scenario: list occurrence downloads by user
    Given 3 predicate downloads
    And 3 sql downloads
    When list downloads by user "WS TEST" using user "registry_admin"
    Then response status should be 200
    And 3 downloads in occurrence downloads list response

  @OccurrenceDownloadList
  Scenario: list occurrence downloads by user using user which does not match user param should be Unauthorized 401
    When list downloads by user "WS TEST" using user "registry_user"
    Then response status should be 401

  @OccurrenceDownloadList
  Scenario: list occurrence downloads by non admin user should be Forbidden 403
    When list downloads using user "registry_user"
    Then response status should be 403
