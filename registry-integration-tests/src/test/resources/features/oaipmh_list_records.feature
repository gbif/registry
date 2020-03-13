@Oaipmh
@OaipmhListRecords
Feature: Test the ListRecords verb of the OAI-PMH endpoint

  Background:
    Given node
      | key                                  | title                  |
      | 5c09dc92-68b0-40e0-aa8a-a98e9126eeea | The UK National Node 1 |
      | e4064af7-8656-4d10-8373-56f902c99ca1 | The UK National Node 2 |
    And organization
      | key                                  | nodeKey                              | title                | country |
      | 353bebfa-801f-4586-9474-2c6a1ec32352 | 5c09dc92-68b0-40e0-aa8a-a98e9126eeea | The BGBM Iceland     | IS      |
      | 8bf7ae57-bbed-4186-aa22-640882198b67 | e4064af7-8656-4d10-8373-56f902c99ca1 | The BGBM New Zealand | NZ      |
    And installation
      | key                                  | orgKey                               | title                           |
      | 4546be6b-2164-4b5f-be0d-7f7e334e6d02 | 353bebfa-801f-4586-9474-2c6a1ec32352 | The BGBM BIOCASE INSTALLATION 1 |
      | 88768fae-f12a-4cf5-b666-1a433685fcea | 353bebfa-801f-4586-9474-2c6a1ec32352 | The BGBM BIOCASE INSTALLATION 2 |
      | 59f5ed3e-0f74-4a77-86ee-f2cd8e3f8a2f | 8bf7ae57-bbed-4186-aa22-640882198b67 | The BGBM BIOCASE INSTALLATION 3 |
    And dataset
      | key                                  | type       | installationKey                      | publishingOrganizationKey            |
      | e367cb07-3c32-4d44-a3c7-8f1da93d3929 | CHECKLIST  | 4546be6b-2164-4b5f-be0d-7f7e334e6d02 | 353bebfa-801f-4586-9474-2c6a1ec32352 |
      | 3c967323-db26-4223-9be3-ed744e45e1f9 | OCCURRENCE | 88768fae-f12a-4cf5-b666-1a433685fcea | 353bebfa-801f-4586-9474-2c6a1ec32352 |
      | 503298df-448a-4b77-b29b-0497ff4778c7 | CHECKLIST  | 59f5ed3e-0f74-4a77-86ee-f2cd8e3f8a2f | 8bf7ae57-bbed-4186-aa22-640882198b67 |


  Scenario Outline: ListRecords with set type "<setType>":"<value>". <expectedResult>
    When Perform OAI-PMH call with parameters
      | verb           | ListRecords          |
      | set            | <setType>:<setValue> |
      | metadataPrefix | eml                  |
    Then response status is 200
    And request parameters in response are correct
      | verb           | ListRecords |
      | metadataPrefix | eml         |
    And response contains <expectedNumberOfRecords> records

    Scenarios:
      | setType          | setValue                             | expectedNumberOfRecords | expectedResult                                            |
      | country          | IS                                   | 2                       | There are two records expected for Iceland                |
      | installation     | 4546be6b-2164-4b5f-be0d-7f7e334e6d02 | 1                       | There is one record expected for this installation        |
      | dataset_type     | OCCURRENCE                           | 1                       | There is one record expected for OCCURRENCE dataset type  |
      | non-existing-set | someValue                            | 0                       | There are zero records expected for non existing set type |


  Scenario: ListRecords without parameter "set" to test paging. There are 3 dataset overall, but only 2 datasets plus resumption token expected in the response
    Given Max list records size is 2
    When Perform OAI-PMH call with parameters
      | verb           | ListRecords |
      | metadataPrefix | eml         |
    Then response status is 200
    And request parameters in response are correct
      | verb           | ListRecords |
      | metadataPrefix | eml         |
    And response contains 2 records
    And resumption token
