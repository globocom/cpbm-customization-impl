/* Copyright (C) 2011 Cloud.com, Inc. All rights reserved. */
package fragment.web;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
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
import org.springframework.web.bind.support.SessionStatus;

import web.WebTestsBaseWithMockConnectors;
import web.support.DispatcherTestServlet;
import web.support.MockSessionStatus;
import citrix.cpbm.access.proxy.CustomProxy;
import citrix.cpbm.portal.fragment.controllers.UsersController;

import com.citrix.cpbm.platform.admin.service.exceptions.ConnectorManagementServiceException;
import com.citrix.cpbm.platform.bootstrap.service.BootstrapActivator;
import com.citrix.cpbm.platform.spi.AccountLifecycleHandler;
import com.citrix.cpbm.platform.spi.CloudConnector;
import com.citrix.cpbm.platform.spi.UserLifecycleHandler;
import com.vmops.internal.service.PaymentGatewayService;
import com.vmops.internal.service.SubscriptionService;
import com.vmops.model.Profile;
import com.vmops.model.Service;
import com.vmops.model.SpendAlertSubscription;
import com.vmops.model.Tenant;
import com.vmops.model.User;
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
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.forms.UserForm;
import com.vmops.web.interceptors.UserContextInterceptor;

public class UsersControllerTest extends WebTestsBaseWithMockConnectors {

  private ModelMap map;

  private MockSessionStatus status;

  @Autowired
  private UsersController controller;

  @Autowired
  private UserService userService;

  private HttpSession session;

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

  private  BootstrapActivator bootstrapActivator;

  private static boolean isMockInstanceCreated = false;

  private PaymentGatewayService ossConnector = null;

  private CloudConnector iaasConnector = null;
  
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
    ossConnector = EasyMock.createMock(PaymentGatewayService.class);
    iaasConnector = EasyMock.createMock(CloudConnector.class);
    mockAccountLifecycleHandler = EasyMock.createMock(AccountLifecycleHandler.class);
    mockUserLifecycleHandler = EasyMock.createMock(UserLifecycleHandler.class);
    EasyMock.reset(iaasConnector);
    EasyMock.reset(ossConnector);
    EasyMock.expect(iaasConnector.getAccountLifeCycleHandler()).andReturn(mockAccountLifecycleHandler).anyTimes();
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
        String.class, HttpServletRequest.class
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

