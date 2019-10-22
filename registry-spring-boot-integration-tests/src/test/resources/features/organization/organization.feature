@Organization
Feature: Organization functionality

  Background:
    # see scripts/organization
    Given node 'UK Node' and node 'UK Node 2'
    And seven organizations in 'UK Node'

  Scenario Outline: Organization suggest
    When call suggest organizations with query "<query>"
    Then response status should be 200
    And <number> organization(s) should be suggested

    Scenarios:
      | query | number |
      | The   | 7      |
      | ORG   | 1      |
      | Stuff | 0      |

  Scenario Outline: List organizations by country
    When call list organizations by country <country>
    Then response status should be 200
    And <number> organization(s) should be listed

    Scenarios:
      | country | number |
      | ANGOLA  | 2      |
      | ARMENIA | 0      |
      | DENMARK | 1      |
      | GERMANY | 1      |
      | FRANCE  | 2      |

    #todo assert organization was properly created (fields)
  @OrganizationCreateUpdate
  Scenario: Create an organization
    When create a new organization "New org A" for node "UK Node"
    Then response status should be 201
    When get organization by key
    Then response status should be 200
    When update organization "New org A" with new title "New Title" for node "UK Node"
    Then response status should be 200
    When get organization by key
    Then response status should be 200
    And title is new "New Title"
    And modification date was not updated
    And modification date is after the creation date

  @OrganizationEndorsement
  Scenario: Organization endorsement
    Given 0 organization(s) endorsed for "UK Node 2"
    And 7 organization(s) pending endorsement in total
    When create a new organization "New org B" for node "UK Node 2"
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

  @OrganizationTitles
  Scenario: Organization titles
    When get titles for empty list
    Then response status should be 201
    And empty titles map is returned
    When get titles for organizations
      | f433944a-ad93-4ea8-bad7-68de7348e65a |
      | 180bc881-9c8f-445b-89d9-40cd099cbdc3 |
      | e47e4958-7dee-475b-98c7-07a2d7de8f96 |
    Then response status should be 201
    And titles map is returned
      | f433944a-ad93-4ea8-bad7-68de7348e65a | The ORG  |
      | 180bc881-9c8f-445b-89d9-40cd099cbdc3 | The BGBM |
      | e47e4958-7dee-475b-98c7-07a2d7de8f96 | The BGBM |
