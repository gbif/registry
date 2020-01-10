package org.gbif.registry.search;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.FacetParams;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.model.registry.search.DatasetSuggestRequest;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.common.search.solr.QueryUtils;
import org.gbif.common.search.solr.SearchDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

import static org.gbif.common.search.solr.QueryUtils.DEFAULT_FACET_COUNT;
import static org.gbif.common.search.solr.QueryUtils.DEFAULT_FACET_SORT;
import static org.gbif.common.search.solr.QueryUtils.PARAMS_JOINER;
import static org.gbif.common.search.solr.QueryUtils.PARAMS_OR_JOINER;
import static org.gbif.common.search.solr.QueryUtils.perFieldParamName;
import static org.gbif.common.search.solr.QueryUtils.toParenthesesQuery;
import static org.gbif.common.search.solr.QueryUtils.toPhraseQuery;
import static org.gbif.common.search.solr.SolrConstants.BLANK;
import static org.gbif.common.search.solr.SolrConstants.DEFAULT_QUERY;
import static org.gbif.common.search.solr.SolrConstants.NOT_OP;
import static org.gbif.common.search.solr.SolrConstants.NUM_HL_SNIPPETS;
import static org.gbif.registry.search.SolrMapping.FIELDS_MAPPING;
import static org.gbif.registry.search.SolrMapping.HIGHLIGHT_FIELDS;
import static org.gbif.ws.util.WebserviceParameter.DEFAULT_SEARCH_PARAM_VALUE;

/**
 * Builder class to generate solr queries based on the dismax query parser.
 */
