package com.custom.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.citrix.cpbm.custom.persistence.TestModelDAO;
import com.custom.service.TestModelService;
import com.vmops.custom.model.TestModel;

@Service("testModelService")
public class TestModelServiceImpl implements TestModelService {

  @Autowired
  TestModelDAO testModelDAO;

  @Override
  public List<TestModel> listTestModel() {
    return testModelDAO.findAll(null);
  }

  @Override
  @Transactional
  public void save(TestModel testModel) {
    testModelDAO.save(testModel);
  }

}
