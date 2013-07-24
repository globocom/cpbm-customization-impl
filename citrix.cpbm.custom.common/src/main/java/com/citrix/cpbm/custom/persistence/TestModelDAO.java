package com.citrix.cpbm.custom.persistence;


import com.vmops.custom.model.TestModel;
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
