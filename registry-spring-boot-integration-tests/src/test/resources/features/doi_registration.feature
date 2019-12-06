@DoiRegistration
Feature: DOI registration functionality

  Scenario: generate DOI
    When generate new DOI of type "DATASET"
    Then response status should be 201
    And DOI is returned
