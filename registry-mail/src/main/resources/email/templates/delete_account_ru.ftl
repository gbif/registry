<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountDeleteDataModel" -->
<#include "header.ftl">

<h4 style="margin: 0 0 20px;padding: 0;font-size: 20px;line-height: 1.25;">Уважаемый/ая ${name},</h4>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Ваш аккаунт
    <b>${name}</b>
    на GBIF.org был удален, а ваша личная информация стерта.
</p>

<#if downloadUrls?size != 0>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Метаданные загрузки находок не были удалены.  Вы по-прежнему можете цитировать любые созданные вами загрузки:</p>
<ul style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <#list downloadUrls as downloadUrl>
    <li style="margin: 0 0 0 20px;padding: 0;line-height: 1.65;"><a href="${downloadUrl}" style="color: #4ba2ce;text-decoration: none;">${downloadUrl}</a></li>
    </#list>
</ul>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">If you use a GBIF.org download in a publication, please notify <a href="mailto:communications@gbif.org" style="color: #4ba2ce;text-decoration: none;">communications@gbif.org</a>.</p>
</#if>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Если вы захотите загрузить данные с GBIF.org в будущем, вам потребуется создать новую учетную запись.</p>


<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>
        Спасибо и с наилучшими пожеланиями,
        <br>
        GBIF Секретариат
    </em>
</p>

<#include "footer.ftl">
