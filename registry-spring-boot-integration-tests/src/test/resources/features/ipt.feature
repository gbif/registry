@IPT
Feature: IPT related functionality

  @RegisterIptInstallation
  Scenario: Register IPT installation
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And new installation to register
    When register new installation for organization "Org" using organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
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

  @UpdateIpt
  Scenario: Update IPT installation
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And installation "Test IPT Registry2" with key "2fe63cec-9b23-4974-bab1-9f4118ef7711"
    And installation to update
    When update installation "Test IPT Registry2" using installation key "2fe63cec-9b23-4974-bab1-9f4118ef7711" and password "welcome"
    Then response status should be 204
    And updated installation is
      | organisationKey                      | title              | type             | description             |
      | 36107c15-771c-4810-a298-b7558828b8bd | Test IPT Registry2 | IPT_INSTALLATION | Description of Test IPT |
    And updated installation contacts are
      | type      | email           | firstName  | primary |
      | technical | kbraak@gbif.org | Kyle Braak | true    |
    And updated installation endpoints are
      | url                        | type |
      | http://ipt.gbif.org/rss.do | FEED |
    And total number of installations / contacts / endpoints is 1 / 1 / 1
    Given store contactKey and endpointKey
    When update installation "Test IPT Registry2" using installation key "2fe63cec-9b23-4974-bab1-9f4118ef7711" and password "welcome"
    Then response status should be 204
    And total number of installations / contacts / endpoints is 1 / 1 / 1
    And contactKey is the same
    But endpointKey was updated

  Scenario: Register IPT installation by invalid random organisation key fails
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And new installation to register
    When register new installation for organization "Org" using organization key "73401488-ac6f-4d5e-b766-50e11d006eeb" and password "welcome"
    Then response status should be 401

  Scenario: Update IPT installation by invalid random installation key fails
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And installation "Test IPT Registry2" with key "2fe63cec-9b23-4974-bab1-9f4118ef7711"
    And installation to update
    When update installation "Test IPT Registry2" using installation key "73401488-ac6f-4d5e-b766-50e11d006eeb" and password "welcome"
    Then response status should be 401

  Scenario: Register IPT installation without primary contact
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And new installation to register
    But without field "primaryContactEmail"
    When register new installation for organization "Org" using organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 400

  Scenario: Update IPT installation without primary contact
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And installation "Test IPT Registry2" with key "2fe63cec-9b23-4974-bab1-9f4118ef7711"
    And installation to update
    But without field "primaryContactEmail"
    When update installation "Test IPT Registry2" using installation key "2fe63cec-9b23-4974-bab1-9f4118ef7711" and password "welcome"
    Then response status should be 400

  @RegisterIptDataset
  Scenario: Register IPT dataset
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And installation "Test IPT Registry2" with key "2fe63cec-9b23-4974-bab1-9f4118ef7711"
    And new dataset to register
    When register new dataset using organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 201
    And dataset UUID is returned
    And registered dataset is
      | organisationKey                      | name                   | primaryContactName | type       | description                 | homepageUrl             | logoUrl               | iptKey                               |
      | 36107c15-771c-4810-a298-b7558828b8bd | Test Dataset Registry2 | Jan Legind         | OCCURRENCE | Description of Test Dataset | http://www.homepage.com | http://www.logo.com/1 | 2fe63cec-9b23-4974-bab1-9f4118ef7711 |
    And registered dataset contacts are
      | type           | email                  | firstName  | address                              | phone    | primary |
      | administrative | elyk-kaarb@euskadi.eus | Jan Legind | Universitetsparken 15, 2100, Denmark | 90909090 | true    |
    And registered dataset endpoints are
      | url                                    | type        |
      | http://ipt.gbif.org/archive.do?r=ds123 | DWC_ARCHIVE |
      | http://ipt.gbif.org/eml.do?r=ds123     | EML         |
