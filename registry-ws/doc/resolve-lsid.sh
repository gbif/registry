#!/bin/bash -e
#

lsid=urn:lsid:nmbe.ch:spidersp:021946
lsid=$1

domain=$(echo $lsid | cut -d: -f 3)
echo "The LSID is from the domain $domain"

# Skip asking lsid-authority.org (or whatever it was).

resolver=http://$(host -t srv _lsid._tcp.$domain | cut -d' ' -f 8):$(host -t srv _lsid._tcp.$domain | cut -d' ' -f 7)

echo "DNS SRV record tells us the resolver is at $resolver"

authority=$(curl -SsfL $resolver/authority/ | xmllint --xpath 'string(*[local-name()="definitions"]/*[local-name()="service"]/*[local-name()="port"]/*[local-name()="address"]/@location)' /dev/stdin)

echo "Querying the resolver tells is the authority is at $authority"
echo "LSID result: ($authority/authority/metadata/?lsid=$lsid)"
echo

curl -iL $authority/authority/metadata/\?lsid=$lsid
