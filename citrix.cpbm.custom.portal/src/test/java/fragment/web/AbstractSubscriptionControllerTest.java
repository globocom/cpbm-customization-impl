/* Copyright (C) 2011 Cloud.com, Inc. All rights reserved. */
package fragment.web;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ModelMap;

import web.WebTestsBase;
import citrix.cpbm.portal.fragment.controllers.SubscriptionController;

import com.vmops.internal.service.SubscriptionService;
import com.vmops.model.Product;
import com.vmops.model.Tenant;
import com.vmops.persistence.SubscriptionDAO;
import com.vmops.service.ProductBundleService;
import com.vmops.service.TenantService;
import com.vmops.web.interceptors.UserContextInterceptor;

public class AbstractSubscriptionControllerTest extends WebTestsBase {

  @Autowired
  SubscriptionController controller;

  @Autowired
  TenantService service;

  @Autowired
  SubscriptionDAO subscriptionDAO;

  @Autowired
  private ProductBundleService bundleservice;

  @Autowired
  private SubscriptionService subscriptionService;

  private ModelMap map;
  
  private MockHttpServletRequest request;

  @Before
  public void init() throws Exception {
    map = new ModelMap();
    request = new MockHttpServletRequest();
    // response = new MockHttpServletResponse();
  }

  @Test
  public void testutilityrates() {
    Tenant tenant = service.getTenantByParam("id", "1", false);
    request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
    request.setAttribute("isSurrogatedTenant", false);
    String utilityrates = controller.utilityrates(tenant, tenant.getParam(), null, null, null, null, map, request);
    Assert.assertNotNull(utilityrates);
    Assert.assertEquals(utilityrates, new String("catalog.utilityrates"));
    Assert.assertTrue(map.containsAttribute("tenant"));
    Assert.assertTrue(map.containsAttribute("currency"));
    Assert.assertTrue(map.containsAttribute("startDate"));
    Assert.assertTrue(map.containsAttribute("usageTypeProductMap"));
    Assert.assertEquals(map.get("tenant"), tenant);
    Assert.assertEquals(map.get("currency"), tenant.getCurrency());
    Assert.assertNotNull(map.get("startDate"));
    Assert.assertNotNull(map.get("usageTypeProductMap"));
  }

  @Test
  public void testutilityrates_lightbox() {
    Tenant tenant = service.getTenantByParam("id", "1", false);
    request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
    request.setAttribute("isSurrogatedTenant", false);
    String utilityrates = controller.utilityrates_lightbox(tenant, tenant.getParam(), null, null, null, null, map, request);
    Assert.assertNotNull(utilityrates);
    Assert.assertEquals(utilityrates, new String("catalog.utilityrates.lightbox"));
    Assert.assertTrue(map.containsAttribute("tenant"));
    Assert.assertTrue(map.containsAttribute("currency"));
    Assert.assertTrue(map.containsAttribute("startDate"));
    Assert.assertTrue(map.containsAttribute("usageTypeProductMap"));
    Assert.assertEquals(map.get("tenant"), tenant);
    Assert.assertEquals(map.get("currency"), tenant.getCurrency());
    Assert.assertNotNull(map.get("startDate"));
    Assert.assertNotNull(map.get("usageTypeProductMap"));
  }

  @Test
  public void testlistUtilityRates() {
    Tenant tenant = service.getTenantByParam("id", "1", false);
    Product product = controller.listUtilityRates(tenant.getParam(), "Volume");
    Assert.assertNotNull(product.getPrice());
    Assert.assertEquals(product.getPrice().toString(), "20.0000");
  }
}
