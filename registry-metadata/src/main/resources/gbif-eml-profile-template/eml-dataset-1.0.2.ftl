<#-- @ftlvariable name="dataset" type="org.gbif.api.model.registry.Dataset" -->
<#escape x as x?xml>
  <#macro elem name value><#if value?has_content><${name}>${value}</${name}></#if></#macro>
  <#macro interpretedEnum enum><#if enum??><#if enum.interpreted?has_content>${enum.interpreted.name()?lower_case?replace("_", " ")?capitalize}<#else>${enum.verbatim!}</#if></#if></#macro>
  <#macro citation cit>
    <#if cit??>
      <#if cit.identifier?has_content>
        <citation identifier="${cit.identifier}">${cit.text!cit.identifier}</citation>
      <#else>
        <citation>${cit.text!}</citation>
      </#if>
    </#if>
  </#macro>
  <#macro contact ct withRole=false>
    <#if ct.lastName?has_content>
    <individualName>
      <@elem "givenName", ct.firstName! />
      <surName>${ct.lastName!}</surName>
    </individualName>
    </#if>
    <#if ct.userId?has_content>
     <#list ct.userId as uid>
    <userId directory="">${uid}</userId>
     </#list>
    </#if>
    <@elem "organizationName", ct.organization! />
    <#list ct.position![] as p>
      <@elem "positionName", p! />
    </#list>
    <#if ct.address?has_content || ct.city?has_content || ct.province?has_content || ct.postalCode?has_content || ct.country?has_content>
    <address>
      <#list ct.address![] as ad>
        <@elem "deliveryPoint", ad! />
      </#list>
      <@elem "city", ct.city! />
      <@elem "administrativeArea", ct.province! />
      <@elem "postalCode", ct.postalCode! />
      <@elem "country", ct.country! />
    </address>
    </#if>
    <#list ct.phone![] as p>
      <@elem "phone", p! />
    </#list>
    <#list ct.email![] as e>
      <@elem "electronicMailAddress", e! />
    </#list>
    <#list ct.homepage![] as h>
      <@elem "onlineUrl", h! />
    </#list>
    <#if withRole && ct.type?has_content>
    <role>${ct.type}</role>
    </#if>
  </#macro>
  <#assign DATEIsoFormat="yyyy-MM-dd"/>
  <#macro xmlSchemaDateTime dt><#assign dt2=dt?datetime?string("yyyy-MM-dd'T'hh:mm:ss.SSSZ")/>${dt2?substring(0, dt2?length-2)}:${dt2?substring(dt2?length-2, dt2?length)}</#macro>
  <#macro isodate dt>
    <#if dt?has_content>
      <#if dt?string("SSS")=="001">
        ${dt?string("yyyy")}
      <#else>
        ${dt?string(DATEIsoFormat)}
      </#if>
    </#if>
  </#macro>
<eml:eml xmlns:eml="eml://ecoinformatics.org/eml-2.1.1"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="eml://ecoinformatics.org/eml-2.1.1 http://rs.gbif.org/schema/eml-gbif-profile/1.0.2/eml.xsd"
         packageId=<#if useDoiAsIdentifier && dataset.doi?exists>"${dataset.doi.doiName}"<#else>"${dataset.key!}"</#if>  system="http://gbif.org" scope="system"
         <#if dataset.language??>xml:lang="${dataset.language.getIso2LetterCode()}"</#if>>

<dataset>
  <#if !useDoiAsIdentifier && dataset.doi?exists>
      <alternateIdentifier system="doi.org">${dataset.doi}</alternateIdentifier>
  </#if>
  <#list dataset.identifiers![] as altid>
    <#if altid.identifier?has_content>
    <alternateIdentifier>${altid.identifier}</alternateIdentifier>
    </#if>
  </#list>
  <title>${dataset.title!}</title>
<#-- The creator is the person who created the resource (not necessarily the author of this metadata about the resource). -->
<creator>
  <@contact ct=eml.resourceCreator! />
</creator>
<#-- The contact responsible for the creation of the metadata. -->
<metadataProvider>
  <@contact ct=eml.metadataProvider! />
</metadataProvider>
<#list eml.associatedParties![] as associatedParty>
<associatedParty>
  <@contact ct=associatedParty withRole=true />
</associatedParty>
</#list>
<#-- The date on which the resource was published. -->
<#if dataset.pubDate?has_content>
<pubDate>
  <@isodate dataset.pubDate />
</pubDate>
</#if>
<language>${dataset.dataLanguage!"en"}</language>
<#-- A brief description of the resource. -->
<abstract>
  <para>${dataset.description!}</para>
