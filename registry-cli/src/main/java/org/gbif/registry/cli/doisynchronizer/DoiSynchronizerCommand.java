package org.gbif.registry.cli.doisynchronizer;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.RelatedIdentifierType;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteService;
import org.gbif.registry.doi.DoiGenerator;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.DoiMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.ws.util.DataCiteConverter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For the moment this Command only allows to print a report all all DOI with the FAILED status.
 *
 */
@MetaInfServices(Command.class)
public class DoiSynchronizerCommand extends BaseCommand {

  private static final Logger LOG = LoggerFactory.getLogger(DoiSynchronizerCommand.class);

  private final DoiSynchronizerConfiguration config = new DoiSynchronizerConfiguration();
  private DoiMapper doiMapper;
  private DatasetMapper datasetMapper;
  private OrganizationMapper organizationMapper;
  private DataCiteService dataCiteService;

  private DoiGenerator doiGenerator;

  public DoiSynchronizerCommand() {
    super("doi-synchronizer");
  }

  @Override
  protected Object getConfigurationObject() {
    return config;
  }

  private void setup(){
    Injector registryInj = config.createMyBatisInjector();
    doiMapper = registryInj.getInstance(DoiMapper.class);
    datasetMapper = registryInj.getInstance(DatasetMapper.class);
    organizationMapper = registryInj.getInstance(OrganizationMapper.class);
    doiGenerator = registryInj.getInstance(DoiGenerator.class);
    dataCiteService = config.createDataCiteService();
  }

  @Override
  protected void doRun() {

    setup();

    List<DOIGbifDataciteDiagnostic> diagnosticResult = runDOIStatusDiagnostic();
    printFailedStatusDoiReport(diagnosticResult);

//    for(DOIGbifDataciteDiagnostic d : diagnosticResult){
//      if(d.getDoi().equals(new DOI("10.15468/1mtkaw"))){
//        fixFailedStatusDoi(d);
//      }
//    }

  }

  /**
   * Print in the System out a diagnostic report from the diagnostic results.
   *
   * @param diagnosticResult
   */
  private void printFailedStatusDoiReport(List<DOIGbifDataciteDiagnostic> diagnosticResult){
    for(DOIGbifDataciteDiagnostic result : diagnosticResult ){
      if(result.getRelatedDataset() != null){
        System.out.println("------ Failed DOI: " + result.getDoi().getDoiName() + "------");
        System.out.println("Dataset key:" + result.getRelatedDataset().getKey());
        System.out.println("DOI found at Datacite?:" + result.isDoiExistsAtDatacite());
        if(result.isDoiExistsAtDatacite()) {
          System.out.println("DOI Status at Datacite?:" + result.getDataciteDoiStatus());
          System.out.println("Datacite Metadata equals?:" + result.isMetadataEquals());
        }
        System.out.println("Is current Dataset DOI?:" +result.isCurrentDOI());
        System.out.println("Is current Dataset DOI a GBIF DOI?:" +
                result.getRelatedDataset().getDoi().getPrefix().equals(DOI.GBIF_PREFIX));
        System.out.println("-------------------------------------------");
      }
      else{
        System.out.println("No dataset found:" + " " + result.getDoi());
      }
    }

    System.out.println("Total number of datasets: " + diagnosticResult.size());
  }

  /**
   * Compare the metadata linked to a DOI between what we have in the database and what is stored at Datacite.
   *
   * @param doi
   * @return
   */
  private boolean compareMetadataWithDatacite(DOI doi){
    String registryDoiMetadata = doiMapper.getMetadata(doi);
    String dataciteDoiMetadata = null;
    try {
      dataciteDoiMetadata = dataCiteService.getMetadata(doi);
    } catch (DoiException e) {
      e.printStackTrace();
    }
    return StringUtils.equals(registryDoiMetadata, dataciteDoiMetadata);
  }

  /**
   * Schedule a registration of the DOI.
   *
   * @param result
   */
  private void fixFailedStatusDoi(DOIGbifDataciteDiagnostic result){
    if(result.getRelatedDataset() != null){

      //if the DOI exists at Datacite, force the status to registered to allow it to get updated
      if(result.doiExistsAtDatacite && result.getDataciteDoiStatus() == DoiStatus.REGISTERED) {
        DoiData doiData = doiMapper.get(result.getDoi());
        doiMapper.update(result.getDoi(), new DoiData(DoiStatus.REGISTERED, doiData.getTarget()), "");
      }

      //if the DOI to fix is NOT the current one and the current DOI is NOT a GBIF DOI
      if(!result.isCurrentDOI() && !result.getRelatedDataset().getDoi().getPrefix().equals(DOI.GBIF_PREFIX)){
        DataCiteMetadata dataciteMetadata = buildMetadataForNonGbifDOI(result.getRelatedDataset());
        scheduleRegistration(result.getDoi(), dataciteMetadata, result.getRelatedDataset().getKey());
        System.out.println("Scheduled Registration for DOI " + result.getDoi().getDoiName());
      }
      else{
        DataCiteMetadata dataciteMetadata = buildMetadata(result.getRelatedDataset());
        scheduleRegistration(result.getDoi(), dataciteMetadata, result.getRelatedDataset().getKey());
        System.out.println("Scheduled Registration for DOI " + result.getDoi().getDoiName());
      }
    }
  }

  private void scheduleRegistration(DOI doi, DataCiteMetadata metadata, UUID datasetKey) {
    try {
      doiGenerator.registerDataset(doi, metadata, datasetKey);
    } catch (InvalidMetadataException e) {
      LOG.error("Failed to schedule DOI update for {}, dataset {}", doi, datasetKey, e);
      doiGenerator.failed(doi, e);
    }
  }

