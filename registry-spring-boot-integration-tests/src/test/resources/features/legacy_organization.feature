@LegacyOrganization
Feature: LegacyOrganizationResource functionality

  Background:
    Given node "The UK National Node" with key "25871695-fe22-4407-894d-cb595d209690"
    And organization "The BGBM" with key "0af41159-061f-4693-b2e5-d3d062a8285d"
    And contact with key "1985" of organization "The GBGM"

  Scenario Outline: Request Organization (GET) with no parameters and "<extension>", signifying that the response must be <case>
    When get organization "0af41159-061f-4693-b2e5-d3d062a8285d" with no credentials and extension "<extension>"
    Then response status should be 200
    And <case> is expected
    And returned response is
      | key                                  | name     | nameLanguage | description             | descriptionLanguage | homepageURL              | primaryContactType | primaryContactName | primaryContactEmail | primaryContactAddress | primaryContactPhone | primaryContactDescription | nodeKey                              | nodeName             | nodeContactEmail |
      | 0af41159-061f-4693-b2e5-d3d062a8285d | The BGBM | de           | The Berlin Botanical... | de                  | [http://www.example.org] | technical          | Tim Robertson      | trobertson@gbif.org | Universitetsparken 15 | +45 28261487        | Description stuff         | 25871695-fe22-4407-894d-cb595d209690 | The UK National Node |                  |

    Scenarios:
      | case    | extension |
      | JSON    | .json     |
      | XML     | .xml      |
      | MISSING |           |

  Scenario Outline: Request Organization (GET) with parameter <paramName>=<paramValue> to check if the organization credentials (key/password) supplied are correct
    When get organization "0af41159-061f-4693-b2e5-d3d062a8285d" with login "0af41159-061f-4693-b2e5-d3d062a8285d" and password "password" and extension ".json" and parameter <paramName> with value <paramValue>
    Then response status should be 200

    Scenarios:
      | paramName | paramValue |
      | op        | login      |
      | op        | password   |
