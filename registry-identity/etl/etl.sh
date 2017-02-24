#!/usr/bin/env bash -e

# Source MySQL DB
source_host=$1
source_username=$2
source_password=$3
source_db=$4

# Target MySQL DB
target_host=$5
target_username=$6
target_password=$7
target_db=$8


# Extract and transform
echo Exporting from $source_host:$source_db
START_TIME=$SECONDS
mysql -N -p$source_password --default-character-set=utf8 -u $source_username -h $source_host $source_db < ./transform.sql
mysql -N -p$source_password --default-character-set=utf8 -u $source_username -h $source_host $source_db -e "SELECT * FROM tmp_export1" > ./updates.sql
mysql -N -p$source_password --default-character-set=utf8 -u $source_username -h $source_host $source_db -e "SELECT * FROM tmp_export2" > ./inserts.sql
ELAPSED_TIME=$(($SECONDS - $START_TIME))
echo Exported in $ELAPSED_TIME seconds

# Load
# TODO: add the password with a -W$source_password
echo Loading into $target_host:$target_db
START_TIME=$SECONDS
PGOPTIONS='--client-min-messages=warning' psql -q -h $target_host -U$target_username $target_db <  ./updates.sql
ELAPSED_TIME=$(($SECONDS - $START_TIME))
echo Updates applied in $ELAPSED_TIME seconds
START_TIME=$SECONDS
PGOPTIONS='--client-min-messages=warning' psql -q -h $target_host -U$target_username $target_db <  ./inserts.sql
ELAPSED_TIME=$(($SECONDS - $START_TIME))
echo New accounts inserted in $ELAPSED_TIME seconds
