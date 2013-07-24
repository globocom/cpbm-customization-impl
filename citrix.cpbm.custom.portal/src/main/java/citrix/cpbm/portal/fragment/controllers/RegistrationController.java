/* Copyright (C) 2011 Cloud.com, Inc. All rights reserved. */
package citrix.cpbm.portal.fragment.controllers;

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