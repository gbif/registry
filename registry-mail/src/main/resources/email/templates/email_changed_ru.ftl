<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountEmailChangedTemplateDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Уважаемый пользователь ${name},</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Адрес электронной почты вашего аккаунта GBIF <b>${name}</b> был изменен на <a href="mailto:${newEmail}" style="margin: 0;padding: 0;line-height: 1.65;color: #4ba2ce;text-decoration: none;font-weight: bold;">${newEmail}</a>.
    Если вы не меняли его, немедленно свяжитесь с <a href="mailto:helpdesk@gbif.org" style="margin: 0;padding: 0;line-height: 1.65;color: #4ba2ce;text-decoration: none;font-weight: bold;">helpdesk@gbif.org</a>.
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>GBIF Секретариат</em>
</p>

<#include "footer.ftl">
