/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */ 
package com.citrix.cpbm.custom.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.citrix.cpbm.custom.model.TestModel;
import com.citrix.cpbm.custom.persistence.TestModelDAO;
import com.citrix.cpbm.custom.service.TestModelService;

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
