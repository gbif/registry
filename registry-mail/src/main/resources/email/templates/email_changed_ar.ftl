<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountEmailChangedTemplateDataModel" -->
<#include "header.ftl">

<h4 style="margin: 0 0 20px;padding: 0;font-size: 20px;line-height: 1.25;">Hello ${name},</h4>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    The email of your GBIF account
    <b>${name}</b>
    was changed to
    <a href="mailto:${newEmail}" style="margin: 0;padding: 0;line-height: 1.65;color: #509E2F;text-decoration: none;font-weight: bold;">${newEmail}</a>.
    If you didn't change it, please contact
    <a href="mailto:helpdesk@gbif.org" style="margin: 0;padding: 0;line-height: 1.65;color: #509E2F;text-decoration: none;font-weight: bold;">helpdesk@gbif.org</a>
    immediately.
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>The GBIF Secretariat</em>
</p>

<#include "footer.ftl">
