@OccurrenceDownload
Feature: Occurrence Download functionality

  Background:
    Given 6 occurrence downloads
      | key                                  | format | createdBy     | status    |
      | ba40b279-7fef-43ab-a0c7-95d4ae2ffaf5 | DWCA   | registry_user | PREPARING |
      | 2393d6f0-cd3f-4395-94ea-0d3389fabeee | DWCA   | WS TEST       | PREPARING |
      | 8b84c7b5-d1e8-4f86-8934-8b6c3298046d | DWCA   | WS TEST       | PREPARING |
      | c277534e-3576-4203-b26c-2ffbe18f61be | SQL    | WS TEST       | PREPARING |
      | 20c7c079-e91a-4cd9-9428-383dd008a47e | SQL    | registry_user | PREPARING |
      | af6093d6-4c57-4b1f-8e3d-ebd84692f8f7 | SQL    | registry_user | CANCELLED |

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

  Scenario: list occurrence downloads
    When list downloads using admin "registry_admin"
    Then response status should be 200
    And 6 downloads in occurrence downloads list response

  Scenario: list occurrence downloads by status
    When list downloads using admin "registry_admin" with query params
      | status | PREPARING,RUNNING,SUSPENDED |
    Then response status should be 200
    And 5 downloads in occurrence downloads list response

  Scenario: list occurrence downloads by user
    When list downloads by user "registry_user" using user "registry_user"
    Then response status should be 200
    And 3 downloads in occurrence downloads list response

  Scenario: list occurrence downloads by user and status
    When list downloads by user "registry_user" using user "registry_user" with query params
      | status | PREPARING,RUNNING,SUSPENDED |
    Then response status should be 200
    And 2 downloads in occurrence downloads list response

  Scenario: list occurrence downloads by user
    When list downloads by user "WS TEST" using user "registry_admin"
    Then response status should be 200
    And 3 downloads in occurrence downloads list response

  Scenario: list occurrence downloads by user using user which does not match user param should be Unauthorized 401
    When list downloads by user "WS TEST" using user "registry_user"
    Then response status should be 401

  Scenario: list occurrence downloads by non admin user should be Forbidden 403
    When list downloads using user "registry_user"
    Then response status should be 403

  Scenario: update occurrence download
    When update occurrence download "ba40b279-7fef-43ab-a0c7-95d4ae2ffaf5" using user "registry_user" with values
      | status | RUNNING |
    Then response status should be 200
    When get download "ba40b279-7fef-43ab-a0c7-95d4ae2ffaf5"
    Then download assertions passed
      | status | RUNNING |

  Scenario: update occurrence download by user using user which does not match download's creator should be Unauthorized 401
    When update occurrence download "2393d6f0-cd3f-4395-94ea-0d3389fabeee" using user "registry_user" with values
      | status | RUNNING |
    Then response status should be 401
