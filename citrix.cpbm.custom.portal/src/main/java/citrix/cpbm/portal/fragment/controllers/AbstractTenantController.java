/* Copyright (C) 2013 Citrix Systems, Inc. All rights reserved */
package citrix.cpbm.portal.fragment.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import citrix.cpbm.access.proxy.CustomProxy;
import citrix.cpbm.portal.forms.TenantForm;

import com.citrix.cpbm.core.workflow.model.BusinessTransaction;
import com.citrix.cpbm.core.workflow.model.Task;
import com.citrix.cpbm.core.workflow.service.BusinessTransactionService;
import com.citrix.cpbm.core.workflow.service.TaskService;
import com.citrix.cpbm.platform.admin.service.ConnectorConfigurationManager;
import com.citrix.cpbm.platform.admin.service.exceptions.ConnectorManagementServiceException;
import com.citrix.cpbm.platform.spi.AccountLifecycleHandler;
import com.citrix.cpbm.platform.spi.CloudConnector;
import com.citrix.cpbm.platform.spi.CloudConnectorFactory.ConnectorType;
import com.citrix.cpbm.platform.util.CssdkConstants;
import com.vmops.config.BillingPostProcessor;
import com.vmops.internal.service.CustomFieldService;
import com.vmops.internal.service.EventService;
import com.vmops.internal.service.NotificationService;
import com.vmops.internal.service.PaymentGatewayService;
import com.vmops.model.AccountControlServiceConfigMetadata;
import com.vmops.model.AccountHolder;
import com.vmops.model.AccountType;
import com.vmops.model.CampaignPromotion;
import com.vmops.model.Catalog;
import com.vmops.model.Channel;
import com.vmops.model.Country;
import com.vmops.model.CreditCard;
import com.vmops.model.CurrencyValue;
import com.vmops.model.DepositRecord;
import com.vmops.model.Event;
import com.vmops.model.Event.Category;
import com.vmops.model.Event.Scope;
import com.vmops.model.Event.Severity;
import com.vmops.model.Event.Source;
import com.vmops.model.IPtoCountry;
import com.vmops.model.PaymentMode;
import com.vmops.model.PendingChange;
import com.vmops.model.ResourceLimit;
import com.vmops.model.Service;
import com.vmops.model.ServiceInstance;
import com.vmops.model.SpendAlertSubscription;
import com.vmops.model.SupportedCurrency;
import com.vmops.model.Tenant;
import com.vmops.model.Tenant.State;
import com.vmops.model.TenantHandle;
import com.vmops.model.User;
import com.vmops.model.UserAlertPreferences;
import com.vmops.model.UserAlertPreferences.AlertType;
import com.vmops.model.billing.AccountStatement;
import com.vmops.model.billing.SalesLedgerRecord;
import com.vmops.model.billing.SalesLedgerRecord.Type;
import com.vmops.portal.config.Configuration;
import com.vmops.portal.config.Configuration.Names;
import com.vmops.service.AccountTypeService;
import com.vmops.service.AuthorityService;
import com.vmops.service.ChannelService;
import com.vmops.service.CountryService;
import com.vmops.service.PromotionService;
import com.vmops.service.RegistrationService;
import com.vmops.service.ResourceLimitService;
import com.vmops.service.SearchService;
import com.vmops.service.TenantService;
import com.vmops.service.UserAlertPreferencesService;
import com.vmops.service.UserService;
import com.vmops.service.UserService.Handle;
import com.vmops.service.billing.BillingAdminService;
import com.vmops.service.exceptions.AjaxFormValidationException;
import com.vmops.service.exceptions.BillingAdminServiceException;
import com.vmops.service.exceptions.IPtoCountryException;
import com.vmops.service.exceptions.InternalIPException;
import com.vmops.service.exceptions.InvalidAjaxRequestException;
import com.vmops.service.exceptions.NoManualRegistrationAccountTypeException;
import com.vmops.service.exceptions.ServiceException;
import com.vmops.service.exceptions.TenantStateChangeFailedException;
import com.vmops.service.exceptions.UserLimitServiceException;
import com.vmops.web.controllers.AbstractAuthenticatedController;
import com.vmops.web.controllers.menu.Level1;
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.forms.AdvancedSearchForm;
import com.vmops.web.forms.BillingInfoForm;
import com.vmops.web.forms.CustomAlertForm;
import com.vmops.web.forms.ResourceLimitForm;
import com.vmops.web.forms.SearchForm;
import com.vmops.web.forms.SetAccountTypeForm;
import com.vmops.web.forms.TenantLogoForm;
import com.vmops.web.forms.UserAlertEmailForm;
import com.vmops.web.interceptors.UserContextInterceptor;
import com.vmops.web.validators.AlertValidator;
import com.vmops.web.validators.TenantLogoFormValidator;
import com.vmops.web.validators.ValidationUtil;

public class AbstractTenantController extends AbstractAuthenticatedController {

  @Autowired
  protected BillingAdminService billingAdminService;

  @Autowired
  private BillingPostProcessor billingPostProcessor;

  @Autowired
  protected SearchService searchService;

  @Autowired
  protected CustomFieldService customFieldService;

  @Autowired
  protected RegistrationService registrationService;

  @Autowired
  protected ResourceLimitService resourceLimitService;

  @Autowired
  protected NotificationService notificationService;

  @Autowired
  protected EventService eventService;

  @Autowired
  protected UserAlertPreferencesService userAlertPreferencesService;

  @Autowired
  protected ChannelService channelService;

  @Autowired
  protected AccountTypeService accountTypeService;

  @Autowired
  protected PromotionService promotionService;

  @Autowired
  private MessageSource messageSource;

  @Autowired
  protected AuthorityService authorityScopeService;

  /**
   * Country service.
   */
  @Autowired
  protected CountryService countryService;

  @Autowired
  private ConnectorConfigurationManager connectorConfigurationManager;

  @Autowired
  protected BusinessTransactionService businessTransactionService;

  @Autowired
  private TaskService taskService;

  Logger logger = Logger.getLogger(AbstractTenantController.class);

  protected enum EventType {
    SYSTEM, SUBSCRIBED
  };

