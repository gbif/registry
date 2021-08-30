<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountDeleteDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Estimado ${name},</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Su cuenta en GBIF.org 
    <b>${name}</b>
    ha sido eliminada y su información personal ha sido borrada.
</p>

<#if downloadUrls?size != 0>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Los metadatos de las descargas de registros de presencia no se han borrado.  Todavía puede citar cualquiera de las descargas que haya realizado:</p>
<ul style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <#list downloadUrls as downloadUrl>
    <li style="margin: 0 0 0 20px;padding: 0;line-height: 1.65;"><a href="${downloadUrl}" style="color: #4ba2ce;text-decoration: none;">${downloadUrl}</a></li>
    </#list>
</ul>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Si utiliza alguna descarga de GBIF.org en una publicación, por favor, notifíquelo a <a href="mailto:communications@gbif.org" style="color: #4ba2ce;text-decoration: none;">communications@gbif.org</a>.</p>
</#if>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Si desea descargar más datos de GBIF.org en el futuro tendrá que crear una nueva cuenta.</p>


<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>
        Gracias en nombre de toda la comunidad de GBIF
        <br>
        La Secretaría de GBIF
    </em>
</p>

<#include "footer.ftl">
