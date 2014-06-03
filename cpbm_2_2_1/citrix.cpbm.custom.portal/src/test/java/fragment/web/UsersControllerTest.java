/*
*  Copyright © 2013 Citrix Systems, Inc.
*  You may not use, copy, or modify this file except pursuant to a valid license agreement from
*  Citrix Systems, Inc.
*/
package fragment.web;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;

import web.WebTestsBaseWithMockConnectors;
import web.support.DispatcherTestServlet;
import web.support.MockSessionStatus;

import com.citrix.cpbm.access.proxy.CustomProxy;
import com.citrix.cpbm.platform.bootstrap.service.BootstrapActivator;
import com.citrix.cpbm.platform.spi.AccountLifecycleHandler;
import com.citrix.cpbm.platform.spi.CloudConnector;
import com.citrix.cpbm.platform.spi.UserLifecycleHandler;
import com.citrix.cpbm.platform.spi.View;
import com.citrix.cpbm.platform.spi.View.ViewMode;
import com.citrix.cpbm.platform.spi.ViewResolver;
import com.citrix.cpbm.portal.fragment.controllers.UsersController;
import com.vmops.internal.service.PaymentGatewayService;
import com.vmops.internal.service.SubscriptionService;
import com.vmops.model.Configuration;
import com.vmops.model.Profile;
import com.vmops.model.Service;
import com.vmops.model.SpendAlertSubscription;
import com.vmops.model.Tenant;
import com.vmops.model.User;
import com.vmops.model.UserAlertPreferences;
import com.vmops.model.UserAlertPreferences.AlertType;
import com.vmops.model.billing.PaymentTransaction;
import com.vmops.model.billing.PaymentTransaction.State;
import com.vmops.persistence.AuditLogDAO;
import com.vmops.persistence.SpendAlertSubscriptionDAO;
import com.vmops.persistence.UserAlertPreferencesDAO;
import com.vmops.portal.config.Configuration.Names;
import com.vmops.service.ConfigurationService;
import com.vmops.service.TenantService;
import com.vmops.service.UserAlertPreferencesService;
import com.vmops.service.UserService;
import com.vmops.service.exceptions.NoSuchTenantException;
import com.vmops.service.exceptions.NoSuchUserException;
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.forms.UserForm;
import com.vmops.web.interceptors.UserContextInterceptor;
import common.MockCloudInstance;
import common.MockTelephoneVerificationService;

public class UsersControllerTest extends WebTestsBaseWithMockConnectors {

  private ModelMap map;

  private MockSessionStatus status;

  @Autowired
  private UsersController controller;

  @Autowired
  private UserService userService;

  private HttpSession session;

  private MockHttpServletRequest request;

  @Autowired
  private UserAlertPreferencesService userAlertPreferencesService;

  @Autowired
  TenantService service;

  @Autowired
  SubscriptionService subscriptionService;

  @Autowired
  SpendAlertSubscriptionDAO dao;

  @Autowired
  UserAlertPreferencesDAO userAlertPreferencesDAO;

  @Autowired
  AuditLogDAO auditDAO;

  @Autowired
  ConfigurationService configurationService;

  private BootstrapActivator bootstrapActivator = new BootstrapActivator();

  private static boolean isMockInstanceCreated = false;

  private PaymentGatewayService ossConnector = null;

  private CloudConnector iaasConnector = null;

  private ViewResolver viewResolver;

  @Before
  public void init() throws Exception {
    map = new ModelMap();
    status = new MockSessionStatus();
    session = new MockHttpSession();
    prepareMock(true, bootstrapActivator);
    if (isMockInstanceCreated == false) {

      Service ossService = serviceDAO.find(7l);
      ossService.setEnabled(true);
      Service cloudService = serviceDAO.find(6l);
      connectorManagementService.getAllServiceInstances(cloudService);

      isMockInstanceCreated = true;
    }
    asRoot();
  }

  @Override
  protected void prepareMock(boolean adaptor, BootstrapActivator bootstrapActivator) {
    super.prepareMock(adaptor, bootstrapActivator);
    MockCloudInstance mock = this.getMockCloudInstance();
    CloudConnector cloudConnector = mock.getCloudConnector();
    viewResolver = mock.getViewResolver();

    ossConnector = EasyMock.createMock(PaymentGatewayService.class);
    iaasConnector = EasyMock.createMock(CloudConnector.class);
    mockAccountLifecycleHandler = EasyMock.createMock(AccountLifecycleHandler.class);
    mockUserLifecycleHandler = EasyMock.createMock(UserLifecycleHandler.class);

    EasyMock.reset(ossConnector);

    EasyMock.expect(iaasConnector.getAccountLifeCycleHandler()).andReturn(mockAccountLifecycleHandler).anyTimes();

    View view = EasyMock.createMock(View.class);
    EasyMock.expect(viewResolver.getConsoleView(EasyMock.anyObject(User.class))).andReturn(view).anyTimes();

    EasyMock.expect(iaasConnector.getUserLifeCycleHandler()).andReturn(mockUserLifecycleHandler).anyTimes();

    EasyMock.expect(ossConnector.getAccountLifeCycleHandler()).andReturn(mockAccountLifecycleHandler).anyTimes();
    EasyMock.expect(ossConnector.getUserLifeCycleHandler()).andReturn(mockUserLifecycleHandler).anyTimes();

    final Capture<BigDecimal> amount = new Capture<BigDecimal>();
    EasyMock.expect(ossConnector.authorize(EasyMock.anyObject(Tenant.class), EasyMock.capture(amount)))
        .andAnswer(new IAnswer<PaymentTransaction>() {

          public PaymentTransaction answer() throws Throwable {
            return new PaymentTransaction(new Tenant(), 0, State.COMPLETED,
                com.vmops.model.billing.PaymentTransaction.Type.CAPTURE);
          }
        }).anyTimes();
    EasyMock.replay(iaasConnector);
    EasyMock.replay(ossConnector);
    EasyMock.replay(viewResolver);
    EasyMock.replay(cloudConnector);

  }

