<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.ConfirmableTemplateDataModel" -->
<#include "header.ftl">

<h4 style="margin: 0 0 20px;padding: 0;font-size: 20px;line-height: 1.25;">Уважаемый/ая ${name},</h4>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Мы получили запрос на изменение адреса электронной почты вашего аккаунта GBIF.
    Нажмите на кнопку, чтобы изменить адрес электронной почты:
</p>

<table style="margin: 0;padding: 0;line-height: 1.65;border-collapse: collapse;width: 100% !important;">
    <tr style="margin: 0;padding: 0;line-height: 1.65;">
        <td align="center" style="margin: 0;padding: 0;line-height: 1.65;">
            <p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
                <a href="${url}" class="button" style="margin: 0;padding: 0;line-height: 1.65;color: white;text-decoration: none;display: inline-block;background: #509E2F;border: solid #509E2F;border-width: 10px 20px 8px;font-weight: bold;border-radius: 4px;">Обновить</a>
            </p>
        </td>
    </tr>
</table>


<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>GBIF Секретариат</em>
</p>

<#include "footer.ftl">
