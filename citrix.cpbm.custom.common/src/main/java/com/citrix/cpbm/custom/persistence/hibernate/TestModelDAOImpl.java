package com.citrix.cpbm.custom.persistence.hibernate;

import java.util.Map;

import org.springframework.stereotype.Repository;

import com.citrix.cpbm.custom.persistence.TestModelDAO;
import com.vmops.custom.model.TestModel;
import com.vmops.persistence.hibernate.GenericHibernateDAO;

@Repository("testModelDAO")
public class TestModelDAOImpl extends GenericHibernateDAO<TestModel> implements TestModelDAO {

  @Override
  public TestModel findById(Long id) {
    Map<String, Object> conditions = getConditionsTemplate();
    conditions.put("id", id);
    return findDistinctByCriteria(conditions);
  }
}
