@IPT
Feature: IPT related functionality

  Scenario: Register IPT installation
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And new installation to create
    When register new installation for organization "Org" using organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 201
    And installation UUID is returned
    And registered installation is valid

  @UpdateIpt
  Scenario: Update IPT installation
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And installation "Test IPT Registry2" with key "2fe63cec-9b23-4974-bab1-9f4118ef7711"
    And installation to update
    When update installation "Test IPT Registry2" using installation key "2fe63cec-9b23-4974-bab1-9f4118ef7711" and password "welcome"
    Then response status should be 204
    And updated installation is valid
    And total number of installation is 1
    And total number of contacts is 1
    And total number of endpoints is 1
    Given store contactKey and endpointKey
    When update installation "Test IPT Registry2" using installation key "2fe63cec-9b23-4974-bab1-9f4118ef7711" and password "welcome"
    Then response status should be 204
    And updated installation is valid
    And total number of installation is 1
    And total number of contacts is 1
    And total number of endpoints is 1
    And contactKey is the same
    But endpointKey was updated

  Scenario: Register IPT installation by invalid random organisation key fails
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And new installation to create
    When register new installation for organization "Org" using organization key "73401488-ac6f-4d5e-b766-50e11d006eeb" and password "welcome"
    Then response status should be 401

  Scenario: Update IPT installation by invalid random installation key fails
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And installation "Test IPT Registry2" with key "2fe63cec-9b23-4974-bab1-9f4118ef7711"
    And installation to update
    When update installation "Test IPT Registry2" using installation key "73401488-ac6f-4d5e-b766-50e11d006eeb" and password "welcome"
    Then response status should be 401

  Scenario: Register IPT installation without primary contact
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And new installation to create
    But without field "primaryContactEmail"
    When register new installation for organization "Org" using organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 400

  Scenario: Update IPT installation without primary contact
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And installation "Test IPT Registry2" with key "2fe63cec-9b23-4974-bab1-9f4118ef7711"
    And installation to update
    But without field "primaryContactEmail"
    When update installation "Test IPT Registry2" using installation key "2fe63cec-9b23-4974-bab1-9f4118ef7711" and password "welcome"
    Then response status should be 400
