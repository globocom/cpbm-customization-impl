/* Copyright (C) 2011 Citrix Systems, Inc. All rights reserved */
package fragment.web;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ModelMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.BindingResult;

import web.WebTestsBaseWithMockConnectors;
import web.support.DispatcherTestServlet;
import citrix.cpbm.portal.fragment.controllers.BillingController;

import com.citrix.cpbm.platform.admin.service.exceptions.ConnectorManagementServiceException;
import com.citrix.cpbm.platform.bootstrap.service.BootstrapActivator;
import com.citrix.cpbm.platform.spi.AccountLifecycleHandler;
import com.citrix.cpbm.platform.spi.CloudConnector;
import com.citrix.cpbm.platform.spi.UserLifecycleHandler;
import com.vmops.config.BillingPostProcessor;
import com.vmops.internal.service.PaymentGatewayService;
import com.vmops.internal.service.SubscriptionService;
import com.vmops.model.Configuration;
import com.vmops.model.Invoice;
import com.vmops.model.InvoiceItem;
import com.vmops.model.ProductBundle;
import com.vmops.model.Service;
import com.vmops.model.Subscription;
import com.vmops.model.Tenant;
import com.vmops.model.User;
import com.vmops.model.billing.AccountStatement;
import com.vmops.model.billing.PaymentTransaction;
import com.vmops.model.billing.PaymentTransaction.State;
import com.vmops.persistence.ConfigurationDAO;
import com.vmops.service.ProductBundleService;
import com.vmops.service.billing.BillingService;
import com.vmops.usage.model.BillingPeriodDates;
import com.vmops.web.forms.BillingInfoForm;
import com.vmops.web.forms.DepositRecordForm;
import com.vmops.web.forms.SetAccountTypeForm;

public class BillingControllerTest extends WebTestsBaseWithMockConnectors {

  private ModelMap map;

  private MockHttpServletRequest request;

  private MockHttpServletResponse response;

  @Autowired
  private BillingController controller;

  @Autowired
  private ConfigurationDAO configurationDAO;

  @Autowired
  private SubscriptionService subscriptionService;

  @Autowired
  private ProductBundleService productBundleService;

  @Autowired
  private BillingService billingService;

  @Autowired
  private BillingPostProcessor billingPostProcessor;

  private BootstrapActivator bootstrapActivator = new BootstrapActivator();

  private static boolean isMockInstanceCreated = false;

  private PaymentGatewayService ossConnector = null;

  private CloudConnector iaasConnector = null;

