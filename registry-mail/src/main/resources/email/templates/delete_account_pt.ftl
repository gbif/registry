<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountDeleteDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Prezado ${name},</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Sua conta GBIF.org
    <b>${name}</b>    
    foi excluída e suas informações pessoais apagadas.
</p>

<#if downloadUrls?size != 0>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Occurrence download metadata is not deleted.  You can still cite any of the downloads you created:</p>
<ul style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <#list downloadUrls as downloadUrl>
    <li style="margin: 0 0 0 20px;padding: 0;line-height: 1.65;"><a href="${downloadUrl}" style="color: #4ba2ce;text-decoration: none;">${downloadUrl}</a></li>
    </#list>
</ul>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">If you use a GBIF.org download in a publication, please notify <a href="mailto:communications@gbif.org" style="color: #4ba2ce;text-decoration: none;">communications@gbif.org</a>.</p>
</#if>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Should you wish to download data from GBIF.org in the future, you will need to create a new account.</p>


<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>
        Obrigado, em nome da rede GBIF,
        <br>
        Secretaria do GBIF
    </em>
</p>

<#include "footer.ftl">
