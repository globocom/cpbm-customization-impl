/* Copyright (C) 2011 Cloud.com, Inc. All rights reserved. */
package fragment.web;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import web.WebTestsBaseWithMockConnectors;
import web.support.DispatcherTestServlet;
import web.support.MockSessionStatus;

import citrix.cpbm.access.proxy.CustomProxy;
import citrix.cpbm.portal.forms.TenantForm;
import citrix.cpbm.portal.fragment.controllers.TenantsController;

import com.citrix.cpbm.core.workflow.service.BusinessTransactionService;
import com.citrix.cpbm.platform.admin.service.exceptions.ConnectorManagementServiceException;
import com.citrix.cpbm.platform.bootstrap.service.BootstrapActivator;
import com.citrix.cpbm.platform.spi.AccountLifecycleHandler;
import com.citrix.cpbm.platform.spi.CloudConnector;
import com.citrix.cpbm.platform.spi.CloudConnectorFactory.ConnectorType;
import com.citrix.cpbm.platform.spi.UserLifecycleHandler;
import com.vmops.event.AccountActivationRequestEvent;
import com.vmops.event.PortalEvent;
import com.vmops.event.TenantActivation;
import com.vmops.event.VerifyAlertEmailRequest;
import com.vmops.internal.service.PaymentGatewayService;
import com.vmops.model.AccountType;
import com.vmops.model.Address;
import com.vmops.model.CampaignPromotion;
import com.vmops.model.CreditCard;
import com.vmops.model.CurrencyValue;
import com.vmops.model.PendingChange;
import com.vmops.model.PendingChange.ChangeType;
import com.vmops.model.PromotionSignup;
import com.vmops.model.PromotionToken;
import com.vmops.model.ResourceLimit;
import com.vmops.model.Service;
import com.vmops.model.SpendAlertSubscription;
import com.vmops.model.Tenant;
import com.vmops.model.Tenant.State;
import com.vmops.model.TenantChange;
import com.vmops.model.TenantChange.Action;
import com.vmops.model.User;
import com.vmops.model.UserAlertPreferences;
import com.vmops.model.UserAlertPreferences.AlertType;
import com.vmops.model.billing.PaymentTransaction;
import com.vmops.persistence.AccountTypeDAO;
import com.vmops.persistence.AddressDAO;
import com.vmops.persistence.CampaignPromotionDAO;
import com.vmops.persistence.PendingChangeDAO;
import com.vmops.persistence.PromotionTokenDAO;
import com.vmops.persistence.SpendAlertSubscriptionDAO;
import com.vmops.persistence.UserAlertPreferencesDAO;
import com.vmops.portal.config.Configuration.Names;
import com.vmops.service.AccountTypeService;
import com.vmops.service.ChannelService;
import com.vmops.service.ConfigurationService;
import com.vmops.service.ResourceLimitService;
import com.vmops.service.TenantService;
import com.vmops.service.UserAlertPreferencesService;
import com.vmops.service.UserService.Handle;
import com.vmops.service.exceptions.NoManualRegistrationAccountTypeException;
import com.vmops.service.exceptions.TenantStateChangeFailedException;
import com.vmops.web.forms.AccountResourceLimitForm;
import com.vmops.web.forms.BillingInfoForm;
import com.vmops.web.forms.CustomAlertForm;
import com.vmops.web.forms.ResourceLimitForm;
import com.vmops.web.forms.SearchForm;
import com.vmops.web.forms.SetAccountTypeForm;
import com.vmops.web.forms.TenantLogoForm;
import com.vmops.web.forms.UserAlertEmailForm;
import com.vmops.web.interceptors.UserContextInterceptor;

public class TenantsControllerTest extends WebTestsBaseWithMockConnectors {

  private ModelMap map;

  private MockSessionStatus status;

  private MockHttpServletResponse response;

  private MockHttpServletRequest request;

  @Autowired
  private CampaignPromotionDAO cmpdao;

  @Autowired
  private PromotionTokenDAO tokendao;

  @Autowired
  private TenantsController controller;

  @Autowired
  PendingChangeDAO pendingChangeDAO;
  
  @Autowired
  TenantService tenantService;

  @Autowired
  protected UserAlertPreferencesService userAlertPreferenceService;

  @Autowired
  AddressDAO addressDAO;

  @Autowired
  AccountTypeService acctypeService;

  @Autowired
  ResourceLimitService service;

  @Autowired
  UserAlertPreferencesDAO userAlertDAO;

  @Autowired
  SpendAlertSubscriptionDAO spendAlertDAO;

  @Autowired
  ChannelService channelService;

  @Autowired
  AccountTypeDAO accTypeDAO;

  @Autowired
  ConfigurationService configurationService;

  private BootstrapActivator bootstrapActivator = new BootstrapActivator();

  private static boolean isMockInstanceCreated = false;

  private PaymentGatewayService ossConnector = null;

  private CloudConnector iaasConnector = null;

  @Autowired
  private BusinessTransactionService businessTransactionService;

