/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */ 
package com.citrix.cpbm.custom.persistence;


import com.citrix.cpbm.custom.model.TestModel;
import com.vmops.persistence.GenericDAO;

public interface TestModelDAO extends GenericDAO<TestModel> {

  /**
   * Returns an address given its Id.
   * 
   * @param domainId the domain id.
   * @return the project tied to the supplied domain ID.
   */
  public TestModel findById(Long id);

}
