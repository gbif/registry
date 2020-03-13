<#assign created = createdIsoDate?date("yyyy-MM-dd") />
## Custom GBIF Occurrence Download (${createdIsoDate})
This custom-made GBIF occurrence download was created on ${createdIsoDate} and contains ${downloadNumberRecords} species occurrence records. The data is derived from a total of ${downloadNumberDatasets} datasets and has been assigned [doi:${downloadDoi}](https://dx.doi.org/${downloadDoi}) to enable tracking its usage in articles.

#### What is its purpose?
${downloadPurpose}

#### Where can it be retrieved from?

The download can be retrieved from ${downloadURL} as a compressed and UTF-8 encoded CSV file.

#### How is it licensed?

This download is licensed under a [Creative Commons Attribution Non Commercial (CC-BY-NC) 4.0 License](http://creativecommons.org/licenses/by-nc/4.0/legalcode). Please note that data from some individual datasets included in this download may be licensed under less restrictive terms.

#### How should it be cited?

When using the download in an article, you must cite it in the references section or in the data references section. By assigning the download a unique DOI [doi:${downloadDoi}](https://dx.doi.org/${downloadDoi}) it keeps it separate from the article. This allows researchers to cite both the article and the data. GBIF will track each time the data has been cited (used) and ultimately relay credit back to the data providers.

For your convenience, the preferred data citation format is listed for you below:

> Legind J (${created?string.yyyy}): GBIF Custom Occurrence Download. v1.0. GBIF Secretariat. Dataset/Occurrence. http://doi.org/${downloadDoi}

In case you'd like to credit each dataset individually in your supplementary materials, the complete list of datasets from which the download is derived can be found [here](https://raw.githubusercontent.com/kbraak/custom-downloads/master/${githubFolderName}/used_datasets.csv)  Note the last column shows how many records each dataset contributed to the download.

***
Folder contents:

* [gbif_taxa.csv](https://github.com/kbraak/custom-downloads/blob/master/${githubFolderName}/gbif_taxa.csv) - the list of taxa used in the HIVE query used to lookup a taxon by its GBIF taxon ID.
* [select.hql](https://github.com/kbraak/custom-downloads/blob/master/${githubFolderName}/select.hql) - the HIVE query used to generate the data
* [create_table.hql](https://github.com/kbraak/custom-downloads/blob/master/${githubFolderName}/create_table.hql) - the HIVE query used to create the table that stored the data
* [download.properties](https://github.com/kbraak/custom-downloads/blob/master/${githubFolderName}/download.properties) - a set of properties related to the download (used to build datacite.xml)
* [used_datasets.csv](https://github.com/kbraak/custom-downloads/blob/master/${githubFolderName}/used_datasets.csv) - the list of datasets the download is derived from (used to build datacite.xml)
* [datacite.xml](https://github.com/kbraak/custom-downloads/blob/master/${githubFolderName}/datacite.xml) - the DataCite metadata document related to the download