  @Before
  public void init() throws Exception {
    map = new ModelMap();
    status = new MockSessionStatus();
    response = new MockHttpServletResponse();
    request = new MockHttpServletRequest();
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
            return new PaymentTransaction(new Tenant(), 0, com.vmops.model.billing.PaymentTransaction.State.COMPLETED,
                com.vmops.model.billing.PaymentTransaction.Type.CAPTURE);
          }
        }).anyTimes();
    EasyMock.replay(iaasConnector);
    EasyMock.replay(ossConnector);
  }

  private SpendAlertSubscription create_spendAlertSubscription(Tenant tenant) {
    SpendAlertSubscription spendAlert = new SpendAlertSubscription();
    spendAlert.setAccountHolder(tenant);
    spendAlert.setData("TEST");
    spendAlert.setUser(userService.getUserByParam("id", "4", false));
    spendAlert.setPercentage(BigDecimal.valueOf(100));
    spendAlert.setSubscriptionType(1);
    spendAlertDAO.save(spendAlert);
    return spendAlert;
  }

  @Test
  public void testRouting() throws Exception {
    logger.debug("Testing routing....");
    DispatcherTestServlet servlet = this.getServletInstance();
    Class<? extends TenantsController> controllerClass = controller.getClass();
    Method expected = locateMethod(controllerClass, "listPopup", new Class[] {
        ModelMap.class, String.class, String.class, String.class, String.class, String.class, String.class
    });
    Method handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/"));
    Assert.assertEquals(expected, handler);

    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "show", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/viewtenant"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "edit", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/edit"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "edit", new Class[] {
        TenantForm.class, BindingResult.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/edit"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "editCurrentTenant", new Class[] {
        String.class, String.class, String.class, ModelMap.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/editcurrent"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "searchlist", new Class[] {
        ModelMap.class, String.class, String.class, String.class, String.class, String.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/searchlist"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "list", new Class[] {
        String.class, String.class, String.class, String.class, String.class, String.class, String.class,
        ModelMap.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/list"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "create", new Class[] {
        ModelMap.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/new"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "create", new Class[] {
        TenantForm.class, BindingResult.class, ModelMap.class, SessionStatus.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "changeState", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/1/changeState"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "changeState", new Class[] {
        String.class, State.class, String.class, HttpServletResponse.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/1/changeState"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "issueCredit", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/issueCredit"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "issueCredit", new Class[] {
        String.class, String.class, String.class, HttpServletRequest.class, HttpServletResponse.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/1/add_credit"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "cleanupTenant", new Class[] {
        String.class, HttpServletResponse.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/clean"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "editOwner", new Class[] {
        String.class, String.class, HttpServletResponse.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/1/set_owner"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "search", new Class[] {
        SearchForm.class, BindingResult.class, String.class, String.class, String.class, String.class, String.class,
        ModelMap.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/search"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "getAuditLog", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/1/audit_log"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "showResourceLimits", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/1/resource_limit"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "showResourceLimit", new Class[] {
        String.class, ResourceLimitForm.class, HttpServletResponse.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/1/resource_limit"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "approvePendingChange", new Class[] {
        String.class, String.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/1/approvePendingChange"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "actOnPendingChange", new Class[] {
        String.class, String.class, com.vmops.model.PendingChange.State.class, String.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/1/actOnPendingChange"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "getAccountType", new Class[] {
      String.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/getAccountType"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "listNotifications", new Class[] {
        Tenant.class, String.class, String.class, String.class, String.class, ModelMap.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/notifications"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "viewNotification", new Class[] {
        Tenant.class, String.class, String.class, String.class, String.class, String.class, ModelMap.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/viewnotification"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "viewAlertsDeliveryOptions", new Class[] {
        String.class, String.class, ModelMap.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/alert_prefs"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "saveAlertsDeliveryOptions", new Class[] {
        UserAlertEmailForm.class, BindingResult.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/alert_prefs"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "deleteAlertsDeliveryOptions", new Class[] {
        Long.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/alert_prefs/1/delete"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "listSubscriptions", new Class[] {
        Tenant.class, String.class, String.class, String.class, ModelMap.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/alerts"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "viewAlert", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/alerts/view"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "createSpendAlertSubscription", new Class[] {
        Tenant.class, String.class, ModelMap.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/alerts/new"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "createSpendAlertSubscription", new Class[] {
        Tenant.class, String.class, CustomAlertForm.class, BindingResult.class, ModelMap.class, HttpServletRequest.class 
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/alerts/new"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "removeSubscription", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/alerts/remove"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "editSpendAlertSubscription", new Class[] {
        String.class, Tenant.class, String.class, ModelMap.class, HttpServletRequest.class 
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/alerts/edit"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "editCurrentTenantLogo", new Class[] {
      ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/editcurrentlogo"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "editTenantLogo", new Class[] {
        String.class, TenantLogoForm.class, BindingResult.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/123/editlogo"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "editSpendAlertSubscription", new Class[] {
        String.class, String.class, CustomAlertForm.class, BindingResult.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/alerts/edit"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "setAccountBudget", new Class[] {
        Tenant.class, String.class, ModelMap.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/setAccountBudget"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "setAccountBudget", new Class[] {
        Tenant.class, String.class, TenantForm.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/setAccountBudget"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "delete", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/1/delete"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "listStateChanges", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/stateChanges"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "listAccountLimits", new Class[] {
        ModelMap.class, String.class, String.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/listaccountlimits"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "editAccountlimits", new Class[] {
        String.class, String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/tenants/editaccountlimits"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "editAccountlimits", new Class[] {
        String.class, String.class, String.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/tenants/editaccountlimits"));
    Assert.assertEquals(expected, handler);

  }

  @Test
  public void testTenantShow() throws Exception {
    Tenant expected = tenantDAO.findAll(null).get(0);

    String view = controller.show(expected.getUuid(), map);

    Assert.assertEquals("tenant.view", view);
    Assert.assertTrue(map.containsKey("creditBalance"));
    Assert.assertTrue(map.containsKey("pendingActions"));

    Assert.assertTrue(map.containsKey("tenant"));
    Tenant found = (Tenant) map.get("tenant");
    Assert.assertEquals(expected, found);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testTenantsListPopup() throws Exception {
    List<Tenant> expected = tenantDAO.findAll(null);

    String view = controller.listPopup(map, "0", "0", "0", "All", null, getSystemTenant().getParam());
    Assert.assertEquals("tenants.list", view);
    Assert.assertTrue(map.containsKey("tenants"));
    List<Tenant> found = (List<Tenant>) map.get("tenants");
    Assert.assertTrue(map.containsKey("url"));
    Assert.assertTrue(found.containsAll(expected));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testTenantsListPopupwithPattern() throws Exception {
    List<Tenant> expected = tenantDAO.findAll(null);

    String view = controller.listPopup(map, "0", "0", "0", "All", "A", getSystemTenant().getParam());
    Assert.assertEquals("tenants.list", view);
    Assert.assertTrue(map.containsKey("tenants"));
    List<Tenant> found = (List<Tenant>) map.get("tenants");
    Assert.assertTrue(map.containsKey("url"));
    Assert.assertTrue(expected.containsAll(found));
  }

  @Test
  public void testTenantForm() {
    TenantForm form = new TenantForm();
    Assert.assertNotNull(form.getCountryList());
  }

  @Test
  public void testTenantActivation() throws Exception {
    asRoot();
    Tenant tenant = createTestTenant(accountTypeDAO.getOnDemandPostPaidAccountType());
    User user = new User("test", "user", "test@test.com", VALID_USER + random.nextInt(), VALID_PASSWORD, VALID_PHONE,
        VALID_TIMEZONE, tenant, ownerProfile, getRootUser());
    userDAO.save(user);
    tenantService.setOwner(tenant, user);
    // user is active now as all the required fields for activation are
    // saved. This triggers activation.
    Tenant found = tenantDAO.find(tenant.getId());
    Assert.assertEquals(State.NEW, found.getState());
  }

  @Test(expected = TenantStateChangeFailedException.class)
  public void testTenantActivationNoOwner() throws Exception {
    asRoot();
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    controller.changeState(tenant.getParam(), State.ACTIVE, "memo", response);
  }

  @Test(expected = TenantStateChangeFailedException.class)
  public void testTenantActivationNoCurrency() throws Exception {
    Tenant tenant = new Tenant("New Tenant", accountTypeDAO.getDefaultRegistrationAccountType(), null, randomAddress(),
        true, null, null);
    List<CurrencyValue> activeCurrencies = currencyValueService.listActiveCurrencies();
    for (CurrencyValue currencyValue : activeCurrencies) {
      tenant.setCurrency(currencyValue);
      break;
    }
    tenantService.createAccount(tenant, userService.getSystemUser(Handle.ROOT), null, null);
    controller.changeState(tenant.getParam(), State.ACTIVE, "memo", response);
  }

  @Test(expected = TenantStateChangeFailedException.class)
  public void testTenantActivationAfterTermination() throws Exception {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    tenantService.setOwner(tenant, getRootUser());
    controller.changeState(tenant.getParam(), State.TERMINATED, "Terminate Account", response);
    controller.changeState(tenant.getParam(), State.ACTIVE, "Reactivate Account", response);
  }

  @Test
  public void testTenantTermination() throws Exception {
    asRoot();
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    User user = new User("test", "user", "test@test.com", VALID_USER + random.nextInt(), VALID_PASSWORD, VALID_PHONE,
        VALID_TIMEZONE, tenant, ownerProfile, getRootUser());
    userDAO.save(user);
    tenantService.setOwner(tenant, user);
    tenantService.changeState(tenant.getUuid(), "ACTIVE", null, "memo");
    HashMap<String, String> result = controller.changeState(tenant.getParam(), State.TERMINATED, "memo", response);
    Assert.assertEquals(HttpStatus.OK.value(), response.getStatus());
    Assert.assertEquals("TERMINATED", result.get("state"));

    Tenant found = tenantDAO.find(tenant.getId());
    Assert.assertTrue(found.getState() == State.TERMINATED);
  }

  @Test
  public void testTenantNew() {
    asRoot();
    String view = controller.create(map, new MockHttpServletRequest());
    Tenant tenant = (Tenant) map.get("tenant");
    Assert.assertNotNull(tenant);
    Assert.assertEquals("tenants.new", view);
    Assert.assertEquals("false", map.get("showSuffixTextBox"));
    Assert.assertEquals("false", map.get("showImportAdButton"));
  }

  @Test
  public void testTenantNewWithSuffixBox() {
    asRoot();
    com.vmops.model.Configuration configuration = configurationService
        .locateConfigurationByName(Names.com_citrix_cpbm_username_duplicate_allowed);
    configuration.setValue("true");
    configurationService.update(configuration);

    configuration = configurationService.locateConfigurationByName(Names.com_citrix_cpbm_portal_directory_service_enabled);
    configuration.setValue("true");
    configurationService.update(configuration);

    configuration = configurationService.locateConfigurationByName(Names.com_citrix_cpbm_directory_mode);
    configuration.setValue("pull");
    configurationService.update(configuration);

    String view = controller.create(map, new MockHttpServletRequest());
    Tenant tenant = (Tenant) map.get("tenant");
    Assert.assertNotNull(tenant);
    Assert.assertEquals("true", map.get("showSuffixTextBox"));
    Assert.assertEquals("true", map.get("showImportAdButton"), "true");
    Assert.assertEquals("tenants.new", view);
  }

  @Test
  public void testTenantNewWithNoManualActivationAccountType() {
    asRoot();
    List<AccountType> accountTypes = accountTypeDAO.getAllAccountTypes();
    for (AccountType type : accountTypes) {
      if (!type.isManualRegistrationAllowed())
        continue;
      type.setManualRegistrationAllowed(false);
      accountTypeDAO.save(type);
    }
    @SuppressWarnings("unused")
    String view = null;
    try {
      view = controller.create(map, new MockHttpServletRequest());
      Assert.fail();
    } catch (NoManualRegistrationAccountTypeException e) {
      Assert.assertEquals("Manual Registration Not Allowed", e.getMessage());
    } catch (Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testCreateTenant() throws Exception {
    asRoot();
    TenantForm form = new TenantForm((citrix.cpbm.access.Tenant)CustomProxy.newInstance(new Tenant()));
    form.setAccountTypeId(tenantService.getDefaultRegistrationAccountType().getId().toString());

    citrix.cpbm.access.Tenant newTenant = form.getTenant();
    newTenant.setName("ACME Corp");
    newTenant.setAddress(new Address("steve", "creek", "cupertino", "ca", "95014", "US"));
    List<CurrencyValue> activeCurrencies = currencyValueService.listActiveCurrencies();
    for (CurrencyValue currencyValue : activeCurrencies) {
      newTenant.setCurrency(currencyValue);
      break;
    }

    citrix.cpbm.access.User newUser = form.getUser();
    newUser.setFirstName("test");
    newUser.setLastName("user");
    newUser.setEmail("test@test.com");
    newUser.setUsername(VALID_USER + random.nextInt());
    newUser.getObject().setClearPassword(VALID_PASSWORD);
    newUser.setPhone(VALID_PHONE);
    newUser.setTimeZone(VALID_TIMEZONE);
    newUser.setProfile(userProfile);
    newUser.getObject().setCreatedBy(getPortalUser());

    newUser.setAddress(new Address("steve", "creek", "cupertino", "ca", "95014", "US"));
    // test add for secondary address
    form.setAllowSecondary(true);
    form.setSecondaryAddress(new Address("steve", "creek", "cupertino", "CHAN", "95014", "IN"));

    BindingResult result = validate(form);
    Assert.assertEquals("validating that the form has no errors", 0, result.getErrorCount());
    controller.create(form, result, map, status, new MockHttpServletRequest());

    Tenant found = tenantDAO.findByBillingId("RSBill");
    Assert.assertNotNull(found);
    Assert.assertEquals(newTenant, found);
    Assert.assertTrue(status.isComplete());
    Assert.assertEquals(1, eventListener.getEvents().size());
    PortalEvent event = eventListener.getEvents().get(0);
    Assert.assertTrue(event.getPayload() instanceof TenantActivation);
    Assert.assertEquals(newTenant.getAccountId(), ((TenantActivation) event.getPayload()).getAccountId());
    Assert.assertEquals(newTenant, event.getSource());

    Assert.assertNotNull(found.getAddress());
    Assert.assertNotNull(found.getSecondaryAddress());

    Assert.assertEquals(found.getAddress().getCountry(), "US");
    Assert.assertEquals(found.getSecondaryAddress().getCountry(), "IN");
  }

  @Test
  public void testCreateTenantWithSuffix() throws Exception {
    asRoot();
    TenantForm form = new TenantForm((citrix.cpbm.access.Tenant)CustomProxy.newInstance(new Tenant()));
    form.setAccountTypeId(tenantService.getDefaultRegistrationAccountType().getId().toString());
    citrix.cpbm.access.Tenant newTenant = form.getTenant();
    newTenant.setName("ACME Corp");
    newTenant.setAddress(new Address("steve", "creek", "cupertino", "ca", "95014", "US"));
    String suffix = "testSuffix";
    newTenant.getObject().setUsernameSuffix(suffix);
    List<CurrencyValue> activeCurrencies = currencyValueService.listActiveCurrencies();
    for (CurrencyValue currencyValue : activeCurrencies) {
      newTenant.setCurrency(currencyValue);
      break;
    }

    citrix.cpbm.access.User newUser = form.getUser();
    newUser.setFirstName("test");
    newUser.setLastName("user");
    newUser.setEmail("test@test.com");
    String userName = VALID_USER + random.nextInt();
    newUser.setUsername(userName);
    newUser.getObject().setClearPassword(VALID_PASSWORD);
    newUser.setPhone(VALID_PHONE);
    newUser.setTimeZone(VALID_TIMEZONE);
    newUser.setProfile(userProfile);
    newUser.getObject().setCreatedBy(getPortalUser());

    newUser.setAddress(new Address("steve", "creek", "cupertino", "ca", "95014", "US"));
    // test add for secondary address
    form.setAllowSecondary(true);
    form.setSecondaryAddress(new Address("steve", "creek", "cupertino", "CHAN", "95014", "IN"));

    BindingResult result = validate(form);
    Assert.assertEquals("validating that the form has no errors", 0, result.getErrorCount());
    controller.create(form, result, map, status, new MockHttpServletRequest());

    newTenant.getOwner().setUserName(userName + "@" + suffix);
    Tenant found = tenantDAO.findByBillingId("RSBill");
    Assert.assertNotNull(found);
    Assert.assertEquals(userName + "@" + suffix, found.getOwner().getUsername());
    Assert.assertEquals(newTenant, found);
    Assert.assertTrue(status.isComplete());
    Assert.assertEquals(1, eventListener.getEvents().size());
    PortalEvent event = eventListener.getEvents().get(0);
    Assert.assertTrue(event.getPayload() instanceof TenantActivation);
    Assert.assertEquals(newTenant.getAccountId(), ((TenantActivation) event.getPayload()).getAccountId());
    Assert.assertEquals(newTenant, event.getSource());

    Assert.assertNotNull(found.getAddress());
    Assert.assertNotNull(found.getSecondaryAddress());

    Assert.assertEquals(found.getAddress().getCountry(), "US");
    Assert.assertEquals(found.getSecondaryAddress().getCountry(), "IN");
  }

  @Test
  public void testTenantEdit() throws Exception {
    Tenant expected = tenantDAO.findAll(null).get(0);

    String view = controller.edit(expected.getParam(), map);
    Assert.assertEquals("tenant.edit", view);
    Assert.assertTrue(map.containsKey("tenantForm"));
    citrix.cpbm.access.Tenant found = ((TenantForm) map.get("tenantForm")).getTenant();
    Assert.assertEquals(expected, found);
  }

  @Test
  public void testTenantsUpdate() throws Exception {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    tenant.setName("UPDATED");
    tenant.getAddress().setCity("UPDATECITY");
    TenantForm form = new TenantForm((citrix.cpbm.access.Tenant)CustomProxy.newInstance(tenant));
    BindingResult result = validate(form);
    HashMap<String, String> map = controller.edit(form, result);
    Assert.assertEquals(tenant.getParam(), map.get("param"));

    Tenant found = tenantDAO.find(tenant.getId());
    Assert.assertEquals("UPDATED", found.getName());
    Assert.assertEquals("UPDATECITY", found.getAddress().getCity());
  }

  @Test
  public void testTenantsUpdateBillingSyncAddress() throws Exception {
    Tenant tenant = createTestTenant(accountTypeDAO.getOnDemandPostPaidAccountType());
    tenant.setName("UPDATED");
    tenant.getAddress().setCity("UPDATECITY");
    User user = new User("test", "user", "test@test.com", VALID_USER + random.nextInt(), VALID_PASSWORD, VALID_PHONE,
        VALID_TIMEZONE, tenant, ownerProfile, getRootUser());
    userDAO.save(user);
    tenantService.setOwner(tenant, user);
    TenantForm form = new TenantForm((citrix.cpbm.access.Tenant)CustomProxy.newInstance(tenant));
    form.setUser((citrix.cpbm.access.User)CustomProxy.newInstance(user));
    BindingResult result = validate(form);
    HashMap<String, String> map = controller.edit(form, result);
    Assert.assertEquals(tenant.getParam(), map.get("param"));

    Tenant found = tenantDAO.find(tenant.getId());
    Assert.assertEquals("UPDATED", found.getName());
    Assert.assertEquals("UPDATECITY", found.getAddress().getCity());
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void testTenantsUpdateFail() throws Exception {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    tenant.setName(null);
    TenantForm form = new TenantForm();
    BindingResult result = validate(form);
    controller.edit(form, result);
  }

  @Test
  public void testSetOwnerPost() throws ConnectorManagementServiceException {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    User user = createTestUserInTenant(tenant);
    String view = controller.editOwner(tenant.getParam(), user.getId().toString(), response);
    Assert.assertEquals("redirect:/portal/tenants/" + tenant.getParam(), view);
    tenantDAO.flush();
    tenantDAO.clear();
    Tenant found = tenantDAO.find(tenant.getId());
    Assert.assertEquals(user, found.getOwner());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetOwnerPostDifferentTenantUser() throws ConnectorManagementServiceException {
    Tenant tenant = tenantDAO.find(2L);
    User user = userDAO.find(4L);
    Assert.assertFalse(user.getTenant().equals(tenant));
    controller.editOwner(tenant.getParam(), user.getId().toString(), response);
  }

  @Test
  public void testGetAuditLog() throws Exception {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    createTestUserInTenant(tenant);
    String view = controller.getAuditLog(tenant.getParam(), map);
    Assert.assertEquals("auditlog.show", view);
  }

  @Test(expected = Exception.class)
  public void testActOnPendingChangeRejectFailTest() throws Exception {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    tenant.getAccountType().setTrial(true);
    Tenant trialTenant = tenantDAO.save(tenant);
    Assert.assertTrue(trialTenant.getAccountType().isTrial());
    PendingChange pendingChange = createPendingChangeForAccountConversion(trialTenant);
    Assert.assertEquals(pendingChange.getState(), com.vmops.model.PendingChange.State.WAITING_FOR_APPROVAL);

    // Approve pending change for this tenant
    controller.actOnPendingChange(trialTenant.getUuid(), pendingChange.getId().toString(),
        com.vmops.model.PendingChange.State.REJECTED, null);
  }

  @Test
  public void testVerifyAlertsDeliveryOptions() throws Exception {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    User user = createTestUserInTenant(tenant);
    String emailAddress = "test_verify_email@test.com";

    userAlertPreferenceService.createUserAlertPreference(user, emailAddress, AlertType.USER_ALERT_EMAIL);

    List<UserAlertPreferences> prefs = userAlertPreferenceService.listAllUserAlertPreferences(user);
    Assert.assertEquals(1, prefs.size());

    UserAlertPreferences selected = null;
    for (UserAlertPreferences pref : prefs) {
      if (pref.getUser().getId() == user.getId() && pref.getAlertType() == AlertType.USER_ALERT_EMAIL) {
        selected = pref;
        break;
      }
    }
    Assert.assertEquals(emailAddress, selected.getEmailAddress());
    Assert.assertEquals(user.getId(), selected.getUser().getId());
    Assert.assertEquals(AlertType.USER_ALERT_EMAIL, selected.getAlertType());

    String view = controller.verifyAlertsDeliveryOptions(selected.getId(), map);
    Assert.assertEquals("success", view);
    Assert.assertEquals(2, eventListener.getEvents().size());
    PortalEvent verifyEvent = eventListener.getEvents().get(1);
    Assert.assertTrue(verifyEvent.getPayload() instanceof VerifyAlertEmailRequest);
    Assert.assertEquals(user.getUsername(), ((VerifyAlertEmailRequest) verifyEvent.getPayload()).getUsername());
    Assert.assertEquals(user, verifyEvent.getSource());
  }

  private PendingChange createPendingChangeForAccountConversion(Tenant tenant) {
    PendingChange pendingChange = new PendingChange();
    pendingChange.setChangeType(ChangeType.CONVERT_ACCOUNT_TYPE);
    pendingChange.setTargetAccountType(accountTypeDAO.find(3L));
    pendingChange.setCreateDate(new Date());
    pendingChange.setTenant(tenant);
    pendingChange.setTargetPaymentInfo("payment_info");
    return pendingChangeDAO.save(pendingChange);
  }

  @Test
  public void testManualActivationFlag() throws Exception {
    asRoot();
    List<AccountType> accountTypes = accountTypeDAO.getAllAccountTypes();
    for (AccountType type : accountTypes) {
      if (type.isTrial())
        continue;
      if (!type.isManualRegistrationAllowed())
        continue;
      type.setManualActivation(true);
      TenantForm form = new TenantForm((citrix.cpbm.access.Tenant)CustomProxy.newInstance(new Tenant()));
      form.setAccountTypeId(type.getId().toString());

      citrix.cpbm.access.Tenant newTenant = form.getTenant();
      newTenant.setName("ACME Corp");
      newTenant.setAddress(new Address("steve", "creek", "cupertino", "ca", "95014", "US"));
      newTenant.setCurrency(null);
      List<CurrencyValue> activeCurrencies = currencyValueService.listActiveCurrencies();
      for (CurrencyValue currencyValue : activeCurrencies) {
        newTenant.setCurrency(currencyValue);
        break;
      }

      citrix.cpbm.access.User newUser = form.getUser();
      newUser.setFirstName("test");
      newUser.setLastName("user");
      newUser.setEmail("test@test.com");
      newUser.setUsername(VALID_USER + random.nextInt());
      newUser.getObject().setClearPassword(VALID_PASSWORD);
      newUser.setPhone(VALID_PHONE);
      newUser.setTimeZone(VALID_TIMEZONE);
      newUser.setProfile(userProfile);
      newUser.getObject().setCreatedBy(getPortalUser());
      newUser.setAddress(new Address("steve", "creek", "cupertino", "ca", "95014", "US"));
      BindingResult result = validate(form);
      Assert.assertEquals("validating that the form has no errors", 0, result.getErrorCount());
      controller.create(form, result, map, status, new MockHttpServletRequest());

      Assert.assertTrue(status.isComplete());
      Assert.assertEquals(1, eventListener.getEvents().size());
      PortalEvent event = eventListener.getEvents().get(0);
      Assert.assertTrue(event.getPayload() instanceof AccountActivationRequestEvent);
      Assert.assertEquals(newTenant, ((AccountActivationRequestEvent) event.getPayload()).getTenant());
      Assert.assertEquals(newTenant, event.getSource());
      eventListener.clear();
    }

  }

  @Test
  public void testeditCurrentTenant() {
    Tenant tenant = tenantService.getTenantByParam("id", "2", false);
    ;
    tenant.getAccountType().setDepositRequired(true);
    tenant.setSecondaryAddress(VALID_BILLING_ADDRESS);
    tenant.getAccountType().setEnableSecondaryAddress(true);
    tenantService.save(tenant);
    request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
    String editTenant = controller.editCurrentTenant(Action.CONVERTED.toString(), tenant.getParam(), "contact", map, request);
    Assert.assertEquals(editTenant, new String("tenants.editcurrent"));
    Assert.assertTrue(map.containsAttribute("tenant"));
    Assert.assertTrue(map.containsAttribute("tenantForm"));
    Assert.assertTrue(map.containsAttribute("showChangeAccountType"));
    Assert.assertTrue(map.containsAttribute("currenttab"));
    Assert.assertEquals(map.get("currenttab"), 2);
    Assert.assertTrue(map.containsAttribute("showMessagePendingConversion"));
    Assert.assertTrue(map.containsAttribute("show_deposit_record_message"));
    Assert.assertTrue(map.containsAttribute("paymentInfoExists"));
    Assert.assertTrue(map.containsAttribute("showCreditCardTabVar"));
    Assert.assertTrue(map.containsAttribute("billingInfo"));
    Assert.assertTrue(map.containsAttribute("setAccountTypeForm"));
    Assert.assertTrue(map.containsAttribute("targetAccountTypes"));
    Assert.assertTrue(map.containsAttribute("currentBillingStart"));
    Assert.assertTrue(map.containsAttribute("currentBillingEnd"));
    Assert.assertTrue(map.containsAttribute("BillingPeriodLength"));
    Assert.assertEquals(map.get("tenant"), tenant);
    TenantForm tenantForm = ((TenantForm) map.get("tenantForm"));
    Assert.assertEquals(tenantForm.getTenant(), tenant);
    Assert.assertEquals(tenantForm.getSecondaryAddress(), VALID_BILLING_ADDRESS);
    Assert.assertEquals(true, tenantForm.isAllowSecondary());
    Assert.assertEquals(true, map.get("showChangeAccountType"));
    Assert.assertEquals(map.get("showMessagePendingConversion"), false);
    Assert.assertEquals(map.get("showCreditCardTabVar"), true);
    BillingInfoForm billingInfoForm = ((BillingInfoForm) map.get("billingInfo"));
    Assert.assertEquals(billingInfoForm.getTenant(), tenant);
    PaymentGatewayService paymentGatewayService = (PaymentGatewayService) connectorManagementService
        .getOssServiceInstancebycategory(ConnectorType.PAYMENT_GATEWAY);
    if (paymentGatewayService != null) {
      CreditCard creditCard = paymentGatewayService.getCreditCard(tenant);
      Assert.assertEquals(billingInfoForm.getCreditCard(), creditCard);
    }
    Assert.assertEquals(map.get("paymentInfoExists"), true);
    SetAccountTypeForm setAccountTypeForm = ((SetAccountTypeForm) map.get("setAccountTypeForm"));
    Assert.assertEquals(setAccountTypeForm.getTenant(), tenant);
    Assert.assertNotNull(map.get("currentBillingStart"));
    Assert.assertNotNull(map.get("currentBillingEnd"));
    Assert.assertNotNull(map.get("BillingPeriodLength"));
    Assert.assertTrue(map.containsKey("transactionHistory"));
  }

  @Test
  public void testeditCurrentTenantLogo() {
    String editLogo = controller.editCurrentTenantLogo(map);
    Assert.assertEquals(editLogo, new String("tenants.editcurrentlogo"));
    Assert.assertTrue(map.containsAttribute("tenantLogoForm"));
    TenantLogoForm tenantLogoForm = ((TenantLogoForm) map.get("tenantLogoForm"));
    Assert.assertNotNull(tenantLogoForm.getTenant());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testsearchlist() {
    String searchList = controller.searchlist(map, "1", "5", null, "All", null);
    Assert.assertEquals(searchList, new String("tenant.search.list"));
    Assert.assertTrue(map.containsAttribute("tenants"));
    List<Tenant> tenants = ((List<Tenant>) map.get("tenants"));
    Assert.assertTrue(tenants.size() == 5);
    map.clear();
    controller.searchlist(map, "1", "10", null, "All", null);
    tenants = ((List<Tenant>) map.get("tenants"));
    Assert.assertTrue(tenants.size() == 10);

  }

  @Test
  public void testchangeState() {
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    String changeState = controller.changeState(tenant.getParam(), map);
    Assert.assertEquals(changeState, new String("tenant.change.state"));
    Assert.assertTrue(map.containsAttribute("tenant"));
    Assert.assertEquals(map.get("tenant"), tenant);
  }
  
  @Test
  @ExpectedException(TenantStateChangeFailedException.class)
  public void testchangeStatePost() {
    Tenant tenant = createTenantWithOwner();
    controller.changeState(tenant.getUuid(), State.ACTIVE, "", response);
  }

  public TenantsControllerTest() {
    // TODO Auto-generated constructor stub
  }

  @Test
  public void testissueCredit() {
    request = new MockHttpServletRequest();
    HttpServletResponse res = new MockHttpServletResponse();

    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    String issueCredit = controller.issueCredit(tenant.getParam(), "100", "JUNIT TEST", request, res);
    Assert.assertNotNull(issueCredit);
    Assert.assertTrue(issueCredit.contains("100"));

  }

  @Test
  public void testIssueCredit() {
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    String issueCredit = controller.issueCredit(tenant.getParam(), map);
    Assert.assertEquals(issueCredit, new String("tenant.issue.credit"));
    Assert.assertTrue(map.containsAttribute("tenant"));
  }

  @Test
  public void testhandleTenantStateChangeFailedException() {
    TenantStateChangeFailedException ex = new TenantStateChangeFailedException("JUNIT TEST");
    ModelAndView view = controller.handleTenantStateChangeFailedException(ex, null);
    Assert.assertNotNull(view);
    Assert.assertTrue(view.getModel().toString().contains("JUNIT TEST"));

  }

  @SuppressWarnings("unchecked")
  @Test
  public void testlist() {
    request = new MockHttpServletRequest();
    AccountType accType = acctypeService.locateAccountTypeById("1");
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    String tenantlist = controller
        .list(tenant.getUuid(), "TEST", "1", null, "1", tenant.getParam(), null, map, request);
    Assert.assertEquals(tenantlist, new String("tenant.listing"));
    Assert.assertTrue(map.containsAttribute("accountTypes"));
    Assert.assertTrue(map.containsAttribute("showAddAccountWizard"));
    Assert.assertTrue(map.containsAttribute("filterBy"));
    Assert.assertTrue(map.containsAttribute("accountresult"));
    Assert.assertTrue(map.containsAttribute("selectedTab"));
    Assert.assertTrue(map.containsAttribute("on"));
    Assert.assertTrue(map.containsAttribute("selectedAccountType"));
    Assert.assertTrue(map.containsAttribute("tenant"));
    Assert.assertTrue(map.containsAttribute("tenants"));
    Assert.assertTrue(map.containsAttribute("creditBalance"));

    List<AccountType> accountTypes = tenantService.getAllAccountTypes();
    Assert.assertEquals(map.get("accountTypes"), accountTypes);
    Assert.assertEquals(map.get("showAddAccountWizard"), new String("TEST"));
    Assert.assertNull(map.get("filterBy"));
    Assert.assertEquals(map.get("accountresult"), new String("true"));
    Assert.assertEquals(map.get("selectedTab"), accType.getName());
    Assert.assertEquals(map.get("on"), new String("on"));
    Assert.assertEquals(map.get("selectedAccountType"), accType.getId().toString());
    List<Tenant> list = ((List<Tenant>) map.get("tenants"));
    Assert.assertTrue(list.size() == 1);
    Assert.assertEquals(list.get(0), tenant);

    map.clear();
    tenant.setState(State.NEW);
    tenantService.save(tenant);
    tenantlist = controller.list(tenant.getUuid(), "TEST", "1", null, "1", null, null, map, request);
    Assert.assertEquals(map.get("filterBy"), "0");

    map.clear();
    tenant.setState(State.ACTIVE);
    tenantService.save(tenant);
    tenantlist = controller.list(tenant.getUuid(), "TEST", "1", null, "1", null, null, map, request);
    Assert.assertEquals(map.get("filterBy"), "1");

    map.clear();
    tenant.setState(State.LOCKED);
    tenantService.save(tenant);
    tenantlist = controller.list(tenant.getUuid(), "TEST", "1", null, "1", null, null, map, request);
    Assert.assertEquals(map.get("filterBy"), "2");

    map.clear();
    tenant.setState(State.SUSPENDED);
    tenantService.save(tenant);
    tenantlist = controller.list(tenant.getUuid(), "TEST", "1", null, "1", null, null, map, request);
    Assert.assertEquals(map.get("filterBy"), "3");

    map.clear();
    tenant.setState(State.TERMINATED);
    tenantService.save(tenant);
    tenantlist = controller.list(tenant.getUuid(), "TEST", "1", "Desc", "1", null, null, map, request);
    Assert.assertEquals(map.get("filterBy"), "4");
    Assert.assertEquals(map.get("filterresult"), new String("true"));

  }

  @Test
  public void testcleanupTenant() {
    response = new MockHttpServletResponse();
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    String cleanUp = controller.cleanupTenant(tenant.getParam(), response);
    Assert.assertEquals("success", cleanUp);
    Assert.assertEquals(State.TERMINATED, tenant.getState() );
    Assert.assertNotNull(tenant.getRemoved());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testsearch() {
    request = new MockHttpServletRequest();
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    SearchForm form = new SearchForm();
    form.setfieldName(tenant.getOwner().getFirstName());
    form.setName(tenant.getName());
    form.setAccountId(tenant.getAccountId());
    HashMap<String, String> custMap = new HashMap<String, String>();
    custMap.put("name", "Acme Corp");
    form.setCustomFields(custMap);

    String search = controller.search(form, null, "1", "1", null, "1", null, map, request);
    Assert.assertEquals(search, new String("tenant.listing"));
    Assert.assertTrue(map.containsAttribute("accountTypes"));
    Assert.assertTrue(map.containsAttribute("selectedAccountType"));
    Assert.assertTrue(map.containsAttribute("filterBy"));
    Assert.assertTrue(map.containsAttribute("searchresult"));
    Assert.assertTrue(map.containsAttribute("fieldName"));
    Assert.assertTrue(map.containsAttribute("name"));
    Assert.assertTrue(map.containsAttribute("accountId"));
    Assert.assertTrue(map.containsAttribute("tenants"));
    Assert.assertTrue(map.containsAttribute("searchForm"));

    List<AccountType> accountTypes = tenantService.getAllAccountTypes();
    Assert.assertEquals(map.get("accountTypes"), accountTypes);
    Assert.assertEquals(map.get("selectedAccountType"), "1");
    Assert.assertEquals(map.get("filterBy"), new String("All"));
    Assert.assertEquals(map.get("searchresult"), new String("true"));
    Assert.assertEquals(map.get("fieldName"), form.getfieldName());
    Assert.assertEquals(map.get("name"), form.getName());
    Assert.assertEquals(map.get("accountId"), form.getAccountId());
    List<Tenant> tenants = ((List<Tenant>) map.get("tenants"));
    Assert.assertTrue(tenants.size() == 1);
    Assert.assertEquals(tenants.get(0), tenant);

  }

  @Test
  public void testshowResourceLimits() {
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    String resource_limit = controller.showResourceLimits(tenant.getParam(), map);
    Assert.assertEquals(resource_limit, new String("tenant.resourcelimits"));
    Assert.assertTrue(map.containsAttribute("resourceLimitForm"));
    Assert.assertTrue(map.containsAttribute("tenant"));
    ResourceLimitForm rlForm = ((ResourceLimitForm) map.get("resourceLimitForm"));
    Assert.assertEquals(rlForm.getId(), tenant.getId());
    Assert.assertEquals(rlForm.getDefaultMaxUsers(), tenant.getAccountType().getMaxUsers().toString());

  }

  @Test
  public void testshowResourceLimit() {
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    ResourceLimitForm form = new ResourceLimitForm();
    form.setMaxUsers("2");

    String resourcelimit = controller.showResourceLimit(tenant.getParam(), form, response);
    Assert.assertEquals(resourcelimit, new String("success"));
    List<ResourceLimit> list = service.locateResourceLimitsByTenant(tenant);
    Assert.assertEquals(list.get(0).getMax(), 2);
    Assert.assertEquals(list.get(0).getType(), ResourceLimit.USER);
    resourcelimit = controller.showResourceLimit(tenant.getParam(), null, response);
    Assert.assertEquals(resourcelimit, new String("fail"));
  }

  @Test
  public void testgetAccountType() {
    AccountType type = controller.getAccountType("RETAIL");
    Assert.assertNotNull(type);
    Assert.assertEquals(type.getId().toString(), "3");
    Assert.assertEquals(type.getDisplayName(), new String("Retail"));

  }

  @Test
  public void testsaveNewPrimaryEmail() {
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    UserAlertPreferences userAlertPreferences = new UserAlertPreferences();
    userAlertPreferences.setUser(getRootUser());
    userAlertPreferences.setCreatedBy(getRootUser());
    userAlertPreferences.setEmailAddress("test" + random.nextInt() + "@test.com");
    userAlertPreferences.setUpdatedBy(getRootUser());
    userAlertPreferences.setAlertType(AlertType.USER_EMAIL);
    userAlertDAO.save(userAlertPreferences);
    request.setAttribute("isSurrogatedTenant", false);
    String saveEmail = controller.saveNewPrimaryEmail(tenant.getParam(), tenant.getOwner().getParam(),
        userAlertPreferences.getId().toString(), map, request);
    Assert.assertEquals(saveEmail, userAlertPreferences.getEmailAddress());
    saveEmail = controller.saveNewPrimaryEmail(tenant.getParam(), tenant.getOwner().getParam(), null, map, request);
    Assert.assertEquals(saveEmail, new String("failure"));

  }

  @Test
  public void testdeleteAlertsDeliveryOptions() {
    UserAlertPreferences userAlertPreferences = new UserAlertPreferences();
    userAlertPreferences.setUser(getRootUser());
    userAlertPreferences.setCreatedBy(getRootUser());
    userAlertPreferences.setEmailAddress("test" + random.nextInt() + "@test.com");
    userAlertPreferences.setUpdatedBy(getRootUser());
    userAlertPreferences.setAlertType(AlertType.USER_EMAIL);
    userAlertDAO.save(userAlertPreferences);

    String delete = controller.deleteAlertsDeliveryOptions(userAlertPreferences.getId(), map);
    Assert.assertEquals(delete, new String("success"));

    delete = controller.deleteAlertsDeliveryOptions(null, map);
    Assert.assertEquals(delete, new String("failure"));

  }

  @Test
  public void testviewAlert() {
    Tenant tenant = tenantService.getTenantByParam("id", "3", false);
    tenant.setSpendBudget(BigDecimal.valueOf(100));
    tenantService.save(tenant);
    SpendAlertSubscription spendAlert = create_spendAlertSubscription(tenant);
    String viewAlert = controller.viewAlert(spendAlert.getId().toString(), map);

    Assert.assertEquals(viewAlert, new String("alerts.view"));
    Assert.assertTrue(map.containsAttribute("subscription"));
    Assert.assertTrue(map.containsAttribute("spendbudget"));
    Assert.assertTrue(map.containsAttribute("tenant"));
    Assert.assertTrue(map.containsAttribute("user_email"));
    Assert.assertTrue(map.containsAttribute("user_phone"));
    Assert.assertEquals(map.get("subscription"), spendAlert);
    Assert.assertEquals(map.get("spendbudget"), tenant.getSpendBudget().toString());
    Assert.assertEquals(map.get("tenant"), tenant);
    Assert.assertEquals(map.get("user_email"), tenant.getOwner().getEmail());
    Assert.assertEquals(map.get("user_phone"), tenant.getOwner().getPhone());
  }

  @Test
  public void testcreateSpendAlertSubscription() {
    Tenant tenant1 = tenantService.getTenantByParam("id", "1", false);
    Tenant tenant2 = tenantService.getTenantByParam("id", "3", false);
    tenant2.setSpendBudget(BigDecimal.valueOf(100));
    tenantService.save(tenant2);
    request.setAttribute("isSurrogatedTenant", true);
    request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant2);
    String create = controller.createSpendAlertSubscription(tenant1, tenant2.getParam(), map, request);
    Assert.assertEquals(create, new String("alerts.new"));
    Assert.assertTrue(map.containsAttribute("tenant"));
    Assert.assertTrue(map.containsAttribute("subscriptionForm"));
    Assert.assertEquals(map.get("tenant"), tenant2);
    CustomAlertForm subscriptionForm = ((CustomAlertForm) map.get("subscriptionForm"));
    Assert.assertEquals(subscriptionForm.getType(), "tenant");
    Assert.assertEquals(subscriptionForm.getBudget().toString(), "100");
    Assert.assertEquals(subscriptionForm.getThresholdType(), "percentage");

  }

  @Test
  public void testeditSpendAlertSubscription() {
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    tenant.setSpendBudget(BigDecimal.valueOf(100));
    tenantService.save(tenant);
    SpendAlertSubscription spendAlert = new SpendAlertSubscription();
    spendAlert.setAccountHolder(tenant);
    spendAlert.setData("TEST");
    spendAlert.setUser(userService.getUserByParam("id", "1", false));
    spendAlert.setPercentage(BigDecimal.valueOf(100));
    spendAlert.setSubscriptionType(1);
    spendAlertDAO.save(spendAlert);
    request.setAttribute("isSurrogatedTenant", false);
    String edit = controller.editSpendAlertSubscription(spendAlert.getId().toString(), tenant, tenant.getParam(), map, request);
    Assert.assertEquals(edit, new String("alerts.edit"));
    Assert.assertTrue(map.containsAttribute("subscriptionForm"));
    Assert.assertTrue(map.containsAttribute("subscriptionId"));
    CustomAlertForm form = ((CustomAlertForm) map.get("subscriptionForm"));
    Assert.assertEquals(form.getThresholdType(), new String("percentage"));
    Assert.assertEquals(form.getTenantPercentage(), spendAlert.getPercentage());
    Assert.assertEquals(map.get("subscriptionId"), spendAlert.getId().toString());
    Assert.assertEquals(map.get("tenant"), tenant);

    map.clear();
    Tenant tenant1 = tenantService.getTenantByParam("id", "3", false);
    edit = controller.editSpendAlertSubscription(spendAlert.getId().toString(), tenant, tenant1.getParam(), map, request);
    Assert.assertEquals(map.get("tenant"), tenant1);
    form = ((CustomAlertForm) map.get("subscriptionForm"));
    Assert.assertEquals(form.getUser(), tenant1.getOwner());

  }

  @Test
  public void testsetAccountBudget() {
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    tenant.setSpendBudget(BigDecimal.valueOf(100));
    tenantService.save(tenant);
    request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
    String setBudget = controller.setAccountBudget(tenant, tenant.getParam(), map, request);
    Assert.assertEquals(setBudget, new String("alerts.setaccountbudget"));
    Assert.assertTrue(map.containsAttribute("tenantForm"));
    Assert.assertTrue(map.containsAttribute("tenant"));
    Assert.assertEquals(map.get("tenant"), tenant);
    TenantForm tenantForm = ((TenantForm) map.get("tenantForm"));
    Assert.assertEquals(tenantForm.getSpendLimit(), tenant.getSpendBudget());

    map.clear();
    Tenant tenant1 = tenantService.getTenantByParam("id", "3", false);
    tenant1.setSpendBudget(BigDecimal.valueOf(100));
    tenantService.save(tenant1);
    request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant1);
    setBudget = controller.setAccountBudget(tenant, tenant1.getParam(), map, request);
    Assert.assertEquals(map.get("tenant"), tenant1);
    tenantForm = ((TenantForm) map.get("tenantForm"));
    Assert.assertEquals(tenantForm.getSpendLimit(), tenant1.getSpendBudget());

  }

  @Test
  public void testSetAccountBudget() {
    Tenant tenant1 = tenantService.getTenantByParam("id", "1", false);
    ;
    tenant1.setSpendBudget(BigDecimal.valueOf(100));
    tenantService.save(tenant1);
    Tenant tenant2 = tenantService.getTenantByParam("id", "3", false);
    TenantForm form = new TenantForm();
    form.setTenant((citrix.cpbm.access.Tenant)CustomProxy.newInstance(tenant1));

    String setBudget = controller.setAccountBudget(tenant1, tenant2.getParam(), form, map);
    Assert.assertEquals(setBudget, new String("redirect:/portal/tenants/alerts?tenant=" + tenant2.getParam()));
    Assert.assertTrue(map.containsAttribute("subscriptions"));
    Assert.assertTrue(map.containsAttribute("size"));
    Assert.assertTrue(map.containsAttribute("user"));
    Assert.assertEquals(map.get("user"), tenant2.getOwner());
    Assert.assertNotNull(map.get("subscriptions"));
    Assert.assertEquals(tenant2.getSpendBudget().toString(), "100");

  }

  @Test
  public void testdelete() {
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    String delete = controller.delete(tenant.getParam(), map);
    Assert.assertEquals(delete, new String("redirect:/portal/tenants/list"));
  }

  @Test
  public void testdeleteWithSuffix() {
    asRoot();
    com.vmops.model.Configuration configuration = configurationService
        .locateConfigurationByName(Names.com_citrix_cpbm_username_duplicate_allowed);
    configuration.setValue("true");
    configurationService.update(configuration);

    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    tenant.setUsernameSuffix("test");
    tenantService.update(tenant);

    String delete = controller.delete(tenant.getParam(), map);
    Assert.assertEquals(delete, new String("redirect:/portal/tenants/list"));

    tenant = tenantService.getTenantByParam("id", "1", true);
    Assert.assertEquals("_1_test", tenant.getUsernameSuffix());
  }

  @Test
  public void testlistStateChanges() {
    AccountType accountType = new AccountType("NewACType_" + System.currentTimeMillis(), "New AccountType",
        "New AccountType Description", false);
    accTypeDAO.save(accountType);

    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    TenantChange tenantChange = new TenantChange();
    tenantChange.setTenant(tenant);
    tenantChange.setOldState(State.SUSPENDED);
    tenantChange.setNewState(State.ACTIVE);
    tenantChange.setOldAccountType(accountType);
    tenantChange.setNewAccountType(accountType);
    tenantChange.setAction(Action.ACTIVATED.toString());
    tenantChange.setCreatedBy(getPortalUser());
    tenantService.saveTenantChange(tenantChange);
    String stateChange = controller.listStateChanges(tenant.getParam(), map);
    Assert.assertEquals(stateChange, new String("tenant.state.changes"));
    Assert.assertTrue(map.containsAttribute("tenant"));
    Assert.assertEquals(map.get("tenant"), tenant);
    Assert.assertTrue(map.containsKey("transactionHistory"));
  }

  @Test
  public void testeditAccountLimits() {
    Tenant tenant = tenantService.getTenantByParam("id", "2", false);
    // TODO:naveen this has to be done by adapter
    String edit = controller.editAccountlimits(tenant.getParam(), "", map);
    Assert.assertEquals(edit, new String("tenant.editaccountlimits"));
    Assert.assertTrue(map.containsAttribute("accountResourceLimitForm"));
    AccountResourceLimitForm resLimitForm = ((AccountResourceLimitForm) map.get("accountResourceLimitForm"));
    Assert.assertEquals(resLimitForm.getIplimit().toString(), "2");
    Assert.assertEquals(resLimitForm.getSnapshotlimit().toString(), "2");
    Assert.assertEquals(resLimitForm.getTemplatelimit().toString(), "1");
    Assert.assertEquals(resLimitForm.getVmlimit().toString(), "2");
    Assert.assertEquals(resLimitForm.getVolumelimit().toString(), "2");
    Assert.assertEquals(resLimitForm.getTenant(), tenant);
  }

  @Test
  public void testlistAccountLimits() {
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    String show = controller.listAccountLimits(map, "dfc84388-d44d-4d8e-9d6a-a62c1c16b7e4", "003fa8ee-fba3-467f-a517-fd806dae8a80");
    Assert.assertEquals(show, new String("tenant.showlimits"));
    Assert.assertEquals(map.get("domainid"), tenantService.getTenantHandle(tenant.getUuid(), "1"));
    Assert.assertEquals(map.get("tenantid"), tenant.getId().toString());
    Assert.assertEquals(map.get("tenantuuid"), tenant.getUuid());
    Assert.assertEquals(map.get("iplimit"), "-1");
    Assert.assertEquals(map.get("vmlimit"), "-1");
    Assert.assertEquals(map.get("snapshotlimit"), "-1");
    Assert.assertEquals(map.get("templatelimit"), "-1");
    Assert.assertEquals(map.get("volumelimit"), "-1");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testviewVerifiedEmails() {
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    User user = tenant.getOwner();
    String emailAddress = "test_verify_email@test.com";

    UserAlertPreferences userAlertPreference = userAlertPreferenceService.createUserAlertPreference(user, emailAddress,
        AlertType.USER_ALERT_EMAIL);
    userAlertPreference.setEmailVerified(true);
    request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
    request.setAttribute("isSurrogatedTenant", false);
    String verifiedmails = controller.viewVerifiedEmails(tenant.getParam(), user.getParam(), map, request);

    Assert.assertEquals(verifiedmails, new String("alerts.change_primary_email"));
    Assert.assertEquals(map.get("user"), user);
    List<UserAlertPreferences> verifiedAlertPrefs = ((List<UserAlertPreferences>) map.get("verifiedAlertsPrefs"));
    Assert.assertEquals(verifiedAlertPrefs.get(0).toString(), userAlertPreference.toString());
    Assert.assertTrue(verifiedAlertPrefs.size() == 1);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testlistSubscriptions() {
    Tenant tenant1 = tenantService.getTenantByParam("id", "1", false);
    tenant1.setSpendBudget(BigDecimal.valueOf(100));
    tenantService.save(tenant1);
    Tenant tenant2 = tenantService.getTenantByParam("id", "3", false);
    SpendAlertSubscription spendAlert = create_spendAlertSubscription(tenant2);
    request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant2);
    request.setAttribute("isSurrogatedTenant", true);
    String listSubscriptions = controller.listSubscriptions(tenant1, tenant2.getParam().toString(), "true", "1", map, request);
    Assert.assertEquals(listSubscriptions, new String("tenants.alerts"));
    Assert.assertEquals(map.get("showUserProfile"), true);
    Assert.assertEquals(map.get("tenant"), tenant2);
    Assert.assertEquals(map.get("subscrptionalerttype"), "tenant");
    Assert.assertNotNull(map.get("subscriptionForm"));
    List<SpendAlertSubscription> subscriptions = ((List<SpendAlertSubscription>) map.get("subscriptions"));
    Assert.assertEquals(subscriptions.get(0), spendAlert);
    Assert.assertEquals(map.get("size"), subscriptions.size());
    Assert.assertNotNull(map.get("tenantForm"));
    Assert.assertEquals(map.get("budget"), "true");
  }

  @Test
  public void testremoveSubscription() {
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    SpendAlertSubscription spendAlert = create_spendAlertSubscription(tenant);
    Assert.assertNotNull(spendAlert.getUser().getSpendAlertSubscriptions());
    Assert.assertTrue(spendAlert.getUser().getSpendAlertSubscriptions().size() == 1);
    String remove = controller.removeSubscription(spendAlert.getId().toString(), map);
    Assert.assertEquals(remove, new String("success"));
    Assert.assertTrue(spendAlert.getUser().getSpendAlertSubscriptions().size() == 0);
  }

  public void testEditAccountLimits()  {
    Tenant tenant = tenantService.getTenantByParam("id", "2", false);
    AccountResourceLimitForm form = new AccountResourceLimitForm();
    form.setIplimit(5L);
    form.setSnapshotlimit(4L);
    form.setTenant(tenant);
    form.setVmlimit(3L);
    form.setVolumelimit(2L);
    form.setTemplatelimit(1L);
    HashMap<String, String> map = controller.editAccountlimits(tenant.getParam(), "", "");
    Assert.assertEquals(map.get("param"), tenant.getParam());
    Assert.assertEquals(map.get("name"), tenant.getName());
    Assert.assertEquals(map.get("accountType"), tenant.getAccountType().getDisplayName());
    Assert.assertEquals(map.get("accountId"), tenant.getAccountId());
    Assert.assertEquals(map.get("state"), tenant.getState().getName());
    Assert.assertEquals(map.get("username"), tenant.getOwner().getUsername());
    Assert.assertEquals(map.get("domainid"), tenantService.getTenantHandle(tenant.getUuid(), "1"));
    Assert.assertEquals(map.get("tenantid"), tenant.getId().toString());
    ModelMap map1 = new ModelMap();
   // controller.listAccountLimits(map1, tenantService.getTenantHandle(tenant.getUuid(), "1");, tenant.getUuid().toString(), tenant
    //    .getId().toString(), "");
    Assert.assertEquals(map1.get("iplimit"), "5");
    Assert.assertEquals(map1.get("vmlimit"), "3");
    Assert.assertEquals(map1.get("snapshotlimit"), "4");
    Assert.assertEquals(map1.get("templatelimit"), "1");
    Assert.assertEquals(map1.get("volumelimit"), "2");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testviewAlertsDeliveryOptions() {
    Tenant tenant = tenantService.getTenantByParam("id", "1", false);
    User user = tenant.getOwner();
    String emailAddress = "test_verify_email@test.com";

    UserAlertPreferences userAlertPreference = userAlertPreferenceService.createUserAlertPreference(user, emailAddress,
        AlertType.USER_ALERT_EMAIL);
    userAlertPreference.setEmailVerified(true);
    request.setAttribute("isSurrogatedTenant", false);
    String options = controller.viewAlertsDeliveryOptions(tenant.getParam(), user.getParam(), map, request);
    Assert.assertEquals(options, new String("alerts.delivery_opts"));
    List<UserAlertPreferences> alertsPrefs = ((List<UserAlertPreferences>) map.get("alertsPrefs"));
    Assert.assertEquals(alertsPrefs.get(0), userAlertPreference);
    Assert.assertEquals(map.get("alertsPrefsSize"), 1);
    Assert.assertEquals(map.get("addAlertEmailLimit"), 2);
    Assert.assertNotNull(map.get("userAlertEmailForm"));
  }

  @Test
  public void testValidTrialCode() throws Exception {
    String response = controller.validatePromotionCodeforTenant("INVALID", "");
    Assert.assertEquals("false", response);

    PromotionSignup promotionSignup = new PromotionSignup("test" + random.nextInt(), "Citrix",
        "PromotionSignUp@citrix.com");
    promotionSignup.setCreateBy(getRootUser());
    promotionSignup.setCurrency(Currency.getInstance("USD"));
    promotionSignup.setPhone("9999999999");

    CampaignPromotion campaignPromotion = new CampaignPromotion();
    campaignPromotion.setCode("USD" + random.nextInt());
    campaignPromotion.setCreateBy(getRootUser());
    campaignPromotion.setTrial(true);
    cmpdao.save(campaignPromotion);

    PromotionToken promotionToken = new PromotionToken(campaignPromotion, "TESTPROMOCODE");
    promotionToken.setCreateBy(getRootUser());
    tokendao.save(promotionToken);

    promotionSignup.setPromotionToken(promotionToken);
    response = controller.validatePromotionCodeforTenant("TESTPROMOCODE", "");
    Assert.assertEquals("true", response);

  }

  @Test
  public void testShow(){
    Tenant tenant = tenantService.getTenantByParam("id", "2", false);
    String view = controller.show(tenant.getParam(), map);
    Assert.assertEquals("tenant.view", view);
    
    Assert.assertTrue("map must have tenant",map.containsKey("tenant"));
    Assert.assertEquals(tenant, map.get("tenant"));
    Assert.assertTrue("map must have actions",map.containsKey("pendingActions"));
    
  }
}
