@Organization
Feature: Organization functionality

  Background:
    # see scripts/organization
    Given node "UK Node" with key "f698c938-d36a-41ac-8120-c35903e1acb9"
    And node "UK Node 2" with key "9996f2f2-f71c-4f40-8e69-031917b314e0"
    And seven organizations for node "UK Node"

  @OrganizationCRUD
  Scenario: CRUD organization
    When create new organization "New org A" for node "UK Node"
    Then response status should be 201
    And organization key is present in response
    When get organization by key
    Then response status should be 200

    When update organization "New org A" with new title "New Title"
    Then response status should be 200
    When get organization by key
    Then response status should be 200

    When delete organization "New org A" by key
    Then response status should be 200
    When get organization by key
    Then response status should be 200

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

  @OrganizationEndorsement
  Scenario: Organization endorsement
    Given 0 organization(s) endorsed for "UK Node 2"
    And 7 organization(s) pending endorsement in total
    When create new organization "New org B" for node "UK Node 2"
    Then organization key is present in response
    And 0 organization(s) endorsed for "UK Node 2"
    And 1 organization(s) pending endorsement for "UK Node 2"
    And 8 organization(s) pending endorsement in total
    When endorse organization "New org B"
    Then 1 organization(s) endorsed for "UK Node 2"
    And 0 organization(s) pending endorsement for "UK Node 2"
    And 7 organization(s) pending endorsement in total

  @OrganizationValidation
  Scenario: Organization create and update exception cases
    When create new organization for "UK Node" with key
    Then response status should be 422
    When create new organization "New org A" for node "UK Node"
    Then response status should be 201
    And organization key is present in response
    When update organization with new invalid too short title "A" for node "UK Node"
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

  @CreateOrganizationByEditor
  Scenario: Editor can create organization if it has rights
    Given user "registry_editor" with editor rights on node "f698c938-d36a-41ac-8120-c35903e1acb9"
    When create new organization "New org A" for node "UK Node" by editor "registry_editor" and password "welcome"
    Then response status should be 201
