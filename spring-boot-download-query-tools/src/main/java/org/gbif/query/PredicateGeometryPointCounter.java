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
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class PredicateGeometryPointCounter extends PredicateVisitor<Integer> {

  public Integer count(Predicate p) {
    if (p == null) {
      return 0;
    }
    return visit(p);
  }

  protected Integer visit(ConjunctionPredicate and) {
    return and.getPredicates().stream().mapToInt(this::visit).max().orElse(0);
  }

  protected Integer visit(DisjunctionPredicate or) {
    return or.getPredicates().stream().mapToInt(this::visit).sum();
  }

  protected Integer visit(EqualsPredicate predicate) { return 0; }
  protected Integer visit(InPredicate predicate) { return 0; }
  protected Integer visit(GreaterThanOrEqualsPredicate predicate) { return 0; }
  protected Integer visit(GreaterThanPredicate predicate) { return 0; }
  protected Integer visit(LessThanOrEqualsPredicate predicate) { return 0; }
  protected Integer visit(LessThanPredicate predicate) { return 0; }
  protected Integer visit(LikePredicate predicate) { return 0; }
  protected Integer visit(IsNotNullPredicate predicate) { return 0; }

  protected Integer visit(WithinPredicate within) {
    try {
      Geometry geometry = new WKTReader().read(within.getGeometry());
      return geometry.getNumPoints();
    } catch (ParseException e) {
      // The geometry has already been validated.
      throw new IllegalArgumentException("Exception parsing WKT", e);
    }
  }

  protected Integer visit(NotPredicate not) {
    return visit(not.getPredicate());
  }
}
