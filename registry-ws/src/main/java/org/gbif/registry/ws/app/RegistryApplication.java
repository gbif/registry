package org.gbif.registry.ws.app;

import com.google.common.base.Preconditions;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;

public class RegistryApplication {

  private final static String  JETTY_PORT = "jetty.port";
  private final static String  JETTY_ADMIN_PORT = "jetty.admin.port";

  public static void main(String[] args) throws Exception
  {
    Server server = new Server();
    server.setConnectors(buildConnectors());
    server.setGracefulShutdown(1000);
    server.setStopAtShutdown(true);
    server.setHandler(buildContexts(server));
    new ShutdownHolder(server);
    server.start();
    server.join();
  }


  private static ContextHandlerCollection buildContexts(Server server){
    String webappDirLocation = "src/main/webapp/";
    ContextHandlerCollection contexts = new ContextHandlerCollection();
    WebAppContext root = new WebAppContext();
    root.setContextPath("/");
    root.setDescriptor(webappDirLocation + "/WEB-INF/web.xml");
    root.setResourceBase(webappDirLocation);
    root.setParentLoaderPriority(true);
    root.setConnectorNames(new String[] {"application"});

    ContextHandler stopContext = new ContextHandler();
    stopContext.setContextPath("/stop");
    stopContext.setHandler(new StopHandler(server,"stop"));
    stopContext.setConnectorNames(new String[] {"admin"});
    contexts.setHandlers(new Handler[]{root, stopContext });
    return contexts;
  }

  private static Connector[] buildConnectors(){
    Preconditions.checkNotNull(System.getProperty(JETTY_PORT), "Jetty port not defined");
    Preconditions.checkNotNull(System.getProperty(JETTY_ADMIN_PORT), "Jetty admin port not defined");

    SelectChannelConnector httpConnector = new SelectChannelConnector();
    httpConnector.setPort(Integer.parseInt(System.getProperty(JETTY_PORT)));
    httpConnector.setMaxIdleTime(30000); //30 seconds
    httpConnector.setRequestHeaderSize(8192); //8 kilobytes
    httpConnector.setName("application");

    SelectChannelConnector adminConnector = new SelectChannelConnector();
    adminConnector.setPort(Integer.parseInt(System.getProperty(JETTY_ADMIN_PORT)));
    adminConnector.setThreadPool(new QueuedThreadPool(65));
    adminConnector.setName("admin");

    return new Connector[]{httpConnector,adminConnector};
  }
}
