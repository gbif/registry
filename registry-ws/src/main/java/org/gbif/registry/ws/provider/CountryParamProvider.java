package org.gbif.registry.ws.provider;

import com.google.inject.Singleton;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.model.Parameter;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;

import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

/**
 * UUID parameter provider.
 */
@Provider
@Singleton
public class CountryParamProvider implements InjectableProvider<QueryParam, Parameter> {

  @Context
  private final HttpContext context;


  /**
   * Creates an instance using a http context.
   */
  public CountryParamProvider(@Context HttpContext context) {
    this.context = context;
  }

  @Override
  public ComponentScope getScope() {
    return ComponentScope.PerRequest;
  }

  @Override
  public Injectable<Country> getInjectable(ComponentContext ic, final QueryParam a, final Parameter p) {

    if (Country.class != p.getParameterClass()) { //not a annotated date
      return null;
    }

    //Returns a new instance of the partial date provider.
    return () -> {
          if (context.getRequest().getQueryParameters() != null
              && context.getRequest().getQueryParameters().containsKey(a.value())) {
            String value = context.getRequest().getQueryParameters().getFirst(a.value()).trim();
            // first try iso codes
            Country c = Country.fromIsoCode(value);
            if (c == null) {
              // if nothing found also try by enum name
              c = VocabularyUtils.lookupEnum(value, Country.class);
            }
            return c;
          }
          return null;
    };
  }

}
