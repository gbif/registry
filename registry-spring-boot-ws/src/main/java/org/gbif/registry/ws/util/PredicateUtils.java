package org.gbif.registry.ws.util;

import java.util.function.Predicate;

public final class PredicateUtils {

  private PredicateUtils() {
  }

  /**
   * Negate provided predicate.
   *
   * @param t predicate
   * @return negated predicate
   */
  public static <T> Predicate<T> not(Predicate<T> t) {
    return t.negate();
  }
}
