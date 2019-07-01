package org.gbif.registry.identity.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * A Java port of password encoding as done natively by Drupal 7.
 * <p/>
 * A password is structured as:
 * <pre>
 *   $S$<iterations><salt><encoded>
 * </pre>
 *
 * Where:
 * <ul>
 *   <li>iterations is a based 64 encoded number of loops to apply the hashing algorithm</li>
 *   <li>salt is an 8 character random string</li>
 *   <li>encoded is the the final encoded hash of the password using SHA-512 encoding applied iterations times and
 *   with the salt key.  The final encoded is truncated to the length provided in the constructor</li>
 * </ul>
 *
 * Mostly this code is copied from http://stackoverflow.com/questions/11736555/java-autentication-of-drupal-passwords
 */
public class PasswordEncoder {

  private static final Logger LOG = LoggerFactory.getLogger(PasswordEncoder.class);
  private static final String ALGORITHM = "SHA-512";
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String PASSWORD_ITOA64 = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

  private final int encodedHashLength;

  public PasswordEncoder() {
    encodedHashLength = 55;
  }

  public PasswordEncoder(int hashLength) {
    encodedHashLength = hashLength;
  }

  /**
   * Reads the iteration count out of the encoded settings.
   */
  private static int passwordGetCountLog2(String settings) {
    return PASSWORD_ITOA64.indexOf(settings.charAt(3));
  }

  /**
   * Encode using the algorithm.
   */
  private static byte[] sha512(String input) {
    return sha512(input.getBytes());
  }

  /**
   * Encode using the algorithm.
   */
  private static byte[] sha512(byte[] input) {
    try {
      return MessageDigest.getInstance(ALGORITHM).digest(input);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Missing required message digest algorithm " +  ALGORITHM);
    }
  }

  /**
   * Encodes the password using a random salt.
   * @param password to encode
   * @return the encoded password which will have a random salt
   */
  public String encode(String password) {
    String settingsHash = randomSalt();
    return encode(password, settingsHash);
  }

  /**
   * Encodes password using the settings and and salt from the provided encoded password.
   * @param preEncoded the pre-encoded version storing individual hashing settings in its first 12 chars.
   *
   * @return the encoded password using the existing hash settings or null on error
   */
  public String encode(final String password, String preEncoded) {
    // The first 12 characters of an existing hash are its setting string.
    preEncoded = preEncoded.substring(0, 12);
    int count_log2 = passwordGetCountLog2(preEncoded);
    String salt = preEncoded.substring(4, 12);
    // Hashes must have an 8 character salt.
    if (salt.length() != 8) {
      return null;
    }

    int count = 1 << count_log2;

    byte[] hash;
    try {
      hash = sha512(salt.concat(password));

      do {
        hash = sha512(joinBytes(hash, password.getBytes("UTF-8")));
      } while (--count > 0);
    } catch (Exception e) {
      LOG.error("Unable to encode the password", e);
      return null;
    }

    String output = preEncoded + base64Encode(hash, hash.length);
    return (output.length() > 0) ? output.substring(0, encodedHashLength) : null;
  }

  /**
   * Joins the byte arrays into a new array, sized to fit.
   */
  private static byte[] joinBytes(byte[] a, byte[] b) {
    byte[] combined = new byte[a.length + b.length];

    System.arraycopy(a, 0, combined, 0, a.length);
    System.arraycopy(b, 0, combined, a.length, b.length);
    return combined;
  }

  /**
   * Encodes the input using some smarts.
   * Understanding those smarts is an exercise left to the reader.
   * @see http://stackoverflow.com/questions/11736555/java-autentication-of-drupal-passwords
   */
  private static String base64Encode(byte[] input, int count) {

    StringBuilder output = new StringBuilder();
    int i = 0;
    CharSequence itoa64 = PASSWORD_ITOA64;
    do {
      long value = SignedByteToUnsignedLong(input[i++]);

      output.append(itoa64.charAt((int) value & 0x3f));
      if (i < count) {
        value |= SignedByteToUnsignedLong(input[i]) << 8;
      }
      output.append(itoa64.charAt((int) (value >> 6) & 0x3f));
      if (i++ >= count) {
        break;
      }
      if (i < count) {
        value |= SignedByteToUnsignedLong(input[i]) << 16;
      }

      output.append(itoa64.charAt((int) (value >> 12) & 0x3f));
      if (i++ >= count) {
        break;
      }
      output.append(itoa64.charAt((int) (value >> 18) & 0x3f));
    } while (i < count);

    return output.toString();
  }

  /**
   * Clears any sign bit on the given byte.
   */
  public static long SignedByteToUnsignedLong(byte b) {
    return b & 0xFF;
  }

  /**
   * Returns a random 8 character salt prefixed with "$S$D" (which is what Drupal 7 did).
   */
  private static String randomSalt() {
    // drupal uses 8 character salts, prefixed with $S$D so we copy that
    StringBuilder sb = new StringBuilder(11);
    sb.append("$S$D");
    for( int i = 0; i < 8; i++ ) {
      sb.append(PASSWORD_ITOA64.charAt(RANDOM.nextInt(PASSWORD_ITOA64.length())));
    }
    return sb.toString();
  }
}
