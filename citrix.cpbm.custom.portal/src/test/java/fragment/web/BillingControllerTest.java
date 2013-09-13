/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
package fragment.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;

import web.WebTestsBaseWithMockConnectors;

import com.citrix.cpbm.platform.admin.service.exceptions.ConnectorManagementServiceException;
import com.citrix.cpbm.platform.bootstrap.service.BootstrapActivator;
import com.citrix.cpbm.platform.spi.CloudConnector;
import com.citrix.cpbm.platform.spi.SubscriptionLifecycleHandler;
import com.citrix.cpbm.portal.fragment.controllers.BillingController;
import com.vmops.config.BillingPostProcessor;
import com.vmops.internal.service.PaymentGatewayService;
import com.vmops.internal.service.SubscriptionService;
import com.vmops.model.AccountType;
import com.vmops.model.Address;
import com.vmops.model.Configuration;
import com.vmops.model.CreditCard;
import com.vmops.model.Event;
import com.vmops.model.Invoice;
import com.vmops.model.InvoiceItem;
import com.vmops.model.ProductBundle;
import com.vmops.model.Service;
import com.vmops.model.Subscription;
import com.vmops.model.Tenant;
import com.vmops.model.User;
import com.vmops.model.billing.AccountStatement;
import com.vmops.model.billing.SalesLedgerRecord;
import com.vmops.model.billing.SalesLedgerRecord.Type;
import com.vmops.persistence.ConfigurationDAO;
import com.vmops.persistence.EventDAO;
import com.vmops.persistence.billing.AccountStatementDAO;
import com.vmops.service.ProductBundleService;
import com.vmops.service.billing.BillingAdminService;
import com.vmops.service.billing.BillingService;
import com.vmops.web.forms.BillingInfoForm;
import com.vmops.web.forms.DepositRecordForm;
import com.vmops.web.forms.SetAccountTypeForm;

