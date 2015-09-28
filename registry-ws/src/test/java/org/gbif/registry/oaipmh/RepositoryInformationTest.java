package org.gbif.registry.oaipmh;

import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.DoiMapper;

import com.google.inject.Injector;
import com.lyncode.test.matchers.xml.XPathMatchers;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Test the OAI-PMH queries used to get information about the repository.
 * Test the responses for OAI-PMH Identify and ListMetadataFormats queries.
 */
public class RepositoryInformationTest {

  private static final String OAI_NAMESPACE = "http://www.openarchives.org/OAI/2.0/";

  private DatasetMapper mapper;

  @Before
  public void setup() {
    Injector inj = RegistryTestModules.mybatis();
    mapper = inj.getInstance(DatasetMapper.class);
  }

  @Test
  public void testIdentify() throws IOException {

    OaipmhEndpoint oaipmhEndpoint = new OaipmhEndpoint(null, new OaipmhSetRepository(mapper));

    InputStream resultStream = oaipmhEndpoint.oaipmh("Identify", null, null, null, null, null);

    String result = IOUtils.toString(resultStream, "UTF-8");

    assertThat("Repository name element is present", result, xPath("//repositoryName", equalTo("GBIF Registry")));
    assertThat("Base URL element is set", result, xPath("//baseURL", equalTo("http://localhost")));
    assertThat("Admin email is set", result, xPath("//adminEmail", equalTo("admin@gbif.org")));
  }

  @Test
  public void testListMetadataFormats() throws IOException {

    OaipmhEndpoint oaipmhEndpoint = new OaipmhEndpoint(null, new OaipmhSetRepository(mapper));

    InputStream resultStream = oaipmhEndpoint.oaipmh("ListMetadataFormats", null, null, null, null, null);

    String result = IOUtils.toString(resultStream, "UTF-8");

    assertThat(result, xPath("count(//metadataFormat)", asInteger(equalTo(2))));
    assertThat("oai_dc metadata prefix is present", result, xPath("//metadataFormat[1]/metadataPrefix", equalTo("oai_dc")));
    assertThat("eml metadata prefix is present", result, xPath("//metadataFormat[2]/metadataPrefix", equalTo("eml")));
  }

  private Matcher<String> xPath(String xpath, Matcher<String> valueMatcher) {
    return XPathMatchers.xPath(xpath, valueMatcher, OAI_NAMESPACE);
  }

  /**
   * A Matcher to match a String with a Matcher<Integer>
   *
   * @param matcher
   * @return
   */
  private Matcher<String> asInteger(final Matcher<Integer> matcher) {
    return new TypeSafeMatcher<String>() {
      @Override
      protected boolean matchesSafely(String item) {
        return matcher.matches(Integer.valueOf(item));
      }

      @Override
      public void describeTo(Description description) {
        description.appendDescriptionOf(matcher);
      }
    };
  }
}
