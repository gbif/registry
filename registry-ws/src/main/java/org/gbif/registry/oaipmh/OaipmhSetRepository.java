package org.gbif.registry.oaipmh;

import java.util.List;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.lyncode.xoai.dataprovider.handlers.results.ListSetsResult;
import com.lyncode.xoai.dataprovider.model.Set;
import com.lyncode.xoai.dataprovider.repository.SetRepository;

/**
 * WIP, sets should include all distinct countries, installations and dataset types.
 * Created by cgendreau on 15/09/15.
 */
@Singleton
public class OaipmhSetRepository implements SetRepository {

  private static final Set COUNTRY_SET = new Set("country").withName("per country");
  private static final Set DATASET_TYPE_SET = new Set("dataset_type").withName("per dataset type");
  private static final Set INSTALLATION_SET = new Set("installation").withName("per installation");
  private static List<Set> SETS_LIST = ImmutableList.of(COUNTRY_SET, DATASET_TYPE_SET, INSTALLATION_SET);
  private static final ListSetsResult COMPLETE_SETS = new ListSetsResult(false, SETS_LIST);
  private static final int NUMBER_OF_SETS = SETS_LIST.size();

  @Override
  public boolean supportSets() {
    return true;
  }

  @Override
  public ListSetsResult retrieveSets(int offset, int length) {

    if(offset == 0 && length == NUMBER_OF_SETS){
      return COMPLETE_SETS;
    }
    return new ListSetsResult(offset + length < NUMBER_OF_SETS, this.SETS_LIST.subList(offset, Math.min(offset + length, NUMBER_OF_SETS)));
  }

  @Override
  public boolean exists(String s) {
    return SETS_LIST.contains(s);
  }
}
