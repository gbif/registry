<p>Dear ${name},</p>

<p>Your GBIF.org account ${name} has been deleted, and your personal information has been erased.</p>

<#if downloadUrls?size != 0>
    <p>Occurrence download metadata is not deleted.  You can still cite any of the downloads you created:</p>
    <p>
        <#list downloadUrls as downloadUrl>
            <a href="${downloadUrl}">${downloadUrl}</a>
        </#list>
    </p>
    <p>If you do cite a GBIF.org download, please write to communications@gbif.org with the DOI.</p>
</#if>

<p>Should you wish to download data from GBIF.org in the future, you will need to create a new account.</p>

<p>Thanks, on behalf of the GBIF network,</p>

<p>The GBIF Secretariat</p>
