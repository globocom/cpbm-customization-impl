/*
 * Copyright © 2013 Citrix Systems, Inc. You may not use, copy, or modify this file except pursuant to a valid license
 * agreement from Citrix Systems, Inc.
 */
package fragment.web;

import java.util.List;
import java.util.Map;

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
    Assert.assertEquals("on", map.get(page.getLevel1().name()));
    Assert.assertEquals("on", map.get(page.getLevel2().name()));
    Assert.assertTrue(map.get("top_nav_health_status") != null);
    Assert.assertTrue(map.get("top_nav_cs_instances") != null);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSetPageAsUserWithNoCloudAccount() throws Exception {
    asRoot();
    Tenant tenant = createTenantWithOwner();

    asUser(tenant.getOwner());

    Page page = Page.DASHBOARD;
    controller.setPage(map, page);
    Assert.assertEquals(map.get("page"), page);
    Assert.assertEquals("on", map.get(page.getLevel1().name()));
    Assert.assertEquals("on", map.get(page.getLevel2().name()));
    Assert.assertTrue(map.get("top_nav_health_status").equals("NORMAL"));
    Assert.assertTrue(map.get("top_nav_cs_instances") != null);
    Assert.assertTrue(((List<Map<String, String>>) map.get("top_nav_cs_instances")).size() == 0);
  }

}
