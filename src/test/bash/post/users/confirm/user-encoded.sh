# Confirms the user/pw of a user
source `dirname $0`/../../../functions.sh POST $*

curl $copts -X POST -H 'Accept: application/json' -H "Authorization:Basic $(echo -n $EXCHANGE_ORG/$EXCHANGE_USER:$EXCHANGE_PW | base64)" $EXCHANGE_URL_ROOT/v1/orgs/$EXCHANGE_ORG/users/$EXCHANGE_USER/confirm | $parse
