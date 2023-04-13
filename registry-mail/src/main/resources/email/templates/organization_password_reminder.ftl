<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.OrganizationPasswordReminderTemplateDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Dear <#if (contact.firstName)??>${contact.firstName}<#else>user</#if>,</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    You, or someone else has requested the shared token for the organisation
    <b>${organization.title}</b>
    to be sent to your e-mail address
    <a href="mailto:${email}" style="margin: 0;padding: 0;line-height: 1.65;color: #4ba2ce;text-decoration: none;font-weight: bold;">${email}</a>
</p>

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">The information requested is</h5>

<ul style="list-style: none;margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <li style="margin: 0;padding: 0;line-height: 1.65;">
        Username: ${organization.key}
    </li>
    <li style="margin: 0;padding: 0;line-height: 1.65;">
        Token: ${organization.password}
    </li>
</ul>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">If you did not request this information, please disregard this message</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>Kind regards,</em>
    <br>
    <em>The GBIF Secretariat</em>
</p>

<#include "footer.ftl">
