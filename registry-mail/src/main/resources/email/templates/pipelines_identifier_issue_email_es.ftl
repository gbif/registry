<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.PipelinesIdentifierIssueDataModel" -->
<#include "header.ftl">

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    La validación del identificador falló para el conjunto de datos <b>${datasetName}</b>, intento de rastreo: <b>${attempt}</b> causa:
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
  <em>${message}</em>
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
	Puedes omitir/corregir la validación de los identificadores mediante <a href="${registryUrl}dataset/${datasetKey}/ingestion-history" style="color: #4ba2ce;text-decoration: none;">Registry UI</a>
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>La Informática de GBIF</em>
</p>

<#include "footer.ftl">
