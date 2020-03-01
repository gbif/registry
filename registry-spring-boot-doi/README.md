# GBIF Registry DOI

This module includes interfaces and utilities to deal with [DOI](https://en.wikipedia.org/wiki/Digital_object_identifier) in the context of the Registry.
Uses [GBIF DOI project](https://github.com/gbif/gbif-doi).

## Configuration

Properties can be configured

* `doi.prefix` is a DOI prefix. DOI consists of two parts: prefix and suffix (parts before/after slash). DOI example `10.21373/abcd`, `10.21373` is a prefix of this DOI.
**DOI must start with `10.`**
[GBIF implementation](https://github.com/gbif/gbif-api/blob/master/src/main/java/org/gbif/api/model/common/DOI.java).

* `doi.datasetParentExcludeList` when configured, we can skip the DOI logic for some dataset when the getParentDatasetKey is in the parentDatasetExcludeList. Can be empty.

[Parent](../README.md)
