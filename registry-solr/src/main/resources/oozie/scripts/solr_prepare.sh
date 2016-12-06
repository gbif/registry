SOLR_HOME=$1
SOLR_HTTP_URL=$2
ZK_HOST=$3
SOLR_COLLECTION=$4
SOLR_COLLECTION_OPTS=$5


SOLR_COLLECTION_TODAY=$SOLR_COLLECTION"_"$(date +"%Y_%m_%d")

curl -s """$SOLR_HTTP_URL"/admin/collections?action=DELETE\&name="$SOLR_COLLECTION_TODAY"""

$SOLR_HOME/server/scripts/cloud-scripts/zkcli.sh  -zkhost $ZK_HOST -cmd upconfig -confname $SOLR_COLLECTION -confdir solr/dataset/conf/

curl -s """$SOLR_HTTP_URL"/admin/collections?action=CREATE\&name="$SOLR_COLLECTION_TODAY"\&"$SOLR_COLLECTION_OPTS"\&collection.configName="$SOLR_COLLECTION_TODAY"""

echo "collectionToday=$SOLR_COLLECTION_TODAY"
