<#-- @ftlvariable name="" type="org.gbif.registry.ws.surety.OrganizationTemplateDataModel" -->
Hello ${name},

A new organization requires your confirmation:
${newOrganisation.title}

<#list newOrganisation.address! as addr>
${addr}
</#list>
${newOrganisation.city!}
${newOrganisation.province!}
${newOrganisation.postalCode!}
${newOrganisation.country.title!}

Description:
${newOrganisation.description!}

Please confirm by clicking the following link: ${url}

Kind regards,
The GBIF Team

