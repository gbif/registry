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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

/**
 * Provider that accepts and transforms partial Dates.
 * For example: 01-2018 or 01/2018 will be translated into 01-01-2018.
 */
@Provider
@Singleton
public class PartialDateProvider implements InjectableProvider<QueryParam, Parameter> {


  private static final String YEAR_ONLY_FORMAT =  "yyyy";
  private static final String[] SUPPORTED_FORMATS = new String[]{"yyyy/MM","yyyy-MM", YEAR_ONLY_FORMAT};

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

    //Returns a new instance of the partial date provider.
    return () -> {
          if (context.getRequest().getQueryParameters() != null
              && context.getRequest().getQueryParameters().containsKey(a.value())) {
            String dateValue = context.getRequest().getQueryParameters().getFirst(a.value());
            return tryDateParse(dateValue, a.value());
          }
          return null;
    };
  }

  /**
   * Tries to parse the input using the supported formats.
   * Adjust the date to the first or last day of the month depending on the param name.
   */
  private Date tryDateParse(String dateValue,String paramName) {
    if (!Strings.isNullOrEmpty(dateValue)) {
      for (String dateFormat : SUPPORTED_FORMATS) {
        try {
          Date date = new SimpleDateFormat(dateFormat).parse(dateValue);
          Calendar cal = Calendar.getInstance();
          cal.setTime(date);
          if (paramName.startsWith("from")) {
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
          } else if (paramName.startsWith("to")) {
            if (YEAR_ONLY_FORMAT.equals(dateFormat)) {
              cal.set(Calendar.MONTH, cal.getActualMaximum(Calendar.MONTH));
            }
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
          }
          return cal.getTime();
        } catch (ParseException ex) {
          // DO NOTHING
        }
      }
    }
    throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
      .entity("Unaccepted parameter value " +  paramName + ":" + dateValue).build());
  }

}
