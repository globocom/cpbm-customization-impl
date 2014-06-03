/*
 * Copyright © 2013 Citrix Systems, Inc. You may not use, copy, or modify this file except pursuant to a valid license
 * agreement from Citrix Systems, Inc.
 */
package fragment.web;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.handler.DispatcherServletWebRequest;

import web.WebTestsBaseWithMockConnectors;
import web.support.DispatcherTestServlet;
import web.support.MockSessionStatus;

import com.citrix.cpbm.access.proxy.CustomProxy;
import com.citrix.cpbm.core.workflow.service.BusinessTransactionService;
import com.citrix.cpbm.core.workflow.service.TaskService;
import com.citrix.cpbm.platform.admin.service.utils.ServiceInstanceConfiguration;
import com.citrix.cpbm.platform.bootstrap.service.BootstrapActivator;
import com.citrix.cpbm.platform.spi.AccountLifecycleHandler;
import com.citrix.cpbm.platform.spi.CloudConnector;
import com.citrix.cpbm.platform.spi.UserLifecycleHandler;
import com.citrix.cpbm.portal.forms.UserRegistration;
import com.citrix.cpbm.portal.fragment.controllers.RegistrationController;
import com.citrix.cpbm.portal.fragment.controllers.TasksController;
import com.vmops.event.EmailVerified;
import com.vmops.event.PortalEvent;
import com.vmops.event.TenantActivation;
import com.vmops.event.VerifyEmailRequest;
import com.vmops.internal.service.PaymentGatewayService;
import com.vmops.model.AccountType;
import com.vmops.model.Address;
import com.vmops.model.CampaignPromotion;
import com.vmops.model.CampaignPromotionsInChannels;
import com.vmops.model.Channel;
import com.vmops.model.Configuration;
import com.vmops.model.Country;
import com.vmops.model.ProductBundleRevision;
import com.vmops.model.PromotionSignup;
import com.vmops.model.PromotionToken;
import com.vmops.model.Service;
import com.vmops.model.Tenant;
import com.vmops.model.User;
import com.vmops.model.billing.PaymentTransaction;
import com.vmops.model.billing.PaymentTransaction.State;
import com.vmops.persistence.AccountTypeDAO;
import com.vmops.persistence.CampaignPromotionDAO;
import com.vmops.persistence.PromotionTokenDAO;
import com.vmops.portal.config.Configuration.Names;
import com.vmops.service.AuthorityService;
import com.vmops.service.ChannelService;
import com.vmops.service.ConfigurationService;
import com.vmops.service.exceptions.UserAuthorizationInvalidException;
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.forms.CreditCardType;
import common.MockCloudInstance;

public class RegistrationControllerTest extends WebTestsBaseWithMockConnectors {

  private ModelMap map;

  private MockSessionStatus status;

  private MockHttpServletRequest request;

  private MockHttpSession session;

  @Autowired
  private ChannelService channelService2;

  @Autowired
  private RegistrationController controller;

  @JsonProperty
  private com.citrix.cpbm.access.Tenant tenant;

  @Autowired
  AccountTypeDAO accountTypeDAO;

  @Autowired
  private CampaignPromotionDAO cmpdao;

  @Autowired
  private PromotionTokenDAO tokendao;

  @Autowired
  private ConfigurationService configurationService;

  @Autowired
  private BusinessTransactionService businessTransactionService;

  @Autowired
  ChannelService channelservice;

  @Autowired
  CampaignPromotionDAO campaignpromotiondao;

  private BootstrapActivator bootstrapActivator = new BootstrapActivator();

  private static boolean isMockInstanceCreated = false;

  private PaymentGatewayService ossConnector = null;

  @Autowired
  private TasksController tasksController;

  @Autowired
  private AuthorityService authorityService;

  @Autowired
  private TaskService taskService;

  @Before
  public void init() throws Exception {
    map = new ModelMap();
    status = new MockSessionStatus();
    request = new MockHttpServletRequest();
    session = new MockHttpSession();
    request.setSession(session);
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
    MockCloudInstance instance = getMockCloudInstance();
    CloudConnector connector = instance.getCloudConnector();
    ossConnector = EasyMock.createMock(PaymentGatewayService.class);
    mockAccountLifecycleHandler = EasyMock.createMock(AccountLifecycleHandler.class);
    mockUserLifecycleHandler = EasyMock.createMock(UserLifecycleHandler.class);
    EasyMock.expect(connector.getServiceInstanceUUID()).andReturn("12345-786").anyTimes();
    EasyMock.replay(connector);
    ServiceInstanceConfiguration sic2 = connector.getServiceInstanceConfiguration();
    EasyMock.reset(ossConnector);
    EasyMock.expect(ossConnector.getAccountLifeCycleHandler()).andReturn(mockAccountLifecycleHandler).anyTimes();
    EasyMock.expect(ossConnector.getUserLifeCycleHandler()).andReturn(mockUserLifecycleHandler).anyTimes();
    EasyMock.expect(sic2.getInstanceUUID()).andReturn("003fa8ee-fba3-467f-a517-ed806dae8a80").anyTimes();
    final Capture<BigDecimal> amount = new Capture<BigDecimal>();
    EasyMock.expect(ossConnector.authorize(EasyMock.anyObject(Tenant.class), EasyMock.capture(amount)))
        .andAnswer(new IAnswer<PaymentTransaction>() {

          @Override
          public PaymentTransaction answer() throws Throwable {
            return new PaymentTransaction(new Tenant(), 0, State.COMPLETED,
                com.vmops.model.billing.PaymentTransaction.Type.CAPTURE);
          }
        }).anyTimes();
    EasyMock.replay(ossConnector);
    EasyMock.replay(sic2);
  }

