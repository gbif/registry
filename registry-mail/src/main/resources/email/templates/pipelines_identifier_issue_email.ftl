<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.PipelinesIdentifierIssueDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Dataset ${datasetName} GBIF identifiers validation failed</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Dataset ${datasetName} attempt ${attempt} identifier validation failed due to:

    ${message}

    You can skip/fix identifier validation using <a href="${registryUrl}dataset/${datasetKey}/ingestion-history" style="color: #4ba2ce;text-decoration: none;">Registry UI</a>
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>The GBIF Informatics</em>
</p>

<#include "footer.ftl">