    expected = locateMethod(controllerClass, "edit", new Class[] {
        String.class, String.class, HttpServletRequest.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/users/1/edit"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "edit", new Class[] {
        String.class, UserForm.class, BindingResult.class, HttpServletRequest.class, ModelMap.class,
        SessionStatus.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/users/1/edit"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "login", new Class[] {
        Tenant.class, String.class, String.class, String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/users/cloud_login"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "getAuditLog", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/users/1/audit_log"));
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
    String view = controller.listUsersForAccount(controller.getTenant(), true, null, map, session, null, 1, "false", request);
    Assert.assertEquals("users.list_with_admin_menu", view);
    Assert.assertTrue(map.containsKey("users"));
    List<User> found = (List<User>) map.get("users");
    Assert.assertEquals(expected, found);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUsersListShowTenant() {
    asRoot();
    Tenant tenant = getDefaultTenant();
    List<User> expected = userService.list(0, 0, null, null, false, null, tenant.getId().toString(), null);
    MockHttpServletRequest request = new MockHttpServletRequest();
    String view = controller.listUsersForAccount(controller.getTenant(), true, null, map, session, null, 1, "true", request);
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
    String view = controller.listUsersForAccount(controller.getTenant(), true, null, map, session, null, 1, "true", request);
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
        ((citrix.cpbm.access.Tenant) map.get("userTenant")).getAccountId());
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
    Assert.assertEquals(other.getAccountId(),
        ((citrix.cpbm.access.Tenant) map.get("userTenant")).getAccountId());
  }

  @Test
  public void testCreateUserStep2() throws Exception {
    User user = userDAO.find(3L);
    asUser(user);
    UserForm form = new UserForm();
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    citrix.cpbm.access.User newUser = form.getUser();
    newUser.setEmail("test@test.com");
    newUser.setUsername("testuser");
    newUser.setFirstName("firstName");
    newUser.setLastName("lastName");
    Profile profile = profileDAO.findByName("User");
    form.setUserProfile(profile.getId());
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("submitButtonEmail", "Finish");
    BindingResult bindingResult = validate(form);
    citrix.cpbm.access.Tenant proxyTenant = (citrix.cpbm.access.Tenant)CustomProxy.newInstance(controller.getTenant());
    String view = controller.createUserStepTwo(form, bindingResult, proxyTenant, map, request,
        new MockHttpSession());
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
    citrix.cpbm.access.User newUser = form.getUser();
    newUser.setEmail("test@test.com");
    newUser.setUsername("testuser");
    newUser.setFirstName("firstName");
    newUser.setLastName("lastName");
    Profile profile = profileDAO.findByName("User");
    form.setUserProfile(profile.getId());
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("submitButtonEmail", "Finish");
    BindingResult bindingResult = validate(form);
    citrix.cpbm.access.Tenant proxyTenant = (citrix.cpbm.access.Tenant)CustomProxy.newInstance(controller.getTenant());
    String view = controller.createUserStepTwo(form, bindingResult, proxyTenant, map, request,
        new MockHttpSession());
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
    citrix.cpbm.access.User newUser = form.getUser();
    newUser.setEmail("test@test.com");
    newUser.setUsername("testuser");
    newUser.setFirstName("firstName");
    newUser.setLastName("lastName");
    Profile profile = profileDAO.findByName("User");
    form.setUserProfile(profile.getId());
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("submitButtonEmail", "CustomeEmail");
    BindingResult bindingResult = validate(form);
    citrix.cpbm.access.Tenant proxyTenant = (citrix.cpbm.access.Tenant)CustomProxy.newInstance(controller.getTenant());
    String view = controller.createUserStepTwo(form, bindingResult, proxyTenant, map, request,
        new MockHttpSession());
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
    citrix.cpbm.access.User newUser = form.getUser();
    newUser.setEmail("test@test.com");
    newUser.setUsername("testuser##");
    newUser.setFirstName("firstName");
    newUser.setLastName("lastName");
    Profile profile = profileDAO.findByName("User");
    form.setUserProfile(profile.getId());

    BindingResult bindingResult = validate(form);
    Assert.assertTrue(bindingResult.hasErrors());
    citrix.cpbm.access.Tenant proxyTenant = (citrix.cpbm.access.Tenant)CustomProxy.newInstance(controller.getTenant());
    String view = controller.createUserStepTwo(form, bindingResult, proxyTenant, map,
        new MockHttpServletRequest(), new MockHttpSession());
    Assert.assertFalse("users.newuserregistration.finish".equals(view));
  }

  @Test
  public void testCreateUserwithEmailAsUserName() throws Exception {
    User user = userDAO.find(3L);
    asUser(user);
    UserForm form = new UserForm();
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    citrix.cpbm.access.User newUser = form.getUser();
    newUser.setEmail("test@test.com");
    newUser.setUsername("testuser@gmail.com");
    newUser.setFirstName("firstName");
    newUser.setLastName("lastName");
    Profile profile = profileDAO.findByName("User");
    form.setUserProfile(profile.getId());

    BindingResult bindingResult = validate(form);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("submitButtonEmail", "Finish");
    citrix.cpbm.access.Tenant proxyTenant = (citrix.cpbm.access.Tenant)CustomProxy.newInstance(controller.getTenant());
    String view = controller.createUserStepTwo(form, bindingResult, proxyTenant, map, request,
        new MockHttpSession());
    Assert.assertEquals("users.newuserregistration.finish", view);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateUserUnprivileged() throws Exception {
    User user = createTestUserInTenant(getDefaultTenant());
    asUser(user);
    UserForm form = new UserForm();
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    citrix.cpbm.access.User newUser = form.getUser();
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
        getRequestTemplate(HttpMethod.GET, "/users/" + user.getUuid() + "/edit"), map);
    UserForm form = (UserForm) map.get("user");
    Assert.assertNotNull(form);
    Assert.assertNotNull(form.getUser());
    Assert.assertEquals(user.getUsername(), ((citrix.cpbm.access.User)form.getUser()).getUsername());
    Assert.assertEquals("users.edit", view);
  }

  @Test
  public void testUpdateUser() throws Exception {
    asRoot();
    User user = userDAO.find(3L);
    UserForm form = new UserForm((citrix.cpbm.access.User)CustomProxy.newInstance(user));
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    user.setFirstName("Updated");
    BindingResult result = validate(form);
    form.setUserClone((citrix.cpbm.access.User)CustomProxy.newInstance((User) user.clone()));
    String view = controller.edit(user.getParam(), form, result, getRequestTemplate(HttpMethod.POST, "/users/3/edit"),
        map, status);
    Assert.assertEquals("redirect:/portal/users/" + user.getUuid() + "/edit", view);
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
    UserForm form = new UserForm((citrix.cpbm.access.User)CustomProxy.newInstance(user));
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    user.setClearPassword("Portal123#");
    user.setOldPassword("Anything123#");
    BindingResult result = validate(form);
    form.setUserClone((citrix.cpbm.access.User)CustomProxy.newInstance((User) user.clone()));
    String response = controller.changePassword(form, result,
        getRequestTemplate(HttpMethod.POST, "/users/changePassword?userParam=3"), map, status, user.getParam());
    Assert.assertEquals("failure", response);

    user = userDAO.find(3L);
    form = new UserForm((citrix.cpbm.access.User)CustomProxy.newInstance(user));
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    user.setClearPassword("Portal123#");
    user.setOldPassword("Portal123#");
    result = validate(form);
    form.setUserClone((citrix.cpbm.access.User)CustomProxy.newInstance((User) user.clone()));
    response = controller.changePassword(form, result,
        getRequestTemplate(HttpMethod.POST, "/users/changePassword?userParam=3"), map, status, user.getParam());
    Assert.assertEquals("success", response);
  }

  @Test
  @Ignore
  public void testUpdateUserFail() throws Exception {
    asRoot();
    User user = userDAO.find(3L);
    UserForm form = new UserForm((citrix.cpbm.access.User)CustomProxy.newInstance(user));
    form.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    user.setFirstName("Updated");
    form.setUserClone((citrix.cpbm.access.User)CustomProxy.newInstance((User) user.clone()));
    BindingResult result = validate(form);
    result.reject("dummy");
    String view = controller.edit(user.getParam(), form, result,
        getRequestTemplate(HttpMethod.POST, "/users/" + user.getUuid() + "/edit"), map, status);
    Assert.assertEquals("redirect:/portal/users/" + user.getUuid() + "/edit", view);
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
  public void testGetAuditLog() throws Exception {
    User user = createTestUserInTenant(getDefaultTenant());
    String view = controller.getAuditLog(user.getUuid(), map);
    Assert.assertEquals("auditlog.show", view);
  }

  @Test
  public void testGetISDCode() throws Exception {
    String code = controller.getISDCodeByCounty("US");
    Assert.assertEquals("1", code);
  }

  @Test
  public void testverifyPassword() {

    Map<String, Object> Hashmap = controller.verifyPassword("Portal123#", new MockHttpServletRequest());
    Assert.assertTrue(Hashmap.containsKey("apiKey"));
    Assert.assertTrue(Hashmap.containsKey("secretKey"));
    Assert.assertTrue(Hashmap.containsKey("success"));
    Assert.assertNotNull(Hashmap.get("apiKey"));
    Assert.assertNotNull(Hashmap.get("secretKey"));
    Assert.assertEquals(Hashmap.get("success"), true);

  }

  @Test
  public void testcreateSpendAlertSubscription() {
    String subscription = controller.createSpendAlertSubscription(map);
    Assert.assertNotNull(subscription);
    Assert.assertEquals(subscription, new String("users.subscriptions.create"));
    Assert.assertTrue(map.containsAttribute("subscriptionForm"));
    Assert.assertTrue(map.containsAttribute("currency"));
    Assert.assertNotNull(map.get("subscriptionForm"));
    Assert.assertEquals(map.get("currency").toString(), new String(
        "CurrencyValue [active=true, currencyCode=USD, currencyName=US Dollar, rank=2]"));

  }

  @Test
  public void testdeactivate() {
    //Case 1: user is active
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

    //Case 2: user is not active
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
    //Case 1: when the user is not active
    User user = userService.getUserByParam("id", "3", false);
    user.setEnabled(false);
    user.setLocked(true);
    userService.save(user);
    String view = controller.Activate(user.getParam(), "ajax", map);
    Assert.assertNotNull(view);
    Assert.assertEquals(view, new String("user.show"));
    Assert.assertEquals(user.isEnabled(), false);
    Assert.assertEquals(user.isLocked(), true);
    Assert.assertEquals(user.getFailedLoginAttempts(), 0);
    Assert.assertTrue(map.containsAttribute("user"));
    Assert.assertTrue(map.containsAttribute("profileName"));
    Assert.assertEquals(user, map.get("user"));
    Assert.assertEquals(map.get("profileName"), user.getProfile().getName());
    
    //Case 2: when the user is active
    user = userService.getUserByParam("id", "4", false);
    user.setEnabled(true);
    user.setLocked(false);
    userService.save(user);
    view = controller.Activate(user.getParam(), "ajax", map);
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
    String view = controller.resendVerificationail(user.getParam(), "ajax", map);
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
    String timeZone = controller.getUserTimezoneOffset(tenant, tenant.getParam(), request);
    Assert.assertEquals(timeZone, new String("5.50"));
    tenant1.getOwner().setTimeZone(null);
    service.save(tenant1);
    request.setAttribute("isSurrogatedTenant", true);
    request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant1);
    timeZone = controller.getUserTimezoneOffset(tenant, tenant1.getParam(), request);
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
  public void testLogin() throws ConnectorManagementServiceException {
    Tenant tenant = service.getTenantByParam("id", "2", false);
    String login = controller.login(tenant, tenant.getParam(), "Test", null, map);
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
    form.setUser((citrix.cpbm.access.User)CustomProxy.newInstance(user));
    MockHttpServletRequest request = new MockHttpServletRequest();
    BindingResult bindingResult = validate(form);
    Map<String, String> HashMap = controller.editPrefs(form, bindingResult, VALID_TIMEZONE, Locale.ENGLISH.toString(), map, request);
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
  public void testlistUsersPerTenantWithPagination() {
    List<String> err = new ArrayList<String>();
    err.add("JnuitTest");
    List<String> errMsgList = new ArrayList<String>();
    errMsgList.add("errorMsgList");
    session.setAttribute("errormsg", "true");
    session.setAttribute("globalErrors", err);
    session.setAttribute("errorMsgList", errMsgList);
    Tenant tenant = service.getTenantByParam("id", "2", false);
    String listUsers = controller.listUsersPerTenantWithPagination(tenant, "1", "1", null, "All", true,
        tenant.getParam(), map, session);
    Assert.assertEquals(listUsers, new String("users1.list"));
    Assert.assertEquals(map.get("isShowingAll"), true);
    Assert.assertEquals(map.get("tenant"), tenant);
    Assert.assertNotNull(map.get("users"));
    Assert.assertEquals(map.get("size"), 1);
    Assert.assertEquals(map.get("globalErrors"), err);
    Assert.assertEquals(map.get("errorMsgList"), errMsgList);
    Assert.assertEquals(map.get("errormsg"), new String("true"));
  }

}
