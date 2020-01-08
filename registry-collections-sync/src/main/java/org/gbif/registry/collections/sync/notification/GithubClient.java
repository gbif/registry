package org.gbif.registry.collections.sync.notification;

import org.gbif.registry.collections.sync.http.BasicAuthInterceptor;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import static org.gbif.registry.collections.sync.http.SyncCall.syncCall;

/** Lightweight client for the Github API. */
public class GithubClient {

  private final API api;

  private GithubClient(String githubWsUrl, String user, String password) {
    OkHttpClient okHttpClient =
        new OkHttpClient.Builder().addInterceptor(new BasicAuthInterceptor(user, password)).build();

    Retrofit retrofit =
        new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(githubWsUrl)
            .addConverterFactory(JacksonConverterFactory.create())
            .build();
    api = retrofit.create(API.class);
  }

  public static GithubClient create(String grSciCollWsUrl, String user, String password) {
    return new GithubClient(grSciCollWsUrl, user, password);
  }

  public void createIssue(String title, String body, List<String> asignees, List<String> labels) {
    syncCall(api.createIssue(new Issue(title, body, asignees, labels)));
  }

  private interface API {
    @POST("issues")
    Call<Void> createIssue(@Body Issue issue);
  }

  @Data
  @AllArgsConstructor
  private static class Issue {
    private String title;
    private String body;
    private List<String> asignees;
    private List<String> labels;
  }
}
