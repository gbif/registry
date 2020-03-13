@Dataset
Feature: Dataset functionality

  @DatasetCRUD
  Scenario: CRUD dataset
    When create new dataset "New org A" for installation "Test IPT Registry2" and organization "Org"
    Then response status should be 201
    And dataset key is present in response
    When get dataset by key
    Then response status should be 200

    When update dataset "New org A"
    Then response status should be 200
    When get dataset by key
    Then response status should be 200

    When delete dataset "New org A" by key
    Then response status should be 200
    When get dataset by key
    Then response status should be 200

  @CreateDatasetByEditor
  Scenario: Editor can create dataset if it has rights
    Given user "registry_editor" with editor rights on organization "36107c15-771c-4810-a298-b7558828b8bd"
    When create new dataset "New org A" for installation "Test IPT Registry2" and organization "Org" by editor "registry_editor" and password "welcome"
    Then response status should be 201
