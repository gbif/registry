package org.gbif.registry.ws.app;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownHolder {

  private static Logger log = LoggerFactory.getLogger(ShutdownHolder.class);

  private final Server server;

  public ShutdownHolder(Server server){
    this.server = server;
    Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
  }

  public void shutdown(){
    stopServer(server);
  }

  public static void stopServer(Server server){
    if(server.isStarted() || server.isRunning()) {
      try {
        log.info("Shutting down Jetty...");
        server.stop();
        log.info("Jetty has stopped.");
      } catch (Exception ex) {
        log.error("Error when stopping Jetty: " + ex.getMessage(), ex);
        ex.printStackTrace();
      }
    }
  }
}
