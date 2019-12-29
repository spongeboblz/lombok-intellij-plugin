package de.plushnikov.intellij.plugin.processor.clazz.log;

import lombok.extern.sponge.SpongeLog;
/**
 * @className SpongeLogProcessor
 * @author: liuzhen
 * @create: 2019-12-15 19:46
 */
public class SpongeLogProcessor extends de.plushnikov.intellij.plugin.processor.clazz.log.AbstractTopicSupportingSimpleLogProcessor {

  private static final String LOGGER_TYPE = "org.slf4j.Logger";
  private static final String LOGGER_INITIALIZER = "org.slf4j.LoggerFactory.getLogger(%s)";

  public SpongeLogProcessor() {
    super(SpongeLog.class, LOGGER_TYPE, LOGGER_INITIALIZER, LoggerInitializerParameter.TYPE);
  }
}
