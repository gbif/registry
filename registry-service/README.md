# GBIF Registry Service

This module is intended for future refactoring three layer architecture.

[Parent](../README.md)


## GRSciColl Lookup Explained

The lookup can receive any of these [parameters](https://github.com/gbif/gbif-api/blob/master/src/main/java/org/gbif/api/model/collections/lookup/LookupParams.java).
All of them are optional.

The matching for institutions and collections is almost independent for each of them with a few exceptions that will be explained later.
The overall matching process for each entity is as follows:

![](docs/grscicoll_lookup.png)

**[1]**

If the code is not provided and the identifier matches it's considered a exact match.

The identifier match also includes the matches by key (UUID) - e.g.: `institutionId=1a69e6fc-4a8d-44d5-90a6-a7dc7a1aa7c7`.

Also, there are some specific conditions based on the entity type:
- Institutions: if the `ownerInstitutionCode` is different than the institutions matched we discard them and
flag them as `AMBIGUOUS_OWNER`

**[2]**

One of the parameters that the lookup service can receive is the `datasetKey`. This parameter has to be used when we want to link institutions/collections
within the context of a dataset.

**When the `datasetKey` param is provided and there hasn't been exact matches** we check the [occurrence mappings](https://github.com/gbif/gbif-api/blob/master/src/main/java/org/gbif/api/model/collections/OccurrenceMapping.java).
The occurrence mappings are a way to manually map occurrences to a specific institution/collection.
An `OccurrenceMapping` contains a `datasetKey` and optionally a `code` and/or a `identifier` that can be used to refine the mappings
when there are more than 1 possible combination within a dataset. This allows us to do things like:
- All the occurrences from the dataset X have to be mapped to the institution I
- All the occurrences from the dataset X and code Y have to be mapped to the institution I
- All the occurrences from the dataset X, code Y and identifier Z have to be mapped to the institution I

There can be as many combinations as needed.

**[3]**

A fuzzy match happens when some fields match but not the `code` and the `identifier` at the same time. Examples of these matches are:
- Only the `code` matches
- Only the `identifier` matches
- An `alternativeCode` matches
- The `code` param matches with the `name` of an entity. E.g: `institutionCode=University of Copenhagen`
- The `identifier` param matches with the `name` of an entity. E.g.: `institutionId=University of Copenhagen`

Additionally, if there is more than 1 fuzzy match we try to check if one of the matches is better than the others
and we can set it as the accepted one. It checks these conditions in this order:
1. If there is only 1 entity where the `identifier` matches we take it
2. If there is only 1 entity where either the `code` or the `identifier` matches **and** another field matches we take it
3. If the `country` param was provided and there is only 1 entity where the `country` matches we take it
4. If there is only one match that is set as `active` we take it

Furthermore, there are some specific conditions based on the type of the entities:
- Institutions: if the `ownerInstitutionCode` is different than the institutions matched we discard them and
flag them as `AMBIGUOUS_OWNER`
- Collections: if the institution of the collections found doesn't match with the institutions previously matched we discard
them and flag them as `AMBIGUOUS_INSTITUTION_MISMATCH`

<br/>

Finally, if we want to know more about the matches there are 2 things that can help us:
- The `reasons` field in the response shows all the fields that matched
- We can send the `verbose` parameter as `true` and all the matches found will be returned
