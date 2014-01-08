package org.gbif.registry.ws.guice;

import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Dataset;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class StringTrimInterceptorTest {

  private static final StringTrimInterceptor TRIMMER = new StringTrimInterceptor();

  @Test
  public void test() {
    Dataset dataset = new Dataset();
    dataset.setTitle("   ");
    TRIMMER.trimStringsOf(dataset);
    assertEquals("Dataset title should be null", null, dataset.getTitle());

    Citation citation = new Citation();
    citation.setText("");
    dataset.setCitation(citation);
    TRIMMER.trimStringsOf(dataset);
    assertEquals("Citation text should be null", null, dataset.getCitation().getText());
  }

}