  @SuppressWarnings({
    "rawtypes"
  })
  @Test
  public void testRouting() throws Exception {
    logger.debug("Testing routing....");
    DispatcherTestServlet servlet = this.getServletInstance();
    Class controllerClass = RegistrationController.class;
    Method expected = locateMethod(controllerClass, "signupStep1", new Class[] {
        ModelMap.class, HttpServletRequest.class
    });
    Method handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/account_type"));

    expected = locateMethod(controllerClass, "register", new Class[] {
        UserRegistration.class, BindingResult.class, String.class, String.class, ModelMap.class, String.class,
        SessionStatus.class, HttpServletRequest.class
    });

    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/register"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "validateUsername", new Class[] {
      String.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/validate_username"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "updatePasswordAndVerifyEmail", new Class[] {
        String.class, String.class, HttpServletRequest.class, ModelMap.class, HttpSession.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/verify_user"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "verifyPhoneVerificationPIN", new Class[] {
        String.class, String.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/phoneverification/verify_pin"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "verifyEmail", new Class[] {
        HttpServletRequest.class, ModelMap.class, HttpSession.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/verify_email"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "requestCall", new Class[] {
        String.class, String.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/request_call"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "requestSMS", new Class[] {
        String.class, String.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/request_sms"));
    Assert.assertEquals(expected, handler);
  }

