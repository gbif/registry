package org.gbif.registry.cli.dataprivacynotification;

import org.gbif.common.messaging.AbstractMessageCallback;
import org.gbif.common.messaging.api.messages.DataPrivacyNotificationMessage;
import org.gbif.registry.dataprivacy.DataPrivacyService;
import org.gbif.registry.dataprivacy.email.DataPrivacyEmailManager;

import java.util.Objects;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataPrivacyNotificationListener extends AbstractMessageCallback<DataPrivacyNotificationMessage> {

  private static final Logger LOG = LoggerFactory.getLogger(DataPrivacyNotificationListener.class);

  private final DataPrivacyNotificationConfiguration config;
  private final DataPrivacyService dataPrivacyService;
  private final DataPrivacyEmailManager dataPrivacyEmailManager;

  private DataPrivacyNotificationListener(DataPrivacyNotificationConfiguration config) {
    this.config = config;
    Injector inj = new DataPrivacyNotificationModule(config).getInjector();
    dataPrivacyService = inj.getInstance(DataPrivacyService.class);
    dataPrivacyEmailManager = inj.getInstance(DataPrivacyEmailManager.class);
  }

  static DataPrivacyNotificationListener newInstance(DataPrivacyNotificationConfiguration config) {
    return new DataPrivacyNotificationListener(config);
  }

  @Override
  public void handleMessage(DataPrivacyNotificationMessage dataPrivacyNotificationMessage) {
    if (!config.dataPrivacyConfig.mailEnabled) {
      LOG.info("Data privacy email notifications disabled");
      return;
    }

    Objects.requireNonNull(dataPrivacyNotificationMessage.getEmail());

    // double check that the email was not sent
    if (dataPrivacyService.existsNotification(dataPrivacyNotificationMessage.getEmail(),
                                              dataPrivacyNotificationMessage.getVersion())) {
      // email already sent
      LOG.info("Data privacy email notification already sent to {}", dataPrivacyNotificationMessage.getEmail());
      return;
    }

    // send email
    boolean sent = dataPrivacyEmailManager.sendDataPrivacyNotification(dataPrivacyNotificationMessage.getEmail(),
                                                                       dataPrivacyNotificationMessage.getContext());

    // create notification in DB
    if (sent) {
      dataPrivacyService.createNotification(dataPrivacyNotificationMessage.getEmail(),
                                            dataPrivacyNotificationMessage.getVersion(),
                                            dataPrivacyNotificationMessage.getContext());
    }

  }
}
