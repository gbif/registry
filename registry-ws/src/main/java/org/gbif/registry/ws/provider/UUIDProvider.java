package org.gbif.registry.ws.provider;

import com.google.common.base.Strings;
import com.google.inject.Singleton;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.Parameter;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.UUID;

/**
 * UUID parameter provider.
 */
@Provider
@Singleton
public class UUIDProvider implements InjectableProvider<QueryParam, Parameter> {

  @Context
  private final HttpContext context;


  /**
   * Creates an instance using a http context.
   */
  public UUIDProvider(@Context HttpContext context) {
    this.context = context;
  }

  @Override
  public ComponentScope getScope() {
    return ComponentScope.PerRequest;
  }

  @Override
  public Injectable<UUID> getInjectable(ComponentContext ic, final QueryParam a, final Parameter p) {

    if (UUID.class != p.getParameterClass()) { //not a annotated date
      return null;
    }

    //Returns a new instance of the partial date provider.
    return () -> {
          if (context.getRequest().getQueryParameters() != null
              && context.getRequest().getQueryParameters().containsKey(a.value())) {
            String keyValue = context.getRequest().getQueryParameters().getFirst(a.value());
            return tryParse(keyValue, a.value());
          }
          return null;
    };
  }

  /**
   * Tries to parse the input using the supported formats.
   * Adjust the date to the first or last day of the month depending on the param name.
   */
  private UUID tryParse(String value,String paramName) {
    if (!Strings.isNullOrEmpty(value)) {
      return UUID.fromString(value);
    }
    throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
      .entity("Unaccepted parameter value " +  paramName + ":" + value).build());
  }

}
