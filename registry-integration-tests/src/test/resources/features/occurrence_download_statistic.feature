@DownloadUserStatistic
Feature: Occurrence download statistics functionality

  Background:
    Given 2 datasets
      | key                                  |
      | d82273f6-9738-48a5-a639-2086f9c49d18 |
      | 4348adaa-d744-4241-92a0-ebf9d55eb9bb |
    And 5 download statistic records
      | yearMonth                  | country | datasetKey                           | totalRecords | numberRecords |
      | 2017-01-16 23:06:18.993000 | DK      | d82273f6-9738-48a5-a639-2086f9c49d18 | 10           | 10            |
      | 2018-05-16 23:06:18.993000 | DK      | d82273f6-9738-48a5-a639-2086f9c49d18 | 20           | 20            |
      | 2018-06-16 23:07:58.624000 | DK      | d82273f6-9738-48a5-a639-2086f9c49d18 | 21           | 21            |
      | 2019-12-16 23:08:18.943000 | DK      | d82273f6-9738-48a5-a639-2086f9c49d18 | 30           | 30            |
      | 2019-11-16 23:08:41.188000 | NO      | 4348adaa-d744-4241-92a0-ebf9d55eb9bb | 10           | 10            |
    And 5 download user statistic records
      | yearMonth                  | country | totalRecords | numberDownloads |
      | 2017-01-16 23:06:18.993000 | DK      | 10           | 10              |
      | 2018-05-16 23:07:58.624000 | DK      | 20           | 20              |
      | 2018-06-17 23:07:58.624000 | DK      | 21           | 21              |
      | 2019-12-16 23:08:18.943000 | DK      | 30           | 30              |
      | 2019-11-16 23:08:41.188000 | NO      | 10           | 10              |

  Scenario: get download statistic by user country
    When get download statistic using user "registry_user" with params
      | userCountry | DK   |
      | fromDate    | 2017 |
      | toDate      | 2018 |
    Then response status should be 200
    And response contains 3 records for 2 years
      | year.month | value |
      | 2017.1     | 10    |
      | 2018.5     | 20    |
      | 2018.6     | 21    |

  Scenario: get download statistic by user country with no parameters
    When get download statistic using user "registry_user" without params
    Then response status should be 200
    And response contains 5 records for 3 years
      | year.month | value |
      | 2017.1     | 10    |
      | 2018.5     | 20    |
      | 2018.6     | 21    |
      | 2019.11    | 10    |
      | 2019.12    | 30    |

  Scenario: get downloaded records by dataset
    When get downloaded records by dataset using user "registry_user" with params
      | publishingCountry | DK                                   |
      | fromDate          | 2017                                 |
      | toDate            | 2018                                 |
      | datasetKey        | d82273f6-9738-48a5-a639-2086f9c49d18 |
    Then response status should be 200
    And response contains 3 records for 2 years
      | year.month | value |
      | 2017.1     | 10    |
      | 2018.5     | 20    |
      | 2018.6     | 21    |

  Scenario: get downloaded records by dataset with no parameters
    When get downloaded records by dataset using user "registry_user" without params
    Then response status should be 200
    And response contains 5 records for 3 years
      | year.month | value |
      | 2017.1     | 10    |
      | 2018.5     | 20    |
      | 2018.6     | 21    |
      | 2019.11    | 10    |
      | 2019.12    | 30    |
