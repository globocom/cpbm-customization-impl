/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */ 
package com.citrix.cpbm.portal.fragment.controllers;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@RequestMapping({
    "/billing", "/usage"
})
@SessionAttributes({
  "depositRecordForm"
})
public class BillingController extends AbstractBillingController {

  Logger logger = Logger.getLogger(com.citrix.cpbm.portal.fragment.controllers.BillingController.class);

}
