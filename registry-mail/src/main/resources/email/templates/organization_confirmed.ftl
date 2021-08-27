<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.OrganizationTemplateDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Dear ${name},</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    The following message is to inform you that
    <b>${endorsingNode.title}</b>
    has endorsed the organization
    <a href="${organizationUrl}" style="margin: 0;padding: 0;line-height: 1.65;color: #4ba2ce;text-decoration: none;font-weight: bold;">${organization.title}</a>
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>Kind regards,</em>
    <br>
    <em>The GBIF Secretariat</em>
</p>

<#include "footer.ftl">
