<#-- @ftlvariable name="" type="org.gbif.registry.dataprivacy.email.DataPrivacyNotificationTemplateDataModel" -->
<p>Dear user,</p>


<p>Your data is being used<p>

<p>Please find more information <a href="${url}">here</a></p>

<p>Sample urls
  <ul>
    <#list sampleUrls! as sample>
      <li>${sample}</li>
    </#list>
  </ul>
</p>


<p>Kind regards,</p>
<p>The GBIF Secretariat</p>

