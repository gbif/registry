<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountEmailChangedTemplateDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">您好 ${name}，</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    您的 GBIF 帳號的電子郵件地址
    <b>${name}</b>
    已變更為
    <a href="mailto:${newEmail}" style="margin: 0;padding: 0;line-height: 1.65;color: #4ba2ce;text-decoration: none;font-weight: bold;">${newEmail}</a>。
    若您未做過此變更，請立即聯絡 <a href="mailto:helpdesk@gbif.org" style="margin: 0;padding: 0;line-height: 1.65;color: #4ba2ce;text-decoration: none;font-weight: bold;">helpdesk@gbif.org</a>。
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>GBIF 秘書處</em>
</p>

<#include "footer.ftl">
