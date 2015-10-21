<#include "../functions.ftl">
<#escape x as x?xml>
<oai_dc:dc xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
    <dc:title>${dataset.title!}</dc:title>
    <dc:publisher>${(organization.title)!}</dc:publisher>

    <#-- Always use only one identifier -->
    <dc:identifier>${dc.identifier}</dc:identifier>

    <#if dataset.keywordCollections?has_content>
    <#list dataset.keywordCollections![] as kwc>
    <dc:subject>${kwc.keywords?join(", ")}</dc:subject>
    </#list>
    </#if>

    <dc:source>${dataset.homepage!}</dc:source>
    <dc:description>${dataset.description!}</dc:description>
    <dc:type>Dataset</dc:type>

    <#list dc.creators![] as creator>
    <dc:creator>${creator}</dc:creator>
    </#list>

    <#-- The date on which the resource was published. -->
    <#if dataset.pubDate?has_content>
    <dc:date>${isodate(dataset.pubDate)}</dc:date>
    </#if>
    <dc:language>${(dataset.dataLanguage.iso2LetterCode)!"en"}</dc:language>
    <#if dataset.rights?has_content>
    <dc:rights>${dataset.rights}</dc:rights>
    </#if>
    <dc:source>Global Biodiversity Information Facility (GBIF) http://www.gbif.org</dc:source>
</oai_dc:dc>
</#escape>