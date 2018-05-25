package org.gbif.registry.metasync.protocols.biocase.model;

import org.apache.commons.digester3.annotations.rules.BeanPropertySetter;
import org.apache.commons.digester3.annotations.rules.ObjectCreate;

/**
 * This object extracts the same information from ABCD 2.06 as the "old" registry did.
 */
@ObjectCreate(pattern = "response/content")
public class BiocaseCount {

  @BeanPropertySetter(pattern = "response/content/count")
  private Long count;

  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }
}
