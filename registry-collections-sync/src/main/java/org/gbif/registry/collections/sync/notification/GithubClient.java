package org.gbif.registry.collections.sync.notification;

import org.gbif.registry.collections.sync.SyncConfig;
import org.gbif.registry.collections.sync.http.BasicAuthInterceptor;

import java.util.List;
import java.util.Objects;

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
  private final List<String> assignees;

  private GithubClient(String githubWsUrl, String user, String password, List<String> assignees) {
    Objects.requireNonNull(githubWsUrl);
    Objects.requireNonNull(user);
    Objects.requireNonNull(password);

    OkHttpClient okHttpClient =
        new OkHttpClient.Builder().addInterceptor(new BasicAuthInterceptor(user, password)).build();

    Retrofit retrofit =
        new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(githubWsUrl)
            .addConverterFactory(JacksonConverterFactory.create())
            .build();
    api = retrofit.create(API.class);
    this.assignees = assignees;
  }

  public static GithubClient create(String githubWsUrl, String user, String password) {
    return new GithubClient(githubWsUrl, user, password, null);
  }

  public static GithubClient create(SyncConfig syncConfig) {
    Objects.requireNonNull(syncConfig);
    Objects.requireNonNull(syncConfig.getNotification());
    return new GithubClient(
        syncConfig.getNotification().getGithubWsUrl(),
        syncConfig.getNotification().getGithubUser(),
        syncConfig.getNotification().getGithubPassword(),
        syncConfig.getNotification().getGhIssuesAssignees());
  }

  public void createIssue(Issue issue) {
    if (assignees != null && !assignees.isEmpty()) {
      // we use the assignees from the config if they were set
      issue.setAssignees(assignees);
    }

    syncCall(api.createIssue(issue));
  }

  private interface API {
    @POST("issues")
    Call<Void> createIssue(@Body Issue issue);
  }
}
