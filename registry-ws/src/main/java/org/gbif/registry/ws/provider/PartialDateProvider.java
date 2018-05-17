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
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * This annotation is used to mark Date parameter that can accept partial dates as input.
 * For example: 01-2018 or 01/2018 will be translated into 01-01-2018.
 */
@Provider
@Singleton
public class PartialDateProvider implements InjectableProvider<QueryParam, Parameter> {


  private static final String[] SUPPORTED_FORMATS = new String[]{"MM/yyyy","MM-yyyy"};

  @Context
  private final HttpContext context;

  /**
   * Has the ic parameter the {@link PartialDate} annotation.
   */
  private boolean hasAnnotation(ComponentContext ic) {
    return (ic.getAnnotations() != null && Arrays.stream(ic.getAnnotations()).anyMatch(a -> a.getClass() == PartialDate.class));
  }

  /**
   * Creates an instance using a http context.
   */
  public PartialDateProvider(@Context HttpContext context) {
    this.context = context;
  }

  @Override
  public ComponentScope getScope() {
    return ComponentScope.PerRequest;
  }

  @Override
  public Injectable<Date> getInjectable(ComponentContext ic, final QueryParam a, final Parameter c) {

    if (Date.class != c.getParameterClass() && !hasAnnotation(ic)) { //not a annotated date
      return null;
    }

    //Resturns a new instance of the partial date provider.
    return () -> {
          if (context.getRequest().getQueryParameters() != null
              && context.getRequest().getQueryParameters().containsKey(a.value())) {
            String dateValue = context.getRequest().getQueryParameters().getFirst(a.value());
            return Strings.isNullOrEmpty(dateValue)? null : tryDateParse(dateValue);
          }
          return null;
    };
  }


  /**
   * Tries to parse the input using the supported formats.
   */
  private Date tryDateParse(String dateValue) {
    for(String dateFormat : SUPPORTED_FORMATS) {
      try {
        return new SimpleDateFormat(dateFormat).parse(dateValue);
      } catch (ParseException ex) {
        //DO NOTHING
      }
    }
    return null;
  }
}
