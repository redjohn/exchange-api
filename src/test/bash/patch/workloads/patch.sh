# Updates a microservice
source `dirname $0`/../../functions.sh PATCH $*

curl $copts -X PATCH -H 'Content-Type: application/json' -H 'Accept: application/json' -H "Authorization:Basic $EXCHANGE_ORG/$EXCHANGE_USER:$EXCHANGE_PW" -d '{
  "downloadUrl": "this is now patched"
}' $EXCHANGE_URL_ROOT/v1/orgs/$EXCHANGE_ORG/workloads/bluehorizon.network-workloads-netspeed_1.0.0_amd64 | $parse
