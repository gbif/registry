@Enumeration
Feature: Enumeration functionality

  Scenario Outline: get enumeration basic <enumeration>
    When get enumeration basic "<enumeration>"
    Then response status should be 200
    And element number 0 is "<expected>"

    Scenarios:
      | enumeration      | expected                   |
      | Country          | AF                         |
      | ContactType      | TECHNICAL_POINT_OF_CONTACT |
      | InstallationType | IPT_INSTALLATION           |
      | Language         | abk                        |
      | Extension        | AUDUBON                    |

  Scenario: get country enumeration
    When get country enumeration
    Then response status should be 200
    And element number 0 is
      | iso2         | AF          |
      | iso3         | AFG         |
      | isoNumerical | 4           |
      | title        | Afghanistan |
      | gbifRegion   | ASIA        |
      | enumName     | AFGHANISTAN |

  Scenario: get language enumeration
    When get language enumeration
    Then response status should be 200
    And element number 0 is
      | iso2        | ab        |
      | iso3        | abk       |
      | title       | Abkhazian |
      | titleNative | Abkhazian |

  Scenario: get license enumeration
    When get license enumeration
    Then response status should be 200
    And element number 0 is "http://creativecommons.org/publicdomain/zero/1.0/legalcode"

  Scenario: get interpretation remark enumeration
    When get interpretationRemark enumeration
    Then response status should be 200
    And element number 0 is
      | id       | ZERO_COORDINATE |
      | severity | WARNING         |

  Scenario: get inventory of the enumeration supported
    When get enumeration inventory
    Then response status should be 200
    And element number 0 is "AccessionStatus"
