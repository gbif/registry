package org.gbif.registry.search;

import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestResult;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import static org.gbif.common.search.solr.SolrConstants.HL_POST;
import static org.gbif.common.search.solr.SolrConstants.HL_PRE;
import static org.gbif.common.search.solr.SolrConstants.HL_PRE_REGEX;
import static org.gbif.registry.search.SolrMapping.KEY_FIELD;

/**
 *
 */
public class ResponseBuilder {

  private final DatasetDocConverter converter = new DatasetDocConverter();

  /**
   * Builds a SearchResponse instance using the current builder state.
   *
   * @return a new instance of a SearchResponse.
   */
  public SearchResponse<DatasetSearchResult, DatasetSearchParameter> buildSearch(DatasetSearchRequest searchRequest, QueryResponse response) {
    // Create response
    SearchResponse<DatasetSearchResult, DatasetSearchParameter> resp = new SearchResponse<DatasetSearchResult, DatasetSearchParameter>(searchRequest);
    resp.setCount(response.getResults().getNumFound());
    resp.setLimit(response.getResults().size());

    // swap stored values for highlighted ones if existing

    // Main result documents
    SolrDocumentList docs = response.getResults();
    for (SolrDocument doc : docs) {
      insertHighlighting(doc, response);
      resp.getResults().add(converter.toSearchResult(doc));
    }

    // add facets
    setFacets(resp, response);

    return resp;
  }

  /**
   * Builds a SearchResponse instance using the current builder state.
   *
   * @return a new instance of a SearchResponse.
   */
  public List<DatasetSuggestResult> buildSuggest(QueryResponse response) {
    List<DatasetSuggestResult> result = Lists.newArrayList();
    // Main result documents
    SolrDocumentList docs = response.getResults();
    for (SolrDocument doc : docs) {
      result.add(converter.toSuggestResult(doc));
    }
    return result;
  }

  /**
   * Helper method that takes Solr response and extracts the facets results.
   * The facets are converted to a list of Facets understood by the search API.
   * The result of this method can be a empty list.
   *
   * @param queryResponse that contains the facets information returned by Solr
   * @return the List of facets retrieved from the Solr response
   */
  private void setFacets(SearchResponse<DatasetSearchResult, DatasetSearchParameter> response, final QueryResponse queryResponse) {
    List<Facet<DatasetSearchParameter>> facets = Lists.newArrayList();
    if (queryResponse.getFacetFields() != null) {
      List<FacetField> facetFields =  queryResponse.getFacetFields();
      for (final FacetField facetField : facetFields) {
        DatasetSearchParameter facetParam = SolrMapping.FIELDS_MAPPING.inverse().get(facetField.getName());
        Facet<DatasetSearchParameter> facet = new Facet<DatasetSearchParameter>(facetParam);

        List<Facet.Count> counts = Lists.newArrayList();
        if (facetField.getValues() != null) {
          for (final FacetField.Count count : facetField.getValues()) {
            String value = SolrMapping.interpretSolrValue(facetParam, count.getName());
            counts.add(new Facet.Count(value, count.getCount()));
          }
        }
        facet.setCounts(counts);
        facets.add(facet);
      }
    }
    response.setFacets(facets);
  }

  /**
   * Takes the highlighted fields form solrResponse and copies them to the regular solr document.
   * @param doc solr document to update
   * @param resp to extract the highlighting information
   */
  private void insertHighlighting(final SolrDocument doc, QueryResponse resp) {
    final String key = (String) doc.getFieldValue(KEY_FIELD);
    if (resp.getHighlighting() != null && !resp.getHighlighting().get(key).isEmpty()) {
      Map<String, List<String>> docHighlights = resp.getHighlighting().get(key);
      for (Map.Entry<String, List<String>> hlField : docHighlights.entrySet()) {
        for (String hlValue : hlField.getValue()) {
          doc.setField(hlField.getKey(), mergeHl((String)doc.getFieldValue(hlField.getKey()), hlValue));
        }
      }
    }
  }

  @VisibleForTesting
  protected static String mergeHl(String original, String hlSnippet) {
    // Cleans the hl markers
    String hlCleaned = cleanHighlightingMarks(hlSnippet);
    // replace snippet in original
    return original.replace(hlCleaned, hlSnippet);
  }


  /**
   * Cleans all occurrences of highlighted tags/marks in the parameter and returns an new instance clean of those
   * marks.
   */
  private static String cleanHighlightingMarks(final String hlText) {
    String hlLiteral = hlText;
    int indexPre = hlLiteral.indexOf(HL_PRE);
    while (indexPre > -1) {
      int indexPost = hlLiteral.indexOf(HL_POST, indexPre + HL_PRE.length());
      if (indexPost > -1) {
        String post = hlLiteral.substring(indexPost + HL_POST.length());
        String pre = hlLiteral.substring(0, indexPost);
        Matcher preMatcher = HL_PRE_REGEX.matcher(pre);
        pre = preMatcher.replaceFirst("");
        hlLiteral = pre + post;
      }
      indexPre = hlLiteral.indexOf(HL_PRE);
    }
    return hlLiteral;
  }

}
