<p>Dear ${name},</p>

<p>The following message is to inform you that account ${name} has been deleted.</p>

<#if downloadUrls?size != 0>
    <p>Your occurrence downloads will still be available:</p>
    <p>
        <#list downloadUrls as downloadUrl>
            <a href="${downloadUrl}">${downloadUrl}</a>
        </#list>
    </p>
</#if>

<p>Kind regards,</p>
<p>The GBIF Secretariat</p>
