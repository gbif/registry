<#include "../functions.ftl">
<#escape x as x?xml>
<oai_dc:dc xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
    <dc:title>${dataset.title!}</dc:title>

    <#if dataset.keywordCollections?has_content>
        <#list dataset.keywordCollections![] as kwc>
            <dc:subject>${kwc.keywords?join(", ")}</dc:subject>
        </#list>
    </#if>

    <dc:description>${dataset.description!}</dc:description>

    <#-- The date on which the resource was published. -->
    <#if dataset.pubDate?has_content>
    <dc:date>${isodate(dataset.pubDate)}</dc:date>
    </#if>
    <dc:language>${dataset.dataLanguage!"en"}</dc:language>
    <#if dataset.rights?has_content>
    <dc:rights>${dataset.rights}</dc:rights>
    </#if>

    <#if !useDoiAsIdentifier && dataset.doi?exists>
        <dc:identifier>${dataset.doi}</dc:identifier>
    </#if>
    <#list dataset.identifiers![] as altid>
        <#if altid.identifier?has_content>
            <dc:identifier>${altid.identifier}</dc:identifier>
        </#if>
    </#list>
</oai_dc:dc>
</#escape>