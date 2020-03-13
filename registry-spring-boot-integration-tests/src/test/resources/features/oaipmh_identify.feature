@Oaipmh
@OaipmhIdentify
Feature: Test the Identify verb of the OAI-PMH endpoint

  Scenario: Identify
    When Perform OAI-PMH call with parameters
      | verb | Identify |
    Then response status is 200
    And request parameters in response are correct
      | verb | Identify |
    And Identify response contains
      | repositoryName | GBIF Test Registry              |
      | baseURL        | ${api.url}oai-pmh/registry |
      | deletedRecord  | persistent                 |
