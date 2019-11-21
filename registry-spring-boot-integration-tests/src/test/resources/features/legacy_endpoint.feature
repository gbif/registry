@LegacyEndpoint
Feature: LegacyEndpointResource functionality

  Background:
    Given organization "Org" with key "36107c15-771c-4810-a298-b7558828b8bd"
    And installation "Test IPT Registry2" with key "2fe63cec-9b23-4974-bab1-9f4118ef7711"
    And dataset "Occurrence Dataset 1" with key "d82273f6-9738-48a5-a639-2086f9c49d18"
    And query parameters for endpoint registration or updating
      | resourceKey    | d82273f6-9738-48a5-a639-2086f9c49d18        |
      | description    | Description of Test Endpoint                |
      | type           | EML                                         |
      | accessPointURL | http://ipt.gbif.org/eml.do?r=bigdbtest&v=18 |

  @RegisterLegacyEndpoint
  Scenario: Register legacy endpoint
    When register new endpoint using valid organization key "36107c15-771c-4810-a298-b7558828b8bd" and password "welcome"
    Then response status should be 201