</abstract>
<#-- Zero or more sets of keywords and an associated thesaurus for each. -->
<#list dataset.keywordCollections![] as ks>
  <#if ks.keywords?has_content>
  <keywordSet>
    <#list ks.keywords![] as k>
      <keyword>${k!}</keyword>
    </#list>
    <#if ks.thesaurus?has_content>
      <keywordThesaurus>${ks.thesaurus}</keywordThesaurus>
    </#if>
  </keywordSet>
  </#if>
</#list>
<#-- Any additional information about the resource not covered in any other element. -->
  <#if dataset.additionalInfo?has_content>
  <additionalInfo>
    <para>${dataset.additionalInfo}</para>
  </additionalInfo>
  </#if>
<#-- A statement of the intellectual property rights associated with the resource. -->
  <#if dataset.rights?has_content>
  <intellectualRights>
    <para>${dataset.rights}</para>
  </intellectualRights>
  </#if>
  <#if dataset.homepage?has_content>
  <distribution scope="document">
    <online>
      <url function="information">${dataset.homepage}</url>
    </online>
  </distribution>
  </#if>
  <#if dataset.geographicCoverages?has_content || dataset.taxonomicCoverages?has_content || dataset.temporalCoverages?has_content>
  <coverage>
    <#list dataset.geographicCoverages![] as geocoverage>
      <geographicCoverage>
        <#if geocoverage.description?has_content>
          <geographicDescription>${geocoverage.description}</geographicDescription>
        </#if>
        <#if geocoverage.boundingBox?has_content>
        <boundingCoordinates>
          <westBoundingCoordinate>${geocoverage.boundingBox.minLongitude}</westBoundingCoordinate>
          <eastBoundingCoordinate>${geocoverage.boundingBox.maxLongitude}</eastBoundingCoordinate>
          <northBoundingCoordinate>${geocoverage.boundingBox.maxLatitude}</northBoundingCoordinate>
          <southBoundingCoordinate>${geocoverage.boundingBox.minLatitude}</southBoundingCoordinate>
        </boundingCoordinates>
        </#if>
      </geographicCoverage>
    </#list>
    <#list dataset.temporalCoverages![] as tempcoverage>
      <temporalCoverage>
        <#if tempcoverage.start?has_content>
          <rangeOfDates>
            <beginDate>
              <calendarDate><@isodate tempcoverage.start! /></calendarDate>
            </beginDate>
            <endDate>
              <calendarDate><@isodate tempcoverage.end! /></calendarDate>
            </endDate>
          </rangeOfDates>
        <#elseif tempcoverage.date?has_content>
          <singleDateTime>
            <calendarDate><@isodate tempcoverage.date! /></calendarDate>
          </singleDateTime>
        <#elseif tempcoverage.period?has_content>
          <#-- The VerbatimTimePeriod class is reserved for the GBIF specific additional metadata-->
        </#if>
      </temporalCoverage>
    </#list>
    <#list dataset.taxonomicCoverages![] as taxoncoverage>
      <#if taxoncoverage.coverages?has_content>
        <taxonomicCoverage>
          <#if taxoncoverage.description?has_content>
            <generalTaxonomicCoverage>${taxoncoverage.description}</generalTaxonomicCoverage>
          </#if>
          <#list taxoncoverage.coverages![] as tk>
            <taxonomicClassification>
              <#if tk.rank?has_content>
                <taxonRankName><@interpretedEnum tk.rank/></taxonRankName>
              </#if>
              <taxonRankValue>${tk.scientificName!}</taxonRankValue>
              <#if tk.commonName?has_content>
                <commonName>${tk.commonName}</commonName>
              </#if>
            </taxonomicClassification>
          </#list>
        </taxonomicCoverage>
      </#if>
    </#list>
  </coverage>
  </#if>
  <#if dataset.purpose?has_content>
  <purpose>
    <para>${dataset.purpose}</para>
  </purpose>
  </#if>
  <#if eml.administrativeContact?has_content>
  <contact>
    <@contact ct=eml.administrativeContact />
  </contact>
  </#if>
  <#if dataset.samplingDescription??>
  <methods>
    <#list dataset.samplingDescription.methodSteps![] as methodStep>
      <methodStep>
        <description>
          <para>${methodStep!}</para>
        </description>
      </methodStep>
    </#list>
    <#if dataset.samplingDescription.studyExtent?has_content || dataset.samplingDescription.sampling?has_content >
      <sampling>
        <studyExtent>
          <description>
            <para>${dataset.samplingDescription.studyExtent!}</para>
          </description>
        </studyExtent>
        <samplingDescription>
          <para>${dataset.samplingDescription.sampling!}</para>
        </samplingDescription>
      </sampling>
    </#if>
    <#if dataset.samplingDescription.qualityControl?has_content>
      <qualityControl>
        <description>
          <para>${dataset.samplingDescription.qualityControl}</para>
        </description>
      </qualityControl>
    </#if>
  </methods>
  </#if>

  <#if dataset.project??>
    <project>
      <title>${dataset.project.title!}</title>
      <#list dataset.project.contacts![] as c>
      <personnel>
        <@contact ct=c withRole=true />
      </personnel>
      </#list>
      <abstract>
        <para>${dataset.project.abstract!}</para>
      </abstract>
      <funding>
        <para>${dataset.project.funding!}</para>
      </funding>
    <#if dataset.project.studyAreaDescription?has_content>
      <studyAreaDescription>
        <descriptor name="description">
          <descriptorValue>${dataset.project.studyAreaDescription}</descriptorValue>
        </descriptor>
      </studyAreaDescription>
    </#if>
    <#if dataset.project.designDescription?has_content>
      <designDescription>
        <description>
          <para>${dataset.project.designDescription}</para>
        </description>
      </designDescription>
    </#if>
    </project>
  </#if>
