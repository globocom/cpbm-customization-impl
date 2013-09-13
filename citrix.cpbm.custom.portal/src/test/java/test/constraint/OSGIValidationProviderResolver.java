/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */ 
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
