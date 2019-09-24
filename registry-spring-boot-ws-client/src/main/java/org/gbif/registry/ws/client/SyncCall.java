package org.gbif.registry.ws.client;

import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

/**
 * Utility class to perform synchronous call on Retrofit services.
 */
public class SyncCall {

  /**
   * Private constructor.
   */
  private SyncCall() {
    //DO NOTHING
  }

  /**
   * Performs a synchronous call to {@link Call} instance.
   *
   * @param call to be executed
   * @param <T>  content of the response object
   * @return {@link Response} with content,
   * throws a {@link RuntimeException} when IOException was thrown from execute method
   */
  public static <T> Response<T> syncCallWithResponse(Call<T> call) {
    try {
      return call.execute();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
