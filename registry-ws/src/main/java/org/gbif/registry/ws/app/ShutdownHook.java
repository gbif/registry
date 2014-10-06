package org.gbif.registry.ws.app;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownHook extends Thread {

  private final ShutdownHolder holder;

  public ShutdownHook(ShutdownHolder holder){
    this.holder = holder;
  }
  @Override
  public void run() {
    holder.shutdown();
  }
}