  @Test
  public void testSelects() throws Exception {
    List<String> countryNames = Arrays.asList("USA", "SINGAPORE", "INDIA");
    List<String> ccNames = Arrays.asList("AMEX", "VISA", "MASTERCARD", "DISCOVER");

    UserRegistration registration = new UserRegistration();
    registration.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));

    /*
     * for (AccountType disposition : registration.getDisposition()) { dispNames.contains(disposition.getName()); }
     */
    for (Country country : registration.getCountryList()) {
      countryNames.contains(country.getName());
    }
    for (CreditCardType card : registration.getCcTypes()) {
      ccNames.contains(card.getName());
    }

  }

  @Test
  public void testRegister() {
    try {
      UserRegistration registration = new UserRegistration();
      registration.setAcceptedTerms(true);
      AccountType disposition = accountTypeDAO.getTrialAccountType();
      setupRegistration(disposition, registration);
      MockHttpServletRequest request = new MockHttpServletRequest();
      DispatcherServletWebRequest webRequest = new DispatcherServletWebRequest(request);
      RequestContextHolder.setRequestAttributes(webRequest);
      String view = controller.signup(map, null, null, "3", status, new MockHttpServletRequest());
      Assert.assertEquals("register.userinfo", view);
      Assert.assertTrue(map.containsKey("registration"));
      UserRegistration userRegistration = (UserRegistration) map.get("registration");
      Assert.assertNotNull("Account Type Id Should not be null ", userRegistration.getAccountTypeId());
    } catch (Exception e) {
      Assert.fail();

    }
  }

  private void beforeRegisterCall(MockHttpServletRequest request, UserRegistration registration) {
    DispatcherServletWebRequest webRequest = new DispatcherServletWebRequest(request);
    RequestContextHolder.setRequestAttributes(webRequest);
    request.getSession().setAttribute("phoneVerificationPin", "12345");
    request.getSession().setAttribute("phoneNumber", "919591241025");
    Tenant tenant1 = new Tenant("New Co", accountTypeDAO.getDefaultRegistrationAccountType(), null, randomAddress(),
        true, currencyValueService.locateBYCurrencyCode("USD"), null);
    registration.setTenant((com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(tenant1));
    registration.setUserEnteredPhoneVerificationPin("12345");
  }

  @Test
  public void testRegisterDefault() throws Exception {
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/portal/register");
    UserRegistration registration = new UserRegistration();

    registration.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    registration.setAcceptedTerms(true);
    registration.setAllowSecondary(true);
    registration.setSecondaryAddress(new Address("steve", "creek", "cupertino", "CHAN", "95014", "IN"));
    AccountType disposition = accountTypeDAO.getDefaultRegistrationAccountType();
    BindingResult result = setupRegistration(disposition, registration);

    beforeRegisterCall(request, registration);
    String view = controller.register(registration, result, "abc", "abc", map, null, status, request);
    Assert.assertEquals("register.registration_success", view);
    Assert.assertTrue(status.isComplete());
    verifyRegistration(disposition, registration.getUser(), registration.getTenant());
    Assert.assertEquals(registration.getTenant().getObject(),
        ((com.citrix.cpbm.access.Tenant) map.get("tenant")).getObject());

  }

  @Test
  public void testRegisterTrial() throws Exception {
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/portal/signup?pc=TESTPROMOCODE");
    UserRegistration registration = new UserRegistration();
    registration.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));

    registration.setAcceptedTerms(true);
    AccountType disposition = accountTypeDAO.getTrialAccountType();
    BindingResult result = setupRegistration(disposition, registration);
    PromotionSignup promotionSignup = new PromotionSignup("test" + random.nextInt(), "Citrix",
        "PromotionSignUp@citrix.com");
    promotionSignup.setCreateBy(getRootUser());
    promotionSignup.setCurrency(Currency.getInstance("USD"));
    promotionSignup.setPhone("9999999999");

    CampaignPromotion campaignPromotion = new CampaignPromotion();
    campaignPromotion.setCode("USD" + random.nextInt());
    campaignPromotion.setCreateBy(getRootUser());
    campaignPromotion.setTrial(true);
    campaignPromotion.setUpdateBy(getRootUser());

    CampaignPromotionsInChannels cpic = new CampaignPromotionsInChannels(campaignPromotion,
        channelservice.getDefaultServiceProviderChannel());
    campaignPromotion.getCampaignPromotionsInChannels().add(cpic);
    cmpdao.save(campaignPromotion);

    PromotionToken promotionToken = new PromotionToken(campaignPromotion, "TESTPROMOCODE");
    promotionToken.setCreateBy(getRootUser());
    tokendao.save(promotionToken);

    promotionSignup.setPromotionToken(promotionToken);

    registration.setTrialCode("TESTPROMOCODE");

    DispatcherServletWebRequest webRequest = new DispatcherServletWebRequest(request);
    RequestContextHolder.setRequestAttributes(webRequest);
    request.getSession().setAttribute("phoneVerificationPin", "12345");
    request.getSession().setAttribute("phoneNumber", "919591241025");
    registration.setUserEnteredPhoneVerificationPin("12345");

    String view = controller.register(registration, result, "abc", "abc", map, null, status, request);
    Assert.assertEquals("register.registration_success", view);
    Assert.assertTrue(status.isComplete());
    verifyRegistration(disposition, registration.getUser(), registration.getTenant());
    Assert.assertEquals(registration.getTenant().getAccountId(),
        ((com.citrix.cpbm.access.Tenant) map.get("tenant")).getAccountId());
  }

  @Test
  public void testValidateSuffix() throws Exception {
    Assert.assertEquals("true", controller.validateSuffix("test"));
    asRoot();
    Tenant defaultTenant = getDefaultTenant();
    defaultTenant.setUsernameSuffix("test");
    tenantService.update(defaultTenant);
    Assert.assertEquals("false", controller.validateSuffix("test"));
  }

  @Test
  public void testRegisterBindingHasErrors() throws Exception {
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/portal/register");
    request.setRemoteAddr("1.1.1.1");
    User user = new User("test", "test", "testtest.com", "testuser", VALID_PASSWORD, VALID_PHONE, VALID_TIMEZONE, null,
        null, getRootUser());
    user.setAddress(randomAddress());
    Tenant tenant = new Tenant("New Co", accountTypeDAO.getDefaultRegistrationAccountType(), null, randomAddress(),
        true, currencyValueService.locateBYCurrencyCode("USD"), null);
    UserRegistration registration = new UserRegistration();
    registration.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    registration.setUser((com.citrix.cpbm.access.User) CustomProxy.newInstance(user));
    registration.setTenant((com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(tenant));
    registration.setDisposition(accountTypeDAO.getDefaultRegistrationAccountType());
    BindingResult result = validate(registration);
    beforeRegisterCall(request, registration);
    String view = controller.register(registration, result, "abc", "abc", map, null, status, request);
    Assert.assertEquals("register.userinfo", view);
    Assert.assertFalse(status.isComplete());
    Assert.assertTrue(((UserRegistration) result.getTarget()).getUser().getObject().getId() == 0);
    Assert.assertTrue(((UserRegistration) result.getTarget()).getTenant().getId() == 0);
  }

  @Test
  public void testRegisterUsernameExists() throws Exception {
    User existing = userDAO.findAll(null).get(1);
    String response = controller.validateUsername(existing.getUsername());
    Assert.assertEquals("false", response);
  }

  @Test
  public void testRegisterPostCaptchaFail() throws Exception {
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/portal/signup");
    request.setRemoteAddr("1.1.1.1");
    User user = new User("test", "test", "testtest.com", "testuser", VALID_PASSWORD, VALID_PHONE, VALID_TIMEZONE, null,
        null, getRootUser());
    user.setAddress(randomAddress());
    Tenant tenant = new Tenant("New Co", accountTypeDAO.getDefaultRegistrationAccountType(), null, randomAddress(),
        true, currencyValueService.locateBYCurrencyCode("USD"), null);
    UserRegistration registration = new UserRegistration();
    registration.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    registration.setUser((com.citrix.cpbm.access.User) CustomProxy.newInstance(user));
    registration.setTenant((com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(tenant));
    BindingResult result = new BindException(registration, "registration");
    beforeRegisterCall(request, registration);
    String view = controller.register(registration, result, "abc", "CAPTCHA_FAIL", map, "1", status, request);
    Assert.assertEquals("register.moreuserinfo", view);
    Assert.assertFalse(status.isComplete());
    Assert.assertTrue(result.hasGlobalErrors());
    Assert.assertTrue(result.getGlobalErrorCount() == 1);
    Assert.assertEquals("errors.registration.captcha", result.getGlobalError().getCode());
    Assert.assertEquals("captcha.error", map.get("registrationError"));
  }

  @Test
  public void testRegisterTrialForInvalidPromoCode() throws Exception {
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/portal/signup?pc=testpromo");
    UserRegistration registration = new UserRegistration();
    registration.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    registration.setAcceptedTerms(true);
    AccountType disposition = accountTypeDAO.getTrialAccountType();
    BindingResult result = setupRegistration(disposition, registration);
    PromotionSignup promotionSignup = new PromotionSignup("test" + random.nextInt(), "Citrix",
        "PromotionSignUp@citrix.com");
    promotionSignup.setCreateBy(getRootUser());
    promotionSignup.setCurrency(Currency.getInstance("USD"));
    promotionSignup.setPhone("9999999999");

    CampaignPromotion campaignPromotion = new CampaignPromotion();
    campaignPromotion.setCode("USD" + random.nextInt());
    campaignPromotion.setCreateBy(getRootUser());
    campaignPromotion.setTrial(true);
    campaignPromotion.setUpdateBy(getRootUser());
    campaignpromotiondao.save(campaignPromotion);

    PromotionToken promotionToken = new PromotionToken(campaignPromotion, "TESTPROMOCODE");
    promotionToken.setCreateBy(getRootUser());
    tokendao.save(promotionToken);

    promotionSignup.setPromotionToken(promotionToken);
    registration.setTrialCode("testpromo");
    beforeRegisterCall(request, registration);
    String view = controller.register(registration, result, "abc", "abc", map, null, status, request);
    Assert.assertEquals("register.fail", view);
    Assert.assertFalse(status.isComplete());

  }

  @Test
  public void testRegisterNotAcceptedTerms() throws Exception {
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/portal/register");
    UserRegistration registration = new UserRegistration();
    registration.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    AccountType disposition = accountTypeDAO.getOnDemandPostPaidAccountType();
    BindingResult result = setupRegistration(disposition, registration);
    beforeRegisterCall(request, registration);
    String view = controller.register(registration, result, "abc", "abc", map, null, status, request);
    Assert.assertEquals("register.userinfo", view);
    Assert.assertFalse(status.isComplete());
    Assert.assertTrue(result.hasFieldErrors());
    Assert.assertTrue(result.getFieldErrorCount() == 1);
    Assert.assertEquals("AssertTrue", result.getFieldError("acceptedTerms").getCode());
  }

  @Test
  public void testValidateUsername() throws Exception {
    asAnonymous();
    User existing = userDAO.findAll(null).get(0);
    String response = controller.validateUsername(existing.getUsername());
    Assert.assertFalse(Boolean.parseBoolean(response));

    response = controller.validateUsername("nonexistent");
    Assert.assertTrue(Boolean.parseBoolean(response));
  }

  @Test
  public void testVerifyEmail() throws Exception {
    User user = createDisabledUser();
    String auth = user.getAuthorization(0);

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("regAuth", auth);
    session.setAttribute("regParam", user.getParam());

    String view = controller.verifyEmail(getRequestTemplate(HttpMethod.GET, "/verify_email"), map, session);
    String redirect = "redirect:/?verify";

    Assert.assertEquals(redirect, view);
    userDAO.flush();
    User found = userDAO.find(user.getId());
    Assert.assertEquals(user, found);
    Assert.assertTrue(found.isEnabled());

  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetLoginPageUIRelatedConfigs() {
    HashMap<String, Object> configs = controller.getLoginPageUIRelatedConfigs();
    Assert.assertEquals(4, configs.size());
    Assert.assertEquals("false", configs.get("isDirectoryServiceAuthenticationON"));
    Assert.assertEquals("N", configs.get("showSuffix"));
    Assert.assertEquals("true", configs.get("showSuffixDropBox"));
    List<String> suffixList = (List<String>) configs.get("suffixList");
    Assert.assertEquals(0, suffixList.size());

    Configuration configuration = configurationService
        .locateConfigurationByName(Names.com_citrix_cpbm_portal_directory_service_enabled);
    configuration.setValue("true");
    configurationService.update(configuration);

    configuration = configurationService.locateConfigurationByName(Names.com_citrix_cpbm_directory_mode);
    configuration.setValue("pull");
    configurationService.update(configuration);

    configs = controller.getLoginPageUIRelatedConfigs();
    Assert.assertEquals(4, configs.size());
    Assert.assertEquals("true", configs.get("isDirectoryServiceAuthenticationON"));
    Assert.assertEquals("N", configs.get("showSuffix"));
    Assert.assertEquals("true", configs.get("showSuffixDropBox"));
    suffixList = (List<String>) configs.get("suffixList");
    Assert.assertEquals(0, suffixList.size());

    configuration = configurationService.locateConfigurationByName(Names.com_citrix_cpbm_username_duplicate_allowed);
    configuration.setValue("Y");
    configurationService.update(configuration);

    configs = controller.getLoginPageUIRelatedConfigs();
    Assert.assertEquals(4, configs.size());
    Assert.assertEquals("true", configs.get("isDirectoryServiceAuthenticationON"));
    Assert.assertEquals("Y", configs.get("showSuffix"));
    Assert.assertEquals("true", configs.get("showSuffixDropBox"));
    suffixList = (List<String>) configs.get("suffixList");
    Assert.assertEquals(0, suffixList.size());

    configuration = configurationService
        .locateConfigurationByName(Names.com_citrix_cpbm_login_screen_tenant_suffix_dropdown_enabled);
    configuration.setValue("false");
    configurationService.update(configuration);

    configs = controller.getLoginPageUIRelatedConfigs();
    Assert.assertEquals(4, configs.size());
    Assert.assertEquals("true", configs.get("isDirectoryServiceAuthenticationON"));
    Assert.assertEquals("Y", configs.get("showSuffix"));
    Assert.assertEquals("false", configs.get("showSuffixDropBox"));
    suffixList = (List<String>) configs.get("suffixList");
    Assert.assertEquals(0, suffixList.size());
  }

  @Test
  public void testVerifyEmailWithNoPasswordSet() throws Exception {
    User user = createUserWithoutPassword();
    String auth = user.getAuthorization(0);

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("regAuth", auth);
    session.setAttribute("regParam", user.getParam());

    String view = controller.verifyEmail(getRequestTemplate(HttpMethod.GET, "/verify_email"), map, session);
    String redirect = "register.setpassword";

    Assert.assertEquals(redirect, view);
    userDAO.flush();
  }

  @Test(expected = UserAuthorizationInvalidException.class)
  public void testVerifyEmailBadAuth() throws Exception {
    User user = createDisabledUser();

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("regAuth", "garbage");
    session.setAttribute("regParam", user.getParam());

    controller.verifyEmail(getRequestTemplate(HttpMethod.GET, "/verify_email"), map, session);
  }

  @Test(expected = UserAuthorizationInvalidException.class)
  public void testVerifyEmailWrongAuth() throws Exception {
    User user = createDisabledUser();
    String auth = user.getAuthorization(1);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("regAuth", auth);
    session.setAttribute("regParam", user.getParam());
    controller.verifyEmail(getRequestTemplate(HttpMethod.GET, "/verify_email"), map, new MockHttpSession());
  }

  @Test
  public void testVerifyUserPasswordNotSet() {
    User user = createUserWithoutPassword();
    String auth = user.getAuthorization(1);
    String view = controller.updatePasswordAndVerifyEmail(auth, user.getParam(), new MockHttpServletRequest(), map,
        new MockHttpSession());
    String expectedView = "register.setpassword";
    Assert.assertEquals(view, expectedView);
  }

  @Test
  public void testVerifyUserPasswordNotSetWithDirectoryServerPullOn() {
    Configuration configuration = configurationService
        .locateConfigurationByName(Names.com_citrix_cpbm_portal_directory_service_enabled);
    configuration.setValue("true");
    configurationService.update(configuration);

    configuration = configurationService.locateConfigurationByName(Names.com_citrix_cpbm_directory_mode);
    configuration.setValue("pull");
    configurationService.update(configuration);

    User user = createUserWithoutPassword();
    String auth = user.getAuthorization(1);
    String view = controller.updatePasswordAndVerifyEmail(auth, user.getParam(), new MockHttpServletRequest(), map,
        new MockHttpSession());
    String expectedView = "redirect:/portal/verify_email";
    Assert.assertEquals(view, expectedView);
  }

  @Test
  public void testVerifyUserEmailNotVerified() {
    User user = createUserWithoutEmailVerification();
    String auth = user.getAuthorization(1);
    String view = controller.updatePasswordAndVerifyEmail(auth, user.getParam(), new MockHttpServletRequest(), map,
        new MockHttpSession());
    String expectedView = "redirect:/portal/verify_email";
    Assert.assertEquals(view, expectedView);
  }

  @Test
  public void testVerifyEnabledUser() {
    User user = createEmailVerifiedUser();
    String auth = user.getAuthorization(1);
    String view = controller.updatePasswordAndVerifyEmail(auth, user.getParam(), new MockHttpServletRequest(), map,
        new MockHttpSession());
    String expectedView = "redirect:/portal/login";
    Assert.assertEquals(view, expectedView);
  }

  private User createDisabledUser() {
    User user = new User("first", "last", "verify@verify.com", "disableduser", VALID_PASSWORD, VALID_PHONE,
        VALID_TIMEZONE, getDefaultTenant(), userProfile, getRootUser());
    userDAO.save(user);
    userDAO.flush();
    return user;
  }

  private User createUserWithoutEmailVerification() {
    User user = new User("first1", "last1", "verify1@verify.com", "disableduser", VALID_PASSWORD, VALID_PHONE,
        VALID_TIMEZONE, getDefaultTenant(), userProfile, getRootUser());
    user.setEmailVerified(false);
    userDAO.save(user);
    userDAO.flush();
    return user;
  }

  private User createUserWithoutPassword() {
    User user = new User("first2", "last2", "verify2@verify.com", "disableduser", null, VALID_PHONE, VALID_TIMEZONE,
        getDefaultTenant(), userProfile, getRootUser());
    userDAO.save(user);
    userDAO.flush();
    return user;
  }

  private User createEmailVerifiedUser() {
    User user = new User("first", "last", "verify@verify.com", "disableduser", VALID_PASSWORD, VALID_PHONE,
        VALID_TIMEZONE, getDefaultTenant(), userProfile, getRootUser());
    user.setEmailVerified(true);
    userDAO.save(user);
    userDAO.flush();
    return user;
  }

  private BindingResult setupRegistration(AccountType disposition, UserRegistration registration) throws Exception {
    String rand = "test" + random.nextInt();
    Address address = randomAddress();
    User user = new User("test", "test", rand + "@test.com", rand, VALID_PASSWORD, VALID_PHONE, VALID_TIMEZONE, null,
        null, null);
    user.setAddress(address);
    Tenant tenant = new Tenant("New Co", disposition, null, address, true,
        currencyValueService.locateBYCurrencyCode("USD"), null);
    registration.setUser((com.citrix.cpbm.access.User) CustomProxy.newInstance(user));
    registration.setTenant((com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(tenant));
    registration.setDisposition(disposition);
    registration.setAccountTypeId(disposition.getId() + "");
    BindingResult result = validate(registration);
    return result;
  }

  private void verifyRegistration(AccountType disposition, com.citrix.cpbm.access.User user,
      com.citrix.cpbm.access.Tenant tenant) {
    Assert.assertTrue(user.getObject().getId() != 0);
    Assert.assertTrue(tenant.getObject().getId() != 0);
    User foundUser = userDAO.find(user.getObject().getId());
    Assert.assertEquals(user, foundUser);
    Assert.assertEquals(getPortalUser(), user.getObject().getCreatedBy());
    Assert.assertFalse(foundUser.isEnabled());
    tenantDAO.merge(tenant.getObject());
    Tenant foundTenant = tenantDAO.find(tenant.getId());
    Assert.assertEquals(tenant, foundTenant);
    Assert.assertEquals(getPortalUser(), tenant.getObject().getCreatedBy());
    Assert.assertEquals(foundUser, tenant.getOwner());
    Assert.assertEquals(foundUser, foundTenant.getOwner());
    Assert.assertEquals(foundTenant, foundUser.getTenant());
    Assert.assertNotNull(foundTenant.getAccountId());
    Assert.assertTrue(ACCOUNT_NUMBER_PATTERN.matcher(foundTenant.getAccountId()).matches());
    Assert.assertEquals(disposition, foundTenant.getAccountType());
    Assert.assertEquals(disposition.getSupportedPaymentModes().get(0), foundTenant.getTenantExtraInformation()
        .getPaymentMode());
    Assert.assertEquals(currencyValueDAO.findByCurrencyCode("USD"), foundTenant.getCurrency());
  }

  @Test
  public void testRequestCall() throws JsonGenerationException, JsonMappingException, IOException {
    request.getSession().setAttribute("phoneVerificationPin", "12345");
    Map<String, String> keyvaluepairs = controller.requestCall("INVALIDNUMBER", "INVALIDCODE", request);

    try {
      Assert.assertTrue("failed".equalsIgnoreCase(keyvaluepairs.get("result")));

      Map<String, String> keyvaluepairs1 = controller.requestCall("123456", "91", request);
      Assert.assertTrue("success".equalsIgnoreCase(keyvaluepairs1.get("result")));
    } catch (Exception e) {

    }

    try {
      request.getSession().setAttribute("phoneVerificationPin", "ABC");
      keyvaluepairs = controller.requestCall("INVALIDNUMBER", "INVALIDCODE", request);
      Assert.assertTrue("failed".equalsIgnoreCase(keyvaluepairs.get("result")));

      Map<String, String> keyvaluepairs1 = controller.requestCall("123456", "91", request);
      Assert.assertTrue("failed".equalsIgnoreCase(keyvaluepairs1.get("result")));
    } catch (Exception e) {

    }
  }

  @Test
  public void testRequestSMS() throws JsonGenerationException, JsonMappingException, IOException {
    request.getSession().setAttribute("phoneVerificationPin", "12345");
    Map<String, String> keyvaluepairs = controller.requestSMS("INVALIDNUMBER", "INVALIDCODE", request);
    Assert.assertEquals("failed", keyvaluepairs.get("result"));

    Map<String, String> keyvaluepairs1 = controller.requestSMS("123456", "91", request);
    Assert.assertEquals("success", keyvaluepairs1.get("result"));
  }

  @Test
  public void testManualActivationFlag() throws Exception {
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/portal/register");
    UserRegistration registration = new UserRegistration();
    registration.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    registration.setAcceptedTerms(true);
    AccountType disposition = accountTypeDAO.getDefaultRegistrationAccountType();
    disposition.setManualActivation(true);
    BindingResult result = setupRegistration(disposition, registration);
    beforeRegisterCall(request, registration);
    String view = controller.register(registration, result, "abc", "abc", map, null, status, request);
    Assert.assertEquals(2, eventListener.getEvents().size());
    PortalEvent event = eventListener.getEvents().get(0);
    Assert.assertTrue(event.getPayload() instanceof VerifyEmailRequest);
    Assert.assertEquals("register.registration_success", view);
    Assert.assertTrue(status.isComplete());
    verifyRegistration(disposition, registration.getUser(), registration.getTenant());
    Assert.assertEquals(registration.getTenant().getObject(),
        ((com.citrix.cpbm.access.Tenant) map.get("tenant")).getObject());
  }

  @Test
  public void testVerifyPhoneVerificationPIN() throws Exception {
    request.getSession().setAttribute("phoneVerificationPin", "12345");
    request.getSession().setAttribute("phoneNumber", "123456789");
    String failedResult = controller.verifyPhoneVerificationPIN("54321", "123456789", request);
    Assert.assertEquals("failed", failedResult);

    String successResult = controller.verifyPhoneVerificationPIN("12345", "123456789", request);
    Assert.assertEquals("success", successResult);

  }

  @Test
  public void testEmailVerifiedEventForManualActivationAccountType() {
    // Sign up for manual activation account type
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/portal/register");
    UserRegistration registration = new UserRegistration();
    registration.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    registration.setAcceptedTerms(true);
    List<AccountType> accountTypes = accountTypeDAO.getManualRegistrationAccountTypes();
    AccountType disposition = null;
    for (AccountType accountType : accountTypes) {
      if (accountType.isManualActivation()) {
        disposition = accountType;
      }
    }
    BindingResult result = null;
    try {
      result = setupRegistration(disposition, registration);
    } catch (Exception e) {
      e.printStackTrace();
    }
    beforeRegisterCall(request, registration);
    String view = controller.register(registration, result, "abc", "abc", map, null, status, request);
    Assert.assertEquals("register.registration_success", view);
    Assert.assertTrue(status.isComplete());
    verifyRegistration(disposition, registration.getUser(), registration.getTenant());
    Assert.assertEquals(registration.getTenant().getObject(),
        ((com.citrix.cpbm.access.Tenant) map.get("tenant")).getObject());
    // account activation request event
    Assert.assertEquals(2, eventListener.getEvents().size());
    PortalEvent verifyEmailRequest = eventListener.getEvents().get(0);
    Assert.assertTrue(verifyEmailRequest.getPayload() instanceof VerifyEmailRequest);
    eventListener.clear();
    // verify email
    String auth = registration.getUser().getObject().getAuthorization(0);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("regAuth", auth);
    session.setAttribute("regParam", registration.getUser().getObject().getParam());
    controller.verifyEmail(getRequestTemplate(HttpMethod.GET, "/verify_email"), map, session);
    // Since tenant is not at activated, we will get only Welcome email
    Assert.assertEquals(1, eventListener.getEvents().size());
    PortalEvent event = eventListener.getEvents().get(0);
    Assert.assertTrue(event.getPayload() instanceof EmailVerified);
  }

  @Test
  public void testWelcomeEmailEventForRetail() {
    // Sign up for manual activation account type
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/portal/register");
    UserRegistration registration = new UserRegistration();
    registration.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    registration.setAcceptedTerms(true);
    AccountType disposition = accountTypeDAO.getDefaultSelfRegistrationAccountType();
    BindingResult result = null;
    try {
      result = setupRegistration(disposition, registration);
    } catch (Exception e) {
      e.printStackTrace();
    }
    beforeRegisterCall(request, registration);
    controller.register(registration, result, "abc", "abc", map, null, status, request);
    // Tenant Activation Event
    // Assert.assertEquals(1, eventListener.getEvents().size());
    // PortalEvent tenantActivationEvent = eventListener.getEvents().get(0);
    // Assert.assertTrue(tenantActivationEvent.getPayload() instanceof TenantActivation);
    eventListener.clear();
    // verify email
    String auth = registration.getUser().getObject().getAuthorization(0);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("regAuth", auth);
    session.setAttribute("regParam", registration.getUser().getObject().getParam());
    controller.verifyEmail(getRequestTemplate(HttpMethod.GET, "/verify_email"), map, session);
    // Since tenant is at activated, we will get Email Verified event
    Assert.assertEquals(1, eventListener.getEvents().size());
  }

  @Test
  public void testWelcomeEmailAndActivationEmailEventsForRetail() {
    // Sign up for manual activation account type
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/portal/register");
    UserRegistration registration = new UserRegistration();
    registration.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    registration.setAcceptedTerms(true);
    AccountType disposition = accountTypeDAO.getDefaultSelfRegistrationAccountType();
    BindingResult result = null;
    try {
      result = setupRegistration(disposition, registration);
    } catch (Exception e) {
      e.printStackTrace();
    }
    beforeRegisterCall(request, registration);
    controller.register(registration, result, "abc", "abc", map, null, status, request);
    // Tenant Activation Event
    // Assert.assertEquals(1, eventListener.getEvents().size());
    // PortalEvent tenantActivationEvent = eventListener.getEvents().get(0);
    // Assert.assertTrue(tenantActivationEvent.getPayload() instanceof TenantActivation);
    eventListener.clear();
    // verify email
    String auth = registration.getUser().getObject().getAuthorization(0);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("regAuth", auth);
    session.setAttribute("regParam", registration.getUser().getObject().getParam());
    controller.verifyEmail(getRequestTemplate(HttpMethod.GET, "/verify_email"), map, session);
    // Since tenant is at activated, we will get only Welcome email and ActivationEmail events
    Assert.assertEquals(1, eventListener.getEvents().size());
  }

  @Test
  public void testWelcomeEmailAndActivationEmailEventsForCorporate() {
    // Sign up for manual activation account type
    MockHttpServletRequest request = getRequestTemplate(HttpMethod.GET, "/portal/register");
    UserRegistration registration = new UserRegistration();
    registration.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    registration.setAcceptedTerms(true);
    List<AccountType> accountTypes = accountTypeDAO.getManualRegistrationAccountTypes();
    AccountType disposition = null;
    for (AccountType accountType : accountTypes) {
      if (accountType.isManualActivation()) {
        disposition = accountType;
      }
    }
    BindingResult result = null;
    try {
      result = setupRegistration(disposition, registration);
    } catch (Exception e) {
      e.printStackTrace();
    }
    beforeRegisterCall(request, registration);
    controller.register(registration, result, "abc", "abc", map, null, status, request);
    // Tenant Activation Event
    // Assert.assertEquals(1, eventListener.getEvents().size());
    // PortalEvent accountActivationRequestEvent = eventListener.getEvents().get(0);
    // Assert.assertTrue(accountActivationRequestEvent.getPayload() instanceof AccountActivationRequestEvent);
    eventListener.clear();

    // verify email
    String auth = registration.getUser().getObject().getAuthorization(0);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("regAuth", auth);
    session.setAttribute("regParam", registration.getUser().getObject().getParam());
    controller.verifyEmail(getRequestTemplate(HttpMethod.GET, "/verify_email"), map, session);
    // Since tenant is at activated, we will get EmailVerified Event
    Assert.assertEquals(1, eventListener.getEvents().size());
    eventListener.clear();

    Tenant tenant = tenantService.get(registration.getTenant().getUuid());
    completeBusinessTransactionsForTenant(tenant);
    tenantService.changeState(registration.getTenant().getUuid(), "ACTIVE", null, "Manual");
    PortalEvent tenantActivationEvent = eventListener.getEvents().get(0);
    Assert.assertTrue(tenantActivationEvent.getPayload() instanceof TenantActivation);
    eventListener.clear();
  }

  @Test
  public void testTrialRegister() {
    Channel channel = channelservice.locateChannel("Channel2");
    String register = controller.trialRegister("trial_camp", "Channel2", map);
    Assert.assertEquals(register, new String("register.userinfo"));
    Assert.assertEquals(map.get("title"), new String("page.order_now"));
    Assert.assertEquals(map.get("homeUrl"), new String("http://www.cloud.com"));
    Assert.assertEquals(map.get("cloudmktgUrl"), new String("http://www.cloud.com"));
    Assert.assertNotNull(map.get("registration"));
    UserRegistration registration = (UserRegistration) map.get("registration");
    Assert.assertNotNull(registration.getTrialCode());
    Assert.assertNotNull(registration.getTenant());
    Assert.assertEquals(map.get("channelParam"), channel.getParam());
    register = controller.trialRegister("trial_camp", null, map);
    Assert.assertEquals(register, new String("register.userinfo"));
  }

  @Test
  public void testInvalidTrailRegister() {

    try {
      Assert.assertEquals(controller.trialRegister("wrong_trialcode", null, map), new String("trial.invalid"));
      Assert.fail();
    } catch (IllegalArgumentException e) {
    }

  }

  @Test
  public void testsetPassword() {

    User user = createUserWithoutPassword();
    Assert.assertNull(user.getPassword());
    session.setAttribute("regParam", user.getParam());
    String setPassword = controller.setPassword("Test123#", session);
    Assert.assertNotNull(user.getPassword());
    Assert.assertEquals(setPassword, new String("redirect:/portal/verify_email"));
    Assert.assertTrue(user.authenticate("Test123#"));
  }

  @Test
  public void testback() {
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    UserRegistration registration = new UserRegistration();
    registration.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    registration.setTenant((com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(tenant));
    String back = controller.back(registration, map);
    Assert.assertEquals(back, new String("register.userinfo"));
    Assert.assertEquals(map.get("title"), new String("page.order_now"));
    Assert.assertEquals(map.get("registration"), registration);
    Assert.assertEquals(((com.citrix.cpbm.access.Tenant) map.get("tenant")).getAccountId(), registration.getTenant()
        .getAccountId());
    Assert.assertEquals(map.get("page"), Page.HOME);
    Assert.assertEquals(map.get("cloudmktgUrl"), new String("http://www.cloud.com"));

  }

  @Test
  public void testutilityratesLightbox() {
    String utilityrates = controller.utilityratesLightbox("0bd2ab86-7402-4815-b785-55c8d9090580", "USD", null, null,
        map);
    Assert.assertNotNull(utilityrates);
    Assert.assertTrue(map.containsAttribute("currency"));
    Assert.assertTrue(map.containsAttribute("startDate"));
    Assert.assertEquals(map.get("currency"), currencyValueService.locateBYCurrencyCode("USD"));
    Assert.assertNotNull(map.get("startDate"));
    utilityrates = controller.utilityratesLightbox("0bd2ab86-7402-4", "USD", null, null, map);
    Assert.assertNotNull(utilityrates);
    Assert.assertTrue(map.containsAttribute("currency"));
    Assert.assertTrue(map.containsAttribute("startDate"));
    Assert.assertEquals(map.get("currency"), currencyValueService.locateBYCurrencyCode("USD"));
    Assert.assertNotNull(map.get("startDate"));
  }

  /*
   * Description : Test signup with promocode added in channel. Author : Avinashg
   */
  @Test
  public void testPromoCodeAddedInGivenChannel() throws Exception {
    AccountType accountType = accountTypeDAO.find("3");

    Channel actualChannel = channelDAO.find(3L);
    Tenant tenant2 = new Tenant("Acme Corp " + random.nextInt(), accountType, null, randomAddress(), true,
        currencyValueDAO.findByCurrencyCode("USD"), null);
    com.citrix.cpbm.access.Tenant tenant1 = (com.citrix.cpbm.access.Tenant) CustomProxy.newInstance(tenant2);
    UserRegistration userRegistration = new UserRegistration();
    userRegistration.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    userRegistration.setTenant(tenant1);
    userRegistration.setAccountTypeId("3");
    controller.signup(map, campaignpromotiondao.find("1").getCode(), actualChannel.getCode(), "3", status, request);
    Assert.assertEquals(actualChannel.getParam(), map.get("channelParam"));

  }

  @Test
  public void testSignupStep1() {
    ModelMap model = new ModelMap();
    String register = controller.signupStep1(model, request);
    Assert.assertEquals(register, new String("register.account_type"));
  }

  @Test
  public void testUserInfo() {
    ModelMap model = new ModelMap();
    UserRegistration registration = new UserRegistration();
    registration.setAcceptedTerms(true);
    AccountType disposition = accountTypeDAO.getTrialAccountType();
    BindingResult result;
    try {
      result = setupRegistration(disposition, registration);
      String register = controller.userInfo(registration, result, model, null, status, request);
      Assert.assertEquals("register.moreuserinfo", register);
    } catch (Exception e) {
      Assert.fail();
    }

  }

  @Test
  public void testListAnonymousProductBundles() {
    List<ProductBundleRevision> productBundleRevisions = controller.listAnonymousProductBundles(getDefaultTenant()
        .getSourceChannel().toString(), "USD", false, null);
    Assert.assertNotNull(productBundleRevisions);
  }

  @Test
  public void testPhoneverification() {
    ModelMap model = new ModelMap();
    UserRegistration registration = new UserRegistration();
    List<Country> countryList = new ArrayList<Country>();
    countryList.addAll(countryService.getCountries(null, null, null, 1, 12, null, false));
    registration.setCountryList(countryList);
    AccountType disposition = accountTypeDAO.getTrialAccountType();
    BindingResult result;
    try {
      result = setupRegistration(disposition, registration);
      String value = controller.phoneverification(registration, result, "abs", "abs", model, null, status, request);
      Assert.assertEquals(value, new String("register.phoneverification"));
    } catch (Exception e) {
      Assert.fail();
    }
    try {
      result = setupRegistration(disposition, registration);
      controller.phoneverification(registration, result, "abs", null, model, null, status, request);
    } catch (Exception e) {
      Assert.fail();
    }
    try {
      countryList = null;
      result = setupRegistration(disposition, registration);
      controller.phoneverification(registration, result, "abs", null, model, null, status, request);
      Assert.assertTrue(true);
    } catch (Exception e) {
      Assert.fail();
    }

  }

  @Test
  public void testValidateMailDomain() {
    String value = controller.validateMailDomain(VALID_EMAIL);
    Assert.assertNotNull(value);
  }

  @Test
  public void testUserInfoWithIntranetOnlyModeEnabled() throws Exception {
    configurationService.clearConfigurationCache(true, "");
    com.vmops.model.Configuration isIntranetModeEnabled = configurationService
        .locateConfigurationByName(Names.com_citrix_cpbm_use_intranet_only);
    isIntranetModeEnabled.setValue("true");
    configurationService.update(isIntranetModeEnabled);

    ModelMap model = new ModelMap();
    UserRegistration registration = new UserRegistration();
    AccountType disposition = accountTypeDAO.getTrialAccountType();
    BindingResult result;
    registration.setAcceptedTerms(true);
    result = setupRegistration(disposition, registration);
    String view = controller.userInfo(registration, result, model, null, status, request);
    Assert.assertEquals("register.moreuserinfo", view);
    Assert.assertFalse(model.containsKey("showCaptcha"));
    Assert.assertFalse(model.containsKey("recaptchaPublicKey"));

  }

  @Test
  public void testUserInfoWithIntranetOnlyModeDisabled() throws Exception {
    configurationService.clearConfigurationCache(true, "");
    com.vmops.model.Configuration isIntranetModeEnabled = configurationService
        .locateConfigurationByName(Names.com_citrix_cpbm_use_intranet_only);
    isIntranetModeEnabled.setValue("false");
    configurationService.update(isIntranetModeEnabled);

    ModelMap model = new ModelMap();
    UserRegistration registration = new UserRegistration();
    AccountType disposition = accountTypeDAO.getTrialAccountType();
    BindingResult result;
    registration.setAcceptedTerms(true);
    result = setupRegistration(disposition, registration);
    String view = controller.userInfo(registration, result, model, null, status, request);
    Assert.assertEquals("register.moreuserinfo", view);
    Assert.assertTrue(model.containsKey("showCaptcha"));
    Assert.assertTrue(Boolean.valueOf(model.get("showCaptcha").toString()));
    Assert.assertTrue(model.containsKey("recaptchaPublicKey"));

  }
}