package org.gbif.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.CaseFormat;
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
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Continent;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.gbif.api.model.occurrence.search.OccurrenceSearchParameter.DEPTH;
import static org.gbif.api.model.occurrence.search.OccurrenceSearchParameter.ELEVATION;
import static org.gbif.api.model.occurrence.search.OccurrenceSearchParameter.GEOMETRY;

/**
 * This class builds a human readable filter from a {@link Predicate} hierarchy.
 * This class is not thread safe, create a new instance for every use if concurrent calls to {#humanFilter} is expected.
 * This builder only supports predicates that follow our search query parameters style with multiple values for the same
 * parameter being logically disjunct (OR) while different search parameters are logically combined (AND). Therefore
 * the {#humanFilter(Predicate p)} result is a map of OccurrenceSearchParameter (AND'ed) to a list of values (OR'ed).
 */
public class HumanPredicateBuilder {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final Logger LOG = LoggerFactory.getLogger(HumanPredicateBuilder.class);
  private static final String DEFAULT_BUNDLE = "org/gbif/query/filter";

  private static final String EQUALS_OPERATOR = "is ";
  private static final String IN_OPERATOR = "is one of ";
  private static final String GREATER_THAN_OPERATOR = "is greater than ";
  private static final String GREATER_THAN_EQUALS_OPERATOR = "is greater than or equal to ";
  private static final String LESS_THAN_OPERATOR = "is less than ";
  private static final String LESS_THAN_EQUALS_OPERATOR = "is less than or equal to ";

  private static final String NOT_OPERATOR = "not";
  private static final String IS_NOT_NULL_OPERATOR = "is not null";

  private static final String LIKE_OPERATOR = "~";
  private static final String ENUM_MONTH = "enum.month.";

  private final PredicateLookupCounter filterLookupCounter = new PredicateLookupCounter();
  private final TitleLookupService titleLookupService;
  private final ResourceBundle resourceBundle;

  public HumanPredicateBuilder(TitleLookupService titleLookupService) {
    this.titleLookupService = titleLookupService;
    resourceBundle = ResourceBundle.getBundle(DEFAULT_BUNDLE);
  }

  /**
   * @param p the predicate to convert
   * @return a list of anded parameters with multiple values to be combined with OR
   * @throws IllegalStateException if more complex predicates than the portal handles are supplied
   */
  public synchronized JsonNode humanFilter(Predicate p) {
    int count = filterLookupCounter.count(p);
    if (count > 10050) {
      throw new IllegalStateException("Too many lookups ("+count+") would be needed.");
    }

    JsonNode rootNode = MAPPER.createObjectNode();
    visit(p, rootNode);

    return rootNode;
  }

  /**
   * @param p the predicate to convert
   * @return a list of anded parameters with multiple values to be combined with OR
   * @throws IllegalStateException if more complex predicates than the portal handles are supplied
   */
  public synchronized String humanFilterString(Predicate p) {
    int count = filterLookupCounter.count(p);
    if (count > 10050) {
      throw new IllegalStateException("Too many lookups ("+count+") would be needed.");
    }

    try {
      JsonNode humanFilterNode = humanFilter(p);
      return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(humanFilterNode);
    } catch (Exception ex) {
      try {
        LOG.error("Error creating filters, using default predicate as JSON", ex);
        return MAPPER.writeValueAsString(p);
      } catch (Exception ex2) {
        throw new RuntimeException(ex2);
      }
    }
  }

