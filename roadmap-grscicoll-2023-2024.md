---
permalink: /road-map
lang-ref: road-map
title: GRSciColl 2023-2024 road map
description: |
    This road map builds in the [2021 road map](https://github.com/gbif/registry/blob/dev/roadmap-grscicoll-2021.md) as well as the efforts in 2022 to build a community of editors and mediators.
background:  "{{ site.data.images.pandeleteius.src }}"
imageLicense: "{{ site.data.images.pandeleteius.caption }}"
height: 70vh
# layout: documentation
# sideNavigation: about.about
# composition: # you can extend the documentation layout with a custom composition
#  - type: postHeader
#  - type: pageMarkdown
toc: true
---

## 1. Review data schema

The data model for GRSciColl has evolved over time to accommodate the data sources being connected, such as iDigBio, Index Herbariorum and the original databases that were integrated. At the same time, there has been extensive work by the [TDWG Collections Descriptions Interest Group](https://www.tdwg.org/community/cd/) to standardize relevant approaches and vocabularies emerging from a framework called [Latimer Core](https://doi.org/10.3897/biss.6.91159).

We will review all the fields in the data schema and their content to ensure they are intuitive, thoroughly documented and, where possible, aligned to Latimer Core. As examples of what this might entail, GRSciColl currently has:

- “ContentType” uses a controlled vocabulary that has caused confusion for editors. This should be reviewed and aligned to Latimer Core.
- “Discipline” with a controlled vocabulary that could be aligned to the Latimer Core Discipline

It is sometimes unclear when and how the fields “inactive” and “isHerbarium” should and might be used. 

## 2. Support structured collection descriptors

GRSciColl is not currently structured to allow discovery of collections or individual specimens, which could be critical for researchers. Support for describing a registered collection in GRSciColl is at present limited to single fields that capture broad statements of taxonomic coverage, geographic coverage and, for example, important collectors. This enhancement is intended to support richer, structured descriptions as well as the ability to upload an inventory of the species represented or a table representing the “species, sex, object count”. This approach aims both to facilitate discovery of collections (“who holds preserved material of a specific species”) and to allow a more accurate description of a collections holding, whether digitized or not.

We anticipate supporting multiple descriptors for a collection, with a descriptor containing a title, textual explanation and a table of data edited inline or uploaded as a spreadsheet.

A simple example is illustrated.

<img width="420" alt="Screenshot 2023-09-28 at 16 43 46" src="https://github.com/gbif/registry/assets/7677271/459e7d2a-2ddb-4307-9e8f-fef88db96ace">

We envisage the system would be flexible enough to accommodate differing levels of detail, from simple lists to detailed representations of the collection that exist for some collections. By supporting multiple descriptors for a collection, the system would support varied levels of documenting detailed aspects of a collection, and indexing the contents would improve their discoverability. For example, we aim to index scientific names using the [GBIF Backbone Taxonomy](https://doi.org/10.15468/39omei) in order to facilitate collection discovery by taxa.

It should be noted that this approach would mean that one may not be able to aggregate counts across descriptors, as objects may be included in multiple places and thus double-counted. However, the primary focus is to support the needs of taxonomists looking to discover collections of interest or identify locations where individual specimens may reside.

The Index Herbariorum has descriptor tables (see, for example, the `collections summary` tab on this [Index Herbariorum page](https://sweetgum.nybg.org/science/ih/herbarium-details/?irn=125976)), which would be automatically incorporated during the synchronization process. 

An API that exposes the descriptors as a Latimer Core document representing the collection will be available (likely in JSON format). 

## 3. Institutional surveys

While the collection descriptors above aim to help taxonomists discover where specimens of a particular species are preserved, institutional surveys are concerned with assessment and comparison across institutions. There are many approaches to assess the composition and size of collections held by institutions, with a view to generate aggregate views at higher scales (e.g. nationally) and to draw comparisons across them. These are often in the form of a survey or structured database and in many cases use a different set of categories than how the institution has registered its collections within GRSciColl. The application of consistent categories across collections are necessary to draw consistent comparisons across them.  

Survey results would be clearly labeled and not be editable as they represent a particular assessment in a defined context.

We intend to explore adding the ability to archive the outputs of such surveys when they have produced data that can be expressed within the framework of Latimer Core. As an example, the Global Collection Survey assessed institutions against a survey scheme that included:

- The type of the collections categorised to a specific list (e.g. amphibians, birds, molluscs, human biology)
- The geographic region categorized to a specific list (e.g. Australasia, Pacific, Europe)
- The number of objects held for the combination of the collection type and region grouped by ranges (1-10, 11-100, etc.)
- The number of staff years of experience per region and separately per collection type

The outputs of these data can likely be structured as Latimer Core and held as a survey response against the specific survey protocol for the institution. 

We foresee that archiving these may bring benefits:

- GRSciColl can help provide services that drive dashboards and reports 
- Survey protocols may be shared with other initiatives, helping increase the range of institutions that can be compared in a consistent manner
- The results of previous surveys become more visible and may reduce effort spent, or the need for more surveys to be conducted

Given the feedback received, we will also explore how to make GRSciColl a place where surveys can converge and be updated. For example, we will try to answers questions like:

- Can we make GRSciColl a place to collect survey results by combining the data schema review and the upload of collection descriptors? 
- Should/can we and how to make surveys editable? 
- Answering those questions will help shape the road map for the following year.

## 4. A new user interface for GRSciColl

The public interface of GRSciColl is limited in capabilities, offering less than both the management interface and what is possible using the GBIF Hosted Portal software like the DiSSCo UK site. We intend to replace the existing GRSciColl site with a hosted portal deployment enhanced with new features, such as maps for institutions and clear explanatory text for users and data managers. Work on this has begun and can be seen at https://scientific-collections.gbif.org.

## 5. Establish mechanism for regular community updates

As the community of GRSciColl editors and users is growing, we want to offer a place where we can communicate updates, provide feedback and discuss improvements for the system. There is already an informal mailing list for GRSciColl mediators and other interested parties. We think that regular virtual meetups would be a great way to exchange more interactively. We aim to have meetings every four months for a year and assess whether the frequency and format of those meetings is adequate.
