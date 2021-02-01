# GBIF Registry Security

This module includes security, authorization and role model functionality.

## Role model

- `REGISTRY_ADMIN`
- `REGISTRY_EDITOR`
- `USER`
- `APP`
- `GRSCICOLL_ADMIN`
- `GRSCICOLL_EDITOR`
- `IDIGBIO_GRSCICOLL_EDITOR`


## Security filters

- *LegacyAuthorizationFilter* is a filter that intercepts legacy web service requests to `/registry/*` and perform authentication setting a security context on the request.

- *EditorAuthorizationFilter* is a filter for requests authenticated with a `REGISTRY_EDITOR` role. Two levels of authorization need to be
                               passed. First of all any resource method is required to have the role included in the `Secured` or
                               `RolesAllowed` annotation. Secondly this request filter needs to be passed for POST/PUT/DELETE
                               requests that act on existing and UUID identified main registry entities such as dataset,
                               organization, node, installation and network.

- *JwtRequestFilter* is a filter which validate JWT tokens.

[Parent](../README.md)
