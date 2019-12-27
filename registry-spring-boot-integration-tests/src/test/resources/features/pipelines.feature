@Pipelines
Feature: pipelines functionality

  Background:
    Given datasets
      | key                                  | name                    |
      | d82273f6-9738-48a5-a639-2086f9c49d18 | Test Dataset Registry   |
      | 4348adaa-d744-4241-92a0-ebf9d55eb9bb | Test Dataset Registry 2 |
    And pipeline processes
      | key | datasetKey                           | attempt |
      | 1   | d82273f6-9738-48a5-a639-2086f9c49d18 | 1       |
      | 2   | d82273f6-9738-48a5-a639-2086f9c49d18 | 2       |
      | 3   | d82273f6-9738-48a5-a639-2086f9c49d18 | 3       |
    And pipeline execution
      | key | processKey |
      | 11  | 1          |
      | 12  | 2          |
    And pipeline step
      | key | executionKey | state     | type             |
      | 101 | 11           | RUNNING   | ABCD_TO_VERBATIM |
      | 102 | 12           | COMPLETED | DWCA_TO_VERBATIM |

  Scenario: create and get pipeline process
    When create pipeline process using admin "registry_admin" with params
      | datasetKey                           | attempt |
      | 4348adaa-d744-4241-92a0-ebf9d55eb9bb | 1       |
    Then response status should be 201
    When get pipeline process by datasetKey "4348adaa-d744-4241-92a0-ebf9d55eb9bb" and attempt 1
    Then response status should be 200
    And pipeline process is
      | datasetKey | 4348adaa-d744-4241-92a0-ebf9d55eb9bb |
      | attempt    | 1                                    |
      | createdBy  | registry_admin                       |

  Scenario: create pipeline process without privileges will cause Forbidden 403
    When create pipeline process using user "registry_user" with params
      | datasetKey                           | attempt |
      | d82273f6-9738-48a5-a639-2086f9c49d18 | 1       |
    Then response status should be 403

  Scenario: get pipeline process which does not exist
    When get pipeline process by datasetKey "aaaaaaaa-9738-48a5-a639-2086f9c49d18" and attempt 1
    Then response status should be 200
    And pipeline process is empty

  Scenario: history pipeline process
    When history pipeline process
    Then response status should be 200
    And pipeline process history contains 3 entity
    When history pipeline process by datasetKey "d82273f6-9738-48a5-a639-2086f9c49d18"
    Then response status should be 200
    And pipeline process history contains 3 entity
    When history pipeline process by datasetKey "4348adaa-d744-4241-92a0-ebf9d55eb9bb"
    Then response status should be 200
    And pipeline process history contains 0 entity

  Scenario: add and get pipeline step
    When add pipeline execution for process 1 using admin "registry_admin"
      | stepsToRun       | rerunReason | remarks |
      | DWCA_TO_VERBATIM | rerun       | remarks |
    Then response status should be 201
    And extract executionKey
    When add pipeline step for process 1 and current execution using admin "registry_admin"
      | message | runner     | type             | state   |
      | message | STANDALONE | ABCD_TO_VERBATIM | RUNNING |
    Then response status should be 201
    And extract stepKey
    When get pipeline step by stepKey for process 1 and current execution
    Then response status should be 200
    And pipeline step is
      | type      | ABCD_TO_VERBATIM |
      | runner    | STANDALONE       |
      | state     | RUNNING          |
      | message   | message          |
      | createdBy | registry_admin   |

  Scenario: add pipeline execution without privileges will cause Forbidden 403
    When add pipeline execution for process 1 using user "registry_user"
      | stepsToRun       | rerunReason | remarks |
      | DWCA_TO_VERBATIM | rerun       | remarks |
    Then response status should be 403

  Scenario: add pipeline step without privileges will cause Forbidden 403
    Given pipeline execution with key 11
    When add pipeline step for process 1 and current execution using user "registry_user"
      | message | runner     | type             | state   |
      | message | STANDALONE | ABCD_TO_VERBATIM | RUNNING |
    Then response status should be 403

  Scenario: update pipeline step status and metrics
    Given pipeline process with key 1
    And pipeline execution with key 11
    And pipeline step with key 101
    When update pipeline step status and metrics using admin "registry_admin"
      | status    | metrics         |
      | COMPLETED | metricName=>100 |
    Then response status should be 200
    When get pipeline step by stepKey for process 1 and current execution
    Then response status should be 200
    And pipeline step is
      | type             | ABCD_TO_VERBATIM |
      | runner           | STANDALONE       |
      | message          | message          |
      | createdBy        | WS TEST          |
      | metrics[0].name  | metricName       |
      | metrics[0].value | 100              |
      | state            | COMPLETED        |
      | modifiedBy       | registry_admin   |
    And finished and modified dates are present

  Scenario: run pipeline attempt
    Given pipeline process with key 2
    And pipeline execution with key 12
    And pipeline step with key 102
    When run pipeline attempt for dataset with key "d82273f6-9738-48a5-a639-2086f9c49d18" and attempt 2 using admin "registry_admin" with params
      | steps  | DWCA_TO_VERBATIM |
      | reason | test reason      |
    Then response status should be 201
    And run pipeline response is
      | responseStatus | OK |
    When get pipeline process by datasetKey "d82273f6-9738-48a5-a639-2086f9c49d18" and attempt 2
    Then response status should be 200
    And pipeline process is
      | datasetKey                  | d82273f6-9738-48a5-a639-2086f9c49d18 |
      | attempt                     | 2                                    |
      | createdBy                   | WS TEST                              |
      | executions[0].rerunReason   | test reason                          |
      | executions[0].stepsToRun[0] | DWCA_TO_VERBATIM                     |
      | executions[0].createdBy     | registry_admin                       |
