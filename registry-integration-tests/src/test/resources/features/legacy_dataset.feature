@LegacyDataset
Feature: LegacyDatasetResource functionality

  Background:
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And organization "Org 2" with key "58f3a88a-0557-4208-a504-1f67fc74764f"
    And installation "Test IPT Registry2" with key "2fe63cec-9b23-4974-bab1-9f4118ef7711" for organization "Org"
    And dataset "Test Dataset Registry" with key "d82273f6-9738-48a5-a639-2086f9c49d18" for installation "Test IPT Registry2"
    And dataset "Test Dataset Registry 2" with key "4348adaa-d744-4241-92a0-ebf9d55eb9bb" for installation "Test IPT Registry2"
    And dataset contact "gbifregistry@mailinator.com" with key 1188 for dataset "Test Dataset Registry"
    And dataset endpoint "http://www.example.org" with key 2055 for dataset "Test Dataset Registry"
    And query parameters to dataset updating
      | name                | Test Dataset Registry2               |
      | nameLanguage        | fr                                   |
      | description         | Description of Test Dataset          |
      | doi                 | http://dx.doi.org/10.1234/timbo      |
      | descriptionLanguage | es                                   |
      | homepageURL         | http://www.homepage.com              |
      | logoURL             | http://www.logo.com/1                |
      | organisationKey     | 36107c15-771c-4810-a298-b7558828b8bd |

  Scenario: Update legacy dataset
    When update dataset "Test Dataset Registry2" with key "d82273f6-9738-48a5-a639-2086f9c49d18" using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
      | name        | Updated Dataset 2             |
      | description | Description of Test Dataset 2 |
    Then response status should be 201
    And updated dataset is
      | key                                  | publishingOrganizationKey            | installationKey                      | external | type       | title             | description                   | homepage                | logoUrl               | createdBy                            | modifiedBy                           | language |
      | d82273f6-9738-48a5-a639-2086f9c49d18 | 36107c15-771c-4810-a298-b7558828b8bd | 2fe63cec-9b23-4974-bab1-9f4118ef7711 | false    | OCCURRENCE | Updated Dataset 2 | Description of Test Dataset 2 | http://www.homepage.com | http://www.logo.com/1 | 36107c15-771c-4810-a298-b7558828b8bd | 36107c15-771c-4810-a298-b7558828b8bd | ENGLISH  |
    And updated dataset contact is
      | key  | type                       | primary | firstName | lastName  | position   | description | email                       | phone        | organization | address               | city       | province | country | postalCode | createdBy | modifiedBy                           |
      | 2055 | TECHNICAL_POINT_OF_CONTACT | true    | Tim       | Robertson | Programmer | About 175cm | gbifregistry@mailinator.com | +45 28261487 | GBIF         | Universitetsparken 15 | Copenhagen | Capital  | DENMARK | 2100       | WS TEST   | 36107c15-771c-4810-a298-b7558828b8bd |

  Scenario Outline: Send a get all datasets owned by organization (GET) request with extension "<extension>", the response having at the very least the dataset key, publishing organization key, dataset title, and dataset description
    When perform get datasets for organization request with extension "<extension>" and parameter organisationKey and value "36107c15-771c-4810-a298-b7558828b8bd"
    Then response status should be 200
    And content type "<expectedContentType>" is expected
    And returned <case> datasets for organization are
      | key                                  | description                   | descriptionLanguage | homepageURL             | name                    | nameLanguage | organisationKey                      |
      | 4348adaa-d744-4241-92a0-ebf9d55eb9bb | Description of Test Dataset 2 | en                  | http://www.homepage.com | Test Dataset Registry 2 | en           | 36107c15-771c-4810-a298-b7558828b8bd |
      | d82273f6-9738-48a5-a639-2086f9c49d18 | Description of Test Dataset   | en                  | http://www.homepage.com | Test Dataset Registry   | en           | 36107c15-771c-4810-a298-b7558828b8bd |

    Scenarios:
      | case         | extension | expectedContentType |
      | JSON         | .json     | application/json    |
      | XML          | .xml      | application/xml     |
      | NO_EXTENSION |           | application/xml     |

  Scenario Outline: Send a get all datasets for organization (GET) request with extension "<extension>", organization does not exist
    When perform get datasets for organization request with extension "<extension>" and parameter organisationKey and value "f86b4bf2-f584-4c42-bc93-c6637ffa58d7"
    Then response status should be 200
    And content type "<expectedContentType>" is expected
    And datasets error <case> response is "No organisation matches the key provided"

    Scenarios:
      | case         | extension | expectedContentType |
      | JSON         | .json     | application/json    |
      | XML          | .xml      | application/xml     |
      | NO_EXTENSION |           | application/xml     |

  Scenario Outline: Send a get all datasets for organization (GET) request with extension "<extension>", organization without datasets
    When perform get datasets for organization request with extension "<extension>" and parameter organisationKey and value "58f3a88a-0557-4208-a504-1f67fc74764f"
    Then response status should be 200
    And content type "<expectedContentType>" is expected
    And datasets <case> response is empty

    Scenarios:
      | case         | extension | expectedContentType |
      | JSON         | .json     | application/json    |
      | XML          | .xml      | application/xml     |
      | NO_EXTENSION |           | application/xml     |

  Scenario: Send a get all datasets for organization (GET) request with unknown extension ".unknown", Not Found 404 is expected
    When perform get datasets for organization request with extension ".unknown" and parameter organisationKey and value "36107c15-771c-4810-a298-b7558828b8bd"
    Then response status should be 404

  Scenario Outline: Send a get dataset (GET) request with extension "<extension>", the response having all of: key, organisationKey, name, description, nameLanguage, descriptionLanguage, homepageURL, primaryContactType/Name/Email/Phone/Address/Desc
    When perform get dataset "d82273f6-9738-48a5-a639-2086f9c49d18" request with extension "<extension>"
    Then response status should be 200
    And content type "<expectedContentType>" is expected
    And dataset response for case <case> is
      | key                                  | description                 | descriptionLanguage | homepageURL             | name                  | nameLanguage | organisationKey                      | primaryContactAddress | primaryContactDescription | primaryContactEmail         | primaryContactName | primaryContactPhone | primaryContactType |
      | d82273f6-9738-48a5-a639-2086f9c49d18 | Description of Test Dataset | en                  | http://www.homepage.com | Test Dataset Registry | en           | 36107c15-771c-4810-a298-b7558828b8bd | Universitetsparken 15 | About 175cm               | gbifregistry@mailinator.com | Tim Robertson      | +45 28261487        | technical          |

    Scenarios:
      | case         | extension | expectedContentType |
      | JSON         | .json     | application/json    |
      | XML          | .xml      | application/xml     |
      | NO_EXTENSION |           | application/xml     |

  Scenario: Send a get dataset (GET) request with unknown extension ".unknown", Not Found 404 is expected
    When perform get dataset "d82273f6-9738-48a5-a639-2086f9c49d18" request with extension ".unknown"
    Then response status should be 404

  Scenario Outline: Send a get dataset (GET) request with extension "<extension>", dataset does not exist, Not Found 404 is expected
    When perform get dataset "58f3a88a-0557-4208-a504-1f67fc74764f" request with extension "<extension>"
    Then response status should be 200
    And content type "<expectedContentType>" is expected
    And dataset error <case> response is "No resource matches the key provided"

    Scenarios:
      | case         | extension | expectedContentType |
      | JSON         | .json     | application/json    |
      | XML          | .xml      | application/xml     |
      | NO_EXTENSION |           | application/xml     |
