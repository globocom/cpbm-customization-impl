/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
package com.citrix.cpbm.custom.service;

import java.util.Date;
import java.util.List;

import com.citrix.cpbm.custom.model.UserLoginAudit;
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
