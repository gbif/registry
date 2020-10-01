<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountDeleteDataModel" -->
<#include "header.ftl">

<h4 style="margin: 0;padding: 0;font-size: 20px;line-height: 1.25;margin-bottom: 20px;">Dear ${name},</h4>

<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">
    Your GBIF.org account
    <b style="margin: 0;padding: 0;line-height: 1.65;">${name}</b>
    has been deleted, and your personal information has been erased.
</p>

<#if downloadUrls?size != 0>
<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">Occurrence download metadata is not deleted.  You can still cite any of the downloads you created:</p>
<ul style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">
    <#list downloadUrls as downloadUrl>
    <li style="margin: 0 0 0 20px;padding: 0;line-height: 1.65;"><a href="${downloadUrl}" style="margin: 0;padding: 0;line-height: 1.65;color: #509E2F;text-decoration: none;">${downloadUrl}</a></li>
    </#list>
</ul>
<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">If you do cite a GBIF.org download, please write to <a href="mailto:communications@gbif.org" style="margin: 0;padding: 0;line-height: 1.65;color: #509E2F;text-decoration: none;">communications@gbif.org</a> with the DOI.</p>
</#if>

<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">Should you wish to download data from GBIF.org in the future, you will need to create a new account.</p>


<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">
    <em style="margin: 0;padding: 0;line-height: 1.65;">
        Thanks, on behalf of the GBIF network,
        <br style="margin: 0;padding: 0;line-height: 1.65;">
        The GBIF Secretariat
    </em>
</p>

<#include "footer.ftl">
