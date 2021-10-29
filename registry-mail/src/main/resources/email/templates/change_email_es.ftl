<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountChangeEmailTemplateDataModel" -->
<#include "header.ftl">

<h5 style="margin: 0 0 20px;padding: 0;font-size: 16px;line-height: 1.25;">Hola ${name},</h5>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Hemos recibido una solictud para cambiar el correo electrónico de su cuenta GBIF de <a href="mailto:${currentEmail}" style="color: #4ba2ce;text-decoration: none;">${currentEmail}</a> a <a href="mailto:${newEmail}" style="color: #4ba2ce;text-decoration: none;">${newEmail}</a>.
    Por favor, pulse el siguiente botón para cambiar su correo electrónico:
</p>

<table style="margin: 0;padding: 0;line-height: 1.65;border-collapse: collapse;width: 100% !important;">
    <tr style="margin: 0;padding: 0;line-height: 1.65;">
        <td align="center" style="margin: 0;padding: 0;line-height: 1.65;">
            <p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
                <a href="${url}" class="button" style="margin: 0;padding: .375rem .75rem;line-height: 1.65;text-decoration: none;display: inline-block;font-weight: 400;text-align: center;vertical-align: middle;cursor: pointer;user-select: none;background-color: transparent;border: 1px solid #61a861;font-size: 14px;border-radius: .25rem;color: #61a861;">Cambiar</a>
            </p>
        </td>
    </tr>
</table>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    También puede copiar la siguiente URL y pegarla en su navegador: <a href="${url}" style="color: #4ba2ce;text-decoration: none;">${url}</a>
</p>


<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>Secretaría de GBIF</em>
</p>

<#include "footer.ftl">
