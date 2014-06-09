/*
 *  Copyright © 2013 Citrix Systems, Inc.
 *  You may not use, copy, or modify this file except pursuant to a valid license agreement from
 *  Citrix Systems, Inc.
 */ 
package com.citrix.cpbm.portal.fragment.controllers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import com.citrix.cpbm.core.workflow.service.BusinessTransactionService;
import com.vmops.internal.service.CustomFieldService;
import com.vmops.internal.service.SubscriptionService;
import com.vmops.model.Subscription;

@Controller
@RequestMapping({
	"/billing", "/usage"
})
@SessionAttributes({
	"depositRecordForm"
})
public class BillingController extends AbstractBillingController {

	@Autowired
	protected SubscriptionService subscriptionService;

	@Autowired
	private CustomFieldService customFieldService;

	@Autowired
	private BusinessTransactionService businessTransactionService;

	private static final Logger logger = LoggerFactory.getLogger(com.citrix.cpbm.portal.fragment.controllers.BillingController.class);

	@RequestMapping(value = "/subscriptions/showDetails", method = RequestMethod.POST)
	public String showSubscriptionDetails(@RequestParam(value = "id", required = true) String uuid,
			@RequestParam(value = "tenant", required = false) String tenantParam, ModelMap map) {
		super.showSubscriptionDetails(uuid, tenantParam, map);

		logger.info("Entry overrided ShowSubscriptionDetails with Id:" + uuid);

		Subscription subscription = subscriptionService.locateSubscriptionByParam(uuid, true);
		logger.info("Showing details for subscription [{}]", subscription.toString());
		
		Map<String, String> configurationProperties = (Map<String, String>) map.get("configurationProperties");
		logger.info("configurationProperties before update: {}",configurationProperties.toString());
		
		Map<String, String> customFieldMap = subscription.getCustomFieldMap();
		//logger.info("customFieldMap: {}",customFieldMap.toString());
		
		configurationProperties.putAll(customFieldMap);
		
		//logger.info("configurationProperties after update: {}",configurationProperties.toString());
		
		map.addAttribute("configurationProperties", configurationProperties);

		logger.info("Exit ShowSubscriptionDetails");
		return "billing.viewSubscription";
	}

}
