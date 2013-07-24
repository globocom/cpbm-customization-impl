/* Copyright (C) 2013 Citrix, Inc. All rights reserved. */
package com.custom.service;

import java.util.Date;
import java.util.List;

import com.vmops.custom.model.UserLoginAudit;
import com.vmops.model.User;

/**
 * The Interface UserLoginAuditService.
 * 
 * @author Shiv Prasad Khillar
 */
public interface UserLoginAuditService {

  /**
   * Find user login audit records.
   * 
   * @param date the date
   * @return the list
   */
  public List<UserLoginAudit> findUserLoginAuditRecords(Date date);

  /**
   * Find user login audit records.
   * 
   * @param user the user
   * @return the list
   */
  public List<UserLoginAudit> findUserLoginAuditRecords(User user);

  public void saveAudit(UserLoginAudit userLoginAudit);

}
