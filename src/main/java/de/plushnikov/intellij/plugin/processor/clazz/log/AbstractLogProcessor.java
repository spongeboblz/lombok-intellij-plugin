package de.plushnikov.intellij.plugin.processor.clazz.log;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import de.plushnikov.intellij.plugin.lombokconfig.ConfigDiscovery;
import de.plushnikov.intellij.plugin.lombokconfig.ConfigKey;
import de.plushnikov.intellij.plugin.problem.ProblemBuilder;
import de.plushnikov.intellij.plugin.processor.clazz.AbstractClassProcessor;
import de.plushnikov.intellij.plugin.psi.LombokLightFieldBuilder;
import de.plushnikov.intellij.plugin.settings.ProjectSettings;
import de.plushnikov.intellij.plugin.util.PsiAnnotationUtil;
import de.plushnikov.intellij.plugin.util.PsiClassUtil;
import de.plushnikov.intellij.plugin.util.PsiAnnotationSearchUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

/**
 * Base lombok processor class for logger processing
 *
 * @author Plushnikov Michail
 */
public abstract class AbstractLogProcessor extends AbstractClassProcessor {
  enum LoggerInitializerParameter {
    TYPE,
    NAME,
    TOPIC,
    NULL,
    UNKNOWN;

    @NotNull
    static LoggerInitializerParameter find(@NotNull String parameter) {
      switch (parameter) {
        case "TYPE":
          return TYPE;
        case "NAME":
          return NAME;
        case "TOPIC":
          return TOPIC;
        case "NULL":
          return NULL;
        default:
          return UNKNOWN;
      }
    }
  }

  AbstractLogProcessor(@NotNull Class<? extends Annotation> supportedAnnotationClass) {
    super(PsiField.class, supportedAnnotationClass);
  }

  @Override
  public boolean isEnabled(@NotNull PropertiesComponent propertiesComponent) {
    return ProjectSettings.isEnabled(propertiesComponent, ProjectSettings.IS_LOG_ENABLED);
  }

  @NotNull
  public static String getLoggerName(@NotNull PsiClass psiClass) {
    return ConfigDiscovery.getInstance().getStringLombokConfigProperty(ConfigKey.LOG_FIELDNAME, psiClass);
  }

  public static boolean isLoggerStatic(@NotNull PsiClass psiClass) {
    return ConfigDiscovery.getInstance().getBooleanLombokConfigProperty(ConfigKey.LOG_FIELD_IS_STATIC, psiClass);
  }

  /**
   * Nullable because it can be called before validation.
   */
  @Nullable
  public abstract String getLoggerType(@NotNull PsiClass psiClass);

  /**
   * Call only after validation.
   */
  @NotNull
  abstract String getLoggerInitializer(@NotNull PsiClass psiClass);

  /**
   * Call only after validation.
   */
  @NotNull
  abstract List<LoggerInitializerParameter> getLoggerInitializerParameters(@NotNull PsiClass psiClass, boolean topicPresent);

