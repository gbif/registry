<#-- @ftlvariable name="" type="org.gbif.registry.ws.surety.OrganizationTemplateDataModel" -->
<p>Dear ${name},</p>

<#if !hasReachableNodeManager()><p><i>Warning: ${endorsingNode.title} doesn't have a NodeManager with email address. Therefore, the email was only sent to Helpdesk.</i></p></#if>

<p>A new organization is requesting to be endorsed by <b>${endorsingNode.title}.</b><p>

<p>
    <b>${organisation.title!}</b>
<ul style="list-style: none;">
  <#list organisation.address! as addr>
  <li>${addr}</li>
  </#list>
    <li>${organisation.city!}</li>
    <li>${organisation.province!}</li>
    <li>${organisation.postalCode!}</li>
    <li>${organisation.country.title!}</li>
</ul>
</p>

<p>
Description:
${organisation.description!}
</p>

<#if organisation.contacts?has_content>
<p>
Contacts:
  <#list organisation.contacts! as contact>
    <ul style="list-style: none;">
    <#if contact.firstName?has_content><li>${contact.computeCompleteName()}</li></#if>
    <#if contact.type?has_content><li>${contact.type}</li></#if>
    <#if contact.position?has_content><li>${contact.position?join(", ")}</li></#if>
    <#if contact.description?has_content><li>${contact.description}</li></#if>
    <#if contact.organization?has_content><li>${contact.organization}</li></#if>

    <#if contact.email?has_content><li>${contact.email?join(", ")}</li></#if>
    <#if contact.phone?has_content><li>${contact.phone?join(", ")}</li></#if>

    <#list contact.address! as addr>
    <li>${addr}</li>
    </#list>
    <#if contact.city?has_content><li>${contact.city}</li></#if>
    <#if contact.province?has_content><li>${contact.province}</li></#if>
    <#if contact.postalCode?has_content><li>${contact.postalCode}</li></#if>
    <#if contact.country?has_content><li>${contact.country}</li></#if>
    </ul>
  </#list>
</p>
</#if>

<#if organisation.comments?has_content>
<p>
Additional information:
<ul style="list-style: none;">
<#list organisation.comments! as comment>
<#--comments can be in Markdown but we only support endline for now-->
  <#list comment.content?split("\n") as subcomment>
    <li>${subcomment}</li>
  </#list>
</#list>
</ul>
</p>
</#if>

<#if !hasReachableNodeManager()>
  <p>If you wish to endorse <b>${organisation.title!}</b> please click the following link: <a href="${url}">Endorse ${organisation.title!}</a></p>
  <p>If you do not want to endorse <b>${organisation.title!}</b> please send an email to <a href="mailto:helpdesk@gbif.org">helpdesk@gbif.org</a> including a copy of this message and a short explanation.</p>
</#if>

<p>Kind regards,</p>
<p>The GBIF Secretariat</p>

