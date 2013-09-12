/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
package fragment.web;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
import web.WebTestsBaseWithMockConnectors;
import web.support.DispatcherTestServlet;
import web.support.MockSessionStatus;

import com.citrix.cpbm.platform.admin.service.exceptions.ConnectorManagementServiceException;
import com.citrix.cpbm.portal.fragment.controllers.HomeController;
import com.vmops.model.JobStatus;
import com.vmops.model.Product;
import com.vmops.model.ServiceInstance;
import com.vmops.model.Tenant;
import com.vmops.model.User;
import com.vmops.persistence.ServiceInstanceDao;
import com.vmops.persistence.TenantDAO;
import com.vmops.service.JobManagementService;
import com.vmops.service.ProductService;
import com.vmops.usage.model.UserDailyUsage;
import com.vmops.usage.persistence.UserDailyUsageDAO;

public class HomeControllerTest extends WebTestsBase {

  ModelMap map;

  SessionStatus status;

  WebTestsBaseWithMockConnectors webtestsbasewithmockconnectors;

  HttpServletRequest request;

  HttpSession session;

  HttpServletResponse response;

  @Autowired
  HomeController controller;

  @Autowired
  JobManagementService jobService;

  @Autowired
  TenantDAO tenantDAO;

  @Autowired
  ServiceInstanceDao serviceInstanceDao;

  @Autowired
  ProductService productService;

  @Autowired
  UserDailyUsageDAO userDailyUsageDAO;

  @Before
  public void init() {
    map = new ModelMap();
    status = new MockSessionStatus();
    request = new MockHttpServletRequest();
    session = new MockHttpSession();
    response = new MockHttpServletResponse();
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
    HttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("effectiveTenant", controller.getTenant());
    Assert.assertEquals("main.home_with_second_level", controller.home(controller.getTenant(), controller.getTenant()
        .getUuid(), false, map, new MockHttpSession(), request));
    Assert.assertTrue(map.containsAttribute("tenant"));
    Assert.assertTrue(map.containsAttribute("users"));
    Assert.assertTrue(map.containsAttribute("tickets"));
    Assert.assertTrue(map.containsAttribute("totalTickets"));
    Assert.assertTrue(map.containsAttribute("serviceCategoryList"));
    Assert.assertTrue(map.containsAttribute("chartData"));
    Assert.assertTrue(map.containsAttribute("currencyValues"));

    map.clear();
    asUser(userDAO.getUserByParam("username", "root", true));
    request = new MockHttpServletRequest();
    request.setAttribute("effectiveTenant", controller.getTenant());
    ((MockHttpServletRequest) request).setParameter("lang", "en_US");
    request.setAttribute("isSurrogatedTenant", Boolean.FALSE);
    Assert.assertEquals("main.home_service_with_second_level", controller.home(controller.getTenant(), controller
        .getTenant().getUuid(), false, map, new MockHttpSession(), request));
    Assert.assertTrue(map.containsAttribute("tenant"));
    Assert.assertTrue(map.containsAttribute("users"));
    Assert.assertTrue(map.containsAttribute("tickets"));
    Assert.assertTrue(map.containsAttribute("currencyValues"));
    Assert.assertTrue(map.containsAttribute("reportFusionNR"));
    Assert.assertTrue(map.containsAttribute("reportFusionCR"));

    map.clear();
    tenant = tenantService.getTenantByParam("id", "2", false);
    user = tenant.getOwner();
    user.setPassword(getSystemTenant().getUsers().get(0).getPassword());
    userDAO.save(user);
    asUser(user);
    request = new MockHttpServletRequest();
    request.setAttribute("effectiveTenant", controller.getTenant());
    ((MockHttpServletRequest) request).setParameter("lang", "en_US");
    request.setAttribute("isSurrogatedTenant", Boolean.FALSE);
    String resultString = controller.home(tenant, tenant.getUuid(), false, map, new MockHttpSession(), request);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("main.home_with_second_level", resultString);
    Assert.assertTrue(map.containsAttribute("tenant"));
    Assert.assertTrue(map.containsAttribute("users"));
    Assert.assertTrue(map.containsAttribute("tickets"));
    Assert.assertTrue(map.containsAttribute("currencyValues"));

  }

