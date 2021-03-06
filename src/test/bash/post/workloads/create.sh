# Adds a workload
source `dirname $0`/../../functions.sh POST '' $*

curl $copts -X POST -H 'Content-Type: application/json' -H 'Accept: application/json' -H "Authorization:Basic $EXCHANGE_ORG/$EXCHANGE_USER:$EXCHANGE_PW" -d '{
  "label": "Location for x86_64",
  "description": "blah blah",
  "public": true,
  "workloadUrl": "https://bluehorizon.network/workloads/netspeed2",
  "version": "1.0.0",
  "arch": "amd64",
  "downloadUrl": "",
  "apiSpec": [
    {
      "specRef": "https://bluehorizon.network/microservices/network",
      "org": "IBM",
      "version": "2.0.0",
      "arch": "amd64"
    },
    {
      "specRef": "https://bluehorizon.network/microservices/rtlsdr",
      "org": "IBM",
      "version": "1.0.0",
      "arch": "amd64"
    }
  ],
  "userInput": [],
  "workloads": []
}' $EXCHANGE_URL_ROOT/v1/orgs/$EXCHANGE_ORG/workloads | $parse
