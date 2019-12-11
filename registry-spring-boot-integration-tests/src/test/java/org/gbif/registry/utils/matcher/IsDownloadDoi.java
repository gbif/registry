package org.gbif.registry.utils.matcher;

import org.gbif.api.model.common.DOI;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class IsDownloadDoi extends TypeSafeMatcher<String> {

  @Override
  protected boolean matchesSafely(String s) {
    if (DOI.isParsable(s)) {
      DOI doi = new DOI(s);
      return doi.getPrefix().equals(DOI.TEST_PREFIX) && doi.getSuffix().startsWith("dl");
    }

    return false;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("Download DOI");
  }
}
