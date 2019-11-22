@LegacyEndpoint
Feature: LegacyEndpointResource functionality

  Background:
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And installation "Test IPT Registry2" with key "2fe63cec-9b23-4974-bab1-9f4118ef7711"
    And dataset "Occurrence Dataset 1" with key "d82273f6-9738-48a5-a639-2086f9c49d18"
    And endpoint request parameters
      | resourceKey    | d82273f6-9738-48a5-a639-2086f9c49d18        |
      | description    | Description of Test Endpoint                |
      | type           | EML                                         |
      | accessPointURL | http://ipt.gbif.org/eml.do?r=bigdbtest&v=18 |

  Scenario: Register and delete legacy endpoint
    Given 0 endpoints in database before
    When register new endpoint using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 201
    And 1 endpoint in database after
    And registered endpoint is
      | resourceKey                          | description                  | type | accessPointURL                              |
      | d82273f6-9738-48a5-a639-2086f9c49d18 | Description of Test Endpoint | EML  | http://ipt.gbif.org/eml.do?r=bigdbtest&v=18 |
    When delete endpoint by valid resource key "d82273f6-9738-48a5-a639-2086f9c49d18" using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 200
    And 0 endpoints in database after

  Scenario: Register legacy endpoint by invalid random organization key fails
    Given 0 endpoints in database before
    When register new endpoint using invalid organization key "a1446513-90b8-481b-9bcf-d78c8f46e47b" and password "welcome"
    Then response status should be 401
    And 0 endpoints in database after

  Scenario Outline: Register legacy endpoint without mandatory parameter <parameter> key fails
    Given exclude parameter "<parameter>" from endpoint request parameters
    When register new endpoint using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 400

    Scenarios:
      | parameter      |
      | type           |
      | accessPointURL |
      | resourceKey    |

  Scenario: Delete legacy endpoint without resource key fails
    When delete endpoint without resource key using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 400

  Scenario: Delete legacy endpoint by resource key which has no dataset associated fails
    When delete endpoint by invalid resource key "a1446513-90b8-481b-9bcf-d78c8f46e47b" using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 401

  Scenario: Delete legacy endpoint by invalid random organization key fails
    When register new endpoint using invalid organization key "a1446513-90b8-481b-9bcf-d78c8f46e47b" and password "welcome"
    Then response status should be 401

  Scenario: Send a get all service types (GET) request, the JSON response having a name, description, overviewURL, and key for each service in the list
    When perform get all service types request
    Then response status should be 200
    And response is following JSON
      | description | name                       | overviewURL | key   |
      |             | EML                        |             | 16000 |
      |             | RSS                        |             | 16010 |
      |             | DIGIR                      |             | 16100 |
      |             | MANISDIGIR                 |             | 16110 |
      |             | TAPIR                      |             | 16120 |
      |             | BIOCASE                    |             | 16130 |
      |             | DWC-ARCHIVE-CHECKLIST      |             | 16160 |
      |             | DWC-ARCHIVE-OCCURRENCE     |             | 16170 |
      |             | OTHER                      |             | 16180 |
      |             | DWC-ARCHIVE-SAMPLING-EVENT |             | 16190 |