  @Test
  public void testHomeWithSurrogatedTenantIsTrue() throws Exception {

    Tenant systemTenant = tenantService.getSystemTenant();
    request = new MockHttpServletRequest();
    request.setAttribute("effectiveTenant", controller.getTenant());
    ((MockHttpServletRequest) request).setParameter("lang", "en_US");
    request.setAttribute("isSurrogatedTenant", Boolean.TRUE);
    String resultString = controller.home(systemTenant, systemTenant.getUuid(), false, map, new MockHttpSession(),
        request);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("main.home_service_with_second_level", resultString);
    Assert.assertTrue(map.containsAttribute("tenant"));
    Assert.assertTrue(map.containsAttribute("users"));
    Assert.assertTrue(map.containsAttribute("tickets"));
    Assert.assertTrue(map.containsAttribute("currencyValues"));

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

  /**
   * @author Abhaik
   * @description : Test to get Home Items for Service Provider User
   */
  @Test
  public void testGetHomeItemsForSPUser() {

    Tenant systemTenant = tenantService.getSystemTenant();
    request.setAttribute("effectiveTenant", systemTenant);
    request.setAttribute("isSurrogatedTenant", Boolean.FALSE);
    ServiceInstance instance = serviceInstanceDao.find(1L);

    String resultString = controller.getHomeItems(systemTenant, systemTenant.getParam(), instance.getUuid(), map,
        request);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("home.items.view", resultString);

    List<Map<String, Object>> dashboardItemsList = (List<Map<String, Object>>) map.get("dashboardItems");
    Assert.assertEquals(3, dashboardItemsList.size());
    for (int i = 0; i < dashboardItemsList.size(); i++) {
      Map<String, Object> dashboardItems = dashboardItemsList.get(i);
      if (dashboardItems.get("itemName").equals("label.active.customers")) {
        Assert.assertEquals(9, dashboardItems.get("itemValue"));
      }
      if (dashboardItems.get("itemName").equals("label.active.users")) {
        Assert.assertEquals(2, dashboardItems.get("itemValue"));
      }
      if (dashboardItems.get("itemName").equals("label.active.subscriptions")) {
        Assert.assertEquals(46, dashboardItems.get("itemValue"));
      }
    }
  }

  /**
   * @author Abhaik
   * @description : Test to get Home Items for Master user of Tenant through Service Provider User
   */
  @Test
  public void testGetHomeItemsForTenantFromRoot() {

    Tenant systemTenant = tenantService.getSystemTenant();
    Tenant tenant = tenantDAO.find(2L);
    request.setAttribute("effectiveTenant", tenant);
    request.setAttribute("isSurrogatedTenant", Boolean.TRUE);
    ServiceInstance instance = serviceInstanceDao.find(1L);
    List<Product> productlist = productService.listProductsByServiceInstance(instance, 1, 10000);

    String resultString = controller.getHomeItems(systemTenant, tenant.getParam(), instance.getUuid(), map, request);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("home.items.view", resultString);

    List<Map<String, Object>> dashboardItemsList = (List<Map<String, Object>>) map.get("dashboardItems");
    int count = 0;
    for (int i = 0; i < dashboardItemsList.size(); i++) {
      Map<String, Object> dashboardItems = dashboardItemsList.get(i);
      if (dashboardItems.containsKey("itemValueType"))
        if (dashboardItems.get("itemValueType").equals("product"))
          count = count + 1;
    }
    Assert.assertEquals(productlist.size(), count);
  }

  /**
   * @author Abhaik
   * @description : Test to get Home Items for Master User of Tenant
   */
  @Test
  public void testGetHomeItemsForMasterUserOfTenant() {

    User user = userDAO.find(3L);
    asUser(user);

    Tenant tenant = tenantDAO.find(2L);
    request.setAttribute("effectiveTenant", tenant);
    request.setAttribute("isSurrogatedTenant", Boolean.FALSE);
    ServiceInstance instance = serviceInstanceDao.find(1L);
    List<Product> productlist = productService.listProductsByServiceInstance(instance, 1, 10000);

    String resultString = controller.getHomeItems(tenant, tenant.getParam(), instance.getUuid(), map, request);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("home.items.view", resultString);

    List<Map<String, Object>> dashboardItemsList = (List<Map<String, Object>>) map.get("dashboardItems");
    int count = 0;
    for (int i = 0; i < dashboardItemsList.size(); i++) {
      Map<String, Object> dashboardItems = dashboardItemsList.get(i);
      if (dashboardItems.containsKey("itemValueType"))
        if (dashboardItems.get("itemValueType").equals("product"))
          count = count + 1;
    }
    Assert.assertEquals(productlist.size(), count);
  }

  /**
   * @author Abhaik
   * @description : Test to get Home Items for Normal User of Tenant
   */
  @Test
  public void testGetHomeItemsForNormalUserOfTenant() {

    User user = userDAO.find(22L);
    asUser(user);

    Tenant tenant = tenantDAO.find(20L);
    request.setAttribute("effectiveTenant", tenant);
    request.setAttribute("isSurrogatedTenant", Boolean.FALSE);
    ServiceInstance instance = serviceInstanceDao.find(4L);
    List<Product> productlist = productService.listProductsByServiceInstance(instance, 1, 10000);

    String resultString = controller.getHomeItems(tenant, tenant.getParam(), instance.getUuid(), map, request);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("home.items.view", resultString);

    List<Map<String, Object>> dashboardItemsList = (List<Map<String, Object>>) map.get("dashboardItems");
    int count = 0;
    for (int i = 0; i < dashboardItemsList.size(); i++) {
      Map<String, Object> dashboardItems = dashboardItemsList.get(i);
      if (dashboardItems.containsKey("itemValueType"))
        if (dashboardItems.get("itemValueType").equals("product"))
          count = count + 1;
    }
    Assert.assertEquals(productlist.size(), count);
  }

  /**
   * @author Abhaik
   * @description : Test to get Home Items for SP User with Null Service Instance
   */
  @Test
  public void testGetHomeItemsForRootWithInstantNull() {

    Tenant systemTenant = tenantService.getSystemTenant();
    request.setAttribute("effectiveTenant", systemTenant);
    request.setAttribute("isSurrogatedTenant", Boolean.FALSE);

    String resultString = controller.getHomeItems(systemTenant, systemTenant.getParam(), null, map, request);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("home.items.view", resultString);

    List<Map<String, Object>> dashboardItemsList = (List<Map<String, Object>>) map.get("dashboardItems");
    Assert.assertEquals(3, dashboardItemsList.size());
    for (int i = 0; i < dashboardItemsList.size(); i++) {
      Map<String, Object> dashboardItems = dashboardItemsList.get(i);
      if (dashboardItems.get("itemName").equals("label.active.customers")) {
        Assert.assertEquals(11, dashboardItems.get("itemValue"));
      }
      if (dashboardItems.get("itemName").equals("label.active.users")) {
        Assert.assertEquals(15, dashboardItems.get("itemValue"));
      }
      if (dashboardItems.get("itemName").equals("label.active.subscriptions")) {
        Assert.assertEquals(46, dashboardItems.get("itemValue"));
      }
    }
  }

  /**
   * @author Abhaik
   * @description : Test to get Home Items for Tenant from SP User with Null Service Instance
   */
  @Test
  public void testGetHomeItemsForTenantWithInstantNullAsRoot() {

    Tenant systemTenant = tenantService.getSystemTenant();
    Tenant tenant = tenantDAO.find(2L);
    request.setAttribute("effectiveTenant", tenant);
    request.setAttribute("isSurrogatedTenant", Boolean.TRUE);

    String resultString = controller.getHomeItems(systemTenant, tenant.getParam(), null, map, request);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("home.items.view", resultString);

    List<Map<String, Object>> dashboardItemsList = (List<Map<String, Object>>) map.get("dashboardItems");
    Assert.assertEquals(3, dashboardItemsList.size());
    for (int i = 0; i < dashboardItemsList.size(); i++) {
      Map<String, Object> dashboardItems = dashboardItemsList.get(i);
      if (dashboardItems.get("itemName").equals("label.active.users")) {
        Assert.assertEquals(1, dashboardItems.get("itemValue"));
      }
      if (dashboardItems.get("itemName").equals("label.active.subscriptions")) {
        Assert.assertEquals(6, dashboardItems.get("itemValue"));
      }
      if (dashboardItems.get("itemName").equals("label.total.spend")) {
        Assert.assertEquals("0.0000", dashboardItems.get("itemValue").toString());
      }
    }
  }

  /**
   * @author Abhaik
   * @description : Test to get Home Items for Tenant Master User with Null Service Instance
   */
  @Test
  public void testGetHomeItemsForMasterUserWithInstantNullAsTenant() {

    User user = userDAO.find(3L);
    asUser(user);

    Tenant tenant = tenantDAO.find(2L);
    request.setAttribute("effectiveTenant", tenant);
    request.setAttribute("isSurrogatedTenant", Boolean.FALSE);

    String resultString = controller.getHomeItems(tenant, tenant.getParam(), null, map, request);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("home.items.view", resultString);

    List<Map<String, Object>> dashboardItemsList = (List<Map<String, Object>>) map.get("dashboardItems");
    for (int i = 0; i < dashboardItemsList.size(); i++) {
      Map<String, Object> dashboardItems = dashboardItemsList.get(i);
      if (dashboardItems.get("itemName").equals("label.active.users")) {
        Assert.assertEquals(1, dashboardItems.get("itemValue"));
      }
      if (dashboardItems.get("itemName").equals("label.active.subscriptions")) {
        Assert.assertEquals(6, dashboardItems.get("itemValue"));
      }
      if (dashboardItems.get("itemName").equals("label.total.spend")) {
        Assert.assertEquals("0.0000", dashboardItems.get("itemValue").toString());
      }
    }
  }

  /**
   * @author Abhaik
   * @description : Test to get Home Items for Tenant Normal User with Null Service Instance
   */
  @Test
  public void testGetHomeItemsForNormalUserWithInstantNullAsNormalUser() {

    User user = userDAO.find(22L);
    asUser(user);

    Tenant tenant = tenantDAO.find(20L);
    request.setAttribute("effectiveTenant", tenant);
    request.setAttribute("isSurrogatedTenant", Boolean.FALSE);

    String resultString = controller.getHomeItems(tenant, tenant.getParam(), null, map, request);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("home.items.view", resultString);

    BigDecimal currentSpend = (BigDecimal) map.get("currentSpend");
    Assert.assertEquals("250.0000", currentSpend.toString());

    List<Map<String, Object>> dashboardItemsList = (List<Map<String, Object>>) map.get("dashboardItems");
    for (int i = 0; i < dashboardItemsList.size(); i++) {
      Map<String, Object> dashboardItems = dashboardItemsList.get(i);
      Assert.assertFalse(dashboardItems.get("itemName").equals("label.active.users"));
      if (dashboardItems.get("itemName").equals("label.active.subscriptions")) {
        Assert.assertEquals(0, dashboardItems.get("itemValue"));
      }
      if (dashboardItems.get("itemName").equals("label.total.spend")) {
        Assert.assertEquals("250.0000", dashboardItems.get("itemValue").toString());
      }
    }
  }

  /**
   * @author Abhaik
   * @throws ConnectorManagementServiceException
   * @description : Test to get Home Items for Tenant User with Login Return is success And Password is Null
   */
  @Test
  public void testGetHomeSettingLoginReturnAndPwd() throws ConnectorManagementServiceException {

    User user = userDAO.find(3L);
    user.setClearPassword(null);
    asUser(user);

    Tenant systemTenant = tenantService.getSystemTenant();
    Tenant tenant = tenantDAO.find(3L);
    request.setAttribute("effectiveTenant", tenant);
    request.setAttribute("isSurrogatedTenant", Boolean.FALSE);

    session.setAttribute("loginreturn", "success");

    String resultString = controller.home(systemTenant, tenant.getParam(), false, map, session, request);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("redirect:/portal/users/" + user.getParam() + "/myprofile", resultString);
  }

  /**
   * @author Abhaik
   * @throws ConnectorManagementServiceException
   * @description : Test to get Home Items for Tenant User with First Name is Null
   */
  @Test
  public void testGetHomeSettingFirstName() throws ConnectorManagementServiceException {

    User spuser = userDAO.find(1L);

    User user = userDAO.find(3L);
    user.setPassword(spuser.getPassword());
    user.setFirstName(null);
    asUser(user);

    Tenant systemTenant = tenantService.getSystemTenant();
    Tenant tenant = tenantDAO.find(2L);
    request.setAttribute("effectiveTenant", tenant);
    request.setAttribute("isSurrogatedTenant", Boolean.FALSE);

    String resultString = controller.home(systemTenant, tenant.getParam(), false, map, session, request);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("redirect:/portal/users/" + user.getParam() + "/myprofile", resultString);
  }

  /**
   * @author Abhaik
   * @description : Test to get Home Items for Tenant Normal User with Null Service Instance
   */
  @Test
  public void testGetGravtarsForTenantAsRoot() {

    Tenant systemTenant = tenantService.getSystemTenant();
    Tenant tenant = tenantDAO.find(3L);
    request.setAttribute("effectiveTenant", systemTenant);
    request.setAttribute("isSurrogatedTenant", Boolean.TRUE);

    String resultString = controller.getGravtars(systemTenant, tenant.getParam(), map, request);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("home.gravtars.show", resultString);
    User user = userDAO.find(1L);
    List<User> usersForGravatarList = (List<User>) map.get("usersForGravatar");
    Assert.assertEquals(2, usersForGravatarList.size());
    User obtainedUser = usersForGravatarList.get(0);
    Assert.assertEquals(user.getEmail(), obtainedUser.getEmail());
    Assert.assertEquals(user.getFirstName(), obtainedUser.getFirstName());
    Assert.assertEquals(user.getLastName(), obtainedUser.getLastName());
    Assert.assertEquals(user.getUsername(), obtainedUser.getUsername());
    user = userDAO.find(2L);
    obtainedUser = usersForGravatarList.get(1);
    Assert.assertEquals(user.getEmail(), obtainedUser.getEmail());
    Assert.assertEquals(user.getFirstName(), obtainedUser.getFirstName());
    Assert.assertEquals(user.getLastName(), obtainedUser.getLastName());
    Assert.assertEquals(user.getUsername(), obtainedUser.getUsername());
  }

  /**
   * @author Abhaik
   * @description : Test to get Home Items for Tenant Normal User with Null Service Instance
   */
  @Test
  public void testGetGravtarsForTenantAsTenant() {

    User user = userDAO.find(3L);
    asUser(user);

    Tenant tenant = tenantDAO.find(2L);
    request.setAttribute("effectiveTenant", tenant);
    request.setAttribute("isSurrogatedTenant", Boolean.FALSE);

    String resultString = controller.getGravtars(tenant, tenant.getParam(), map, request);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("home.gravtars.show", resultString);
    List<User> usersForGravatarList = (List<User>) map.get("usersForGravatar");
    User obtainedUser = usersForGravatarList.get(0);
    Assert.assertEquals(user.getEmail(), obtainedUser.getEmail());
    Assert.assertEquals(user.getFirstName(), obtainedUser.getFirstName());
    Assert.assertEquals(user.getLastName(), obtainedUser.getLastName());
    Assert.assertEquals(user.getUsername(), obtainedUser.getUsername());
  }

}
