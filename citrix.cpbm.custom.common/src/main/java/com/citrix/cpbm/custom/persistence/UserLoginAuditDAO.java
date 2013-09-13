/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
package com.citrix.cpbm.custom.persistence;

import java.util.List;

import com.citrix.cpbm.custom.model.UserLoginAudit;
import com.vmops.model.User;
import com.vmops.persistence.GenericDAO;

/**
 * The Interface UserLoginAuditDAO.
 * 
 * @author Shiv Prasad Khillar
 */
public interface UserLoginAuditDAO extends GenericDAO<UserLoginAudit> {

  /**
   * Find login audit records.
   * 
   * @param user the user
   * @return the list
   */
  List<UserLoginAudit> findLoginAuditRecords(User user);

}
