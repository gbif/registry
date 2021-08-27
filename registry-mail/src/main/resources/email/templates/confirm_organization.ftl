<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.OrganizationTemplateDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Dear ${name},</h5>

<#if !hasReachableNodeManager()>
    <p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
        <i>Warning: <b>${endorsingNode.title}</b> doesn't have a registered GBIF Participant Node Manager with email address. Therefore, the email was only sent to Helpdesk.</i>
    </p>
</#if>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    A new organization
    <b>${organization.title!}</b>
    is requesting to be endorsed by
    <b>${endorsingNode.title}.</b>
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    To help you decide whether to endorse this organization, consult our
    <a href="https://www.gbif.org/endorsement-guidelines" style="color: #509E2F;text-decoration: none;">endorsement guidelines</a>
    and the information included below.
    If you are ready to endorse, you can go straight ahead, using the button at the bottom of this email.
    If the process takes longer, if you cannot take it on at this time, or if you decide against endorsing the organization,
    please notify us by email to
    <a href="mailto:helpdesk@gbif.org" style="color: #509E2F;text-decoration: none;">helpdesk@gbif.org</a>.
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Please remember our commitment to prospective publishers,
    and respond to this endorsement request as soon as possible, and within a maximum of 30 days.
</p>

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">${organization.title!}</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <ul style="list-style: none;margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <#list organization.address! as addr>
        <li style="margin: 0;padding: 0;line-height: 1.65;">${addr}</li>
    </#list>
        <li style="margin: 0;padding: 0;line-height: 1.65;">${organization.city!}</li>
        <li style="margin: 0;padding: 0;line-height: 1.65;">${organization.province!}</li>
        <li style="margin: 0;padding: 0;line-height: 1.65;">${organization.postalCode!}</li>
    <#if (organization.country??) >
        <li style="margin: 0;padding: 0;line-height: 1.65;">${organization.country.title!}</li>
    </#if>
    <#list organization.homepage! as homepage>
        <li style="margin: 0;padding: 0;line-height: 1.65;"><a href="${homepage}" style="color: #509E2F;text-decoration: none;">${homepage}</a></li>
    </#list>
    </ul>
</p>

<#if organization.description?has_content>
<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Description</h5>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    ${organization.description!}
</p>
</#if>

<#if organization.contacts?has_content>
<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Contacts</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <#list organization.contacts! as contact>
<ul style="list-style: none;margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <#if contact.firstName?has_content><li style="margin: 0;padding: 0;line-height: 1.65;">${contact.computeCompleteName()}</li></#if>
    <#if contact.type?has_content><li style="margin: 0;padding: 0;line-height: 1.65;">${contact.type}</li></#if>
    <#if contact.position?has_content><li style="margin: 0;padding: 0;line-height: 1.65;">${contact.position?join(", ")}</li></#if>
    <#if contact.description?has_content><li style="margin: 0;padding: 0;line-height: 1.65;">${contact.description}</li></#if>
    <#if contact.organization?has_content><li style="margin: 0;padding: 0;line-height: 1.65;">${contact.organization}</li></#if>

    <#if contact.email?has_content><li style="margin: 0;padding: 0;line-height: 1.65;">${contact.email?join(", ")}</li></#if>
    <#if contact.phone?has_content><li style="margin: 0;padding: 0;line-height: 1.65;">${contact.phone?join(", ")}</li></#if>

    <#list contact.address! as addr>
    <#if addr?has_content><li style="margin: 0;padding: 0;line-height: 1.65;">${addr}</li></#if>
    </#list>
    <#if contact.city?has_content><li style="margin: 0;padding: 0;line-height: 1.65;">${contact.city}</li></#if>
    <#if contact.province?has_content><li style="margin: 0;padding: 0;line-height: 1.65;">${contact.province}</li></#if>
    <#if contact.postalCode?has_content><li style="margin: 0;padding: 0;line-height: 1.65;">${contact.postalCode}</li></#if>
    <#if contact.country?has_content><li style="margin: 0;padding: 0;line-height: 1.65;">${contact.country}</li></#if>
</ul>
</#list>
</p>
</#if>

<#if organization.comments?has_content>
<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Additional information</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <ul style="list-style: none;margin: 0 0 20px;padding: 0;line-height: 1.65;">
        <#list organization.comments! as comment>
        <#--comments can be in Markdown but we only support endline for now-->
        <#list comment.content?split("\n") as subcomment>
            <#if subcomment?has_content><li style="margin: 0;padding: 0;line-height: 1.65;">${subcomment}</li></#if>
        </#list>
        </#list>
    </ul>
</p>
</#if>


<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Endorsement</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Click this button to endorse <b>${organization.title!}</b>:</p>

<table style="margin: 0;padding: 0;line-height: 1.65;border-collapse: collapse;width: 100% !important;">
    <tr style="margin: 0;padding: 0;line-height: 1.65;">
        <td align="center" style="margin: 0;padding: 0;line-height: 1.65;">
            <p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
                <a href="${url}" class="button" style="margin: 0;padding: 0;line-height: 1.65;color: white;text-decoration: none;display: inline-block;background: #509E2F;border: solid #509E2F;border-width: 10px 20px 8px;font-weight: bold;border-radius: 4px;">Endorse</a>
            </p>
        </td>
    </tr>
</table>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    You can also copy the following URL and paste into your browser: <a href="${url}" style="color: #509E2F;text-decoration: none;">${url}</a>
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    If you do NOT want to endorse this organization, please send an email to
    <a href="mailto:helpdesk@gbif.org" style="color: #509E2F;text-decoration: none;">helpdesk@gbif.org</a> including a copy
    of this message and a short explanation.
</p>


<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>
        Kind regards,
        <br>
        The GBIF Secretariat
    </em>
</p>

<#include "footer.ftl">
