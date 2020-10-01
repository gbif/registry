<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.ConfirmableTemplateDataModel" -->
<#include "header.ftl">

<h4 style="margin: 0;padding: 0;font-size: 20px;line-height: 1.25;margin-bottom: 20px;">Уважаемый/ая ${name},</h4>

<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">Спасибо за регистрацию на GBIF.org.
    Подтвердите свою учетную запись GBIF, нажав кнопку ниже:</p>

<table style="margin: 0;padding: 0;line-height: 1.65;border-collapse: collapse;width: 100% !important;">
    <tr style="margin: 0;padding: 0;line-height: 1.65;">
        <td align="center" style="margin: 0;padding: 0;line-height: 1.65;">
            <p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">
                <a href="${url}" class="button" style="margin: 0;padding: 0;line-height: 1.65;color: white;text-decoration: none;display: inline-block;background: #509E2F;border: solid #509E2F;border-width: 10px 20px 8px;font-weight: bold;border-radius: 4px;">Подтвердить</a>
            </p>
        </td>
    </tr>
</table>


<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">
    <em style="margin: 0;padding: 0;line-height: 1.65;">GBIF Секретариат</em>
</p>

<#include "footer.ftl">
