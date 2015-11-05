<#assign DATEIsoFormat="yyyy-MM-dd"/>
<#macro xmlSchemaDateTime dt><#assign dt2=dt?datetime?string("yyyy-MM-dd'T'hh:mm:ss.SSSZ")/>${dt2?substring(0, dt2?length-2)}:${dt2?substring(dt2?length-2, dt2?length)}</#macro>
<#function isodate dt>
    <#if dt?has_content>
        <#if dt?string("SSS")=="001">
            <#return dt?string("yyyy")>
        <#else>
            <#return dt?string(DATEIsoFormat)>
        </#if>
    </#if>
</#function>

<#-- Usage: <@printIfHasContent dataset.rights!; r><dc:rights>${r}</dc:rights></@printIfHasContent> -->
<#macro printIfHasContent var>
<#if var?has_content>
<#nested var>
</#if>
</#macro>
