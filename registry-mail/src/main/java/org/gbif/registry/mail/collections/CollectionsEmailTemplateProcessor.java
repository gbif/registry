package org.gbif.registry.mail.collections;

import org.gbif.registry.mail.FreemarkerEmailTemplateProcessor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("collectionsEmailTemplateProcessor")
public class CollectionsEmailTemplateProcessor extends FreemarkerEmailTemplateProcessor {}
