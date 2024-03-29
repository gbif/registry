<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountDeleteDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">尊敬的 ${name}，</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    您的 GBIF.org 帐户
    <b>${name}</b>
    已被删除，您的个人信息已被删除。
</p>

<#if downloadUrls?size != 0>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">事件下载元数据未被删除。  您仍然可以引用您创建的任何下载：</p>
<ul style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <#list downloadUrls as downloadUrl>
    <li style="margin: 0 0 0 20px;padding: 0;line-height: 1.65;"><a href="${downloadUrl}" style="color: #4ba2ce;text-decoration: none;">${downloadUrl}</a></li>
    </#list>
</ul>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">如果您在出版物中使用从GBIF.org下载的内容，请通知 <a href="mailto:communications@gbif.org" style="color: #4ba2ce;text-decoration: none;">communications@gbif.org</a>。</p>
</#if>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">如果您希望以后从 GBIF.org 下载数据，您将需要创建一个新帐户。</p>


<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>
        代表GBIF网络感谢您，
        <br>
        GBIF秘书处
    </em>
</p>

<#include "footer.ftl">