  /**
   * Get the list of DOIGbifDataciteDiagnostic.
   *
   */
  private List<DOIGbifDataciteDiagnostic> runDOIStatusDiagnostic(){
    List<DOIGbifDataciteDiagnostic> list = Lists.newArrayList();
    //get all the DOI with the FAILED status. Note that they are all GBIF DOI.
    List<Map<String,Object>> failedDoiList = doiMapper.list(DoiStatus.FAILED, DoiType.DATASET, null);
    DOI doi;
    for(Map<String,Object> failedDoi : failedDoiList ) {
      doi = new DOI((String) failedDoi.get("doi"));
      list.add(runDOIGbifDataciteDiagnostic(doi));
    }
    return list;
  }

  /**
   * Check the status of a DOI between GBIF and Datacite.
   *
   * @param doi
   * @return
   */
  private DOIGbifDataciteDiagnostic runDOIGbifDataciteDiagnostic(DOI doi) {

    DOIGbifDataciteDiagnostic doiGbifDataciteDiagnostic = new DOIGbifDataciteDiagnostic(doi);
    try {
      doiGbifDataciteDiagnostic.setDoiExistsAtDatacite(dataCiteService.exists(doi));
    } catch (DoiException e) {
      LOG.warn("Can not check existence of DOI " + doi.getDoiName(), e);
    }

    List<Dataset> datasets = datasetMapper.listByIdentifier(IdentifierType.DOI, doi.toString(), null);
    if(!datasets.isEmpty()) {
      if(datasets.size() == 1){
        doiGbifDataciteDiagnostic.setRelatedDataset(datasets.get(0));
      }
      else{
        LOG.warn("More than 1 dataset linked to DOI " + doi.getDoiName());
      }
    }
    else{
      Dataset dataset = datasetMapper.getByDOI(doi.getDoiName());
      if(dataset != null){
        doiGbifDataciteDiagnostic.setRelatedDataset(dataset);
      }
    }

    if(doiGbifDataciteDiagnostic.getRelatedDataset() != null){
      doiGbifDataciteDiagnostic.setIsCurrentDOI(doi.equals(doiGbifDataciteDiagnostic.getRelatedDataset().getDoi()));
    }

    if(doiGbifDataciteDiagnostic.isDoiExistsAtDatacite()) {
      doiGbifDataciteDiagnostic.setMetadataEquals(compareMetadataWithDatacite(doi));

      try {
        DoiData doiStatus = dataCiteService.resolve(doi);
        doiGbifDataciteDiagnostic.setDataciteDoiStatus(doiStatus.getStatus());
      } catch (DoiException e) {
        e.printStackTrace();
      }
    }

    return doiGbifDataciteDiagnostic;
  }

  //copied from Registry DatasetResource

  /**
   * If the old DOI was a GBIF one and the new one is different, update its metadata with a version relationship.
   * @param dataset (that includes the new non GBIF DOI)
   * @return
   */
  private DataCiteMetadata buildMetadataForNonGbifDOI(Dataset dataset) {
    Preconditions.checkArgument(!DOI.GBIF_PREFIX.equals(dataset.getDoi().getPrefix()));

    Organization publisher = organizationMapper.get(dataset.getPublishingOrganizationKey());
    DataCiteMetadata m = DataCiteConverter.convert(dataset, publisher);
    // add previous relationship

    m.getRelatedIdentifiers().getRelatedIdentifier()
            .add(DataCiteMetadata.RelatedIdentifiers.RelatedIdentifier.builder()
                            .withRelationType(RelationType.IS_PREVIOUS_VERSION_OF)
                            .withValue(dataset.getDoi().getDoiName())
                            .withRelatedIdentifierType(RelatedIdentifierType.DOI)
                            .build()
            );

    return m;
  }

  private DataCiteMetadata buildMetadata(Dataset dataset) {
    Organization publisher = organizationMapper.get(dataset.getPublishingOrganizationKey());
    return DataCiteConverter.convert(dataset, publisher);
  }

  /**
   * Contains all the result from the diagnostic of a single DOI.
   *
   */
  private static class DOIGbifDataciteDiagnostic {

    private DOI doi;
    private Dataset relatedDataset;

    private boolean doiExistsAtDatacite;

    private DoiStatus dataciteDoiStatus;
    private boolean metadataEquals;

    private boolean isCurrentDOI;

    public DOIGbifDataciteDiagnostic(DOI doi){
      this.doi = doi;
    }

    public DOI getDoi(){
      return doi;
    }

    public Dataset getRelatedDataset() {
      return relatedDataset;
    }

    public void setRelatedDataset(Dataset relatedDataset) {
      this.relatedDataset = relatedDataset;
    }

    public boolean isDoiExistsAtDatacite() {
      return doiExistsAtDatacite;
    }

    public void setDoiExistsAtDatacite(boolean doiExistsAtDatacite) {
      this.doiExistsAtDatacite = doiExistsAtDatacite;
    }

    public boolean isMetadataEquals() {
      return metadataEquals;
    }

    public void setMetadataEquals(boolean metadataEquals) {
      this.metadataEquals = metadataEquals;
    }

    public boolean isCurrentDOI() {
      return isCurrentDOI;
    }

    public void setIsCurrentDOI(boolean isCurrentDOI) {
      this.isCurrentDOI = isCurrentDOI;
    }

    public DoiStatus getDataciteDoiStatus() {
      return dataciteDoiStatus;
    }

    public void setDataciteDoiStatus(DoiStatus dataciteDoiStatus) {
      this.dataciteDoiStatus = dataciteDoiStatus;
    }
  }
}
