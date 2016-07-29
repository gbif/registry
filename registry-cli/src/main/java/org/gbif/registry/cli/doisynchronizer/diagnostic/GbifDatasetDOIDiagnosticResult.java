package org.gbif.registry.cli.doisynchronizer.diagnostic;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Dataset;

import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Container object that represents the result of the diagnostic of a single Dataset DOI.
 */
@NotThreadSafe
public class GbifDatasetDOIDiagnosticResult extends GbifDOIDiagnosticResult {

  private static final Joiner JOINER = Joiner.on(',');
  private List<Dataset> relatedDataset;

  private boolean doiIsInAlternateIdentifiers;

  public GbifDatasetDOIDiagnosticResult(DOI doi){
    super(doi);
  }

  public boolean isDoiIsInAlternateIdentifiers() {
    return doiIsInAlternateIdentifiers;
  }

  public void setDoiIsInAlternateIdentifiers(boolean doiIsInAlternateIdentifiers) {
    this.doiIsInAlternateIdentifiers = doiIsInAlternateIdentifiers;
  }

  public List<Dataset> getRelatedDatasetList() {
    return relatedDataset;
  }

  public boolean isLinkedToASingleDataset(){
    return relatedDataset != null && relatedDataset.size() == 1;
  }

  public boolean isCurrentDOI(){
    Preconditions.checkArgument(relatedDataset.size() == 1,
            "This method can only be used when there is a single related dataset");
    return doi.equals(getRelatedDataset().getDoi());
  }

  public Dataset getRelatedDataset() {
    Preconditions.checkArgument(relatedDataset.size() == 1,
            "This method can only be used when there is a single related dataset");
    return relatedDataset.get(0);
  }

  public void setRelatedDataset(List<Dataset> relatedDataset) {
    this.relatedDataset = relatedDataset;
  }

  public List<String> getContextInformation(){

    List<String> contextInformation = Lists.newArrayList();

    if(getRelatedDatasetList() == null || getRelatedDatasetList().isEmpty()){
      contextInformation.add("WARNING: No dataset found");
      return contextInformation;
    }

    //from here we know we have at least on dataset linked to this DOI
    List<String> relatedDatasetKeys = Lists.newArrayListWithCapacity(relatedDataset.size());
    for(Dataset dataset : relatedDataset){
      relatedDatasetKeys.add(dataset.getKey().toString());
    }

    contextInformation.add("Dataset key: " + JOINER.join(relatedDatasetKeys));

    if(isLinkedToASingleDataset()){
      boolean isCurrentDOI = isCurrentDOI();
      contextInformation.add("Is current DOI in Dataset table?: " + isCurrentDOI);
      if(!isCurrentDOI) {
        contextInformation.add("Current DOI: " + getRelatedDataset().getDoi().getDoiName());
      }
      contextInformation.add("DOI (" + doi.getDoiName() + ") is in dataset alternative identifiers?: "
              + isDoiIsInAlternateIdentifiers());
    }
    else{
      contextInformation.add("WARNING: DOI used by multiple datasets");
    }
    return contextInformation;
  }

}
