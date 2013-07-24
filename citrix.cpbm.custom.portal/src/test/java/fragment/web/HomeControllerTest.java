/* Copyright (C) 2011 Cloud.com, Inc. All rights reserved. */
package fragment.web;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.support.SessionStatus;

import web.WebTestsBase;
import web.support.DispatcherTestServlet;
import web.support.MockSessionStatus;

import citrix.cpbm.portal.fragment.controllers.HomeController;

import com.vmops.model.JobStatus;
import com.vmops.model.Tenant;
import com.vmops.model.User;
import com.vmops.service.JobManagementService;

public class HomeControllerTest extends WebTestsBase {

  ModelMap map;

  SessionStatus status;

  @Autowired
  HomeController controller;

  @Autowired
  JobManagementService jobService;

  @Before
  public void init() {
    map = new ModelMap();
    status = new MockSessionStatus();
  }

  @Test
  public void testHomeRouting() throws Exception {
    logger.debug("Testing routing....");
    DispatcherTestServlet servlet = this.getServletInstance();
    Method expected = locateMethod(controller.getClass(), "home", new Class[] {
        Tenant.class, String.class, Boolean.TYPE, ModelMap.class, HttpSession.class, HttpServletRequest.class
    });
    Method handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/home"));
    Assert.assertEquals(expected, handler);
    expected = locateMethod(controller.getClass(), "forum", new Class[] {
        HttpServletResponse.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/forum"));
    Assert.assertEquals(expected, handler);
  }

  @Test
  public void testHome() throws Exception {

    Tenant tenant = tenantService.getTenantByParam("id", "2", false);
    User user = new User("test", "user", "test@test.com", VALID_USER + random.nextInt(), VALID_PASSWORD, VALID_PHONE,
        VALID_TIMEZONE, null, userProfile, getRootUser());
    userService.createUserInTenant(user, tenant);
    asUser(user);

    Assert.assertEquals("main.home_with_second_level", controller.home(controller.getTenant(), controller.getTenant()
        .getUuid(), false, map, new MockHttpSession(), new MockHttpServletRequest()));
    Assert.assertTrue(map.containsAttribute("tenant"));
    Assert.assertTrue(map.containsAttribute("users"));
    Assert.assertTrue(map.containsAttribute("hasBilling"));
    Assert.assertTrue(map.containsAttribute("tickets"));
    Assert.assertTrue(map.containsAttribute("chartData"));
    Assert.assertTrue(map.containsAttribute("currencyValues"));

    map.clear();
    asUser(userDAO.getUserByParam("username", "root", true));
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("lang", "en_US");
    Assert.assertEquals("main.home_service_with_second_level", controller.home(controller.getTenant(), controller
        .getTenant().getUuid(), false, map, new MockHttpSession(), request));
    Assert.assertTrue(map.containsAttribute("tenant"));
    Assert.assertTrue(map.containsAttribute("users"));
    Assert.assertTrue(map.containsAttribute("hasBilling"));
    Assert.assertTrue(map.containsAttribute("tickets"));
    Assert.assertTrue(map.containsAttribute("currencyValues"));
    Assert.assertTrue(map.containsAttribute("reportFusionNR"));
    Assert.assertTrue(map.containsAttribute("reportFusionCR"));

  }

  @Test
  public void testForum() throws Exception {

    MockHttpServletResponse response = new MockHttpServletResponse();
    controller.forum(response, map);
    Object found = map.get("forumContext");
    Assert.assertNotNull(found);
    Assert.assertNotNull(response.getCookie("JforumSSO"));

  }

  @Test
  public void testshowBatchStatus() {
    JobStatus jobStatus = new JobStatus();

    Calendar calendar = Calendar.getInstance();
    calendar.set(2011, 11, 24, 17, 0, 0);
    Date startdate = calendar.getTime();
    jobStatus = new JobStatus("NEW_JOB" + Integer.toString(random.nextInt()), startdate, "RUNNING");

    jobService.createJobStatus(jobStatus);
    String batchList = controller.showBatchStatus(map);
    Assert.assertEquals(batchList, new String("batch.list"));
    Assert.assertTrue(map.containsAttribute("batchList"));
    List<JobStatus> list = ((List<JobStatus>) map.get("batchList"));
    Assert.assertEquals(list.get(0), jobStatus);
    Assert.assertTrue(list.size() == 1);
  }

}
