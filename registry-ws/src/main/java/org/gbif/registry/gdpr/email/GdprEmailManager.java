package org.gbif.registry.gdpr.email;

import org.gbif.registry.surety.SuretyConstants;
import org.gbif.registry.surety.email.BaseEmailModel;
import org.gbif.registry.surety.email.EmailSender;
import org.gbif.registry.surety.email.EmailTemplateProcessor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.gbif.common.messaging.api.messages.GdprNotificationMessage.EntityType;

/**
 * Manager to send gdpr-related emails.
 */
@Singleton
public class GdprEmailManager {

  private static final Logger LOG = LoggerFactory.getLogger(GdprEmailManager.class);

  private final GdprEmailConfiguration config;
  private final EmailTemplateProcessor templateProcessor;
  private final EmailSender emailSender;
  private final Map<EntityType, String> urlTemplatesByEntityType = new HashMap<>();

  @Inject
  public GdprEmailManager(
    GdprEmailConfiguration config, EmailTemplateProcessor templateProcessor, EmailSender emailSender
  ) {
    this.config = config;
    this.templateProcessor = templateProcessor;
    this.emailSender = emailSender;
    initUrlTemplatesMap();
  }

  private void initUrlTemplatesMap() {
    urlTemplatesByEntityType.put(EntityType.Dataset, config.getDatasetUrlTemplate());
    urlTemplatesByEntityType.put(EntityType.Organization, config.getOrganizationUrlTemplate());
    urlTemplatesByEntityType.put(EntityType.Node, config.getNodeUrlTemplate());
    urlTemplatesByEntityType.put(EntityType.Network, config.getNetworkUrlTemplate());
    urlTemplatesByEntityType.put(EntityType.Installation, config.getInstallationUrlTemplate());
  }

  /**
   * Sends a gdpr notification email.
   *
   * @param email   destination email.
   * @param context context with information related to the entity change.
   */
  public void sendGdprNotification(String email, Map<EntityType, List<UUID>> context) {
    BaseEmailModel emailModel;
    try {
      emailModel = generateGdprNotificationEmail(email, context);
    } catch (IOException | TemplateException exc) {
      LOG.error(SuretyConstants.NOTIFY_ADMIN,
                "Error while trying to generate gdpr notificaiton email for email {}",
                email,
                exc);
      return;
    }
    emailSender.send(emailModel);
  }

  @VisibleForTesting
  BaseEmailModel generateGdprNotificationEmail(String email, Map<EntityType, List<UUID>> context)
    throws IOException, TemplateException {
    // create template data model
    GdprNotificationTemplateDataModel templateDataModel =
      GdprNotificationTemplateDataModel.newInstance(new URL(config.getInformationPage()), generateSampleUrls(context));

    // build email with the template processor
    return templateProcessor.buildEmail(email, templateDataModel, Locale.ENGLISH);
  }

  @VisibleForTesting
  List<URL> generateSampleUrls(Map<EntityType, List<UUID>> context) throws MalformedURLException {
    List<URL> urls = new ArrayList<>();

    if (Objects.nonNull(context)) {
      for (Map.Entry<EntityType, List<UUID>> entry : context.entrySet()) {
        for (UUID uuid : entry.getValue()) {
          urls.add(generateUrl(urlTemplatesByEntityType.get(entry.getKey()), uuid));
        }
      }
    }

    return urls;
  }

  private URL generateUrl(String urlTemplate, UUID uuid) throws MalformedURLException {
    return new URL(MessageFormat.format(urlTemplate, uuid.toString()));
  }

}