  @Before
  public void init() throws Exception {
    map = new ModelMap();
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
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

          @Override
          public PaymentTransaction answer() throws Throwable {
            return new PaymentTransaction(new Tenant(), 0, State.COMPLETED,
                com.vmops.model.billing.PaymentTransaction.Type.CAPTURE);
          }
        }).anyTimes();
    EasyMock.replay(iaasConnector);
    EasyMock.replay(ossConnector);
  }

  @Test
  public void testRouting() throws Exception {
    logger.debug("Testing routing....");
    DispatcherTestServlet servlet = getServletInstance();
    Class<? extends BillingController> controllerClass = BillingController.class;
    Method expected = locateMethod(controllerClass, "showSubscriptions", new Class[] {
        Tenant.class, String.class, String.class, String.class, String.class, ModelMap.class, HttpServletRequest.class
    });
    Method handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/billing/subscriptions"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "showSubscriptionDetails", new Class[] {
        String.class, String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/billing/subscriptions/showDetails"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "terminateSubscription", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/billing/subscriptions/terminate/1234"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "showHistory", new Class[] {
        Tenant.class, String.class, String.class, String.class, HttpSession.class, ModelMap.class,
        HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/billing/history"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "showPaymentHistory", new Class[] {
        Tenant.class, String.class, String.class, HttpSession.class, HttpServletRequest.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/billing/paymenthistory"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "viewBillingActivity", new Class[] {
        String.class, String.class, String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/billing/123/viewbillingactivity"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "getInvoiceDetails", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/billing/invoice/123/invoiceDetails"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "showCreditCard", new Class[] {
        Tenant.class, String.class, ModelMap.class, HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/billing/showcreditcarddetails"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "editCreditCard", new Class[] {
        String.class, BillingInfoForm.class, HttpServletResponse.class, HttpServletRequest.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/billing/editcreditcarddetails"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "usageBilling", new Class[] {
        Tenant.class, String.class, String.class, String.class, String.class, String.class, ModelMap.class,
        HttpServletRequest.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/billing/usageBilling"));
    Assert.assertEquals(expected, handler);
    // handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/billing/myUsage"));
    // Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "generateUDR", new Class[] {
        Tenant.class, String.class, String.class, String.class, String.class, ModelMap.class, HttpServletRequest.class,
        HttpServletResponse.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/billing/generateUDR"));
    Assert.assertEquals(expected, handler);

    /*
     * expected = locateMethod(controllerClass, "makePaymentToInvoice", new Class[] { String.class, ModelMap.class });
     * handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/billing/1/make_payment"));
     * Assert.assertEquals(expected, handler);
     */

    expected = locateMethod(controllerClass, "makePayment", new Class[] {
        BigDecimal.class, String.class, HttpSession.class, HttpServletResponse.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/billing/make_payment"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "recordPayment", new Class[] {
        BigDecimal.class, String.class, HttpServletResponse.class, ModelMap.class
    });
    /*
     * handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/billing/1/recordpayment"));
     * Assert.assertEquals(expected, handler); expected = locateMethod(controllerClass, "recordPaymentInvoice", new
     * Class[] { String.class, BigDecimal.class, String.class, HttpServletResponse.class, ModelMap.class });
     */
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/billing/recordpayment"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "chargeBack", new Class[] {
        String.class, String.class, HttpServletRequest.class, HttpServletResponse.class, ModelMap.class
    });
    /*
     * handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/billing/chargeback"));
     * Assert.assertEquals(expected, handler); expected = locateMethod(controllerClass, "chargeBack", new Class[] {
     * String.class, BigDecimal.class, String.class, HttpServletRequest.class, HttpServletResponse.class, ModelMap.class
     * });
     */
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/billing/chargeback"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "changeAccountType", new Class[] {
        String.class, SetAccountTypeForm.class, HttpServletRequest.class, HttpServletResponse.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/billing/changeaccounttype"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "changeAccountType", new Class[] {
        String.class, ModelMap.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/billing/changeaccounttype"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "recordDeposit", new Class[] {
        String.class, DepositRecordForm.class, BindingResult.class, ModelMap.class, HttpServletResponse.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.POST, "/billing/record_deposit"));
    Assert.assertEquals(expected, handler);

    expected = locateMethod(controllerClass, "sendEmailPdfAccountStatement", new Class[] {
        Tenant.class, String.class, String.class, String.class, String.class, ModelMap.class, HttpServletRequest.class,
        HttpServletResponse.class
    });
    handler = servlet.recognize(getRequestTemplate(HttpMethod.GET, "/billing/sendEmailPdfInvoice"));
    Assert.assertEquals(expected, handler);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void usageTest() throws ConnectorManagementServiceException {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    User user = createTestUserInTenant(tenant);
    tenantService.setOwner(tenant, user);
    asUser(tenant.getOwner());
    // FIXME: fixing compilation errors. call the method appropriately
    String view = controller.usageBilling(tenant, null, null, "1", null, null, map, request);
    Assert.assertEquals("billing.usageBilling", view);
    Assert.assertEquals(1,
        ((LinkedHashMap<BillingPeriodDates, Map<String, Object>>) map.get("billingPeriodDatesList")).size());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void usageTestMoreThanOneBP() throws ConnectorManagementServiceException {
    int billingPeriodParam = 30;
    Configuration configuration = configurationDAO
        .findByName(com.vmops.portal.config.Configuration.Names.com_citrix_cpbm_portal_billing_billingPeriod_type
            .name().replaceAll("_", "."));
    configuration.setValue(new Integer(1).toString());
    configurationDAO.save(configuration);
    configuration = configurationDAO
        .findByName(com.vmops.portal.config.Configuration.Names.com_citrix_cpbm_portal_billing_billingPeriod_config
            .name().replaceAll("_", "."));
    configuration.setValue(new Integer(billingPeriodParam).toString());
    configurationDAO.save(configuration);

    int noOfdays = 120;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 - noOfdays); // TODO is there any API which returns -ve of given integer
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType(), createdAt.getTime());
    User user = createTestUserInTenant(tenant);
    tenantService.setOwner(tenant, user);

    asUser(tenant.getOwner());
    // FIXME: fixing compilation errors. call the method appropriately
    String view = controller.usageBilling(tenant, null, null, "1", null, null, map, request);
    Assert.assertEquals("billing.usageBilling", view);
    Assert.assertEquals((noOfdays / billingPeriodParam) + 1,
        ((LinkedHashMap<BillingPeriodDates, Map<String, Object>>) map.get("billingPeriodDatesList")).size());
  }

  // TODO: Ashishj Fix the test as per new design.
  /*
   * @Test
   * @SuppressWarnings("unchecked") public void usageTestRetrieveLatest() throws ConnectorManagementServiceException {
   * int billingPeriodParam = 30; Configuration configuration =
   * configurationDAO.findByName(com.vmops.portal.config.Configuration
   * .Names.com_citrix_cpbm_portal_billing_billingPeriod_type.name().replaceAll("_", ".")); configuration.setValue(new
   * Integer(BillingPeriodType.FIXED_DATE.ordinal() + 1).toString()); configurationDAO.save(configuration);
   * configuration = configurationDAO.findByName(com.vmops.portal.config.Configuration.Names.
   * com_citrix_cpbm_portal_billing_billingPeriod_config .name().replaceAll("_", ".")); configuration.setValue(new
   * Integer(billingPeriodParam).toString()); configurationDAO.save(configuration); int noOfdays = 120; Calendar
   * createdAt = Calendar.getInstance(); createdAt.add(Calendar.DATE, 0 - noOfdays); //TODO is there any API which
   * returns -ve of given integer Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType(),
   * createdAt.getTime()); User user = createTestUserInTenant(tenant); tenantService.setOwner(tenant, user); //Creating
   * Inovices... BillingPeriodDetail billingPeriodDetails = config.getBillingPeriodDetail(tenant);
   * List<BillingPeriodDates> billingPeriodDatesList = billingPeriodDetails.getAllBillingPeriodDates(new Date()); for
   * (BillingPeriodDates billingPeriodDates : billingPeriodDatesList) { Invoice invoice = new Invoice(); Date
   * currentDate = new Date(); invoice.setType(Type.Subscription); invoice.setAmount(new
   * BigDecimal(random.nextDouble())); invoice.setAmountDue(invoice.getAmount()); invoice.setCreatedAt(currentDate);
   * invoice.setTenant(tenant); invoice.setInvoiceHandle(new Long(random.nextLong()).toString());
   * invoice.setBillingPeriodStartDate(billingPeriodDates.getBillingPeriodStartDate().getTime());
   * invoice.setBillingPeriodEndDate(billingPeriodDates.getBillingPeriodEndDate().getTime()); Calendar generatedOn =
   * (Calendar) billingPeriodDates.getBillingPeriodEndDate().clone(); generatedOn.add(Calendar.DATE, 1);
   * invoice.setGeneratedOn(generatedOn); billingService.updateInvoiceSummary(invoice, createInvoiceItems(tenant,
   * invoice)); invoiceDAO.save(invoice); } asUser(tenant.getOwner()); String view = controller.usageBilling(tenant,
   * "1", null,"1", map, request); Assert.assertEquals("billing.usageBilling", view); Assert.assertEquals((noOfdays /
   * billingPeriodParam) + 1, ((LinkedHashMap<BillingPeriodDates, Map<String, Object>>) map
   * .get("billingPeriodDatesList")).size()); Assert.assertEquals(false, map.get("isCurrent")); Assert.assertEquals("1",
   * map.get("viewBy")); }
   */

  protected List<InvoiceItem> createInvoiceItems(Tenant tenant, Invoice invoice) {

    List<InvoiceItem> items = new ArrayList<InvoiceItem>();
    InvoiceItem invoiceItem = new InvoiceItem();
    // invoiceItem.setAmountAfterDiscount(invoice.getAmount().subtract(invoice.getTaxAmount()));
    // invoiceItem.setAmountAfterTax(invoice.getAmount());
    invoiceItem.setDescription("Testing");
    invoiceItem.setAmount(invoice.getAmount().subtract(invoice.getTaxAmount()));
    // invoiceItem.setChargeType(ChargeType.CHARGE);
    // invoiceItem.setDiscountAmount(new BigDecimal(0));
    // invoiceItem.setTaxAmount(new BigDecimal(0));
    invoiceItem.setQuantity(new BigDecimal(1));
    invoiceItem.setInvoice(invoice);
    invoiceItem.setServiceStartDate(invoice.getServiceStartDate());
    invoiceItem.setServiceEndDate(invoice.getServiceEndDate());
    items.add(invoiceItem);
    return items;
  }

  @Test
  public void testRecordDepositPost() throws Exception {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    tenant.setAccountType(accountTypeDAO.getDefaultRegistrationAccountType());
    tenantService.save(tenant);
    DepositRecordForm recordForm = new DepositRecordForm();
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    recordForm.setReceivedOn(sdf.format(getDaysFromNow(-1)));
    recordForm.setAmount("1000.00");
    BindingResult result = validate(recordForm);
    Assert.assertEquals("validating that the form has no errors", 0, result.getErrorCount());
    DepositRecordForm returnForm = controller.recordDeposit(tenant.getParam(), recordForm, result, map, response);
    // Assert.assertEquals("redirect:/portal/billing/show_record_deposit?tenant=" + tenant.getParam(), view);
    Assert.assertNotNull(returnForm);
    Assert.assertEquals(200, response.getStatus());
    tenantDAO.flush();
    tenantDAO.clear();
    Tenant found = tenantDAO.find(tenant.getId());
    Assert.assertEquals(tenantService.getDepositRecordByTenant(tenant), found.getDeposit());
  }

  @Test
  public void testRecordDepositPostBindFail() throws Exception {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    tenant.setAccountType(accountTypeDAO.getDefaultRegistrationAccountType());
    tenantService.save(tenant);
    DepositRecordForm recordForm = new DepositRecordForm();
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    recordForm.setReceivedOn(sdf.format(getDaysFromNow(-30)));
    recordForm.setAmount("1000.00");
    BindingResult result = validate(recordForm);
    Assert.assertEquals("validating that the form has no errors", 0, result.getErrorCount());
    DepositRecordForm returnForm = controller.recordDeposit(tenant.getParam(), recordForm, result, map, response);
    // Assert.assertEquals("billing.record_deposit", view);
    Assert.assertNotNull(returnForm);
    Assert.assertNotNull(tenant.getDeposit());
  }

  @Test
  public void testShowSubscriptionDetails() {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    tenant.setAccountType(accountTypeDAO.getDefaultRegistrationAccountType());
    tenantService.save(tenant);
    List<ProductBundle> bundles = productBundleService.listProductBundles(0, 0);
    ProductBundle nonVmBundle = null;
    for (ProductBundle bundle : bundles) {
      if (!bundle.getResourceType().getResourceTypeName().equals("VirtualMachine")) {
        nonVmBundle = bundle;
        break;
      }
    }
    Subscription subscription = subscriptionService.createSubscription(tenant.getOwner(), nonVmBundle, null, null,
        false, true, null);
    Assert.assertNotNull(subscription);

    String view = controller.showSubscriptionDetails(subscription.getUuid(), tenant.getUuid(), map);
    Assert.assertEquals("billing.viewSubscription", view);
    Assert.assertEquals(true, map.get("allowTermination"));
    Assert.assertEquals(subscription, map.get("subscription"));
    Assert.assertNotNull(((Subscription) map.get("subscription")).getUtilityCharges());
    Assert.assertTrue(((Subscription) map.get("subscription")).getUtilityCharges().size() > 0);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testShowSubscriptions() {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    tenant.setAccountType(accountTypeDAO.getDefaultRegistrationAccountType());
    tenantService.save(tenant);
    List<ProductBundle> bundles = productBundleService.listProductBundles(0, 0);
    ProductBundle nonVmBundle = null;
    for (ProductBundle bundle : bundles) {
      if (!bundle.getResourceType().getResourceTypeName().equals("VirtualMachine")) {
        nonVmBundle = bundle;
        break;
      }
    }
    Subscription subscription = subscriptionService.createSubscription(tenant.getOwner(), nonVmBundle, null, null,
        false, true, null);
    Assert.assertNotNull(subscription);

    String view = controller.showSubscriptions(tenant, tenant.getUuid(), "1", subscription.getUuid(), null, map,
        request);
    Assert.assertEquals("billing.showSubscriptions", view);
    Assert.assertNotNull(map.get("subscriptions"));
    Assert.assertEquals(1, ((List<Subscription>) map.get("subscriptions")).size());
    Assert.assertEquals(subscription, ((List<Subscription>) map.get("subscriptions")).get(0));
    Assert.assertEquals(subscription.getUuid(), ((List<Subscription>) map.get("subscriptions")).get(0).getUuid());
  }

  @Test
  public void testSendEmailPdfAccountStatement() {

    Tenant tenant = tenantService.getTenantByParam("accountId", "AA000002", false);

    controller.sendEmailPdfAccountStatement(tenant, "1", tenant.getParam(), null, "1", null, request, response);

  }

  @Test
  public void testPopulatePaymentsAndCredits() {
    Tenant tenant = tenantDAO.find(1729l);
    Method method = ReflectionUtils.findMethod(controller.getClass(), "populatePaymentsAndCredits");
    AccountStatement statement = billingService.getAccountStatements(tenant, null, 1, 1, null, null, null).get(0);
    Assert.assertNotNull(statement);
    ReflectionUtils.invokeMethod(method, controller, statement, map, request, response);
    Assert.assertTrue(map.containsKey("payments"));
    Assert.assertTrue(map.containsKey("paymentsTotal"));
    Assert.assertTrue(map.containsKey("creditsIssued"));
    Assert.assertTrue(map.containsKey("creditsIssuedTotal"));
    // TODO Manish: Need to add more asserts after compilation issues are fixed.
  }
}
