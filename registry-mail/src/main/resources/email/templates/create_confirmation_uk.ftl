<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.ConfirmableTemplateDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Вітаємо, {name}!</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">Дякуємо, що зареєструвалися на GBIF.org.
    Будь ласка, підтвердьте ваш обліковий запис GBIF, натиснувши на кнопку нижче:</p>

<table style="margin: 0;padding: 0;line-height: 1.65;border-collapse: collapse;width: 100% !important;">
    <tr style="margin: 0;padding: 0;line-height: 1.65;">
        <td align="center" style="margin: 0;padding: 0;line-height: 1.65;">
            <p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
                <a href="${url}" class="button" style="margin: 0;padding: .375rem .75rem;line-height: 1.65;text-decoration: none;display: inline-block;font-weight: 400;text-align: center;vertical-align: middle;cursor: pointer;user-select: none;background-color: transparent;border: 1px solid #61a861;font-size: 14px;border-radius: .25rem;color: #61a861;">Підтвердити</a>
            </p>
        </td>
    </tr>
</table>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Також ви можете скопіювати наступний URL та вставити у ваш браузер: <a href="${url}" style="color: #4ba2ce;text-decoration: none;">${url}</a>
</p>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>Секретаріат GBIF</em>
</p>

<#include "footer.ftl">
