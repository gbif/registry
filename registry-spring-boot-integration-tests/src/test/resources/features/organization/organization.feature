@OrganizationPositive
Feature: Organization functionality

  Background:
    Given node 'UK Node' and node 'UK Node 2'
    And seven organizations in 'UK Node'
#    TODO add organization explicitly with fields to background?

  # TODO convert to tables
  Scenario: Organization suggest
    When call suggest organizations with query "The"
    Then response status should be 200
    And 7 organization(s) should be suggested
    When call suggest organizations with query "ORG"
    Then response status should be 200
    And 1 organization(s) should be suggested
    When call suggest organizations with query "Stuff"
    Then response status should be 200
    And 0 organization(s) should be suggested

  Scenario: List organizations by country
    When call list organizations by country "ANGOLA"
    Then response status should be 200
    And 2 organization(s) should be listed
    When call list organizations by country "ARMENIA"
    Then response status should be 200
    And 0 organization(s) should be listed

    # TODO perform full CRUD flow?
  Scenario: Create an organization
    When create a new organization "New org A" for "UK Node"
    Then response status should be 201
    When get organization by id
    Then response status should be 200

  @OrganizationEndorsement
  Scenario: Organization endorsement
    Given 0 organization(s) endorsed for "UK Node 2"
    And 7 organization(s) pending endorsement in total
    When create a new organization "New org B" for "UK Node 2"
    Then 0 organization(s) endorsed for "UK Node 2"
    And 1 organization(s) pending endorsement for "UK Node 2"
    And 8 organization(s) pending endorsement in total
    When endorse organization "New org B"
    Then 1 organization(s) endorsed for "UK Node 2"
    And 0 organization(s) pending endorsement for "UK Node 2"
    And 7 organization(s) pending endorsement in total

  @CreateOrganizationWithKey
  Scenario: Organization can't be created with key present
    When create a new organization for "UK Node" with key
    Then response status should be 422
