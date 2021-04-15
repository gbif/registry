# GRSciColl Catalogue priority roadmap 2021

This roadmap builds on the previous work of GRSciColl Catalogue that connected Index Herbariorum, imported the iDigBio content and linked GBIF occurrence records to the entities in GRSciColl. The roadmap identifies six key priorities to progress.

## Reduce the amount of duplicate records

The connection with Index Herbariorum and import of iDigBio enriched the catalogue, but also increased the number of duplicate entities that can’t be automatically handled. This will be addressed by:

*   Documenting guidelines on how a data manager can resolve duplicate issues [[REG-316](https://github.com/gbif/registry/issues/316)]. The guidelines will provide example scenarios, explain the recommended approach to defining codes and explain the implications on external systems (see master data management below).
*   Develop tools that help identify potential duplicates alerting them to managers [[REG-191](https://github.com/gbif/registry/issues/191)] 


## Allow anyone to propose changes

The current processes are weak, and don’t capture the proposed change in a structured manner.

*   Develop an interface allowing anyone to propose a change to any/all fields and state whether they have authority to approve it. Changes are then to be reviewed and applied by the editorial team [[REG-CONSOLE-376](https://github.com/gbif/registry-console/issues/376)]. 

## Improve documentation

*   Document the technical aspects of the system focusing on the data model [[REG-317](https://github.com/gbif/registry/issues/317)], authorization rules [[REG-310](https://github.com/gbif/registry/issues/310)] and the details around master data management (see below).
*   Document the guidelines for data editors including the decision process of merging entities and assigning IDs and codes [[DP-3](https://github.com/gbif/data-products/issues/3)] [[REG-316](https://github.com/gbif/registry/issues/316)]. 

## Grow the pool of editors

*   Present the system at the global nodes meeting and openly invite node managers to assign staff 
    *   Identify specific tasks we would ask them to do, arranging into a TODO list so it is clear for contributors and community involvement can easily be measured.
*   Review the authorization rules to ensure that editors can be granted access to work on only those areas they are responsible for [[REG-310](https://github.com/gbif/registry/issues/310)] 

## Define and implement the master data management solution

There are potentially multiple sources of truth for the metadata in the catalogue which needs to be resolved; a problem known as [master data management](https://en.wikipedia.org/wiki/Master_data_management). For example we have information available in a dataset metadata description, an existing GRSciColl entry and an Index Herbariorum record.

*   Define, implement and document the approach taken by the catalogue for handling differing views of metadata [[REG-319](https://github.com/gbif/registry/issues/319)] 
    *   An approach <span style="text-decoration:underline;">could</span> be as follows: 
        *   For each institution and collection entry in the catalogue, a single source of truth is identified for the key metadata (title, description etc). This may be one of: 
            *   An entry from Index Herbariorum, or other system that is automatically integrated through harvesting
            *   Metadata for a dataset registered in GBIF (i.e. an EML file) [[REG-305](https://github.com/gbif/registry/issues/305)] 
            *   An entry made directly into the catalogue through the user interface, or pushed through the API by an application (e.g. a collection management system) 
        *   The core metadata is never changed in GRSciColl for externally sourced entities, and edits must be applied in the system providing the master record.
            *   The entries in GRSciColl may be enriched with the following fields:
                *   Additional identifiers to link to alternative views or aid discovery 

## Develop a richer user interface

*   Implement a new user interface for the GrSciColl based on the [visual concepts](http://labs.gbif.org/visual-concepts/) including:
    *   Institution and collection search and detail pages
    *   Integration of specimen-related occurrences (search, maps, gallery, detail, clustering)
    *   Capability for any user to “suggest a change”
*   Explore citation tracking based on data mediated through GBIF for GRSciColl institutions and collections. For more information, follow the discussion here [[REG-323](https://github.com/gbif/registry/issues/323)].
*   Launch the new site considering aspects of branding/naming with a call for institutions to review their data and clear instructions on how to suggest edits.


## Areas not covered in this immediate roadmap

This is a non exhaustive list of items that are not part of our immediate priorities in 2021 but that we aim to address in the longer term (possibly starting end 2021):

*   Explore adding DOIs or similar to institutions and collections [[REG-320](https://github.com/gbif/registry/issues/320)]
*   Explore synchronization with the NCBI BioCollections [[REG-307](https://github.com/gbif/registry/issues/307)] 
*   Explore integration of the TDWG Collection Descriptors [[REG-176](https://github.com/gbif/registry/issues/176)]
*   Explore synchronization with CETAF [[REG-322](https://github.com/gbif/registry/issues/322)] 
*   Explore integration of external identifiers [[REG-274]](https://github.com/gbif/registry/issues/274)
*   Improve the staff entities (people added as contacts to collections and institutions) [[REG-321](https://github.com/gbif/registry/issues/321)] 
