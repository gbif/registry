# GBIF Registry Surety

## ChallengeCode
This module includes interfaces and utilities to deal with "surety" (only ChallengeCode based at the moment) in general.
From a database perspective, the ChallengeCode approach simply links an arbitrary entity with a ChallengeCode table. Using
the `ChallengeCodeManager` it is possible to create, check and remove ChallengeCode as long as the targeted entity mapper
implements `ChallengeCodeSupportMapper`.

## Email
This module also includes classes to generate and send emails from templates. At some points this part could be moved
into its own package.

## Scope
This module offers:
  * MyBatis interface and mapper for ChallengeCode table
  * MyBatis interface for ChallengeCode support to allow another mapper to link to ChallengeCode.
  * ChallengeCodeManager to allow manipulation of ChallengeCode between ChallengeCode and a ChallengeCodeSupportMapper.
  * Email generation from Freemarker template (with possibility for multilingual templates)
  * Email sending (using `javax.mail`)
  * Guice Email module (`EmailManagerModule`)

## Sequence
The sequence is not defined or enforced by this module since it may (and will) vary depending of the usage.
But, it was designed to follow more or less the following sequence:
 * An entity is created
 * A ChallengeCode is created and stored (`ChallengeCodeManager.create`)
 * An email is generated (`EmailTemplateProcessor.buildEmail`)
 * The email is sent with the created ChallengeCode (`EmailManagers.send`)
 * Once the ChallengeCode is received for the created entity the ChallengeCode is removed (`ChallengeCodeManager.remove`)
 * A confirmation email is sent



