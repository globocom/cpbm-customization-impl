/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
package fragment.web;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ModelMap;

import web.WebTestsBase;

import com.citrix.cpbm.portal.fragment.controllers.SubscriptionController;
import com.vmops.internal.service.SubscriptionService;
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
  public void testutilityrates_lightbox() {
    Tenant tenant = service.getTenantByParam("id", "1", false);
    request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
    request.setAttribute("isSurrogatedTenant", false);
    String utilityrates = controller.utilityrates_lightbox(null, null, null, null, map, request);
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

}
