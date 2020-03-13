@Oaipmh
@OaipmhListMetadataFormats
Feature: Test the ListMetadataFormats verb of the OAI-PMH endpoint

  Scenario: List metadata formats with non existent record identifier causes "idDoesNotExist" error
    When Perform OAI-PMH call with parameters
      | verb       | ListMetadataFormats            |
      | identifier | non-existent-record-identifier |
    Then response status is 200
    And request parameters in response are correct
      | verb       | ListMetadataFormats            |
      | identifier | non-existent-record-identifier |
    And error code is "idDoesNotExist"

  Scenario: List metadata formats
    When Perform OAI-PMH call with parameters
      | verb | ListMetadataFormats |
    Then response status is 200
    And request parameters in response are correct
      | verb | ListMetadataFormats |
    And metadata formats are
      | oai_dc |
      | eml    |
