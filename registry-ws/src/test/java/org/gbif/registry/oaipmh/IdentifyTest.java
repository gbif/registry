package org.gbif.registry.oaipmh;

import com.lyncode.test.matchers.xml.XPathMatchers;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test the response for an OAI-PMH Identify query.
 */
public class IdentifyTest {

    private static final String OAI_NAMESPACE = "http://www.openarchives.org/OAI/2.0/";

    @Test
    public void testIdentify() throws IOException {

        OaipmhEndpoint oaipmhEndpoint = new OaipmhEndpoint();

        InputStream resultStream = oaipmhEndpoint.oaipmh("Identify");

        String result = IOUtils.toString(resultStream, "UTF-8");

        assertThat("Repository name element is present", result, xPath("//repositoryName", equalTo("GBIF Registry")));
        assertThat("Base URL element is set", result, xPath("//baseURL", equalTo("http://localhost")));
        assertThat("Admin email is set", result, xPath("//adminEmail", equalTo("admin@gbif.org")));
    }


    private Matcher<String> xPath(String xpath, Matcher<String> valueMatcher) {
        return XPathMatchers.xPath(xpath, valueMatcher, OAI_NAMESPACE);
    }
}
