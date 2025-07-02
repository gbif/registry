package org.gbif.registry.ws.provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.gbif.api.model.collections.request.CollectionDescriptorsSearchRequest;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.request.FacetedSearchRequest;
import org.gbif.api.model.collections.request.InstitutionFacetedSearchRequest;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.collections.request.SearchRequest;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.GbifRegion;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.SortOrder;
import org.gbif.api.vocabulary.collections.CollectionFacetParameter;
import org.gbif.api.vocabulary.collections.CollectionsFacetParameter;
import org.gbif.api.vocabulary.collections.CollectionsSortField;
import org.gbif.api.vocabulary.collections.InstitutionFacetParameter;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.api.vocabulary.collections.Source;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
public class CollectionsSearchRequestHandlerMethodArgumentResolverTest {

  @Mock private NativeWebRequest webRequest;
  @Mock private WebDataBinderFactory binderFactory;
  @Mock private ModelAndViewContainer mavContainer;
  @Mock private MethodParameter methodParameter;

  private final InstitutionSearchRequestHandlerMethodArgumentResolver instResolver =
      new InstitutionSearchRequestHandlerMethodArgumentResolver();
  private final CollectionSearchRequestHandlerMethodArgumentResolver collResolver =
      new CollectionSearchRequestHandlerMethodArgumentResolver();

  @Test
  public void commonParamsTest() {
    mockCommonParams();
    InstitutionSearchRequest institutionSearchRequest =
        (InstitutionSearchRequest)
            instResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
    assertCommonParams(institutionSearchRequest);

    mockCommonParams();
    CollectionSearchRequest collectionSearchRequest =
        (CollectionSearchRequest)
            collResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
    assertCommonParams(collectionSearchRequest);
  }

  @Test
  public void institutionParamsTest() {
    Map<String, String[]> parameterMap = new HashMap<>();
    parameterMap.put("type", new String[] {"t1", "t2"});
    parameterMap.put("institutionalGovernance", new String[] {"i1", "i2"});
    parameterMap.put("disciPLINE", new String[] {"d1", "d2"});
    when(webRequest.getParameterMap()).thenReturn(parameterMap);

    InstitutionSearchRequest searchRequest =
        (InstitutionSearchRequest)
            instResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
    assertEquals(Arrays.asList("t1", "t2"), searchRequest.getType());
    assertEquals(Arrays.asList("i1", "i2"), searchRequest.getInstitutionalGovernance());
    assertEquals(Arrays.asList("d1", "d2"), searchRequest.getDisciplines());
  }

  @Test
  public void collectionDescriptorsParamsTest() {
    Map<String, String[]> parameterMap = new HashMap<>();
    parameterMap.put("usageKey", new String[] {"1", "2"});
    parameterMap.put("usageName", new String[] {"n1", "n2"});
    parameterMap.put("usageRank", new String[] {"KINGDOM", "PHYLUM"});
    parameterMap.put("taxonKey", new String[] {"11", "22"});
    parameterMap.put("identifiedBy", new String[] {"ide1", "ide2"});
    parameterMap.put("typeStatus", new String[] {"t1", "t2"});
    parameterMap.put("recordedBy", new String[] {"rec1", "rec2"});
    parameterMap.put("discipline", new String[] {"d1", "d2"});
    parameterMap.put("objectClassification", new String[] {"oc1", "oc2"});
    parameterMap.put("issue", new String[] {"iss1", "iss2"});
    parameterMap.put("individualCount", new String[] {"111", "222"});
    parameterMap.put("descriptorCountry", new String[] {"DK", "ES"});
    parameterMap.put(
        "dateIdentified", new String[] {"2024-01-01", "2024-01-01,*", "2024-01-01,2024-03-01"});
    when(webRequest.getParameterMap()).thenReturn(parameterMap);

    CollectionDescriptorsSearchRequestHandlerMethodArgumentResolver resolver =
        new CollectionDescriptorsSearchRequestHandlerMethodArgumentResolver();
    CollectionDescriptorsSearchRequest searchRequest =
        (CollectionDescriptorsSearchRequest)
            resolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);

