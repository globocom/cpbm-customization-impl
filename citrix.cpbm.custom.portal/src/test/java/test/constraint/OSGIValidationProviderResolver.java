/*
*  Copyright © 2013 Citrix Systems, Inc.
*  You may not use, copy, or modify this file except pursuant to a valid license agreement from
*  Citrix Systems, Inc.
*/ 
package test.constraint;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ValidationProviderResolver;
import javax.validation.spi.ValidationProvider;

public class OSGIValidationProviderResolver implements ValidationProviderResolver {

  @Override
  public List<ValidationProvider<?>> getValidationProviders() {

    List<ValidationProvider<?>> providers = new ArrayList<ValidationProvider<?>>();
    providers.add(new CustomHibernateValidator());
    return providers;
  }
}
