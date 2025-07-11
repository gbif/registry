<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.DescriptorChangeSuggestionDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Descriptor Suggestion Discarded!</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Descriptor change suggestion discarded. See the details on <a href="${changeSuggestionUrl!""}" style="color: #4ba2ce;text-decoration: none;">${changeSuggestionUrl!""}</a>.</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
  Details of the descriptor suggestion:
  <ul>
    <li>Suggestion type: ${suggestionType!""}</li>
    <li>Collection: ${collectionName!""}</li>
    <li>Collection country: ${collectionCountry!""}</li>
    <li>Title: ${title!""}</li>
    <li>Description: ${description!""}</li>
    <li>Format: ${format!""}</li>
    <li>Proposer email: ${proposerEmail!""}</li>
  </ul>
</p>

<#if comments?? && comments?size gt 0>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
  Comments:
  <ul>
    <#list comments as comment>
    <li>${comment}</li>
    </#list>
  </ul>
</p>
</#if>

<#if tags?? && tags?size gt 0>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
  Tags: ${tags?join(", ")}
</p>
</#if>

<#include "footer.ftl"> 