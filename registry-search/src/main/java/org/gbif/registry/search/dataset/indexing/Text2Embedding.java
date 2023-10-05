package org.gbif.registry.search.dataset.indexing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import ai.djl.util.Utils;


public class Text2Embedding {

  public static void main(String[] args) throws Exception {

    List<String> inputs = new ArrayList<>();
    inputs.add("The quick brown fox jumps over the lazy dog.");
    inputs.add("I am a sentence for which I would like to get its embedding");
    float[][] embdeddings =  predict(inputs);
    System.out.println("dasdsa");
  }

  public static float[][] predict(List<String> inputs) throws MalformedModelException, ModelNotFoundException, IOException,
    TranslateException {


    String modelUrl =
      "https://storage.googleapis.com/tfhub-modules/google/universal-sentence-encoder/4.tar.gz";

    Criteria<String[], float[][]> criteria =
      Criteria.builder()
        .optApplication(Application.NLP.TEXT_EMBEDDING)
        .setTypes(String[].class, float[][].class)
        .optModelUrls(modelUrl)
        .optEngine("TensorFlow")
        .optProgress(new ProgressBar())
        .build();

    try (ZooModel<String[], float[][]> model = criteria.loadModel();
         Predictor<String[], float[][]> predictor = model.newPredictor()) {
      return predictor.predict(inputs.toArray(Utils.EMPTY_ARRAY));
    }
  }

}
