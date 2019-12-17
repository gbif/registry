@DownloadUserStatistic
Feature: Occurrence download statistics functionality

  Scenario: get download statistic by user country
    When get download statistic using user "registry_user" with params
      | userCountry | DK   |
      | fromDate    | 2017 |
      | toDate      | 2018 |
    Then response status should be 200
    And response contains statistics for 2 years
      | year.month | value |
      | 2017.1     | 10    |
      | 2018.5     | 20    |
      | 2018.6     | 21    |

  Scenario: get download statistic by user country with no parameters
    When get download statistic using user "registry_user" without params
    Then response status should be 200
    And response contains statistics for 3 years
      | year.month | value |
      | 2017.1     | 10    |
      | 2018.5     | 20    |
      | 2018.6     | 21    |
      | 2019.12    | 10    |

  Scenario: get downloaded records by dataset
    When get downloaded records by dataset using user "registry_user" with params
      | publishingCountry | DK                                   |
      | fromDate          | 2017                                 |
      | toDate            | 2018                                 |
      | datasetKey        | d82273f6-9738-48a5-a639-2086f9c49d18 |
    Then response status should be 200
    And response contains statistics for 2 years
      | year.month | value |
      | 2017.1     | 10    |
      | 2018.5     | 20    |
      | 2018.6     | 21    |

  Scenario: get downloaded records by dataset with no parameters
    When get downloaded records by dataset using user "registry_user" without params
    Then response status should be 200
    And response contains statistics for 3 years
      | year.month | value |
      | 2017.1     | 10    |
      | 2018.5     | 20    |
      | 2018.6     | 21    |
      | 2019.12    | 30    |
