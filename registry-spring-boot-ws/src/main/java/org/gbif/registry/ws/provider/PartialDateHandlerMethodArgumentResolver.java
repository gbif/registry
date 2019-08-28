package org.gbif.registry.ws.provider;

import com.google.common.base.Strings;
import org.gbif.ws.WebApplicationException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

/**
 * Provider that accepts and transforms partial Dates.
 * For example: 01-2018 or 01/2018 will be translated into 01-01-2018.
 */
public class PartialDateHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

  private static final String YEAR_ONLY_FORMAT = "yyyy";
  private static final String[] SUPPORTED_FORMATS = new String[]{"yyyy/MM", "yyyy-MM", YEAR_ONLY_FORMAT};

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return Date.class.equals(parameter.getParameterType()) && parameter.getParameterAnnotation(PartialDate.class) != null;
  }

  @Override
  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
    final String paramName = parameter.getParameter().getName();
    final String paramValue = webRequest.getParameter(paramName);
    return Optional.ofNullable(paramValue)
        .map(value -> tryDateParse(value, paramName))
        .orElse(null);
  }

  /**
   * Tries to parse the input using the supported formats.
   * Adjust the date to the first or last day of the month depending on the param name.
   */
  private Date tryDateParse(String dateValue, String paramName) {
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
    throw new WebApplicationException(ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body("Unaccepted parameter value " + paramName + ":" + dateValue));
  }
}
