#!/bin/bash -eu
#
# This is an outline Bash script to show updating a set of datasets registered with GBIF.org.
#
# The script isn't recommended for production use; it shows the steps involved, but does not have any error checking.
#
# Using a process like this, you should make sure you store the UUID GBIF assigns to your dataset, so you don't accidentally
# re-register existing datasets as new ones.

# Starting point:
# - One or more datasets as DarwinCore Archives or EML files in the current directory.
#   - This directory has an EML file, which is sufficient for a metadata-only dataset.
# - A web server exposing these files
#   - We can use the GitHub view of this directory for that:
ACCESS_ENDPOINT=https://raw.githubusercontent.com/gbif/registry/master/registry-examples/src/test/scripts
# - An organization registered in GBIF
# - An installation registered in GBIF (represents the server this script runs on)
# - A GBIF.org user account with publishing rights for the registered organization.
#   - These are the test values available for use on GBIF-UAT.org
ORGANIZATION=0a16da09-7719-40de-8d4f-56a15ed52fb6
INSTALLATION=92d76df5-3de1-4c89-be03-7a17abad962a
GBIF_USER=ws_client_demo
GBIF_PASSWORD=Demo123

# Loop through all the DWCA and EML files:

shopt -s extglob
for dataset_file in *.@(zip|eml) ; do

	# Guess dataset type (script doesn't handle checklists or sampling event datasets)
	case $dataset_file in
		*.eml)
			dataset_type=METADATA
			endpoint_type=EML
			;;
		*)
			dataset_type=OCCURRENCE
			endpoint_type=DWC_ARCHIVE
			;;
	esac

	# Check if the dataset is already registered -- we have a local file recording the UUID if that is the case.
	if [[ -e $dataset_file.registration ]]; then
		dataset=$(cat $dataset_file.registration)
		echo "Dataset $dataset_file is already registered at $dataset"
	else

		echo "Registering dataset $dataset_file"

		# Make a JSON object representing the minimum metadata necessary to register a dataset.  The rest of the metadata will
		# be added when GBIF.org retrieves the dataset for indexing.

		# The license isn't essential, but is very helpful to us if you can provide it (correctly) in the initial registration.

		# If using your own DOIs, add "doi": "10.xxxx/xxxx" to this JSON object.

		cat > $dataset_file.registration_json <<-EOF
		{
		  "publishingOrganizationKey": "$ORGANIZATION",
		  "installationKey": "$INSTALLATION",
		  "type": "$dataset_type",
		  "title": "Example dataset registration",
		  "description": "The dataset is registered with minimal metadata, which is overwritten once GBIF can access the file.",
		  "language": "eng",
		  "license": "http://creativecommons.org/publicdomain/zero/1.0/legalcode"
		}
		EOF

		# Send the request by HTTP:
		curl -Ssf --user $GBIF_USER:$GBIF_PASSWORD -H "Content-Type: application/json" -X POST --data @$dataset_file.registration_json https://api.gbif-uat.org/v1/dataset | tr -d '"' > $dataset_file.registration
		dataset=$(cat $dataset_file.registration)
	fi

	if [[ -e $dataset_file.endpoint ]]; then
		echo "	Endpoint is already set"
	else

		# Add an endpoint, the location GBIF.org will retrieve the archive (or EML) file from:
		cat > $dataset_file.endpoint_json <<-EOF
		{
			"type": "$endpoint_type",
			"url": "$ACCESS_ENDPOINT/$dataset_file"
		}
		EOF

		curl -Ssf --user $GBIF_USER:$GBIF_PASSWORD -H "Content-Type: application/json" -X POST --data @$dataset_file.endpoint_json https://api.gbif-uat.org/v1/dataset/$dataset/endpoint > $dataset_file.endpoint

		echo "Dataset registered, see https://www.gbif-uat.org/dataset/$dataset or https://api.gbif-uat.org/v1/dataset/$dataset"
	fi

done
