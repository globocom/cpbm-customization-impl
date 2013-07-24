/* Copyright (C) 2011 Cloud.com, Inc. All rights reserved. */
package fragment.web;

import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.ui.ModelMap;

import web.WebTestsBase;
import web.support.DispatcherTestServlet;


import citrix.cpbm.portal.fragment.controllers.AuthenticationController;

import com.vmops.event.PasswordResetRequest;
import com.vmops.event.PortalEvent;
import com.vmops.model.Tenant;
import com.vmops.model.User;
import com.vmops.model.UserAlertPreferences;
import com.vmops.model.UserAlertPreferences.AlertType;
import com.vmops.portal.config.Configuration;
import com.vmops.portal.config.Configuration.Names;
import com.vmops.service.ConfigurationService;
import com.vmops.service.UserAlertPreferencesService;
import com.vmops.service.exceptions.NoSuchUserException;
import com.vmops.service.exceptions.UserAuthorizationInvalidException;

public class AuthenticationControllerTest extends WebTestsBase {

  @Autowired
  AuthenticationController controller;

  @Autowired
  Configuration config;

  @Autowired
  ConfigurationService configurationService;

  ModelMap map;

  private MockHttpSession session;

  @Autowired
  private UserAlertPreferencesService userAlertPreferencesService;

  @BeforeClass
  public static void initMail() {
    setupMail();
  }

  @Before
  public void init() {
    map = new ModelMap();
    session = new MockHttpSession();
    asAnonymous();
  }

