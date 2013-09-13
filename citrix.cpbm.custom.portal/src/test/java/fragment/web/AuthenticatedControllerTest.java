/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
package fragment.web;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;

import web.WebTestsBaseWithMockConnectors;

import com.citrix.cpbm.portal.fragment.controllers.WorkflowController;
import com.vmops.model.Tenant;
import com.vmops.web.controllers.menu.Page;

/**
 * @author Manish
 */
public class AuthenticatedControllerTest extends WebTestsBaseWithMockConnectors {

  private ModelMap map;

  @Autowired
  private WorkflowController controller;
  
  @Before
  public void init() throws Exception {
    map = new ModelMap();
  }

  @Test
  public void testSetPageAsRoot() throws Exception {
    asRoot();
    Page page = Page.DASHBOARD;
    controller.setPage(map, page);
    Assert.assertEquals(map.get("page"), page);
    Assert.assertEquals("on",map.get(page.getLevel1().name()));
    Assert.assertEquals("on",map.get(page.getLevel2().name()));
    Assert.assertTrue(map.get("top_nav_health_status") != null);
    Assert.assertTrue(map.get("top_nav_cs_instances") != null);
  }
  
  @Test
  public void testSetPageAsUserWithNoCloudAccount() throws Exception {
    asRoot();
    Tenant tenant = createTenantWithOwner();
    
    asUser(tenant.getOwner());
    
    Page page = Page.DASHBOARD;
    controller.setPage(map, page);
    Assert.assertEquals(map.get("page"), page);
    Assert.assertEquals("on",map.get(page.getLevel1().name()));
    Assert.assertEquals("on",map.get(page.getLevel2().name()));
    Assert.assertTrue(map.get("top_nav_health_status") == null);
    Assert.assertTrue(map.get("top_nav_cs_instances") == null);
  }

}
