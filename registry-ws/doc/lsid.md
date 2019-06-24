# LSIDs

We have inherited an LSID resolver from GRSciColl / biocol.org.

With a small amount of [Varnish configuration](https://github.com/gbif/c-deploy/blob/master/services/roles/varnish5/templates/api-default.vcl#L445) and some [static XML files](https://github.com/gbif/rs.gbif.org/tree/master/lsid) we can implement an LSID resolver.

It is difficult to test LSID resolution in Dev or UAT, since the domain name is part of the identifier.

The [script](resolve-lsid.sh) will make the minimal steps to resolve an LSID.

(The LSID "server" and resolution script aren't well-tested, it's possible I've missed a required part of the specification.)
