package com.custom.service;

import java.util.List;

import com.vmops.custom.model.TestModel;

public interface TestModelService {

  public List<TestModel> listTestModel();

  public void save(TestModel testModel);
}
