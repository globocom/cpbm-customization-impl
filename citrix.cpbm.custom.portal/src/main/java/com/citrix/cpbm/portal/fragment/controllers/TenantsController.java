/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
package com.citrix.cpbm.portal.fragment.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

/**
 * Controller that handles all tenant pages.
 * 
 * @author vijay
 */
@Controller
@RequestMapping("/tenants")
@SessionAttributes({
    "tenantForm", "depositForm", "sapForm", "tenantLogoForm", "accountResourceLimitForm", "account",
    "userAlertEmailForm"
})
public class TenantsController extends AbstractTenantController {

}