  protected enum Resource_Limit_Enum {
    VM_LIMIT, PUBLIC_IP_LIMIT, VOLUME_LIMIT, SNAPSHOT_LIMIT, TEMPLATE_LIMIT
  };

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.setDisallowedFields("state");
  }

  @RequestMapping(value = "/viewtenant", method = RequestMethod.GET)
  public String show(@RequestParam(value = "tenant", required = true) String tenantParam, ModelMap map) {
    logger.debug("###Entering in show(tenantId,map) method @GET");
    Tenant tenant = tenantService.get(tenantParam);
    map.addAttribute("tenant", tenant);
    BigDecimal creditBalance = BigDecimal.ZERO;
    if (!tenant.equals(tenantService.getSystemTenant())) {
      creditBalance = billingAdminService.getOrCreateProvisionalAccountStatement(tenant).getFinalCharges().negate(); // toConfirm
    }
    map.addAttribute("creditBalance", creditBalance);
    User user = getCurrentUser(true);
    Map<Task, String> actions = taskService.getPendingTasksMap(tenant, user, 0, 0);
    map.addAttribute("pendingActions", actions);

    List<ServiceInstance> enabledCSInstances = tenantService.getEnabledCSInstances(tenant);
    map.addAttribute("instances", enabledCSInstances);
    map.addAttribute("services", this.getServices(enabledCSInstances));

    Long userLimit = tenant.getMaxUsers();
    map.addAttribute("userLimit", userLimit); // TODO show userlimit in UI

    logger.debug("###Exiting show(map) method @GET");
    return "tenant.view";
  }

  private Set<Service> getServices(List<ServiceInstance> instances) {
    Set<Service> services = new HashSet<Service>();
    for (ServiceInstance instance : instances) {
      services.add(instance.getService());
    }
    return services;
  }

  @RequestMapping(value = "/edit", method = RequestMethod.GET)
  public String edit(@RequestParam(value = "tenant", required = true) String tenantParam, ModelMap map) {
    logger.debug("### edit method starting...(GET)");
    Tenant tenant = tenantService.get(tenantParam);
    customFieldService.populateCustomFields(tenant);
    citrix.cpbm.access.Tenant proxyTenant = (citrix.cpbm.access.Tenant) CustomProxy.newInstance(tenant);
    TenantForm tenantForm = new TenantForm(proxyTenant);
    tenantForm.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    
    map.addAttribute("tenantForm", tenantForm);
    map.addAttribute("allowSecondaryCheckBox", proxyTenant.getAccountType().isEnableSecondaryAddress());
    if (proxyTenant.getSecondaryAddress() != null) {
      tenantForm.setAllowSecondary(true);
    }
    logger.debug("### edit method end");
    return "tenant.edit";
  }

  @RequestMapping(value = "/editcurrent", method = RequestMethod.GET)
  public String editCurrentTenant(@RequestParam(value = "action", required = false) String action,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "currenttab", required = false) String tabParam, ModelMap map, HttpServletRequest request) {
    logger.debug("### edit current tenant method starting...(GET)");

    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);

    citrix.cpbm.access.Tenant proxyTenant = (citrix.cpbm.access.Tenant) CustomProxy.newInstance(effectiveTenant);
    map.addAttribute("tenant", proxyTenant);
    map.addAttribute("userHasCloudServiceAccount", userService.isUserHasAnyActiveCloudService(proxyTenant.getOwner()));
    customFieldService.populateCustomFields(proxyTenant.getObject());
    TenantForm tenantForm = new TenantForm(proxyTenant);
    tenantForm.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    
    if (!tenantService.getSystemTenant().equals(proxyTenant.getObject())) {
      tenantForm.setCreditBalance(getCreditBalance(proxyTenant.getObject()));
    }
    map.addAttribute("tenantForm", tenantForm);
    map.addAttribute("allowSecondaryCheckBox", proxyTenant.getAccountType().isEnableSecondaryAddress());
    if (proxyTenant.getSecondaryAddress() != null) {
      tenantForm.setSecondaryAddress(proxyTenant.getSecondaryAddress());
    } else {
      tenantForm.setSecondaryAddress(null);
    }
    tenantForm.setAllowSecondary(proxyTenant.getAccountType().isEnableSecondaryAddress());

    CreditCard creditCard = null;

    List<AccountType> allowedTargetTypes = tenantService.getAllowedTargetAccountTypes(proxyTenant.getObject());
    if (allowedTargetTypes != null && !allowedTargetTypes.isEmpty()) {
      map.addAttribute("showChangeAccountType", true);
    } else {
      map.addAttribute("showChangeAccountType", false);
    }
    List<PendingChange> pendingConversions = tenantService.getPendingConversions(proxyTenant.getObject());

    if (pendingConversions != null && !pendingConversions.isEmpty()) {
      map.addAttribute("showMessagePendingConversion", true);
    } else {
      map.addAttribute("showMessagePendingConversion", false);
    }
    if (proxyTenant.getAccountType().isDepositRequired()
        && proxyTenant.getAccountType().getPaymentModes().intValue() != PaymentMode.INVOICE.getMask()) {
      DepositRecord depositRecord = tenantService.getDepositRecordByTenant(proxyTenant.getObject());
      if (depositRecord == null
          && proxyTenant.getAccountType().getInitialDeposit(proxyTenant.getObject().getCurrency().getCurrencyCode())
              .doubleValue() > 0) {
        map.addAttribute("show_deposit_record_message", true);
        map.addAttribute("initialDepositAmount", proxyTenant.getCurrency().getCurrencyCode() + " "
            + proxyTenant.getAccountType().getInitialDeposit(proxyTenant.getCurrency().getCurrencyCode()));
      } else {
        map.addAttribute("show_deposit_record_message", false);
      }
    } else {
      map.addAttribute("show_deposit_record_message", false);
    }

    effectiveTenant = tenantService.get(proxyTenant.getObject().getParam());
    try {

      creditCard = billingAdminService.getCreditCard(effectiveTenant);
    } catch (Exception e) {
      logger.error("###Failed to get payment information", e);
      // we have profile id but failing to get cc details.
      map.addAttribute("errormsg", messageSource.getMessage("ui.usage.billing.paymentinfo.cc.errormsg", null,
          proxyTenant.getOwner().getLocale()));
      if (((PaymentGatewayService) connectorManagementService
          .getOssServiceInstancebycategory(ConnectorType.PAYMENT_GATEWAY))
          .isAccountExistInPaymentGateway(effectiveTenant)) {
        creditCard = new CreditCard();

      }

    }

    boolean paymentInfoExists = creditCard != null;

    map.addAttribute("paymentInfoExists", paymentInfoExists);

    BillingInfoForm billingInfoForm = new BillingInfoForm(effectiveTenant, null, creditCard);
    billingInfoForm.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));

    
    boolean showCreditCardtab = true;
    if (action != null) {
      billingInfoForm.setAction(action);
    } else {
      showCreditCardtab = false;
    }
    map.addAttribute("showCreditCardTabVar", showCreditCardtab);

    User user = actorService.getActor();
    int currenttab = 1;
    if (userService.hasAuthority(user, "ROLE_ACCOUNT_ADMIN") || userService.hasAuthority(user, "ROLE_ACCOUNT_CRUD")) {
      currenttab = 1;
    } else if (userService.hasAuthority(user, "ROLE_ACCOUNT_BILLING_ADMIN")) {
      currenttab = 4;
    }

    if (tabParam != null) {
      if (tabParam.equals("contact")) {
        currenttab = 2;
      } else if (tabParam.equals("billingAddress")) {
        currenttab = 4;
      }
    }

    map.addAttribute("currenttab", currenttab);

    map.addAttribute("billingInfo", billingInfoForm);

    SetAccountTypeForm setAccountTypeForm = new SetAccountTypeForm(effectiveTenant);
    map.addAttribute("setAccountTypeForm", setAccountTypeForm);

    List<AccountType> targetAccountTypes = tenantService.getAllowedTargetAccountTypes(effectiveTenant.getAccountType());
    map.addAttribute("targetAccountTypes", targetAccountTypes);
    if (!tenantService.getSystemTenant().equals(effectiveTenant)) {
      AccountStatement accountStatement = billingAdminService.getOrCreateProvisionalAccountStatement(effectiveTenant);
      map.addAttribute("currentBillingStart", accountStatement.getBillingPeriodStartDate());
      map.addAttribute("currentBillingEnd", accountStatement.getBillingPeriodEndDate());
      map.addAttribute("BillingPeriodLength", accountStatement.getTotalDaysInBillingPeriod());
    }
    List<BusinessTransaction> transactionHistory = businessTransactionService.getAllBusinessTransactions(
        effectiveTenant, 0, 0);
    map.addAttribute("transactionHistory", transactionHistory);

    String doNotShowPasswordEditLink = "false";
    if (config.getBooleanValue(Configuration.Names.com_citrix_cpbm_portal_directory_service_enabled)
        && config.getValue(Names.com_citrix_cpbm_directory_mode).equals("pull")) {
      doNotShowPasswordEditLink = "true";
    }
    map.addAttribute("doNotShowPasswordEditLink", doNotShowPasswordEditLink);

    logger.debug("### edit current tenant method end");
    setPage(map, Page.COMPANY_PROFILE_PAGE);
    return "tenants.editcurrent";
  }

  @RequestMapping(value = "/editcurrentlogo", method = RequestMethod.GET)
  public String editCurrentTenantLogo(ModelMap map) {
    logger.debug("### edit current tenant logo method starting...(GET)");
    TenantLogoForm tenantLogoForm = new TenantLogoForm(this.getTenant());
    map.addAttribute("tenantLogoForm", tenantLogoForm);
    logger.debug("### edit current tenant logo method end");
    setPage(map, Page.HOME);
    return "tenants.editcurrentlogo";
  }

  @RequestMapping(value = ("/edit"), method = RequestMethod.POST)
  @ResponseBody
  public HashMap<String, String> edit(@ModelAttribute("tenantForm") TenantForm tenantForm, BindingResult result) {
    logger.debug("### edit method starting...(POST)");
    citrix.cpbm.access.Tenant proxyTenant = tenantForm.getTenant();
    if (proxyTenant != null) {
      if (this.getTenant().getSecondaryAddress() == null && !tenantForm.isAllowSecondary()) {
        proxyTenant.setSecondaryAddress(null);
      } else {
        if (tenantForm.getSecondaryAddress() != null && tenantForm.getSecondaryAddress().getCountry() != null) {
          proxyTenant.setSecondaryAddress(tenantForm.getSecondaryAddress());
        }
      }
    }
    try {
      if (isAdmin() && proxyTenant.getObject().equals(tenantService.getSystemTenant())) {
        // root does not have extra information, so setting it null
        proxyTenant.getObject().setTenantExtraInformation(null);
      }
      tenantService.update(proxyTenant, result);
    } catch (ServiceException ex) {
      throw new InvalidAjaxRequestException(ex.getMessage());
    }
    return this.getTenantDataHashMap(proxyTenant.getObject());
  }

  @RequestMapping(value = "/{tenantParam}/editlogo", method = RequestMethod.POST)
  public String editTenantLogo(@PathVariable String tenantParam,
      @ModelAttribute("tenantLogoForm") TenantLogoForm tenantLogoForm, BindingResult result, ModelMap map) {
    logger.debug("### edit logo method starting...(POST)");
    String rootImageDir = config.getValue(Names.com_citrix_cpbm_portal_settings_images_uploadPath);
    if (rootImageDir != null && !rootImageDir.trim().equals("")) {
      Tenant tenant = tenantService.get(tenantParam);
      citrix.cpbm.access.Tenant proxyTenant = (citrix.cpbm.access.Tenant) CustomProxy.newInstance(tenant);
      String relativeImageDir = tenant.getParam();
      TenantLogoFormValidator validator = new TenantLogoFormValidator();
      validator.validate(tenantLogoForm, result);
      if (result.hasErrors()) {
        setPage(map, Page.HOME);
        return "tenants.editcurrentlogo";
      } else {
        File file = new File(FilenameUtils.concat(rootImageDir, relativeImageDir));
        if (!file.exists()) {
          file.mkdir();
        }
        MultipartFile logoFile = tenantLogoForm.getLogo();
        MultipartFile faviconFile = tenantLogoForm.getFavicon();
        try {
          if (!logoFile.getOriginalFilename().trim().equals("")) {
            String logoFileRelativePath = writeMultiPartFileToLocalFile(rootImageDir, relativeImageDir, logoFile);
            proxyTenant.getObject().setImagePath(logoFileRelativePath);
          }
          if (!faviconFile.getOriginalFilename().trim().equals("")) {
            String faviconFileRelativePath = writeMultiPartFileToLocalFile(rootImageDir, relativeImageDir, faviconFile);
            proxyTenant.getObject().setFaviconPath(faviconFileRelativePath);
          }
          tenantService.update(proxyTenant, result);
        } catch (IOException e) {
          logger.debug("###IO Exception in writing custom image file");
        }
      }
      return "redirect:/portal/home";
    } else {
      result.rejectValue("logo", "error.custom.image.upload.dir");
      result.rejectValue("favicon", "error.custom.image.upload.dir");
      setPage(map, Page.HOME);
      return "tenants.editcurrentlogo";
    }
  }

  @RequestMapping(value = {
      "", "/"
  }, method = RequestMethod.GET)
  public String listPopup(ModelMap map,
      @RequestParam(value = "currentPage", required = false, defaultValue = "1") String currentPage,
      @RequestParam(value = "perPage", required = false, defaultValue = "5") String perPage,
      @RequestParam(value = "size", required = false) String size,
      @RequestParam(value = "charRange", required = false, defaultValue = "All") String charRange,
      @RequestParam(value = "pattern", required = false) String pattern,
      @RequestParam(value = "effectiveTenantParam", required = true) String effectiveTenantParam) {

    logger.debug("###Entering in list(map) method @GET");
    List<AccountType> accountTypes = tenantService.getAllAccountTypes();
    map.addAttribute("accountTypes", accountTypes);
    int currentPageValue = Integer.parseInt(currentPage);
    int perPageValue = Integer.parseInt(perPage);
    setPage(map, Page.CRM);
    List<Tenant> tenants = tenantService.list(currentPageValue, perPageValue, null, null, false, null, null, null,
        null, null, null, charRange, pattern);
    int sizeInt = 0;
    if (size == null || size.equals("")) {
      sizeInt = tenantService.getCount(charRange, pattern);
    } else {
      sizeInt = Integer.parseInt(size);
    }
    map.addAttribute("tenants", tenants);
    map.addAttribute("pattern", pattern);
    // Need for pagination
    map.addAttribute("url", "/portal/portal/tenants?");
    setPaginationValues(map, perPageValue, currentPageValue, sizeInt, charRange);
    logger.debug("###Exiting list(map) method @GET");
    return "tenants.list";
  }

  @RequestMapping(value = "/searchlist", method = RequestMethod.GET)
  public String searchlist(ModelMap map,
      @RequestParam(value = "currentPage", required = false, defaultValue = "1") String currentPage,
      @RequestParam(value = "perPage", required = false, defaultValue = "5") String perPage,
      @RequestParam(value = "size", required = false) String size,
      @RequestParam(value = "charRange", required = false, defaultValue = "All") String charRange,
      @RequestParam(value = "pattern", required = false) String pattern) {
    logger.debug("###Entering in searchlist(map) method @GET");
    int currentPageValue = Integer.parseInt(currentPage);
    int perPageValue = Integer.parseInt(perPage);
    List<Tenant> tenants = tenantService.list(currentPageValue, perPageValue, null, null, false, null, null, null,
        null, null, null, charRange, pattern);
    int sizeInt = 0;
    if (size == null || size.equals("")) {
      sizeInt = tenantService.getCount(charRange, pattern);
    }
    // else {
    // sizeInt = Integer.parseInt(size);
    // }
    map.addAttribute("tenants", tenants);
    map.addAttribute("pattern", pattern);
    setPaginationValues(map, perPageValue, currentPageValue, sizeInt, charRange);
    logger.debug("###Exiting searchlist(map) method @GET");
    return "tenant.search.list";
  }

  @RequestMapping(value = "/list", method = RequestMethod.GET)
  public String list(@RequestParam(value = "tenantUUId", required = false) String tenantUUId,
      @RequestParam(value = "showAddAccountWizard", required = false) String showAddAccountWizard,
      @RequestParam(value = "accountType", required = false) String accountType,
      @RequestParam(value = "filterBy", required = false) String filterBy,
      @RequestParam(value = "currentPage", required = false, defaultValue = "1") String currentPage,
      @RequestParam(value = "tenantParam", required = false) String tenantParam,
      @RequestParam(value = "size", required = false) String size, ModelMap map, HttpServletRequest request) {

    logger.debug("###Entering in list(map) method @GET");
    //The tenantForm object is added into map just to populate country list in country_states.jsp
    TenantForm tenantForm = new TenantForm();
    tenantForm.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    map.addAttribute("tenantForm", tenantForm);
    
    List<AccountType> accountTypes = tenantService.getAllAccountTypes();
    map.addAttribute("accountTypes", accountTypes);
    if (showAddAccountWizard != null) {
      map.addAttribute("showAddAccountWizard", showAddAccountWizard);
    }
    AccountType accountTypeObj = null;
    if (accountType != null && !("".equals(accountType))) {
      accountTypeObj = tenantService.getAccountTypeById(accountType);
    }

    int currentPageValue = currentPage != null ? Integer.parseInt(currentPage) : 1;
    int perPageValue = getDefaultPageSize();
    int sizeInt = 0;
    Tenant newlyAddedTenant = null;
    if (size == null || size.equals("")) {
      if (tenantParam != null) {
        newlyAddedTenant = tenantService.get(tenantParam);
        sizeInt = 1;
      } else {
        List<Tenant> tenantsFound = new ArrayList<Tenant>();
        if (tenantUUId != null && tenantUUId != "") {
          Tenant tenant = tenantService.getTenantByParam("uuid", tenantUUId, false);
          if (tenant != null) {
            if (accountTypeObj == null) {
              if (filterBy != null && !filterBy.equals("") && !filterBy.equals("All")) {
                if ((filterBy.equals("0") && tenant.getState().equals(State.NEW))
                    || (filterBy.equals("1") && tenant.getState().equals(State.ACTIVE))
                    || (filterBy.equals("2") && tenant.getState().equals(State.LOCKED))
                    || (filterBy.equals("3") && tenant.getState().equals(State.SUSPENDED))
                    || (filterBy.equals("4") && tenant.getState().equals(State.TERMINATED))) {
                  tenantsFound.add(tenant);
                } else {
                  tenantsFound.add(tenant);
                }
              } else {
                tenantsFound.add(tenant);
              }
            } else {
              if (tenant.getAccountType().equals(accountTypeObj)) {
                if (filterBy != null && !filterBy.equals("") && !filterBy.equals("All")) {
                  if ((filterBy.equals("0") && tenant.getState().equals(State.NEW))
                      || (filterBy.equals("1") && tenant.getState().equals(State.ACTIVE))
                      || (filterBy.equals("2") && tenant.getState().equals(State.LOCKED))
                      || (filterBy.equals("3") && tenant.getState().equals(State.SUSPENDED))
                      || (filterBy.equals("4") && tenant.getState().equals(State.TERMINATED))) {
                    tenantsFound.add(tenant);
                  } else {
                    tenantsFound.add(tenant);
                  }
                } else {
                  tenantsFound.add(tenant);
                }
              }
            }
          }
        } else {
          Map<String, String> searchVar = new HashMap<String, String>();
          if (StringUtils.isNotBlank(filterBy)) {
            searchVar.put("filterBy", filterBy);
          }
          tenantsFound = tenantService.list(0, 0, null, null, false, filterBy, null, accountType, null, null,
              searchVar, null, null);
        }
        if (tenantsFound != null && !tenantsFound.isEmpty()) {
          sizeInt = tenantsFound.size();
        } else {
          sizeInt = 0;
        }
      }
    } else {
      sizeInt = Integer.parseInt(size);
    }

    Map<String, String> filtersMap = new LinkedHashMap<String, String>();
    filtersMap.put("All", messageSource.getMessage("ui.dropdown.account.filter.all", null, getSessionLocale(request)));
    filtersMap.put("0", messageSource.getMessage("ui.dropdown.account.filter.new", null, getSessionLocale(request)));
    filtersMap.put("1", messageSource.getMessage("ui.dropdown.account.filter.active", null, getSessionLocale(request)));
    filtersMap.put("2", messageSource.getMessage("ui.dropdown.account.filter.locked", null, getSessionLocale(request)));
    filtersMap.put("3",
        messageSource.getMessage("ui.dropdown.account.filter.suspended", null, getSessionLocale(request)));
    filtersMap.put("4",
        messageSource.getMessage("ui.dropdown.account.filter.terminated", null, getSessionLocale(request)));
    map.addAttribute("filtersMap", filtersMap);

    if (filterBy == null) {
      filterBy = "All";
    } else {
      if (!filterBy.equals("All")) {
        map.addAttribute("filterresult", "true");
      }
    }
    if (newlyAddedTenant != null) {
      filterBy = null;
    }
    map.addAttribute("filterBy", filterBy);

    List<Tenant> tenants = new ArrayList<Tenant>();
    AccountType tenantAccountTypeObj = null;
    if (newlyAddedTenant != null) {
      tenants.add(newlyAddedTenant);
    } else {

      if (tenantUUId != null && tenantUUId != "") {
        Tenant tenant = tenantService.getTenantByParam("uuid", tenantUUId, false);
        if (tenant != null) {
          if (accountTypeObj == null) {
            if (filterBy != null && !filterBy.equals("") && !filterBy.equals("All")) {
              if ((filterBy.equals("0") && tenant.getState().equals(State.NEW))
                  || (filterBy.equals("1") && tenant.getState().equals(State.ACTIVE))
                  || (filterBy.equals("2") && tenant.getState().equals(State.LOCKED))
                  || (filterBy.equals("3") && tenant.getState().equals(State.SUSPENDED))
                  || (filterBy.equals("4") && tenant.getState().equals(State.TERMINATED))) {
                tenants.add(tenant);
              } else {
                tenants.add(tenant);
              }
            } else {
              tenants.add(tenant);
            }
          } else {
            if (tenant.getAccountType().equals(accountTypeObj)) {
              if (filterBy != null && !filterBy.equals("") && !filterBy.equals("All")) {
                if ((filterBy.equals("0") && tenant.getState().equals(State.NEW))
                    || (filterBy.equals("1") && tenant.getState().equals(State.ACTIVE))
                    || (filterBy.equals("2") && tenant.getState().equals(State.LOCKED))
                    || (filterBy.equals("3") && tenant.getState().equals(State.SUSPENDED))
                    || (filterBy.equals("4") && tenant.getState().equals(State.TERMINATED))) {
                  tenants.add(tenant);
                } else {
                  tenants.add(tenant);
                }
              } else {
                tenants.add(tenant);
              }
            }
          }
        }
      } else {
        Map<String, String> searchVar = new HashMap<String, String>();
        if (StringUtils.isNotBlank(filterBy)) {
          searchVar.put("filterBy", filterBy);
        }
        tenants = tenantService.list(currentPageValue, perPageValue, null, null, false, filterBy, null, accountType,
            null, null, searchVar, null, null);
      }

      if (tenantUUId != null && !tenants.isEmpty()) {
        // Set account type for tabs
        tenantAccountTypeObj = tenants.get(0).getAccountType();
        accountType = tenantAccountTypeObj.getId().toString();
        // Set state for tabs
        State tenantState = tenants.get(0).getState();
        if (tenantState == State.NEW) {
          map.addAttribute("filterBy", "0");
        } else if (tenantState == State.ACTIVE) {
          map.addAttribute("filterBy", "1");
        } else if (tenantState == State.LOCKED) {
          map.addAttribute("filterBy", "2");
        } else if (tenantState == State.SUSPENDED) {
          map.addAttribute("filterBy", "3");
        } else if (tenantState == State.TERMINATED) {
          map.addAttribute("filterBy", "4");
        }
        map.addAttribute("filterresult", "true");
      }
    }
    if (accountType == null) {
      accountType = "";
      setPage(map, Page.ACCOUNT_ALL_ACCOUNTS);
    } else {
      String accountTypeName = accountTypeObj != null ? accountTypeObj.getName()
          : tenantAccountTypeObj != null ? tenantAccountTypeObj.getName() : "";

      if (!("".equals(accountType))) {
        map.addAttribute("accountresult", "true");
        map.addAttribute("selectedTab", accountTypeName);
        if (accountType.equals("1")) {
          setPage(map, Page.ACCOUNT_SYSTEM);
        } else if (accountType.equals("3")) {
          setPage(map, Page.ACCOUNT_RETAIL);
        } else if (accountType.equals("4")) {
          setPage(map, Page.ACCOUNT_CORPORATE);
        } else if (accountType.equals("5")) {
          setPage(map, Page.ACCOUNT_TRIAL);
        } else {
          setCustomizePage(map, "page.title." + accountTypeName, Level1.Crm.getName(), accountTypeName, null);
        }
      }
    }
    map.addAttribute("on", "on");
    map.addAttribute("selectedAccountType", accountType);
    map.addAttribute("tenant", getTenant());
    map.addAttribute("tenants", tenants);
    if (tenants.size() > 0) {
      Tenant firstTenant = tenants.get(0);
      BigDecimal creditBalance = BigDecimal.ZERO;
      if (!firstTenant.equals(tenantService.getSystemTenant())) {
        creditBalance = getCreditBalance(firstTenant);
      }
      map.addAttribute("creditBalance", creditBalance);
    }
    setPaginationValues(map, perPageValue, currentPageValue, sizeInt, null);

    SearchForm searchForm = new SearchForm();

    /*
     * ResourceBundle resourceBundle = ResourceBundle.getBundle("cloud"); String customFieldsProp =
     * resourceBundle.getString("tenant.custom.searchField"); String
     * customFieldsNamesProp=resourceBundle.getString("tenant.custom.searchField.displayName");
     */

    String customFieldsProp = config.getValue(Names.com_citrix_cpbm_portal_settings_search_customFields);

    String[] customFields = null;

    if (customFieldsProp != null && customFieldsProp.length() != 0) {
      customFields = customFieldsProp.split(",");
      for (String customField : customFields) {
        searchForm.getCustomFields().put(customField, null);
      }
    }
    map.addAttribute("customFieldList", customFields);
    map.addAttribute("searchForm", searchForm);

    logger.debug("###Exiting list(map) method @GET");
    return "tenant.listing";
  }

  protected AdvancedSearchForm serachHelper() {
    HashMap<String, String> searchablefields = new HashMap<String, String>();
    List<String> entityList = new ArrayList<String>();
    entityList.add("com.vmops.model.Tenant");
    for (TenantService.SearchFields c : TenantService.SearchFields.values()) {
      searchablefields.put("com.vmops.model.Tenant" + "." + c.name(), c.getPropertyName());
    }

    entityList.add("com.vmops.model.User");
    for (UserService.SearchFields c : UserService.SearchFields.values()) {
      searchablefields.put("com.vmops.model.User" + "." + c.name(), c.getPropertyName());
    }

    AdvancedSearchForm searchForm = new AdvancedSearchForm();
    searchForm.setResultantEntity("com.vmops.model.Tenant");
    searchForm.setFieldList(searchService.getSerachField(entityList, searchablefields));

    return searchForm;
  }

  @RequestMapping(value = "/new", method = RequestMethod.GET)
  public String create(ModelMap map, HttpServletRequest request) throws IPtoCountryException, InternalIPException {
    IPtoCountry iPtoCountry = new IPtoCountry();
    iPtoCountry.setCountryCode("");
    iPtoCountry.setBlackListed(false);
    try {
      iPtoCountry = super.getGeoIpToCountry(request);
    } catch (InternalIPException e) {
      if (!getCurrentUser().equals(userService.getSystemUser(Handle.ROOT))) {
        logger.error(e.getMessage());
        throw e;
      }
    }
    setPage(map, Page.TENANT_ADD);
    citrix.cpbm.access.Tenant tenant = (citrix.cpbm.access.Tenant) CustomProxy.newInstance(new Tenant());
    tenant.getObject().getTenantExtraInformation().setPaymentMode(PaymentMode.CREDIT_CARD);
    TenantForm tenantForm = new TenantForm(tenant);
    tenantForm.setCountryList(countryService.getCountries(null, null, null, null, null, null, null));
    
    map.addAttribute("tenant", CustomProxy.newInstance(getCurrentUser().getTenant()));
    tenantForm.setCurrency(config.getValue(Names.com_citrix_cpbm_portal_settings_default_currency));
    if (tenantService.getDefaultRegistrationAccountType() != null) {
      tenantForm.setAccountTypeId(tenantService.getDefaultRegistrationAccountType().getId().toString());
    }
    List<AccountType> manualRegistrationAccountTypes = tenantService.getManualRegistrationAccountTypes();
    if (manualRegistrationAccountTypes.size() == 0) {
      throw new NoManualRegistrationAccountTypeException("Manual Registration Not Allowed");
    }
    tenantForm.setAccountTypes(manualRegistrationAccountTypes);
    List<Channel> channels = channelService.getChannels();
    if (channels != null && channels.size() > 0) {
      List<CurrencyValue> currenciesList = new ArrayList<CurrencyValue>();
      Channel channel = channels.get(0);
      Catalog catalog = channel.getCatalog();
      if (catalog != null) {
        for (SupportedCurrency sc : catalog.getSupportedCurrenciesByOrder()) {
          currenciesList.add(sc.getCurrency());
        }
      }
      tenantForm.setCurrencyValueList(currenciesList);
    }
    map.addAttribute("account", tenantForm);
    map.addAttribute("channels", channels);

    List<Country> filteredCountryList = getFilteredCountryList(tenantForm.getCountryList());
    map.addAttribute("filteredCountryList", filteredCountryList);
    map.addAttribute("ipToCountryCode", iPtoCountry.getCountryCode());
    map.addAttribute("supportedLocaleList", this.getLocaleDisplayName(listSupportedLocales()));
    map.addAttribute("defaultLocale", getDefaultLocale());
    String showImportFromDirectoryServiceButton = "false";
    if (config.getBooleanValue(Configuration.Names.com_citrix_cpbm_portal_directory_service_enabled)
        && config.getValue(Names.com_citrix_cpbm_directory_mode).equals("pull")) {
      showImportFromDirectoryServiceButton = "true";
    }
    map.addAttribute("showImportAdButton", showImportFromDirectoryServiceButton);
    String showSuffixTextBox = "false";
    if (config.getValue(Names.com_citrix_cpbm_username_duplicate_allowed).equals("true")) {
      showSuffixTextBox = "true";
    }
    map.addAttribute("showSuffixTextBox", showSuffixTextBox);
    return "tenants.new";
  }

  @RequestMapping(value = {
      "", "/"
  }, method = RequestMethod.POST)
  public String create(@ModelAttribute("account") final TenantForm form, BindingResult result, ModelMap map,
      SessionStatus sessionStatus, HttpServletRequest request) throws Exception {
    logger.debug("###Entering in create(form,map,status) method @POST");
    // setPage(map, Page.ACCOUNT_MANAGEMENT);
    String promocode = form.getTrialCode();
    if (promocode != null) {
      CampaignPromotion campaignPromotion = promotionService.locatePromotionByToken(promocode);
      if (campaignPromotion == null) {
        logger.debug("Campaign is not valid");
        result.reject("trialCode",
            messageSource.getMessage("errors.registration.invalid_trial_code", null, getSessionLocale(request)));
        return "tenants.new";
      }
    }
    String email = form.getUser().getEmail();
    if (isEmailBlacklisted(email.toLowerCase())) {
      map.addAttribute("supportedLocaleList", this.getLocaleDisplayName(listSupportedLocales()));
      map.addAttribute("defaultLocale", getDefaultLocale());
      map.addAttribute("ipToCountryCode", form.getUser().getAddress().getCountry());
      form.setAccountTypes(tenantService.getManualRegistrationAccountTypes());
      map.addAttribute("tenant", CustomProxy.newInstance(getCurrentUser().getTenant()));
      map.addAttribute("account", form);
      map.addAttribute("channels", channelService.getChannels());
      List<Country> filteredCountryList = getFilteredCountryList(form.getCountryList());
      map.addAttribute("filteredCountryList", filteredCountryList);
      map.addAttribute("signuperror", "emaildomainblacklisted");
      return "tenants.new";
    }
    if (result.hasErrors()) {
      displayErrors(result);
      parseResult(result, map);
      return "tenants.new";
    }
    // set currency
    for (CurrencyValue cv : form.getCurrencyValueList()) {
      if (cv.getCurrencyCode().equals(form.getCurrency())) {
        form.getTenant().setCurrency(cv);
        break;
      }
    }
    form.getTenant().setAddress(form.getUser().getAddress());

    if (form.isAllowSecondary()) {
      form.getTenant().setSecondaryAddress(form.getSecondaryAddress());
    }
    AccountType at = tenantService.getAccountTypeById(form.getAccountTypeId());
    List<PaymentMode> paymentModes = at.getSupportedPaymentModes();
    if (paymentModes != null && paymentModes.size() > 0) {
      form.getTenant().getObject().getTenantExtraInformation().setPaymentMode(paymentModes.get(0));
    }
    List<String> errorMsgList = new ArrayList<String>();
    try {
      final citrix.cpbm.access.User owner = form.getUser();

      // Country country = countryService.locateCountryByCode(form.getTenant().getAddress().getCountry());
      String phoneNo = owner.getObject().getCountryCode().replaceAll(PHONE_NUMBER_REGEX, "")
          + COUNTRY_CODE_TO_PHONE_NUMBER_SEPERATOR + owner.getPhone().replaceAll(PHONE_NUMBER_REGEX, "");
      owner.setPhone(phoneNo);
      owner.setLocale(form.getUser().getLocale());
      form.getTenant().setRemoteAddress(getRemoteUserIp(request));
      String channelParam = form.getChannelParam();
      if (at.equals(tenantService.getTrialAccountType())) {
        citrix.cpbm.access.Tenant trialTenant = form.getTenant();
        trialTenant.setAccountType(at);
        registrationService.registerTrialAccount(form.getTrialCode(), trialTenant, owner, channelParam);
      } else {
        tenantService.createAccount(form.getTenant(), owner, channelParam, form.getAccountTypeId(), result);
      }
      map.addAttribute("tenant", form.getTenant());
      String homeUrl = config.getValue(Names.com_citrix_cpbm_portal_marketing_home_url);
      String cloudmktgUrl = config.getValue(Names.com_citrix_cpbm_portal_marketing_marketing_url);
      if (homeUrl != null) {
        map.addAttribute("homeUrl", homeUrl);
      }
      if (cloudmktgUrl != null) {
        map.addAttribute("cloudmktgUrl", cloudmktgUrl);
      }
      sessionStatus.setComplete(); // clean up parameters in session.
    } catch (DataAccessException ex) {
      logger.error(ex);
      result.reject("errors.registration", new Object[] {
        ex.getMessage()
      }, null);
      errorMsgList.add("You must accept the terms and conditions to use this service");
    } catch (Exception ex) {
      logger.error("###handleTenantCreationError:" + ex.getMessage());
      throw ex;
    }
    if (result.hasErrors()) {
      displayErrors(result);
      form.reset();
      if (errorMsgList.size() > 0) {
        map.addAttribute("errorMsgList", errorMsgList);
        map.addAttribute("errormsg", true);
      }
      logger.debug("###Exiting register(registration,result,captchaChallenge,,captchaResponse,"
          + "map,sessionStatus,request) method @POST");
      return "tenants.new";
    } else {
      logger.debug("###Exiting create(registration,result,map,sessionStatus,request) method @POST");
      String tenantParam = form.getTenant().getUuid();
      map.clear();
      map.addAttribute("tenantParam", tenantParam);
      map.addAttribute("tenantName", form.getTenant().getName());
      map.addAttribute("tenantAccountTypeName", form.getTenant().getAccountType().getName());
      map.addAttribute("tenantAccountId", form.getTenant().getObject().getAccountId());
      map.addAttribute("tenantOwnerUserName", form.getTenant().getOwner().getUsername());
      map.addAttribute("tenantId", form.getTenant().getObject().getId());
      return result.toString();
    }
  }

  @RequestMapping(value = "/{tenantParam}/changeState", method = RequestMethod.GET)
  public String changeState(@PathVariable String tenantParam, ModelMap map) {
    logger.debug("###Starting changeState() method @GET start");
    Tenant tenant = tenantService.get(tenantParam);
    map.addAttribute("tenant", tenant);
    logger.debug("###Exiting changeState() method @GET start");
    return "tenant.change.state";
  }

  // updated for Danube
  @RequestMapping(value = "/{tenantParam}/add_credit", method = RequestMethod.POST)
  @ResponseBody
  public String issueCredit(@PathVariable String tenantParam,
      @RequestParam(value = "credit", required = true) String credit,
      @RequestParam(value = "comment", required = true) String comment, HttpServletRequest request,
      HttpServletResponse response) {
    logger.debug("###Entering in issueCredit(tenantId,credit,comment,response) method @POST");
    try {
      Tenant tenant = tenantService.get(tenantParam);
      JSONObject result = new JSONObject();
      BigDecimal creditDecimal = billingPostProcessor.setScaleByCurrency(new BigDecimal(credit), tenant.getCurrency());
      // check if the credit is negitive return
      if (creditDecimal.compareTo(new BigDecimal(0)) < 0) {// negative
        response.setStatus(HttpStatus.PRECONDITION_FAILED.value());
      } else {

        SalesLedgerRecord creditSLR = billingAdminService.addPaymentOrCredit(tenant, creditDecimal,
            Type.SERVICE_CREDIT, comment, null);

        AccountStatement accountStatement = creditSLR.getAccountStatement();
        response.setStatus(HttpStatus.OK.value());
        String message = messageSource.getMessage("tenant.credit.confirmation", null, getSessionLocale(request));
        result.put("creditBalance", accountStatement.getFinalCharges().negate()) // toConfirm
            .put("message", StringEscapeUtils.escapeHtml(message));
      }
      logger.debug(result.toString());
      logger.debug("###Exiting issueCredit(tenantId,credit,comment,response) method @POST");
      return result.toString();
      // return tenant;
    } catch (Exception ex) {
      response.setStatus(HttpStatus.PRECONDITION_FAILED.value());
      logger.error(ex);
      return null;
    }
  }

  // updated for Danube
  @RequestMapping(value = "/{tenantParam}/cancel_credit", method = RequestMethod.POST)
  @ResponseBody
  public String cancelCredit(@PathVariable String tenantParam,
      @RequestParam(value = "slrUuid", required = true) String slrUuid,
      @RequestParam(value = "comment", required = true) String comment, HttpServletRequest request,
      HttpServletResponse response) {
    logger.debug("###Entering in issueCredit(tenantId,credit,comment,response) method @POST");
    try {
      Tenant tenant = tenantService.get(tenantParam);
      JSONObject result = new JSONObject();
      SalesLedgerRecord slr = billingAdminService.getSalesLedgerRecord(slrUuid);

      // Check if it is already cancelled, if cancelled then return error
      if (slr != null && slr.getCancellationReferenceId() != null) {
        throw new BillingAdminServiceException(" This payment has already been cancelled by slr:"
            + slr.getCancellationReferenceId().getParam());
      }

      if (slr != null && !slr.getType().equals(SalesLedgerRecord.Type.INVOICE)) {
        SalesLedgerRecord slr1 = billingAdminService.cancelCredit(tenant, slr, comment);
        if (slr1 == null) {// null is returned if any precondition fails
          response.setStatus(HttpStatus.PRECONDITION_FAILED.value());
        } else {
          response.setStatus(HttpStatus.OK.value());
          String message = messageSource.getMessage("tenant.credit.confirmation", null, getSessionLocale(request));
          result.put("creditBalance", slr1.getAccountStatement().getFinalCharges().negate()) // toConfirm
              .put("message", StringEscapeUtils.escapeHtml(message));
        }
      } else {
        response.setStatus(HttpStatus.PRECONDITION_FAILED.value());
      }
      logger.debug(result.toString());
      logger.debug("###Exiting issueCredit(tenantId,credit,comment,response) method @POST");
      return "success";
      // return tenant;
    } catch (Exception ex) {
      response.setStatus(HttpStatus.PRECONDITION_FAILED.value());
      logger.error(ex);
      return "failure";
    }
  }

  @RequestMapping(value = "/issueCredit", method = RequestMethod.GET)
  public String issueCredit(@RequestParam(value = "tenant", required = true) String tenantParam, ModelMap map) {
    logger.debug("###Starting issueCredit() method @GET start");
    Tenant tenant = tenantService.get(tenantParam);
    map.addAttribute("tenant", tenant);
    logger.debug("###Exiting issueCredit() method @GET start");
    return "tenant.issue.credit";
  }

  @ExceptionHandler(TenantStateChangeFailedException.class)
  @ResponseStatus(value = HttpStatus.PRECONDITION_FAILED)
  public ModelAndView handleTenantStateChangeFailedException(TenantStateChangeFailedException ex,
      HttpServletRequest request) {
    logger.error("handleTenantStateChangeFailedException####" + ex.getMessage(), ex);
    ModelAndView viewData = new ModelAndView();
    viewData.setViewName("errors.messagepage");
    viewData.addObject("errorMessage", ex.getMessage());
    return viewData;
  }

  @RequestMapping(value = "/{tenantParam}/changeState", method = RequestMethod.POST)
  @ResponseBody
  public HashMap<String, String> changeState(@PathVariable String tenantParam,
      @RequestParam(value = "new_state", required = true) State state,
      @RequestParam(value = "memo", required = true) String memo, HttpServletResponse response) {
    logger.debug("###Entering in changeState(tenantId,state,memo,response) method @POST");
    Tenant tenant = tenantService.getTenantByParam("uuid", tenantParam, true);
    if (tenant.getState() == State.TERMINATED || tenant.getState() == State.NEW) {
      throw new TenantStateChangeFailedException("State of terminated or new account cannot be changed.");
    }
    if (tenant.getState() != state) {
      switch (state) {
        case ACTIVE:
          tenantService.changeState(tenant.getUuid(), "ACTIVE", null, memo);
          break;
        case LOCKED:
          tenantService.changeState(tenant.getUuid(), "LOCKED", null, memo);
          break;
        case SUSPENDED:
          tenantService.changeState(tenant.getUuid(), "SUSPENDED", null, memo);
          break;
        case TERMINATED:
          tenantService.changeState(tenant.getUuid(), "TERMINATED", null, memo);
          break;
        case NEW:
        default:
          throw new TenantStateChangeFailedException("Invalid state change requested");
      }
      String message = "state.changed";
      String messageArgs = state + "," + tenant.getName() + "," + tenant.getName() + " : " + tenant.getParam() + ","
          + state + "," + tenant.getName();
      // String descriptionArgs = tenant.getName() + " : " + tenant.getParam() + "," + state + "," + tenant.getName();

      eventService.createEvent(new Date(), tenant, message, messageArgs, Source.PORTAL, Scope.ACCOUNT, Category.SYSTEM,
          Severity.INFORMATION, true);
    }
    logger.debug("###Exiting changeState(tenantId,state,memo,response) method @POST");
    response.setStatus(HttpStatus.OK.value());
    return this.getTenantDataHashMap(tenant);
  }

  @RequestMapping(value = {
      "/clean", "/tenants/clean"
  }, method = RequestMethod.GET)
  @ResponseBody
  public String cleanupTenant(@RequestParam("tenant") final String tenantParam, HttpServletResponse response) {
    Tenant tenant = tenantService.get(tenantParam);
    try {
      tenantService.cleanTenant(tenant, "cleaning on " + new Date());
    } catch (Exception e) {
      logger.error(e);
      return "failure";
    }
    return "success";
  }

  @RequestMapping(value = "/{tenantParam}/set_owner", method = RequestMethod.POST)
  @ResponseBody
  public String editOwner(@PathVariable String tenantParam, @RequestParam("owner") String ownerId,
      HttpServletResponse response) throws ConnectorManagementServiceException {
    logger.debug("###Entering in editOwner(tenantId,ownerId,map) method @POST");
    Tenant tenant = tenantService.get(tenantParam);
    User owner = userService.getUserByParam("id", ownerId, false);
    try {
      tenantService.setOwner(tenant, owner);
    } catch (HibernateOptimisticLockingFailureException ex) {
      logger.error(ex);
      tenantService.refresh(tenant);
      return ex.getMessage();
    }
    response.setStatus(HttpStatus.OK.value());
    logger.debug("###Exiting editOwner(tenantId,ownerId,map) method @POST");
    return "redirect:/portal/tenants/" + tenant.getParam();
  }

  @RequestMapping(value = "/search", method = RequestMethod.POST)
  public String search(SearchForm form, BindingResult result,
      @RequestParam(value = "tenantId", required = false) String tenantId,
      @RequestParam(value = "accountType", required = false) String accountType,
      @RequestParam(value = "filterBy", required = false) String filterBy,
      @RequestParam(value = "currentPage", required = false, defaultValue = "1") String currentPage,
      @RequestParam(value = "size", required = false) String size, ModelMap map, HttpServletRequest request) {

    logger.debug("###Entering in search(form,result,map) method @POST");
    List<AccountType> accountTypes = tenantService.getAllAccountTypes();
    map.addAttribute("accountTypes", accountTypes);

    /*
     * search shud take care of filter and accountType next-prev in search - pagination
     */
    AccountType accountTypeObj = null;
    if (accountType != null && accountType != "") {
      accountTypeObj = tenantService.getAccountTypeById(accountType);
    }

    if (accountType == null) {
      accountType = "";
      setPage(map, Page.ACCOUNT_ALL_ACCOUNTS);
    } else {
      if (accountType != "") {
        map.addAttribute("accountresult", "true");

        if (accountType.equals("1")) {
          setPage(map, Page.ACCOUNT_SYSTEM);
        } else if (accountType.equals("3")) {
          setPage(map, Page.ACCOUNT_RETAIL);
        } else if (accountType.equals("4")) {
          setPage(map, Page.ACCOUNT_CORPORATE);
        } else if (accountType.equals("5")) {
          setPage(map, Page.ACCOUNT_TRIAL);
        }
      } else {
        setPage(map, Page.ACCOUNT_ALL_ACCOUNTS);
      }
    }
    map.addAttribute("selectedAccountType", accountType);

    Map<String, String> filtersMap = new LinkedHashMap<String, String>();
    filtersMap.put("All", messageSource.getMessage("ui.dropdown.account.filter.all", null, getSessionLocale(request)));
    filtersMap.put("0", messageSource.getMessage("ui.dropdown.account.filter.new", null, getSessionLocale(request)));
    filtersMap.put("1", messageSource.getMessage("ui.dropdown.account.filter.active", null, getSessionLocale(request)));
    filtersMap.put("2", messageSource.getMessage("ui.dropdown.account.filter.locked", null, getSessionLocale(request)));
    filtersMap.put("3",
        messageSource.getMessage("ui.dropdown.account.filter.suspended", null, getSessionLocale(request)));
    filtersMap.put("4",
        messageSource.getMessage("ui.dropdown.account.filter.terminated", null, getSessionLocale(request)));
    map.addAttribute("filtersMap", filtersMap);

    if (filterBy == null) {
      filterBy = "All";
    } else {
      if (!filterBy.equals("All")) {
        map.addAttribute("filterresult", "true");
      }
    }
    map.addAttribute("filterBy", filterBy);

    int currentPageValue = currentPage != null ? Integer.parseInt(currentPage) : 1;
    int perPageValue = getDefaultPageSize();
    int sizeInt = 0;

    map.addAttribute("searchresult", "true");
    List<Tenant> tenants = new ArrayList<Tenant>();
    Map<String, String> vars = new HashMap<String, String>();
    Map<String, String> vars2 = new HashMap<String, String>();

    boolean hasStaticCriteria = false;
    if (accountType != null && accountType != "") {
      vars.put("accountType", accountType);
      vars2.put("accountType", accountType);
      hasStaticCriteria = true;
    }
    if (filterBy != null && !filterBy.equals("") && !filterBy.equals("All")) {
      vars.put("filterBy", filterBy);
      vars2.put("filterBy", filterBy);
      hasStaticCriteria = true;
    }
    if (form.getfieldName() != null) {
      vars.put("fieldName", form.getfieldName());
      vars2.put("fieldName", form.getfieldName());
      map.addAttribute("fieldName", form.getfieldName());
      hasStaticCriteria = true;
    }
    if (form.getName() != null) {
      vars.put("name", form.getName());
      vars2.put("name", form.getName());
      map.addAttribute("name", form.getName());
      hasStaticCriteria = true;
    }
    if (form.getAccountId() != null) {
      vars.put("accountId", form.getAccountId());
      vars2.put("accountId", form.getAccountId());
      map.addAttribute("accountId", form.getAccountId());
      hasStaticCriteria = true;
    }
    if (form.getSfdcId() != null) {
      vars.put("sfdcId", form.getSfdcId());
      vars2.put("sfdcId", form.getSfdcId());
      hasStaticCriteria = true;
    }

    if (hasStaticCriteria) {
      tenants = tenantService.list(currentPageValue, perPageValue, null, null, false, null, null, null, null, null,
          vars, null, null);
    } else {
      tenants = tenantService.list(currentPageValue, perPageValue, null, null, false, filterBy, null, accountTypeObj
          .getId().toString(), null, null, null, null, null);
    }

    if (size == null || size.equals("")) {
      if (hasStaticCriteria) {
        sizeInt = tenantService.list(currentPageValue, perPageValue, null, null, false, null, null, null, null, null,
            vars2, null, null).size();
      } else {
        sizeInt = tenantService.list(0, 0, null, null, false, filterBy, null, accountTypeObj.getId().toString(), null,
            null, null, null, null).size();
      }
    } else {
      sizeInt = Integer.parseInt(size);
    }
    boolean hasCustomFieldCriteria = false;
    SearchForm searchForm = new SearchForm();
    String customFieldsProp = config.getValue(Names.com_citrix_cpbm_portal_settings_search_customFields);
    String[] customFields = null;

    if (customFieldsProp != null && customFieldsProp.length() != 0) {
      customFields = customFieldsProp.split(",");
      for (String customField : customFields) {
        if (form.getCustomFields().get(customField) != null) {
          hasCustomFieldCriteria = true;
        }
        searchForm.getCustomFields().put(customField, null);
      }
    }
    if (hasCustomFieldCriteria) {
      tenants = tenantService.filterByCustomFields(form.getCustomFields(), tenants);
    }

    map.addAttribute("tenant", getTenant());
    map.addAttribute("tenants", tenants);
    setPaginationValues(map, perPageValue, currentPageValue, sizeInt, null);

    if (tenants.size() > 0) {
      Tenant firstTenant = tenants.get(0);
      BigDecimal creditBalance = BigDecimal.ZERO;
      if (!firstTenant.equals(tenantService.getSystemTenant())) {
        creditBalance = getCreditBalance(firstTenant);
      }
      map.addAttribute("creditBalance", creditBalance);
    }

    /*
     * ResourceBundle resourceBundle = ResourceBundle.getBundle("cloud"); String customFieldsProp =
     * resourceBundle.getString("tenant.custom.searchField"); String
     * customFieldsNamesProp=resourceBundle.getString("tenant.custom.searchField.displayName");
     */

    map.addAttribute("customFieldList", customFields);
    map.addAttribute("searchForm", searchForm);

    logger.debug("###Exiting search(form,result,map) method @POST");
    return "tenant.listing";
  }

  // @RequestMapping(value = "/{tenantParam}/add_credit", method = RequestMethod.POST)
  // @ResponseBody
  // public String addCredit(@PathVariable String tenantParam,
  // @RequestParam(value = "amount", required = true) BigDecimal amount,
  // HttpServletResponse response) {
  // logger
  // .debug("###Entering in addCredit(tenantId,amount,response) method @POST");
  // try {
  // Tenant tenant = tenantService.locateTenantByParam(tenantParam);
  // billingService.addToCreditBalance(tenant, amount);
  // response.setStatus(HttpStatus.OK.value());
  // logger
  // .debug("###Exiting addCredit(tenantId,amount,response) method @POST");
  // return messageSource.getMessage("tenant.credit.confirmation", null, null);
  // } catch (BillingServiceException ex) {
  // logger.error(ex);
  // response.setStatus(HttpStatus.PRECONDITION_FAILED.value());
  // return ex.getMessage();
  // }
  //
  // }

  @RequestMapping(value = "/{tenantParam}/audit_log", method = RequestMethod.POST)
  public String getAuditLog(@PathVariable String tenantParam, ModelMap map) {
    Tenant tenant = tenantService.get(tenantParam);
    map.addAttribute("auditLogs", auditLogService.getAuditLogsByTypeId(tenant));
    return "auditlog.show";
  }

  @RequestMapping(value = "/{tenantParam}/resource_limit", method = RequestMethod.GET)
  public String showResourceLimits(@PathVariable String tenantParam, ModelMap map) {
    logger.debug("###Starting showResourceLimit() method @GET start");
    Tenant tenant = tenantService.get(tenantParam);
    ResourceLimitForm resourceLimitForm = getResourceLimit(tenant);
    map.addAttribute("resourceLimitForm", resourceLimitForm);
    map.addAttribute("tenant", tenant);
    logger.debug("###Exiting showResourceLimit() method @GET start");
    return "tenant.resourcelimits";
  }

  @RequestMapping(value = "/{tenantParam}/resource_limit", method = RequestMethod.POST)
  @ResponseBody
  public String showResourceLimit(@PathVariable String tenantParam,
      @ModelAttribute("ResourceLimitForm") ResourceLimitForm resourceLimitForm, HttpServletResponse response) {
    logger.debug("###Starting showResourceLimit() method @POST start");
    try {
      Tenant tenant = tenantService.get(tenantParam);
      if (resourceLimitForm.getMaxUsers() != null) {
        try {
          int usercount = Integer.parseInt(resourceLimitForm.getMaxUsers());
          ResourceLimit rsLimitUser = new ResourceLimit();
          rsLimitUser.setTenant(tenant);
          rsLimitUser.setType(ResourceLimit.USER);
          rsLimitUser.setMax(usercount);
          resourceLimitService.save(rsLimitUser);
        } catch (NumberFormatException e) {
          logger.error("### Max Users should be integer");
        }
      }
    } catch (Exception e) {
      return "fail";
    }
    logger.debug("###Exiting showResourceLimit() method @POST end");
    return "success";
  }

  protected ResourceLimitForm getResourceLimit(Tenant tenant) {
    ResourceLimitForm rlForm = new ResourceLimitForm();
    rlForm.setId(tenant.getId());
    rlForm.setDefaultMaxUsers(tenant.getAccountType().getMaxUsers().toString());
    ResourceLimit userrs = resourceLimitService.getResourceLimitByTenantAndType(tenant, ResourceLimit.USER);
    if (userrs != null) {
      rlForm.setMaxUsers("" + userrs.getMax());
    }
    return rlForm;
  }

  /**
   * @param tenantParam
   * @param pendingChangeId
   * @deprecated Use actOnPendingChange directly to pass the state to which it is getting changed to
   * @return
   */
  /**
   * {@inheritDoc}
   */
  @Deprecated
  @RequestMapping(value = "/{tenantParam}/approvePendingChange", method = RequestMethod.POST)
  @ResponseBody
  public HashMap<String, String> approvePendingChange(@PathVariable String tenantParam,
      @RequestParam(value = "Id", required = true) String pendingChangeId) {

    return this.actOnPendingChange(tenantParam, pendingChangeId, com.vmops.model.PendingChange.State.APPROVED, null);
  }

  /**
   * This method is responsible to handle the requests for Pending changes based on Reject or Approve state.
   * 
   * @param tenantParam
   * @param pendingChangeId
   * @param stateChangeTo
   * @param memo
   * @return
   */
  /**
   * {@inheritDoc}
   */
  @RequestMapping(value = "/{tenantParam}/actOnPendingChange", method = RequestMethod.POST)
  @ResponseBody
  public HashMap<String, String> actOnPendingChange(@PathVariable String tenantParam,
      @RequestParam(value = "Id", required = true) String pendingChangeId,
      @RequestParam(value = "State", required = true) com.vmops.model.PendingChange.State stateChangeTo,
      @RequestParam(value = "Memo", required = false) String memo) {
    logger.debug("### In approvePendingChange() method starting...");
    if (stateChangeTo == com.vmops.model.PendingChange.State.REJECTED && StringUtils.isEmpty(memo)) {
      throw new InvalidAjaxRequestException("Memo is Mandatory in case of REJECTED state..");
    }
    Tenant tenant = tenantService.get(tenantParam);
    try {
      tenantService.actOnPendingChange(tenant, pendingChangeId, stateChangeTo, memo);
    } catch (TenantStateChangeFailedException e) {
      throw new InvalidAjaxRequestException(e.getMessage());
    }
    logger.debug("### In approvePendingChange() method ending.");

    return this.getTenantDataHashMap(tenant);
  }

  @RequestMapping(value = "/getAccountType", method = RequestMethod.GET)
  public AccountType getAccountType(@RequestParam(value = "accountTypeName", required = true) String accountTypeName) {
    return tenantService.getAccountTypeByName(accountTypeName);
  }

  @RequestMapping(value = "/notifications", method = RequestMethod.GET)
  public String listNotifications(@ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "filterBy", required = false) String filterBy,
      @RequestParam(value = "currentPage", required = false, defaultValue = "1") String currentPage,
      @RequestParam(value = "size", required = false) String size, ModelMap map, HttpServletRequest request) {
    logger.debug("Entering in listNotifications");
    User user = getCurrentUser();
    int currentPageValue = currentPage != null ? Integer.parseInt(currentPage) : 1;
    int perPageValue = getDefaultPageSize();
    int sizeInt = 0;

    if ((userService.hasAuthority(user, "ROLE_ACCOUNT_CRUD") || userService.hasAuthority(user, "ROLE_ACCOUNT_MGMT"))
        && (Boolean) request.getAttribute("isSurrogatedTenant")) {
      tenant = tenantService.get(tenantParam);
      user = tenant.getOwner();
      setPage(map, Page.CRM_ALL_NOTIFICATIONS);
      map.addAttribute("showUserProfile", true);
    } else {
      user = userService.get(user.getParam());
      setPage(map, Page.HOME_ALL_NOTIFICATIONS);
    }
    map.addAttribute("tenant", tenant);
    map.addAttribute("userHasCloudServiceAccount", userService.isUserHasAnyActiveCloudService(user));
    List<Event> eventLst = new ArrayList<Event>();
    try {
      if (size == null || size.equals("")) {
        // sizeInt = notificationService.showNotificationDates(user, filterBy, 0, 0).size();
        sizeInt = eventService.showEventDates(user, filterBy, 0, 0).size();
      } else {
        sizeInt = Integer.parseInt(size);
      }
      eventLst = eventService.showEventDates(user, filterBy, currentPageValue, perPageValue);
      // List<Alert> subscribedAlerts = alertService.getSubscribedAlerts(user);
      // alerts.addAll(subscribedAlerts);
      map.addAttribute("notificationsList", eventLst);
      map.addAttribute("size", eventLst.size());
    } catch (Exception e) {
      logger.error("Error while retrieving notifications: " + e);
    }

    map.addAttribute("user", user);
    // filters
    // The filters entry are made in ApplicationResources.properties for internationalization.
    // Adding filters need an entry in ApplicationResources.prperties
    List<String> filtersList = new ArrayList<String>();
    Map<String, String> filtersMap = new LinkedHashMap<String, String>();
    filtersMap.put("All",
        messageSource.getMessage("ui.dropdown.notification.filter.all", null, getSessionLocale(request)));
    filtersMap.put("Today",
        messageSource.getMessage("ui.dropdown.notification.filter.today", null, getSessionLocale(request)));
    filtersMap.put("This Week",
        messageSource.getMessage("ui.dropdown.notification.filter.thisWeek", null, getSessionLocale(request)));
    filtersMap.put("Last Week",
        messageSource.getMessage("ui.dropdown.notification.filter.lastWeek", null, getSessionLocale(request)));
    filtersMap.put("Older",
        messageSource.getMessage("ui.dropdown.notification.filter.older", null, getSessionLocale(request)));

    filtersList.add("All");
    filtersList.add("Today");
    filtersList.add("This Week");
    filtersList.add("Last Week");
    filtersList.add("Older");
    map.addAttribute("filtersList", filtersList);
    map.addAttribute("filtersMap", filtersMap);

    if (filterBy == null) {
      filterBy = "All";
    }
    map.addAttribute("filterBy", filterBy);
    // Need for pagination
    map.addAttribute("url", "/portal/portal/tenants/notifications?");
    setPaginationValues(map, perPageValue, currentPageValue, sizeInt, null);
    map.addAttribute("currentUser", user);
    logger.debug("Leaving in listNotifications");
    return "tenants.notifications";
  }

  /**
   * View Notification
   * 
   * @param date
   * @param map
   * @return
   */
  @RequestMapping(value = "/viewnotification", method = RequestMethod.GET)
  public String viewNotification(@ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "date", required = true) String date,
      @RequestParam(value = "currentPage", required = false, defaultValue = "1") String currentPage,
      @RequestParam(value = "perPage", required = false, defaultValue = "10") String perPage,
      @RequestParam(value = "size", required = false) String size, ModelMap map, HttpServletRequest request) {
    logger.debug("### viewNotification method starting...(GET)");
    User user = getCurrentUser();
    int currentPageValue = currentPage != null ? Integer.parseInt(currentPage) : 1;
    int perPageValue = perPage != null ? Integer.parseInt(perPage) : 10;
    int sizeInt = 0;

    if ((userService.hasAuthority(user, "ROLE_ACCOUNT_CRUD") || userService.hasAuthority(user, "ROLE_ACCOUNT_MGMT"))
        && (Boolean) request.getAttribute("isSurrogatedTenant")) {
      tenant = tenantService.get(tenantParam);
      user = tenant.getOwner();
      setPage(map, Page.CRM_ALL_NOTIFICATIONS);
      map.addAttribute("showUserProfile", true);
    } else {
      user = userService.get(user.getParam());
      setPage(map, Page.HOME_ALL_NOTIFICATIONS);
    }
    map.addAttribute("tenant", tenant);
    map.addAttribute("selectedDate", date);
    List<Event> eventLst = new ArrayList<Event>();
    try {
      date = date.substring(0, date.indexOf(' '));// get date as '%Y-%m-%d' from 'yyyy-MM-dd HH:mm:ss'
      if (size == null || size.equals("")) {
        sizeInt = eventService.showEventsByDate(user, date, 0, 0).size();
      } else {
        sizeInt = Integer.parseInt(size);
      }
      eventLst = eventService.showEventsByDate(user, date, currentPageValue, perPageValue);

      map.addAttribute("notificationsList2", eventLst);
      map.addAttribute("notificationListLen2", eventLst.size());
      map.addAttribute("notificationsList2", eventLst);

      if (eventLst.size() != 0) {

      }
    } catch (Exception e) {
      logger.error("Error while retrieving notifications: " + e);
    }

    map.addAttribute("user", user);

    // Need for pagination
    map.addAttribute("url", "/portal/portal/tenants/notifications?");
    // setPaginationValues(map, perPageValue, currentPageValue, sizeInt, null);

    int totalpages = 1;
    if (perPageValue > 0) {
      if (sizeInt != 0 && sizeInt % perPageValue == 0) {
        totalpages = sizeInt / perPageValue;
      } else {
        totalpages = (sizeInt / perPageValue) + 1;
      }
    }

    // necessary values for pagination
    map.addAttribute("currentPage2", currentPageValue);
    map.addAttribute("totalpages2", totalpages);
    map.addAttribute("size2", sizeInt);
    map.addAttribute("perPage2", perPageValue);
    map.addAttribute("prevPage2", currentPageValue > 1 ? currentPageValue - 1 : 1);
    map.addAttribute("nextPage2", currentPageValue < totalpages ? currentPageValue + 1 : totalpages);
    map.addAttribute("lowerBound2", currentPageValue + 2 < totalpages ? currentPageValue : (totalpages - 2 < 1 ? 1
        : totalpages - 2));
    map.addAttribute("upperBound2", currentPageValue + 2 < totalpages ? currentPageValue + 2 : totalpages);
    map.addAttribute("currentUser", user);
    logger.debug("### viewNotification method end");
    return "notifications.view";
  }

  /**
   * list the alerts
   * 
   * @param map
   * @return
   */
  @RequestMapping(value = "/alert_prefs", method = RequestMethod.GET)
  public String viewAlertsDeliveryOptions(@RequestParam(value = "param", required = true) String tenantParam,
      @RequestParam(value = "userParam", required = true) String userParameter, ModelMap map, HttpServletRequest request) {
    logger.debug("###Entering in viewAlertsDeliveryOptions(map) method @GET");
    setPage(map, Page.DASHBOARD_ALL_ALERTS);
    User currentUser = getCurrentUser();
    User effectiveUser = currentUser;

    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);
    if (userService.hasAnyAuthority(currentUser, "ROLE_ACCOUNT_CRUD", "ROLE_ACCOUNT_MGMT")
        && (Boolean) request.getAttribute("isSurrogatedTenant")) {
      effectiveUser = effectiveTenant.getOwner();
    }
    if (userParameter != null) {
      effectiveUser = userService.get(userParameter);
    }

    List<UserAlertPreferences> alertsPrefs = new ArrayList<UserAlertPreferences>();
    try {
      alertsPrefs = userAlertPreferencesService.listAllUserAlertPreferences(effectiveUser);
      map.addAttribute("alertsPrefs", alertsPrefs);
      map.addAttribute("alertsPrefsSize", alertsPrefs.size());
      map.addAttribute("addAlertEmailLimit",
          config.getIntValue(Names.com_citrix_cpbm_accountManagement_resourceLimits_registered_emailAddresses));
      map.addAttribute("userAlertEmailForm", new UserAlertEmailForm(effectiveUser, AlertType.USER_ALERT_EMAIL));
    } catch (Exception e) {
      logger.error(e);
    }
    map.addAttribute("user", effectiveUser);
    map.addAttribute("tenant", effectiveTenant);
    logger.debug("###Exiting viewAlertsDeliveryOptions(map) method @GET");
    return "alerts.delivery_opts";
  }

  /**
   * list the alerts
   * 
   * @param map
   * @return
   */
  @RequestMapping(value = "/change_primary_email", method = RequestMethod.POST)
  @ResponseBody
  public String saveNewPrimaryEmail(@RequestParam(value = "param", required = true) String tenantParam,
      @RequestParam(value = "userParam", required = true) String userParameter,
      @RequestParam(value = "newEmail", required = false) String notificationPrefId, ModelMap map,
      HttpServletRequest request) {
    logger.debug("###Entering in saveNewPrimaryEmail(map) method @POST");
    User currentUser = getCurrentUser();
    User effectiveUser = currentUser;

    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);

    if (userService.hasAnyAuthority(currentUser, "ROLE_ACCOUNT_CRUD", "ROLE_ACCOUNT_MGMT")
        && (Boolean) request.getAttribute("isSurrogatedTenant")) {
      effectiveUser = effectiveTenant.getOwner();
    }
    if (userParameter != null) {
      effectiveUser = userService.get(userParameter);
    }
    String emailAddress = null;
    String secondaryEmailAdd = null;// to make the existing primary email address to secondary email address.
    try {
      UserAlertPreferences userAlertPreferences = userAlertPreferencesService.locateUserAlertPreference(new Long(
          notificationPrefId));
      emailAddress = userAlertPreferences.getEmailAddress();
      secondaryEmailAdd = effectiveUser.getEmail();
      effectiveUser.setEmail(emailAddress);
      effectiveUser.setEmailVerified(true);
      userService.save(effectiveUser);
      userAlertPreferences.setEmailAddress(secondaryEmailAdd);
      userAlertPreferences.setUser(effectiveUser);
      userAlertPreferencesService.save(userAlertPreferences);// After making the secondary email id as primary the old
// primary email
      // id should become secondary (DE3540)
    } catch (Exception e) {
      logger.debug("###Exiting saveNewPrimaryEmail(map) method @POST - failure", e);
      return "failure";
    }
    logger.debug("###Exiting saveNewPrimaryEmail(map) method @POST - success");
    return secondaryEmailAdd;
  }

  /**
   * list the verified email addresses
   * 
   * @param map
   * @return
   */
  @RequestMapping(value = "/change_primary_email", method = RequestMethod.GET)
  public String viewVerifiedEmails(@RequestParam(value = "param", required = true) String tenantParam,
      @RequestParam(value = "userParam", required = true) String userParameter, ModelMap map, HttpServletRequest request) {
    logger.debug("###Entering in viewVerifiedEmails(map) method @GET");
    setPage(map, Page.DASHBOARD_ALL_ALERTS);
    User currentUser = getCurrentUser();
    User effectiveUser = currentUser;

    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);

    if (userService.hasAnyAuthority(currentUser, "ROLE_ACCOUNT_CRUD", "ROLE_ACCOUNT_MGMT")
        && (Boolean) request.getAttribute("isSurrogatedTenant")) {
      effectiveUser = effectiveTenant.getOwner();
    }
    if (userParameter != null) {
      effectiveUser = userService.get(userParameter);
    }
    List<UserAlertPreferences> alertsPrefs = new ArrayList<UserAlertPreferences>();
    try {
      alertsPrefs = userAlertPreferencesService.listAllUserAlertPreferences(effectiveUser);
      List<UserAlertPreferences> verifiedAlertPrefs = new ArrayList<UserAlertPreferences>();
      UserAlertPreferences alertPref = null;
      Iterator<UserAlertPreferences> alertPrefIt = alertsPrefs.iterator();
      while (alertPrefIt.hasNext()) {
        alertPref = alertPrefIt.next();
        if (alertPref.isEmailVerified()) {
          verifiedAlertPrefs.add(alertPref);
        }
      }
      map.addAttribute("verifiedAlertsPrefs", verifiedAlertPrefs);
      map.addAttribute("verifiedAlertsPrefsSize", verifiedAlertPrefs.size());
    } catch (Exception e) {
      logger.error(e);
    }
    map.addAttribute("user", effectiveUser);
    map.addAttribute("tenant", effectiveUser);
    logger.debug("###Exiting viewVerifiedEmails(map) method @GET");
    return "alerts.change_primary_email";
  }

  /**
   * list the alerts
   * 
   * @param map
   * @return
   */
  @RequestMapping(value = "/alert_prefs", method = RequestMethod.POST)
  @ResponseBody
  public String saveAlertsDeliveryOptions(@Valid @ModelAttribute("userAlertEmailForm") UserAlertEmailForm form,
      BindingResult result) {
    logger.debug("###Entering in viewAlertsDeliveryOptions(map) method @POST");
    if (result.hasErrors()) {
      throw new AjaxFormValidationException(result);
    }
    User user = form.getUser();
    String email = form.getEmail();
    AlertType alertType = form.getAlertType();

    long addAlertEmailLimit = config
        .getIntValue(Names.com_citrix_cpbm_accountManagement_resourceLimits_registered_emailAddresses);
    long alertsPrefsSize = userAlertPreferencesService.getCount(user);
    if (alertsPrefsSize >= addAlertEmailLimit) {
      result.rejectValue("email", "error.verifyAlertEmail.limitReached");
      throw new AjaxFormValidationException(result);
    }

    if (isEmailBlacklisted(email)) {
      result.rejectValue("email", "signup.emaildomain.blacklist.error");
      throw new AjaxFormValidationException(result);
    }

    if (email.equals(user.getEmail())) {
      result.rejectValue("email", "js.errors.addsecAlert.sameAsPrimaryEmail");
      throw new AjaxFormValidationException(result);
    }

    if (userAlertPreferencesService.checkUserAlertPrefsExists(alertType, user, email)) {
      result.rejectValue("email", "error.verifyAlertEmail.exists");
      throw new AjaxFormValidationException(result);
    }

    UserAlertPreferences alertPref = userAlertPreferencesService.createUserAlertPreference(user, email, alertType);
    logger.debug("###Exiting viewAlertsDeliveryOptions(map) method @POST - success");
    return alertPref.getId().toString();
  }

  /**
   * Delete the alert preference
   * 
   * @param map
   * @return
   */
  @RequestMapping(value = "/alert_prefs/{prefId}/delete", method = RequestMethod.POST)
  @ResponseBody
  public String deleteAlertsDeliveryOptions(@PathVariable Long prefId, ModelMap map) {
    logger.debug("###Entering in DeleteAlertsDeliveryOptions(map) method @POST");
    try {
      UserAlertPreferences pref = userAlertPreferencesService.locateUserAlertPreference(prefId);
      userAlertPreferencesService.deleteUserAlertPreference(pref);
    } catch (Exception e) {
      logger.debug("###Exiting DeleteAlertsDeliveryOptions(map) method @GET - failure");
      return "failure";
    }

    logger.debug("###Exiting DeleteAlertsDeliveryOptions(map) method @POST - success");
    return "success";
  }

  /**
   * Verify the email for an alert preference
   * 
   * @param map
   * @return
   */
  @RequestMapping(value = "/alert_prefs/{prefId}/verify", method = RequestMethod.POST)
  @ResponseBody
  public String verifyAlertsDeliveryOptions(@PathVariable Long prefId, ModelMap map) {
    logger.debug("###Entering in VerifyAlertsDeliveryOptions(map) method @POST");

    try {
      UserAlertPreferences pref = userAlertPreferencesService.locateUserAlertPreference(prefId);
      userAlertPreferencesService.verifyUserAlertPreference(pref);
    } catch (Exception e) {
      logger.debug("###Exiting VerifyAlertsDeliveryOptions(map) method @POST - failure", e);
      return "failure";
    }

    logger.debug("###Exiting VerifyAlertsDeliveryOptions(map) method @POST - success");
    return "success";
  }

  /**
   * list the subscriptions
   * 
   * @param map
   * @return string
   * @throws ConnectorManagementServiceException
   */
  @RequestMapping(value = "/alerts", method = RequestMethod.GET)
  public String listSubscriptions(@ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "budget", required = false) String budget,
      @RequestParam(value = "page", required = false, defaultValue = "1") String currentPage, ModelMap map,
      HttpServletRequest request) {
    logger.debug("###Entering in viewSubscriptions(map) method @GET");

    int page;
    int perPage;
    try {
      page = Integer.parseInt(currentPage);
    } catch (NumberFormatException nFE) {
      page = 1;
    }
    try {
      perPage = getDefaultPageSize();
      if (perPage > 14) {
        perPage = 14;
      }
    } catch (NumberFormatException nFE) {
      perPage = 14;
    }

    User user = getCurrentUser();
    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);

    if ((Boolean) request.getAttribute("isSurrogatedTenant")) {
      user = effectiveTenant.getOwner();
      setPage(map, Page.CRM_ALL_ALERTS);
      map.addAttribute("showUserProfile", true);
      map.addAttribute("userHasCloudServiceAccount", userService.isUserHasAnyActiveCloudService(user));
    } else {
      setPage(map, Page.DASHBOARD_ALL_ALERTS);
      map.addAttribute("userHasCloudServiceAccount", userService.isUserHasAnyActiveCloudService(getCurrentUser()));
    }
    map.addAttribute("tenant", effectiveTenant);
    int totalSubscriptions;
    List<SpendAlertSubscription> subscriptions = null;
    if (user != effectiveTenant.getOwner() && !userService.hasAuthority(user, "ROLE_ACCOUNT_BILLING_ADMIN")) {
      subscriptions = notificationService.getAllSubscriptions(user, page, perPage);
      totalSubscriptions = notificationService.getCountByAccountHolder(user);
      map.addAttribute("subscrptionalerttype", "user");
    } else {
      subscriptions = notificationService.getAllSubscriptions(effectiveTenant, page, perPage);
      totalSubscriptions = notificationService.getCountByAccountHolder(effectiveTenant);
      map.addAttribute("subscrptionalerttype", "tenant");
    }

    CustomAlertForm subscriptionForm = new CustomAlertForm(user);
    subscriptionForm.setThresholdType("percentage");
    map.addAttribute("subscriptionForm", subscriptionForm);

    map.addAttribute("subscriptions", subscriptions);
    map.addAttribute("size", subscriptions.size());

    if (budget != null && budget.toString().equals("true")) {
      TenantForm tenantForm = new TenantForm((citrix.cpbm.access.Tenant) CustomProxy.newInstance(effectiveTenant));
      tenantForm.setSpendLimit(effectiveTenant.getSpendLimit());
      map.addAttribute("tenantForm", tenantForm);
      map.addAttribute("budget", budget);
    } else {
      // budget is not passed in cmd, look it up
      map.addAttribute("spendbudget", tenant.getSpendBudget().toString());
    }
    map.addAttribute("spendbudget_effectiveTenant", effectiveTenant.getSpendBudget().toString());

    if (totalSubscriptions - (page * perPage) > 0) {
      map.addAttribute("enable_next", "True");
    } else {
      map.addAttribute("enable_next", "False");
    }
    map.addAttribute("current_page", page);

    logger.debug("###Exiting viewSubscriptions(map) method @GET");
    return "tenants.alerts";
  }

  /**
   * View Alert
   * 
   * @param ID
   * @param map
   * @return
   */
  @RequestMapping(value = ("/alerts/view"), method = RequestMethod.GET)
  public String viewAlert(@RequestParam(value = "Id", required = true) String ID, ModelMap map) {
    logger.debug("### viewAlert method starting...(POST)");
    SpendAlertSubscription subscription = notificationService.getSpendAlertSubscription(new Long(ID));
    Tenant tenant = subscription.getUser().getTenant();
    map.addAttribute("subscription", subscription);
    map.addAttribute("spendbudget", tenant.getSpendBudget().toString());
    map.addAttribute("tenant", tenant);

    float spend_budget_alert_cap;
    spend_budget_alert_cap = (tenant.getSpendBudget().floatValue()) * (subscription.getPercentage().floatValue()) / 100;
    map.addAttribute("spend_budget_alert_cap", spend_budget_alert_cap);
    String user_email = subscription.getUser().getEmail();
    String user_phone = subscription.getUser().getPhone();
    map.addAttribute("user_email", user_email);
    map.addAttribute("user_phone", user_phone);

    logger.debug("### viewAlert method end");
    return "alerts.view";
  }

  /**
   * Create new Alert
   * 
   * @param map
   * @return
   */

  @RequestMapping(value = ("/alerts/new"), method = RequestMethod.GET)
  public String createSpendAlertSubscription(@ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "tenant", required = false) String tenantParam, ModelMap map, HttpServletRequest request) {
    logger.debug("### createSubscription method starting...(GET)");
    User user = getCurrentUser();
    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);
    if ((Boolean) request.getAttribute("isSurrogatedTenant")) {
      user = effectiveTenant.getOwner();
    }
    CustomAlertForm subscriptionForm = new CustomAlertForm(user);
    if (user != effectiveTenant.getOwner() && !userService.hasAuthority(user, "ROLE_ACCOUNT_BILLING_ADMIN")) {
      subscriptionForm.setType("user");
      subscriptionForm.setBudget(user.getSpendBudget());
    } else {
      subscriptionForm.setType("tenant");
      subscriptionForm.setBudget(user.getTenant().getSpendBudget());
    }

    map.addAttribute("tenant", effectiveTenant);

    subscriptionForm.setThresholdType("percentage");
    map.addAttribute("subscriptionForm", subscriptionForm);
    logger.debug("### createSubscription method end");
    return "alerts.new";
  }

  /**
   * creates subscriptions
   * 
   * @param form
   * @param map
   * @return string
   */
  @RequestMapping(value = "/alerts/new", method = RequestMethod.POST)
  @ResponseBody
  public SpendAlertSubscription createSpendAlertSubscription(@ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @ModelAttribute("subscriptionForm") CustomAlertForm form, BindingResult result, ModelMap map,
      HttpServletRequest request) {
    logger.debug("###Entering in createSubscription(form,result,map) method @POST");
    User user = getCurrentUser();
    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);
    if ((Boolean) request.getAttribute("isSurrogatedTenant")) {
      user = effectiveTenant.getOwner();
    }
    int subscriptionType = 1; // Spend Limit money value type
    SpendAlertSubscription subscription = new SpendAlertSubscription();
    subscription.setSubscriptionType(subscriptionType);
    AccountHolder accountHolder = null;
    if (form.getType().equals("tenant")) {
      accountHolder = effectiveTenant;
    } else {
      accountHolder = user;
    }
    // Validation code.
    AlertValidator validator = new AlertValidator();
    validator.validate(form, result);
    if (result.hasErrors()) {
      throw new AjaxFormValidationException(result);
    }
    if (effectiveTenant != null) {
      List<SpendAlertSubscription> spendAlertSubscription = notificationService.getAllSubscriptions(effectiveTenant);
      if (spendAlertSubscription.size() > 0) {
        for (SpendAlertSubscription spendAlertSubscription2 : spendAlertSubscription) {
          if (spendAlertSubscription2.getPercentage().doubleValue() == form.getTenantPercentage().doubleValue()) {
            result.rejectValue("tenantPercentage", "js.errors.tenantPercentage.validatePercentage");
            throw new AjaxFormValidationException(result);
          }
        }
      }
    }

    subscription.setPercentage(form.getTenantPercentage());

    subscription.setAccountHolder(accountHolder);
    subscription.setUser(user);
    // add new subscription
    subscription = notificationService.saveSubscription(subscription);
    return subscription;

  }

  /**
   * deletes subscription
   * 
   * @param subscriptionId
   * @param map
   * @return
   */
  @ResponseBody
  @RequestMapping(value = "/alerts/remove", method = RequestMethod.GET)
  public String removeSubscription(@RequestParam(value = "Id", required = true) String subscriptionId, ModelMap map) {
    logger.debug("###Entering removeSubscription method @POST");
    // removes subscription
    notificationService.removeSubscription(notificationService.getSpendAlertSubscription(new Long(subscriptionId)));
    map.clear();
    logger.debug("###Leaving removeSubscription method @POST");
    return "success";
  }

  /**
   * Edit product
   * 
   * @param ID
   * @param map
   * @return
   */
  @RequestMapping(value = ("/alerts/edit"), method = RequestMethod.GET)
  public String editSpendAlertSubscription(@RequestParam(value = "Id", required = true) String ID,
      @ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "tenant", required = false) String tenantParam, ModelMap map, HttpServletRequest request) {
    logger.debug("### editProduct method starting...(GET)");
    SpendAlertSubscription subscription = notificationService.getSpendAlertSubscription(new Long(ID));
    User user = getCurrentUser();
    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);
    if ((Boolean) request.getAttribute("isSurrogatedTenant")) {
      user = effectiveTenant.getOwner();
    }
    map.addAttribute("tenant", effectiveTenant);
    CustomAlertForm subscriptionForm = new CustomAlertForm(user);
    subscriptionForm.setThresholdType("percentage");
    subscriptionForm.setTenantPercentage(subscription.getPercentage());
    map.addAttribute("subscriptionForm", subscriptionForm);
    map.addAttribute("subscriptionId", ID);
    logger.debug("### editProduct method end");
    return "alerts.edit";
  }

  /**
   * deletes subscription
   * 
   * @param subscriptionId
   * @param map
   * @return
   */
  @ResponseBody
  @RequestMapping(value = "/alerts/edit", method = RequestMethod.POST)
  public SpendAlertSubscription editSpendAlertSubscription(
      @RequestParam(value = "Id", required = true) String subscriptionId,
      @RequestParam(value = "newValue", required = true) String newVal,
      @ModelAttribute("subscriptionForm") CustomAlertForm form, BindingResult result, ModelMap map) {
    logger.debug("###Entering editSubscription method @POST");
    SpendAlertSubscription subscription = notificationService.getSpendAlertSubscription(new Long(subscriptionId));
    if (newVal == null) {
      result.rejectValue("tenantPercentage", "js.errors.tenantPercentage.required");
      throw new AjaxFormValidationException(result);
    }
    BigDecimal percentage = new BigDecimal(newVal);
    if ((percentage.intValue() < 0) || (percentage.intValue() > 100)) {
      result.rejectValue("tenantPercentage", "js.errors.tenantPercentage.percentage");
      throw new AjaxFormValidationException(result);
    }
    subscription.setPercentage(percentage);
    subscription = notificationService.saveSubscription(subscription);
    logger.debug("###Leaving editSubscription method @POST");

    return subscription;
  }

  /**
   * Create new Alert
   * 
   * @param map
   * @return
   */

  @RequestMapping(value = ("/setAccountBudget"), method = RequestMethod.GET)
  public String setAccountBudget(@ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "tenant", required = false) String tenantParam, ModelMap map, HttpServletRequest request) {
    logger.debug("### setAccountBudget method starting...(GET)");
    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);

    TenantForm tenantForm = new TenantForm((citrix.cpbm.access.Tenant) CustomProxy.newInstance(effectiveTenant));
    tenantForm.setSpendLimit(effectiveTenant.getSpendBudget());
    map.addAttribute("tenantForm", tenantForm);
    map.addAttribute("tenant", effectiveTenant);
    logger.debug("### setAccountBudget method end");
    return "alerts.setaccountbudget";
  }

  @RequestMapping(value = "/setAccountBudget", method = RequestMethod.POST)
  public String setAccountBudget(@ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "tenantparam", required = false) String tenantParam,
      @ModelAttribute("tenantForm") final TenantForm form, ModelMap map) {
    logger.debug("###Entering in setAccountBudget(map) method @POST");
    Tenant effectiveTenant = tenantService.get(tenantParam);
    User user = effectiveTenant.getOwner();
    effectiveTenant.setSpendBudget(form.getTenant().getObject().getSpendBudget());
    tenantService.save(effectiveTenant);
    List<SpendAlertSubscription> subscriptions = notificationService.getAllSubscriptions(effectiveTenant);
    setPage(map, Page.DASHBOARD_ALL_ALERTS);

    map.addAttribute("subscriptions", subscriptions);
    map.addAttribute("size", subscriptions.size());
    map.addAttribute("user", user);
    return "redirect:/portal/tenants/alerts?tenant=" + tenantParam;
  }

  protected void parseResult(BindingResult result, ModelMap map) {
    if (result.getFieldErrors().size() > 0) {
      List<String> errorMsgList = new ArrayList<String>();
      for (FieldError fieldError : result.getFieldErrors()) {
        String fieldName = fieldError.getField();
        if (fieldName.contains(".")) {
          fieldName = fieldName.substring(fieldName.lastIndexOf(".") + 1);
        }
        errorMsgList.add(fieldName + " field value '" + fieldError.getRejectedValue() + "' is not valid.");
      }

      map.addAttribute("errorMsgList", errorMsgList);
      map.addAttribute("errormsg", true);
    }
  }

  @Override
  protected String writeMultiPartFileToLocalFile(String imageFolder, String relativeImageDir,
      MultipartFile multipartFile) throws IOException {
    String fileRelativePath = FilenameUtils.concat(relativeImageDir,
        FilenameUtils.getName(multipartFile.getOriginalFilename()));
    String logoFilelocalPath = FilenameUtils.concat(imageFolder, fileRelativePath);
    FileOutputStream outputStream = new FileOutputStream(logoFilelocalPath);
    InputStream inputStream = multipartFile.getInputStream();
    byte buf[] = new byte[1024];
    int len;
    while ((len = inputStream.read(buf)) > 0) {
      outputStream.write(buf, 0, len);
    }
    outputStream.close();
    inputStream.close();
    return fileRelativePath;
  }

  @RequestMapping(value = "/{tenantParam}/delete", method = RequestMethod.POST)
  public String delete(@PathVariable String tenantParam, ModelMap map) {
    logger.debug("###Entering in delete(@PathVariable String tenantParam, ModelMap map) method @POST");
    Tenant tenant = tenantService.get(tenantParam);
    try {
      tenantService.cleanTenant(tenant, "Deleting tenant id:" + tenant.getId());

    } catch (Exception e) {
      // TODO Auto-generated catch block
      if (logger.isDebugEnabled()) {
        logger.debug("###Exception whilst deleting tenant id:" + tenant.getId());
      }
    }
    logger.debug("###Exitng in delete(@PathVariable String tenantParam, ModelMap map) method @POST");
    return "redirect:/portal/tenants/list";
  }

  protected HashMap<String, String> getTenantDataHashMap(Tenant tenant) {
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("param", tenant.getParam());
    map.put("name", tenant.getName());
    map.put("accountType", tenant.getAccountType().getDisplayName());
    map.put("accountId", tenant.getAccountId());
    map.put("state", tenant.getState().getName());
    map.put("username", tenant.getOwner().getUsername());
    return map;
  }

  @RequestMapping(value = "/stateChanges", method = RequestMethod.GET)
  public String listStateChanges(@RequestParam(value = "tenant", required = true) String tenantParam, ModelMap map) {
    Tenant tenant = tenantService.get(tenantParam);
    List<BusinessTransaction> transactionHistory = businessTransactionService.getAllBusinessTransactions(tenant, 0, 0);
    map.addAttribute("transactionHistory", transactionHistory);
    map.addAttribute("tenant", tenant);
    return "tenant.state.changes";
  }

  private String getAccountLimits(ModelMap map, String instanceParam, String tenantParam) {
    logger.debug("###Entering getAccountLimits method...(GET)");
    Tenant tenant = tenantService.get(tenantParam);
    ServiceInstance instance = connectorConfigurationManager.getInstance(instanceParam);
    Map<String, String> resLimitMap = this.getAccountLifecycleHandler(instanceParam).getControls(tenant);

    if (resLimitMap != null && resLimitMap.size() > 0) {
      Set<AccountControlServiceConfigMetadata> properties = instance.getService()
          .getAccountControlServiceConfigMetadata();
      List<AccountControlServiceConfigMetadata> sortedProperties = new ArrayList<AccountControlServiceConfigMetadata>(
          properties);
      Collections.sort(sortedProperties);
      Map<String, String> sortedResourceLimitMap = new LinkedHashMap<String, String>();
      for (AccountControlServiceConfigMetadata accountControlServiceConfigMetadata : sortedProperties) {
        if (resLimitMap.containsKey(accountControlServiceConfigMetadata.getName())) {
          sortedResourceLimitMap.put(accountControlServiceConfigMetadata.getName(),
              resLimitMap.get(accountControlServiceConfigMetadata.getName()));
        }
      }
      map.addAttribute("resourceLimitsMap", resLimitMap);
    }

    map.addAttribute("tenantuuid", tenantParam);
    map.addAttribute("instance", instance);

    Long userLimit = tenant.getMaxUsers();
    map.addAttribute("userLimit", userLimit); // TODO show userlimit in UI

    logger.debug("### Exiting getAccountLimits method...(GET)");
    return "tenant.showlimits";
  }

  private String editAccountLimits(String tenantParam, String instanceParam, ModelMap map) {
    logger.debug("### Entering editAccountLimits method...(GET)");
    Tenant tenant = tenantService.get(tenantParam);
    Map<String, String> resLimitMap = this.getAccountLifecycleHandler(instanceParam).getControls(tenant);

    ServiceInstance instance = connectorConfigurationManager.getInstance(instanceParam);
    Set<AccountControlServiceConfigMetadata> properties = instance.getService()
        .getAccountControlServiceConfigMetadata();
    Map<String, AccountControlServiceConfigMetadata> metadata = new HashMap<String, AccountControlServiceConfigMetadata>();
    for (AccountControlServiceConfigMetadata accountControlServiceConfigMetadata : properties) {
      metadata.put(accountControlServiceConfigMetadata.getName(), accountControlServiceConfigMetadata);
    }

    List<AccountControlServiceConfigMetadata> propertyList = new ArrayList<AccountControlServiceConfigMetadata>();
    Set<String> resourceNames = resLimitMap.keySet();
    for (String resourceName : resourceNames) {
      AccountControlServiceConfigMetadata property = metadata.get(resourceName);
      propertyList.add(property);
    }
    List<AccountControlServiceConfigMetadata> sortedProperties = new ArrayList<AccountControlServiceConfigMetadata>(
        propertyList);
    Collections.sort(sortedProperties);
    map.addAttribute("account_control_edit_properties", sortedProperties);
    map.addAttribute("service", instance.getService());
    map.addAttribute("instance", instance);
    map.addAttribute("tenantuuid", tenantParam);
    map.addAttribute("resLimitMap", resLimitMap);

    logger.debug("### Exiting editAccountLimits method...(GET)");
    return "tenant.editaccountlimits";
  }

  private HashMap<String, String> editAccountLimits(String tenantParam, String instanceParam, String configProperties) {
    logger.debug("### Entering edit account limits method...(POST)");

    HashMap<String, String> map = new HashMap<String, String>();
    HashMap<String, String> limitMap = new HashMap<String, String>();
    ServiceInstance instance = connectorConfigurationManager.getInstance(instanceParam);
    Service service = instance.getService();
    Set<AccountControlServiceConfigMetadata> properties = service.getAccountControlServiceConfigMetadata();
    Map<String, AccountControlServiceConfigMetadata> mapOfServiceMetaData = new HashMap<String, AccountControlServiceConfigMetadata>();
    for (AccountControlServiceConfigMetadata serviceConfigurationMetadata : properties) {
      mapOfServiceMetaData.put(serviceConfigurationMetadata.getName(), serviceConfigurationMetadata);
    }
    try {
      JSONArray jsonArray = new JSONArray(configProperties);
      for (int index = 0; index < jsonArray.length(); index++) {
        JSONObject jsonObj = jsonArray.getJSONObject(index);
        String fieldName = jsonObj.get("name").toString();
        String fieldValue = jsonObj.get("value").toString();

        AccountControlServiceConfigMetadata configurationMetadata = mapOfServiceMetaData.get(fieldName);
        map.put("validationResult", CssdkConstants.SUCCESS);
        String validationJson = configurationMetadata.getValidation();

        String validationResult = ValidationUtil.valid(validationJson, fieldName, fieldValue, messageSource);
        if (!CssdkConstants.SUCCESS.equals(validationResult)) {
          map.put("validationResult", validationResult);
          return map;
        }
        limitMap.put(fieldName, fieldValue);
      }
      Tenant tenant = tenantService.get(tenantParam);
      this.getAccountLifecycleHandler(instanceParam).setControls(tenant, limitMap);
      map.put("result", CssdkConstants.SUCCESS);
    } catch (Exception ex) {
      map.put("result", CssdkConstants.FAILURE);
      logger.error("Exception in editing account controls ", ex);
      map.put("message", ex.getMessage());
    }

    logger.debug("### Exiting edit account limits method...(POST)");
    return map;
  }

  @RequestMapping(value = "/listaccountlimits", method = RequestMethod.GET)
  public String listAccountLimits(ModelMap map,
      @RequestParam(value = "tenantParam", required = true, defaultValue = "1") String tenantParam,
      @RequestParam(value = "instanceParam", required = false) String instanceParam) {
    return getAccountLimits(map, instanceParam, tenantParam);
  }

  private AccountLifecycleHandler getAccountLifecycleHandler(String instanceParam) {
    CloudConnector connector = (CloudConnector) connectorManagementService.getServiceInstance(instanceParam);
    return connector != null ? connector.getAccountLifeCycleHandler() : null;
  }

  @RequestMapping(value = "/editaccountlimits", method = RequestMethod.GET)
  public String editAccountlimits(@RequestParam(value = "tenantuuid", required = true) String tenantuuid,
      @RequestParam(value = "instanceParam", required = false) String instanceParam, ModelMap map) {
    return editAccountLimits(tenantuuid, instanceParam, map);
  }

  @RequestMapping(value = "/validateUserLimit", method = RequestMethod.GET)
  @ResponseBody
  public String validateUserLimit(@RequestParam(value = "userlimit", required = true) String userLimit,
      @RequestParam(value = "tenantid", required = true) String tenantParam) {
    Tenant tenant = tenantService.get(tenantParam);
    try {
      Long userLimitValue = Long.parseLong(userLimit);
      tenantService.validateUserLimit(tenant, userLimitValue);
    } catch (UserLimitServiceException e) {
      throw new InvalidAjaxRequestException(e.getMessage());
    } catch (NumberFormatException e) {
      String message = messageSource.getMessage("js.errors.tenants.resourceLimitForm.unacceptableMaxUserValue", null,
          getCurrentUser().getLocale());
      throw new InvalidAjaxRequestException(message);
    }
    return "true";
  }

  @RequestMapping(value = ("/editaccountlimits"), method = RequestMethod.POST)
  public HashMap<String, String> editAccountlimits(
      @RequestParam(value = "tenantParam", required = true) String tenantParam,
      @RequestParam(value = "instanceParam", required = true) String instanceParam,
      @RequestParam("configProperties") final String configProperties) {
    return editAccountLimits(tenantParam, instanceParam, configProperties);
  }

  @RequestMapping(value = "/{channelParam}/listcurrencies", method = RequestMethod.GET)
  public String getChannelCurrencies(@PathVariable String channelParam, ModelMap map) {
    TenantForm tenantForm = (TenantForm) map.get("account");
    List<CurrencyValue> currenciesList = channelService.listCurrencies(channelParam);
    tenantForm.setCurrencyValueList(currenciesList);
    return "tenant.channel.currencies";
  }

  @RequestMapping(value = "/validate_trial")
  @ResponseBody
  public String validatePromotionCodeforTenant(@RequestParam("trialCode") final String trialCode,
      @RequestParam("channelParam") final String channelParam) {

    logger.debug("###validatePromotionCode() method starting...." + trialCode);

    CampaignPromotion promotion = promotionService.locatePromotionByToken(trialCode);
    if (promotion == null) {
      return Boolean.FALSE.toString();
    }
    if (promotion.getSupportedChannels() != null && promotion.getSupportedChannels().size() > 0) {
      Channel channel = channelService.getChannel(channelParam);
      if (!promotion.getSupportedChannels().contains(channel)) {
        return Boolean.FALSE.toString();
      }
    }

    return Boolean.TRUE.toString();
  }

  @RequestMapping(value = "/getapidetails", method = RequestMethod.POST)
  @ResponseBody
  public Map<String, Object> verifyPasswordAndGetApiCredentials(
      @RequestParam(value = "password", required = true) final String password, HttpServletRequest request) {
    Map<String, Object> mapresult = new HashMap<String, Object>();
    logger.debug("###Entering in getapidetails(password,session) method @POST");
    User user = getCurrentUser(true);
    boolean authenticated = false;
    if (config.getBooleanValue(Configuration.Names.com_citrix_cpbm_portal_directory_service_enabled)
        && config.getValue(Names.com_citrix_cpbm_directory_mode).equals("pull")) {
      authenticated = true;
    } else {
      authenticated = user.authenticate(password);
    }
    if (authenticated) {
      Tenant currentTenant = this.getTenant();
      Tenant effectiveTenant = null;
      String tenantPara = currentTenant.getParam();
      effectiveTenant = tenantService.get(tenantPara);
      Set<TenantHandle> handles = effectiveTenant.getTenantHandles();
      List<Map<String, String>> credentialList = new ArrayList<Map<String, String>>();
      Iterator<TenantHandle> handlesIterator = handles.iterator();
      while (handlesIterator.hasNext()) {
        TenantHandle handle = handlesIterator.next();
        Map<String, String> credentialMap = handle.getApiCredentials();
        if (credentialMap != null) {
          String serviceInstanceUUID = handle.getServiceInstanceId();
          try {
            ServiceInstance instance = connectorConfigurationManager.getInstance(serviceInstanceUUID);
            if (!instance.getService().getType().equals(CssdkConstants.OSS)) {
              String instanceName = instance.getName();
              Service service = instance.getService();
              String serviceName = service.getServiceName();
              serviceName = messageSource.getMessage(serviceName + ".service.name", null, getSessionLocale(request));
              credentialMap.put("ServiceName", serviceName);
              credentialMap.put("ServiceUuid", service.getUuid());
              credentialMap.put("InstanceName", instanceName);
              credentialList.add(credentialMap);
            }
          } catch (Exception e) {
            logger.error(e);
          }
        }
      }
      mapresult.put("tenantCredentialList", credentialList);
      mapresult.put("success", true);
    }
    logger.debug("###Exiting getapidetails(password,map) method @POST returning " + authenticated + " for "
        + user.getName());

    return mapresult;
  }

  private BigDecimal getCreditBalance(Tenant tenant) {
    AccountStatement accountStatement = billingAdminService.getOrCreateProvisionalAccountStatement(tenant);
    List<SalesLedgerRecord> salesLedgerRecordList = accountStatement.getSalesLedgerRecords();
    if (salesLedgerRecordList.size() > 0) {
      return salesLedgerRecordList.get(0).getBalanceAmount();
    } else {
      return accountStatement.getBalanceForwardAmount().negate(); // toConfirm
    }
  }

  @ResponseBody
  @RequestMapping(value = "/enableServiceForTenant", method = RequestMethod.POST)
  public Map<String, List<String>> enableServiceForTenant(
      @RequestParam(value = "tenantparam", required = true) String tenantParam,
      @RequestParam(value = "instanceUuid", required = true) String serviceInstanceUuid,
      @RequestParam(value = "instanceProperty", required = true) String instancePropertyJson) {
    Map<String, List<String>> serviceEnabled = new HashMap<String, List<String>>();
    @SuppressWarnings("unchecked")
    Map<String, String> instanceProperty = net.sf.json.JSONObject.fromObject(instancePropertyJson);
    Tenant tenant = tenantService.get(tenantParam);
    serviceEnabled = tenantService.enableServiceForTenant(tenant, serviceInstanceUuid, instanceProperty);
    tenantService.refresh(tenant);
    return serviceEnabled;

  }

  @ResponseBody
  @RequestMapping(value = "/enableServicesForThisUser", method = RequestMethod.POST)
  public Map<String, Boolean> enableServiceForUser(
      @RequestParam(value = "tenantparam", required = true) String tenantParam,
      @RequestParam(value = "userparam", required = true) String userParam) {

    Tenant tenant = tenantService.get(tenantParam);
    User user = userService.get(userParam);
    return userService.enableServicesForUser(tenant, user);
  }

}
