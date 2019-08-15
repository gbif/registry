package org.gbif.ws.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.codec.digest.DigestUtils;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.server.RequestObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.gbif.ws.util.SecurityConstants.GBIF_SCHEME_PREFIX;
import static org.gbif.ws.util.SecurityConstants.HEADER_CONTENT_MD5;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_USER;
import static org.gbif.ws.util.SecurityConstants.HEADER_ORIGINAL_REQUEST_URL;

// TODO: 2019-08-12 refactor it, reduce total quantity of private methods, use standalone services instead
/**
 * The GBIF authentication scheme is modelled after the Amazon scheme on how to sign REST HTTP requests
 * using a private key. It uses the standard HTTP Authorization header to transport the following information:
 * Authorization: GBIF applicationKey:signature
 * <p>
 * <br/>
 * The header starts with the authentication scheme (GBIF), followed by the plain applicationKey (the public key)
 * and a unique signature for the very request which is generated using a fixed set of request attributes
 * which are then encrypted by a standard HMAC-SHA1 algorithm.
 * <p>
 * <br/>
 * A POST request with a GBIF header would look like this:
 *
 * <pre>
 * POST /dataset HTTP/1.1
 * Host: api.gbif.org
 * Date: Mon, 26 Mar 2007 19:37:58 +0000
 * x-gbif-user: trobertson
 * Content-MD5: LiFThEP4Pj2TODQXa/oFPg==
 * Authorization: GBIF gbif.portal:frJIUN8DYpKDtOLCwo//yllqDzg=
 * </pre>
 * <p>
 * When signing an HTTP request in addition to the Authorization header some additional custom headers are added
 * which are used to sign and digest the message.
 * <br/>
 * x-gbif-user is added to transport a proxied user in which the application is acting.
 * <br/>
 * Content-MD5 is added if a body entity exists.
 * See Content-MD5 header specs: http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.15
 */
@Service
public class GbifAuthServiceImpl implements GbifAuthService {

  private static final Logger LOG = LoggerFactory.getLogger(GbifAuthServiceImpl.class);

  private static final String ALGORITHM = "HmacSHA1";
  private static final char NEWLINE = '\n';
  private static final Pattern COLON_PATTERN = Pattern.compile(":");

  // TODO: 2019-07-31 it should be JacksonJsonContextResolver
  private final ObjectMapper mapper = new ObjectMapper();
  private final ImmutableMap<String, String> keyStore;

  public GbifAuthServiceImpl(AppkeysConfiguration appkeysConfiguration) {
    try {
      Properties props = PropertiesUtil.loadProperties(appkeysConfiguration.getFile());
      keyStore = Maps.fromProperties(props);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Property file path to application keys does not exist: " + appkeysConfiguration.getFile(), e);
    }
    LOG.info("Initialised appkey store with {} keys", keyStore.size());
  }

  /**
   * Extracts the information to be encrypted from a request and concatenates them into a single String.
   * When the server receives an authenticated request, it compares the computed request signature
   * with the signature provided in the request in StringToSign.
   * For that reason this string may only contain information also available in the exact same form to the server.
   *
   * @return unique string for a request
   * @see <a href="http://docs.amazonwebservices.com/AmazonS3/latest/dev/RESTAuthentication.html">AWS Docs</a>
   */
  private String buildStringToSign(final RequestObject request) {
    StringBuilder sb = new StringBuilder();

    sb.append(request.getMethod());
    sb.append(NEWLINE);
    // custom header set by varnish overrides real URI
    // see http://dev.gbif.org/issues/browse/GBIFCOM-137
    final HttpHeaders httpHeaders = request.getHttpHeaders();

    if (httpHeaders.containsKey(HEADER_ORIGINAL_REQUEST_URL)) {
      sb.append(httpHeaders.getFirst(HEADER_ORIGINAL_REQUEST_URL));
    } else {
      sb.append(getCanonicalizedPath(request.getRequestURI()));
    }

    appendHeader(sb, httpHeaders, HttpHeaders.CONTENT_TYPE, false);
    appendHeader(sb, httpHeaders, HEADER_CONTENT_MD5, true);
    appendHeader(sb, httpHeaders, HEADER_GBIF_USER, true);

    return sb.toString();
  }

  private void appendHeader(final StringBuilder sb, final HttpHeaders headers, final String header, final boolean caseSensitive) {
    if (headers.containsKey(header)) {
      sb.append(NEWLINE);
      if (caseSensitive) {
        sb.append(headers.getFirst(header));
      } else {
        sb.append(headers.getFirst(header).toLowerCase());
      }
    }
  }

  // TODO: 2019-07-26 mb get rid of this one?
  /**
   * @return an absolute uri of the resource path alone, excluding host, scheme and query parameters
   */
  private String getCanonicalizedPath(final String strUri) {
    return URI.create(strUri).normalize().getPath();
  }

