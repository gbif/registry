#!/usr/bin/env bash
#
# Calculate the statistics for the "download_statistics" table.
#
# TODO: This needs to be run when the (UTC) month changes.
#
# e.g. ./populate_downloaded_records_statistics.sh | psql -h pg1.gbif-dev.org -U registry dev_registry -a
#

set -e

#echo "TRUNCATE TABLE download_statistics;"
#echo "TRUNCATE TABLE download_user_statistics;"
echo "SET TIME ZONE 'UTC';"
echo

year=2018
month=1

endYear=$(date +%Y -d '1 month ago')
endMonth=$(date +%m -d '1 month ago')

while [[ $year -le $endYear ]]; do
	if [[ $year -eq $endYear ]]; then
		endM=$endMonth
	else
		endM=12
	fi

	while [[ ($month -le $endM) ]]; do

		nextYear=$year
		nextMonth=$((month + 1))
		if [[ $nextMonth = "13" ]]; then
			nextYear=$(($year + 1))
			nextMonth=1
		fi

		echo >&2 "$year-$month-01 to $nextYear-$nextMonth-01"

		cat <<SQL
DO
\$do\$
BEGIN
IF NOT EXISTS (SELECT year_month FROM download_statistics WHERE year_month = '$year-$month-01') THEN
  INSERT INTO download_statistics (year_month, publishing_organization_country, dataset_key, total_records, number_downloads, type) (
    SELECT date_trunc('month', oc.created) AS year_month, COALESCE(o.country,'ZZ') AS publishing_organization_country, dod.dataset_key, SUM(dod.number_records) AS total_records, COUNT(dod.download_key) AS number_downloads, oc.type AS type
      FROM dataset_occurrence_download dod
      JOIN occurrence_download oc ON oc.key = dod.download_key AND oc.status IN ('SUCCEEDED','FILE_ERASED')
      AND oc.created >= '$year-$month-01' AND oc.created < '$nextYear-$nextMonth-01'
      JOIN dataset d ON dod.dataset_key = d.key
      JOIN organization o ON d.publishing_organization_key = o.key
      GROUP BY year_month, dod.dataset_key, o.country, oc.type ORDER BY dataset_key, publishing_organization_country
  );
END IF;
END
\$do\$;
SQL
		sleep 2
		echo

		cat <<SQL
DO
\$do\$
BEGIN
IF NOT EXISTS (SELECT year_month FROM download_user_statistics WHERE year_month = '$year-$month-01') THEN
  INSERT INTO download_user_statistics (year_month, user_country, total_records, number_downloads, type) (
    SELECT date_trunc('month', oc.created) AS year_month, COALESCE(u.settings->'country','ZZ') AS user_country, SUM(oc.total_records) AS total_records, COUNT(oc.key) AS number_downloads, oc.type AS type
      FROM occurrence_download oc
      JOIN "user" u ON oc.created_by = u.username
      WHERE oc.status IN ('SUCCEEDED','FILE_ERASED')
      AND oc.created >= '$year-$month-01' AND oc.created < '$nextYear-$nextMonth-01'
      GROUP BY year_month, user_country, oc.type ORDER BY year_month, user_country
  );
END IF;
END
\$do\$;
SQL
		sleep 2
		echo

		cat <<SQL
DO
\$do\$
BEGIN
IF NOT EXISTS (SELECT year_month FROM download_source_statistics WHERE year_month = '$year-$month-01') THEN
  INSERT INTO download_source_statistics (year_month, source, total_records, number_downloads, type) (
    SELECT date_trunc('month', oc.created) AS year_month, oc.source, SUM(oc.total_records) AS total_records, COUNT(oc.key) AS number_downloads, oc.type AS type
      FROM occurrence_download oc
      WHERE oc.status IN ('SUCCEEDED','FILE_ERASED')
      AND oc.created >= '$year-$month-01' AND oc.created < '$nextYear-$nextMonth-01'
      GROUP BY year_month, oc.source, oc.type ORDER BY year_month, oc.source
  );
END IF;
END
\$do\$;
SQL
		sleep 2
		echo

		month=$(($month + 1))
	done
	year=$(($year + 1))
	month=1

done
