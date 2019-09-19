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

public class PredicateCounter extends PredicateVisitor<Integer> {

  public Integer count(Predicate p) {
    if (p == null) {
      return 0;
    }
    return visit(p);
  }

  protected Integer visit(ConjunctionPredicate and) {
    return and.getPredicates().stream().mapToInt(this::visit).sum();
  }
  protected Integer visit(DisjunctionPredicate or) {
    return or.getPredicates().stream().mapToInt(this::visit).sum();
  }

  protected Integer visit(EqualsPredicate predicate) {
    return 1;
  }
  protected Integer visit(InPredicate predicate) { return predicate.getValues().size(); }
  protected Integer visit(GreaterThanOrEqualsPredicate predicate) { return 1; }
  protected Integer visit(GreaterThanPredicate predicate) { return 1; }
  protected Integer visit(LessThanOrEqualsPredicate predicate) { return 1; }
  protected Integer visit(LessThanPredicate predicate) { return 1; }
  protected Integer visit(LikePredicate predicate) { return 1; }
  protected Integer visit(IsNotNullPredicate predicate) { return 1; }
  protected Integer visit(WithinPredicate within) { return 1; }

  protected Integer visit(NotPredicate not) {
    return visit(not.getPredicate());
  }
}
