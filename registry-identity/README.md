# GBIF Registry Identity

This module includes most of the classes to deal with users and user management. It is responsible coordinates the sequence
of actions, generate the content of the emails related to users and interacts with the `registry-surety` module for everything that is related to challenge code and sending emails.

## Modules
 - `IdentityAccessModule`: exposes IdentityAccessService to login and get a single user
 - `IdentityModule`: exposes IdentityService mostly to manage users

## Internals
Internally, the entry point is the `IdentityServiceModule`.