public class SolrQueryBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(SolrQueryBuilder.class);

  private static final String QUERY_PARSER   = "edismax";
  private static final String QUERY_FIELDS = "title^10 keyword^5 description publishing_organization_title hosting_organization_title project_id metadata^0.5";
  private static final String PHRASE_FIELDS = "title^100 description";
  private static final String PHRASE_SLOP = "100";
  private static final String PHRASE_FIELDS_SHINGLE = "title^10 description^1.5";
  private static final String PHRASE_SLOP_SHINGLE = "10";
  private static final String BOOST_FUNCTION = "log(sum(2,record_count))";
  private static final String MINIMUM_SHOULD_MATCH = "25%";
  private static final String TIE_BREAKER = "0.2";

  private static final String SUGGEST_QUERY_FIELDS   = "title_ngram title^10";
  private static final String SUGGEST_PHRASE_FIELDS  = "title^100";

  private static final Integer FRAGMENT_SIZE = 100;

  private static String prepareQ(String q) {
    if (Strings.isNullOrEmpty(q)) return null;
    q = q.trim();
    // the common-ws utils replaces empty queries with * as the default - this does not work for dismax, remove it
    if (q.equals(DEFAULT_SEARCH_PARAM_VALUE)) return null;

    return q.contains(BLANK) ? QueryUtils.toPhraseQuery(q) : q;
  }

  public SolrQuery build(DatasetSearchRequest request) {
    SolrQuery query = buildBase(request);

    // dismax fields
    query.set(DisMaxParams.QF, QUERY_FIELDS);
    query.set(DisMaxParams.PF, PHRASE_FIELDS);
    query.set(DisMaxParams.MM, MINIMUM_SHOULD_MATCH);
    query.set(DisMaxParams.PS, PHRASE_SLOP);

    // request facets
    requestFacets(request, query);

    // highlight
    setHighLightParams(request, query);

    LOG.debug("Solr search query build: {}", query);
    return query;
  }

  public SolrQuery build(DatasetSuggestRequest request) {
    SolrQuery query = buildBase(request);

    // dismax fields
    query.set(DisMaxParams.QF, SUGGEST_QUERY_FIELDS);
    query.set(DisMaxParams.PF, SUGGEST_PHRASE_FIELDS);

    LOG.debug("Solr suggest query build: {}", query);
    return query;
  }

  private SolrQuery buildBase(SearchRequest<DatasetSearchParameter> request) {
    SolrQuery query = new SolrQuery();
    // q param
    String q = prepareQ(request.getQ());
    if (!Strings.isNullOrEmpty(q)) {
      query.setQuery(q);
    }
    // use dismax query parser
    query.set("defType", QUERY_PARSER);
    // sets the default catch all, alternative query if q above is empty
    query.set(DisMaxParams.ALTQ, DEFAULT_QUERY);

    // facet based filter query
    setFacetFilterQuery(request, query);

    // boost larger datasets
    query.set(DisMaxParams.BF, BOOST_FUNCTION);

    // Tie Breaker
    query.set(DisMaxParams.TIE, TIE_BREAKER);

    // paging
    QueryUtils.setQueryPaging(request, query);

    return query;
  }

  /**
   * Helper method that sets the highlighting parameters.
   *
   * @param searchRequest the searchRequest used to extract the parameters
   * @param solrQuery this object is modified by adding the facets parameters
   */
  private void setHighLightParams(DatasetSearchRequest searchRequest, SolrQuery solrQuery) {
    if (searchRequest.isHighlight()) {
      solrQuery.setHighlight(searchRequest.isHighlight());
      solrQuery.setHighlightSnippets(NUM_HL_SNIPPETS);
      solrQuery.setHighlightFragsize(FRAGMENT_SIZE);
      for (String hlField : HIGHLIGHT_FIELDS) {
        solrQuery.addHighlightField(hlField);
      }
    }
  }

  /**
   * Adds the filter query to SolrQuery object.
   * Creates a conjunction of disjunctions: disjunctions(ORs) are created for the filter applied to the same field;
   * those disjunctions are joint in a big conjunction.
   *
   * @throws IllegalArgumentException if request is bad, e.g. wrongly typed data for given filter parameters
   */
  private static void setFacetFilterQuery(SearchRequest<DatasetSearchParameter> request, SolrQuery solrQuery) throws IllegalArgumentException {
    Multimap<DatasetSearchParameter, String> params = request.getParameters();
    if (params != null) {
      for (DatasetSearchParameter param : params.keySet()) {
        String solrField = FIELDS_MAPPING.get(param);
        if (solrField == null) {
          LOG.warn("Requested facet {} not mapped! It will be ignored", param);

        } else {
          List<String> predicates = Lists.newArrayList();
          Boolean negated = null;
          for (String value : params.get(param)) {
            if (Strings.isNullOrEmpty(value)) {
              throw new IllegalArgumentException("Null value not allowed for filter parameter " + param);
            }

            // treat negation
            if (negated == null) {
              negated = QueryUtils.isNegated(value);
            } else {
              // make sure we do not mix negated and unnegated filters for the same parameter - this is too complex and not supported
              if (QueryUtils.isNegated(value) != negated) {
                throw new IllegalArgumentException("Mixing of negated and not negated filters for the same parameter " + param.name() + " is not allowed");
              }
            }

            // strip off negation symbol before we parse the value
            if (negated) {
              value = QueryUtils.removeNegation(value);
            }

            // parse value into typed instance
            String filterVal;
            if (Enum.class.isAssignableFrom(param.type())) {
              Enum<?> e;
              if (Country.class.isAssignableFrom(param.type())) {
                e = Country.fromIsoCode(value);
              } else if (Language.class.isAssignableFrom(param.type())) {
                e = Language.fromIsoCode(value);
              } else {
                e = VocabularyUtils.lookupEnum(value, (Class<? extends Enum<?>>) param.type());
              }
              if (e==null) {
                throw new IllegalArgumentException("Invalid " + param.name() + " parameter value " + value);
              }
              filterVal = String.valueOf(e.ordinal());

            } else if (UUID.class.isAssignableFrom(param.type())) {
              filterVal = UUID.fromString(value).toString();

            } else if (Double.class.isAssignableFrom(param.type())) {
              filterVal = String.valueOf(Double.parseDouble(value));

            } else if (Integer.class.isAssignableFrom(param.type())) {
              filterVal = String.valueOf(Integer.parseInt(value));

            } else if (Boolean.class.isAssignableFrom(param.type())) {
              filterVal = String.valueOf(Boolean.parseBoolean(value));

            } else {

              if(DatasetSearchParameter.MODIFIED_DATE == param){
                filterVal = SearchDateUtils.toDateQuery(value);
              }
              else {
                filterVal = toPhraseQuery(value);
              }
            }

            final String predicate = PARAMS_JOINER.join(solrField, filterVal);
            predicates.add(predicate);
          }

          // combine all parameter predicates with OR
          if (!predicates.isEmpty()) {
            String parenthesis = toParenthesesQuery(PARAMS_OR_JOINER.join(predicates));
            // tag filter queries so we can exclude them later for multi value faceting
            // http://yonik.com/multi-select-faceting/
            solrQuery.addFilterQuery(tag(solrField, negated ? NOT_OP + parenthesis : parenthesis));
          }
        }
      }
    }
  }

  private static String ex(String tag, String filter) {
    return "{!ex=" + tag + "}" + filter;
  }

  private static String tag(String tag, String filter) {
    return "{!tag=" + tag + "}" + filter;
  }

  /**
   * Helper method that sets the parameter for a faceted query.
   *
   * @param searchRequest the searchRequest used to extract the parameters
   * @param solrQuery this object is modified by adding the facets parameters
   */
  private void requestFacets(FacetedSearchRequest<DatasetSearchParameter> searchRequest, SolrQuery solrQuery) {

    if (!searchRequest.getFacets().isEmpty()) {
      // Only show facets that contains at least 1 record
      solrQuery.setFacet(true);
      // defaults if not overridden on per field basis
      solrQuery.setFacetMinCount(MoreObjects.firstNonNull(searchRequest.getFacetMinCount(), DEFAULT_FACET_COUNT));
      solrQuery.setFacetMissing(false);
      solrQuery.setFacetSort(DEFAULT_FACET_SORT.toString().toLowerCase());

      if (searchRequest.getFacetLimit() != null) {
        solrQuery.setFacetLimit(searchRequest.getFacetLimit());
      }

      if (searchRequest.getFacetOffset() != null) {
        solrQuery.setParam(FacetParams.FACET_OFFSET, searchRequest.getFacetOffset().toString());
      }

      for (final DatasetSearchParameter facet : searchRequest.getFacets()) {
        if (!FIELDS_MAPPING.containsKey(facet)) {
          LOG.warn("{} is no valid facet. Ignore", facet);
          continue;
        }
        final String field = FIELDS_MAPPING.get(facet);
        if (searchRequest.isMultiSelectFacets()) {
          // use exclusion filter with same name as used in filter query
          // http://wiki.apache.org/solr/SimpleFacetParameters#Tagging_and_excluding_Filters
          // http://yonik.com/multi-select-faceting/
          solrQuery.addFacetField(ex(field, field));
        } else {
          solrQuery.addFacetField(field);
        }

        Pageable facetPage = searchRequest.getFacetPage(facet);
        if (facetPage != null) {
          solrQuery.setParam(perFieldParamName(field, FacetParams.FACET_OFFSET), Long.toString(facetPage.getOffset()));
          solrQuery.setParam(perFieldParamName(field, FacetParams.FACET_LIMIT), Integer.toString(facetPage.getLimit()));
        }
      }
    }
  }
}
