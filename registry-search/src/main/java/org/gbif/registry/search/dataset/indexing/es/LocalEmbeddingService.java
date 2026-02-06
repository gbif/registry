package org.gbif.registry.search.dataset.indexing.es;

import lombok.SneakyThrows;

import org.gbif.api.model.registry.Dataset;

import org.springframework.stereotype.Service;

import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;

@Service
public class LocalEmbeddingService {

  private final ZooModel<String, float[]> model;
  private final Predictor<String, float[]> predictor;

  @SneakyThrows
  public LocalEmbeddingService(String modelUrl) {
    Criteria<String, float[]> criteria = Criteria.builder()
      .setTypes(String.class, float[].class)
      .optModelUrls(modelUrl)
      .optEngine("PyTorch")
      .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
      .build();

    this.model = criteria.loadModel();
    this.predictor = model.newPredictor();
  }

  public String buildEmbeddingText(Dataset dataset) {
    StringBuilder sb = new StringBuilder();
    if (dataset.getTitle() != null) sb.append(dataset.getTitle()).append(" ");
    if (dataset.getDescription() != null) sb.append(dataset.getDescription()).append(" ");
    if (dataset.getKeywordCollections() != null) {
      dataset.getKeywordCollections().forEach(kc -> {
        if (kc.getKeywords() != null) {
          kc.getKeywords().forEach(k -> sb.append(k).append(" "));
        }
      });
    }
    return sb.toString().trim();
  }

  public float[] generateEmbedding(String text) {
    try {
      return predictor.predict(text);
    } catch (Exception e) {
      throw new RuntimeException("Embedding generation failed", e);
    }
  }
}
