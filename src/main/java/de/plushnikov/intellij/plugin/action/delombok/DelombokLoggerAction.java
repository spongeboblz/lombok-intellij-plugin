package de.plushnikov.intellij.plugin.action.delombok;

import com.intellij.openapi.components.ServiceManager;
import de.plushnikov.intellij.plugin.processor.clazz.log.CommonsLogProcessor;
import de.plushnikov.intellij.plugin.processor.clazz.log.CustomLogProcessor;
import de.plushnikov.intellij.plugin.processor.clazz.log.FloggerProcessor;
import de.plushnikov.intellij.plugin.processor.clazz.log.JBossLogProcessor;
import de.plushnikov.intellij.plugin.processor.clazz.log.Log4j2Processor;
import de.plushnikov.intellij.plugin.processor.clazz.log.Log4jProcessor;
import de.plushnikov.intellij.plugin.processor.clazz.log.LogProcessor;
import de.plushnikov.intellij.plugin.processor.clazz.log.Slf4jProcessor;
import de.plushnikov.intellij.plugin.processor.clazz.log.XSlf4jProcessor;
import org.jetbrains.annotations.NotNull;

public class DelombokLoggerAction extends de.plushnikov.intellij.plugin.action.delombok.AbstractDelombokAction {
  @NotNull
  protected de.plushnikov.intellij.plugin.action.delombok.DelombokHandler createHandler() {
    return new de.plushnikov.intellij.plugin.action.delombok.DelombokHandler(
      ServiceManager.getService(CommonsLogProcessor.class), ServiceManager.getService(JBossLogProcessor.class),
      ServiceManager.getService(Log4jProcessor.class), ServiceManager.getService(Log4j2Processor.class), ServiceManager.getService(LogProcessor.class),
      ServiceManager.getService(Slf4jProcessor.class), ServiceManager.getService(XSlf4jProcessor.class), ServiceManager.getService(FloggerProcessor.class),
      ServiceManager.getService(CustomLogProcessor.class),ServiceManager.getService(de.plushnikov.intellij.plugin.processor.clazz.log.SpongeLogProcessor.class),
      ServiceManager.getService(de.plushnikov.intellij.plugin.processor.clazz.log.HoppipSlf4jProcessor.class));
  }
}
