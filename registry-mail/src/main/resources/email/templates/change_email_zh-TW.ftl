<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountChangeEmailTemplateDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">您好 ${name}，</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    我們收到您來信申請 GBIF 帳號的電子郵件地址由 <a href="mailto:${currentEmail}" style="color: #4ba2ce;text-decoration: none;">${currentEmail}</a> 變更為 <a href="mailto:${newEmail}" style="color: #4ba2ce;text-decoration: none;">${newEmail}</a>。
    請點按下方按鈕以確認變更您的電子郵件地址。
</p>

<table style="margin: 0;padding: 0;line-height: 1.65;border-collapse: collapse;width: 100% !important;">
    <tr style="margin: 0;padding: 0;line-height: 1.65;">
        <td align="center" style="margin: 0;padding: 0;line-height: 1.65;">
            <p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
                <a href="${url}" class="button" style="margin: 0;padding: .375rem .75rem;line-height: 1.65;text-decoration: none;display: inline-block;font-weight: 400;text-align: center;vertical-align: middle;cursor: pointer;user-select: none;background-color: transparent;border: 1px solid #61a861;font-size: 14px;border-radius: .25rem;color: #61a861;">確認變更</a>
            </p>
        </td>
    </tr>
</table>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    您也可以複製此網址至您的瀏覽器： <a href="${url}" style="color: #4ba2ce;text-decoration: none;">${url}</a>
</p>


<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>GBIF 秘書處</em>
</p>

<#include "footer.ftl">