  @SuppressWarnings({
    "rawtypes"
  })
  @Test
  public void testRouting() throws Exception {
    logger.debug("Testing routing....");
    DispatcherTestServlet servlet = this.getServletInstance();
    Class controllerClass = controller.getClass();
    Method expected = locateMethod(controllerClass, "listUsersForAccount", new Class[] {
        Tenant.class, Boolean.TYPE, String.class, ModelMap.class, HttpSession.class, String.class, int.class,
        int.class, String.class, HttpServletRequest.class
    });

    Method handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/users"));
    Assert.assertEquals(expected, handler);

    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/users/"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "show", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/users/1"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "createStepOne", new Class[] {
        Tenant.class, String.class, ModelMap.class, HttpSession.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/users/new/step1"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "login", new Class[] {
        Tenant.class, String.class, String.class, String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/users/cloud_login"));
    Assert.assertEquals(expected, handler);

  }

  @Test
  public void testUsersShow() throws Exception {
    User expected = userDAO.findAll(null).get(0);
    asUser(expected.getTenant().getOwner());

    String view = controller.show(expected.getUuid(), map);
    Assert.assertEquals("user.show", view);
    Assert.assertTrue(map.containsKey("user"));
    User found = (User) map.get("user");
    Assert.assertEquals(expected, found);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUsersListShowAll() {
    asRoot();

    List<User> expected = userDAO.findAll(null);
    User portalUser = userDAO.find("2");
    if (expected.contains(portalUser)) {
      expected.remove(portalUser);// As root user doesn't have permission on Portal User
    }
    MockHttpServletRequest request = new MockHttpServletRequest();
    String view = controller.listUsersForAccount(controller.getTenant(), true, null, map, session, null, 1, 20,
        "false", request);
    Assert.assertEquals("users.list_with_admin_menu", view);
    Assert.assertTrue(map.containsKey("users"));
    List<User> found = (List<User>) map.get("users");
    Assert.assertEquals(new HashSet<User>(expected), new HashSet<User>(found));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUsersListShowTenant() {
    asRoot();
    Tenant tenant = getDefaultTenant();
    List<User> expected = userService.list(0, 0, null, null, false, null, tenant.getId().toString(), null);
    MockHttpServletRequest request = new MockHttpServletRequest();
    String view = controller.listUsersForAccount(controller.getTenant(), true, null, map, session, null, 1, 20, "true",
        request);
    Assert.assertEquals("users.list_with_admin_menu", view);
    Assert.assertTrue(map.containsKey("users"));
    List<User> found = (List<User>) map.get("users");
    Assert.assertTrue(found.containsAll(expected));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUsersList() {
    User user = userDAO.find(3L);
    asUser(user);
    List<User> expected = userService.list(0, 0, null, null, false, null, user.getTenant().getId().toString(), null);
    MockHttpServletRequest request = new MockHttpServletRequest();
    String view = controller.listUsersForAccount(controller.getTenant(), true, null, map, session, null, 1, 20, "true",
        request);
    Assert.assertEquals("users.nonroot.list_with_user_menu", view);
    Assert.assertTrue(map.containsKey("users"));
    List<User> found = (List<User>) map.get("users");
    Assert.assertTrue(found.containsAll(expected));
    Assert.assertTrue(map.get("page") == Page.ADMIN_ALL_USERS);
  }

  @Test
  public void testUserNewStep1() {
    User user = userDAO.find(3L);
    asUser(user);
    MockHttpServletRequest request = new MockHttpServletRequest();
    String view = controller.createStepOne(controller.getTenant(), null, map, new MockHttpSession(), request);
    UserForm form = (UserForm) map.get("user");
    Assert.assertNotNull(form);
    Assert.assertNotNull(form.getUser());
    Assert.assertEquals("users.new.step1", view);
    Assert.assertEquals(user.getTenant().getAccountId(),
        ((com.citrix.cpbm.access.Tenant) map.get("userTenant")).getAccountId());
  }

  @Test
  public void testUserNewDifferentTenantAsRoot() {
    asRoot();

    Tenant other = tenantDAO.find(3L);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("isSurrogatedTenant", false);
    String view = controller.createStepOne(other, other.getUuid(), map, new MockHttpSession(), request);
    UserForm form = (UserForm) map.get("user");
    Assert.assertNotNull(form);
    Assert.assertNotNull(form.getUser());
    Assert.assertEquals("users.new.step1", view);
    Assert.assertEquals(other.getAccountId(), ((com.citrix.cpbm.access.Tenant) map.get("userTenant")).getAccountId());
  }

  @Test
  public void testCreateUserStep2() throws Exception {
    User user = userDAO.find(3L);
    asUser(user);
    UserForm form = new UserForm();
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    com.citrix.cpbm.access.User newUser = form.getUser();
    newUser.setEmail("test@test.com");
    newUser.setUsername("testuser");
    newUser.setFirstName("firstName");
    newUser.setLastName("lastName");
    Profile profile = profileDAO.findByName("User");
    form.setUserProfile(profile.getId());
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("submitButtonEmail", "Finish");
    BindingResult bindingResult = validate(form);
    com.citrix.cpbm.access.Tenant proxyTenant = (com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(controller
        .getTenant());
    String view = controller.createUserStepTwo(form, bindingResult, proxyTenant, map, request, new MockHttpSession());
    Assert.assertEquals("users.newuserregistration.finish", view);
    Assert.assertNotNull(map.get("user"));
    Assert.assertEquals(form, map.get("user"));
  }

  @Test
  public void testCreateUserStep2WithSuffix() throws Exception {
    asRoot();
    com.vmops.model.Configuration configuration = configurationService
        .locateConfigurationByName(Names.com_citrix_cpbm_username_duplicate_allowed);
    configuration.setValue("true");
    configurationService.update(configuration);

    User user = userDAO.find(3L);

    Tenant userTenant = user.getTenant();
    String suffix = "testSuffix";
    userTenant.setUsernameSuffix(suffix);
    tenantService.update(userTenant);

    asUser(user);
    UserForm form = new UserForm();
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    com.citrix.cpbm.access.User newUser = form.getUser();
    newUser.setEmail("test@test.com");
    newUser.setUsername("testuser");
    newUser.setFirstName("firstName");
    newUser.setLastName("lastName");
    Profile profile = profileDAO.findByName("User");
    form.setUserProfile(profile.getId());
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("submitButtonEmail", "Finish");
    BindingResult bindingResult = validate(form);
    com.citrix.cpbm.access.Tenant proxyTenant = (com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(controller
        .getTenant());
    String view = controller.createUserStepTwo(form, bindingResult, proxyTenant, map, request, new MockHttpSession());
    Assert.assertEquals("users.newuserregistration.finish", view);
    Assert.assertNotNull(map.get("user"));
    Assert.assertEquals(form, map.get("user"));

    User found = userService.getUserByParam("username", "testuser@" + suffix, false);
    Assert.assertNotNull(found);
    Assert.assertEquals("firstName", found.getFirstName());
    Assert.assertEquals("lastName", found.getLastName());

  }

  @Test
  public void testCreateUserStep2CustomizeEmail() throws Exception {
    User user = userDAO.find(3L);
    asUser(user);
    UserForm form = new UserForm();
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    com.citrix.cpbm.access.User newUser = form.getUser();
    newUser.setEmail("test@test.com");
    newUser.setUsername("testuser");
    newUser.setFirstName("firstName");
    newUser.setLastName("lastName");
    Profile profile = profileDAO.findByName("User");
    form.setUserProfile(profile.getId());
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("submitButtonEmail", "CustomeEmail");
    BindingResult bindingResult = validate(form);
    com.citrix.cpbm.access.Tenant proxyTenant = (com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(controller
        .getTenant());
    String view = controller.createUserStepTwo(form, bindingResult, proxyTenant, map, request, new MockHttpSession());
    Assert.assertEquals("users.newuser.customemail", view);
    Assert.assertNotNull(map.get("emailText"));
    Assert.assertEquals(form, map.get("user"));
  }

  @Test
  public void testCreateUserStep2Failed() throws Exception {
    User user = userDAO.find(3L);
    asUser(user);
    UserForm form = new UserForm();
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    com.citrix.cpbm.access.User newUser = form.getUser();
    newUser.setEmail("test@test.com");
    newUser.setUsername("testuser##");
    newUser.setFirstName("firstName");
    newUser.setLastName("lastName");
    Profile profile = profileDAO.findByName("User");
    form.setUserProfile(profile.getId());

    BindingResult bindingResult = validate(form);
    Assert.assertTrue(bindingResult.hasErrors());
    com.citrix.cpbm.access.Tenant proxyTenant = (com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(controller
        .getTenant());
    String view = controller.createUserStepTwo(form, bindingResult, proxyTenant, map, new MockHttpServletRequest(),
        new MockHttpSession());
    Assert.assertFalse("users.newuserregistration.finish".equals(view));
  }

  @Test
  public void testCreateUserwithEmailAsUserName() throws Exception {
    User user = userDAO.find(3L);
    asUser(user);
    UserForm form = new UserForm();
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    com.citrix.cpbm.access.User newUser = form.getUser();
    newUser.setEmail("test@test.com");
    newUser.setUsername("testuser@gmail.com");
    newUser.setFirstName("firstName");
    newUser.setLastName("lastName");
    Profile profile = profileDAO.findByName("User");
    form.setUserProfile(profile.getId());

    BindingResult bindingResult = validate(form);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("submitButtonEmail", "Finish");
    com.citrix.cpbm.access.Tenant proxyTenant = (com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(controller
        .getTenant());
    String view = controller.createUserStepTwo(form, bindingResult, proxyTenant, map, request, new MockHttpSession());
    Assert.assertEquals("users.newuserregistration.finish", view);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateUserUnprivileged() throws Exception {
    User user = createTestUserInTenant(getDefaultTenant());
    asUser(user);
    UserForm form = new UserForm();
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    com.citrix.cpbm.access.User newUser = form.getUser();
    newUser.setUsername("foobar");
    newUser.setEmail("test@test.com");

    validate(form);
    MockHttpServletRequest request = new MockHttpServletRequest();
    controller.createStepOne(controller.getTenant(), null, map, new MockHttpSession(), request);
  }

  @Test
  public void testEditUser() throws Exception {
    User user = userDAO.find(3L);
    asRoot();

    String view = controller.edit(null, user.getUuid(),
        getRequestTemplate(HttpMethod.GET, "/users/" + user.getUuid() + "/myprofile"), map);
    UserForm form = (UserForm) map.get("user");
    Assert.assertNotNull(form);
    Assert.assertNotNull(form.getUser());
    Assert.assertEquals(user.getUsername(), ((com.citrix.cpbm.access.User) form.getUser()).getUsername());
    Assert.assertEquals("users.edit.myprofile", view);
  }

  @Test
  public void testUpdateUser() throws Exception {
    asRoot();
    User user = userDAO.find(3L);
    UserForm form = new UserForm((com.citrix.cpbm.access.User) CustomProxy.newInstance(user));
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    user.setFirstName("Updated");
    BindingResult result = validate(form);
    form.setUserClone((com.citrix.cpbm.access.User) CustomProxy.newInstance((User) user.clone()));
    String view = controller.edit(user.getParam(), form, result,
        getRequestTemplate(HttpMethod.POST, "/users/3/myprofile"), map, status);
    Assert.assertEquals("redirect:/portal/users/" + user.getUuid() + "/myprofile", view);
    Assert.assertTrue(status.isComplete());
    User found = userDAO.find(user.getId());
    Assert.assertEquals(user.getFirstName(), found.getFirstName());
    Assert.assertEquals(user.getAddress(), found.getAddress());
    Assert.assertTrue(status.isComplete());
  }

  @Test
  public void testVerifyOldPassword() throws Exception {
    asRoot();
    User user = userDAO.find(3L);
    UserForm form = new UserForm((com.citrix.cpbm.access.User) CustomProxy.newInstance(user));
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    user.setClearPassword("Portal123#");
    user.setOldPassword("Anything123#");
    BindingResult result = validate(form);
    form.setUserClone((com.citrix.cpbm.access.User) CustomProxy.newInstance((User) user.clone()));
    String response = controller.changePassword(form, result,
        getRequestTemplate(HttpMethod.POST, "/users/changePassword?userParam=3"), map, status, user.getParam());
    Assert.assertEquals("failure", response);

    user = userDAO.find(3L);
    form = new UserForm((com.citrix.cpbm.access.User) CustomProxy.newInstance(user));
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    user.setClearPassword("Portal123#");
    user.setOldPassword("Portal123#");
    result = validate(form);
    form.setUserClone((com.citrix.cpbm.access.User) CustomProxy.newInstance((User) user.clone()));
    response = controller.changePassword(form, result,
        getRequestTemplate(HttpMethod.POST, "/users/changePassword?userParam=3"), map, status, user.getParam());
    Assert.assertEquals("success", response);
  }

  @Test
  @Ignore
  public void testUpdateUserFail() throws Exception {
    asRoot();
    User user = userDAO.find(3L);
    UserForm form = new UserForm((com.citrix.cpbm.access.User) CustomProxy.newInstance(user));
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    user.setFirstName("Updated");
    form.setUserClone((com.citrix.cpbm.access.User) CustomProxy.newInstance((User) user.clone()));
    BindingResult result = validate(form);
    result.reject("dummy");
    String view = controller.edit(user.getParam(), form, result,
        getRequestTemplate(HttpMethod.POST, "/users/" + user.getUuid() + "/myprofile"), map, status);
    Assert.assertEquals("redirect:/portal/users/" + user.getUuid() + "/myprofile", view);
    Object o = map.get("user");
    Assert.assertNull(o);
    Assert.assertFalse(status.isComplete());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUserSearchAsRoot() throws Exception {
    asRoot();

    String view = controller.search(controller.getTenant(), "po", null, map);
    Assert.assertEquals("users.search_results", view);
    Object o = map.get("users");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof List);
    List<User> list = (List<User>) o;
    Assert.assertEquals(0, list.size());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUserSearchDifferentTenant() throws Exception {
    asRoot();
    String view = controller.search(controller.getTenant(), "2_", getDefaultTenant().getUuid(), map);
    Assert.assertEquals("users.search_results", view);
    Object o = map.get("users");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof List);
    List<User> list = (List<User>) o;
    Assert.assertEquals(1, list.size());
    Assert.assertEquals("2_retail1", list.get(0).getUsername());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUserSearch() throws Exception {
    asUser(getDefaultTenant().getOwner());

    String view = controller.search(controller.getTenant(), "2_", null, map);
    Assert.assertEquals("users.search_results", view);
    Object o = map.get("users");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof List);
    List<User> list = (List<User>) o;
    Assert.assertEquals(1, list.size());
    Assert.assertEquals("2_retail1", list.get(0).getUsername());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUserSearchDifferentTenantNotRoot() throws Exception {
    asRoot();
    Tenant tenant = createTestTenant(accountTypeDAO.getOnDemandPostPaidAccountType());
    asUser(getDefaultTenant().getOwner());

    String view = controller.search(controller.getTenant(), "2_", tenant.getAccountId(), map);
    Assert.assertEquals("users.search_results", view);
    Object o = map.get("users");
    Assert.assertNotNull(o);
    Assert.assertTrue(o instanceof List);
    List<User> list = (List<User>) o;
    Assert.assertEquals(1, list.size());
    Assert.assertEquals("2_retail1", list.get(0).getUsername());
  }

  @Test
  public void testGetISDCode() throws Exception {
    String code = controller.getISDCodeByCounty("US");
    Assert.assertEquals("1", code);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testverifyPassword() {
    Map<String, Object> Hashmap = controller.verifyPassword("Portal123#", new MockHttpServletRequest());
    Assert.assertNotNull(Hashmap.get("userCredentialList"));
    List<Map<String, String>> credentialList = new ArrayList<Map<String, String>>();
    credentialList = (List<Map<String, String>>) Hashmap.get("userCredentialList");
    Assert.assertEquals(credentialList.get(0).get("apiKey"),
        "_KvuJ0mLMn4Z_FyLnQawAth-y1tobX3t44UUN1QYVyGVVyZETh_EcjyMrmEoEV-4y_5G_6AcHUw61FcFVv_qGw");
    Assert.assertEquals(credentialList.get(0).get("secretKey"),
        "gvuiqvCkBzcLuVFEtYVjY_vE9-X6m4bsxmcHMcXQMz7Iy-QPZs1X_RlvZW0-5reoWqMmql2eA1DEFNOsMVyFkg");
    Assert.assertEquals(credentialList.get(0).get("InstanceName"), "SERVICE11");
    Assert.assertEquals(credentialList.get(0).get("ServiceName"), "com.company1.service1.service.name");
    Assert.assertEquals(credentialList.get(0).get("InstanceName"), "SERVICE11");

  }

  @Test
  public void testdeactivate() {
    // Case 1: user is active
    User user = userService.getUserByParam("id", "3", false);
    user.setEnabled(true);
    user.setLocked(false);
    userService.save(user);
    String view = controller.deactivate(user.getParam(), "ajax", map);
    Assert.assertNotNull(view);
    Assert.assertEquals(view, new String("user.show"));
    Assert.assertEquals(user.isEnabled(), true);
    Assert.assertEquals(user.isLocked(), false);
    Assert.assertTrue(map.containsAttribute("user"));
    Assert.assertTrue(map.containsAttribute("profileName"));
    Assert.assertEquals(user, map.get("user"));
    Assert.assertEquals(map.get("profileName"), user.getProfile().getName());

    // Case 2: user is not active
    user = userService.getUserByParam("id", "3", false);
    user.setEnabled(false);
    user.setLocked(true);
    userService.save(user);
    view = controller.deactivate(user.getParam(), "ajax", map);
    Assert.assertNotNull(view);
    Assert.assertEquals(view, new String("user.show"));
    Assert.assertEquals(user.isEnabled(), false);
    Assert.assertEquals(user.isLocked(), true);
    Assert.assertTrue(map.containsAttribute("user"));
    Assert.assertTrue(map.containsAttribute("profileName"));
    Assert.assertEquals(user, map.get("user"));
    Assert.assertEquals(map.get("profileName"), user.getProfile().getName());
  }

  @Test
  public void testdeactivateUser() {
    User user = userService.getUserByParam("id", "1", false);
    user = controller.deactivateUser(user.getParam(), map);
    Assert.assertEquals(user.isEnabled(), false);
    Assert.assertEquals(user.isLocked(), true);
    Assert.assertTrue(map.containsAttribute("user"));
    Assert.assertEquals(user, map.get("user"));

  }

  @Test
  public void testActivate() {
    // Case 1: when the user is not active
    User user = userService.getUserByParam("id", "3", false);
    user.setEnabled(false);
    user.setLocked(true);
    userService.save(user);
    String view = controller.activate(user.getParam(), "ajax", map);
    Assert.assertNotNull(view);
    Assert.assertEquals(view, new String("user.show"));
    Assert.assertEquals(user.isEnabled(), false);
    Assert.assertEquals(user.isLocked(), true);
    Assert.assertEquals(user.getFailedLoginAttempts(), 0);
    Assert.assertTrue(map.containsAttribute("user"));
    Assert.assertTrue(map.containsAttribute("profileName"));
    Assert.assertEquals(user, map.get("user"));
    Assert.assertEquals(map.get("profileName"), user.getProfile().getName());

    // Case 2: when the user is active
    user = userService.getUserByParam("id", "4", false);
    user.setEnabled(true);
    user.setLocked(false);
    userService.save(user);
    view = controller.activate(user.getParam(), "ajax", map);
    Assert.assertNotNull(view);
    Assert.assertEquals(view, new String("user.show"));
    Assert.assertEquals(user.isEnabled(), true);
    Assert.assertEquals(user.isLocked(), false);
    Assert.assertEquals(user.getFailedLoginAttempts(), 0);
    Assert.assertTrue(map.containsAttribute("user"));
    Assert.assertTrue(map.containsAttribute("profileName"));
    Assert.assertEquals(user, map.get("user"));
    Assert.assertEquals(map.get("profileName"), user.getProfile().getName());
  }

  @Test
  public void testactivateUser() {
    User user = userService.getUserByParam("id", "1", false);
    user = controller.activateUser(user.getParam(), map);
    Assert.assertEquals(user.isEnabled(), true);
    Assert.assertEquals(user.isLocked(), false);
    Assert.assertTrue(map.containsAttribute("user"));
    Assert.assertEquals(user, map.get("user"));
  }

  @Test
  public void testresendVerificationail() {
    User user = userService.getUserByParam("id", "1", false);
    String view = controller.resendVerificationEmail(user.getParam(), "ajax", map);
    Assert.assertNotNull(view);
    Assert.assertEquals(view, new String("user.show"));
    Assert.assertTrue(map.containsAttribute("user"));
    Assert.assertTrue(map.containsAttribute("profileName"));
    Assert.assertEquals(user, map.get("user"));
    Assert.assertEquals(map.get("profileName"), user.getProfile().getName());

  }

  @Test
  public void testverifyMaxSubscription() {
    Tenant tenant = service.getTenantByParam("id", "2", false);
    String verify = controller.verifyMaxSubscription("2", tenant.getAccountId());
    Assert.assertTrue(Boolean.valueOf(verify));
    try {
      controller.verifyMaxSubscription("2", "2");
      Assert.fail();
    } catch (NoSuchTenantException e) {
    }
  }

  @Test
  public void testgetUserTimezoneOffset() {
    Tenant tenant = service.getTenantByParam("id", "2", false);
    Tenant tenant1 = service.getTenantByParam("id", "3", false);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("isSurrogatedTenant", false);
    request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
    String timeZone = controller.getUserTimezoneOffset(tenant, request);
    Assert.assertEquals(timeZone, new String("5.50"));
    tenant1.getOwner().setTimeZone(null);
    service.save(tenant1);
    request.setAttribute("isSurrogatedTenant", true);
    request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant1);
    timeZone = controller.getUserTimezoneOffset(tenant, request);
    Assert.assertNotNull(timeZone);
  }

  @Test
  public void testremoveSpendAlertSubscription() {
    Tenant tenant = createTestTenant(accountTypeDAO.getOnDemandPostPaidAccountType());
    User owner = createTestUserInTenant(tenant);
    SpendAlertSubscription subscription = new SpendAlertSubscription(41, "444", tenant, owner);
    dao.save(subscription);
    String remove = controller.removeSpendAlertSubscription(subscription.getId().toString(), map);
    Assert.assertEquals(remove, new String("redirect:/portal/users/subscribe"));
  }

  @Test
  public void testsaveAlertsDeliveryOptions() {
    String status = controller.saveAlertsDeliveryOptions("Test@citrix.com", map);
    Assert.assertEquals(status, new String("success"));
    Assert.assertNotNull(map.containsAttribute("user"));
    Assert.assertEquals(userAlertPreferencesService.getCount((User) map.get("user")), 1);
    status = controller.saveAlertsDeliveryOptions(null, map);
    Assert.assertEquals(status, new String("failure"));

  }

  @Test
  public void testLogin() {
    Tenant tenant = service.getTenantByParam("id", "2", false);
    String login = controller.login(tenant, tenant.getParam(), "Test", "4847df70-63bb-4273-a8db-30662b32d098", map);
    Assert.assertEquals(login, new String("redirect:users.cloud_deleted&lp=Test"));
  }

  @Test
  public void testvalidateemail() {
    String valid = controller.validateemail(null, "TEST@citrix.com", null, map);
    Assert.assertTrue(Boolean.valueOf(valid));

  }

  @Test
  public void testeditPrefs() throws Exception {
    User user = userService.getUserByParam("id", "3", false);
    UserForm form = new UserForm();
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    form.setUser((com.citrix.cpbm.access.User) CustomProxy.newInstance(user));
    MockHttpServletRequest request = new MockHttpServletRequest();
    BindingResult bindingResult = validate(form);
    Map<String, String> HashMap = controller.editPrefs(form, bindingResult, VALID_TIMEZONE, Locale.ENGLISH.toString(),
        map, request);
    Assert.assertNotNull(HashMap);
    Assert.assertEquals(user.getTimeZone(), VALID_TIMEZONE);
    Assert.assertEquals(user.getLocale(), Locale.ENGLISH);
    Assert.assertTrue(HashMap.containsKey("timeZone"));
    Assert.assertTrue(HashMap.containsKey("locale"));
    Assert.assertTrue(HashMap.containsKey("lastLogin"));
    Assert.assertEquals(HashMap.get("timeZone"), VALID_TIMEZONE);
    Assert.assertEquals(HashMap.get("locale").toString(), new String("English/US"));
    HashMap.clear();
    HashMap = controller.editPrefs(form, null, null, null, map, request);
    Assert.assertNull(user.getLocale());

  }

  @Test
  public void testdelete() {
    User user = userDAO.find(3L);
    String view = null;
    try {
      view = controller.delete(user.getParam(), map);
    } catch (NoSuchUserException e) {
      e.printStackTrace();
    }
    Assert.assertNull(view);
  }

  @Test
  public void testResolveViewForSettingFromServiceInstance() {
    View view = controller.resolveViewForSettingFromServiceInstance(getDefaultTenant(),
        "003fa8ee-fba3-467f-a517-fd806dae8a80");
    Assert.assertEquals("view", view.getName());
    Assert.assertEquals("http://www.google.com", view.getURL());
    Assert.assertEquals(ViewMode.IFRAME, view.getMode());
  }

  @Test
  public void testResolveViewForAccountSettingFromServiceInstance() {
    Tenant tenant = service.getTenantByParam("id", "2", false);
    View view = controller.resolveViewForAccountSettingFromServiceInstance(tenant.getParam(),
        "003fa8ee-fba3-467f-a517-fd806dae8a80");
    Assert.assertEquals("view", view.getName());
    Assert.assertEquals("http://www.google.com", view.getURL());
    Assert.assertEquals(ViewMode.IFRAME, view.getMode());
  }

  @Test
  public void testshow() {
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    asUser(tenant.getOwner());
    controller.show(tenant.getOwner().getParam(), map);
    Assert.assertTrue(map.containsKey("serviceRegistrationStatus"));
    Assert.assertEquals(Boolean.FALSE, map.get("showEnableServiceLink"));
  }

  @Test
  public void testEnableService() {
    Tenant tenant = tenantService.get("17eda09f-506b-4364-b67e-469071429b76");
    User user = createTestUserInTenant(tenant);
    user = controller.activateUser(user.getParam(), map);
    user.setEnabled(true);
    String userUUID = user.getUuid();
    Map<String, Boolean> es = controller.enableServices("17eda09f-506b-4364-b67e-469071429b76", userUUID);
    Assert.assertTrue(!es.isEmpty());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUsersListShowForSurrogatedTenant() {
    asRoot();
    Tenant tenant = getDefaultTenant();
    List<User> expected = userService.list(0, 0, null, null, false, null, tenant.getId().toString(), null);
    HttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("isSurrogatedTenant", Boolean.TRUE);
    String view = controller.listUsersForAccount(controller.getTenant(), false, tenant.getParam(), map, session, null,
        1, 20, "true", request);
    Assert.assertEquals("users.list_with_user_menu", view);
    Assert.assertTrue(map.containsKey("users"));
    List<User> found = (List<User>) map.get("users");
    Assert.assertTrue(found.containsAll(expected));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUsersListWithUserParam() {
    User user = userDAO.find(3L);
    asUser(user);
    List<User> expected = userService.list(0, 0, null, null, false, null, user.getTenant().getId().toString(), null);
    MockHttpServletRequest request = new MockHttpServletRequest();
    String view = controller.listUsersForAccount(controller.getTenant(), true, null, map, session, user.getParam(), 1,
        20, "true", request);
    Assert.assertEquals("users.nonroot.list_with_user_menu", view);
    Assert.assertTrue(map.containsKey("users"));
    List<User> found = (List<User>) map.get("users");
    Assert.assertTrue(found.containsAll(expected));
    Assert.assertTrue(map.get("page") == Page.ADMIN_ALL_USERS);
  }

  @Test
  public void testCreateUserwithBlackListedEmail() throws Exception {
    Configuration configuration = configurationService
        .locateConfigurationByName("com.citrix.cpbm.accountManagement.onboarding.emailDomain.blacklist");
    configuration.setValue("test.com");
    configurationService.update(configuration);

    User user = userDAO.find(3L);
    asUser(user);
    UserForm form = new UserForm();
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    com.citrix.cpbm.access.User newUser = form.getUser();
    newUser.setEmail("subodh@test.com");
    newUser.setUsername("Subodh");
    newUser.setFirstName("firstName");
    newUser.setLastName("lastName");
    Profile profile = profileDAO.findByName("User");
    form.setUserProfile(profile.getId());

    BindingResult bindingResult = validate(form);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("submitButtonEmail", "Finish");
    com.citrix.cpbm.access.Tenant proxyTenant = (com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(controller
        .getTenant());
    String view = controller.createUserStepTwo(form, bindingResult, proxyTenant, map, request, new MockHttpSession());
    Assert.assertEquals("users.new.step1", view);
  }

  @Test
  public void testCreateUserwithBlackListedEmail2() throws Exception {
    Configuration configuration = configurationService
        .locateConfigurationByName("com.citrix.cpbm.accountManagement.onboarding.emailDomain.blacklist");
    configuration.setValue("test.com");
    configurationService.update(configuration);
    asRoot();
    UserForm form = new UserForm();
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    com.citrix.cpbm.access.User newUser = form.getUser();
    newUser.setEmail("subodh@test.com");
    newUser.setUsername("Subodh");
    newUser.setFirstName("firstName");
    newUser.setLastName("lastName");
    Profile profile = profileDAO.findByName("User");
    form.setUserProfile(profile.getId());

    BindingResult bindingResult = validate(form);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("submitButtonEmail", "Finish");
    com.citrix.cpbm.access.Tenant proxyTenant = (com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(controller
        .getTenant());
    String view = controller.createUserStepTwo(form, bindingResult, proxyTenant, map, request, new MockHttpSession());
    Assert.assertEquals("users.new.step1", view);
  }

  @Test
  public void testCreateUserwithSurrogatedTenantBlackListedEmail() throws Exception {
    Configuration configuration = configurationService
        .locateConfigurationByName("com.citrix.cpbm.accountManagement.onboarding.emailDomain.blacklist");
    configuration.setValue("test.com");
    configurationService.update(configuration);
    asRoot();
    // Tenant tenant = getDefaultTenant();
    // User user = tenant.getOwner();
    UserForm form = new UserForm();
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    com.citrix.cpbm.access.User newUser = form.getUser();
    newUser.setEmail("subodh@test.com");
    newUser.setUsername("Subodh");
    newUser.setFirstName("firstName");
    newUser.setLastName("lastName");
    Profile profile = profileDAO.findByName("User");
    form.setUserProfile(profile.getId());
    request = new MockHttpServletRequest();
    request.setAttribute("isSurrogatedTenant", Boolean.TRUE);
    BindingResult bindingResult = validate(form);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("submitButtonEmail", "Finish");
    Tenant tenant = tenantDAO.find(3L);
    com.citrix.cpbm.access.Tenant proxyTenant = (com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(tenant);
    String view = controller.createUserStepTwo(form, bindingResult, proxyTenant, map, request, new MockHttpSession());
    Assert.assertEquals("users.new.step1", view);
  }

  @Test
  public void testCreateUserwithSurrogatedTenantBlackListedEmail2() throws Exception {
    Configuration configuration = configurationService
        .locateConfigurationByName("com.citrix.cpbm.accountManagement.onboarding.emailDomain.blacklist");
    configuration.setValue("test.com");
    configurationService.update(configuration);
    asRoot();
    // Tenant tenant = getDefaultTenant();
    // User user = tenant.getOwner();
    UserForm form = new UserForm();
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    com.citrix.cpbm.access.User newUser = form.getUser();
    newUser.setEmail("subodh@test.com");
    newUser.setUsername("Subodh");
    newUser.setFirstName("firstName");
    newUser.setLastName("lastName");
    Profile profile = profileDAO.findByName("User");
    form.setUserProfile(profile.getId());

    BindingResult bindingResult = validate(form);
    MockHttpServletRequest request = new MockHttpServletRequest();
    // request.setAttribute("isSurrogatedTenant", Boolean.TRUE);
    request.addParameter("submitButtonEmail", "Finish");
    Tenant tenant = tenantDAO.find(3L);
    com.citrix.cpbm.access.Tenant proxyTenant = (com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(tenant);
    String view = controller.createUserStepTwo(form, bindingResult, proxyTenant, map, request, new MockHttpSession());
    Assert.assertEquals("users.new.step1", view);
  }

  @Test
  public void testCreateUserMaxUserCount() throws Exception {

    Configuration configuration = configurationService
        .locateConfigurationByName("com.citrix.cpbm.accountManagement.onboarding.emailDomain.blacklist");
    configuration.setValue("test.com");
    configurationService.update(configuration);
    Tenant tenant = tenantDAO.find(3L);
    List<User> userList = tenant.getAllUsers();
    int count = userList.size();
    long temp = Long.valueOf(count);
    tenant.setMaxUsers(temp);
    tenantService.update(tenant);
    UserForm form = new UserForm();
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    com.citrix.cpbm.access.User newUser = form.getUser();
    newUser.setEmail("subodh@test.com");
    newUser.setUsername("Subodh");
    newUser.setFirstName("firstName");
    newUser.setLastName("lastName");
    Profile profile = profileDAO.findByName("User");
    form.setUserProfile(profile.getId());

    BindingResult bindingResult = validate(form);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("submitButtonEmail", "Finish");
    com.citrix.cpbm.access.Tenant proxyTenant = (com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(tenant);
    try {
      controller.createUserStepTwo(form, bindingResult, proxyTenant, map, request, new MockHttpSession());
    } catch (Exception e) {
      Assert.assertEquals(
          "You have reached the maximum number of users. To increase your limit, please contact support",
          e.getMessage());
      logger.debug("###Exiting testCreateUserMaxUserCount");
    }
  }

  @Test
  public void testCreateStepThree() throws Exception {

    UserForm form = new UserForm();
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    com.citrix.cpbm.access.User newUser = form.getUser();
    newUser.setEmail("subodh@test.com");
    newUser.setUsername("Subodh");
    newUser.setFirstName("firstName");
    newUser.setLastName("lastName");
    Profile profile = profileDAO.findByName("User");
    form.setUserProfile(profile.getId());
    form.setEmailText("Customized Email");

    BindingResult bindingResult = validate(form);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("submitButtonEmail", "Finish");
    com.citrix.cpbm.access.Tenant proxyTenant = (com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(controller
        .getTenant());
    String response = controller.createStepThree(form, bindingResult, proxyTenant, map, request);
    Assert.assertEquals("users.newuserregistration.finish", response);
  }

  @Test
  public void testEditUserWithTeleVerification() throws Exception {
    User user = userDAO.find(3L);
    asRoot();
    MockTelephoneVerificationService.ENABLE_FLAG = true;
    String view = controller.edit(null, user.getUuid(),
        getRequestTemplate(HttpMethod.GET, "/users/" + user.getUuid() + "/myprofile"), map);
    UserForm form = (UserForm) map.get("user");
    Assert.assertNotNull(form);
    Assert.assertNotNull(form.getUser());
    Assert.assertEquals(user.getUsername(), ((com.citrix.cpbm.access.User) form.getUser()).getUsername());
    Assert.assertEquals("users.edit.myprofile", view);
  }

  @Test
  public void testViewAlertsDeliveryOptions() {
    String response = controller.viewAlertsDeliveryOptions(map);
    Assert.assertEquals(true, map.containsAttribute("alertsPrefs"));
    Assert.assertEquals("users.alerts.delivery_opts", response);
  }

  @Test
  public void testEditUserWithBlaclistedEmail() throws Exception {
    Configuration configuration = configurationService
        .locateConfigurationByName("com.citrix.cpbm.accountManagement.onboarding.emailDomain.blacklist");
    configuration.setValue("test.com");
    configurationService.update(configuration);
    asRoot();
    User user = userDAO.find(3L);
    UserForm form = new UserForm((com.citrix.cpbm.access.User) CustomProxy.newInstance(user));
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    user.setFirstName("Updated");
    user.setEmail("subodh@test.com");
    BindingResult result = validate(form);
    String view = controller.edit(user.getParam(), form, result,
        getRequestTemplate(HttpMethod.POST, "/users/3/myprofile"), map, status);
    Assert.assertEquals("users.edit.myprofile", view);

    User found = userDAO.find(user.getId());
    Assert.assertEquals(user.getFirstName(), found.getFirstName());

  }

  @Test
  public void testEditUserWithAlertPref() throws Exception {
    User user = userDAO.find(3L);
    asRoot();
    String emailAddress = "sk@test.com";

    UserAlertPreferences userAlertPreferences = userAlertPreferencesService.createUserAlertPreference(user,
        emailAddress, AlertType.USER_ALERT_EMAIL);
    userAlertPreferencesService.verifyUserAlertPreference(userAlertPreferences);
    String view = controller.edit(null, user.getUuid(),
        getRequestTemplate(HttpMethod.GET, "/users/" + user.getUuid() + "/myprofile"), map);
    UserForm form = (UserForm) map.get("user");
    Assert.assertNotNull(form);
    Assert.assertNotNull(form.getUser());
    Assert.assertEquals(user.getUsername(), ((com.citrix.cpbm.access.User) form.getUser()).getUsername());
    Assert.assertEquals("users.edit.myprofile", view);
  }

}
