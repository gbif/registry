SOLR_HOME=$1
SOLR_HTTP_URL=$2
ZK_HOST=$3
SOLR_COLLECTION=$4
SOLR_COLLECTION_OPTS=$5


SOLR_COLLECTION_TODAY=$SOLR_COLLECTION"_"$(date +"%Y_%m_%d")

curl -s """$SOLR_HTTP_URL"/admin/collections?action=DELETE\&name="$SOLR_COLLECTION_TODAY"""

$SOLR_HOME/server/scripts/cloud-scripts/zkcli.sh  -zkhost $ZK_HOST -cmd upconfig -confname $SOLR_COLLECTION_TODAY -confdir solr/dataset/conf/

curl -s """$SOLR_HTTP_URL"/admin/collections?action=CREATE\&name="$SOLR_COLLECTION_TODAY"\&"$SOLR_COLLECTION_OPTS"\&collection.configName="$SOLR_COLLECTION_TODAY"""

echo "collectionToday=$SOLR_COLLECTION_TODAY"






SOLR_HOME=/opt/cloudera/parcels/SOLR5/
SOLR_HTTP_URL=http://uatsolr-vh.gbif.org:8983/solr
ZK_HOST="prodmaster1-vh.gbif.org:2181,prodmaster2-vh.gbif.org:2181,prodmaster3-vh.gbif.org:2181/uatsolr"
SOLR_COLLECTION=registry_ng
SOLR_COLLECTION_OPTS="numShards=1&replicationFactor=1&maxShardsPerNode=1"


$SOLR_HOME/server/scripts/cloud-scripts/zkcli.sh  -zkhost $ZK_HOST -cmd upconfig -confname $SOLR_COLLECTION -confdir ./conf/
curl -s """$SOLR_HTTP_URL"/admin/collections?action=DELETE\&name="$SOLR_COLLECTION"""
curl -s """$SOLR_HTTP_URL"/admin/collections?action=CREATE\&name="$SOLR_COLLECTION"\&"$SOLR_COLLECTION_OPTS"\&collection.configName="$SOLR_COLLECTION"""


http://uatsolr02-vh.gbif.org:8983/solr/#/registry_ng_shard1_replica1/analysis?analysis.fieldname=title&verbose_output=1

http://uatsolr02-vh.gbif.org:8983/solr/registry_ng/dataimport?command=reload-config
http://uatsolr02-vh.gbif.org:8983/solr/registry_ng/dataimport?command=status
http://uatsolr02-vh.gbif.org:8983/solr/registry_ng/dataimport?command=full-import&clean=false

http://uatsolr02-vh.gbif.org:8983/solr/admin/collections?action=RELOAD&name=registry_ng