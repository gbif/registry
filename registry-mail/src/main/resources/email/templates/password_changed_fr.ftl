<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.BaseTemplateDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Bonjour ${name},</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Le mot de passe de votre compte GBIF <b>${name}</b> a été modifié. 
    Si vous ne l'avez pas changé, veuillez contacter <a href="mailto:helpdesk@gbif.org" style="margin: 0;padding: 0;line-height: 1.65;color: #4ba2ce;text-decoration: none;font-weight: bold;">helpdesk@gbif.org</a> immédiatement.
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>Le Secrétariat du GBIF</em>
</p>

<#include "footer.ftl">
