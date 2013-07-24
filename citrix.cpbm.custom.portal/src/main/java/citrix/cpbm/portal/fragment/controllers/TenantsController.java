/* Copyright (C) 2011 Cloud.com, Inc. All rights reserved. */
package citrix.cpbm.portal.fragment.controllers;

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