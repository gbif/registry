<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountDeleteDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">${name} 様</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    あなたの GBIF.org アカウント
    <b>${name}</b>
    が削除され、個人情報が消去されました。
</p>

<#if downloadUrls?size != 0>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">オカレンスのダウンロードメタデータは削除されません。  あなたが作成したダウンロードはどれも引用可能です：</p>
<ul style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <#list downloadUrls as downloadUrl>
    <li style="margin: 0 0 0 20px;padding: 0;line-height: 1.65;"><a href="${downloadUrl}" style="color: #4ba2ce;text-decoration: none;">${downloadUrl}</a></li>
    </#list>
</ul>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">出版物でGBIF.orgのダウンロードをご利用になる場合は、 <a href="mailto:communications@gbif.org" style="color: #4ba2ce;text-decoration: none;">communications@gbif.org</a> までご連絡ください。</p>
</#if>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">今後GBIF.orgからデータをダウンロードしたい場合は、新しいアカウントを作成する必要があります。</p>


<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>
        よろしくお願いいたします。
        <br>
        GBIF事務局
    </em>
</p>

<#include "footer.ftl">
