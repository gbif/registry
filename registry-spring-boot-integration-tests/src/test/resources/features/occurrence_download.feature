@OccurrenceDownload
Feature: Occurrence Download functionality

  Scenario: create and get instance download
    Given instance predicate download
    When create download
    Then response status should be 201
    When get download
    Then response status should be 200
    And download assertions passed
