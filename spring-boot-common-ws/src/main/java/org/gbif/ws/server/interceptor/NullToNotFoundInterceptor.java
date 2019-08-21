package org.gbif.ws.server.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.gbif.ws.NotFoundException;

/**
 * This method interceptor throws a {@link NotFoundException} for every {@code null} return value of a method.
 * <p/>
 * This exception is mapped to a <em>404</em> response code (<em>Not found</em>).
 */
public class NullToNotFoundInterceptor implements MethodInterceptor {

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {

    Object result = invocation.proceed();

    if (result == null) {
      throw new NotFoundException();
    }
    return result;
  }
}
