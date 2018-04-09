#!/bin/bash -eu
#
# This is an outline Bash script to show updating a set of datasets registered with GBIF.org.
#
# The script isn't recommended for production use; it shows the steps involved, but does not have enough error checking.
#
# Using a process like this, you should make sure you store the UUID GBIF assigns to your dataset, so you don't accidentally re-register
# existing datasets as new ones.

# Starting point:
# - One or more datasets as DarwinCore Archives or EML files in the current directory.
# - A web server exposing these files
ACCESS_ENDPOINT=https://raw.githubusercontent.com/gbif/registry/master/registry-examples/src/test/scripts
# - An organization registered in GBIF
ORGANIZATION=0a16da09-7719-40de-8d4f-56a15ed52fb6
# - An installation registered in GBIF (represents the server this script runs on)
INSTALLATION=92d76df5-3de1-4c89-be03-7a17abad962a
# - A GBIF.org user account with publishing rights for the registered organization.
GBIF_USER=ws_client_demo
GBIF_PASSWORD=Demo123

# Loop through all the DWCA and EML files:

shopt -s extglob
for dataset_file in *.@(zip|eml) ; do

	# Check if the dataset is already registered.
	if [[ -e $dataset_file.registration ]]; then
		dataset=$(cat $dataset_file.registration)
		echo "Dataset $dataset_file is already registered at $dataset"
	else

		# Guess dataset type (script doesn't handle checklists)
		case $dataset_file in
			*.eml)
				dataset_type=METADATA
				;;
			*)
				dataset_type=OCCURRENCE
				;;
		esac

		echo "Registering dataset $dataset_file"

		# Make a JSON object representing the minimum metadata necessary to register a dataset.  The rest of the metadata will
		# be added when GBIF.org retrieves the dataset for indexing.

		cat > $dataset_file.registration_json <<-EOF
		{
		  "publishingOrganizationKey": "$ORGANIZATION",
		  "installationKey": "$INSTALLATION",
		  "type": "$dataset_type",
		  "title": "Example dataset registration",
		  "description": "The dataset is registered with minimal metadata, which is overwritten once GBIF can access the file.",
		  "language": "eng"
		}
		EOF

		# Send the request by HTTP:
		curl -Ss --user $GBIF_USER:$GBIF_PASSWORD -H "Content-Type: application/json" -X POST --data @$dataset_file.registration_json https://api.gbif-uat.org/v1/dataset | tr -d '"' > $dataset_file.registration
		dataset=$(cat $dataset_file.registration)
	fi

	if [[ -e $dataset_file.endpoint ]]; then
		echo "	Endpoint is already set"
	else

		# Add an endpoint, the location GBIF.org will retrieve the archive file from:
		cat > $dataset_file.endpoint_json <<-EOF
		{
			"type": "DWC_ARCHIVE",
			"url": "$ACCESS_ENDPOINT/$dataset_file"
		}
		EOF

		curl -Ss --user $GBIF_USER:$GBIF_PASSWORD -H "Content-Type: application/json" -X POST --data @$dataset_file.endpoint_json https://api.gbif-uat.org/v1/dataset/$dataset/endpoint > $dataset_file.endpoint

		echo "Dataset registered, see https://www.gbif-uat.org/dataset/$dataset or https://api.gbif-uat.org/v1/dataset/$dataset"
	fi

done
