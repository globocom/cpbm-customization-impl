/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
package com.citrix.cpbm.portal.fragment.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.SessionAttributes;

/**
 * Controller that handles all user pages.
 * 
 * @author vijay
 */
@Controller
@SessionAttributes({
    "registration", "channelParam", "isTrialSignup"
})
public class RegistrationController extends AbstractRegistrationController {

}