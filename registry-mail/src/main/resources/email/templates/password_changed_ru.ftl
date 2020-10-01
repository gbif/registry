<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.BaseTemplateDataModel" -->
<#include "header.ftl">

<h4 style="margin: 0 0 20px;padding: 0;font-size: 20px;line-height: 1.25;">Уважаемый/ая ${name},</h4>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Пароль для вашей учетной записи GBIF
    <b>${name}</b>
    был изменен. Если вы не меняли его, немедленно свяжитесь с <a href="mailto:helpdesk@gbif.org" style="margin: 0;padding: 0;line-height: 1.65;color: #509E2F;text-decoration: none;font-weight: bold;">helpdesk@gbif.org</a>.
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>GBIF Секретариат</em>
</p>

<#include "footer.ftl">
