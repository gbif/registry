<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.OrganizationPasswordReminderTemplateDataModel" -->
<p>Dear ${contact.firstName}:</p>

<p>You, or someone else has requested the password for the organisation '${organization.title}' to be sent to your
    e-mail address (${email})</p>

<p>The information requested is:</p>

<p>Username: ${organization.key}</p>
<p>Password: ${organization.password}</p>

<p>If you did not request this information, please disregard this message</p>

<p>GBIF (Global Biodiversity Information Facility)</p>
<p><a href="${url}">${url}</a></p>
<p>
    <#list ccEmail as item>
        ${item}
    </#list>
</p>
