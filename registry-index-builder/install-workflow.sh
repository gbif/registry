#exit on any failure
set -e

#!/bin/bash
P=$1
TOKEN=$2

echo "Getting latest registry-index-builder workflow properties file from github"
curl -s -H "Authorization: token $TOKEN" -H 'Accept: application/vnd.github.v3.raw' -O -L https://api.github.com/repos/gbif/gbif-configuration/contents/registry-index-builder/$P.properties

#extract the oozie.url value from the properties file
oozie_url=`cat $P.properties| grep "oozie.url" | cut -d'=' -f2`

echo "Assembling jar for $ENV"

mvn -Poozie,$P clean package -DskipTests assembly:single
mvn -Psolr,$P package -DskipTests assembly:single

if hdfs dfs -test -d /registry-index-builder-$P/; then
   echo "Removing content of current Oozie workflow directory"
   hdfs dfs -rm -f -r /registry-index-builder-$P/*
else
   echo "Creating workflow directory"
   hdfs dfs -mkdir /registry-index-builder-$P/
fi
echo "Copying new Oozie workflow to HDFS"
hdfs dfs -copyFromLocal target/oozie-workflow/* /registry-index-builder-$P/
hdfs dfs -copyFromLocal $P.properties /registry-index-builder-$P/lib/

echo "Executing Oozie workflow"
oozie job --oozie ${oozie_url} -config $P.properties -run

