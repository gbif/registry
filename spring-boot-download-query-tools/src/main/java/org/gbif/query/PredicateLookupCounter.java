package org.gbif.query;

import org.gbif.api.model.occurrence.predicate.ConjunctionPredicate;
import org.gbif.api.model.occurrence.predicate.DisjunctionPredicate;
import org.gbif.api.model.occurrence.predicate.EqualsPredicate;
import org.gbif.api.model.occurrence.predicate.GreaterThanOrEqualsPredicate;
import org.gbif.api.model.occurrence.predicate.GreaterThanPredicate;
import org.gbif.api.model.occurrence.predicate.InPredicate;
import org.gbif.api.model.occurrence.predicate.IsNotNullPredicate;
import org.gbif.api.model.occurrence.predicate.LessThanOrEqualsPredicate;
import org.gbif.api.model.occurrence.predicate.LessThanPredicate;
import org.gbif.api.model.occurrence.predicate.LikePredicate;
import org.gbif.api.model.occurrence.predicate.NotPredicate;
import org.gbif.api.model.occurrence.predicate.Predicate;
import org.gbif.api.model.occurrence.predicate.WithinPredicate;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class counts the number of webservice lookups needed to format a {@link Predicate} hierarchy.
 */
public class PredicateLookupCounter extends PredicateVisitor<Integer> {

  protected static final Logger LOG = LoggerFactory.getLogger(PredicateLookupCounter.class);

  public Integer count(Predicate p) {
    if (p == null) {
      return 0;
    }
    return visit(p);
  }

  protected Integer getHumanValue(OccurrenceSearchParameter param) {
    // lookup values
    switch (param) {
      case SCIENTIFIC_NAME:
      case ACCEPTED_TAXON_KEY:
      case TAXON_KEY:
      case KINGDOM_KEY:
      case PHYLUM_KEY:
      case CLASS_KEY:
      case ORDER_KEY:
      case FAMILY_KEY:
      case GENUS_KEY:
      case SUBGENUS_KEY:
      case SPECIES_KEY:
      case DATASET_KEY:
        return 1;
      default:
        return 0;
    }
  }

  protected Integer visit(ConjunctionPredicate and) {
    int count = 0;
    for (Predicate p : and.getPredicates()) {
      count += visit(p);
    }
    return count;
  }

  protected Integer visit(DisjunctionPredicate or) {
    int count = 0;
    for (Predicate p : or.getPredicates()) {
      count += visit(p);
    }
    return count;
  }

  protected Integer visit(EqualsPredicate predicate) {
    return getHumanValue(predicate.getKey());
  }

  protected Integer visit(InPredicate predicate) {
    return predicate.getValues().size() * getHumanValue(predicate.getKey());
  }

  protected Integer visit(GreaterThanOrEqualsPredicate predicate) {
    return 0;
  }

  protected Integer visit(GreaterThanPredicate predicate) {
    return 0;
  }

  protected Integer visit(LessThanOrEqualsPredicate predicate) {
    return 0;
  }

  protected Integer visit(LessThanPredicate predicate) {
    return 0;
  }

  protected Integer visit(LikePredicate predicate) {
    return 0;
  }

  protected Integer visit(IsNotNullPredicate predicate) {
    return 0;
  }

  protected Integer visit(WithinPredicate within) {
    return 0;
  }

  protected Integer visitRange(ConjunctionPredicate and) {
    return 0;
  }

  protected Integer visit(NotPredicate not) {
    return visit(not.getPredicate());
  }
}
