<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountChangeEmailTemplateDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Уважаемый пользователь ${name},</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Мы получили запрос на изменение адреса электронной почты вашего аккаунта GBIF c <a href="mailto:${currentEmail}" style="color: #4ba2ce;text-decoration: none;">${currentEmail}</a> на <a href="mailto:${newEmail}" style="color: #4ba2ce;text-decoration: none;">${newEmail}</a>.
    Нажмите на кнопку, чтобы изменить адрес электронной почты:
</p>

<table style="margin: 0;padding: 0;line-height: 1.65;border-collapse: collapse;width: 100% !important;">
    <tr style="margin: 0;padding: 0;line-height: 1.65;">
        <td align="center" style="margin: 0;padding: 0;line-height: 1.65;">
            <p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
                <a href="${url}" class="button" style="margin: 0;padding: .375rem .75rem;line-height: 1.65;text-decoration: none;display: inline-block;font-weight: 400;text-align: center;vertical-align: middle;cursor: pointer;user-select: none;background-color: transparent;border: 1px solid #61a861;font-size: 14px;border-radius: .25rem;color: #61a861;">Обновить</a>
            </p>
        </td>
    </tr>
</table>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Вы также можете скопировать следующий URL и вставить в свой браузер: <a href="${url}" style="color: #509E2F;text-decoration: none;">${url}</a>
</p>


<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>GBIF Секретариат</em>
</p>

<#include "footer.ftl">
