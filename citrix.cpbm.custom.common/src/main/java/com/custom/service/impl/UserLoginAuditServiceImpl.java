/* Copyright (C) 2013 Citrix, Inc. All rights reserved. */
package com.custom.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.citrix.cpbm.custom.persistence.UserLoginAuditDAO;
import com.custom.service.UserLoginAuditService;
import com.vmops.custom.model.UserLoginAudit;
import com.vmops.model.User;

/**
 * The Class UserLoginAuditServiceImpl.
 * 
 * @author Shiv Prasad Khillar
 */
@Service("userLoginAuditService")
public class UserLoginAuditServiceImpl implements UserLoginAuditService {

  /** The user login audit dao. */
  @Autowired
  private UserLoginAuditDAO userLoginAuditDAO;

  /**
   * Find user login audit records.
   * 
   * @param date the date
   * @return the list
   * @see com.custom.service.UserLoginAuditService#findUserLoginAuditRecords(java.util.Date)
   */
  @Override
  public List<UserLoginAudit> findUserLoginAuditRecords(Date date) {
    Map<String, Object> conditions = new HashMap<String, Object>();
    conditions.put("createdAt", date);
    return userLoginAuditDAO.findByCriteria(null, conditions);
  }

  /**
   * Find user login audit records.
   * 
   * @param user the user
   * @return the list
   * @see com.custom.service.UserLoginAuditService#findUserLoginAuditRecords(com.vmops.model.User)
   */
  @Override
  public List<UserLoginAudit> findUserLoginAuditRecords(User user) {
    return userLoginAuditDAO.findLoginAuditRecords(user);
  }

  @Override
  public void saveAudit(UserLoginAudit userLoginAudit) {
    userLoginAuditDAO.save(userLoginAudit);

  }

}
