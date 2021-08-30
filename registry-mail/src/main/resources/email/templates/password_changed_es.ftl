<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.BaseTemplateDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Hola ${name},</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    La contraseña de su cuenta de GBIF <b>${name}</b> ha cambiado. 
    If you didn't change it, please contact <a href="mailto:helpdesk@gbif.org" style="margin: 0;padding: 0;line-height: 1.65;color: #4ba2ce;text-decoration: none;font-weight: bold;">helpdesk@gbif.org</a> immediately.
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>La Secretaría de GBIF</em>
</p>

<#include "footer.ftl">
