@Oaipmh
@OaipmhGetRecord
Feature: Test the GetRecord verb of the OAI-PMH endpoint

  Background:
    Given node
      | key                                  | title                |
      | a49e75d9-7b07-4d01-9be8-6ab2133f42f9 | The UK National Node |
    And organization
      | key                                  | nodeKey                              | title    |
      | ff593857-44c2-4011-be20-8403e8d0bd9a | a49e75d9-7b07-4d01-9be8-6ab2133f42f9 | The BGBM |
    And installation
      | key                                  | orgKey                               | title                         |
      | 1e9136f0-78fd-40cd-8b25-26c78a376d8d | ff593857-44c2-4011-be20-8403e8d0bd9a | The BGBM BIOCASE INSTALLATION |
    And dataset
      | key                                  | gbif_region | continent | title                | country | created_by | modified_by | created                    | modified                   | deleted | fulltext_search                                                    | type    | participation_status |
      | b951d9f4-57f8-4cd8-b7cf-6b44f325d318 | EUROPE      | EUROPE    | The UK National Node | GB      | WS TEST    | WS TEST     | 2020-02-22 09:54:09.835039 | 2020-02-22 09:54:09.835039 | null    | 'countri':5 'europ':7,8 'gb':9 'nation':3 'node':4 'uk':2 'vote':6 | COUNTRY | VOTING               |
    And metadata
      | key | datasetKey                           |
      | -1  | b951d9f4-57f8-4cd8-b7cf-6b44f325d318 |


  Scenario: Get record with non existent record identifier causes "idDoesNotExist" error
    When Perform OAI-PMH call with parameters
      | verb           | GetRecord                      |
      | identifier     | non-existent-record-identifier |
      | metadataPrefix | eml                            |
    Then response status is 200
    And request parameters in response are correct
      | verb           | GetRecord                      |
      | identifier     | non-existent-record-identifier |
      | metadataPrefix | eml                            |
    And error code is "idDoesNotExist"


  Scenario: Get record with unsupported metadata format causes "cannotDisseminateFormat" error
    When Perform OAI-PMH call with parameters
      | verb           | GetRecord                      |
      | identifier     | non-existent-record-identifier |
      | metadataPrefix | made-up-metadata-format        |
    Then response status is 200
    And request parameters in response are correct
      | verb           | GetRecord                      |
      | identifier     | non-existent-record-identifier |
      | metadataPrefix | made-up-metadata-format        |
    And error code is "cannotDisseminateFormat"


  Scenario Outline: Get record with unsupported date format in <paramName> causes "badArgument" error
    When Perform OAI-PMH call with parameters
      | verb           | GetRecord |
      | metadataPrefix | eml       |
      | <paramName>    | 111       |
    Then response status is 200
    And request parameters in response are correct
      | verb           | GetRecord |
      | metadataPrefix | eml       |
    And error code is "badArgument"

    Scenarios:
      | paramName |
      | from      |
      | until     |


  Scenario: Get record with augmented data
    When Perform OAI-PMH call with parameters
      | verb           | GetRecord                            |
      | identifier     | b951d9f4-57f8-4cd8-b7cf-6b44f325d318 |
      | metadataPrefix | eml                                  |
    Then response status is 200
    And request parameters in response are correct
      | verb           | GetRecord                            |
      | identifier     | b951d9f4-57f8-4cd8-b7cf-6b44f325d318 |
      | metadataPrefix | eml                                  |
    And no error in response
    And response contains processed citation "The BGBM (2010). Pontaurus needs more than 255 characters for it's title. It is a very, very, very, very long title in the German language. Word by word and character by character it's exact title is: \"Vegetationskundliche Untersuchungen in der Hochgebirgsregion der Bolkar Daglari & Aladaglari, Türkei\". Checklist dataset https://doi.org/10.21373/gbif.2014.xsd123 accessed via GBIF.org on %s."

# todo fix 403 issues with datasetService update/delete
#  Scenario: Get record deleted\restored dataset
#    When Perform OAI-PMH call with parameters
#      | verb           | GetRecord                            |
#      | identifier     | b951d9f4-57f8-4cd8-b7cf-6b44f325d318 |
#      | metadataPrefix | eml                                  |
#    Then response status is 200
#    And no record status
#    When delete dataset "b951d9f4-57f8-4cd8-b7cf-6b44f325d318"
#    And Perform OAI-PMH call with parameters
#      | verb           | GetRecord                            |
#      | identifier     | b951d9f4-57f8-4cd8-b7cf-6b44f325d318 |
#      | metadataPrefix | eml                                  |
#    Then response status is 200
#    And record status is "deleted"
#    When restore dataset "b951d9f4-57f8-4cd8-b7cf-6b44f325d318"
#    And Perform OAI-PMH call with parameters
#      | verb           | GetRecord                            |
#      | identifier     | b951d9f4-57f8-4cd8-b7cf-6b44f325d318 |
#      | metadataPrefix | eml                                  |
#    Then response status is 200
#    And no record status
