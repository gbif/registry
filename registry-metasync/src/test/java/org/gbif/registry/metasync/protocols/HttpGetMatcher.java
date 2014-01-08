package org.gbif.registry.metasync.protocols;

import org.apache.http.client.methods.HttpGet;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class HttpGetMatcher extends TypeSafeMatcher<HttpGet> {

  private final String urlToMatch;

  public HttpGetMatcher(String urlToMatch) {
    this.urlToMatch = urlToMatch;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("url does not match");
  }

  @Override
  public boolean matchesSafely(HttpGet item) {
    return item.getURI().toASCIIString().equals(urlToMatch);
  }

  public static Matcher<HttpGet> matchUrl(String url) {
    return new HttpGetMatcher(url);
  }

}
