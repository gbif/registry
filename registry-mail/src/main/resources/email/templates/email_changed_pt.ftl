<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountEmailChangedTemplateDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Olá ${name},</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    O e-mail da sua conta GBIF
    <b>${name}</b>
    foi alterado para
    <a href="mailto:${newEmail}" style="margin: 0;padding: 0;line-height: 1.65;color: #4ba2ce;text-decoration: none;font-weight: bold;">${newEmail}</a>.
    Se você não alterou, por favor contate
    <a href="mailto:helpdesk@gbif.org" style="margin: 0;padding: 0;line-height: 1.65;color: #4ba2ce;text-decoration: none;font-weight: bold;">helpdesk@gbif.org</a>
    imediatamente.
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>Secretaria do GBIF</em>
</p>

<#include "footer.ftl">
