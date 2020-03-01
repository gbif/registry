# GBIF Registry Surety

## ChallengeCode
This module includes interfaces and utilities to deal with "surety" (something that makes sure; ground of confidence or safety) for the registry general.
This module handles surety by using ChallengeCodes to verify and confirm actions that can be triggered externally. In order to confirm an action, the module will produce
a ChallengeCode and check if the initiator of the action can send it back using a predefined mechanism (currently, emails).

From a database perspective, the ChallengeCode approach simply links an arbitrary entity with a ChallengeCode table. Using the `ChallengeCodeManager` it is possible to create, check and remove ChallengeCode as long as the targeted entity mapper implements `ChallengeCodeSupportMapper`.

## Scope
This module offers:
  * ChallengeCodeManager to allow manipulation of ChallengeCode between ChallengeCode and a ChallengeCodeSupportMapper.

## Sequence
The sequence is not defined or enforced by this module since it may (and will) vary depending of the usage.
But, it was designed to follow more or less the following sequence:
 * An entity is created
 * A ChallengeCode is created and stored (`ChallengeCodeManager.create`)
 * An email is generated (`EmailTemplateProcessor.buildEmail`)
 * The email is sent with the created ChallengeCode (`EmailSender.send`)
 * Once the ChallengeCode is received for the created entity the ChallengeCode is removed (`ChallengeCodeManager.remove`)
 * A confirmation email is sent

[Parent](../README.md)

