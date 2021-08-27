<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountChangeEmailTemplateDataModel" -->
<#include "header.ftl">

<h4 style="margin: 0 0 20px;padding: 0;font-size: 20px;line-height: 1.25;">Hello ${name},</h4>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    We received a request to change the email address of your GBIF account from <a href="mailto:${currentEmail}" style="color: #509E2F;text-decoration: none;">${currentEmail}</a> to <a href="mailto:${newEmail}" style="color: #509E2F;text-decoration: none;">${newEmail}</a>.
    Please click the button below to change your email:
</p>

<table style="margin: 0;padding: 0;line-height: 1.65;border-collapse: collapse;width: 100% !important;">
    <tr style="margin: 0;padding: 0;line-height: 1.65;">
        <td align="center" style="margin: 0;padding: 0;line-height: 1.65;">
            <p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
                <a href="${url}" class="button" style="margin: 0;padding: 0;line-height: 1.65;color: white;text-decoration: none;display: inline-block;background: #509E2F;border: solid #509E2F;border-width: 10px 20px 8px;font-weight: bold;border-radius: 4px;">Change</a>
            </p>
        </td>
    </tr>
</table>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    You can also copy the following URL and paste into your browser: <a href="${url}" style="color: #509E2F;text-decoration: none;">${url}</a>
</p>


<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>The GBIF Secretariat</em>
</p>

<#include "footer.ftl">
