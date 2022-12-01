<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.PipelinesIdentifierIssueDataModel" -->
<#include "header.ftl">

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Identifier validation failed for the dataset <b>${datasetName}</b>, crawler attempt: <b>${attempt}</b> cause:
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
  <em>${message}</em>
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
	You can skip/fix identifier validation using <a href="${registryUrl}dataset/${datasetKey}/ingestion-history" style="color: #4ba2ce;text-decoration: none;">Registry UI</a>
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>The GBIF Informatics</em>
</p>

<#include "footer.ftl">
