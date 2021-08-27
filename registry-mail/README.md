# GBIF Registry Mail

This module includes interfaces and utilities to deal with freemarker template processing and email sending.


## Scope
This module offers:
  * `EmailSender` interface for email sending
  * `EmailTemplateProcessor` interface for template processing (Freemarker implementation)
  * `EmailDataProvider` interface which provide template name and email subject

## Configuration

  * `mail.devemail.enabled` enable dev mode. If true sends email to `mail.devemail.address` and ignores copy addresses

  * `mail.devemail.address` email address for dev mode

  * `mail.cc` mail carbon copy addresses (comma separated list)

  * `mail.bcc` blind mail carbon copy addresses (comma separated list)

  * `mail.enabled` if false then email sending switches off

  * `organization.surety.mail.urlTemplate.confirmOrganization` template url address 'confirm organization'

  * `organization.surety.mail.urlTemplate.organization` template url address 'organization endorsed'

  * `organization.surety.mail.helpdesk` GBIF helpdesk email address

  * `identity.surety.mail.urlTemplate.confirmUser` template url address 'confirm user'

  * `identity.surety.mail.urlTemplate.resetPassword` template url address 'reset password'

## Translations

We support localization for GRSciColl and identity emails. 
Organization related emails and their properties not to be translated!

All the translations are managed at the CrowdIn project https://crowdin.com/project/gbif-registry.
So we apply changes only for english files (without extension).


[Parent](../README.md)

