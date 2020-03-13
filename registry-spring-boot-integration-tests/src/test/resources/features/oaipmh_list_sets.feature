@Oaipmh
@OaipmhListSets
Feature: Test the ListSets verb of the OAI-PMH endpoint

  Scenario: ListSets verb when no data in database returns only 7 static Sets when database is empty: dataset_type, dataset_type:OCCURRENCE, dataset_type:CHECKLIST, dataset_type:METADATA, dataset_type:SAMPLING_EVENT, country, installation
    When Perform OAI-PMH call with parameters
      | verb | ListSets |
    Then response status is 200
    And request parameters in response are correct
      | verb | ListSets |
    And ListSets response contains 7 records

  Scenario: ListSets verb when one dataset in DB, returns 9 sets in total: 7 static ones, and two dataset specific, installation:1e9136f0-78fd-40cd-8b25-26c78a376d8d and country:GB
    Given one dataset "b951d9f4-57f8-4cd8-b7cf-6b44f325d318"
    When Perform OAI-PMH call with parameters
      | verb | ListSets |
    Then response status is 200
    And request parameters in response are correct
      | verb | ListSets |
    And ListSets response contains 9 records