import common.MockCloudInstance;
import common.MockPaymentGatewayService;

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
  AccountStatementDAO accountStatementDAO;

  @Autowired
  BillingAdminService billingAdminService;

  @Autowired
  private ProductBundleService productBundleService;

  @Autowired
  private BillingService billingService;

  @Autowired
  EventDAO eventDAO;

  @Autowired
  private BillingPostProcessor billingPostProcessor;

  private BootstrapActivator bootstrapActivator = new BootstrapActivator();

  private static boolean isMockInstanceCreated = false;

  private PaymentGatewayService ossConnector = null;

  private CloudConnector iaasConnector = null;

  private SubscriptionLifecycleHandler mockSubscriptionLifecycleHandler;

  private HttpSession session;

  private MockPaymentGatewayService paymentGatewayService;

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
  public void prepareMock() {

    MockCloudInstance mock = this.getMockCloudInstance();
    CloudConnector connector = mock.getCloudConnector();
    EasyMock.replay(connector);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void usageTest() throws ConnectorManagementServiceException {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    User user = createTestUserInTenant(tenant);
    tenantService.setOwner(tenant, user);
    asUser(tenant.getOwner());
    String view = controller.usageBilling(tenant, null, null, "1", null, null, map, request);
    Assert.assertEquals("billing.usageBilling", view);
    Assert.assertEquals(true, map.containsAttribute("userHasCloudServiceAccount"));
    Assert.assertEquals(false, map.containsAttribute("showUserProfile"));
    Assert.assertEquals(true, map.containsAttribute("accountStatements"));
    Assert.assertEquals(true, map.containsAttribute("accountStatementUuid"));
    Assert.assertEquals(true, map.containsAttribute("accountStatementState"));
    Assert.assertEquals(true, map.containsAttribute("payments"));
    Assert.assertEquals(true, map.containsAttribute("creditsIssued"));
    Assert.assertEquals(true, map.containsAttribute("bigPaymentsSum"));
    Assert.assertEquals(map.get("isSystemProviderUser"), new String("N"));
    Assert.assertEquals(map.get("userHasCloudServiceAccount"), Boolean.valueOf(false));
    List<AccountStatement> accountStatements = (List<AccountStatement>) map.get("accountStatements");
    Assert.assertEquals(accountStatements.size(), Integer.parseInt("1"));
    Assert.assertEquals(map.get("accountStatementUuid"), accountStatements.get(0).getUuid());
    Assert.assertEquals(map.get("accountStatementState"), accountStatements.get(0).getState().name());
    Assert.assertEquals(map.get("newBigAmount"), BigDecimal.ZERO);
    List<InvoiceItem> items = new ArrayList<InvoiceItem>();
    InvoiceItem invoiceItem = new InvoiceItem();
    invoiceItem.setDescription("Testing");
    items.add(invoiceItem);

  }

  @SuppressWarnings("unchecked")
  @Test
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
    createdAt.add(Calendar.DATE, 0 - noOfdays);
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType(), createdAt.getTime());
    User user = createTestUserInTenant(tenant);
    tenantService.setOwner(tenant, user);

    asUser(tenant.getOwner());
    String view = controller.usageBilling(tenant, null, null, "1", null, null, map, request);
    Assert.assertEquals("billing.usageBilling", view);

  }

  // TODO: Ashishj Fix the test as per new design.
  /*
   * @Test
   * @SuppressWarnings("unchecked") public void usageTestRetrieveLatest() throws ConnectorManagementServiceException {
   * int billingPeriodParam = 30; Configuration configuration =
   * configurationDAO.findByName(com.vmops.portal.config.Configuration
   * .Names.com_citrix_cpbm_portal_billing_billingPeriod_type .name().replaceAll("_", ".")); configuration.setValue(new
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
   * invoice.setTenant(tenant); invoice.setInvoiceHandle(new Long(random.nextLong()).toString()); invoice.
   * setBillingPeriodStartDate(billingPeriodDates.getBillingPeriodStartDate ().getTime());
   * invoice.setBillingPeriodEndDate(billingPeriodDates.getBillingPeriodEndDate ().getTime()); Calendar generatedOn =
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
    DepositRecordForm recordForm = new DepositRecordForm();
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    recordForm.setReceivedOn(sdf.format(getDaysFromNow(-1)));
    recordForm.setAmount("1000.00");
    BindingResult result = validate(recordForm);
    Assert.assertEquals("validating that the form has no errors", 0, result.getErrorCount());
    DepositRecordForm returnForm = controller.recordDeposit(tenant.getParam(), recordForm, result, map, response);
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
    recordForm.setReceivedOn(sdf.format(getDaysFromNow(-3)));
    recordForm.setAmount("1000.00");
    BindingResult result = validate(recordForm);
    Assert.assertEquals("validating that the form has no errors", 0, result.getErrorCount());
    DepositRecordForm returnForm = controller.recordDeposit(tenant.getParam(), recordForm, result, map, response);
    Assert.assertNotNull(returnForm);
    Assert.assertNull(tenant.getDeposit()); // ask ??
  }

  @Test
  public void testShowSubscriptionDetails() {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    tenant.setAccountType(accountTypeDAO.getDefaultRegistrationAccountType());
    tenantService.update(tenant);

    Subscription sub = subscriptionService.locateSubscriptionById(2L);
    String view = controller.showSubscriptionDetails(sub.getUuid(), tenant.getUuid(), map);
    Assert.assertEquals("billing.viewSubscription", view);
    Assert.assertEquals(true, map.get("allowTermination"));
    Assert.assertEquals(true, map.get("toProvision"));
    Assert.assertEquals(sub.getState().toString(), "ACTIVE");
    Assert.assertNotNull(((Subscription) map.get("subscription")).getUtilityCharges());
    Assert.assertTrue(((Subscription) map.get("subscription")).getUtilityCharges().size() > 0);
  }

  @Test
  public void testSendEmailPdfAccountStatement() {
    Tenant tenant = tenantDAO.find(2L);
    List<Event> list = eventDAO.findByTenant(tenant);
    asUser(tenant.getOwner());
    Assert.assertEquals(list.size(), 0);

    AccountStatement accountStatement = accountStatementDAO.find(17291L);
    request.setAttribute("effectiveTenant", tenant);
    controller.sendEmailPdfAccountStatement(tenant, "1", tenant.getParam(), accountStatement.getUuid(), "1", map,
        request, response);
    Assert.assertNotNull(tenant);
    list = eventDAO.findByTenant(tenant);
    Assert.assertNotNull(list);
    Assert.assertEquals(list.size(), 1);

  }

  @Test
  public void testGeneratePdfAccountStatement() {

    Tenant tenant = tenantDAO.find(2L);
    AccountStatement accountStatement = accountStatementDAO.find(17291L);
    request.setAttribute("effectiveTenant", tenant);
    String view = controller.generatePdfAccountStatement(tenant, "1", tenant.getParam(), accountStatement.getUuid(),
        "1", map, request, response);
    Assert.assertEquals(response.getContentType(), new String("application/pdf"));
    Assert.assertNull(view);
  }

  @Test
  public void testGenerateUDR() {

    Tenant tenant = tenantDAO.find(2L);
    AccountStatement accountStatement = accountStatementDAO.find(17291L);
    request.setAttribute("effectiveTenant", tenant);
    controller.generateUDR(tenant, "1", tenant.getParam(), accountStatement.getUuid(), "1", map, request, response);
    Assert.assertEquals(response.getContentType(), new String("application/xml"));
    Configuration conf = configurationDAO.findByName("com.citrix.cpbm.portal.billing.export.fileExtension");
    conf.setValue("csv");
    configurationDAO.save(conf);
    controller.generateUDR(tenant, "1", tenant.getParam(), accountStatement.getUuid(), "1", map, request, response);
    Assert.assertEquals(response.getContentType(), new String("text/csv"));

  }

  @Test
  public void testShowSubscriptions() {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    tenant.setAccountType(accountTypeDAO.getDefaultRegistrationAccountType());
    User user = createTestUserInTenant(tenant);
    tenantService.setOwner(tenant, user);
    asUser(tenant.getOwner());
    tenantService.update(tenant);

    Subscription sub = subscriptionService.locateSubscriptionById(2L);
    request.setAttribute("isSurrogatedTenant", Boolean.TRUE);
    request.setAttribute("effectiveTenant", tenant);
    String view = controller.showSubscriptions(tenant.getUuid(), "1", sub.getUuid(), sub.getState().toString(),
        user.getUuid(), map, request);
    Assert.assertEquals("billing.showSubscriptions", view);
    Assert.assertEquals(null, map.get("allowTermination"));
    Assert.assertEquals(sub.getState().toString(), "ACTIVE");
  }

  @Test
  public void testTerminateSubscription() {

    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    tenant.setAccountType(accountTypeDAO.getDefaultRegistrationAccountType());
    User user = createTestUserInTenant(tenant);
    tenantService.setOwner(tenant, user);
    List<ProductBundle> bundles = productBundleService.listProductBundles(0, 0);
    ProductBundle nonVmBundle = null;
    for (ProductBundle bundle : bundles) {
      if (!bundle.getResourceType().getResourceTypeName().equals("VirtualMachine")) {
        nonVmBundle = bundle;
        break;
      }
    }
    Subscription subscription = subscriptionService.createSubscription(tenant.getOwner(), nonVmBundle, null, null,
        false, false, null, new HashMap<String, String>());
    Assert.assertNotNull(subscription);

    Subscription sub = controller.terminateSubscription(subscription.getParam(), map);
    Assert.assertNotNull(sub);
    Assert.assertEquals(new Date().toString(), sub.getTerminationDateWithTime().toString());
    Assert.assertEquals(sub.getState().name(), new String("EXPIRED"));

  }

  @Test
  public void testCancelSubscription() {

    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    tenant.setAccountType(accountTypeDAO.getDefaultRegistrationAccountType());
    User user = createTestUserInTenant(tenant);
    tenantService.setOwner(tenant, user);
    List<ProductBundle> bundles = productBundleService.listProductBundles(0, 0);
    ProductBundle nonVmBundle = null;
    for (ProductBundle bundle : bundles) {
      if (!bundle.getResourceType().getResourceTypeName().equals("VirtualMachine")) {
        nonVmBundle = bundle;
        break;
      }
    }
    Subscription subscription = subscriptionService.createSubscription(tenant.getOwner(), nonVmBundle, null, null,
        false, false, null, new HashMap<String, String>());
    Assert.assertNotNull(subscription);

    Subscription sub = controller.cancelSubscription(subscription.getParam(), map);
    Assert.assertNotNull(sub);
    Assert.assertEquals(sub.getState().name(), new String("CANCELED"));

  }

  @Test
  public void testShowHistory() {
    session = new MockHttpSession();
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    tenant.setAccountType(accountTypeDAO.getDefaultRegistrationAccountType());
    User user = createTestUserInTenant(tenant);
    tenantService.setOwner(tenant, user);
    List<ProductBundle> bundles = productBundleService.listProductBundles(0, 0);
    ProductBundle nonVmBundle = null;
    for (ProductBundle bundle : bundles) {
      if (!bundle.getResourceType().getResourceTypeName().equals("VirtualMachine")) {
        nonVmBundle = bundle;
        break;
      }
    }
    Subscription subscription = subscriptionService.createSubscription(tenant.getOwner(), nonVmBundle, null, null,
        false, false, null, new HashMap<String, String>());
    Assert.assertNotNull(subscription);
    request.setAttribute("isSurrogatedTenant", Boolean.TRUE);
    request.setAttribute("effectiveTenant", tenant);
    session.setAttribute("makepayment_status", "false");
    String view = controller.showHistory(tenant, tenant.getParam(), user.getParam(), "1", session, map, request);
    Assert.assertNotNull(view);
    Assert.assertEquals(false, view.contentEquals("showUserProfile"));
    Assert.assertEquals(false, view.contentEquals("userHasCloudServiceAccount"));
    List<AccountStatement> accountStatements = (List<AccountStatement>) map.get("AccountStatement");
    Assert.assertEquals(map.get("currentUser"), getRootUser());
    Assert.assertNotNull(map.get("billingActivities"));
    Assert.assertNotNull(map.get("users"));
    Assert.assertEquals(map.get("showUsers"), true);
    Assert.assertNotNull(map.get("tenant"));
    Assert.assertNotNull(map.get("statusMessage"));
    Assert.assertNull(map.get("makepayment_status"));

  }

  @Test
  public void testShowHistory1() {
    session = new MockHttpSession();

    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    tenant.setAccountType(accountTypeDAO.getDefaultRegistrationAccountType());
    User user = createTestUserInTenant(tenant);
    tenantService.setOwner(tenant, user);
    List<ProductBundle> bundles = productBundleService.listProductBundles(0, 0);
    ProductBundle nonVmBundle = null;
    for (ProductBundle bundle : bundles) {
      if (!bundle.getResourceType().getResourceTypeName().equals("VirtualMachine")) {
        nonVmBundle = bundle;
        break;
      }
    }
    Subscription subscription = subscriptionService.createSubscription(tenant.getOwner(), nonVmBundle, null, null,
        false, false, null, new HashMap<String, String>());
    Assert.assertNotNull(subscription);
    request.setAttribute("isSurrogatedTenant", Boolean.TRUE);
    request.setAttribute("effectiveTenant", tenant);
    session.setAttribute("makepayment_status", "true");
    String view = controller.showHistory(tenant, tenant.getParam(), user.getParam(), "1", session, map, request);
    Assert.assertNotNull(view);
    Assert.assertEquals(false, view.contentEquals("showUserProfile"));
    Assert.assertEquals(false, view.contentEquals("userHasCloudServiceAccount"));
    List<AccountStatement> accountStatements = (List<AccountStatement>) map.get("AccountStatement");
    Assert.assertEquals(map.get("currentUser"), getRootUser());
    Assert.assertNotNull(map.get("billingActivities"));
    Assert.assertNotNull(map.get("users"));
    Assert.assertEquals(map.get("showUsers"), true);
    Assert.assertNotNull(map.get("tenant"));
    Assert.assertNotNull(map.get("statusMessage"));
    Assert.assertNull(map.get("makepayment_status"));

  }

  @Test
  public void testShowPaymentHistory() {
    session = new MockHttpSession();

    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    tenant.setAccountType(accountTypeDAO.getDefaultRegistrationAccountType());
    User user = createTestUserInTenant(tenant);
    tenantService.setOwner(tenant, user);
    List<ProductBundle> bundles = productBundleService.listProductBundles(0, 0);
    ProductBundle nonVmBundle = null;
    for (ProductBundle bundle : bundles) {
      if (!bundle.getResourceType().getResourceTypeName().equals("VirtualMachine")) {
        nonVmBundle = bundle;
        break;
      }
    }
    Subscription subscription = subscriptionService.createSubscription(tenant.getOwner(), nonVmBundle, null, null,
        false, false, null, new HashMap<String, String>());
    Assert.assertNotNull(subscription);
    request.setAttribute("isSurrogatedTenant", Boolean.TRUE);
    request.setAttribute("effectiveTenant", tenant);
    session.setAttribute("makepayment_status", "true");
    String view = controller.showPaymentHistory(tenant, tenant.getParam(), "1", session, request, map);
    Assert.assertNotNull(view);

    Assert.assertNotNull(map.get("userHasCloudServiceAccount"));
    Assert.assertNotNull(map.get("showUserProfile"));
    Assert.assertNotNull(map.get("errorMsg"));
    Assert.assertNotNull(map.get("tenant"));

    List<SalesLedgerRecord> slrList = (List<SalesLedgerRecord>) map.get("SalesLedgerRecord");

    Assert.assertNotNull(map.get("errorMsg"));
    Assert.assertNotNull(map.get("salesLedgerRecords"));
    Assert.assertNotNull(map.get("enable_next"));
    Assert.assertNotNull(map.get("current_page"));

  }

  @Test
  public void testViewBillingActivity() {

    Tenant tenant1 = tenantDAO.find(20L);
    AccountStatement accountStatement = accountStatementDAO.find(41000L);
    asUser(tenant1.getOwner());

    request.setAttribute("isSurrogatedTenant", Boolean.TRUE);
    request.setAttribute("effectiveTenant", tenant1);
    String view = controller.viewBillingActivity(accountStatement.getUuid(), "Invoice", tenant1.getOwner().getParam(),
        map);

  }

  @Test
  public void testViewSalesLedgerRecord() {
    Tenant tenant = tenantDAO.find(25L);

    BigDecimal creditDecimal = billingPostProcessor.setScaleByCurrency(

    new BigDecimal("100"), tenant.getCurrency());

    SalesLedgerRecord creditSLR = billingAdminService

    .addPaymentOrCredit(tenant, creditDecimal, Type.SERVICE_CREDIT,

    "Payment Done", null);

    String view = controller.viewSalesLedgerRecord(creditSLR.getUuid(), map);
    Assert.assertNotNull(view);
    Assert.assertEquals(view, new String("billing.view.billing.paymentactivity"));
    Assert.assertEquals(map.get("salesLedgerRecord"), creditSLR);
  }

  @Test
  public void testViewShowPaymentHistory() {

    Tenant tenant = tenantDAO.find(25L);
    request.setAttribute("isSurrogatedTenant", Boolean.TRUE);
    request.setAttribute("effectiveTenant", tenant);
    String makePayment = controller.makePayment(new BigDecimal("100"), tenant.getParam(), session, response);
    String view = controller.showPaymentHistory(tenant, tenant.getParam(), "1", session, request, map);
    Assert.assertEquals(map.get("userHasCloudServiceAccount"), Boolean.valueOf(false));
    Assert.assertEquals(map.get("showUserProfile"), Boolean.valueOf(true));
    Assert.assertEquals(map.get("errorMsg"), "");
    Assert.assertEquals(map.get("tenant"), tenant);
    Assert.assertNotNull(map.get("salesLedgerRecords"));
    makePayment = controller.makePayment(new BigDecimal("0"), tenant.getParam(), session, response);
    Assert.assertEquals(makePayment, new String("failed"));

  }

  @Test
  public void testRecordDeposit2() throws Exception {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    DepositRecordForm recordForm = new DepositRecordForm();
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    recordForm.setReceivedOn(sdf.format(getDaysFromNow(-1)));
    recordForm.setAmount("1000.00");
    BindingResult result = validate(recordForm);
    Assert.assertEquals("validating that the form has no errors", 0, result.getErrorCount());
    String returnForm = controller.recordDeposit(tenant.getParam(), tenant, map, request);
    Assert.assertNotNull(returnForm);
    Assert.assertEquals(200, response.getStatus());
    tenantDAO.flush();
    tenantDAO.clear();
    Tenant found = tenantDAO.find(tenant.getId());
    Assert.assertEquals(tenantService.getDepositRecordByTenant(tenant), found.getDeposit());
  }

  @Test
  public void testMakePayment() {

    Tenant tenant = tenantDAO.find(25L);
    String makePayment = controller.makePayment(new BigDecimal("100"), tenant.getParam(), session, response);
    Assert.assertEquals("success", makePayment);
    Assert.assertNotNull("response");

  }

  @Test
  public void testMakePaymentFail() {

    Tenant tenant = tenantDAO.find(25L);
    String makePayment = controller.makePayment(new BigDecimal("-100"), tenant.getParam(), session, response);
    Assert.assertEquals("failed", makePayment);
    Assert.assertNotNull("response");
  }

  @Test
  public void testRecordPayment() {
    Tenant tenant = tenantDAO.find(25L);
    String recordPayment = controller.recordPayment(new BigDecimal("100"), tenant.getParam(), response, map);
    Assert.assertEquals("success", recordPayment);
    Assert.assertNotNull("response");
  }

  @Test
  public void testRecordPaymentFail() {
    Tenant tenant = tenantDAO.find(25L);
    String recordPayment = controller.recordPayment(new BigDecimal("-100"), tenant.getParam(), response, map);
    Assert.assertEquals("failed", recordPayment);
    Assert.assertNotNull("response");
  }

  @Test
  public void testShowRecordDeposit() throws Exception {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    DepositRecordForm recordForm = new DepositRecordForm();
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    recordForm.setReceivedOn(sdf.format(getDaysFromNow(-1)));
    recordForm.setAmount("1000.00");
    BindingResult result = validate(recordForm);
    Assert.assertEquals("validating that the form has no errors", 0, result.getErrorCount());
    DepositRecordForm returnForm = controller.recordDeposit(tenant.getParam(), recordForm, result, map, response);

    String viewRecordDeposit = controller.showRecordDeposit(tenant.getParam(), tenant, map, request);

    Assert.assertNotNull(returnForm);
    Assert.assertEquals(200, response.getStatus());
    tenantDAO.flush();
    tenantDAO.clear();
    Tenant found = tenantDAO.find(tenant.getId());
    Assert.assertEquals(tenantService.getDepositRecordByTenant(tenant), found.getDeposit());
    Assert.assertEquals(map.get("show_deposit_record"), true);
    Assert.assertNotNull(map.get("depositRecord"));
  }

  @Test
  public void testShowCreditCard() {

    Tenant tenant = tenantDAO.find(25L);
    String viewCreditCardDetails = controller.showCreditCard(tenant, tenant.getParam(), map, request);
    Assert.assertNull(map.get("showUserProfile"));
    Assert.assertEquals(map.get("tenant"), tenant);
    Assert.assertEquals(map.get("showChangeAccountType"), true);
    Assert.assertEquals(map.get("errorMsg"), null);
    Assert.assertNotNull(map.get("billingInfo"));
    Assert.assertEquals(map.get("showMessagePendingConversion"), false);
    BillingInfoForm billingInfoForm = (BillingInfoForm) map.get("billingInfo");
    Assert.assertNotNull(billingInfoForm.getCountryList());
    Assert.assertEquals(billingInfoForm.getCreditCard(), billingService.getCreditCard(tenant));
    Assert.assertEquals(viewCreditCardDetails, new String("billing.showCreditCardDetails"));

  }

  @SuppressWarnings("unchecked")
  @Test
  public void testChangeAccountType() throws IOException {

    Tenant tenant = tenantDAO.find(25L);
    String resuString = controller.changeAccountType(tenant.getParam(), map);
    Assert.assertNotNull(resuString);
    Assert.assertEquals("billing.change.account.type", resuString);
    List<AccountType> targetAccountTypes = (List<AccountType>) map.get("targetAccountTypes");
    Assert.assertEquals(1, targetAccountTypes.size());
  }

  @Test
  public void testChangeAccountType1() throws IOException {

    Tenant tenant = tenantDAO.find(25L);
    CreditCard cc = new CreditCard("VISA", "4111111111111111", 3, 2015, "123", tenant.getOwner().getFirstName(),
        VALID_BILLING_ADDRESS);
    SetAccountTypeForm form = new SetAccountTypeForm(tenant);
    form.setAccountTypeName(accountTypeDAO.find(4L).getName());
    form.setBillingAddress(VALID_BILLING_ADDRESS);
    form.setInitialPayment(BigDecimal.TEN);
    form.setCreditCard(cc);
    ModelMap resultMap = controller.changeAccountType(tenant.getParam(), form, request, response, map);
    Assert.assertNotNull(resultMap);
    String result = (String) resultMap.get("redirecturl");
    Assert.assertNotNull(result);
    Assert.assertEquals("/portal/portal/tenants/editcurrent", result);

  }

  @Test
  public void testEditCreditCard() throws IOException {

    paymentGatewayService = new MockPaymentGatewayService();
    Tenant tenant = tenantDAO.find(3L);
    Address billingAddress = paymentGatewayService.getBillingAddress(tenant);
    CreditCard creditCard = paymentGatewayService.getCreditCard(tenant);
    creditCard.setCreditCardNumber("4111111111111444");
    creditCard.setFirstNameOnCard("FName");
    creditCard.setLastNameOnCard("LName");
    BillingInfoForm form = new BillingInfoForm(tenant, billingAddress, creditCard);
    form.setAction("launchvm");
    ModelMap obtainedMap = controller.editCreditCard(tenant.getParam(), form, response, request, map);
    Assert.assertNotNull(obtainedMap);
    Assert.assertTrue(map.containsKey("redirecturl"));
    String redirectURL = (String) map.get("redirecturl");
    Assert.assertEquals("/portal/portal/tenants/editcurrent?action=showcreditcardtab&tenant=" + tenant.getParam(),
        redirectURL);
    Tenant obtainedTenant = (Tenant) map.get("tenant");
    CreditCard newCreditCard = paymentGatewayService.getCreditCard(obtainedTenant);
    Assert.assertEquals("4111111111111444", newCreditCard.getCreditCardNumber());
    Assert.assertEquals("FName", newCreditCard.getFirstNameOnCard());
    Assert.assertEquals("LName", newCreditCard.getLastNameOnCard());
  }

  @Test
  public void testChargeBack() {

    Tenant tenant = tenantDAO.find(25L);
    String result = controller.chargeBack("8c4732b3-b319-4aef-ad46-25f9b187cf11", tenant.getParam(), request, response,
        map);
    Assert.assertEquals("success", result);
    Assert.assertNotNull("response");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void TestUsageInvoice() throws ConnectorManagementServiceException {
    Tenant tenant = createTestTenant(accountTypeDAO.getDefaultRegistrationAccountType());
    User user = createTestUserInTenant(tenant);
    tenantService.setOwner(tenant, user);
    asUser(tenant.getOwner());

    List<ProductBundle> bundles = productBundleService.listProductBundles(0, 0);

    Subscription subscription = subscriptionService.createSubscription(tenant.getOwner(), bundles.get(14), null, null,
        false, false, null, new HashMap<String, String>());
    Assert.assertNotNull(subscription);

    String view = controller.usageBilling(tenant, null, null, "1", null, null, map, request);
    Assert.assertEquals("billing.usageBilling", view);
    Assert.assertEquals(true, map.containsAttribute("userHasCloudServiceAccount"));
    Assert.assertEquals(false, map.containsAttribute("showUserProfile"));
    Assert.assertEquals(true, map.containsAttribute("accountStatements"));
    Assert.assertEquals(true, map.containsAttribute("accountStatementUuid"));
    Assert.assertEquals(true, map.containsAttribute("accountStatementState"));
    Assert.assertEquals(true, map.containsAttribute("payments"));
    Assert.assertEquals(true, map.containsAttribute("creditsIssued"));
    Assert.assertEquals(true, map.containsAttribute("bigPaymentsSum"));
    Assert.assertEquals(map.get("isSystemProviderUser"), new String("N"));
    Assert.assertEquals(map.get("userHasCloudServiceAccount"), Boolean.valueOf(false));
    List<AccountStatement> accountStatements = (List<AccountStatement>) map.get("accountStatements");
    Assert.assertEquals(accountStatements.size(), Integer.parseInt("1"));
    Assert.assertEquals(map.get("accountStatementUuid"), accountStatements.get(0).getUuid());
    Assert.assertEquals(map.get("accountStatementState"), accountStatements.get(0).getState().name());
    Assert.assertEquals(map.get("newBigAmount"), BigDecimal.ZERO);
    List<InvoiceItem> items = new ArrayList<InvoiceItem>();
    InvoiceItem invoiceItem = new InvoiceItem();
    invoiceItem.setDescription("Testing");
    items.add(invoiceItem);

  }
}