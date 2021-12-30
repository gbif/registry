<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountDeleteDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Шановний ${name},</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Ваш обліковий запис GBIF.org
    <b>${name}</b>
    був видалений, і вашу особисту інформацію було стерто.
</p>

<#if downloadUrls?size != 0>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Метадані завантажених знахідок збережено.  Ви все ще можете цитувати будь-яке з ваших завантажень:</p>
<ul style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <#list downloadUrls as downloadUrl>
    <li style="margin: 0 0 0 20px;padding: 0;line-height: 1.65;"><a href="${downloadUrl}" style="color: #4ba2ce;text-decoration: none;">${downloadUrl}</a></li>
    </#list>
</ul>
<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Якщо ви використовуєте завантаження з GBIF.org в публікації, будь ласка, повідомте <a href="mailto:communications@gbif.org" style="color: #4ba2ce;text-decoration: none;">communications@gbif.org</a>.</p>
</#if>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Якщо ви захочете завантажити дані з GBIF.org в майбутньому, вам потрібно створити новий обліковий запис.</p>


<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>
        Дякуємо, від імені мережі GBIF,
        <br>
        Секретаріат GBIF
    </em>
</p>

<#include "footer.ftl">
