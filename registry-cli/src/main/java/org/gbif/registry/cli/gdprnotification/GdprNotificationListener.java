package org.gbif.registry.cli.gdprnotification;

import org.gbif.common.messaging.AbstractMessageCallback;
import org.gbif.common.messaging.api.messages.GdprNotificationMessage;
import org.gbif.registry.gdpr.GdprService;
import org.gbif.registry.gdpr.email.GdprEmailManager;

import java.util.Objects;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GdprNotificationListener extends AbstractMessageCallback<GdprNotificationMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(GdprNotificationListener.class);

  private final GdprNotificationConfiguration config;
  private final GdprService gdprService;
  private final GdprEmailManager gdprEmailManager;

  private GdprNotificationListener(GdprNotificationConfiguration config) {
    this.config = config;
    Injector inj = new GdprNotificationModule(config).getInjector();
    gdprService = inj.getInstance(GdprService.class);
    gdprEmailManager = inj.getInstance(GdprEmailManager.class);
  }

  static GdprNotificationListener newInstance(GdprNotificationConfiguration config) {
    return new GdprNotificationListener(config);
  }

  @Override
  public void handleMessage(GdprNotificationMessage gdprNotificationMessage) {
    if (!config.gdprConfig.mailEnabled) {
      LOG.info("GDPR email notifications disabled");
      return;
    }

    Objects.requireNonNull(gdprNotificationMessage.getEmail());

    // double check that the email was not sent
    if (gdprService.existsNotification(gdprNotificationMessage.getEmail(), gdprNotificationMessage.getVersion())) {
      // email already sent
      LOG.info("GDPR email notification already sent to {}", gdprNotificationMessage.getEmail());
      return;
    }

    // send email
    gdprEmailManager.sendGdprNotification(gdprNotificationMessage.getEmail(), gdprNotificationMessage.getContext());

    // create notification in DB
    gdprService.createNotification(gdprNotificationMessage.getEmail(),
                                   gdprNotificationMessage.getVersion(),
                                   gdprNotificationMessage.getContext());

  }
}
