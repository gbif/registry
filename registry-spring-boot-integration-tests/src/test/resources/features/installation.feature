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
