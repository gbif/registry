package org.gbif.ws.security;

public interface SigningService {

  String buildSignature(String stringToSign, String secretKey);
}
