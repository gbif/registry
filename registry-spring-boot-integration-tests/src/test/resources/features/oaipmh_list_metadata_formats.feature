@Oaipmh
@OaipmhListMetadataFormats
Feature: Test ListMetadataFormats verb of the OAI-PMH endpoint

  Scenario: List metadata formats with non existent record identifier causes "idDoesNotExist" error
    When Get record by parameters
      | verb       | ListMetadataFormats            |
      | identifier | non-existent-record-identifier |
    Then response status is 200
    And request parameters in response are correct
      | verb       | ListMetadataFormats            |
      | identifier | non-existent-record-identifier |
    And error code is "idDoesNotExist"

  Scenario: List metadata formats
    When Get record by parameters
      | verb | ListMetadataFormats |
    Then response status is 200
    And request parameters in response are correct
      | verb | ListMetadataFormats |
    And metadata formats are
      | oai_dc |
      | eml    |
