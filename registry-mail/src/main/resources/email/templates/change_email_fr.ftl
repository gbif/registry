<#-- @ftlvariable name="" type="org.gbif.registry.domain.mail.AccountChangeEmailTemplateDataModel" -->
<#include "header.ftl">

<h4 style="margin: 0 0 20px;padding: 0;font-size: 20px;line-height: 1.25;">Bonjour ${name},</h4>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Nous avons reçu une demande pour changer l'adresse e-mail associé à votre compte GBIF de <a href="mailto:${currentEmail}" style="color: #509E2F;text-decoration: none;">${currentEmail}</a> à <a href="mailto:${newEmail}" style="color: #509E2F;text-decoration: none;">${newEmail}</a>.
    Veuillez cliquer sur le bouton ci-dessous pour confirmer le changement:
</p>

<table style="margin: 0;padding: 0;line-height: 1.65;border-collapse: collapse;width: 100% !important;">
    <tr style="margin: 0;padding: 0;line-height: 1.65;">
        <td align="center" style="margin: 0;padding: 0;line-height: 1.65;">
            <p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
                <a href="${url}" class="button" style="margin: 0;padding: 0;line-height: 1.65;color: white;text-decoration: none;display: inline-block;background: #509E2F;border: solid #509E2F;border-width: 10px 20px 8px;font-weight: bold;border-radius: 4px;">Mise à jour</a>
            </p>
        </td>
    </tr>
</table>

<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    Vous pouvez également copier l'URL suivante et la coller dans votre navigateur : <a href="${url}" style="color: #509E2F;text-decoration: none;">${url}</a>
</p>


<p style="margin: 0 0 20px;padding: 0;line-height: 1.65;">
    <em>Le Secrétariat du GBIF</em>
</p>

<#include "footer.ftl">