  @Override
  public boolean isValidRequest(final RequestObject request) {
    // parse auth header
    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (Strings.isNullOrEmpty(authHeader) || !authHeader.startsWith(GBIF_SCHEME_PREFIX)) {
      LOG.info("{} header is no GBIF scheme", HttpHeaders.AUTHORIZATION);
      return false;
    }

    String[] values = COLON_PATTERN.split(authHeader.substring(5), 2);
    if (values.length < 2) {
      LOG.warn("Invalid syntax for application key and signature: {}", authHeader);
      return false;
    }

    final String appKey = values[0];
    final String signatureFound = values[1];
    if (appKey == null || signatureFound == null) {
      LOG.warn("Authentication header missing applicationKey or signature: {}", authHeader);
      return false;
    }

    final String secretKey = getPrivateKey(appKey);
    if (secretKey == null) {
      LOG.warn("Unknown application key: {}", appKey);
      return false;
    }
    //
    final String stringToSign = buildStringToSign(request);
    // sign
    final String signature = buildSignature(stringToSign, secretKey);
    // compare signatures
    if (signatureFound.equals(signature)) {
      LOG.debug("Trusted application with matching signatures");
      return true;
    }
    LOG.info("Invalid signature: {}", authHeader);
    LOG.debug("StringToSign: {}", stringToSign);
    return false;
  }

  /**
   * Generates a Base64 encoded HMAC-SHA1 signature of the passed in string with the given secret key.
   * See Message Authentication Code specs http://tools.ietf.org/html/rfc2104
   *
   * @param stringToSign the string to be signed
   * @param secretKey    the secret key to use in the
   */
  private String buildSignature(String stringToSign, String secretKey) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      SecretKeySpec secret = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
      mac.init(secret);
      byte[] digest = mac.doFinal(stringToSign.getBytes());

      return new String(Base64.getEncoder().encode(digest), StandardCharsets.US_ASCII);

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Cant find " + ALGORITHM + " message digester", e);
    } catch (InvalidKeyException e) {
      throw new RuntimeException("Invalid secret key " + secretKey, e);
    }
  }

  private String getPrivateKey(final String applicationKey) {
    return keyStore.get(applicationKey);
  }

  /**
   * Signs a request by adding a Content-MD5 and Authorization header.
   * For PUT/POST requests that contain a body entity the Content-MD5 header is created using the same
   * JSON mapper for serialization as the clients use.
   *
   * Other format than JSON are not supported currently !!!
   */
  @Override
  public RequestObject signRequest(final String username, final RequestObject request) {
    Preconditions.checkState(keyStore.size() == 1, "To sign the request a single application key is required");
    // first add custom GBIF headers so we can use them to build the string to sign
    // the proxied username
    request.getHttpHeaders().add(HEADER_GBIF_USER, username);

    // the canonical path header
    request.getHttpHeaders().add(HEADER_ORIGINAL_REQUEST_URL, getCanonicalizedPath(request.getRequestURI()));

    String content = null;
    if (request.getContent() != null) {
      content = request.getContent();
    }

    // adds content md5
    if (!Strings.isNullOrEmpty(content)) {
      request.getHttpHeaders().add(HEADER_CONTENT_MD5, buildContentMD5(content));
    }

    // build the unique string to sign
    final String stringToSign = buildStringToSign(request);
    // find private key for this app
    final String appKey = keyStore.keySet().iterator().next();
    final String secretKey = getPrivateKey(appKey);
    if (secretKey == null) {
      LOG.warn("Skip signing request with unknown application key: {}", appKey);
      return new RequestObject(request);
    }
    // sign
    final String signature = buildSignature(stringToSign, secretKey);

    // build authorization header string
    final String header = buildAuthHeader(appKey, signature);
    // add authorization header
    LOG.debug("Adding authentication header to request {} for proxied user {} : {}", request.getRequestURI(), username, header);
    request.getHttpHeaders().add(HttpHeaders.AUTHORIZATION, header);

    return request;
  }

  /**
   * Generates the Base64 encoded 128 bit MD5 digest of the entire content string suitable for the
   * Content-MD5 header value.
   * See http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.15
   */
  private String buildContentMD5(Object entity) {
    try {
      byte[] content = mapper.writeValueAsBytes(entity);

      // TODO: 2019-07-31 char encoding should be ASCII
      return Base64.getEncoder().encodeToString(DigestUtils.md5(content));
    } catch (IOException e) {
      LOG.error("Failed to serialize http entity [{}]", entity);
      throw Throwables.propagate(e);
    }
  }

  private static String buildAuthHeader(String applicationKey, String signature) {
    return GBIF_SCHEME_PREFIX + applicationKey + ':' + signature;
  }
}
