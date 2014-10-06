package org.gbif.registry.ws.app;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopHandler extends AbstractHandler {

  private static Logger log = LoggerFactory.getLogger(StopHandler.class);

  private final Server server;
  private final String secret;

  public StopHandler(Server server, String secret){
    this.server = server;
    this.secret = secret;
  }

  @Override
  public void handle(
    String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response
  ) throws IOException, ServletException {
    if (!stopServer(response)){
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
      response.setContentType("text/plain");
    }
  }

  private boolean stopServer(HttpServletResponse response) throws IOException {
    log.warn("Stopping Jetty");
    response.setStatus(HttpStatus.ACCEPTED_202);
    response.setContentType("text/plain");
    ServletOutputStream os = response.getOutputStream();
    os.println("Shutting down.");
    os.close();
    response.flushBuffer();
    try {
      // Stop the server.
      new Thread(){
        @Override
        public void run() {
          ShutdownHolder.stopServer(server);
          System.exit(1);
        }
      }.start();
    } catch (Exception ex) {
      log.error("Unable to stop Jetty: " + ex);
      return false;
    }
    return true;
  }
}
