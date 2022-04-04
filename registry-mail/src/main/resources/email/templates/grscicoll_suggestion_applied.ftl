<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.GrscicollChangeSuggestionDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Change Suggestion Applied!</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">GRSciColl change suggestion applied. See the comments on <a href="${changeSuggestionUrl}" style="color: #4ba2ce;text-decoration: none;">${changeSuggestionUrl}</a>.</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
  Details of the change suggestion:
  <ul>
    <li>Suggestion type: ${suggestionType}</li>
    <li>Entity type: ${entityType}</li>
    <li>Entity name: ${entityName}</li>
    <li>Entity country: ${entityCountry}</li>
  </ul>
</p>

<#include "footer.ftl">
