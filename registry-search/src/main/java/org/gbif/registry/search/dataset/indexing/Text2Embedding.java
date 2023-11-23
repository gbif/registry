/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.search.dataset.indexing;

import java.io.Closeable;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.djl.Application;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;


import lombok.SneakyThrows;

public class Text2Embedding implements Closeable {

  private final Predictor<String, float[]> predictor;

  private final  Criteria<String, float[]> criteria;

  private final ZooModel<String, float[]> model;

  @SneakyThrows
  public Text2Embedding() {
    String modelUrl = "/Users/xrc439/Downloads/all-mpnet-base-v2/";

    criteria =
      Criteria.builder()
        .optApplication(Application.NLP.TEXT_EMBEDDING)
        .setTypes(String.class, float[].class)
        .optModelPath(Paths.get(modelUrl))
        .optEngine("TensorFlow")
        .optProgress(new ProgressBar())
        .optTranslator(new MyTranslator())
        .build();

    model = criteria.loadModel();
    predictor = model.newPredictor();
  }

  public static void main(String[] args) throws Exception {
    try(Text2Embedding text2Embedding = new Text2Embedding()) {

      Float[] embeddings = text2Embedding.predict("Description of Two New Species");
      StringBuilder stringBuilder = new StringBuilder();
      for (float emb : embeddings) {
        stringBuilder.append(emb);
        stringBuilder.append(", ");
      }
      System.out.println(stringBuilder);
    }
  }

  @SneakyThrows
  public Float[] predict(String input) {
    float[] embedding = predictor.predict(input);
    return IntStream.range(0, embedding.length)
              .mapToObj(Float::valueOf).collect(Collectors.toList()).toArray(new Float[]{});
  }

  public void close() {
    model.close();
    predictor.close();
  }

  private static final class MyTranslator implements Translator<String, float[]> {

    @Override
    public NDList processInput(TranslatorContext ctx, String input) {
      // manually stack for faster batch inference
      NDManager manager = ctx.getNDManager();
      return new NDList(manager.create(input));
    }

    @Override
    public float[] processOutput(TranslatorContext ctx, NDList list) {
      return list.singletonOrThrow().toFloatArray();
    }
  }

}