  private Stream<JsonNode> toStream(Iterator<JsonNode> iterator) {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.NONNULL), false);
  }

  public synchronized String humanFilterString(String predicate) {
    try {
      return humanFilterString(MAPPER.readValue(predicate, Predicate.class));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private void addParamValue(OccurrenceSearchParameter param, String op, Collection<String> values, JsonNode node) {
    addParamValue(param, op + "(" + values.stream().map(p -> getHumanValue(param, p)).collect(Collectors.joining(", ")) + ")", node);
  }

  private void addParamValue(OccurrenceSearchParameter param, String op, String value, JsonNode node) {
    addParamValue(param, op + getHumanValue(param, value), node);
  }

  /**
   * Gets the human readable value of the parameter value.
   */
  private String getHumanValue(OccurrenceSearchParameter param, String value) {
    String humanValue;
    // lookup values
    switch (param) {
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
        humanValue = titleLookupService.getSpeciesName(value);
        break;
      case DATASET_KEY:
        humanValue = titleLookupService.getDatasetTitle(value);
        break;
      case COUNTRY:
      case PUBLISHING_COUNTRY:
        humanValue = lookupCountryCode(value);
        break;
      case CONTINENT:
        humanValue = lookupContinent(value);
        break;
      case MONTH:
        humanValue = lookupMonth(value);
        break;

      default:
        if (param.type().isEnum()) {
          humanValue = lookupEnum(param.type(), value);
        } else {
          humanValue = value;
        }
    }
    // add unit symbol
    if (param == DEPTH || param == ELEVATION) {
      humanValue = humanValue + "m";
    }
    return humanValue;
  }

  private String lookupContinent(String value) {
    Continent c = VocabularyUtils.lookupEnum(value, Continent.class);
    return c.getTitle();
  }

  private void addParamValue(OccurrenceSearchParameter param, String op, JsonNode node) {
    // verify that last param if existed was the same

    String paramName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, param.name());
    if (node.isObject()) {
      if (!node.has(paramName)) {
        ((ObjectNode) node).put(paramName, MAPPER.createArrayNode());
      }
      ((ArrayNode) node.get(paramName)).add(op);
    } else if (node.isArray()) {
      ((ArrayNode) node).add(TextNode.valueOf(paramName + " " + op));
    }
  }

  private String lookupEnum(Class clazz, String value) {
    return resourceBundle.getString("enum." + clazz.getSimpleName().toLowerCase() + "." + (clazz == MediaType.class ? value.trim() : value.trim().toUpperCase()));
  }

  private String lookupCountryCode(String code) {
    Country c = Country.fromIsoCode(code);
    if (c != null) {
      return c.getTitle();
    }
    return code;
  }

  private String lookupMonth(String month) {
    //It's a range
    String[] monthRange = month.split("-");
    if (monthRange.length == 2) {
      return resourceBundle.getString(ENUM_MONTH + Integer.parseInt(monthRange[0])) + "-" + resourceBundle.getString(ENUM_MONTH + Integer.parseInt(monthRange[1]));
    }
    return resourceBundle.getString(ENUM_MONTH + Integer.parseInt(month));
  }

  private static void addOrPut(JsonNode node, String fieldName, JsonNode newNode) {
    if (node.isObject()) {
      ((ObjectNode) node).put(fieldName, newNode);
    } else if (node.isArray()) {
      ObjectNode andBaseNode = MAPPER.createObjectNode();
      andBaseNode.put(fieldName, newNode);
      ((ArrayNode) node).add(andBaseNode);
    }
  }

  private void visit(ConjunctionPredicate and, JsonNode node) {
    JsonNode andNode = MAPPER.createArrayNode();
    // ranges are allowed underneath root - try first
    try {
      visitRange(and, node);
      return;
    } catch (IllegalArgumentException e) {
      // must be a root AND
    }
    and.getPredicates().forEach(p -> visit(p, andNode));
    addOrPut(node, "and", andNode);
  }

  private void visit(DisjunctionPredicate or, JsonNode node) {
    JsonNode orNode = MAPPER.createArrayNode();
    or.getPredicates().forEach(p -> visit(p, orNode));
    addOrPut(node, "or", orNode);
  }

  private void visit(EqualsPredicate predicate, JsonNode node) {
    addParamValue(predicate.getKey(), EQUALS_OPERATOR, predicate.getValue(), node);
  }

  private void visit(GreaterThanOrEqualsPredicate predicate, JsonNode node) {
    addParamValue(predicate.getKey(), GREATER_THAN_EQUALS_OPERATOR, predicate.getValue(), node);
  }

  private void visit(GreaterThanPredicate predicate, JsonNode node) {
    addParamValue(predicate.getKey(), GREATER_THAN_OPERATOR, predicate.getValue(), node);
  }

  private void visit(InPredicate in, JsonNode node) {
    addParamValue(in.getKey(), IN_OPERATOR, in.getValues(), node);
  }

  private void visit(LessThanOrEqualsPredicate predicate, JsonNode node) {
    addParamValue(predicate.getKey(), LESS_THAN_EQUALS_OPERATOR, predicate.getValue(), node);
  }

  private void visit(LessThanPredicate predicate, JsonNode node) {
    addParamValue(predicate.getKey(), LESS_THAN_OPERATOR, predicate.getValue(), node);
  }

  private void visit(LikePredicate predicate, JsonNode node) {
    addParamValue(predicate.getKey(), LIKE_OPERATOR, predicate.getValue(), node);
  }

  private void visit(NotPredicate not, JsonNode node) {
    JsonNode notNode = MAPPER.createObjectNode();
    visit(not.getPredicate(), notNode);
    addOrPut(node, NOT_OPERATOR, notNode);
  }

  private void visit(IsNotNullPredicate predicate, JsonNode node) {
    addParamValue(predicate.getParameter(), IS_NOT_NULL_OPERATOR, node);
  }


  private void visit(Predicate p, JsonNode node) {
    Method method;
    try {
      method = getClass().getDeclaredMethod("visit", p.getClass(), JsonNode.class);
    } catch (NoSuchMethodException e) {
      LOG.warn(
          "Visit method could not be found. That means a Predicate has been passed in that is unknown to this class", e);
      throw new IllegalArgumentException("Unknown Predicate", e);
    }

    try {
      method.setAccessible(true);
      method.invoke(this, p, node);
    } catch (IllegalAccessException e) {
      LOG.error(
          "This should never happen as we set accessible to true explicitly before. Probably a programming error", e);
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      LOG.info("Exception thrown while building the human query string", e);
      throw new IllegalArgumentException(e);
    }
  }

  private void visit(WithinPredicate within, JsonNode node) {
    addParamValue(GEOMETRY, "", within.getGeometry(), node);
  }

  private void visitRange(ConjunctionPredicate and, JsonNode node) {
    if (and.getPredicates().size() != 2) {
      throw new IllegalArgumentException("no valid range");
    }
    GreaterThanOrEqualsPredicate lower = null;
    LessThanOrEqualsPredicate upper = null;
    for (Predicate p : and.getPredicates()) {
      if (p instanceof GreaterThanOrEqualsPredicate) {
        lower = (GreaterThanOrEqualsPredicate) p;
      } else if (p instanceof LessThanOrEqualsPredicate) {
        upper = (LessThanOrEqualsPredicate) p;
      }
    }
    if (lower == null || upper == null || lower.getKey() != upper.getKey()) {
      throw new IllegalArgumentException("no valid range");
    }
    addParamValue(lower.getKey(), "", lower.getValue() + "-" + upper.getValue(), node);
  }

}


