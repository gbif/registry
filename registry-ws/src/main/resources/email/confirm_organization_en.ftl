<#-- @ftlvariable name="" type="org.gbif.registry.ws.surety.OrganizationTemplateDataModel" -->
Hello ${name},

A new organization requires your confirmation:
${organisation.title!}

<#list organisation.address! as addr>
${addr}
</#list>
${organisation.city!}
${organisation.province!}
${organisation.postalCode!}
${organisation.country.title!}

Description:
${organisation.description!}

Please confirm by clicking the following link: ${url}

Kind regards,
The GBIF Team

