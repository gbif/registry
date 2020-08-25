<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.OrganizationTemplateDataModel" -->
<p>Уважаемый/ая ${name},</p>

<#if !hasReachableNodeManager()><p><i>Предупреждение: ${endorsingNode.title} не имеет менеджера с адресом электронной почты. Таким образом, письмо было отправлено только в службу поддержки.</i></p></#if>

<p>Новая организация запрашивает подтверждение у <b>${endorsingNode.title}.</b><p>

<p><b>${organization.title!}</b></p>

<p>Чтобы помочь вам решить, подтверждать ли данную организацию, ознакомьтесь с нашими <a href="https://www.gbif.org/endorsement-guidelines"> рекомендациями по подтверждению </a> и информацией, приведенной ниже. Если вы готовы подтвердить, вы можете пройти дальше, используя ссылку внизу этого письма. Если процесс занимает больше времени, если вы не можете его взять на себя в настоящее время или если вы решите не подтверждать организацию, сообщите нам об этом по электронной почте helpdesk@gbif.org.</p>

<p>Помните о наших обязательствах перед потенциальными публикаторами и ответьте на этот запрос о подтверждении как можно скорее и в течение максимум 30 дней.</p>

<p>
    <b>${organization.title!}</b>
<ul style="list-style: none;">
    <#list organization.address! as addr>
        <li>${addr}</li>
    </#list>
    <li>${organization.city!}</li>
    <li>${organization.province!}</li>
    <li>${organization.postalCode!}</li>
    <#if (organization.country??) >
        <li>${organization.country.title!}</li>
    </#if>
    <#list organization.homepage! as homepage>
        <li><a href="${homepage}">${homepage}</a></li>
    </#list>
</ul>
</p>

<p>
    Описание:
    ${organization.description!}
</p>

<#if organization.contacts?has_content>
    <p>
    Контакты:
    <#list organization.contacts! as contact>
        <ul style="list-style: none;">
            <#if contact.firstName?has_content><li>${contact.computeCompleteName()}</li></#if>
            <#if contact.type?has_content><li>${contact.type}</li></#if>
            <#if contact.position?has_content><li>${contact.position?join(", ")}</li></#if>
            <#if contact.description?has_content><li>${contact.description}</li></#if>
            <#if contact.organization?has_content><li>${contact.organization}</li></#if>

            <#if contact.email?has_content><li>${contact.email?join(", ")}</li></#if>
            <#if contact.phone?has_content><li>${contact.phone?join(", ")}</li></#if>

            <#list contact.address! as addr>
                <li>${addr}</li>
            </#list>
            <#if contact.city?has_content><li>${contact.city}</li></#if>
            <#if contact.province?has_content><li>${contact.province}</li></#if>
            <#if contact.postalCode?has_content><li>${contact.postalCode}</li></#if>
            <#if contact.country?has_content><li>${contact.country}</li></#if>
        </ul>
    </#list>
    </p>
</#if>

<#if organization.comments?has_content>
    <p>
        Дополнительная информация:
    <ul style="list-style: none;">
        <#list organization.comments! as comment>
        <#--comments can be in Markdown but we only support endline for now-->
            <#list comment.content?split("\n") as subcomment>
                <li>${subcomment}</li>
            </#list>
        </#list>
    </ul>
    </p>
</#if>

<p>Пройдите по ссылке чтобы подтвердить <b>${organization.title!}</b>: <a href="${url}">ПОДТВЕРДИТЬ</a>.</p>
<p>Если вы не хотите подтверждать данную организацию, отправьте электронное письмо по адресу <a href="mailto:helpdesk@gbif.org">helpdesk@gbif.org</a> включая копию этого сообщения и краткое объяснение.</p>

<p>С уважением,</p>
<p>GBIF секретариат</p>
