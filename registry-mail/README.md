# GBIF Registry Mail

This module includes interfaces and utilities to deal with freemarker template processing and email sending.


## Scope
This module offers:
  * `EmailSender` interface for email sending
  * `EmailTemplateProcessor` interface for template processing (Freemarker implementation)
  * `EmailDataProvider` interface which provide template name and email subject

## Configuration

  * Per-category dev redirect (e.g. for workshops: allow identity emails, redirect endorsement emails):
    * `mail.devEmailForIdentity.enabled` / `mail.devEmailForIdentity.address` — identity (user registration, password reset, etc.)
    * `mail.devEmailForOrganizationsEndorsement.enabled` / `mail.devEmailForOrganizationsEndorsement.address` — organization endorsement emails to nodes
    * `mail.devEmailForCollections.enabled` / `mail.devEmailForCollections.address` — collections (GRSciColl) notifications
    * `mail.devEmailForPipelines.enabled` / `mail.devEmailForPipelines.address` — pipelines notifications

  * `mail.useInMemoryEmailSender` when true, use an in-memory sender (no real emails). Set to `true` in integration tests.

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

