/* Copyright (C) 2011 Cloud.com, Inc. All rights reserved. */
package citrix.cpbm.portal.fragment.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

/**
 * Controller for System Health related requests.
 * 
 * @author vaibhav
 */
@Controller
@SessionAttributes("serviceNotificationForm")
@RequestMapping("/health")
public class SystemHealthController extends AbstractSystemHealthController {

}