  @Override
  protected boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass, @NotNull ProblemBuilder builder) {
    boolean result = true;
    if (psiClass.isInterface() || psiClass.isAnnotationType()) {
      builder.addError("@%s is legal only on classes and enums", getSupportedAnnotationClasses()[0].getSimpleName());
      result = false;
    }
    if (result) {
      final String annotationName = PsiAnnotationSearchUtil.getSimpleNameOf(psiAnnotation);
      if("HoppipSlf4j".equals(annotationName)){
        return validateHoppipSlf4j(psiAnnotation,psiClass,builder);
      }

      String loggerNameTemp = getLoggerName(psiClass);
      if("SpongeLog".equals(annotationName)){
        loggerNameTemp="spongeLog";
      }
      String loggerName=loggerNameTemp;
      if (hasFieldByName(psiClass, loggerName)) {
        builder.addError("Not generating field %s: A field with same name already exists", loggerName);
        result = false;
      }
    }
    return result;
  }
  private boolean validateHoppipSlf4j(PsiAnnotation psiAnnotation,PsiClass psiClass,ProblemBuilder builder){
    final Collection<String> names = PsiAnnotationUtil.getAnnotationValues(psiAnnotation,"names",String.class);
    final Collection<String> params = PsiAnnotationUtil.getAnnotationValues(psiAnnotation,"params",String.class);
    if(names.size()!=params.size()){
      builder.addError("@HoppipSlf4j names length not equal to params length");
      return false;
    }
    String[] nameArr=names.toArray(new String[]{});
    String[] paramArr=params.toArray(new String[]{});
    for(int i=0;i<names.size();i++){
      if(nameArr[0]==null||nameArr[i].trim().length()==0){
        builder.addError("@HoppipSlf4j names is not null or empty");
        return false;
      }
      if(paramArr[i]==null){
        builder.addError("@HoppipSlf4j params is not null or empty");
        return false;
      }
      nameArr[i]=nameArr[i].trim();
    }
    for(int i=0;i<nameArr.length-1;i++){
      for(int j=i+1;j<nameArr.length;j++){
        if(nameArr[i].equals(nameArr[j])){
          builder.addError("@HoppipSlf4j : duplicate name ["+nameArr[i]+"] not allowed");
          return false;
        }
      }
    }
    for(String name:names){
      if (hasFieldByName(psiClass, name)) {
        builder.addError("Not generating field %s: A field with same name already exists", name);
        return false;
      }
    }
    return true;
  }

  protected void generatePsiElements(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation, @NotNull List<? super PsiElement> target) {
    final String annotationName = PsiAnnotationSearchUtil.getSimpleNameOf(psiAnnotation);
    if("HoppipSlf4j".equals(annotationName)){
      final Collection<String> names = PsiAnnotationUtil.getAnnotationValues(psiAnnotation,"names",String.class);
      final Collection<String> params = PsiAnnotationUtil.getAnnotationValues(psiAnnotation,"params",String.class);
      String[] nameArr=names.toArray(new String[]{});
      String[] paramArr=params.toArray(new String[]{});
      for(int i=0;i<nameArr.length;i++){
        target.add(createHoppipSlf4jField(psiClass, psiAnnotation,nameArr[i].trim(),paramArr[i]));
      }
    }else{
      target.add(createLoggerField(psiClass, psiAnnotation));
    }
  }

  /**
   * 创建特殊的日志对象
   * @param psiClass
   * @param psiAnnotation
   * @param loggerName
   * @param param
   * @return
   */
  private LombokLightFieldBuilder createHoppipSlf4jField(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation,String loggerName,String param) {
    // called only after validation succeeded
    final Project project = psiClass.getProject();
    final PsiManager manager = psiClass.getContainingFile().getManager();

    final PsiElementFactory psiElementFactory = JavaPsiFacade.getElementFactory(project);
    String loggerType = getLoggerType(psiClass);
    if (loggerType == null) {
      // validated
      throw new IllegalStateException("Invalid custom log declaration.");
    }
    final PsiType psiLoggerType = psiElementFactory.createTypeFromText(loggerType, psiClass);

    LombokLightFieldBuilder loggerField = new LombokLightFieldBuilder(manager, loggerName, psiLoggerType)
      .withContainingClass(psiClass)
      .withModifier(PsiModifier.FINAL)
      .withModifier(PsiModifier.PRIVATE)
      .withNavigationElement(psiAnnotation);
    if (isLoggerStatic(psiClass)) {
      loggerField.withModifier(PsiModifier.STATIC);
    }

    final String initializerText = String.format(getLoggerInitializer(psiClass), "\""+param+"\"");
    final PsiExpression initializer = psiElementFactory.createExpressionFromText(initializerText, psiClass);
    loggerField.setInitializer(initializer);
    return loggerField;
  }

  private LombokLightFieldBuilder createLoggerField(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation) {
    // called only after validation succeeded

    final Project project = psiClass.getProject();
    final PsiManager manager = psiClass.getContainingFile().getManager();

    final PsiElementFactory psiElementFactory = JavaPsiFacade.getElementFactory(project);
    String loggerType = getLoggerType(psiClass);
    if (loggerType == null) {
      throw new IllegalStateException("Invalid custom log declaration."); // validated
    }
    final PsiType psiLoggerType = psiElementFactory.createTypeFromText(loggerType, psiClass);

    String loggerName=getLoggerName(psiClass);
    final String annotationName = PsiAnnotationSearchUtil.getSimpleNameOf(psiAnnotation);
    if("SpongeLog".equals(annotationName)){
      loggerName="spongeLog";
    }

    LombokLightFieldBuilder loggerField = new LombokLightFieldBuilder(manager, loggerName, psiLoggerType)
      .withContainingClass(psiClass)
      .withModifier(PsiModifier.FINAL)
      .withModifier(PsiModifier.PRIVATE)
      .withNavigationElement(psiAnnotation);
    if (isLoggerStatic(psiClass)) {
      loggerField.withModifier(PsiModifier.STATIC);
    }

    final String loggerInitializerParameters = createLoggerInitializeParameters(psiClass, psiAnnotation);
    final String initializerText = String.format(getLoggerInitializer(psiClass), loggerInitializerParameters);
    final PsiExpression initializer = psiElementFactory.createExpressionFromText(initializerText, psiClass);
    loggerField.setInitializer(initializer);
    return loggerField;
  }

  @NotNull
  private String createLoggerInitializeParameters(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation) {
    final StringBuilder parametersBuilder = new StringBuilder();
    final String topic = PsiAnnotationUtil.getStringAnnotationValue(psiAnnotation, "topic");
    final boolean topicPresent = !StringUtil.isEmptyOrSpaces(topic);
    final List<LoggerInitializerParameter> loggerInitializerParameters = getLoggerInitializerParameters(psiClass, topicPresent);
    for (LoggerInitializerParameter loggerInitializerParameter : loggerInitializerParameters) {
      if (parametersBuilder.length() > 0) {
        parametersBuilder.append(", ");
      }
      switch (loggerInitializerParameter) {
        case TYPE:
          parametersBuilder.append(psiClass.getName()).append(".class");
          break;
        case NAME:
          parametersBuilder.append(psiClass.getName()).append(".class.getName()");
          break;
        case TOPIC:
          if (!topicPresent) {
            // sanity check; either implementation of CustomLogParser or predefined loggers is wrong
            throw new IllegalStateException("Topic can never be a parameter when topic was not set.");
          }
          parametersBuilder.append('"').append(StringUtil.escapeStringCharacters(topic)).append('"');
          break;
        case NULL:
          parametersBuilder.append("null");
          break;
        default:
          // sanity check; either implementation of CustomLogParser or predefined loggers is wrong
          throw new IllegalStateException("Unexpected logger initializer parameter " + loggerInitializerParameter);
      }
    }
    return parametersBuilder.toString();
  }

  private boolean hasFieldByName(@NotNull PsiClass psiClass, @NotNull String fieldName) {
    final Collection<PsiField> psiFields = PsiClassUtil.collectClassFieldsIntern(psiClass);
    for (PsiField psiField : psiFields) {
      if (fieldName.equals(psiField.getName())) {
        return true;
      }
    }
    return false;
  }

}
