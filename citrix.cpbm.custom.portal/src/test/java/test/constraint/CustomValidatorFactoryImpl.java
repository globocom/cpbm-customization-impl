package test.constraint;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ConfigurationState;

import org.hibernate.validator.metadata.AnnotationIgnores;
import org.hibernate.validator.metadata.BeanMetaDataCache;
import org.hibernate.validator.metadata.BeanMetaDataImpl;
import org.hibernate.validator.metadata.ConstraintDescriptorImpl;
import org.hibernate.validator.metadata.ConstraintHelper;
import org.hibernate.validator.metadata.ConstraintOrigin;
import org.hibernate.validator.metadata.MetaConstraint;
import org.hibernate.validator.util.ReflectionHelper;
import org.hibernate.validator.xml.XmlMappingParser;

public class CustomValidatorFactoryImpl implements ValidatorFactory {

  private final MessageInterpolator messageInterpolator;

  private final TraversableResolver traversableResolver;

  private final ConstraintValidatorFactory constraintValidatorFactory;

  private final ConstraintHelper constraintHelper;

  private final BeanMetaDataCache beanMetaDataCache;

  public CustomValidatorFactoryImpl(ConfigurationState configurationState) {
    this.messageInterpolator = configurationState.getMessageInterpolator();
    this.constraintValidatorFactory = configurationState.getConstraintValidatorFactory();
    this.traversableResolver = configurationState.getTraversableResolver();
    this.constraintHelper = new ConstraintHelper();
    this.beanMetaDataCache = new BeanMetaDataCache();

    initBeanMetaData(configurationState.getMappingStreams());
  }

  @Override
  public Validator getValidator() {
    return usingContext().getValidator();
  }

  @Override
  public MessageInterpolator getMessageInterpolator() {
    return messageInterpolator;
  }

  @Override
  public TraversableResolver getTraversableResolver() {
    return traversableResolver;
  }

  @Override
  public ConstraintValidatorFactory getConstraintValidatorFactory() {
    return constraintValidatorFactory;
  }

  @Override
  public <T> T unwrap(Class<T> type) {
    throw new ValidationException("Type " + type + " not supported");
  }

  @Override
  public ValidatorContext usingContext() {
    return new CustomValidatorContextImpl(constraintValidatorFactory, messageInterpolator, traversableResolver,
        constraintHelper, beanMetaDataCache);
  }

  private <T> void initBeanMetaData(Set<InputStream> mappingStreams) {

    XmlMappingParser mappingParser = new XmlMappingParser(constraintHelper);
    mappingParser.parse(mappingStreams);

    Set<Class<?>> processedClasses = mappingParser.getProcessedClasses();
    AnnotationIgnores annotationIgnores = mappingParser.getAnnotationIgnores();
    for (Class<?> clazz : processedClasses) {
      @SuppressWarnings("unchecked")
      Class<T> beanClass = (Class<T>) clazz;
      BeanMetaDataImpl<T> metaData = new BeanMetaDataImpl<T>(beanClass, constraintHelper, annotationIgnores);

      List<Class<?>> classes = new ArrayList<Class<?>>();
      ReflectionHelper.computeClassHierarchy(beanClass, classes);
      for (Class<?> classInHierarchy : classes) {
        if (processedClasses.contains(classInHierarchy)) {
          addXmlConfiguredConstraintToMetaData(mappingParser, beanClass, classInHierarchy, metaData);
        }
      }

      if (!mappingParser.getDefaultSequenceForClass(beanClass).isEmpty()) {
        metaData.setDefaultGroupSequence(mappingParser.getDefaultSequenceForClass(beanClass));
      }

      beanMetaDataCache.addBeanMetaData(beanClass, metaData);
    }
  }

  @SuppressWarnings("unchecked")
  private <T, A extends Annotation> void addXmlConfiguredConstraintToMetaData(XmlMappingParser mappingParser,
      Class<T> rootClass, Class<?> hierarchyClass, BeanMetaDataImpl<T> metaData) {
    for (MetaConstraint<?, ? extends Annotation> constraint : mappingParser.getConstraintsForClass(hierarchyClass)) {
      ConstraintOrigin definedIn = definedIn(rootClass, hierarchyClass);
      ConstraintDescriptorImpl<A> descriptor = new ConstraintDescriptorImpl<A>((A) constraint.getDescriptor()
          .getAnnotation(), constraintHelper, constraint.getElementType(), definedIn);
      MetaConstraint<T, A> newMetaConstraint = new MetaConstraint<T, A>(rootClass, constraint.getMember(), descriptor);
      metaData.addMetaConstraint(hierarchyClass, newMetaConstraint);
    }

    for (Member m : mappingParser.getCascadedMembersForClass(hierarchyClass)) {
      metaData.addCascadedMember(m);
    }
  }

  private ConstraintOrigin definedIn(Class<?> rootClass, Class<?> hierarchyClass) {
    if (hierarchyClass.equals(rootClass)) {
      return ConstraintOrigin.DEFINED_LOCALLY;
    } else {
      return ConstraintOrigin.DEFINED_IN_HIERARCHY;
    }
  }
}
