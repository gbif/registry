#!/usr/bin/env bash
#
# Calculate the statistics for the "download_statistics" table.
#
# TODO: This needs to be run when the (UTC) month changes.
#
# e.g. ./populate_downloaded_records_statistics.sh | psql -h pg1.gbif-dev.org -U registry dev_registry -a
#

set -e

echo "TRUNCATE TABLE download_statistics;"
echo

for year in {2013..2018}; do
  for month in {1..12}; do
    nextYear=$year
    nextMonth=$((month + 1))
    if [[ $nextMonth = "13" ]]; then
      nextYear=$(($year + 1))
      nextMonth=01
    fi

	cat <<SQL
INSERT INTO download_statistics (year_month, publishing_organization_country, dataset_key, user_country, count) (
  SELECT date_trunc('month', oc.created AT TIME ZONE 'UTC') AT TIME ZONE 'UTC' AS year_month, COALESCE(o.country,'ZZ') AS publishing_organization_country, dod.dataset_key, COALESCE(u.settings->'country','ZZ') AS user_country, SUM(dod.number_records) AS count
  FROM dataset_occurrence_download dod
  JOIN occurrence_download oc ON oc.key = dod.download_key AND (oc.status = 'SUCCEEDED' or oc.status = 'FILE_ERASED')
  AND oc.created AT TIME ZONE 'UTC' >= '$year-$month-01' AND oc.created AT TIME ZONE 'UTC' < '$nextYear-$nextMonth-01'
  JOIN "user" u ON oc.created_by = u.username
  JOIN dataset d ON dod.dataset_key = d.key
  JOIN organization o ON d.publishing_organization_key = o.key
  GROUP BY year_month, u.settings -> 'country', dod.dataset_key, o.country ORDER BY year_month, publishing_organization_country, dataset_key, user_country
);
SQL
	sleep 5
	echo
  done
done
