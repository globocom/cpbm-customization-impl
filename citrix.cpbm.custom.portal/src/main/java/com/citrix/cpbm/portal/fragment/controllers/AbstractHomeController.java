/*
*  Copyright © 2013 Citrix Systems, Inc.
*  You may not use, copy, or modify this file except pursuant to a valid license agreement from
*  Citrix Systems, Inc.
*/
package com.citrix.cpbm.portal.fragment.controllers;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.citrix.cpbm.core.workflow.model.Task;
import com.citrix.cpbm.core.workflow.service.TaskService;
import com.citrix.cpbm.platform.admin.service.ConnectorConfigurationManager;
import com.citrix.cpbm.platform.admin.service.exceptions.ConnectorManagementServiceException;
import com.citrix.cpbm.platform.util.CssdkConstants;
import com.vmops.internal.service.CustomFieldService;
import com.vmops.internal.service.EventService;
import com.vmops.internal.service.SubscriptionService;
import com.vmops.internal.service.UsageService;
import com.vmops.model.AccountType;
import com.vmops.model.Event;
import com.vmops.model.Invoice;
import com.vmops.model.JobStatus;
import com.vmops.model.Product;
import com.vmops.model.Report;
import com.vmops.model.ServiceInstance;
import com.vmops.model.Subscription;
import com.vmops.model.Tenant;
import com.vmops.model.Tenant.State;
import com.vmops.model.User;
import com.vmops.model.billing.AccountStatement;
import com.vmops.model.billing.UserSpendSummary;
import com.vmops.portal.config.Configuration;
import com.vmops.portal.config.Configuration.Names;
import com.vmops.portal.reports.GenericReport;
import com.vmops.reports.CustomerRankReport;
import com.vmops.reports.NewRegistrationReport;
import com.vmops.service.ConfigurationService;
import com.vmops.service.CurrencyValueService;
import com.vmops.service.JobManagementService;
import com.vmops.service.ProductService;
import com.vmops.service.ReportService;
import com.vmops.service.SystemHealthService;
import com.vmops.service.UserService;
import com.vmops.service.billing.BillingAdminService;
import com.vmops.service.billing.BillingService;
import com.vmops.service.exceptions.UsageServiceException;
import com.vmops.usage.model.ProductUsage;
import com.vmops.utils.DateUtils;
import com.vmops.utils.JSONUtils;
import com.vmops.web.controllers.AbstractAuthenticatedController;
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.interceptors.UserContextInterceptor;

public abstract class AbstractHomeController extends AbstractAuthenticatedController {

  @Autowired
  protected ConnectorConfigurationManager connectorConfigurationManager;

  @Autowired
  private UserService userService;

  @Autowired
  private ReportService reportService;

  @Autowired
  protected EventService eventService;

  @Autowired
  protected ConfigurationService configurationService;

  @Autowired
  private CurrencyValueService currencyValueService;

  @Autowired
  JobManagementService jobManagementService;

  @Autowired
  private SystemHealthService healthService;

  @Autowired
  private CustomFieldService customFieldService;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private BillingService billingService;

  @Autowired
  private TaskService taskService;

  @Autowired
  protected BillingAdminService billingAdminService;

  @Autowired
  protected SubscriptionService subscriptionService;

  @Autowired
  private ProductService productService;

  @Autowired
  private UsageService usageService;

