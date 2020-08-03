/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.metasync.protocols;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.vocabulary.Language;
import org.gbif.common.parsers.LicenseParser;
import org.gbif.registry.metasync.api.ErrorCode;
import org.gbif.registry.metasync.api.MetadataException;
import org.gbif.registry.metasync.api.MetadataProtocolHandler;
import org.gbif.registry.metasync.util.converter.DateTimeConverter;
import org.gbif.registry.metasync.util.converter.LanguageConverter;
import org.gbif.registry.metasync.util.converter.PeriodConverter;
import org.gbif.registry.metasync.util.converter.UriConverter;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.annotations.FromAnnotationsRuleModule;
import org.apache.commons.digester3.binder.DigesterLoader;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.google.common.collect.Lists;

import static org.apache.commons.digester3.binder.DigesterLoader.newLoader;

/**
 * This class is used as a base for all protocols and provides common methods to be used by all
 * protocols.
 */
public abstract class BaseProtocolHandler implements MetadataProtocolHandler {

  private static final Logger LOG = LoggerFactory.getLogger(BaseProtocolHandler.class);
  private final HttpClient httpClient;
  private final LicenseParser licenseParser;

  protected BaseProtocolHandler(HttpClient httpClient) {
    this.httpClient = httpClient;
    this.licenseParser = LicenseParser.getInstance();
  }

  /**
   * Takes two collections of {@link Contact}s and tries to match the {@code newContacts} to the
   * {@code existingContacts} and returns all Contacts that could <b>not</b> be matched (i.e. those
   * are new Contacts that did not previously exist).
   *
   * @return new Contacts that do not exist in the existing Contacts already
   */
  protected List<Contact> matchContacts(
      Iterable<Contact> existingContacts, Iterable<Contact> newContacts) {
    List<Contact> resultList = Lists.newArrayList();
    for (Contact newContact : newContacts) {
      boolean found = false;
      for (Contact existingContact : existingContacts) {
        if (existingContact.lenientEquals(newContact)) {
          resultList.add(existingContact);
          found = true;
          break;
        }
      }

      if (!found) {
        resultList.add(newContact);
      }
    }

    return resultList;
  }

  /**
   * Makes a HTTP request to the provided {@link URI} and uses the {@link Digester} to parse the
   * response.
   *
   * @param uri to issue request against
   * @param digester to parse response with
   * @param <T> type of Object to return
   * @throws MetadataException in case anything goes wrong during the request, all underlying HTTP
   *     errors are mapped to the appropriate {@link ErrorCode}s.
   */
  protected <T> T doHttpRequest(URI uri, Digester digester) throws MetadataException {
    LOG.info("Issuing request: {}", uri);
    HttpUriRequest get = new HttpGet(uri);
    HttpResponse response;
    try {
      // Not using a Response Handler here because that can't throw arbitrary Exceptions
      response = httpClient.execute(get);
    } catch (ClientProtocolException e) {
      throw new MetadataException(e, ErrorCode.HTTP_ERROR);
    } catch (IOException e) {
      throw new MetadataException(e, ErrorCode.IO_EXCEPTION);
    }

    try {
      // Everything but HTTP status 200 is an error
      if (response.getStatusLine().getStatusCode() != 200) {
        LOG.debug(
            "Received HTTP code[{}] cause[{}] for request: {}",
            response.getStatusLine().getStatusCode(),
            response.getStatusLine().getReasonPhrase(),
            uri);
        String cause =
            String.format(
                "Received HTTP code[%d], phrase[%s]",
                response.getStatusLine().getStatusCode(),
                response.getStatusLine().getReasonPhrase());
        throw new MetadataException(cause, ErrorCode.HTTP_ERROR);
      }

      return digester.parse(response.getEntity().getContent());
    } catch (SAXException e) {
      throw new MetadataException(e, ErrorCode.PROTOCOL_ERROR);
    } catch (IOException e) {
      throw new MetadataException(e, ErrorCode.IO_EXCEPTION);
    } finally {
      try {
        EntityUtils.consume(response.getEntity());
      } catch (IOException e2) {
        LOG.warn("Error consuming content after an exception", e2);
      }
    }
  }

  /**
   * Returns a new Digester which is configured with the annotation rules from the passed in class.
   */
  protected Digester newDigester(final Class<?> clazz) {
    DigesterLoader loader =
        newLoader(
            new FromAnnotationsRuleModule() {

              @Override
              protected void configureRules() {
                bindRulesFrom(clazz);
              }
            });

    loader.setNamespaceAware(true);
    ConvertUtils.register(new DateTimeConverter(), DateTime.class);
    ConvertUtils.register(new LanguageConverter(), Language.class);
    ConvertUtils.register(new PeriodConverter(), Period.class);
    ConvertUtils.register(new UriConverter(), URI.class);
    return loader.newDigester();
  }

  /** @return instance of LicenseParser */
  protected LicenseParser getLicenseParser() {
    return licenseParser;
  }
}
