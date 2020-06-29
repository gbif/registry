<p>Dear ${name},</p>

<p>The following message is to inform you that account ${name} has been deleted.</p>

Your occurrence downloads will still be available:
<#list downloadUrls as downloadUrl>
    <li><a href="${downloadUrl}">${downloadUrl}</a></li>
</#list>

<p>Kind regards,</p>
<p>The GBIF Secretariat</p>
