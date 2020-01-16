package org.gbif.registry.search.dataset.indexing;

import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.search.dataset.indexing.es.IndexingConstants;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

@Slf4j
@AllArgsConstructor
public class DatasetRealtimeIndexer {

  private final RestHighLevelClient restHighLevelClient;

  private final DatasetJsonConverter datasetJsonConverter;


  /**
   * Creates or Updates asynchronously an existing dataset in ElasticSearch.
   */
  public void index(Dataset dataset) {
    IndexRequest indexRequest = new IndexRequest()
                                  .id(dataset.getKey().toString())
                                  .index(IndexingConstants.ALIAS)
                                  .type(IndexingConstants.DATASET_RECORD_TYPE)
                                  .opType(DocWriteRequest.OpType.INDEX)
                                  .source(datasetJsonConverter.convert(dataset));

   restHighLevelClient.indexAsync(indexRequest, RequestOptions.DEFAULT,
                                  new ActionListener<IndexResponse>() {
                                     @Override
                                     public void onResponse(IndexResponse indexResponse) {
                                       log.info("Dataset indexed {}, result {}", dataset, indexResponse);
                                     }
                                     @Override
                                     public void onFailure(Exception ex) {
                                       log.error("Error indexing dataset {}", dataset, ex);
                                     }
                                   });
  }

  /**
   * Deletes asynchronously a dataset from the ElasticSearch index if it exists.
   */
  public void delete(Dataset dataset) {
    DeleteRequest deleteRequest = new DeleteRequest()
                                    .id(dataset.getKey().toString())
                                    .index(IndexingConstants.ALIAS)
                                    .type(IndexingConstants.DATASET_RECORD_TYPE);

    restHighLevelClient.deleteAsync(deleteRequest, RequestOptions.DEFAULT,
                                    new ActionListener<DeleteResponse>() {
                                       @Override
                                       public void onResponse(DeleteResponse deleteResponse) {
                                         log.info("Dataset deleted {}, result {}", dataset, deleteResponse);
                                       }
                                       @Override
                                       public void onFailure(Exception ex) {
                                         log.error("Error deleting dataset {}", dataset, ex);
                                       }
                                     });
  }

}
