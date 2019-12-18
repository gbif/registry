@Enumeration
Feature: Enumeration functionality

  Scenario: get enumeration basic
    When get enumeration basic "Language"
    Then response status should be 200
