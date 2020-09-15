# How citations are generated

In order to offer consistent citations, citations are auto-generated for most datasets.
 * Documentation available on the GBIF.org [FAQ](https://www.gbif.org/faq?q=citation)
 * Based on discussion from [Issue #4](https://github.com/gbif/registry/issues/4).
 * Source Code: [CitationGenerator.java](https://github.com/gbif/registry/blob/master/registry-metadata/src/main/java/org/gbif/registry/metadata/CitationGenerator.java)

## General Formula
```
{dataset.authors} ({dataset.pubDate})
{dataset.title}. [Version {dataset.version}]. {organization.title}.
{dataset.type} Dataset {dataset.doi}, accessed via GBIF.org on {YYYY-MM-DD}.
```

Example:

```
Smirnov I, Golikov A, Khalikov R (2017).
Ophiuroidea collections of the Zoological Institute Russian Academy of Sciences. Version 1.32.
Zoological Institute, Russian Academy of Sciences, St. Petersburg.
Occurrence Dataset https://doi.org/10.15468/ej3i4f accessed via GBIF.org on 2017-08-21.
```

### Author List Generation

From the dataset contacts, in order, take all (unique person based on the first and last name)
`ContactType.ORIGINATOR` and `ContactType.METADATA_AUTHOR`. For each of the eligible contacts,
use the form `lastName` and first letter of `firstNames` (e.g. Smirnov I).

Note:
  * At least one `ContactType.ORIGINATOR` shall be present. Otherwise the authors are replaced
  by the organization (`{organization.title}`) publishing the dataset. If the organization name is used, it won't be repeated in the citation.
  * An author is only considered if a lastName is provided.
  * The name of the organization on the contact itself is ignored for the citation generation.
  `organization.title` represents the name of the organization registered with GBIF that is publishing this dataset.
