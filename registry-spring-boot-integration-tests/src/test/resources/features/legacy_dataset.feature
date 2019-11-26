@LegacyDataset
Feature: LegacyDatasetResource functionality

  Background:
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And installation "Test IPT Registry2" with key "2fe63cec-9b23-4974-bab1-9f4118ef7711"
    And dataset "Occurrence Dataset 1" with key "d82273f6-9738-48a5-a639-2086f9c49d18"
    And dataset contact "gbifregistry@mailinator.com" with key 1188
    And dataset endpoint "http://www.example.org" with key 2055
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

