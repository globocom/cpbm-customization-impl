/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */ 
package com.citrix.cpbm.portal.fragment.controllers;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/logo")
public class LogoController extends AbstractLogoController {

  Logger logger = Logger.getLogger(com.citrix.cpbm.portal.fragment.controllers.LogoController.class);
}
