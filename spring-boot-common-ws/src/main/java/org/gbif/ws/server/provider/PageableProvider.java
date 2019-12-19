package org.gbif.ws.server.provider;

import com.google.common.annotations.VisibleForTesting;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

import static org.gbif.api.model.common.paging.PagingConstants.DEFAULT_PARAM_LIMIT;
import static org.gbif.api.model.common.paging.PagingConstants.DEFAULT_PARAM_OFFSET;
import static org.gbif.api.model.common.paging.PagingConstants.PARAM_LIMIT;
import static org.gbif.api.model.common.paging.PagingConstants.PARAM_OFFSET;
import static org.gbif.ws.util.CommonWsUtils.getFirst;

/**
 * Jersey provider class that extracts the page size and offset from the query parameters, or provides the default
 * implementation if necessary.
 * <p/>
 * Example resource use:
 * <pre>
 * {@code
 * public List<Checklist> list(@QueryParam("page") Pageable pageable) {
 *   // do stuff
 * }
 * }
 * </pre>
 * <p/>
 * Note, this implementation is based on the documentation provided on:
 * http://stackoverflow.com/questions/5722506/how-do-you-map-multiple-query-parameters-to-the-fields-of-a-bean-on-jersey-get-re
 */
public class PageableProvider implements ContextProvider<Pageable> {

  private static final Logger LOG = LoggerFactory.getLogger(PageableProvider.class);

  @VisibleForTesting
  static final int LIMIT_CAP = 1000;

  @Override
  public Pageable getValue(WebRequest webRequest) {
    return getPagingRequest(webRequest);
  }

  public static PagingRequest getPagingRequest(WebRequest webRequest) {
    Map<String, String[]> params = webRequest.getParameterMap();

    int limit = DEFAULT_PARAM_LIMIT;
    if (getFirst(params, PARAM_LIMIT) != null) {
      try {
        limit = Integer.parseInt(getFirst(params, PARAM_LIMIT));
        if (limit < 0) {
          LOG.info("Limit parameter was no positive integer [{}]. Using default {}",
              getFirst(params, PARAM_LIMIT), DEFAULT_PARAM_LIMIT);
          limit = DEFAULT_PARAM_LIMIT;
        } else if (limit > LIMIT_CAP) {
          LOG.debug("Limit parameter too high. Use maximum {}", LIMIT_CAP);
          limit = LIMIT_CAP;
        }
      } catch (NumberFormatException e) {
        LOG.warn("Unparsable value supplied for limit [{}]. Using default {}", getFirst(params, PARAM_LIMIT),
            DEFAULT_PARAM_LIMIT);
      }
    }

    long offset = DEFAULT_PARAM_OFFSET;
    if (getFirst(params, PARAM_OFFSET) != null) {
      try {
        offset = Long.parseLong(getFirst(params, PARAM_OFFSET));
        if (offset < 0) {
          LOG.warn("Offset parameter is a negative integer [{}]. Using default {}", getFirst(params, PARAM_OFFSET),
              DEFAULT_PARAM_OFFSET);
          offset = DEFAULT_PARAM_OFFSET;
        }
      } catch (NumberFormatException e) {
        LOG.warn("Unparsable value supplied for offset [{}]. Using default {}", getFirst(params, PARAM_OFFSET),
            DEFAULT_PARAM_OFFSET);
      }
    }
    return new PagingRequest(offset, limit);
  }
}
