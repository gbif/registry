<#-- @ftlvariable name="" type="org.gbif.registry.ws.surety.OrganizationTemplateDataModel" -->
Dear ${name},

<#if !hasReachableNodeManager()>Warning: ${endorsingNode.title} doesn't have a NodeManager with email address. Therefore, the email was only sent to Helpdesk.</#if>
A new organization is requesting to be endorsed by ${endorsingNode.title}:

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

<#if organisation.contacts?has_content>
Contacts:
  <#list organisation.contacts! as contact>

    <#if contact.firstName?has_content>${contact.computeCompleteName()}</#if>
    <#if contact.type?has_content>${contact.type}</#if>
    <#if contact.position?has_content>${contact.position?join(", ")}</#if>
    <#if contact.description?has_content>${contact.description}</#if>
    <#if contact.organization?has_content>${contact.organization}</#if>

    <#if contact.email?has_content>${contact.email?join(", ")}</#if>
    <#if contact.phone?has_content>${contact.phone?join(", ")}</#if>

    <#list contact.address! as addr>
    ${addr}
    </#list>
    <#if contact.city?has_content>${contact.city}</#if>
    <#if contact.province?has_content>${contact.province}</#if>
    <#if contact.postalCode?has_content>${contact.postalCode}</#if>
    <#if contact.country?has_content>${contact.country}</#if>
  </#list>
</#if>

Please confirm by clicking the following link: ${url}

Kind regards,
The GBIF Team

