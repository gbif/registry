<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountDeleteDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">您好 ${name}，</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    您的 GBIF.org 帳號
    <b>${name}</b>
    已刪除，您的個人資訊也已被清除。
</p>

<#if downloadUrls?size != 0>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">出現紀錄下載檔案未刪除，  您仍可持續引用您所建立的下載文件：</p>
<ul style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <#list downloadUrls as downloadUrl>
    <li style="margin: 0 0 0 20px;padding: 0;line-height: 1.65;"><a href="${downloadUrl}" style="color: #4ba2ce;text-decoration: none;">${downloadUrl}</a></li>
    </#list>
</ul>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">若您在出版品中運用 GBIF.org 的下載文件，請通知 <a href="mailto:communications@gbif.org" style="color: #4ba2ce;text-decoration: none;">communications@gbif.org</a>。</p>
</#if>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">若您希望未來在 GBIF.org 下載資料，您將需要建立一個新帳號。</p>


<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>
        謝謝您，謹代表 GBIF 網絡
        <br>
        GBIF 秘書處
    </em>
</p>

<#include "footer.ftl">
