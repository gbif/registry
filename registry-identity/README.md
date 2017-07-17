# GBIF Registry Identity

This module includes most of the classes to deal with users and user management. It is responsible coordinates the sequence
of actions, generate the content of the emails related to users and interacts with the `registry-surety` module for everything that is related to challenge code and sending emails.

## Modules
 - `IdentityAccessModule`: exposes IdentityAccessService to login and get a single user
 - `IdentityModule`: exposes IdentityService mostly to manage users

## Scope
 This module contains:
   * MyBatis interface and mapper for User table
   * Business logic related to user login and user management (`IdentityService`)
   * User related email generation from Freemarker template
   * Guice modules (`IdentityAccessModule`, `IdentityModule`)

 This module does not contain:
  * Authorisation related stuff other than providing the role(s) a the user(s)
  * User database table definition (see `registry-liquibase` module)

## Internals
Internally, the entry point is the `IdentityServiceModule`.

## Table schema design
The account, roles, settings are all managed in a single table for simplicity.
