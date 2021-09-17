<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountDeleteDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Bonjour ${name},</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Votre compte GBIF.org
    <b>${name}</b>
    a été supprimé et vos informations personnelles ont été effacées.
</p>

<#if downloadUrls?size != 0>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Les métadonnées de téléchargement d'occurrence n'ont pas été supprimées.  Vous pouvez toujours citer les téléchargements que vous avez créés :</p>
<ul style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <#list downloadUrls as downloadUrl>
    <li style="margin: 0 0 0 20px;padding: 0;line-height: 1.65;"><a href="${downloadUrl}" style="color: #4ba2ce;text-decoration: none;">${downloadUrl}</a></li>
    </#list>
</ul>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Si vous utilisez un téléchargement GBIF.org dans une publication, veuillez en informer <a href="mailto:communications@gbif.org" style="color: #509E2F;text-decoration: none;">communications@gbif.org</a>.</p>
</#if>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Si vous souhaitez télécharger des données depuis GBIF.org dans le futur, vous devrez créer un nouveau compte.</p>


<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>
        Merci, au nom du réseau GBIF,
        <br>
        Le Secrétariat GBIF
    </em>
</p>

<#include "footer.ftl">
