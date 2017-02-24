package org.gbif.identity.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for encoding and decoding the session token.
 */
public class SessionTokens {
  private static final int TICKET_LENGTH = 36; // chars in the UUID
  private static Base64.Decoder MIME_DECODER = Base64.getMimeDecoder();
  private static Base64.Encoder MIME_ENCODER = Base64.getMimeEncoder();

  public static String newSessionToken(String username) {
    String session = UUID.randomUUID().toString();
    String token = session + ":" + username;
    return MIME_ENCODER.encodeToString(token.getBytes());
  }

  public static String username(String sessionToken) {
    String decoded = new String(MIME_DECODER.decode(sessionToken.getBytes()));
    // must be length plus : plus at least a 1 char username
    if (decoded.length() > TICKET_LENGTH+2) {
      return decoded.substring(TICKET_LENGTH+1);
    } else {
      // deliberately vauge message as it implies tampering
      throw new IllegalArgumentException("Provided token is invalid");
    }
  }

  public static String session(String sessionToken) {
    String decoded = new String(MIME_DECODER.decode(sessionToken.getBytes()));
    if (decoded.length() > TICKET_LENGTH+2) {
      return decoded.substring(0, TICKET_LENGTH);
    } else {
      // deliberately vauge message as it implies tampering
      throw new IllegalArgumentException("Provided token is invalid");
    }
  }
}
