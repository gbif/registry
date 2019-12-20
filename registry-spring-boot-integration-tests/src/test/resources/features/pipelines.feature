@Pipelines
Feature: pipelines functionality

  Background:
    Given datasets
      | key                                  | name                    |
      | d82273f6-9738-48a5-a639-2086f9c49d18 | Test Dataset Registry   |
      | 4348adaa-d744-4241-92a0-ebf9d55eb9bb | Test Dataset Registry 2 |
    And pipeline processes
      | key | datasetKey                           | attempt |
      | 20  | d82273f6-9738-48a5-a639-2086f9c49d18 | 1       |
      | 21  | d82273f6-9738-48a5-a639-2086f9c49d18 | 2       |
      | 22  | d82273f6-9738-48a5-a639-2086f9c49d18 | 3       |

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
    When add pipeline execution for process 20 using admin "registry_admin"
      | stepsToRun       | rerunReason | remarks |
      | DWCA_TO_VERBATIM | rerun       | remarks |
    Then response status should be 201
    And extract executionKey
    When add pipeline step for process 20 and current execution using admin "registry_admin"
      | message | runner     | type             | state   |
      | message | STANDALONE | ABCD_TO_VERBATIM | RUNNING |
    Then response status should be 201
    And extract stepKey
    When get pipeline step fro process 20 and current execution
    Then response status should be 200
    And pipeline step is
      | type      | ABCD_TO_VERBATIM |
      | runner    | STANDALONE       |
      | state     | RUNNING          |
      | message   | message          |
      | createdBy | registry_admin   |
