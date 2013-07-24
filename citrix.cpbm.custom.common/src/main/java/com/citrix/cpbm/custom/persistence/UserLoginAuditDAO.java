/* Copyright (C) 2013 Citrix, Inc. All rights reserved. */
package com.citrix.cpbm.custom.persistence;

import java.util.List;

import com.vmops.custom.model.UserLoginAudit;
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
