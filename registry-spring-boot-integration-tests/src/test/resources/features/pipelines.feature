@Pipelines
Feature: pipelines functionality

  Scenario: create pipeline process
    Given dataset "Test Dataset Registry" with key "d82273f6-9738-48a5-a639-2086f9c49d18"
    When create pipeline process using admin "registry_admin" with params
      | datasetKey                           | attempt |
      | d82273f6-9738-48a5-a639-2086f9c49d18 | 1       |
    Then response status should be 201
    When get pipeline process by datasetKey "d82273f6-9738-48a5-a639-2086f9c49d18" and attempt 1
    Then response status should be 200
    And pipeline process is
      | datasetKey | d82273f6-9738-48a5-a639-2086f9c49d18 |
      | attempt    | 1                                    |
      | createdBy  | registry_admin                       |
