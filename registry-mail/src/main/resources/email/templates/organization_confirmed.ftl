<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.OrganizationTemplateDataModel" -->
<h4 style="margin: 0;padding: 0;font-size: 20px;line-height: 1.25;margin-bottom: 20px;">Dear ${name},</h4>

<p style="margin: 0;padding: 0;line-height: 1.65;font-weight: normal;margin-bottom: 20px;">
    The following message is to inform you that
    <b style="margin: 0;padding: 0;font-size: 100%;line-height: 1.65;">${endorsingNode.title}</b>
    has endorsed the organization
    <a href="${organizationUrl}" style="margin: 0;padding: 0;font-size: 100%;line-height: 1.65;color: #509E2F;text-decoration: none;font-weight: bold;">${organization.title}</a>
</p>

<p style="margin: 0;padding: 0;line-height: 1.65;font-weight: normal;margin-bottom: 20px;">
    <em style="margin: 0;padding: 0;font-size: 100%;line-height: 1.65;">Kind regards,</em>
    <br>
    <em style="margin: 0;padding: 0;font-size: 100%;line-height: 1.65;">The GBIF Secretariat</em>
</p>
