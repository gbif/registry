# Registration with GBIF: Bash shell script example

Registration with GBIF can be achived with two REST calls — the first to register a dataset, the second to add an endpoint (HTTP location) for GBIF to access the dataset.

This script demonstrates the two calls to register a metadata-only dataset, and records the response (the dataset key).

To run it, just run `./register.sh` — it uses the test GBIF at GBIF-UAT.org.

To create datasets for your own organization, you should ask helpdesk@gbif.org for `editor_rights` permissions for those organizations.
