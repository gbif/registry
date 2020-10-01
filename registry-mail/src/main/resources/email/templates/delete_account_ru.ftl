<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountDeleteDataModel" -->
<#include "header.ftl">

<h4 style="margin: 0;padding: 0;font-size: 20px;line-height: 1.25;margin-bottom: 20px;">Уважаемый/ая ${name},</h4>

<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">
    Ваш аккаунт
    <b style="margin: 0;padding: 0;line-height: 1.65;">${name}</b>
    на GBIF.org был удален, а ваша личная информация стерта.
</p>

<#if downloadUrls?size != 0>
<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">Метаданные загрузки находок не были удалены. Вы по-прежнему можете цитировать любые созданные вами загрузки:</p>
<ul style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">
    <#list downloadUrls as downloadUrl>
    <li style="margin: 0 0 0 20px;padding: 0;line-height: 1.65;"><a href="${downloadUrl}" style="margin: 0;padding: 0;line-height: 1.65;color: #509E2F;text-decoration: none;">${downloadUrl}</a></li>
    </#list>
</ul>
<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">Если вы все же цитируете загрузку GBIF.org, напишите по адресу <a href="mailto:communications@gbif.org" style="margin: 0;padding: 0;line-height: 1.65;color: #509E2F;text-decoration: none;">communications@gbif.org</a>, указав DOI.</p>
</#if>

<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">Если вы захотите загрузить данные с GBIF.org в будущем, вам потребуется создать новую учетную запись.</p>


<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">
    <em style="margin: 0;padding: 0;line-height: 1.65;">
        Спасибо и с наилучшими пожеланиями,
        <br style="margin: 0;padding: 0;line-height: 1.65;">
        GBIF Секретариат
    </em>
</p>

<#include "footer.ftl">
