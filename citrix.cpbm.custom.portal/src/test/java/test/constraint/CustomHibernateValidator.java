/*
*  Copyright © 2013 Citrix Systems, Inc.
*  You may not use, copy, or modify this file except pursuant to a valid license agreement from
*  Citrix Systems, Inc.
*/ 
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
