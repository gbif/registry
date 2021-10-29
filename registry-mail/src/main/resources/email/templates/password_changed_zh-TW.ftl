<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.BaseTemplateDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">您好 ${name}，</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    您的 GBIF 帳號 <b>${name}</b> 密碼已變更。 
    若您未做過此變更，請立即與 <a href="mailto:helpdesk@gbif.org" style="margin: 0;padding: 0;line-height: 1.65;color: #4ba2ce;text-decoration: none;font-weight: bold;">helpdesk@gbif.org</a> 聯絡。
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>GBIF 秘書處</em>
</p>

<#include "footer.ftl">
