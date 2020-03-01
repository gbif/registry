# Registy OAI-PMH

In addition to the RESTful JSON API, Datasets are exposed using OAI-PMH. Two metadata formats can be retrieved: Ecological Metadata Language (EML) and OAI Dublin Core. Datasets are grouped into sets according to type, country and installation.

The endpoint is at https://api.gbif.org/v1/oai-pmh/registry (production).

Full details of the protocol can be found in [The Open Archives Initiative Protocol for Metadata Harvesting](https://www.openarchives.org/OAI/openarchivesprotocol.html), in particular the section on [Protocol Requests and, Responses](https://www.openarchives.org/OAI/openarchivesprotocol.html#ProtocolMessages).

## Example queries

* Retrieve information about the OAI-PMH service: [Identify](https://api.gbif.org/v1/oai-pmh/registry?verb=Identify).

* Retrieve a list of available sets (dataset types, countries and serving installations): [ListSets](https://api.gbif.org/v1/oai-pmh/registry?verb=ListSets).
Sets have names like `dataset_type:CHECKLIST` and `country:NL`.

* Retrieve the identifiers for all datasets from a particular installation: ListIdentifiers.
According to the OAI-PMH protocol, `metadataPrefix` must be set to either `oai_dc` or `eml`, even though both formats are supported for all datasets.

* Retrieve the metadata for all datasets served by installations in a country: [ListRecords](https://api.gbif.org/v1/oai-pmh/registry?verb=ListRecords&metadataPrefix=oai_dc&set=Country:TG).
Country codes are based on the [ISO 3166-1](https://www.iso.org/obp/ui/#search) standard.
Some queries will return more than one page of results. In this case, the XML will end with a resumption token element, for example:
`<resumptionToken cursor="1">MToxMDB8Mjpjb3VudHJ5Ok5MfDM6fDQ6fDU6b2FpX2Rj</resumptionToken>`
The second page of results can be retrieved like this: [Resume](https://api.gbif.org/v1/oai-pmh/registry?verb=ListRecords&resumptionToken=MToxMDB8MjpDb3VudHJ5Ok5MfDM6fDQ6fDU6b2FpX2Rj).

## Configuration

Properties can be configured

* `oaipmh.baseUrl` is an API url (https://api.gbif.org/v1/oai-pmh/registry for production).

* `oaipmh.adminEmail` is an admin email (dev@gbif.org for production).
