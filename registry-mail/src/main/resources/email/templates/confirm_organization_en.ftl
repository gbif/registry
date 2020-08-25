<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.OrganizationTemplateDataModel" -->
<p>Dear ${name},</p>

<#if !hasReachableNodeManager()><p><i>Warning: ${endorsingNode.title} doesn't have a NodeManager with email address. Therefore, the email was only sent to Helpdesk.</i></p></#if>

<p>A new organization is requesting to be endorsed by <b>${endorsingNode.title}.</b><p>

<p><b>${organization.title!}</b></p>

<p>To help you decide whether to endorse this organization, consult our <a href="https://www.gbif.org/endorsement-guidelines">endorsement guidelines</a> and the information included below.  If you are ready to endorse, you can go straight ahead, using the link at the bottom of this email.  If the process takes longer, if you cannot take it on at this time, or if you decide against endorsing the organization, please notify us by email to helpdesk@gbif.org.</p>

<p>Please remember our commitment to prospective publishers, and respond to this endorsement request as soon as possible, and within a maximum of 30 days.</p>

<p>
    <b>${organization.title!}</b>
    <ul style="list-style: none;">
    <#list organization.address! as addr>
      <li>${addr}</li>
    </#list>
    <li>${organization.city!}</li>
    <li>${organization.province!}</li>
    <li>${organization.postalCode!}</li>
    <#if (organization.country??) >
    <li>${organization.country.title!}</li>
    </#if>
    <#list organization.homepage! as homepage>
      <li><a href="${homepage}">${homepage}</a></li>
    </#list>
  </ul>
</p>

<p>
Description:
${organization.description!}
</p>

<#if organization.contacts?has_content>
<p>
Contacts:
  <#list organization.contacts! as contact>
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

<#if organization.comments?has_content>
<p>
Additional information:
<ul style="list-style: none;">
<#list organization.comments! as comment>
<#--comments can be in Markdown but we only support endline for now-->
  <#list comment.content?split("\n") as subcomment>
    <li>${subcomment}</li>
  </#list>
</#list>
</ul>
</p>
</#if>

<p>Click this link to endorse <b>${organization.title!}</b>: <a href="${url}">ENDORSE</a>.</p>
<p>If you do NOT want to endorse this organization, please send an email to <a href="mailto:helpdesk@gbif.org">helpdesk@gbif.org</a> including a copy of this message and a short explanation.</p>

<p>Kind regards,</p>
<p>The GBIF Secretariat</p>
