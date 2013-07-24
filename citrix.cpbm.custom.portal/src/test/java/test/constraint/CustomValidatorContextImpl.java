package test.constraint;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.Validator;

import org.hibernate.validator.engine.ValidatorContextImpl;
import org.hibernate.validator.metadata.BeanMetaDataCache;
import org.hibernate.validator.metadata.ConstraintHelper;

public class CustomValidatorContextImpl extends ValidatorContextImpl {

  private final MessageInterpolator messageInterpolator;

  private final TraversableResolver traversableResolver;

  private final ConstraintValidatorFactory constraintValidatorFactory;

  private final MessageInterpolator factoryMessageInterpolator;

  private final TraversableResolver factoryTraversableResolver;

  private final ConstraintValidatorFactory factoryConstraintValidatorFactory;

  private final ConstraintHelper constraintHelper;

  private final BeanMetaDataCache beanMetaDataCache;

  public CustomValidatorContextImpl(ConstraintValidatorFactory constraintValidatorFactory,
      MessageInterpolator factoryMessageInterpolator, TraversableResolver factoryTraversableResolver,
      ConstraintHelper constraintHelper, BeanMetaDataCache beanMetaDataCache) {

    super(constraintValidatorFactory, factoryMessageInterpolator, factoryTraversableResolver, constraintHelper,
        beanMetaDataCache);

    this.factoryConstraintValidatorFactory = constraintValidatorFactory;
    this.factoryMessageInterpolator = factoryMessageInterpolator;
    this.factoryTraversableResolver = factoryTraversableResolver;
    this.constraintHelper = constraintHelper;
    this.beanMetaDataCache = beanMetaDataCache;
    this.messageInterpolator = factoryMessageInterpolator;
    this.traversableResolver = factoryTraversableResolver;
    this.constraintValidatorFactory = constraintValidatorFactory;
  }

  @Override
  public Validator getValidator() {
    return new CustomValidatorImpl(constraintValidatorFactory, messageInterpolator, traversableResolver,
        constraintHelper, beanMetaDataCache);
  }
}
