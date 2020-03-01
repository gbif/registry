# GBIF Registry Identity

This module includes most of the classes to deal with users and user management. It is responsible coordinates the sequence
of actions, generate the content of the emails related to users and interacts with the `registry-surety` module for everything that is related to challenge code and sending emails.

## Scope
 This module contains:
   * `IdentityService` business logic related to user login and user management
   * `UserSuretyDelegate` coordinate actions between the MyBatis layer and the email manager

 This module does not contain:
  * Authorisation related stuff other than providing the role(s) and the user(s)
  * Mappers or user database table definition (see `registry-persistence` module)
  * Ftl templates (see `registry-mail`)

## Table schema design
The account, roles, settings are all managed in a single table for simplicity.

[Parent](../README.md)