  @Test
  public void testLandingRouting() throws Exception {
    logger.debug("Testing routing....");
    DispatcherTestServlet servlet = this.getServletInstance();
    Method expected = locateMethod(controller.getClass(), "login", new Class[] {
        HttpServletRequest.class, ModelMap.class, HttpSession.class
    });
    Method handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/login"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controller.getClass(), "loggedout", new Class[] {
        java.lang.String.class, ModelMap.class, HttpSession.class, HttpServletResponse.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/userParam/loggedout"));
    Assert.assertEquals(expected, handler);

    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/reset_password");

    expected = locateMethod(controller.getClass(), "requestReset", new Class[] {
      ModelMap.class,
    });
    handler = servlet.recognize(request);
    Assert.assertEquals(expected, handler);

    request.removeAllParameters();
    request.addParameter("username", "value");
    request.setMethod(HttpMethod.POST.name());
    expected = locateMethod(controller.getClass(), "requestReset", new Class[] {
        String.class, HttpServletRequest.class, ModelMap.class
    });
    handler = servlet.recognize(request);
    Assert.assertEquals(expected, handler);

    request.removeAllParameters();
    request.setMethod(HttpMethod.GET.name());
    request.addParameter("a", "value");
    request.addParameter("t", "0");
    request.addParameter("i", "value");
    expected = locateMethod(controller.getClass(), "reset", new Class[] {
        String.class, Long.TYPE, String.class, HttpSession.class, ModelMap.class
    });
    handler = servlet.recognize(request);
    Assert.assertEquals(expected, handler);

    request.removeAllParameters();
    request.addParameter("password", "password");
    request.setMethod(HttpMethod.POST.name());
    expected = locateMethod(controller.getClass(), "reset", new Class[] {
        String.class, HttpSession.class
    });
    handler = servlet.recognize(request);
    Assert.assertEquals(expected, handler);

    request.removeAllParameters();
    expected = locateMethod(controller.getClass(), "requestCall", new Class[] {
        String.class, String.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/requestCallByUser"));
    Assert.assertEquals(expected, handler);

    request.removeAllParameters();
    expected = locateMethod(controller.getClass(), "requestSMS", new Class[] {
        String.class, String.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/requestSMSByUser"));
    Assert.assertEquals(expected, handler);

    request.removeAllParameters();
    expected = locateMethod(controller.getClass(), "verifyAdditionalEmail", new Class[] {
        String.class, String.class, Long.TYPE, HttpServletRequest.class, ModelMap.class, HttpSession.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/verify_additional_email"));
    Assert.assertEquals(expected, handler);
  }

  @Test
  public void testLogin() throws Exception {
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/login");
    String actualResult = controller.login(request, map, new MockHttpSession());
    if (config.getAuthenticationService().compareToIgnoreCase("cas") == 0) {
      Assert.assertEquals(
          "redirect:" + config.getCasLoginUrl() + "?service=" + URLEncoder.encode(config.getCasServiceUrl(), "UTF-8"),
          actualResult);
    } else {
      Assert.assertEquals("auth.login", actualResult);
    }
  }

  @Test
  public void testLoginFailed() throws Exception {
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/login");
    request.setParameter("login_failed", "");
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(UsernamePasswordAuthenticationFilter.SPRING_SECURITY_LAST_USERNAME_KEY, "someuser");
    session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new DisabledException("Account is disabled"));
    String actualResult = controller.login(request, map, session);
    if (config.getAuthenticationService().compareToIgnoreCase("cas") == 0) {
      Assert.assertEquals(
          "redirect:" + config.getCasLoginUrl() + "?service=" + URLEncoder.encode(config.getCasServiceUrl(), "UTF-8"),
          actualResult);
    } else {
      Assert.assertEquals("auth.login", actualResult);
    }
    Assert.assertTrue((Boolean) map.get("loginFailed"));
    Assert.assertEquals("someuser", map.get("lastUser"));
    Assert.assertNotNull(map.get("error"));
  }

  @Test
  public void testLoginFailedDisabled() throws Exception {
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/login");
    request.setParameter("login_failed", "");
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(UsernamePasswordAuthenticationFilter.SPRING_SECURITY_LAST_USERNAME_KEY, "someuser");
    session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new DisabledException("Account is disabled"));
    String actualResult = controller.login(request, map, session);
    if (config.getAuthenticationService().compareToIgnoreCase("cas") == 0) {
      Assert.assertEquals(
          "redirect:" + config.getCasLoginUrl() + "?service=" + URLEncoder.encode(config.getCasServiceUrl(), "UTF-8"),
          actualResult);
    } else {
      Assert.assertEquals("auth.login", actualResult);
    }
    String message = (String) map.get("error");
    Assert.assertNotNull(message);
    Assert.assertThat(message, JUnitMatchers.containsString("Username or password incorrect."));
  }

  @Test
  public void testLoginFailedLocked() throws Exception {
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/login");
    request.setParameter("login_failed", "");
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(UsernamePasswordAuthenticationFilter.SPRING_SECURITY_LAST_USERNAME_KEY, "someuser");
    session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new LockedException("Account is locked"));
    String actualResult = controller.login(request, map, session);
    if (config.getAuthenticationService().compareToIgnoreCase("cas") == 0) {
      Assert.assertEquals(
          "redirect:" + config.getCasLoginUrl() + "?service=" + URLEncoder.encode(config.getCasServiceUrl(), "UTF-8"),
          actualResult);
    } else {
      Assert.assertEquals("auth.login", actualResult);
    }
    String message = (String) map.get("error");
    Assert.assertNotNull(message);
    Assert.assertThat(message, JUnitMatchers.containsString("locked"));
  }

  @Test
  public void testLoginFailedAuth() throws Exception {
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/login");
    request.setParameter("login_failed", "");
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(UsernamePasswordAuthenticationFilter.SPRING_SECURITY_LAST_USERNAME_KEY, "someuser");
    session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new BadCredentialsException("Bad creds"));
    String actualResult = controller.login(request, map, session);
    if (config.getAuthenticationService().compareToIgnoreCase("cas") == 0) {
      Assert.assertEquals(
          "redirect:" + config.getCasLoginUrl() + "?service=" + URLEncoder.encode(config.getCasServiceUrl(), "UTF-8"),
          actualResult);
    } else {
      Assert.assertEquals("auth.login", actualResult);
    }
    String message = (String) map.get("error");
    Assert.assertNotNull(message);
    Assert.assertThat(message, JUnitMatchers.containsString("Username or password incorrect."));
  }

  @Test
  public void testLoginFailedOther() throws Exception {
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/login");
    request.setParameter("login_failed", "");
    MockHttpSession session = new MockHttpSession();
    session.setAttribute(UsernamePasswordAuthenticationFilter.SPRING_SECURITY_LAST_USERNAME_KEY, "someuser");
    session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, new Exception());
    String actualResult = controller.login(request, map, session);
    if (config.getAuthenticationService().compareToIgnoreCase("cas") == 0) {
      Assert.assertEquals(
          "redirect:" + config.getCasLoginUrl() + "?service=" + URLEncoder.encode(config.getCasServiceUrl(), "UTF-8"),
          actualResult);
    } else {
      Assert.assertEquals("auth.login", actualResult);
    }
    String message = (String) map.get("error");
    Assert.assertNotNull(message);
    Assert.assertThat(message, JUnitMatchers.containsString("Unknown error"));
  }

  @Test
  public void testLoginAfterLogout() throws Exception {
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/login");
    request.setParameter("logout", "");
    String actualResult = controller.login(request, map, new MockHttpSession());
    if (config.getAuthenticationService().compareToIgnoreCase("cas") == 0) {
      Assert.assertEquals(
          "redirect:" + config.getCasLoginUrl() + "?service=" + URLEncoder.encode(config.getCasServiceUrl(), "UTF-8"),
          actualResult);
    } else {
      Assert.assertEquals("auth.login", actualResult);
    }
    boolean message = (Boolean) map.get("logout");
    Assert.assertTrue(message);
  }

  @Test
  public void testLoggedOut() throws Exception {
    asRoot();
    User user = getSystemTenant().getOwner();
    MockHttpServletResponse response = new MockHttpServletResponse();
    String view = controller.loggedout(user.getUuid(), map, session, response);
    Cookie cookie = response.getCookie("JforumSSO");
    Assert.assertEquals(cookie.getValue(), "");
    if (config.getAuthenticationService().compareToIgnoreCase("cas") == 0) {
      Assert.assertEquals(
          "redirect:" + config.getCasLogoutUrl() + "?service=" + URLEncoder.encode(config.getCasServiceUrl(), "UTF-8"),
          view);
    } else {
      Assert.assertEquals("auth.loggedout", view);
    }

  }

  @SuppressWarnings("unchecked")
  @Test
  public void testLoginWithSuffixList() throws Exception {
    asRoot();
    Tenant defaultTenant = getDefaultTenant();
    defaultTenant.setUsernameSuffix("test");
    tenantService.save(defaultTenant);

    com.vmops.model.Configuration configuration = configurationService
        .locateConfigurationByName(Names.com_citrix_cpbm_username_duplicate_allowed);
    configuration.setValue("true");
    configurationService.update(configuration);

    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/login");
    String actualResult = controller.login(request, map, new MockHttpSession());
    if (config.getAuthenticationService().compareToIgnoreCase("cas") == 0) {
      Assert.assertEquals(
          "redirect:" + config.getCasLoginUrl() + "?service=" + URLEncoder.encode(config.getCasServiceUrl(), "UTF-8"),
          actualResult);
    } else {
      Assert.assertEquals("auth.login", actualResult);
    }

    List<String> suffixList = (List<String>) map.get("suffixList");
    Assert.assertEquals(1, suffixList.size());

    defaultTenant = getDefaultTenant();
    defaultTenant.setUsernameSuffix(null);
    tenantService.save(defaultTenant);

  }

  @SuppressWarnings("unchecked")
  @Test
  public void testLoggedOutWithSuffix() throws Exception {
    asRoot();
    Tenant defaultTenant = getDefaultTenant();
    defaultTenant.setUsernameSuffix("test");
    tenantService.save(defaultTenant);

    com.vmops.model.Configuration configuration = configurationService
        .locateConfigurationByName(Names.com_citrix_cpbm_username_duplicate_allowed);
    configuration.setValue("true");
    configurationService.update(configuration);

    User user = getSystemTenant().getOwner();
    MockHttpServletResponse response = new MockHttpServletResponse();
    String view = controller.loggedout(user.getUuid(), map, session, response);
    Cookie cookie = response.getCookie("JforumSSO");
    Assert.assertEquals(cookie.getValue(), "");
    if (config.getAuthenticationService().compareToIgnoreCase("cas") == 0) {
      Assert.assertEquals(
          "redirect:" + config.getCasLogoutUrl() + "?service=" + URLEncoder.encode(config.getCasServiceUrl(), "UTF-8"),
          view);
    } else {
      Assert.assertEquals("auth.loggedout", view);
    }

    List<String> suffixList = (List<String>) map.get("suffixList");
    Assert.assertEquals(1, suffixList.size());

    defaultTenant = getDefaultTenant();
    defaultTenant.setUsernameSuffix(null);
    tenantService.save(defaultTenant);

  }

  @Test
  public void testPasswordResetRequest() throws Exception {
    String view = controller.requestReset(map);
    Assert.assertEquals("auth.request_reset", view);
  }

  @Test
  public void testPasswordResetRequestSubmit() throws Exception {
    User user = userDAO.find(2L);
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/portal/reset_password");
    request.setServletPath("/portal");
    request.setContextPath("/portal");
    request.setLocalPort(8080);
    String view = controller.requestReset(user.getUsername(), request, new ModelMap());
    Assert.assertEquals("auth.request_reset_success", view);
    Assert.assertEquals(1, eventListener.getEvents().size());
    PortalEvent event = eventListener.getEvents().get(0);
    Assert.assertTrue(event.getPayload() instanceof PasswordResetRequest);
    Assert.assertEquals(user.getUsername(), ((PasswordResetRequest) event.getPayload()).getUsername());
    Assert.assertEquals(user, event.getSource());
  }

  @Test
  public void testPasswordResetRequestSubmitUnverifiedUser() throws Exception {
    asRoot();
    User user = createTestUserInTenant(getDefaultTenant());
    eventListener.clear();
    asAnonymous();
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/portal/reset_password");
    request.setServletPath("/portal");
    request.setContextPath("/portal");
    request.setLocalPort(8080);
    String view = controller.requestReset(user.getUsername(), request, new ModelMap());
    Assert.assertEquals("auth.request_reset_success", view);
    Assert.assertEquals(0, eventListener.getEvents().size());
  }

  @Test
  public void testReset() {
    MockHttpSession session = new MockHttpSession();
    User user = userDAO.find(3L);
    long genTime = System.currentTimeMillis();
    String auth = user.getAuthorization(genTime);
    String view = controller.reset(auth, genTime, user.getParam(), session, map);
    Assert.assertEquals("auth.reset", view);
    Assert.assertNotNull(session.getAttribute(AuthenticationController.RESET_USER_KEY));
    Assert.assertEquals(user.getUsername(), session.getAttribute(AuthenticationController.RESET_USER_KEY));
  }

  @Test(expected = UserAuthorizationInvalidException.class)
  public void testBadReset() {
    MockHttpSession session = new MockHttpSession();
    User user = userDAO.find(3L);
    long genTime = System.currentTimeMillis();
    String auth = user.getAuthorization(genTime);
    String view = controller.reset(auth, genTime - 100, user.getParam(), session, map);
    Assert.assertEquals("auth.reset", view);
    Assert.assertNotNull(session.getAttribute(AuthenticationController.RESET_USER_KEY));
    Assert.assertEquals(user.getUsername(), session.getAttribute(AuthenticationController.RESET_USER_KEY));
  }

  @Test
  public void testResetPassword() {
    MockHttpSession session = new MockHttpSession();
    User user = userDAO.find(3L);
    session.setAttribute(AuthenticationController.RESET_USER_KEY, user.getUsername());
    String newpass = "newPassw0rd";
    String view = controller.reset(newpass, session);
    Assert.assertEquals("redirect:/portal/login", view);
    userDAO.flush();
    userDAO.refresh(user);
    Assert.assertTrue(user.authenticate(newpass));
  }

  @Test(expected = NoSuchUserException.class)
  public void testBadResetPassword() {
    MockHttpSession session = new MockHttpSession();
    String newpass = "newPassw0rd";
    controller.reset(newpass, session);
  }

  @Test
  public void testVerifyAdditionalEmail() {
    asRoot();
    User user = createTestUserInTenant(getDefaultTenant());
    UserAlertPreferences uap = userAlertPreferencesService.createUserAlertPreference(user, "test@test123.com",
        AlertType.USER_ALERT_EMAIL);
    controller.verifyAdditionalEmail(user.getAuthorization(0), user.getParam(), uap.getId(),
        getRequestTemplate(HttpMethod.GET, "/verify_additional_email"), map, session);
    UserAlertPreferences uapSaved = userAlertPreferencesService.locateUserAlertPreference(uap.getId());
    Assert.assertTrue(uapSaved.isEmailVerified());
  }

  @Test
  public void testVerifyAdditionalEmailWithoutLogin() {
    asRoot();
    User user = createTestUserInTenant(getDefaultTenant());
    UserAlertPreferences uap = userAlertPreferencesService.createUserAlertPreference(user, "test@test123.com",
        AlertType.USER_ALERT_EMAIL);
    asAnonymous();
    controller.verifyAdditionalEmail(user.getAuthorization(0), user.getParam(), uap.getId(),
        getRequestTemplate(HttpMethod.GET, "/verify_additional_email"), map, session);
    UserAlertPreferences uapSaved = userAlertPreferencesService.locateUserAlertPreference(uap.getId());
    Assert.assertTrue(uapSaved.isEmailVerified());
  }

  @Test(expected = UserAuthorizationInvalidException.class)
  public void testVerifyAdditionalEmailInvalidAuthorization() {
    asRoot();
    User user = createTestUserInTenant(getDefaultTenant());
    UserAlertPreferences uap = userAlertPreferencesService.createUserAlertPreference(user, "test@test1234.com",
        AlertType.USER_ALERT_EMAIL);
    controller.verifyAdditionalEmail(user.getAuthorization(1), user.getParam(), uap.getId(),
        getRequestTemplate(HttpMethod.GET, "/verify_additional_email"), map, session);
    userAlertPreferencesService.locateUserAlertPreference(uap.getId());
  }
}