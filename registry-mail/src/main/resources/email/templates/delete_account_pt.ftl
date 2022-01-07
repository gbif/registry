<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountDeleteDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Prezado ${name},</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Sua conta GBIF.org
    <b>${name}</b>    
    foi excluída e suas informações pessoais apagadas.
</p>

<#if downloadUrls?size != 0>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Os metadados do download de ocorrências não foram excluídos.  Você ainda pode citar qualquer um dos downloads que você criou:</p>
<ul style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <#list downloadUrls as downloadUrl>
    <li style="margin: 0 0 0 20px;padding: 0;line-height: 1.65;"><a href="${downloadUrl}" style="color: #4ba2ce;text-decoration: none;">${downloadUrl}</a></li>
    </#list>
</ul>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Se você usar um download de GBIG.org em uma publicação, por favor notifique <a href="mailto:communications@gbif.org" style="color: #4ba2ce;text-decoration: none;">communications@gbif.org</a>.</p>
</#if>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Caso deseje baixar dados do GBIF.org no futuro, você precisará criar uma nova conta.</p>


<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>
        Obrigado, em nome da rede GBIF,
        <br>
        Secretaria do GBIF
    </em>
</p>

<#include "footer.ftl">
