<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.OrganizationPasswordReminderTemplateDataModel" -->
<p>Уважаемый/ая ${contact.firstName}:</p>

<p>Вы или кто-то другой запросил пароль для организации "${organization.title}", для отправки на ваш
    адрес электронной почты (${email})</p>

<p>Запрошенная информация:</p>

<p>Логин: ${organization.key}</p>
<p>Пароль: ${organization.password}</p>

<p>Если вы не запрашивали эту информацию, не обращайте внимания на это сообщение.</p>

<p>GBIF (Global Biodiversity Information Facility)</p>
<p><a href="${url}">${url}</a></p>
<p>
    <#list ccEmail as item>
        ${item}
    </#list>
</p>
