<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.OrganizationPasswordReminderTemplateDataModel" -->
<#include "header.ftl">

<h4 style="margin: 0;padding: 0;font-size: 20px;line-height: 1.25;margin-bottom: 20px;">Dear ${contact.firstName},</h4>

<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">
    You, or someone else has requested the password for the organisation
    <b style="margin: 0;padding: 0;line-height: 1.65;">${organization.title}</b>
    to be sent to your e-mail address
    <a href="mailto:${email}" style="margin: 0;padding: 0;line-height: 1.65;color: #509E2F;text-decoration: none;font-weight: bold;">${email}</a>
</p>

<h5 style="margin: 0;padding: 0;line-height: 1.25;margin-bottom: 20px;">The information requested is</h5>
<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">
    <ul style="list-style: none;margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">
        <li style="margin: 0;padding: 0;line-height: 1.65;">
            Username: ${organization.key}
        </li>
        <li style="margin: 0;padding: 0;line-height: 1.65;">
            Password: ${organization.password}</li>
    </ul>
</p>

<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">If you did not request this information, please disregard this message</p>

<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">
    <em style="margin: 0;padding: 0;line-height: 1.65;">Kind regards,</em>
    <br>
    <em style="margin: 0;padding: 0;line-height: 1.65;">The GBIF Secretariat</em>
</p>

<#include "footer.ftl">
