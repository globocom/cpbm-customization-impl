/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */ 
package test.constraint;

import javax.validation.ValidatorFactory;
import javax.validation.spi.ConfigurationState;

import org.hibernate.validator.HibernateValidator;

public class CustomHibernateValidator extends HibernateValidator {

  @Override
  public ValidatorFactory buildValidatorFactory(ConfigurationState configurationState) {
    return new CustomValidatorFactoryImpl(configurationState);
  }

}
