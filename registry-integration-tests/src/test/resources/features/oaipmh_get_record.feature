@OaipmhGetRecord
Feature: Test GetRecord verb of the OAI-PMH endpoint

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
    Given Request parameters
      | verb           | GetRecord                      |
      | identifier     | non-existent-record-identifier |
      | metadataPrefix | eml                            |
    When Get record
    Then response status is 200
    And request parameters in response are correct
    And error code is "idDoesNotExist"


  Scenario: Get record with unsupported metadata format causes "cannotDisseminateFormat" error
    Given Request parameters
      | verb           | GetRecord                      |
      | identifier     | non-existent-record-identifier |
      | metadataPrefix | made-up-metadata-format        |
    When Get record
    Then response status is 200
    And request parameters in response are correct
    And error code is "cannotDisseminateFormat"


  Scenario: Get record with augmented data
    Given Request parameters
      | verb           | GetRecord                            |
      | identifier     | b951d9f4-57f8-4cd8-b7cf-6b44f325d318 |
      | metadataPrefix | eml                                  |
    When Get record
    Then response status is 200
    And request parameters in response are correct
    And no error in response
    And response contains processed citation "The BGBM (2010). Pontaurus needs more than 255 characters for it's title. It is a very, very, very, very long title in the German language. Word by word and character by character it's exact title is: \"Vegetationskundliche Untersuchungen in der Hochgebirgsregion der Bolkar Daglari & Aladaglari, Türkei\". Checklist dataset https://doi.org/10.21373/gbif.2014.xsd123 accessed via GBIF.org on %s."