    assertEquals(Arrays.asList("1", "2"), searchRequest.getUsageKey());
    assertEquals(Arrays.asList("n1", "n2"), searchRequest.getUsageName());
    assertEquals(Arrays.asList(Rank.KINGDOM.toString(), Rank.PHYLUM.toString()), searchRequest.getUsageRank());
    assertEquals(Arrays.asList("11", "22"), searchRequest.getTaxonKey());
    assertEquals(Arrays.asList("ide1", "ide2"), searchRequest.getIdentifiedBy());
    assertEquals(Arrays.asList("t1", "t2"), searchRequest.getTypeStatus());
    assertEquals(Arrays.asList("rec1", "rec2"), searchRequest.getRecordedBy());
    assertEquals(Arrays.asList("d1", "d2"), searchRequest.getDiscipline());
    assertEquals(Arrays.asList("oc1", "oc2"), searchRequest.getObjectClassification());
    assertEquals(Arrays.asList("iss1", "iss2"), searchRequest.getIssue());
    assertEquals(Arrays.asList("111", "222"), searchRequest.getIndividualCount());
    assertEquals(
        Arrays.asList(Country.DENMARK, Country.SPAIN), searchRequest.getDescriptorCountry());
    assertEquals(
        Arrays.asList("2024-01-01", "2024-01-01,*", "2024-01-01,2024-03-01"),
        searchRequest.getDateIdentified());
  }

  @Test
  public void facetsTest() {
    mockFacetParams();
    InstitutionFacetedSearchRequestHandlerMethodArgumentResolver resolver =
        new InstitutionFacetedSearchRequestHandlerMethodArgumentResolver();
    InstitutionFacetedSearchRequest searchRequest =
        (InstitutionFacetedSearchRequest)
            resolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
    assertFacetParams(searchRequest, InstitutionFacetParameter.COUNTRY);

    mockFacetParams();
    CollectionDescriptorsSearchRequestHandlerMethodArgumentResolver collResolver =
        new CollectionDescriptorsSearchRequestHandlerMethodArgumentResolver();
    CollectionDescriptorsSearchRequest collSearchRequest =
        (CollectionDescriptorsSearchRequest)
            collResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
    assertFacetParams(collSearchRequest, CollectionFacetParameter.COUNTRY);
  }

  private static <T extends CollectionsFacetParameter> void assertFacetParams(
      FacetedSearchRequest<T> searchRequest, T facetParam) {
    assertEquals(1, searchRequest.getFacets().size());
    assertEquals(facetParam, searchRequest.getFacets().iterator().next());
    assertTrue(searchRequest.isMultiSelectFacets());
    assertEquals(2, searchRequest.getFacetMinCount());
    assertEquals(1, searchRequest.getFacetLimit());
    assertEquals(2, searchRequest.getFacetOffset());
    assertEquals(1, searchRequest.getFacetPages().size());
    assertEquals(4, searchRequest.getFacetPages().get(facetParam).getOffset());
  }

  private void mockFacetParams() {
    Map<String, String[]> parameterMap = new HashMap<>();
    parameterMap.put("facet", new String[] {"COUNTRY"});
    parameterMap.put("facetMultiselect", new String[] {"true"});
    parameterMap.put("facetMincount", new String[] {"2"});
    parameterMap.put("facetLimit", new String[] {"1"});
    parameterMap.put("facetOffset", new String[] {"2"});
    parameterMap.put("country.facetOffset", new String[] {"4"});
    when(webRequest.getParameterMap()).thenReturn(parameterMap);
  }

  @Test
  public void collectionParamsTest() {
    Map<String, String[]> parameterMap = new HashMap<>();
    parameterMap.put(
        "institution",
        new String[] {
          "b600ae47-97a9-4b63-85f3-21d4f52523a1", "d2434ba9-10ef-4308-bd45-a69af379b5d9"
        });
    parameterMap.put("contentType", new String[] {"c1", "c2"});
    parameterMap.put("preservationType", new String[] {"p1", "p2"});
    parameterMap.put("accessionStatus", new String[] {"a1", "a2"});
    parameterMap.put("personalCollection", new String[] {"true", "false"});
    when(webRequest.getParameterMap()).thenReturn(parameterMap);

    CollectionSearchRequest collectionSearchRequest =
        (CollectionSearchRequest)
            collResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
    assertEquals(
        Arrays.asList(
            UUID.fromString("b600ae47-97a9-4b63-85f3-21d4f52523a1"),
            UUID.fromString("d2434ba9-10ef-4308-bd45-a69af379b5d9")),
        collectionSearchRequest.getInstitution());
    assertEquals(Arrays.asList("c1", "c2"), collectionSearchRequest.getContentTypes());
    assertEquals(Arrays.asList("p1", "p2"), collectionSearchRequest.getPreservationTypes());
    assertEquals(Arrays.asList("a1", "a2"), collectionSearchRequest.getAccessionStatus());
    assertEquals(Arrays.asList(true, false), collectionSearchRequest.getPersonalCollection());
  }

  private void mockCommonParams() {
    Map<String, String[]> parameterMap = new HashMap<>();
    parameterMap.put("limit", new String[] {"200"});
    parameterMap.put("offset", new String[] {"10"});
    parameterMap.put("alternativeCode", new String[] {"ac1", "ac2"});
    parameterMap.put("CODE", new String[] {"c1"});
    parameterMap.put("name", new String[] {"n1"});
    parameterMap.put("contact", new String[] {"b600ae47-97a9-4b63-85f3-21d4f52523a1"});
    parameterMap.put("identifier", new String[] {"ide1", "ide2"});
    parameterMap.put(
        "identifierType",
        new String[] {IdentifierType.CITES.name(), IdentifierType.GRSCICOLL_URI.name()});
    parameterMap.put("machineTagName", new String[] {"mt1", "mt2"});
    parameterMap.put("machineTagNamespace", new String[] {"mt1", "mt2"});
    parameterMap.put("machineTagValue", new String[] {"mt1", "mt2"});
    parameterMap.put("city", new String[] {"city1"});
    parameterMap.put("fuzzyName", new String[] {"fuzzn1"});
    parameterMap.put("active", new String[] {"true"});
    parameterMap.put("masterSourceType", new String[] {MasterSourceType.IH.name()});
    parameterMap.put("numberSpecimens", new String[] {"1,*", "2,4", "3"});
    parameterMap.put("displayOnNHCPortal", new String[] {"true", "false"});
    parameterMap.put("occurrenceCount", new String[] {"1,*", "2,4", "3"});
    parameterMap.put("typeSpecimenCount", new String[] {"1,*", "2,4", "3"});
    parameterMap.put(
        "institutionKey",
        new String[] {
          "b600ae47-97a9-4b63-85f3-21d4f52523a1", "d2434ba9-10ef-4308-bd45-a69af379b5d9"
        });
    parameterMap.put("source", new String[] {Source.IH_IRN.name()});
    parameterMap.put("sourceId", new String[] {"214"});
    parameterMap.put("country", new String[] {"DK", "ES"});
    parameterMap.put("gbifRegion", new String[] {GbifRegion.EUROPE.name()});

    when(webRequest.getParameterMap()).thenReturn(parameterMap);
    when(webRequest.getParameter("hl")).thenReturn("true");
    when(webRequest.getParameter("q")).thenReturn("query");
    when(webRequest.getParameter("sortBy"))
        .thenReturn(CollectionsSortField.NUMBER_SPECIMENS.name());
    when(webRequest.getParameter("sortOrder")).thenReturn(SortOrder.ASC.name());
  }

  private void assertCommonParams(SearchRequest searchRequest) {
    assertEquals("query", searchRequest.getQ());
    assertEquals(200, searchRequest.getLimit());
    assertEquals(10, searchRequest.getOffset());
    assertEquals(CollectionsSortField.NUMBER_SPECIMENS, searchRequest.getSortBy());
    assertEquals(SortOrder.ASC, searchRequest.getSortOrder());
    assertEquals(Boolean.TRUE, searchRequest.getHl());
    assertEquals(Arrays.asList("ac1", "ac2"), searchRequest.getAlternativeCode());
    assertEquals(Collections.singletonList("c1"), searchRequest.getCode());
    assertEquals(Collections.singletonList("n1"), searchRequest.getName());
    assertEquals(
        Collections.singletonList(UUID.fromString("b600ae47-97a9-4b63-85f3-21d4f52523a1")),
        searchRequest.getContact());
    assertEquals(Arrays.asList("ide1", "ide2"), searchRequest.getIdentifier());
    assertEquals(
        Arrays.asList(IdentifierType.CITES, IdentifierType.GRSCICOLL_URI),
        searchRequest.getIdentifierType());
    assertEquals(Arrays.asList("mt1", "mt2"), searchRequest.getMachineTagName());
    assertEquals(Arrays.asList("mt1", "mt2"), searchRequest.getMachineTagNamespace());
    assertEquals(Arrays.asList("mt1", "mt2"), searchRequest.getMachineTagValue());
    assertEquals(Collections.singletonList("city1"), searchRequest.getCity());
    assertEquals(Collections.singletonList("fuzzn1"), searchRequest.getFuzzyName());
    assertEquals(Collections.singletonList(true), searchRequest.getActive());
    assertEquals(
        Collections.singletonList(MasterSourceType.IH), searchRequest.getMasterSourceType());
    assertEquals(Arrays.asList("1,*", "2,4", "3"), searchRequest.getNumberSpecimens());
    assertEquals(Arrays.asList(true, false), searchRequest.getDisplayOnNHCPortal());
    assertEquals(Arrays.asList("1,*", "2,4", "3"), searchRequest.getOccurrenceCount());
    assertEquals(Arrays.asList("1,*", "2,4", "3"), searchRequest.getTypeSpecimenCount());
    assertEquals(
        Arrays.asList(
            UUID.fromString("b600ae47-97a9-4b63-85f3-21d4f52523a1"),
            UUID.fromString("d2434ba9-10ef-4308-bd45-a69af379b5d9")),
        searchRequest.getInstitutionKeys());
    assertEquals(Collections.singletonList(Source.IH_IRN), searchRequest.getSource());
    assertEquals(Collections.singletonList("214"), searchRequest.getSourceId());
    assertEquals(Arrays.asList(Country.DENMARK, Country.SPAIN), searchRequest.getCountry());
    assertEquals(Collections.singletonList(GbifRegion.EUROPE), searchRequest.getGbifRegion());
  }
}
