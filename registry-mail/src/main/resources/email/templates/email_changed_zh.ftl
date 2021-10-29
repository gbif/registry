<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountEmailChangedTemplateDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">${name}，您好！</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    您的 GBIF 帐户
    <b>${name}</b>
    的电子邮件已更改为
    <a href="mailto:${newEmail}" style="margin: 0;padding: 0;line-height: 1.65;color: #4ba2ce;text-decoration: none;font-weight: bold;">${newEmail}</a>。
    如果您没有更改，请立即联系
    <a href="mailto:helpdesk@gbif.org" style="margin: 0;padding: 0;line-height: 1.65;color: #4ba2ce;text-decoration: none;font-weight: bold;">helpdesk@gbif.org</a>。
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>GBIF 秘书处</em>
</p>

<#include "footer.ftl">
