@OccurrenceDownloadUsage
Feature: Occurrence download usage functionality

  Background:
    Given 1 occurrence download
      | key                                  | format | createdBy     | status    |
      | ba40b279-7fef-43ab-a0c7-95d4ae2ffaf5 | DWCA   | registry_user | PREPARING |
    And 2 datasets
      | key                                  |
      | d82273f6-9738-48a5-a639-2086f9c49d18 |
      | 4348adaa-d744-4241-92a0-ebf9d55eb9bb |

  Scenario: create occurrence download usage and list for download and dataset
    When create occurrence download usage for download "ba40b279-7fef-43ab-a0c7-95d4ae2ffaf5" using admin "registry_admin" with citations
      | d82273f6-9738-48a5-a639-2086f9c49d18 | 10 |
    Then response status should be 201
    When create occurrence download usage for download "ba40b279-7fef-43ab-a0c7-95d4ae2ffaf5" using admin "registry_admin" with citations
      | 4348adaa-d744-4241-92a0-ebf9d55eb9bb | 20 |
    Then response status should be 201

    # list by download
    When list dataset usages for download "ba40b279-7fef-43ab-a0c7-95d4ae2ffaf5" using user "registry_user"
    Then response status should be 200
    And occurrence downloads usage list contains 2 elements

    # list by dataset
    When list dataset usages by dataset "d82273f6-9738-48a5-a639-2086f9c49d18" using user "registry_user"
    Then response status should be 200
    And occurrence downloads usage list of 1 elements is
      | downloadKey                          | datasetKey                           | datasetTitle          | datasetCitation | numberRecords | datasetDOI   |
      | ba40b279-7fef-43ab-a0c7-95d4ae2ffaf5 | d82273f6-9738-48a5-a639-2086f9c49d18 | Test Dataset Registry | Citation stuff  | 10            | 10.21373/abc |

  Scenario: create occurrence download usage using user which does not have admin rights should be Forbidden 403
    When create occurrence download usage for download "ba40b279-7fef-43ab-a0c7-95d4ae2ffaf5" using user "registry_user" with citations
      | d82273f6-9738-48a5-a639-2086f9c49d18 | 10 |
    Then response status should be 403

  Scenario: list occurrence download usages for download which does not exist should be Not Found 404
    When list dataset usages for download "blablabl-blab-43ab-a0c7-95d4ae2ffaf5" using user "registry_user"
    Then response status should be 404
