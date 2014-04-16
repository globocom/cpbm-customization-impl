/*
*  Copyright © 2013 Citrix Systems, Inc.
*  You may not use, copy, or modify this file except pursuant to a valid license agreement from
*  Citrix Systems, Inc.
*/
package fragment.web;

import java.util.Currency;

import junit.framework.Assert;

import org.apache.tiles.definition.NoSuchDefinitionException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.ui.ModelMap;

import web.WebTestsBase;

import com.citrix.cpbm.portal.fragment.controllers.SubscriptionController;
import com.vmops.internal.service.SubscriptionService;
import com.vmops.model.Tenant;
import com.vmops.persistence.SubscriptionDAO;
import com.vmops.portal.config.Configuration;
import com.vmops.portal.config.Configuration.Names;
import com.vmops.service.ConfigurationService;
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

  @Autowired
  protected Configuration config;
  
  @Autowired
  private ConfigurationService configurationService;
  
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
  
  @Test
  @ExpectedException(NoSuchDefinitionException.class)
  public void testAnonymousCatalogFail(){
    controller.anonymousCatalog(map, null, "JPY", null, null, request);
  }
  
  @Test
  @DirtiesContext
  public void testAnonymousCatalog(){
    com.vmops.model.Configuration conf = configurationService.locateConfigurationByName(Names.com_citrix_cpbm_public_catalog_display);
    conf.setValue("true");
    configurationService.update(conf);
    request.setAttribute("isSurrogatedTenant", true);
    String view = controller.anonymousCatalog(map, null, "JPY", null, null, request);
    Assert.assertEquals("anonymous.catalog", view);
    Assert.assertTrue(map.containsKey(UserContextInterceptor.MIN_FRACTION_DIGITS));
    Assert.assertEquals(Currency.getInstance("JPY").getDefaultFractionDigits(), map.get(UserContextInterceptor.MIN_FRACTION_DIGITS));
  }

}