  @RequestMapping(value = {
    "/home"
  })
  public String home(@ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "secondLevel", required = false) boolean secondLevel, ModelMap map, HttpSession session,
      HttpServletRequest request) throws ConnectorManagementServiceException {
    logger.debug("###Entering in home(map) method");

    map.addAttribute("maintenance", healthService.listPlannedNotifications(null));
    map.addAttribute("showWarningOfServiceInstanceNotEnabled", true);

    User user = getCurrentUser();

    // setting login success message only when request come from login page.
    if (session.getAttribute("loginreturn") != null && session.getAttribute("loginreturn").equals("success")) {
      map.addAttribute("return_message", "true");
      map.addAttribute("return_message_type", "info");
      session.removeAttribute("loginreturn");
      // login audit details
      // createloginauditrecord(user, request);
    }

    if (!(config.getBooleanValue(Configuration.Names.com_citrix_cpbm_portal_directory_service_enabled))) {
      if (user.getPassword() == null) {
        map.clear();
        return "redirect:/portal/users/" + user.getParam() + "/myprofile";
      }
    }

    if ((user.getFirstName() == null || user.getFirstName().isEmpty())
        || (user.getLastName() == null || user.getLastName().isEmpty())) {
      map.clear();
      return "redirect:/portal/users/" + user.getParam() + "/myprofile";
    }

    List<String> currentUserServiceCategoryList = userService.getAllAccessibleCloudServiceCategories(user);

    tenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);

    map.addAttribute("userHasCloudServiceAccount", userService.isUserHasAnyActiveCloudService(user));
    if ((userService.hasAnyAuthority(user, "ROLE_ACCOUNT_CRUD", "ROLE_ACCOUNT_MGMT", "ROLE_FINANCE_CRUD"))
        && (Boolean) request.getAttribute("isSurrogatedTenant")) {
      user = tenant.getOwner();
      map.addAttribute("showUserProfile", true);
      setPage(map, Page.CRM_HOME);
    } else {
      setPage(map, Page.HOME);
    }
    fetchCurrencies(map);
    fetchEvents(user, map);

    if (!userService.hasAuthority(user, "ROLE_TICKET_MANAGEMENT") && !tenant.equals(tenantService.getSystemTenant())) {
      if (user.equals(user.getTenant().getOwner()) || userService.hasAuthority(user, "ROLE_ACCOUNT_BILLING_ADMIN")
          || userService.hasAuthority(user, "ROLE_ACCOUNT_ADMIN")) {
        try {

          AccountStatement provisionalAccountStatement = billingService.getOrCreateProvisionalAccountStatement(tenant);
          HashMap<String, Object> spend_vs_budget_chart_data = reportService.getChartData(user, "tenant",
              getSessionLocale(request), provisionalAccountStatement);
          map.addAttribute("chartData", JSONUtils.toJSONString(spend_vs_budget_chart_data));
          map.addAttribute("spend_vs_budget_chart_data_obj", spend_vs_budget_chart_data);

          addToChartData(map, provisionalAccountStatement);

        } catch (Exception e) {
          logger.error("Caught Exception while getting chart data", e);
          map.addAttribute("chartData", null);
        }
      } else {
        try {
          map.addAttribute("chartData",
              JSONUtils.toJSONString(reportService.getChartData(user, "user", getSessionLocale(request))));
        } catch (Exception e) {
          logger.error("Caught Exception while getting chart data", e);
          map.addAttribute("chartData", null);
        }
      }
    }

    // Showing only four tasks on the dashboard
    Map<Task, String> taskUrlMap = taskService.getPendingTasksMap(tenant, user, 1, 4);
    map.addAttribute("taskUrlMap", taskUrlMap);

    map.addAttribute("tickets", null);
    map.addAttribute("totalTickets", 0);
    List<User> usersUnderTenant = userService.list(0, 0, null, null, false, null, tenant.getId().toString(), null);

    map.addAttribute("users", usersUnderTenant);

    map.addAttribute("isOwner", getCurrentUser().equals(tenant.getOwner()));
    map.addAttribute("user", user);
    // check user limit
    int userLimit = tenant.getAccountType().getMaxUsers().intValue();
    int noOfUsers = usersUnderTenant.size();
    if (userLimit >= 0 && noOfUsers >= userLimit) {
      map.addAttribute("isUsersMaxReached", "Y");
    }
    map.addAttribute("currentDate", new Date());
    map.addAttribute("tenant", tenant);

    // Fetching category list and prepending it with All category
    List<String> serviceCategoryList = userService.getAllAccessibleCloudServiceCategories(user);
    serviceCategoryList.retainAll(currentUserServiceCategoryList);
    serviceCategoryList.add(0, CssdkConstants.ALL);
    map.addAttribute("serviceCategoryList", serviceCategoryList);

    // populate custom fields
    customFieldService.populateCustomFields(user);
    customFieldService.populateCustomFields(user.getTenant());

    // Intances added for service health
    List<ServiceInstance> cloudTypeServiceInstances = userService.getCloudServiceInstance(user, null);
    map.addAttribute("cloudTypeServiceInstances", cloudTypeServiceInstances);

    String view = null;
    if (tenant.equals(tenantService.getSystemTenant())) {
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      Calendar start = Calendar.getInstance();
      Calendar end = Calendar.getInstance();
      DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
      start.add(Calendar.DATE, -7);
      parameters.put("startDate", format.format(start.getTime()));
      parameters.put("endDate", format.format(end.getTime()));
      List<AccountType> accountTypeList = tenantService.getAllAccountTypes();
      accountTypeList.remove(tenantService.getAccountTypeByName("SYSTEM"));
      GenericReport nrr = new NewRegistrationReport(parameters, dataSource, accountTypeList, getSessionLocale(request),
          messageSource);

      Report reportFusionNR = null;
      reportFusionNR = reportService.generateFusionReport(nrr);
      reportFusionNR.getAttributes().put("chartId", "HomeNewReg");
      reportFusionNR.getAttributes().put("fusionChartType", "MSColumn2D");
      map.addAttribute("reportFusionNR", reportFusionNR);

      HashMap<String, Object> parametersCR = new HashMap<String, Object>();
      parametersCR.put("month", Calendar.getInstance().get(Calendar.MONTH) + 1);
      parametersCR.put("year", Calendar.getInstance().get(Calendar.YEAR));
      parametersCR.put("defaultCurrency", messageSource.getMessage(
          "currency.symbol." + config.getValue(Names.com_citrix_cpbm_portal_settings_default_currency), null,
          getSessionLocale(request)));
      GenericReport crr = new CustomerRankReport(parametersCR, dataSource, config, getSessionLocale(request),
          messageSource);

      Report reportFusionCR = null;
      reportFusionCR = reportService.generateFusionReport(crr);
      reportFusionCR.getAttributes().put("chartId", "HomeCustRank");
      reportFusionCR.getAttributes().put("fusionChartType", "Column2D");
      map.addAttribute("reportFusionCR", reportFusionCR);
      map.addAttribute("showCloudConsoleLink", true);
      view = "main.home_service_with_second_level";
    } else {
      AccountStatement accountStatement = billingAdminService.getOrCreateProvisionalAccountStatement(tenant);
      map.addAttribute("currentBillingStart", accountStatement.getBillingPeriodStartDate());

      view = "main.home_with_second_level";
    }
    logger.debug("###Exiting home(map) method");

    return view;
  }

  @RequestMapping(value = {
    "/home/get_home_items"
  }, method = RequestMethod.GET)
  public String getHomeItems(@ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "serviceInstanceUUID", required = false) String serviceInstanceUUID, ModelMap map,
      HttpServletRequest request) {

    User user = getCurrentUser();
    boolean isSurrogated = false;
    tenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);
    if ((Boolean) request.getAttribute("isSurrogatedTenant")) {

      user = tenant.getOwner();
      isSurrogated = true;
    }

    List<Map<String, Object>> dashboardItems = new ArrayList<Map<String, Object>>();

    if (isTenantDashboard(tenant, user, isSurrogated)) {
      AccountStatement currentAccountStatement = billingService.getOrCreateProvisionalAccountStatement(tenant);

      if (serviceInstanceUUID == null || serviceInstanceUUID.trim().equals("")) {
        drawServicelessTenantDashboard(tenant, request, dashboardItems, currentAccountStatement);
      } else {
        drawTenantDashboard(tenant, serviceInstanceUUID, request, dashboardItems, currentAccountStatement);
      }

    } else if (isRootDashboard(tenant)) {

      if (serviceInstanceUUID == null || serviceInstanceUUID.trim().equals("")) {
        drawServicelessRootDashboard(request, dashboardItems);
      } else {
        drawRootDashboard(serviceInstanceUUID, request, dashboardItems);
      }

    } else { // normal user
      AccountStatement currentAccountStatement = billingService.getOrCreateProvisionalAccountStatement(tenant);

      if (serviceInstanceUUID == null || serviceInstanceUUID.trim().equals("")) {
        drawServicelessNormalDashboard(map, request, user, dashboardItems, currentAccountStatement);
      } else {
        drawNormalDashboard(tenant, serviceInstanceUUID, request, user, dashboardItems, currentAccountStatement);
      }

    }
    map.addAttribute("dashboardItems", dashboardItems);

    return "home.items.view";
  }

  @RequestMapping(value = "/forum", method = RequestMethod.GET)
  public String forum(HttpServletResponse response, ModelMap map) {
    logger.debug("###Entering in forum(response,map) method @POST");
    // setPage(map, Page.FORUM);
    Cookie cookie = new Cookie("JforumSSO", "User" + getCurrentUser().getId());
    cookie.setMaxAge(-1);
    cookie.setPath("/");
    response.addCookie(cookie);
    map.addAttribute("forumContext", config.getForumContextPath());
    logger.debug("###Exiting forum(response,map) method @POST");
    return "forum.show";
  }

  @RequestMapping(value = "/home/batch/status", method = RequestMethod.GET)
  public String showBatchStatus(ModelMap map) {
    logger.debug("### In showBatchStatus()  start method...");
    setPage(map, Page.ADMIN);
    List<JobStatus> batchList = jobManagementService.getJobStatus();
    map.addAttribute("batchList", batchList);
    logger.debug("### In showBatchStatus()  end");
    return "batch.list";
  }

  @RequestMapping(value = "/home/getgravtars", method = RequestMethod.GET)
  public String getGravtars(@ModelAttribute("currentTenant") Tenant currentTenant,
      @RequestParam(value = "tenant", required = false) String tenantParam, ModelMap map, HttpServletRequest request) {
    logger.debug("###Entering in getGravtars method @GET");

    Tenant tenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);

    List<User> usersForGravatar = userService.list(1, 7, null, null, false, null, tenant.getId().toString(), null);
    List<String> gravatarUrlsForUsers = new ArrayList<String>();
    // now generate the gravatar url for each of these users and return the gravatars
    for (User u : usersForGravatar) {
      String gravatarUrl = generateGravatarUrl(u.getEmail());
      u.setGravatarUrl(gravatarUrl);
      gravatarUrlsForUsers.add(gravatarUrl);
    }
    map.addAttribute("usersForGravatar", usersForGravatar);
    map.addAttribute("gravatars", gravatarUrlsForUsers);
    logger.debug("###Exiting getGravtars method @GET");
    return "home.gravtars.show";
  }

  /**
   * This method is the helper for the gravatar url generation This method gets the user email, trims it for leading and
   * trailing white spaces and generates an md5 hash of this email address, finally returning this newly generated
   * gravatar url in the format http://www.gravatar.com/avatar/<md5 hash>
   */
  private String generateGravatarUrl(String emailAddress) {
    String trimmedEmailAddress = emailAddress.toLowerCase().trim();
    StringBuilder gravatarUrl = new StringBuilder("http://www.gravatar.com/avatar/");
    String md5Hash = md5Hex(trimmedEmailAddress);
    gravatarUrl.append(md5Hash);
    // this part appends the default img
    gravatarUrl.append("?d=http://www.gravatar.com/avatar/6a33002708e4e19578cbadaaae09507d.png");

    return gravatarUrl.toString();
  }

  /**
   * Hex representation of the incoming email address string
   * 
   * @param array -- email address byte array
   * @return -- hex rep string
   */
  private String hex(byte[] array) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < array.length; ++i) {
      sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
    }
    return sb.toString();
  }

  /**
   * This method represents the MD5 Hash of the input message
   * 
   * @param message
   * @return -- md5 encoded string
   */
  private String md5Hex(String message) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      return hex(md.digest(message.getBytes("CP1252")));
    } catch (NoSuchAlgorithmException e) {
    } catch (UnsupportedEncodingException e) {
    }
    return null;
  }

  private void fetchCurrencies(ModelMap map) {
    map.addAttribute("currencyValues", currencyValueService.getCurrencyValues());
  }

  private void fetchEvents(User user, ModelMap map) {
    // get alerts
    List<Event> alerts_for_today = new ArrayList<Event>();
    List<Event> alerts_for_yesterday = new ArrayList<Event>();

    try {
      DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
      String date = formatter.format(new Date());
      alerts_for_today = eventService.showEvents(user, date, 1, 2, null, null, null, null, null, false);
      // alerts_for_today = eventService.showEventsByDate(user, date, 1, 2);

      if (alerts_for_today.size() > 2) {
        map.addAttribute("alerts_for_today", alerts_for_today.subList(0, 2));
      } else {
        map.addAttribute("alerts_for_today", alerts_for_today);
      }
    } catch (Exception e) {
      logger.error(e);
    }

    try {
      DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
      String date = formatter.format(DateUtils.minusOneDay(new Date()));
      alerts_for_yesterday = eventService.showEvents(user, date, 1, 1, null, null, null, null, null, false);

      if (alerts_for_yesterday.size() > 1) {
        map.addAttribute("alerts_for_yesterday", alerts_for_yesterday.subList(0, 1));
      } else {
        map.addAttribute("alerts_for_yesterday", alerts_for_yesterday);
      }
    } catch (Exception e) {
      logger.error(e);
    }
  }

  private void addToChartData(ModelMap map, AccountStatement provisionalAccountStatement) {
    if (!map.containsKey("renewalCharge")) {
      map.addAttribute("renewalCharge", provisionalAccountStatement.getRenewalCharges());
    }

    BigDecimal balanceForward = (BigDecimal) map.get("balanceForward");
    if (!map.containsKey("balanceForward")) {
      balanceForward = provisionalAccountStatement.getBalanceForwardAmount(); // toConfirm
      map.addAttribute("balanceForward", balanceForward);
    }

    BigDecimal paymentsCreditsTotal = (BigDecimal) map.get("paymentsCreditsTotal");
    if (!map.containsKey("paymentsCreditsTotal")) {
      paymentsCreditsTotal = provisionalAccountStatement.getCredits();
      map.addAttribute("paymentsCreditsTotal", paymentsCreditsTotal);
    }

    BigDecimal totalAmount = (BigDecimal) map.get("totalAmount");
    if (!map.containsKey("totalAmount")) {
      totalAmount = provisionalAccountStatement.getNewCharges();
      map.addAttribute("totalAmount", totalAmount);
    }
    BigDecimal currentBalance = (BigDecimal) map.get("currentBalance");
    if (!map.containsKey("currentBalance")) {
      currentBalance = provisionalAccountStatement.getFinalCharges();
      map.addAttribute("currentBalance", currentBalance);
    }
  }

  private boolean isTenantDashboard(Tenant tenant, User user, boolean isSurrogated) {
    return isSurrogated
        || (!tenant.equals(tenantService.getSystemTenant()) && (user.equals(tenant.getOwner()) || userService
            .hasAuthority(user, "ROLE_ACCOUNT_BILLING_ADMIN")));
  }

  private boolean isRootDashboard(Tenant tenant) {
    return tenant.equals(tenantService.getSystemTenant());
  }

  private void drawNormalDashboard(Tenant tenant, String serviceInstanceUUID, HttpServletRequest request, User user,
      List<Map<String, Object>> dashboardItems, AccountStatement currentAccountStatement) {
    ServiceInstance serviceInstance = null;
    if (serviceInstanceUUID != null) {
      serviceInstance = connectorConfigurationManager.getInstanceByUUID(serviceInstanceUUID);
      int activeSubscriptions = subscriptionService.getCountByState(null, user, serviceInstance,
          Subscription.State.ACTIVE);

      Set<Invoice> invoices = currentAccountStatement.getInvoices(user);
      BigDecimal renewalAmount = BigDecimal.ZERO;
      for (Invoice invoice : invoices) {
        if (invoice.getType().equals(Invoice.Type.Renewal)
            && invoice.getSubscription().getServiceInstance().getUuid().equals(serviceInstanceUUID)
            && invoice.getRemoved() == null) {
          renewalAmount = renewalAmount.add(invoice.getAmount());
        }
      }
      Map<String, Object> dashboardItem1 = new HashMap<String, Object>();
      dashboardItem1.put("itemName",
          messageSource.getMessage("label.active.subscriptions", null, getSessionLocale(request)));
      dashboardItem1.put("itemValue", activeSubscriptions);
      dashboardItem1.put("itemValue2", renewalAmount);
      dashboardItem1.put("itemValue2Type", "currency");
      dashboardItem1.put("itemValue2Prefix",
          messageSource.getMessage("label.dashboard.renewal.value", null, getSessionLocale(request)));
      dashboardItem1.put("itemImage", "dashboard_active_subscriptions");
      dashboardItem1.put("itemUom",
          messageSource.getMessage("label.dashboard.subscriptions", null, getSessionLocale(request)));
      dashboardItems.add(dashboardItem1);

      Map<String, List<BigDecimal>> productInvoiceItemsMap = new HashMap<String, List<BigDecimal>>();

      List<ProductUsage> productUsages = null;
      try {
        productUsages = usageService.getAggregatedProductUsageForUser(tenant, user, null, null, false, null, null,
            serviceInstanceUUID).getProductUsages();
      } catch (UsageServiceException use) {
        // no products found most likely
        logger.error(use);
      }

      if (productUsages != null)
        for (ProductUsage productUsage : productUsages) {
          List<BigDecimal> productUsageAmountAndQuantity = new ArrayList<BigDecimal>();
          productUsageAmountAndQuantity.add(productUsage.getRawUsage());
          productUsageAmountAndQuantity.add(productUsage.getRatedUsage());
          productInvoiceItemsMap.put(productUsage.getProductCode(), productUsageAmountAndQuantity);
        }

      List<Product> products = productService.listProductsByServiceInstance(serviceInstance, 0, 60);// adding
      // perPage as 60 for now so that dashboard UI pagination doesn't break
      int productImageCount = 0;
      BigDecimal totalSpendByInstance = BigDecimal.ZERO;
      for (Product product : products) {
        Map<String, Object> productValues = new HashMap<String, Object>();
        if (productInvoiceItemsMap.containsKey(product.getCode())) {
          float quantityTotal = Math.round(productInvoiceItemsMap.get(product.getCode()).get(0).floatValue() * 100.0f) / 100.0f;
          BigDecimal amount = productInvoiceItemsMap.get(product.getCode()).get(1);
          totalSpendByInstance = totalSpendByInstance.add(amount);
          productValues.put("itemValue", quantityTotal);// quantity
          productValues.put("itemValue2", amount);// amount
          productValues.put("itemValue2Type", "currency");
        }
        String customImagePath = product.getImagePath();
        if (customImagePath != null) {
          productValues.put("itemCustomImage", "/portal/portal/logo/product/" + product.getId());
          productValues.put("itemId", product.getId());
        } else {
          if (productImageCount == 3) {
            productImageCount = 0;
          }
          productImageCount = productImageCount + 1;
          productValues.put("itemImage", "dashboard_product" + productImageCount);
        }

        productValues.put("itemName", product.getName());

        productValues.put("itemValueType", "product");
        productValues.put("itemUom", product.getUom());
        dashboardItems.add(productValues);
      }

      Map<String, Object> dashboardItem2 = new HashMap<String, Object>();
      dashboardItem2.put("itemName", messageSource.getMessage("label.total.spend", null, getSessionLocale(request)));
      dashboardItem2.put("itemValue", totalSpendByInstance);
      dashboardItem2.put("itemValueType", "currency");
      dashboardItem2.put("itemImage", "dashboard_total_spend");

      dashboardItems.add(1, dashboardItem2);

    }
  }

  private void drawServicelessNormalDashboard(ModelMap map, HttpServletRequest request, User user,
      List<Map<String, Object>> dashboardItems, AccountStatement currentAccountStatement) {
    BigDecimal spendToDate;
    int totalSubscriptions = subscriptionService.getCountByState(null, user, null, Subscription.State.ACTIVE);
    UserSpendSummary userSpendSummary = currentAccountStatement.getSpendSummaryByUser(user);
    spendToDate = userSpendSummary.getSpendToMonth();

    map.addAttribute("currentSpend", spendToDate);
    Map<String, Object> dashboardItem1 = new HashMap<String, Object>();

    dashboardItem1.put("itemName",
        messageSource.getMessage("label.active.subscriptions", null, getSessionLocale(request)));
    dashboardItem1.put("itemValue", totalSubscriptions);
    dashboardItem1.put("itemImage", "dashboard_active_subscriptions");
    dashboardItem1.put("itemUom",
        messageSource.getMessage("label.dashboard.subscriptions", null, getSessionLocale(request)));
    dashboardItems.add(dashboardItem1);

    Map<String, Object> dashboardItem2 = new HashMap<String, Object>();

    dashboardItem2.put("itemName", messageSource.getMessage("label.total.spend", null, getSessionLocale(request)));
    dashboardItem2.put("itemValue", spendToDate);
    dashboardItem2.put("itemValueType", "currency");
    dashboardItem2.put("itemImage", "dashboard_total_spend");
    dashboardItems.add(dashboardItem2);
  }

  private void drawRootDashboard(String serviceInstanceUUID, HttpServletRequest request,
      List<Map<String, Object>> dashboardItems) {
    int totalActiveTenants = tenantService.count(State.ACTIVE.getName(), serviceInstanceUUID);
    ServiceInstance serviceInstance = connectorConfigurationManager.getInstanceByUUID(serviceInstanceUUID);
    int activeUsers = userService.count(true, serviceInstanceUUID);
    int totalSubscriptions = subscriptionService.getCountByState(null, null, serviceInstance,
        com.vmops.model.Subscription.State.ACTIVE);

    Map<String, Object> dashboardItem1 = new HashMap<String, Object>();
    dashboardItem1.put("itemName", messageSource.getMessage("label.active.customers", null, getSessionLocale(request)));
    dashboardItem1.put("itemValue", totalActiveTenants);
    dashboardItem1.put("itemImage", "dashboard_active_customers");
    dashboardItem1.put("itemUom",
        messageSource.getMessage("label.dashboard.customers", null, getSessionLocale(request)));
    dashboardItems.add(dashboardItem1);

    Map<String, Object> dashboardItem2 = new HashMap<String, Object>();
    dashboardItem2.put("itemName", messageSource.getMessage("label.active.users", null, getSessionLocale(request)));
    dashboardItem2.put("itemValue", activeUsers);
    dashboardItem2.put("itemImage", "dashboard_active_users");
    dashboardItem2.put("itemUom", messageSource.getMessage("label.dashboard.users", null, getSessionLocale(request)));
    dashboardItems.add(dashboardItem2);

    Map<String, Object> dashboardItem3 = new HashMap<String, Object>();
    dashboardItem3.put("itemName",
        messageSource.getMessage("label.active.subscriptions", null, getSessionLocale(request)));
    dashboardItem3.put("itemValue", totalSubscriptions);
    dashboardItem3.put("itemImage", "dashboard_active_subscriptions");
    dashboardItem3.put("itemUom",
        messageSource.getMessage("label.dashboard.subscriptions", null, getSessionLocale(request)));
    dashboardItems.add(dashboardItem3);
  }

  private void drawServicelessRootDashboard(HttpServletRequest request, List<Map<String, Object>> dashboardItems) {
    // subtracting system tenant
    int totalActiveTenants = tenantService
        .count(null, State.ACTIVE.getName(), null, null, null, null, null, null, null);
    totalActiveTenants = ((totalActiveTenants - 1) < 0) ? 0 : (totalActiveTenants - 1);
    // subtracting only user 'root' and 'portal'
    int activeUsers = userService.count(null, true, null, null);
    int totalSubscriptions = subscriptionService.getCountByState(null, null, null,
        com.vmops.model.Subscription.State.ACTIVE);

    Map<String, Object> dashboardItem1 = new HashMap<String, Object>();
    dashboardItem1.put("itemName", messageSource.getMessage("label.active.customers", null, getSessionLocale(request)));
    dashboardItem1.put("itemValue", totalActiveTenants);
    dashboardItem1.put("itemImage", "dashboard_active_customers");
    dashboardItem1.put("itemUom",
        messageSource.getMessage("label.dashboard.customers", null, getSessionLocale(request)));
    dashboardItems.add(dashboardItem1);

    Map<String, Object> dashboardItem2 = new HashMap<String, Object>();
    dashboardItem2.put("itemName", messageSource.getMessage("label.active.users", null, getSessionLocale(request)));
    dashboardItem2.put("itemValue", activeUsers);
    dashboardItem2.put("itemImage", "dashboard_active_users");
    dashboardItem2.put("itemUom", messageSource.getMessage("label.dashboard.users", null, getSessionLocale(request)));
    dashboardItems.add(dashboardItem2);

    Map<String, Object> dashboardItem3 = new HashMap<String, Object>();
    dashboardItem3.put("itemName",
        messageSource.getMessage("label.active.subscriptions", null, getSessionLocale(request)));
    dashboardItem3.put("itemValue", totalSubscriptions);
    dashboardItem3.put("itemValue2", billingAdminService.getTotalRenewalAmount());
    dashboardItem3.put("itemValue2Type", "currency");
    dashboardItem3.put("itemValue2Prefix",
        messageSource.getMessage("label.dashboard.renewal.value", null, getSessionLocale(request)));
    dashboardItem3.put("itemImage", "dashboard_active_subscriptions");
    dashboardItem3.put("itemUom",
        messageSource.getMessage("label.dashboard.subscriptions", null, getSessionLocale(request)));
    dashboardItems.add(dashboardItem3);
  }

  private void drawTenantDashboard(Tenant tenant, String serviceInstanceUUID, HttpServletRequest request,
      List<Map<String, Object>> dashboardItems, AccountStatement currentAccountStatement) {
    ServiceInstance serviceInstance = null;
    if (serviceInstanceUUID != null) {
      serviceInstance = connectorConfigurationManager.getInstanceByUUID(serviceInstanceUUID);
      int activeSubscriptions = subscriptionService.getCountByState(tenant, null, serviceInstance,
          Subscription.State.ACTIVE);

      Set<Invoice> invoices = currentAccountStatement.getRenewalInvoices();
      BigDecimal renewalAmount = BigDecimal.ZERO;
      for (Invoice invoice : invoices) {
        if (invoice.getSubscription().getServiceInstance().getUuid().equals(serviceInstanceUUID)) {
          renewalAmount = renewalAmount.add(invoice.getAmount());
        }
      }
      Map<String, Object> dashboardItem1 = new HashMap<String, Object>();
      dashboardItem1.put("itemName",
          messageSource.getMessage("label.active.subscriptions", null, getSessionLocale(request)));
      dashboardItem1.put("itemValue", activeSubscriptions);
      dashboardItem1.put("itemValue2", renewalAmount);
      dashboardItem1.put("itemValue2Type", "currency");
      dashboardItem1.put("itemValue2Prefix",
          messageSource.getMessage("label.dashboard.renewal.value", null, getSessionLocale(request)));
      dashboardItem1.put("itemImage", "dashboard_active_subscriptions");
      dashboardItem1.put("itemUom",
          messageSource.getMessage("label.dashboard.subscriptions", null, getSessionLocale(request)));
      dashboardItems.add(dashboardItem1);

      Map<String, List<BigDecimal>> productInvoiceItemsMap = new HashMap<String, List<BigDecimal>>();

      List<ProductUsage> productUsages = null;
      try {
        productUsages = usageService.getAggregatedProductUsageForTenant(tenant, null, null, false, null, null,
            serviceInstanceUUID).getProductUsages();
      } catch (UsageServiceException use) {
        // no products found most likely
        logger.error(use);
      }

      if (productUsages != null)
        for (ProductUsage productUsage : productUsages) {
          List<BigDecimal> productUsageAmountAndQuantity = new ArrayList<BigDecimal>();
          productUsageAmountAndQuantity.add(productUsage.getRawUsage());
          productUsageAmountAndQuantity.add(productUsage.getRatedUsage());
          productInvoiceItemsMap.put(productUsage.getProductCode(), productUsageAmountAndQuantity);
        }

      List<Product> products = productService.listProductsByServiceInstance(serviceInstance, 0, 60);// adding
      // perPage as 60 for now so that dashboard UI pagination doesn't break
      int productImageCount = 0;

      BigDecimal totalSpendByTenantByInstance = BigDecimal.ZERO;
      for (Product product : products) {
        Map<String, Object> productValues = new HashMap<String, Object>();
        if (productInvoiceItemsMap.containsKey(product.getCode())) {
          float quantityTotal = Math.round(productInvoiceItemsMap.get(product.getCode()).get(0).floatValue() * 100.0f) / 100.0f;
          BigDecimal amount = productInvoiceItemsMap.get(product.getCode()).get(1);
          totalSpendByTenantByInstance = totalSpendByTenantByInstance.add(amount);
          productValues.put("itemValue", quantityTotal);// quantity
          productValues.put("itemValue2", amount);// amount
          productValues.put("itemValue2Type", "currency");
        }
        String customImagePath = product.getImagePath();
        if (customImagePath != null) {
          productValues.put("itemCustomImage", "/portal/portal/logo/product/" + product.getId());
          productValues.put("itemId", product.getId());
        } else {
          if (productImageCount == 3) {
            productImageCount = 0;
          }
          productImageCount = productImageCount + 1;
          productValues.put("itemImage", "dashboard_product" + productImageCount);
        }
        productValues.put("itemName", product.getName());
        productValues.put("itemValueType", "product");
        productValues.put("itemUom", product.getUom());
        dashboardItems.add(productValues);
      }

      Map<String, Object> dashboardItem2 = new HashMap<String, Object>();
      dashboardItem2.put("itemName", messageSource.getMessage("label.total.spend", null, getSessionLocale(request)));
      dashboardItem2.put("itemValue", totalSpendByTenantByInstance);
      dashboardItem2.put("itemValueType", "currency");
      dashboardItem2.put("itemImage", "dashboard_total_spend");
      dashboardItems.add(1, dashboardItem2);
    }
  }

  private void drawServicelessTenantDashboard(Tenant tenant, HttpServletRequest request,
      List<Map<String, Object>> dashboardItems, AccountStatement currentAccountStatement) {
    BigDecimal spendToDate;
    int activeUsers = userService.count(null, true, tenant.getId().toString(), null);
    int totalSubscriptions = subscriptionService.getCountByState(tenant, null, null, Subscription.State.ACTIVE);

    spendToDate = currentAccountStatement.getFinalCharges();

    Map<String, Object> dashboardItem1 = new HashMap<String, Object>();
    dashboardItem1.put("itemName", messageSource.getMessage("label.active.users", null, getSessionLocale(request)));
    dashboardItem1.put("itemValue", activeUsers);
    dashboardItem1.put("itemImage", "dashboard_active_users");
    dashboardItem1.put("itemUom", messageSource.getMessage("label.dashboard.users", null, getSessionLocale(request)));
    dashboardItems.add(dashboardItem1);

    Map<String, Object> dashboardItem2 = new HashMap<String, Object>();
    dashboardItem2.put("itemName",
        messageSource.getMessage("label.active.subscriptions", null, getSessionLocale(request)));
    dashboardItem2.put("itemValue", totalSubscriptions);
    dashboardItem2.put("itemValue2", currentAccountStatement.getRenewalCharges());
    dashboardItem2.put("itemValue2Type", "currency");
    dashboardItem2.put("itemValue2Prefix",
        messageSource.getMessage("label.dashboard.renewal.value", null, getSessionLocale(request)));
    dashboardItem2.put("itemImage", "dashboard_active_subscriptions");
    dashboardItem2.put("itemUom",
        messageSource.getMessage("label.dashboard.subscriptions", null, getSessionLocale(request)));
    dashboardItems.add(dashboardItem2);

    Map<String, Object> dashboardItem3 = new HashMap<String, Object>();
    dashboardItem3.put("itemName", messageSource.getMessage("label.total.spend", null, getSessionLocale(request)));
    dashboardItem3.put("itemValue", spendToDate);
    dashboardItem3.put("itemValueType", "currency");
    dashboardItem3.put("itemImage", "dashboard_total_spend");

    dashboardItems.add(dashboardItem3);
  }

  /*
   * private void createLoginAuditRecord(User user, HttpServletRequest request) { UserLoginAudit userLoginAudit = new
   * UserLoginAudit(user, getRemoteUserIp(request)); List<TestModel> data = testModelService.listTestModel(); TestModel
   * testModel = new TestModel(new Date(), user); testModelService.save(testModel);
   * userLoginAuditService.saveAudit(userLoginAudit); }
   */
}
