package org.gbif.registry.cli.doisynchronizer;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;
import org.gbif.doi.service.DoiException;
import org.gbif.doi.service.datacite.DataCiteService;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.doi.generator.DoiGenerator;
import org.gbif.registry.doi.handler.DataCiteDoiHandlerStrategy;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.DoiMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This Command allows to print a report of all DOI with the FAILED status and/or fix them.
 * Currently limited to Dataset.
 * There is currently no way to specify a single DOI through the command line.
 *
 */
@MetaInfServices(Command.class)
public class DoiSynchronizerCommand extends BaseCommand {

  private static final Logger LOG = LoggerFactory.getLogger(DoiSynchronizerCommand.class);

  private final DoiSynchronizerConfiguration config = new DoiSynchronizerConfiguration();
  private DoiMapper doiMapper;
  private DataCiteDoiHandlerStrategy dataCiteDoiHandlerStrategy;
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
    Injector registryInj = config.createRegistryInjector();
    doiMapper = registryInj.getInstance(DoiMapper.class);
    dataCiteDoiHandlerStrategy = registryInj.getInstance(DataCiteDoiHandlerStrategy.class);
    datasetMapper = registryInj.getInstance(DatasetMapper.class);
    organizationMapper = registryInj.getInstance(OrganizationMapper.class);
    doiGenerator = registryInj.getInstance(DoiGenerator.class);
    dataCiteService = config.createDataCiteService();
  }

  @Override
  protected void doRun() {

    setup();

    List<DOIGbifDataciteDiagnostic> datasetDiagnosticResult = runDOIStatusDiagnostic(DoiType.DATASET);

    if(config.printReport) {
      printFailedStatusDoiReport(datasetDiagnosticResult);
    }

    // Should we try to fix those DOI ?
    if(config.fixDOI){
      for(DOIGbifDataciteDiagnostic d : datasetDiagnosticResult) {
        // waiting for code review
        // fixFailedStatusDoi(d);
      }
    }
  }

  /**
   * Print in the console a diagnostic report based on the diagnostic results.
   *
   * @param diagnosticResult
   */
  private void printFailedStatusDoiReport(List<DOIGbifDataciteDiagnostic> diagnosticResult){
    for(DOIGbifDataciteDiagnostic result : diagnosticResult ){
      if(result.isLinkedToASingleDataset()){
        System.out.println("------ Failed DOI: " + result.getDoi().getDoiName() + "------");
        System.out.println("Dataset key: " + result.getRelatedDataset().getKey());
        System.out.println("DOI found at Datacite?: " + result.isDoiExistsAtDatacite());
        if(result.isDoiExistsAtDatacite()) {
          System.out.println("DOI Status at Datacite?: " + result.getDataciteDoiStatus());
          System.out.println("Datacite Metadata equals?: " + result.isMetadataEquals());
          System.out.println("Datacite target URI: " + result.getDataciteTarget());
        }
        System.out.println("Is current Dataset DOI?: " +result.isCurrentDOI());
        System.out.println("Is current Dataset DOI a GBIF DOI?: " +
                result.getRelatedDataset().getDoi().getPrefix().equals(DOI.GBIF_PREFIX));
        System.out.println("-------------------------------------------");
      }
      else{
        if(result.getRelatedDatasetList() != null && result.getRelatedDatasetList().size() != 0){
          System.out.println("------ DOI used by multiple datasets: " + result.getDoi().getDoiName() + "------");
          for(Dataset dataset : result.getRelatedDatasetList()){
            System.out.println("Dataset key: " + dataset.getKey());
          }
        }
        else {
          System.out.println("No dataset found: " + result.getDoi());
        }
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
      LOG.error("Can't compare DOI metadata", e);
    }
    return StringUtils.equals(registryDoiMetadata, dataciteDoiMetadata);
  }

  /**
   * Try to fix a DOI based on the {@link DOIGbifDataciteDiagnostic}.
   *
   *
   *
   * @param result
   */
  private void fixFailedStatusDoi(DOIGbifDataciteDiagnostic result){

    if(result.getRelatedDataset() != null){
      if(result.isCurrentDOI()){
        dataCiteDoiHandlerStrategy.datasetChanged(result.getRelatedDataset(), null);
        System.out.println("datasetChanged triggered on dataCiteDoiHandlerStrategy");
      }
      else{
        System.out.println("not implemented yet");

      }
//      if(!result.isCurrentDOI() && !result.getRelatedDataset().getDoi().getPrefix().equals(DOI.GBIF_PREFIX)){
//        dataCiteDoiHandlerStrategy.datasetChanged(result.getRelatedDataset(), result.getDoi());
//        //dataciteMetadata = buildMetadataForNonGbifDOI(result.getRelatedDataset());
//      }
//      else{
//        dataciteMetadata = buildMetadata(result.getRelatedDataset());
//        dataCiteDoiHandlerStrategy.
//      }
     // scheduleRegistration(result.getDoi(), dataciteMetadata, result.getRelatedDataset().getKey());
     // System.out.println("Scheduled Registration for DOI " + result.getDoi().getDoiName());
    }
  }

  /**
   * Get the list of DOIGbifDataciteDiagnostic for a specific DoiType.
   *
   * @param doiType
   *
   */
  private List<DOIGbifDataciteDiagnostic> runDOIStatusDiagnostic(DoiType doiType){
    List<DOIGbifDataciteDiagnostic> list = Lists.newArrayList();
    //get all the DOI with the FAILED status. Note that they are all GBIF assigned DOI.
    List<Map<String,Object>> failedDoiList = doiMapper.list(DoiStatus.FAILED, doiType, null);
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
    //Could they conflict??
    if(!datasets.isEmpty()) {
      doiGbifDataciteDiagnostic.setRelatedDataset(datasets);
    }
    else{
      doiGbifDataciteDiagnostic.setRelatedDataset(datasetMapper.listByDOI(doi.getDoiName()));
    }

    if(doiGbifDataciteDiagnostic.isLinkedToASingleDataset()){
      doiGbifDataciteDiagnostic.setIsCurrentDOI(doi.equals(doiGbifDataciteDiagnostic.getRelatedDataset().getDoi()));
    }

    if(doiGbifDataciteDiagnostic.isDoiExistsAtDatacite()) {
      doiGbifDataciteDiagnostic.setMetadataEquals(compareMetadataWithDatacite(doi));

      try {
        DoiData doiStatus = dataCiteService.resolve(doi);
        doiGbifDataciteDiagnostic.setDataciteDoiStatus(doiStatus.getStatus());
        doiGbifDataciteDiagnostic.setDataciteTarget(doiStatus.getTarget());
      } catch (DoiException e) {
        LOG.error("Failed to resolve DOI {}", doi);
      }
    }
    return doiGbifDataciteDiagnostic;
  }

  /**
   * Contains all the result from the diagnostic of a single DOI.
   *
   */
  private static class DOIGbifDataciteDiagnostic {

    private DOI doi;
    private List<Dataset> relatedDataset;

    private boolean doiExistsAtDatacite;

    private DoiStatus dataciteDoiStatus;

    private URI dataciteTarget;
    private boolean metadataEquals;

    private boolean isCurrentDOI;

    public DOIGbifDataciteDiagnostic(DOI doi){
      this.doi = doi;
    }

    public DOI getDoi(){
      return doi;
    }

    public List<Dataset> getRelatedDatasetList() {
      return relatedDataset;
    }

    public boolean isLinkedToASingleDataset(){
      return relatedDataset != null && relatedDataset.size() == 1;
    }

    public Dataset getRelatedDataset() {
      Preconditions.checkArgument(relatedDataset.size() == 1,
              "This method can only be used when there is a single related dataset");
      return relatedDataset.get(0);
    }

    public void setRelatedDataset(List<Dataset> relatedDataset) {
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

    public URI getDataciteTarget() {
      return dataciteTarget;
    }

    public void setDataciteTarget(URI dataciteTarget) {
      this.dataciteTarget = dataciteTarget;
    }
  }
}
