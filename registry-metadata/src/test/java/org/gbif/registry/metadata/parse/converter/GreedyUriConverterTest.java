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
package org.gbif.registry.metadata.parse.converter;

import java.net.URI;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GreedyUriConverterTest {

  private static final GreedyUriConverter CONVERTER = new GreedyUriConverter();

  @Test
  public void testConvertToType() throws Throwable {
    // Malformed URLs, including unrecognized protocols
    URI uri = (URI) CONVERTER.convertToType(URI.class, "- - - ");
    assertNull(uri);
    // test a very complete uri
    uri =
        (URI)
            CONVERTER.convertToType(
                URI.class, "http://gbif.org:8080/my/path/search?q=abies&rank=species#images");
    assertEquals("images", uri.getFragment());
    assertEquals("http", uri.getScheme());
    // test missing scheme
    uri =
        (URI)
            CONVERTER.convertToType(
                URI.class, "www.gbif.org:8080/my/path/search?q=abies&rank=species#images");
    assertEquals("images", uri.getFragment());
    assertEquals("http", uri.getScheme());

    uri = (URI) CONVERTER.convertToType(URI.class, "//**##");
    assertNull(uri);
    uri = (URI) CONVERTER.convertToType(URI.class, "      ");
    assertNull(uri);
    uri = (URI) CONVERTER.convertToType(URI.class, "torrent://www.gbif.org");
    assertEquals("torrent", uri.getScheme());

    // Invalid URL with recognized protocols
    uri = (URI) CONVERTER.convertToType(URI.class, "ftp://ftp.gbif.org //h");
    assertNull(uri);

    // Conversion here, to greedily capture such URLs starting with www - otherwise they are
    // discarded
    uri = (URI) CONVERTER.convertToType(URI.class, "www.gbif.org");
    assertNotNull(uri);
    assertEquals(URI.create("http://www.gbif.org"), uri);
    uri = (URI) CONVERTER.convertToType(URI.class, "ftp://ftp.gbif.org");
    assertNotNull(uri);
    assertEquals("ftp", uri.getScheme());
    uri = (URI) CONVERTER.convertToType(URI.class, "http://www.gbif.org");
    assertNotNull(uri);
    assertEquals("http", uri.getScheme());
    uri = (URI) CONVERTER.convertToType(URI.class, "https://www.gbif.org");
    assertNotNull(uri);
    assertEquals("https", uri.getScheme());

    assertEquals("http://www.wii.gov.in", CONVERTER.convert("http://www.wii.gov.in").toString());
    assertEquals("http://www.wii.gov.in", CONVERTER.convert("www.wii.gov.in").toString());
    assertEquals(
        "http://www.wii.gov.in/hello?q=world",
        CONVERTER.convert(" www.wii.gov.in/hello?q=world").toString());
    assertEquals(
        "https://ftp.gbif.org/mine.zip",
        CONVERTER.convert("https://ftp.gbif.org/mine.zip").toString());
  }
}
