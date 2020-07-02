# Registry Editors

These roles defined for users in the registry:

* `USER`
* `DATA_REPO_USER`
* `REGISTRY_EDITOR`
* `REGISTRY_ADMIN`

These have the following permissions:

## `USER`

No special permissions.

## `DATA_REPO_USER`

Not relevant to the registry. (I think.)

## `REGISTRY_EDITOR`

Their permissions are defined by entity keys in the `editor_rights` table in the database.

If the key is a… | Then they can…
-----------------|-------------------------------------------------------------------------------------------------------------------
Dataset          | Update and delete the dataset.
Installation     | Update and delete the installation and datasets under that installation.
Organization     | Update and delete the organization.  Create, update and delete datasets and installations under this organization.
Node             | Update and delete the node.  Create, update and delete datasets, installations and organizations under this node.
Network          | Update and delete the network.

## `REGISTRY_ADMIN`

Has full access.
