@LegacyEndpoint
Feature: LegacyEndpointResource functionality

  Background:
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And installation "Test IPT Registry2" with key "2fe63cec-9b23-4974-bab1-9f4118ef7711"
    And dataset "Occurrence Dataset 1" with key "d82273f6-9738-48a5-a639-2086f9c49d18"
    And dataset endpoint "A Tapir installation" with key 1181
    And dataset endpoint "A Tapir installation 2" with key 1182
    And endpoint request parameters
      | resourceKey    | d82273f6-9738-48a5-a639-2086f9c49d18        |
      | description    | Description of Test Endpoint                |
      | type           | EML                                         |
      | accessPointURL | http://ipt.gbif.org/eml.do?r=bigdbtest&v=18 |

  Scenario: Register and delete legacy endpoint
    Given 2 endpoints in database before
    When register new endpoint using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 201
    And 3 endpoints in database after
    And registered endpoint in XML is
      | resourceKey                          | description                  | type | accessPointURL                              |
      | d82273f6-9738-48a5-a639-2086f9c49d18 | Description of Test Endpoint | EML  | http://ipt.gbif.org/eml.do?r=bigdbtest&v=18 |
    When delete endpoint by valid resource key "d82273f6-9738-48a5-a639-2086f9c49d18" using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 200
    And 0 endpoints in database after

  Scenario: Register legacy endpoint by invalid random organization key fails
    Given 2 endpoints in database before
    When register new endpoint using invalid organization key "a1446513-90b8-481b-9bcf-d78c8f46e47b" and password "welcome"
    Then response status should be 401
    And 2 endpoints in database after

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
    When perform get dataset endpoints request with extension ".json" and parameter op and value "types"
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

  Scenario Outline: Send a get all endpoints for dataset (GET) request with extension "<extension>", the response having at the very least the dataset key, endpoint key, endpoint type, and endpoint url
    Given 2 endpoint in database before
    When perform get dataset endpoints request with extension "<extension>" and parameter resourceKey and value "d82273f6-9738-48a5-a639-2086f9c49d18"
    Then response status should be 200
    And content type "<expectedContentType>" is expected
    And 2 endpoint in database after
    And returned response in <case> is
      | resourceKey                          | description            | type  | accessPointURL           |
      | d82273f6-9738-48a5-a639-2086f9c49d18 | A Tapir installation   | TAPIR | http://www.example.org/1 |
      | d82273f6-9738-48a5-a639-2086f9c49d18 | A Tapir installation 2 | TAPIR | http://www.example.org/2 |

    Scenarios:
      | case         | extension | expectedContentType |
      | JSON         | .json     | application/json    |
      | XML          | .xml      | application/xml     |
      | NO_EXTENSION |           | application/xml     |

  Scenario: Send a get endpoints (GET) request for a dataset that does not exist. The JSON response having an error message, not a 404
    When perform get dataset endpoints request with extension ".json" and parameter resourceKey and value "a1446513-90b8-481b-9bcf-d78c8f46e47b"
    Then response status should be 200
    And returned get endpoints error response is
      | No dataset matches the key provided |

  Scenario: Send a get endpoints (GET) request for a dataset without resourceKey parameter. Response should be 400
    When perform get dataset endpoints request with extension ".json" and without resourceKey parameter
    Then response status should be 400

  Scenario: Send a get endpoints (GET) request for a dataset with unknown extension parameter. Response should be 404
    When perform get dataset endpoints request with extension ".unknown" and parameter resourceKey and value "a1446513-90b8-481b-9bcf-d78c8f46e47b"
    Then response status should be 404
