@Installation
Feature: Installation functionality

  @InstallationCRUD
  Scenario: CRUD installation
    When create new installation "New org A" for organization "Org"
    Then response status should be 201
    And installation key is present in response
    When get installation by key
    Then response status should be 200

    When update installation "New org A"
    Then response status should be 200
    When get installation by key
    Then response status should be 200

    When delete installation "New org A" by key
    Then response status should be 200
    When get installation by key
    Then response status should be 200

  @CreateInstallationByEditor
  Scenario: Editor can create installation if it has rights
    Given user "registry_editor" with editor rights on organization "36107c15-771c-4810-a298-b7558828b8bd"
    When create new installation "New org A" for organization "Org" by editor "registry_editor" and password "welcome"
    Then response status should be 201
