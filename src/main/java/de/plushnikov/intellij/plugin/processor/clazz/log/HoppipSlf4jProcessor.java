package de.plushnikov.intellij.plugin.processor.clazz.log;

import lombok.extern.hoppip.HoppipSlf4j;
/**
 * @className HoppipSlf4jProcessor
 * @author: liuzhen
 * @create: 2019-12-26 02:12
 */
public class HoppipSlf4jProcessor extends de.plushnikov.intellij.plugin.processor.clazz.log.AbstractTopicSupportingSimpleLogProcessor {
  private static final String LOGGER_TYPE = "com.souche.hoppip.logger.Logger";
  private static final String LOGGER_INITIALIZER = "com.souche.hoppip.logger.LoggerManager.getLogger(%s)";

  public HoppipSlf4jProcessor() {
    super(HoppipSlf4j.class, LOGGER_TYPE, LOGGER_INITIALIZER, LoggerInitializerParameter.TYPE);
  }
}
