@OrganizationPositive
Feature: Check Organization Resource positive cases.

  Background:
    Given node 'UK Node' and node 'UK Node 2'
    And seven organizations in 'UK Node'
#    TODO add organization explicitly with fields to background?

  # TODO convert to tables
  Scenario: Test organization suggest
    When call suggest organizations with query "The"
    Then response status should be 200
    And 7 organization(s) should be suggested
    When call suggest organizations with query "ORG"
    Then response status should be 200
    And 1 organization(s) should be suggested
    When call suggest organizations with query "Stuff"
    Then response status should be 200
    And 0 organization(s) should be suggested

  Scenario: Test list organizations by country
    When call list organizations by country "ANGOLA"
    Then response status should be 200
    And 2 organization(s) should be listed
    When call list organizations by country "ARMENIA"
    Then response status should be 200
    And 0 organization(s) should be listed

    # TODO perform full CRUD flow?
  Scenario: Test create an organization
    Given new not created organization
    When try to create that organization
    Then response status should be 201
    When get organization by id
    Then response status should be 200
