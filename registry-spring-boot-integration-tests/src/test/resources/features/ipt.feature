@IPT
Feature: IPT related functionality

  Scenario: Register IPT installation
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    When register new installation for organization "Org"
    Then response status should be 201
    And installation UUID is returned
    And registered installation is valid

  @UpdateIpt
  Scenario: Update IPT installation
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And installation "Test IPT Registry2" with key "2fe63cec-9b23-4974-bab1-9f4118ef7711"
    When update installation "Test IPT Registry2"
    Then response status should be 204
    And updated installation is valid
#    And some installation information was updated
#    But some installation information was not updated
    And total number of installation is 1
    And total number of contacts is 1
    And total number of endpoints is 1
    When update installation "Test IPT Registry2"
#    Then response status should be 204
#    And updated installation is valid
#    And total number of installation is 1
#    And total number of contacts is 1
#    And total number of endpoints is 1
#    And contactKey is the same
#    But endpointKey was updated

