<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.ConfirmableTemplateDataModel" -->
<#include "header.ftl">

<h4 style="margin: 0;padding: 0;font-size: 20px;line-height: 1.25;margin-bottom: 20px;">Hello ${name},</h4>

<p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">We received a request to reset the password of your GBIF account. Please click the following button to reset your password:</p>

    <table style="margin: 0;padding: 0;line-height: 1.65;border-collapse: collapse;width: 100% !important;">
        <tr style="margin: 0;padding: 0;line-height: 1.65;">
            <td align="center" style="margin: 0;padding: 0;line-height: 1.65;">
                <p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">
                    <a href="${url}" class="button" style="margin: 0;padding: 0;line-height: 1.65;color: white;text-decoration: none;display: inline-block;background: #509E2F;border: solid #509E2F;border-width: 10px 20px 8px;font-weight: bold;border-radius: 4px;">Reset</a>
                </p>
            </td>
        </tr>
    </table>


</p><p style="margin: 0;padding: 0;line-height: 1.65;margin-bottom: 20px;">
    <em style="margin: 0;padding: 0;line-height: 1.65;">The GBIF Secretariat</em>
</p>

<#include "footer.ftl">
