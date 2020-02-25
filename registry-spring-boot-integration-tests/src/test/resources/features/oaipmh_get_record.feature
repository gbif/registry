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
      | a49e75d9-7b07-4d01-9be8-6ab2133f42f9 | EUROPE      | EUROPE    | The UK National Node | GB      | WS TEST    | WS TEST     | 2020-02-22 09:54:09.835039 | 2020-02-22 09:54:09.835039 | null    | 'countri':5 'europ':7,8 'gb':9 'nation':3 'node':4 'uk':2 'vote':6 | COUNTRY | VOTING               |

  Scenario: Get record with non existent record identifier causes IdDoesNotExistException
    Given Get record parameters
      | identifier                     | metadataFormatPrefix |
      | non-existent-record-identifier | eml                  |
    When Get record
    Then IdDoesNotExistException is expected

  Scenario: Get record with unsupported metadata format causes CannotDisseminateFormatException
    Given Get record parameters
      | identifier                     | metadataFormatPrefix    |
      | non-existent-record-identifier | made-up-metadata-format |
    When Get record
    Then CannotDisseminateFormatException is expected
