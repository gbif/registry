INSERT INTO public.node (key, gbif_region, continent, title, country, created_by, modified_by,
                         created, modified, deleted, fulltext_search, type, participation_status)
VALUES ('a49e75d9-7b07-4d01-9be8-6ab2133f42f9', 'EUROPE', 'EUROPE', 'The UK National Node', 'GB',
        'WS TEST', 'WS TEST', '2020-02-22 09:54:09.835039', '2020-02-22 09:54:09.835039', null,
        '''countri'':5 ''europ'':7,8 ''gb'':9 ''nation'':3 ''node'':4 ''uk'':2 ''vote'':6',
        'COUNTRY', 'VOTING');

INSERT INTO public.organization (key, endorsing_node_key, endorsement_approved, password, title,
                                 abbreviation, description, language, logo_url, city, province,
                                 country, postal_code, latitude, longitude, created_by, modified_by,
                                 created, modified, deleted, fulltext_search, email, phone,
                                 homepage, address, challenge_code_key)
VALUES ('ff593857-44c2-4011-be20-8403e8d0bd9a', 'a49e75d9-7b07-4d01-9be8-6ab2133f42f9', false,
        'password', 'The BGBM', 'BGBM', 'The Berlin Botanical...', 'de', 'http://www.example.org',
        'BERLIN', 'BERLIN', 'GB', '1408', null, null, 'WS TEST', 'WS TEST',
        '2020-02-22 09:54:09.988088', '2020-02-22 09:54:09.988088', null,
        '''1408'':16 ''2920202'':9 ''a@b.com'':8 ''berlin'':5,12,13,14 ''bgbm'':2,3 ''botan'':6 ''de'':7 ''gb'':15 ''www.example.org'':10,11',
        '{a@b.com}', '{2920202}', '{http://www.example.org}', '{Berliner}', null);

INSERT INTO public.installation (key, organization_key, type, title, description, created_by,
                                 modified_by, created, modified, deleted, fulltext_search, password,
                                 disabled)
