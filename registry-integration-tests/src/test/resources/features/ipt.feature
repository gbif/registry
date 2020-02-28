@IPT
Feature: IPT related functionality

  Background:
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And installation "Test IPT Registry2" with key "2fe63cec-9b23-4974-bab1-9f4118ef7711"
    And dataset "Occurrence Dataset 1" with key "d82273f6-9738-48a5-a639-2086f9c49d18"
    And query parameters for installation registration or updating
      | organisationKey     | 36107c15-771c-4810-a298-b7558828b8bd |
      | name                | Test IPT Registry2                   |
      | description         | Description of Test IPT              |
      | primaryContactType  | technical                            |
      | primaryContactEmail | kbraak@gbif.org                      |
      | primaryContactName  | Kyle Braak                           |
      | serviceTypes        | RSS                                  |
      | serviceURLs         | http://ipt.gbif.org/rss.do           |
      | wsPassword          | welcome                              |
    And query parameters to dataset registration or updating
      | organisationKey       | 36107c15-771c-4810-a298-b7558828b8bd                                       |
      | name                  | Test Dataset Registry2                                                     |
      | description           | Description of Test Dataset                                                |
      | homepageURL           | http://www.homepage.com                                                    |
      | logoURL               | http://www.logo.com/1                                                      |
      | primaryContactType    | administrative                                                             |
      | primaryContactEmail   | elyk-kaarb@euskadi.eus                                                     |
      | primaryContactName    | Jan Legind                                                                 |
      | primaryContactAddress | Universitetsparken 15, 2100, Denmark                                       |
      | primaryContactPhone   | 90909090                                                                   |
      | serviceTypes          | EML\|DWC-ARCHIVE-OCCURRENCE                                                |
      | serviceURLs           | http://ipt.gbif.org/eml.do?r=ds123\|http://ipt.gbif.org/archive.do?r=ds123 |
      | iptKey                | 2fe63cec-9b23-4974-bab1-9f4118ef7711                                       |

  @RegisterIptInstallation
  Scenario: Register IPT installation
    When register new installation for organization "Org" using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 201
    And installation UUID is returned
    And registered installation is
      | organisationKey                      | title              | type             | description             |
      | 36107c15-771c-4810-a298-b7558828b8bd | Test IPT Registry2 | IPT_INSTALLATION | Description of Test IPT |
    And registered installation contacts are
      | type      | email           | firstName  | primary |
      | technical | kbraak@gbif.org | Kyle Braak | true    |
    And registered installation endpoints are
      | url                        | type |
      | http://ipt.gbif.org/rss.do | FEED |

  @UpdateIptInstallation
  Scenario: Update IPT installation
    When update installation "Test IPT Registry2" using valid installation key "2fe63cec-9b23-4974-bab1-9f4118ef7711" and password "welcome"
      | name        | Updated Test IPT Registry2      |
      | description | Updated Description of Test IPT |
    Then response status should be 204
    And updated installation is
      | organisationKey                      | title                      | type             | description                     |
      | 36107c15-771c-4810-a298-b7558828b8bd | Updated Test IPT Registry2 | IPT_INSTALLATION | Updated Description of Test IPT |
    And updated installation contacts are
      | type      | email           | firstName  | primary |
      | technical | kbraak@gbif.org | Kyle Braak | true    |
    And updated installation endpoints are
      | url                        | type |
      | http://ipt.gbif.org/rss.do | FEED |
    And following installation fields were not updated
      | created | createdBy |
    And total number of installations is 1
    Given store installation contactKey and endpointKey
    When update installation "Updated Test IPT Registry2" using valid installation key "2fe63cec-9b23-4974-bab1-9f4118ef7711" and password "welcome"
      | name        | Test IPT Registry2      |
      | description | Description of Test IPT |
    Then response status should be 204
    And updated installation is
      | organisationKey                      | title              | type             | description             |
      | 36107c15-771c-4810-a298-b7558828b8bd | Test IPT Registry2 | IPT_INSTALLATION | Description of Test IPT |
    And total number of installations is 1
    And installation contactKey is the same
    But installation endpointKey was updated

  Scenario: Register IPT installation by invalid random organisation key fails
    When register new installation for organization "Org" using invalid organization key "73401488-ac6f-4d5e-b766-50e11d006eeb" and password "welcome"
    Then response status should be 401

  Scenario: Update IPT installation by invalid random installation key fails
    When update installation "Test IPT Registry2" using invalid installation key "73401488-ac6f-4d5e-b766-50e11d006eeb" and password "welcome"
      | name        | Updated Test IPT Registry2      |
      | description | Updated Description of Test IPT |
    Then response status should be 401

  Scenario: Register IPT installation without primary contact fails
    Given installation parameters without field "primaryContactEmail"
    When register new installation for organization "Org" using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 400

  Scenario: Update IPT installation without primary contact fails
    Given installation parameters without field "primaryContactEmail"
    When update installation "Test IPT Registry2" using valid installation key "2fe63cec-9b23-4974-bab1-9f4118ef7711" and password "welcome"
      | name | Updated Test IPT Registry2 |
    Then response status should be 400

  @RegisterIptDataset
  Scenario Outline: Register IPT <datasetType> dataset
    Given dataset parameter serviceTypes is "<serviceType>"
    And dataset parameter name is "<name>"
    And SAMPLING_EVENT does not have parameter iptKey and infers it from organization
    When register new dataset using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 201
    And dataset UUID is returned
    And registered dataset is
      | publishingOrganizationKey            | installationKey                      | type          | title  | description                 | logoUrl               | homepage                |
      | 36107c15-771c-4810-a298-b7558828b8bd | 2fe63cec-9b23-4974-bab1-9f4118ef7711 | <datasetType> | <name> | Description of Test Dataset | http://www.logo.com/1 | http://www.homepage.com |
    And registered dataset contacts are
      | type           | email                  | firstName  | address                              | phone    | primary |
      | administrative | elyk-kaarb@euskadi.eus | Jan Legind | Universitetsparken 15, 2100, Denmark | 90909090 | true    |
    And registered dataset endpoints are
      | url                                    | type        |
      | http://ipt.gbif.org/archive.do?r=ds123 | DWC_ARCHIVE |
      | http://ipt.gbif.org/eml.do?r=ds123     | EML         |

    Scenarios:
      | serviceType                     | datasetType    | name                        |
      | EML\|DWC-ARCHIVE-OCCURRENCE     | OCCURRENCE     | Occurrence Test Dataset     |
      | EML\|DWC-ARCHIVE-SAMPLING-EVENT | SAMPLING_EVENT | Sampling Event Test Dataset |

  Scenario: Register IPT dataset by invalid random organisationKey fails
    When register new dataset using invalid organization key "73401488-ac6f-4d5e-b766-50e11d006eeb" and password "welcome"
    Then response status should be 401

  Scenario: Register IPT dataset without primary contact fails
    Given dataset parameters without field "primaryContactType"
    When register new dataset using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 400

  @UpdateIptDataset
  Scenario: Update IPT dataset
    When update dataset "Test Dataset Registry2" with key "d82273f6-9738-48a5-a639-2086f9c49d18" using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
      | name        | Updated Dataset 1             |
      | description | Description of Test Dataset 1 |
    Then response status should be 204
    And updated dataset is
      | publishingOrganizationKey            | installationKey                      | type       | title             | description                   | logoUrl               | homepage                |
      | 36107c15-771c-4810-a298-b7558828b8bd | 2fe63cec-9b23-4974-bab1-9f4118ef7711 | OCCURRENCE | Updated Dataset 1 | Description of Test Dataset 1 | http://www.logo.com/1 | http://www.homepage.com |
    And updated dataset contacts are
      | type           | email                  | firstName  | address                              | phone    | primary |
      | administrative | elyk-kaarb@euskadi.eus | Jan Legind | Universitetsparken 15, 2100, Denmark | 90909090 | true    |
    And updated dataset endpoints are
      | url                                    | type        |
      | http://ipt.gbif.org/archive.do?r=ds123 | DWC_ARCHIVE |
      | http://ipt.gbif.org/eml.do?r=ds123     | EML         |
    And following dataset fields were not updated
      | created | createdBy | language | rights | citation.identifier | abbreviation | alias |
    And total number of datasets is 1
    Given store dataset contactKey and endpointKey
    When update dataset "Test Dataset Registry2" with key "d82273f6-9738-48a5-a639-2086f9c49d18" using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
      | name        | Updated Dataset 2             |
      | description | Description of Test Dataset 2 |
    Then response status should be 204
    And updated dataset is
      | publishingOrganizationKey            | installationKey                      | type       | title             | description                   | logoUrl               | homepage                |
      | 36107c15-771c-4810-a298-b7558828b8bd | 2fe63cec-9b23-4974-bab1-9f4118ef7711 | OCCURRENCE | Updated Dataset 2 | Description of Test Dataset 2 | http://www.logo.com/1 | http://www.homepage.com |
    And total number of datasets is 1
    And dataset contactKey is the same
    But dataset endpointKey was updated

  Scenario: Update IPT dataset by invalid random organization key fails
    When update dataset "Test Dataset Registry2" with key "d82273f6-9738-48a5-a639-2086f9c49d18" using valid organization key "73401488-ac6f-4d5e-b766-50e11d006eeb" and password "welcome"
      | name        | Updated Dataset 1             |
      | description | Description of Test Dataset 1 |
    Then response status should be 401

  Scenario: Update IPT dataset without primary contact fails
    Given dataset parameters without field "primaryContactEmail"
    When update dataset "Test Dataset Registry2" with key "d82273f6-9738-48a5-a639-2086f9c49d18" using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
      | name        | Updated Dataset 1             |
      | description | Description of Test Dataset 1 |
    Then response status should be 400

  Scenario: Delete IPT dataset
    Given total number of datasets is 1
    When delete dataset "Test Dataset Registry2" with valid key "d82273f6-9738-48a5-a639-2086f9c49d18" using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 200
    And total number of datasets is 0

  Scenario: Delete IPT not existing dataset
    When delete dataset "Test Dataset Registry2" with invalid key "73401488-ac6f-4d5e-b766-50e11d006eeb" using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 401
    And total number of datasets is 1
