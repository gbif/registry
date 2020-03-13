@Node
Feature: Node functionality

  @NodeCRUD
  Scenario: CRUD node
    When create new node "New org A"
    Then response status should be 201
    And node key is present in response
    When get node by key
    Then response status should be 200

    When update node "New org A"
    Then response status should be 200
    When get node by key
    Then response status should be 200

    When delete node "New org A" by key
    Then response status should be 200
    When get node by key
    Then response status should be 200
