<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountEmailChangedTemplateDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Hola ${name},</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    El correo electrónico de su cuenta de GBIF
    <b>${name}</b>
    fue cambiado a
    <a href="mailto:${newEmail}" style="margin: 0;padding: 0;line-height: 1.65;color: #4ba2ce;text-decoration: none;font-weight: bold;">${newEmail}</a>.
    Si no ha solicitado este cambio, por favor póngase en contacto con
    <a href="mailto:helpdesk@gbif.org" style="margin: 0;padding: 0;line-height: 1.65;color: #4ba2ce;text-decoration: none;font-weight: bold;">helpdesk@gbif.org</a>
    inmediatamente.
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>Secretaría de GBIF</em>
</p>

<#include "footer.ftl">