VALUES ('1e9136f0-78fd-40cd-8b25-26c78a376d8d', 'ff593857-44c2-4011-be20-8403e8d0bd9a',
        'IPT_INSTALLATION', 'The BGBM BIOCASE INSTALLATION', 'The Berlin Botanical...', 'WS TEST',
        'WS TEST', '2020-02-22 09:54:10.094782', '2020-02-22 09:54:10.094782', null,
        '''berlin'':8 ''bgbm'':2 ''biocas'':3 ''botan'':9 ''instal'':4,6 ''ipt'':5', null, false);

INSERT INTO public.dataset (key, parent_dataset_key, duplicate_of_dataset_key, installation_key, publishing_organization_key, external, type, sub_type, title, alias, abbreviation, description, language, homepage, logo_url, citation, citation_identifier, rights, locked_for_auto_update, created_by, modified_by, created, modified, deleted, fulltext_search, doi, license, maintenance_update_frequency, version) VALUES ('b951d9f4-57f8-4cd8-b7cf-6b44f325d318', null, null, '1e9136f0-78fd-40cd-8b25-26c78a376d8d', 'ff593857-44c2-4011-be20-8403e8d0bd9a', false, 'CHECKLIST', null, 'Pontaurus needs more than 255 characters for it''s title. It is a very, very, very, very long title in the German language. Word by word and character by character it''s exact title is: "Vegetationskundliche Untersuchungen in der Hochgebirgsregion der Bolkar Daglari & Aladaglari, Türkei"', 'BGBM', 'BGBM', 'The Berlin Botanical...', 'da', 'http://www.example.org', 'http://www.example.org', 'This is a citation text', 'ABC', 'The rights', false, 'WS TEST', 'WS TEST', '2020-02-22 09:54:10.223198', '2020-02-21 23:00:00.000000', null, '''255'':5 ''aladaglari'':44 ''berlin'':50 ''bgbm'':47,48 ''bolkar'':42 ''botan'':51 ''charact'':6,28,30 ''checklist'':46 ''citat'':56 ''daglari'':43 ''der'':39,41 ''exact'':33 ''german'':22 ''hochgebirgsregion'':40 ''languag'':23 ''long'':18 ''need'':2 ''pontaurus'':1 ''text'':57 ''titl'':10,19,34 ''turkei'':45 ''untersuchungen'':37 ''vegetationskundlich'':36 ''word'':24,26 ''www.example.org'':52', '10.21373/gbif.2014.xsd123', 'CC_BY_NC_4_0', null, null);


INSERT INTO public.metadata (key, created_by, modified_by, created, modified, dataset_key, content, type) VALUES (-1, 'UNKNOWN USER', 'UNKNOWN USER', '2020-02-27 16:37:04.353203', '2020-02-27 16:37:04.353203', 'b951d9f4-57f8-4cd8-b7cf-6b44f325d318', '<?xml version="1.0" encoding="utf-8"?>
<!--
        This is a sample metadata document that complies with GBIF Extended Metadata Profile v1.0.1.
        It is intended for use in unit testing only and does not contain real data.
        -->
<eml:eml xmlns:eml="eml://ecoinformatics.org/eml-2.1.1"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="eml://ecoinformatics.org/eml-2.1.1 http://rs.gbif.org/schema/eml-gbif-profile/1.0.1/eml-gbif-profile.xsd"
         xml:lang="en_US"
         packageId="619a4b95-1a82-4006-be6a-7dbe3c9b33c5/v7" system="http://gbif.org" scope="system">

  <!-- The IPT is concerned with descriptions of datasets only -->
  <dataset>
    <alternateIdentifier>619a4b95-1a82-4006-be6a-7dbe3c9b33c5</alternateIdentifier>
    <alternateIdentifier>doi:10.1093/ageing/29.1.57</alternateIdentifier>
    <alternateIdentifier>http://ageing.oxfordjournals.org/content/29/1/57</alternateIdentifier>

    <title xml:lang="en">Tanzanian Entomological Collection</title>
    <title xml:lang="de">Entymologische Sammlung Tansania</title>

    <!-- The creator is the person who created the resource (not necessarily the
author of this metadata about the resource) -->
    <creator>
      <individualName>
        <givenName>DavidTheCreator</givenName>
        <surName>Remsen</surName>
      </individualName>
      <organizationName>GBIF</organizationName>
      <positionName>ECAT Programme Officer</positionName>
      <address>
        <deliveryPoint>Universitestparken 15</deliveryPoint>
        <city>Copenhagen</city>
        <administrativeArea>Sjaelland</administrativeArea>
        <postalCode>2100</postalCode>
        <country>DK</country>
      </address>
      <phone>+4528261487</phone>
      <electronicMailAddress>dremsen@gbif.org</electronicMailAddress>
      <onlineUrl>http://www.gbif.org</onlineUrl>
    </creator>

    <!-- The responsible party for the creation of the metadata -->
    <metadataProvider>
      <individualName>
        <givenName>Tim</givenName>
        <surName>Robertson</surName>
      </individualName>
      <address>
        <deliveryPoint>Universitestparken 15</deliveryPoint>
        <city>Copenhagen</city>
        <administrativeArea>Copenhagen</administrativeArea>
        <postalCode>2100</postalCode>
        <country>DK</country>
      </address>
      <phone>+4528261487</phone>
      <electronicMailAddress>trobertson@gbif.org</electronicMailAddress>
      <onlineUrl>http://www.gbif.org</onlineUrl>
    </metadataProvider>

    <!-- Note that associated parties have roles -->
    <associatedParty>
      <individualName>
        <surName>Doering</surName>
      </individualName>
      <phone>4535321487</phone>
      <!-- The IPT will define a controlled vocabulary for this term -->
      <role>principalInvestigator</role>
    </associatedParty>
    <associatedParty>
      <individualName>
        <surName>Hahn</surName>
      </individualName>
      <phone>4535321478</phone>
      <!-- The IPT will define a controlled vocabulary for this term -->
      <role>pointOfContact</role>
    </associatedParty>

    <!-- Identified in the Other section of the GBIF Extended Metadata Doc -->
    <pubDate>2010-02-02</pubDate>

    <!-- Identified in the Other section of the GBIF Extended Metadata Doc -->
    <!-- This is the RESOURCE language and not the metadata language which is at the bottom -->
    <language>en_US</language>

    <!-- The brief overview -->
    <abstract>
      <para>Specimens in jars</para>
    </abstract>

    <!-- Keywords can optionally reference a thesaurus -->
    <keywordSet>
      <keyword>Insect</keyword>
      <keyword>Fly</keyword>
      <keyword>Bee</keyword>
      <keywordThesaurus>Zoology Vocabulary Version 1</keywordThesaurus>
    </keywordSet>
    <keywordSet>
      <keyword>Spider</keyword>
      <keywordThesaurus>Zoology Vocabulary Version 1</keywordThesaurus>
    </keywordSet>

    <!-- Not mentioned in the GBIF Extended Metadata Doc but seems sensible to keep this element -->
    <additionalInfo>
      <para>Where can the additional information possibly come from?!</para>
    </additionalInfo>

    <!-- Identified in the Other section of the GBIF Extended Metadata Doc -->
    <intellectualRights>
      <para>Owner grants XXX a worldwide, non-exclusive right to: (i) use, reproduce, perform,
        display, archive, transmit and distribute the Content (including any trademarks,
        tradenames and logos in the Content) in electronic form in connection with the Site,
        (ii) allow users of the Site to use, search, copy, download and transmit the
        Content, and (iii) modify and reformat the Content, but solely to the extent
        necessary and for the purposes of: (a) conforming to the format and "look and feel"
        of the Site, and (b) creating snippets, headlines or teasers consisting of selected
        lines or sections from the Content to be displayed on the Site (or displayed on
        other websites owned by XXX for the purposes of directing traffic to the Site).
      </para>
    </intellectualRights>


    <!-- The distributionType URL is generally meant for informational purposes, and the "function" attribute should be set to "information". -->
    <distribution scope="document">
      <online>
        <url function="information">http://www.any.org/fauna/coleoptera/beetleList.html
        </url>
      </online>
    </distribution>

    <!-- 3 types of coverage are supported with example repetition -->
    <coverage>
      <geographicCoverage>
        <geographicDescription>Bounding Box 1</geographicDescription>
        <boundingCoordinates>
          <westBoundingCoordinate>-1.564</westBoundingCoordinate>
          <eastBoundingCoordinate>0.703</eastBoundingCoordinate>
          <northBoundingCoordinate>23.975</northBoundingCoordinate>
          <southBoundingCoordinate>-22.745</southBoundingCoordinate>
        </boundingCoordinates>
      </geographicCoverage>
      <geographicCoverage>
        <geographicDescription>Bounding Box 2</geographicDescription>
        <boundingCoordinates>
          <westBoundingCoordinate>-10.703</westBoundingCoordinate>
          <eastBoundingCoordinate>11.564</eastBoundingCoordinate>
          <northBoundingCoordinate>43.975</northBoundingCoordinate>
          <southBoundingCoordinate>-32.745</southBoundingCoordinate>
        </boundingCoordinates>
      </geographicCoverage>
      <temporalCoverage>
        <rangeOfDates>
          <beginDate>
            <calendarDate>2009-12-01</calendarDate>
          </beginDate>
          <endDate>
            <calendarDate>2009-12-30</calendarDate>
          </endDate>
        </rangeOfDates>
      </temporalCoverage>
      <temporalCoverage>
        <singleDateTime>
          <calendarDate>2008-06-01</calendarDate>
        </singleDateTime>
      </temporalCoverage>
      <taxonomicCoverage>
        <generalTaxonomicCoverage>This is a general taxon coverage with only the scientific name</generalTaxonomicCoverage>
        <taxonomicClassification>
          <taxonRankValue>Mammalia</taxonRankValue>
        </taxonomicClassification>
        <taxonomicClassification>
          <taxonRankValue>Reptilia</taxonRankValue>
        </taxonomicClassification>
        <taxonomicClassification>
          <taxonRankValue>Coleoptera</taxonRankValue>
        </taxonomicClassification>
      </taxonomicCoverage>
      <taxonomicCoverage>
        <generalTaxonomicCoverage>This is a second taxon coverage with all fields</generalTaxonomicCoverage>
        <taxonomicClassification>
          <taxonRankName>Class</taxonRankName>
          <taxonRankValue>Aves</taxonRankValue>
          <commonName>Birds</commonName>
        </taxonomicClassification>
        <taxonomicClassification>
          <taxonRankName>kingdom</taxonRankName>
          <taxonRankValue>Plantae</taxonRankValue>
          <commonName>Plants</commonName>
        </taxonomicClassification>
        <taxonomicClassification>
          <taxonRankName>kingggggggggggggdom</taxonRankName>
          <taxonRankValue>Animalia</taxonRankValue>
          <commonName>Animals</commonName>
        </taxonomicClassification>
      </taxonomicCoverage>
    </coverage>

    <!-- Not mentioned in the GBIF Extended Metadata Doc but seems sensible to keep this element -->
    <purpose>
      <para>Provide data to the whole world.</para>
    </purpose>

    <!-- This is mandatory in EML
    In terms of the IPT, propose this be the same as the and should be the same as the <creator/>
    Therefore, it can be ignored in Parsing, but needs to be created in the output rendering
    -->
    <contact>
      <individualName>
        <givenName>David</givenName>
        <surName>Remsen</surName>
      </individualName>
      <organizationName>GBIF</organizationName>
      <positionName>ECAT Programme Officer</positionName>
      <address>
        <deliveryPoint>Universitestparken 15</deliveryPoint>
        <city>Copenhagen</city>
        <administrativeArea>Sjaelland</administrativeArea>
        <postalCode>2100</postalCode>
        <country>DK</country>
      </address>
      <phone>+4528261487</phone>
      <electronicMailAddress>dremsen@gbif.org</electronicMailAddress>
      <onlineUrl>http://www.gbif.org</onlineUrl>
    </contact>

    <!-- Methods used -->
    <methods>
      <methodStep>
        <description>
          <para>Took picture, identified</para>
        </description>
      </methodStep>
      <sampling>
        <studyExtent>
          <description>
            <para>Daily Obersevation of Pigeons Eating Habits</para>
          </description>
        </studyExtent>
        <samplingDescription>
          <para>44KHz is what a CD has... I was more like one a day if I felt like it</para>
        </samplingDescription>
      </sampling>
      <qualityControl>
        <description>
          <para>None</para>
        </description>
      </qualityControl>
      <!-- This step deliberately has no QC-->
      <methodStep>
        <description>
          <para>Themometer based test</para>
        </description>
      </methodStep>
      <!-- This step deliberately has no Sampling or QC -->
      <methodStep>
        <description>
          <para>Visual based test</para>
          <para>and one more time</para>
        </description>
      </methodStep>
    </methods>


    <project>
      <title>Documenting Some Asian Birds and Insects</title>
      <personnel>
        <individualName>
          <surName>Remsen</surName>
        </individualName>
        <role>publisher</role>
      </personnel>
      <funding>
        <para>My Deep Pockets</para>
      </funding>
      <studyAreaDescription>
        <descriptor name="generic" citableClassificationSystem="false">
          <descriptorValue>Turkish Mountains</descriptorValue>
        </descriptor>
      </studyAreaDescription>
      <designDescription>
        <description>
          <para>This was done in Avian Migration patterns</para>
        </description>
      </designDescription>
    </project>

  </dataset>


  <additionalMetadata>
    <metadata>
      <gbif>
        <!-- eml file creation date -->
        <dateStamp>2002-10-23T18:13:51.235+01:00</dateStamp>

        <!-- level to which the metadata dcoument applies; default for GBIF is "dataset";  "series" is the other common level -->
        <hierarchyLevel>dataset</hierarchyLevel>

        <!-- a citation for a "names" dataset -->
        <!-- Seems strange that there is no obvious place for citation in the /eml/dataset
Could be this can find a better home in the future -->
        <citation identifier="doi:tims-ident.2135.ex43.33.d">Tims assembled checklist</citation>
        <!-- citations of resources used, e.g., in a checklist -->
        <bibliography>
          <citation identifier="doi:tims-ident.2136.ex43.33.d">title 1</citation>
          <citation identifier="doi:tims-ident.2137.ex43.33.d">title 2</citation>
          <citation identifier="doi:tims-ident.2138.ex43.33.d">title 3</citation>
        </bibliography>

        <!-- Note the repetition -->
        <physical>
          <objectName>INV-GCEM-0305a1_1_1.shp</objectName>
          <characterEncoding>ASCII</characterEncoding>
          <dataFormat>
            <externallyDefinedFormat>
              <formatName>shapefile</formatName>
              <formatVersion>2.0</formatVersion>
            </externallyDefinedFormat>
          </dataFormat>
          <distribution>
            <online>
              <url function="download"
                >http://metacat.lternet.edu/knb/dataAccessServlet?docid=knb-lter-gce.109.10&amp;urlTail=accession=INV-GCEM-0305a1&amp;filename=INV-GCEM-0305a1_1_1.TXT
              </url>
            </online>
          </distribution>
        </physical>
        <physical>
          <objectName>INV-GCEM-0305a1_1_2.shp</objectName>
          <characterEncoding>ASCII</characterEncoding>
          <dataFormat>
            <externallyDefinedFormat>
              <formatName>shapefile</formatName>
              <formatVersion>2.0</formatVersion>
            </externallyDefinedFormat>
          </dataFormat>
          <distribution>
            <online>
              <url function="download"
                >http://metacat.lternet.edu/knb/dataAccessServlet?docid=knb-lter-gce.109.10&amp;urlTail=accession=INV-GCEM-0305a1&amp;filename=INV-GCEM-0305a1_1_2.TXT
              </url>
            </online>
          </distribution>
        </physical>

        <!-- URL of the logo associated with a resource -->
        <resourceLogoUrl>http://www.tim.org/logo.jpg</resourceLogoUrl>

        <!-- This combines 3 optional fields in one section, and dictates the format output by the IPT -->
        <collection>
          <parentCollectionIdentifier>urn:lsid:tim.org:12:1</parentCollectionIdentifier>
          <collectionIdentifier>urn:lsid:tim.org:12:2</collectionIdentifier>
          <collectionName>Mammals</collectionName>
        </collection>

        <!-- derived from NHC  -->
        <formationPeriod>During the 70s</formationPeriod>

        <!-- derived from NHC  -->
        <specimenPreservationMethod>alcohol</specimenPreservationMethod>
        <!-- derived from NHC  -->
        <livingTimePeriod>Jurassic</livingTimePeriod>
        <!-- for quantifying natural history collections datasets  -->
        <jgtiCuratorialUnit>
          <jgtiUnitType>SPECIMENS</jgtiUnitType>
          <jgtiUnits uncertaintyMeasure="1">5</jgtiUnits>
        </jgtiCuratorialUnit>
        <jgtiCuratorialUnit>
          <jgtiUnitType>Drawers</jgtiUnitType>
          <jgtiUnitRange>
            <beginRange>7</beginRange>
            <endRange>2</endRange>
          </jgtiUnitRange>
        </jgtiCuratorialUnit>

      </gbif>
    </metadata>
  </additionalMetadata>

</eml:eml>
', 'EML');
