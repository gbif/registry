@LegacyOrganization
Feature: LegacyOrganizationResource functionality

  Background:
    Given node "The UK National Node" with key "25871695-fe22-4407-894d-cb595d209690"
    And organization "The BGBM" with key "0af41159-061f-4693-b2e5-d3d062a8285d"
    And organization "The Second Org" with key "d3894ec1-6eb0-48c8-bbb1-0a63f8731159"
    And organization "The Third Org" with key "c55dc610-63a2-4fbf-919d-0ad74b4f24dd"
    And contact with key "1985" of organization "The GBGM"
    And contact with key "2042" of organization "Org without email"

  Scenario Outline: Request Organization (GET) with no parameters and extension "<extension>", response content type should be "<expectedContentType>"
    When get organization "0af41159-061f-4693-b2e5-d3d062a8285d" with no credentials and extension "<extension>"
    Then response status should be 200
    And content type "<expectedContentType>" is expected
    And returned organization for case <case> is
      | key                                  | name     | nameLanguage | description             | descriptionLanguage | homepageURL              | primaryContactType | primaryContactName | primaryContactEmail | primaryContactAddress | primaryContactPhone | primaryContactDescription | nodeKey                              | nodeName             | nodeContactEmail |
      | 0af41159-061f-4693-b2e5-d3d062a8285d | The BGBM | de           | The Berlin Botanical... | de                  | [http://www.example.org] | technical          | Tim Robertson      | test@mailinator.com | Universitetsparken 15 | +45 28261487        | Description stuff         | 25871695-fe22-4407-894d-cb595d209690 | The UK National Node |                  |

    Scenarios:
      | case         | extension | expectedContentType |
      | JSON         | .json     | application/json    |
      | XML          | .xml      | application/xml     |
      | NO_EXTENSION |           | application/xml     |

  Scenario: Request Organization (GET) with parameter op and value "login" to check if the organization credentials (key/password) supplied are correct
    When get organization "0af41159-061f-4693-b2e5-d3d062a8285d" with login "0af41159-061f-4693-b2e5-d3d062a8285d" and password "password" and extension ".json" and parameter op with value login
    Then response status should be 200

  Scenario: Request Organization (GET) with parameter op and value "password"
    When get organization "0af41159-061f-4693-b2e5-d3d062a8285d" with extension ".json" and parameter op with value "password"
    Then response status should be 200

  Scenario: Request Organization (GET) with callback parameter, signifying that the response must be JSONP
    When get organization "0af41159-061f-4693-b2e5-d3d062a8285d" with extension ".json" and parameter callback with value "jQuery15106997501577716321_1384974875868&_=1384974903371"
    Then response status should be 200
    And response should start with "jQuery15106997501577716321_1384974875868&_=1384974903371({"

  @GetOrganizations
  Scenario Outline: Request all organizations (GET) with extension "<extension>", the response having a key and name for each organisation in the list
    When get organizations with extension "<extension>"
    Then response status should be 200
    And content type "<expectedContentType>" is expected
    And returned brief organizations response for case <case> are
      | key                                  | name           |
      | 0af41159-061f-4693-b2e5-d3d062a8285d | The BGBM       |
      | d3894ec1-6eb0-48c8-bbb1-0a63f8731159 | The Second Org |
      | c55dc610-63a2-4fbf-919d-0ad74b4f24dd | The Third Org  |

    Scenarios:
      | case         | extension | expectedContentType |
      | JSON         | .json     | application/json    |
      | XML          | .xml      | application/xml     |
      | NO_EXTENSION |           | application/xml     |

  Scenario: Send a password reminder (GET) request with op=password parameter, using an organization whose primarycontact doesn't have an email address. Internal Server Error 500 response is expected
    When get organization "c55dc610-63a2-4fbf-919d-0ad74b4f24dd" with extension ".json" and parameter op with value "password"
    Then response status should be 500

  Scenario: Send a request organisation (GET) with unknown extension ".unknown". Not Found 404 is expected
    When get organization "0af41159-061f-4693-b2e5-d3d062a8285d" with no credentials and extension ".unknown"
    Then response status should be 404

  Scenario: Send a request all organisations (GET) with unknown extension ".unknown". Not Found 404 is expected
    When get organizations with extension ".unknown"
    Then response status should be 404

  Scenario: Send a request organisation (GET) with unknown key. OK with explanation is expected
    When get organization "0af41159-061f-4693-b2e5-d3d062a82833" with no credentials and extension ".json"
    Then response status should be 200
    And returned response is "No organisation matches the key provided"

  Scenario: Send a request organisation (GET) with key with parameter op and value login using random UUID as login. Unauthorized 401 is expected
    When get organization "0af41159-061f-4693-b2e5-d3d062a8285d" with login "c55dc610-63a2-4fbf-919d-0ad74b4f24dd" and password "password" and extension ".json" and parameter op with value login
    Then response status should be 401


