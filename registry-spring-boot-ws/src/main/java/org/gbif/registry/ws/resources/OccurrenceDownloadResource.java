package org.gbif.registry.ws.resources;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.doi.generator.DoiGenerator;
import org.gbif.registry.doi.handler.DataCiteDoiHandlerStrategy;
import org.gbif.registry.persistence.mapper.DatasetOccurrenceDownloadMapper;
import org.gbif.registry.persistence.mapper.OccurrenceDownloadMapper;
import org.gbif.registry.ws.Trim;
import org.gbif.registry.ws.provider.PartialDate;
import org.gbif.ws.NotFoundException;
import org.gbif.ws.server.interceptor.NullToNotFound;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.util.DownloadSecurityUtils.checkUserIsInSecurityContext;
import static org.gbif.registry.ws.util.DownloadSecurityUtils.clearSensitiveData;

/**
 * Occurrence download resource/web service.
 */
// TODO: 27/08/2019 add javascript produce type
// TODO: 27/08/2019 implement validation
@RestController
@RequestMapping(value = "occurrence/download", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class OccurrenceDownloadResource implements OccurrenceDownloadService {

  private final OccurrenceDownloadMapper occurrenceDownloadMapper;
  private final DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper;
  private final IdentityAccessService identityService;
  private final DataCiteDoiHandlerStrategy doiHandlingStrategy;
  private final DoiGenerator doiGenerator;

  //Page size to iterate over dataset usages
  private static final int BATCH_SIZE = 5_000;

  public OccurrenceDownloadResource(OccurrenceDownloadMapper occurrenceDownloadMapper,
                                    DatasetOccurrenceDownloadMapper datasetOccurrenceDownloadMapper,
                                    DoiGenerator doiGenerator,
                                    @Lazy DataCiteDoiHandlerStrategy doiHandlingStrategy,
                                    IdentityAccessService identityService) {
    this.occurrenceDownloadMapper = occurrenceDownloadMapper;
    this.datasetOccurrenceDownloadMapper = datasetOccurrenceDownloadMapper;
    this.doiHandlingStrategy = doiHandlingStrategy;
    this.doiGenerator = doiGenerator;
    this.identityService = identityService;
  }

  @RequestMapping(method = RequestMethod.POST)
  @Trim
  @Transactional
//  @Validate(groups = {PrePersist.class, Default.class})
  @Secured(ADMIN_ROLE)
  @Override
  public void create(@RequestBody @Valid @NotNull @Trim Download occurrenceDownload) {
    occurrenceDownload.setDoi(doiGenerator.newDownloadDOI());
    occurrenceDownload.setLicense(License.UNSPECIFIED);
    occurrenceDownloadMapper.create(occurrenceDownload);
  }

  @GetMapping("{key}")
  @Nullable
  @NullToNotFound
  @Override
  public Download get(@NotNull @PathVariable("key") String key) {
    Download download = occurrenceDownloadMapper.get(key);
    if (download == null && DOI.isParsable(key)) { //maybe it's a DOI?
      download = occurrenceDownloadMapper.getByDOI(new DOI(key));
    }
    if (download != null) { // the user can request a non-existing download
      final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      clearSensitiveData(authentication, download);
    }
    return download;
  }

  /**
   * Lists all the downloads. This operation can be executed by role ADMIN only.
   */
  @RequestMapping(method = RequestMethod.GET)
  @Secured(ADMIN_ROLE)
  @Override
  public PagingResponse<Download> list(Pageable page, @Nullable @RequestParam("status") Set<Download.Status> status) {
    if (status == null || status.isEmpty()) {
      return new PagingResponse<>(page, (long) occurrenceDownloadMapper.count(), occurrenceDownloadMapper.list(page));
    } else {
      return new PagingResponse<>(page, (long) occurrenceDownloadMapper.countByStatus(status), occurrenceDownloadMapper.listByStatus(page, status));
    }
  }

  @GetMapping("user/{user}")
  @NullToNotFound
  public PagingResponse<Download> listByUser(@NotNull @PathVariable("user") String user, Pageable page,
                                             @Nullable @RequestParam(value = "status", required = false) Set<Download.Status> status) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    checkUserIsInSecurityContext(user, authentication);
    return new PagingResponse<>(page, (long) occurrenceDownloadMapper.countByUser(user, status),
        occurrenceDownloadMapper.listByUser(user, page, status));
  }

  @PutMapping("{key}")
  @Transactional
  @Override
  public void update(@RequestBody @NotNull Download download) {
    // The current download is retrieved because its user could be modified during the update
    Download currentDownload = get(download.getKey());
    Preconditions.checkNotNull(currentDownload);
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final UserDetails principal = (UserDetails) authentication.getPrincipal();
    checkUserIsInSecurityContext(currentDownload.getRequest().getCreator(), authentication);
    GbifUser user = identityService.get(principal.getUsername());
    doiHandlingStrategy.downloadChanged(download, currentDownload, user);
    occurrenceDownloadMapper.update(download);
  }

  @GetMapping("{key}/datasets")
  @Override
  @NullToNotFound
  public PagingResponse<DatasetOccurrenceDownloadUsage> listDatasetUsages(@NotNull @PathVariable("key") String downloadKey,
                                                                          Pageable page) {
    Download download = get(downloadKey);
    if (download != null) {
      List<DatasetOccurrenceDownloadUsage> usages = datasetOccurrenceDownloadMapper.listByDownload(downloadKey, page);
      final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      clearSensitiveData(authentication, usages);
      return new PagingResponse<>(page, download.getNumberDatasets(), usages);
    }
    throw new NotFoundException();
  }

  @PostMapping("{key}/datasets")
  @Transactional
//  @Validate(groups = {PrePersist.class, Default.class})
  @Secured(ADMIN_ROLE)
  @Override
  public void createUsages(@NotNull @PathVariable("key") String downloadKey,
                           @RequestBody @Valid @NotNull Map<UUID, Long> datasetCitations) {
    Iterators.partition(datasetCitations.entrySet().iterator(), BATCH_SIZE)
        .forEachRemaining(batch -> datasetOccurrenceDownloadMapper.createUsages(downloadKey, batch.stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue))));
  }

  @GetMapping("statistics/downloadsByUserCountry")
  @Override
  @NullToNotFound
  public Map<Integer, Map<Integer, Long>> getDownloadsByUserCountry(@Nullable @RequestParam(value = "fromDate", required = false) @PartialDate Date fromDate,
                                                                    @Nullable @RequestParam(value = "toDate", required = false) @PartialDate Date toDate,
                                                                    @RequestParam("userCountry") Country userCountry) {
    return groupByYear(occurrenceDownloadMapper.getDownloadsByUserCountry(fromDate, toDate,
        Optional.ofNullable(userCountry).map(Country::getIso2LetterCode).orElse(null)));
  }

  // TODO: 27/08/2019 test PartialDate
  @GetMapping("statistics/downloadedRecordsByDataset")
  @Override
  @NullToNotFound
  public Map<Integer, Map<Integer, Long>> getDownloadedRecordsByDataset(@Nullable @RequestParam(value = "fromDate", required = false) @PartialDate Date fromDate,
                                                                        @Nullable @RequestParam(value = "toDate", required = false) @PartialDate Date toDate,
                                                                        @RequestParam("publishingCountry") Country publishingCountry,
                                                                        @RequestParam("datasetKey") UUID datasetKey) {
    return groupByYear(occurrenceDownloadMapper.getDownloadedRecordsByDataset(fromDate, toDate,
        Optional.ofNullable(publishingCountry).map(Country::getIso2LetterCode).orElse(null),
        datasetKey));
  }

  /**
   * Aggregates the download statistics in tree structure of month grouped by year.
   */
  private Map<Integer, Map<Integer, Long>> groupByYear(List<Facet.Count> counts) {
    Map<Integer, Map<Integer, Long>> yearsGrouping = new TreeMap<>();
    counts.forEach(count -> yearsGrouping.computeIfAbsent(Integer.valueOf(count.getName().substring(0, 4)), year -> new TreeMap<>()).put(Integer.valueOf(count.getName().substring(5)), count.getCount()));
    return yearsGrouping;
  }
}
