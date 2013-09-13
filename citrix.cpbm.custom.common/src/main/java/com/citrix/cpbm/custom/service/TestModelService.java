/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */ 
package com.citrix.cpbm.custom.service;

import java.util.List;

import com.citrix.cpbm.custom.model.TestModel;

public interface TestModelService {

  public List<TestModel> listTestModel();

  public void save(TestModel testModel);
}