</dataset>

<additionalMetadata>
  <metadata>
    <gbif>
      <#-- TODO: correctly mapped? -->
      <#if dataset.modified??>
        <dateStamp><@xmlSchemaDateTime dataset.modified/></dateStamp>
      </#if>
      <#-- How to cite the resource. -->
      <#if dataset.citation?? && dataset.citation.text?has_content>
        <@citation dataset.citation />
      </#if>
      <#if dataset.bibliographicCitations?has_content>
        <bibliography>
          <#list dataset.bibliographicCitations as bcit>
            <@citation bcit />
          </#list>
        </bibliography>
      </#if>
      <#list dataset.dataDescriptions![] as pdata>
        <physical>
          <objectName>${pdata.name!}</objectName>
          <characterEncoding>${pdata.charset!}</characterEncoding>
          <dataFormat>
            <externallyDefinedFormat>
              <formatName>${pdata.format!}</formatName>
              <#if pdata.formatVersion?has_content>
                <formatVersion>${pdata.formatVersion}</formatVersion>
              </#if>
            </externallyDefinedFormat>
          </dataFormat>
          <distribution>
            <online>
              <url function="download">${pdata.url!}</url>
            </online>
          </distribution>
        </physical>
      </#list>
      <#if dataset.logoUrl?has_content>
        <resourceLogoUrl>${dataset.logoUrl}</resourceLogoUrl>
      </#if>
      <#if dataset.collections?has_content >
        <#-- TODO: Current GBIF Profile only allows a single collection! -->
        <#assign col=dataset.collections[0] />
        <collection>
          <parentCollectionIdentifier>${col.parentIdentifier!}</parentCollectionIdentifier>
          <collectionIdentifier>${col.identifier!}</collectionIdentifier>
          <collectionName>${col.name!}</collectionName>
        </collection>
      </#if>
      <#list eml.formationPeriods![] as p>
        <formationPeriod>${p.period!}</formationPeriod>
      </#list>
      <#list dataset.collections![] as col>
       <#if col.specimenPreservationMethod??>
        <specimenPreservationMethod>${col.specimenPreservationMethod}</specimenPreservationMethod>
       </#if>
      </#list>
      <#list eml.livingTimePeriods![] as p>
        <livingTimePeriod>${p.period!}</livingTimePeriod>
      </#list>
      <#list dataset.collections![] as col>
        <#list col.curatorialUnits![] as unit>
          <#if unit.count gt 0>
              <jgtiCuratorialUnit>
                  <jgtiUnitType>${unit.type!unit.typeVerbatim!}</jgtiUnitType>
                  <jgtiUnits<#if unit.deviation gt 0> uncertaintyMeasure="${unit.deviation}"</#if>>${unit.count}</jgtiUnits>
              </jgtiCuratorialUnit>
          </#if>
          <#if unit.lower gt 0 || unit.upper gt 0>
            <jgtiCuratorialUnit>
                <jgtiUnitType>${unit.type!unit.typeVerbatim!}</jgtiUnitType>
                <jgtiUnitRange>
                    <beginRange>${unit.lower}</beginRange>
                    <endRange>${unit.upper}</endRange>
                </jgtiUnitRange>
            </jgtiCuratorialUnit>
          </#if>
        </#list>
      </#list>
    </gbif>
  </metadata>
</additionalMetadata>

</eml:eml>
</#escape>
