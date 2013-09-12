/*
*  Copyright © 2013 Citrix Systems, Inc.
*  You may not use, copy, or modify this file except pursuant to a valid license agreement from
*  Citrix Systems, Inc.
*/
package com.citrix.cpbm.portal.fragment.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tiles.definition.NoSuchDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.citrix.cpbm.access.proxy.CustomProxy;
import com.citrix.cpbm.platform.admin.service.ConnectorConfigurationManager;
import com.citrix.cpbm.platform.admin.service.ConnectorManagementService;
import com.citrix.cpbm.platform.admin.service.exceptions.ConnectorManagementServiceException;
import com.citrix.cpbm.platform.spi.CloudConnector;
import com.citrix.cpbm.platform.spi.FilterComponent;
import com.citrix.cpbm.platform.spi.ResourceComponent;
import com.citrix.cpbm.platform.util.CssdkConstants;
import com.citrix.cpbm.portal.forms.SubscriptionForm;
import com.vmops.internal.service.SubscriptionService;
import com.vmops.model.Catalog;
import com.vmops.model.Channel;
import com.vmops.model.CurrencyValue;
import com.vmops.model.ProductBundle;
import com.vmops.model.ProductBundleRevision;
import com.vmops.model.ProductRevision;
import com.vmops.model.Service;
import com.vmops.model.ServiceInstance;
import com.vmops.model.ServiceResourceType;
import com.vmops.model.ServiceResourceTypeGeneratedUsage;
import com.vmops.model.ServiceResourceTypeGroupComponent;
import com.vmops.model.ServiceResourceTypeProperty;
import com.vmops.model.Subscription;
import com.vmops.model.Tenant;
import com.vmops.model.Tenant.State;
import com.vmops.model.User;
import com.vmops.portal.config.Configuration;
import com.vmops.portal.config.Configuration.Names;
import com.vmops.service.ChannelService;
import com.vmops.service.CurrencyValueService;
import com.vmops.service.ProductBundleService;
import com.vmops.service.UserService.Handle;
import com.vmops.service.billing.BillingAdminService;
import com.vmops.service.exceptions.AjaxFormValidationException;
import com.vmops.service.exceptions.CloudServiceException;
import com.vmops.service.exceptions.SubscriptionServiceException;
import com.vmops.web.controllers.AbstractAuthenticatedController;
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.interceptors.UserContextInterceptor;
import com.vmops.web.validators.ValidationUtil;

public abstract class AbstractSubscriptionController extends AbstractAuthenticatedController {

  @Autowired
  private ConnectorManagementService connectorManagementService;

  @Autowired
  private SubscriptionService subscriptionService;

  @Autowired
  private ProductBundleService productBundleService;

  @Autowired
  protected Configuration config;

  @Autowired
  protected ConnectorConfigurationManager connectorConfigurationManager;

  @Autowired
  private BillingAdminService billingAdminService;

  @Autowired
  private ChannelService channelService;

  @Autowired
  private CurrencyValueService currencyValueService;

  Logger logger = Logger.getLogger(AbstractSubscriptionController.class);

  private static String tncString = "";

  @PostConstruct
  public void getTermsAndConditions() {
    try {
      File file = new File(config.getTncFileLocation());
      BufferedReader br = new BufferedReader(new FileReader(file));
      StringBuilder sb = new StringBuilder();
      String line = br.readLine();
      while (line != null) {
        sb.append(line);
        line = br.readLine();
      }
      br.close();
      tncString = sb.toString();
    } catch (Exception e) {
      logger.debug("Failed to read Terms and conditions file" + e.getMessage());
    }
  }

  private String subscribeBundleGet(HttpServletRequest request, Tenant effectiveTenant, String tenantParam,
      String successView, ModelMap map, ServiceInstance serviceInstance, String resourceType) {
    map.addAttribute("showUserProfile", true);
    map.addAttribute("tenant", effectiveTenant);
    User user = getCurrentUser();
    if (effectiveTenant.getState() == State.NEW) {
      return "redirect:/portal/home?tenant=" + tenantParam + "&secondLevel=true";
    } else if (!userIsRoot(user) || (Boolean) request.getAttribute("isSurrogatedTenant")) {
      boolean isDelinquent = effectiveTenant.isDelinquent();
      if (isDelinquent && hasFullBillingView(user)) {
        return "redirect:/portal/billing/history?tenant=" + tenantParam + "&action=launchvm";
      }
    }
    addValuesForRenderingConnectorValues(map, serviceInstance, resourceType);
    return successView;
  }

  @RequestMapping(value = "/utilityrates_lightbox", method = RequestMethod.GET)
  public String utilityrates_lightbox(
      @RequestParam(value = "serviceInstanceUuid", required = false) String serviceInstanceUuid,
      @RequestParam(value = "resourceTypeName", required = false) String resourceTypeName,
      @RequestParam(value = "contextString", required = false) String contextString,
      @RequestParam(value = "filters", required = false) String filters, ModelMap map, HttpServletRequest request) {
    logger.debug("### utilityrates_lightbox method starting...(GET)");
    Tenant tenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);
    setPage(map, Page.UTILITY_RATE_VIEW);
    List<ServiceResourceTypeGeneratedUsage> generatedUsageListForServiceResourceType = null;
    ServiceResourceType serviceResourceType = null;
    List<ProductRevision> productRevisions = null;
    Date startDate = productService.getCurrentRevision(tenant.getCatalog()).getStartDate();
    if (StringUtils.isNotBlank(serviceInstanceUuid) && StringUtils.isNotBlank(resourceTypeName)) {
      ServiceInstance serviceInstance = connectorConfigurationManager.getInstanceByUUID(serviceInstanceUuid);
      serviceResourceType = connectorConfigurationManager.getServiceResourceType(serviceInstanceUuid, resourceTypeName);
      Map<String, String> discriminators = createStringMap(contextString);
      discriminators.putAll(createStringMap(filters));
      if (!resourceTypeName.equals(SERVICEBUNDLE)) {
        productRevisions = productService.listProductRevisions(serviceInstance, serviceResourceType, discriminators,
            getCurrentUser());
      }
    }
    if (serviceResourceType != null) {
      generatedUsageListForServiceResourceType = serviceResourceType.getServiceResourceGenerate();
    }
    CurrencyValue currency = tenant.getCurrency();
    Channel channel = tenant.getSourceChannel();
    Map<Object, Object> retMap = productService.getCurrentUtilityChargesMap(channel, currency, serviceInstanceUuid,
        null);
    Map<Object, Object> topReturnMap = null;
    if (productRevisions != null && productRevisions.size() > 0) {
      topReturnMap = productService.getCurrentUtilityChargesMap(channel, currency, serviceInstanceUuid,
          productRevisions);
    }
    map.addAttribute("retMap", retMap);
    map.addAttribute("startDate", startDate);
    map.addAttribute("tenant", tenant);
    map.addAttribute("currency", currency);
    map.addAttribute("topReturnMap", topReturnMap);
    map.addAttribute("resourceTypeName", resourceTypeName);
    map.addAttribute("generatedUsageListForServiceResourceType", generatedUsageListForServiceResourceType);
    logger.debug("### utilityrates_lightbox method ending...(GET)");
    return "catalog.utilityrates.lightbox";
  }

  @RequestMapping(value = "/utilityrates_table", method = RequestMethod.GET)
  public String utilityrates_table(@RequestParam(value = "tenant", required = true) String tenantParam,
      @RequestParam(value = "serviceInstanceUuid", required = false) String serviceInstanceUuid,
      @RequestParam(value = "resourceTypeName", required = false) String resourceTypeName,
      @RequestParam(value = "contextString", required = false) String contextString,
      @RequestParam(value = "currencyCode", required = false) String currencyCode,
      @RequestParam(value = "isDialog", required = false) String isDialog, ModelMap map, HttpServletRequest request) {
    logger.debug("### utilityrates_table method starting...(GET)");
    Tenant tenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);
    Channel channel = null;
    CurrencyValue currency = null;
    if (tenant == null) {
      channel = channelService.getDefaultServiceProviderChannel();
      currency = currencyValueService.locateBYCurrencyCode(currencyCode);
    } else {
      channel = tenant.getSourceChannel();
      currency = tenant.getCurrency();
    }
    if (channel == null) {
      channel = channelService.getDefaultServiceProviderChannel();
      if (currencyCode != null && !currencyCode.equals("")) {
        currency = currencyValueService.locateBYCurrencyCode(currencyCode);
      }
    }
    List<ServiceResourceTypeGeneratedUsage> generatedUsageListForServiceResourceType = null;
    Date startDate = productService.getCurrentRevision(channel.getCatalog()).getStartDate();
    ServiceResourceType serviceResourceType = connectorConfigurationManager.getServiceResourceType(serviceInstanceUuid,
        resourceTypeName);
    if (serviceResourceType != null) {
      generatedUsageListForServiceResourceType = serviceResourceType.getServiceResourceGenerate();
    }
    Map<Object, Object> retMap = productService.getCurrentUtilityChargesMap(channel, currency, serviceInstanceUuid,
        null);
    map.addAttribute("retMap", retMap);
    map.addAttribute("startDate", startDate);
    map.addAttribute("generatedUsageListForServiceResourceType", generatedUsageListForServiceResourceType);
    map.addAttribute("tenant", tenant);
    map.addAttribute("currency", currency);
    map.addAttribute("resourceTypeName", resourceTypeName);
    map.addAttribute("isDialog", isDialog);
    logger.debug("### utilityrates_table method ending...(GET)");
    return "catalog.utilityrates.table";
  }

  private boolean hasFullBillingView(User user) {
    // only master user has ROLE_ACCOUNT_ADMIN, certain system users has finance role.
    return userHasPrivilegeOf(user, "ROLE_ACCOUNT_ADMIN") || userHasFinancePrivilege(user)
        || userHasBillingPrivilege(user);
  }

  @RequestMapping(value = "/taxable_amount", method = RequestMethod.GET)
  @ResponseBody
  public String getTaxableAmount(@RequestParam("amount") final String amount) {
    logger.debug("getTaxableAmount method starting...");
    BigDecimal taxableAmount = BigDecimal.ZERO;
    try {
      if (amount != null) {
        taxableAmount = billingAdminService.getTaxableAmount(new BigDecimal(amount));
      }
    } catch (Exception e) {
      logger.error("Failed to get taxable amount", e);
    }
    logger.debug("getTaxableAmount method end");
    return taxableAmount.toString();
  }

  @RequestMapping(value = ("/createsubscription"), method = RequestMethod.GET)
  public String createSubscription(@ModelAttribute("currentTenant") Tenant currentTenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "serviceInstanceUUID", required = false) String serviceInstanceUUID,
      @RequestParam(value = "subscriptionId", required = false) String subscriptionId,
      @RequestParam(value = "resourceType", required = false) String resourceType, ModelMap map,
      HttpServletRequest request) throws ConnectorManagementServiceException {
    logger.debug("### createsubscription method starting...(GET)");
    setPage(map, Page.VM_BUNDLES_SUBSCRIPTION);
    String successView = "subscriptions.new";
    Tenant tenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);
    String sourceChannelName = tenant.getSourceChannel().getName();
    return getResourceComponentsAndFilterData(tenant, tenantParam, serviceInstanceUUID, subscriptionId, resourceType,
        map, request, successView, sourceChannelName);
  }

  private String getResourceComponentsAndFilterData(Tenant tenant, String tenantParam, String serviceInstanceUUID,
      String subscriptionId, String resourceType, ModelMap map, HttpServletRequest request, String successView,
      String sourceChannelName) throws ConnectorManagementServiceException {

    User user = getCurrentUser();
    List<String> currentUserServiceCategoryList = userService.getAllAccessibleCloudServiceCategories(user);
    if ((Boolean) request.getAttribute("isSurrogatedTenant")) {
      map.addAttribute("showUserProfile", true); // if service provider
      user = tenant.getOwner();
    }
    ServiceInstance serviceInstance = null;
    List<String> serviceCategoryList = userService.getAllAccessibleCloudServiceCategories(user);
    serviceCategoryList.retainAll(currentUserServiceCategoryList);

    boolean isPayAsYouGoChosen = config.getBooleanValue(Names.com_citrix_cpbm_catalog_payAsYouGoMode);
    map.addAttribute("isPayAsYouGoChosen", isPayAsYouGoChosen);
    map.addAttribute("userHasCloudServiceAccount", userService.isUserHasAnyActiveCloudService(user));
    map.addAttribute("tenant", tenant);
    map.addAttribute("serviceCategoryList", serviceCategoryList);
    map.addAttribute("chargeRecurrenceFrequencyList", productBundleService.getChargeRecurrenceFrequencyList());
    // If there is a subscription get the service instance directly from subscription
    if (StringUtils.isNotBlank(subscriptionId)) {
      Subscription subscription = subscriptionService.locateSubscriptionById(Long.parseLong(subscriptionId));
      resourceType = subscription.getResourceType().getResourceTypeName();
      serviceInstance = subscription.getServiceInstance();
      serviceInstanceUUID = subscription.getServiceInstance().getUuid();
      map.addAttribute("selectedCloudServiceInstance", serviceInstanceUUID);
      map.addAttribute("selectedCategory", serviceInstance.getService().getCategory());
    } else {
      // if service instance is null get the first cloud service instance
      if (serviceInstanceUUID == null) {
        for (String category : serviceCategoryList) {
          List<ServiceInstance> currentUserCloudTypeServiceInstances = userService.getCloudServiceInstance(
              getCurrentUser(), category);
          List<ServiceInstance> cloudTypeServiceInstances = userService.getCloudServiceInstance(user, category);
          cloudTypeServiceInstances.retainAll(currentUserCloudTypeServiceInstances);
          if (cloudTypeServiceInstances != null && cloudTypeServiceInstances.size() > 0) {
            serviceInstance = cloudTypeServiceInstances.get(0);
            serviceInstanceUUID = serviceInstance.getUuid();
            break;
          }
        }
      } else {
        // get the service instance from service instance uuid
        serviceInstance = connectorConfigurationManager.getInstanceByUUID(serviceInstanceUUID);
        map.addAttribute("selectedCloudServiceInstance", serviceInstanceUUID);
        map.addAttribute("selectedCategory", serviceInstance.getService().getCategory());
      }
    }
    if (serviceInstance != null) {
      try {
        String tenantDataJsonStr = tenantService.getTenantHandle(tenant.getUuid(), serviceInstanceUUID).getData();
        Service service = serviceInstance.getService();
        if (StringUtils.isBlank(resourceType)) {
          // No resource type is given. Enable the first in the list.
          resourceType = service.getServiceResourceTypes().get(0).getResourceTypeName();
        }
        map.addAttribute("tenantDataJsonStr", tenantDataJsonStr);
        map.addAttribute("service", service);
        map.addAttribute("serviceInstanceUuid", serviceInstanceUUID);
        map.addAttribute("resourceTypes", service.getServiceResourceTypes());
        map.addAttribute("resourceType", resourceType);
        map.addAttribute("serviceBundleResourceType", SERVICEBUNDLE);

        ServiceResourceType serviceResourceType = connectorConfigurationManager.getServiceResourceType(
            serviceInstanceUUID, resourceType);
        if (serviceResourceType != null) {
          List<String> uniqueResourceComponentNames = new ArrayList<String>();
          uniqueResourceComponentNames = getUniqueResourceComponents(serviceInstanceUUID, resourceType);
          map.addAttribute("uniqueResourceComponentNames", uniqueResourceComponentNames);
          map.addAttribute("groups", serviceResourceType.getServiceResourceGroups());
        }
        if (StringUtils.isNotBlank(subscriptionId)) {
          Subscription subscription = subscriptionService.locateSubscriptionById(Long.parseLong(subscriptionId));
          JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(subscription.getConfigurationData());
          map.addAttribute("subscription", subscription);
          map.addAttribute("configurationData", jsonObject);
          com.citrix.cpbm.access.Subscription subscriptionProxy = (com.citrix.cpbm.access.Subscription) CustomProxy
              .newInstance(subscription);
          SubscriptionForm subscriptionForm = new SubscriptionForm(subscriptionProxy);
          map.addAttribute("subscriptionForm", subscriptionForm);
        } else {
          com.citrix.cpbm.access.Subscription subscription = (com.citrix.cpbm.access.Subscription) CustomProxy
              .newInstance(new Subscription());
          SubscriptionForm subscriptionForm = new SubscriptionForm(subscription);
          map.addAttribute("subscriptionForm", subscriptionForm);
        }
        addValuesForRenderingConnectorValues(map, serviceInstance, resourceType);
        boolean isAlive = ((CloudConnector) connectorManagementService.getServiceInstance(serviceInstanceUUID))
            .getStatus();
        if (!isAlive) {
          throw new CloudServiceException(messageSource.getMessage("cloud.service.down", null, user.getLocale()));
        }
      } catch (CloudServiceException cse) {
        map.addAttribute("cloudServiceException", true);
        map.addAttribute("cloudServiceExceptionStr", cse.getMessage());
      } catch (Exception e) {
        logger.error("Error in creating the Service Instance Map...", e);
        throw new ConnectorManagementServiceException("Error in creating the Service Instance Map...", e);
      }
      map.addAttribute("tnc", tncString);
      map.addAttribute("sourceChannelName", sourceChannelName);
      String result = subscribeBundleGet(request, tenant, tenantParam, successView, map, serviceInstance, resourceType);
      return result;
    } else {
      map.addAttribute("cloudServiceException", true);
      map.addAttribute("cloudServiceExceptionStr", "");
      return successView;
    }
  }

  @RequestMapping(value = {
    "/getFilterComponents"
  }, method = RequestMethod.GET)
  @ResponseBody
  public List<FilterComponent> getFilterComponents(@ModelAttribute("currentTenant") Tenant currentTenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "serviceInstanceUuid", required = true) final String serviceInstanceUuid,
      @RequestParam(value = "filterType", required = true) final String filterType,
      @RequestParam(value = "viewCatalog", required = false, defaultValue = "false") Boolean viewCatalog,
      HttpServletRequest request) throws ConnectorManagementServiceException {
    List<FilterComponent> filterComponents = new ArrayList<FilterComponent>();
    if (getCurrentUser() == null
        || (viewCatalog == true && getCurrentUser().getTenant().equals(tenantService.getSystemTenant()))) {
      filterComponents = privilegeService.runAsPortal(new PrivilegedAction<List<FilterComponent>>() {

        @Override
        public List<FilterComponent> run() {
          return ((CloudConnector) connectorManagementService.getServiceInstance(serviceInstanceUuid))
              .getMetadataRegistry().getFilterValues(
                  tenantService.getTenantHandle(tenantService.getSystemTenant().getUuid(), serviceInstanceUuid)
                      .getHandle(),
                  userService.getUserHandleByServiceInstanceUuid(tenantService.getSystemUser(Handle.PORTAL).getUuid(),
                      serviceInstanceUuid).getHandle(), filterType);
        }
      });

    } else {

      User user = getCurrentUser();
      Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);
      if ((Boolean) request.getAttribute("isSurrogatedTenant")) {
        user = effectiveTenant.getOwner();
      }

      String userHandle = userService.getUserHandleByServiceInstanceUuid(user.getUuid(), serviceInstanceUuid)
          .getHandle();

      filterComponents = ((CloudConnector) connectorManagementService.getServiceInstance(serviceInstanceUuid))
          .getMetadataRegistry().getFilterValues(
              tenantService.getTenantHandle(effectiveTenant.getUuid(), serviceInstanceUuid).getHandle(), userHandle,
              filterType);
    }
    return filterComponents;
  }

  @RequestMapping(value = {
    "/getFilterComponentsForBundle"
  }, method = RequestMethod.GET)
  @ResponseBody
  public List<FilterComponent> getFilterComponentsForBundle(@ModelAttribute("currentTenant") Tenant currentTenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "serviceInstanceUuid", required = true) String serviceInstanceUuid,
      @RequestParam(value = "filterType", required = true) String filterType,
      @RequestParam(value = "bundleId", required = true) Long bundleId, HttpServletRequest request)
      throws ConnectorManagementServiceException {
    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);

    List<FilterComponent> filterComponents = new ArrayList<FilterComponent>();
    List<FilterComponent> allFilterComponents = getFilterComponents(currentTenant, tenantParam, serviceInstanceUuid,
        filterType, false, request);

    // Get specified bundle by revision.
    ProductBundle productBundle = productBundleService.getProductBundleById(bundleId);
    ProductBundleRevision selectedProductBundleRevision = productBundleService
        .getCurrentProductBundleRevisionForTenant(productBundle, effectiveTenant);

    for (FilterComponent fc : allFilterComponents) {

      Map<String, String> discriminatorMap = new HashMap<String, String>();
      discriminatorMap.put(filterType, fc.getValue());

      if (productBundleService.isValidBundleForGivenDiscriminators(
          selectedProductBundleRevision.getProvisioningConstraints(), discriminatorMap)) {
        filterComponents.add(fc);
      }
    }
    return filterComponents;
  }

  @RequestMapping(value = {
    "/getResourceComponents"
  }, method = RequestMethod.GET)
  @ResponseBody
  public List<ResourceComponent> getResourceComponents(@ModelAttribute("currentTenant") Tenant currentTenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "serviceInstanceUuid", required = true) final String serviceInstanceUuid,
      @RequestParam(value = "resourceType", required = true) final String resourceType,
      @RequestParam(value = "componentType", required = true) final String componentType,
      @RequestParam(value = "contextString", required = true) final String contextString,
      @RequestParam(value = "viewCatalog", required = false, defaultValue = "false") Boolean viewCatalog,
      @RequestParam(value = "filters", required = false) final String filters, HttpServletRequest request)
      throws ConnectorManagementServiceException {
    List<ResourceComponent> resourceComponents = new ArrayList<ResourceComponent>();
    if (getCurrentUser() == null
        || (viewCatalog == true && getCurrentUser().getTenant().equals(tenantService.getSystemTenant()))) {
      resourceComponents = privilegeService.runAsPortal(new PrivilegedAction<List<ResourceComponent>>() {

        @Override
        public List<ResourceComponent> run() {
          return ((CloudConnector) connectorManagementService.getServiceInstance(serviceInstanceUuid))
              .getMetadataRegistry().getResourceComponentValues(
                  resourceType,
                  componentType,
                  tenantService.getTenantHandle(tenantService.getSystemTenant().getUuid(), serviceInstanceUuid)
                      .getHandle(),
                  userService.getUserHandleByServiceInstanceUuid(tenantService.getSystemUser(Handle.PORTAL).getUuid(),
                      serviceInstanceUuid).getHandle(), createStringMap(contextString), createStringMap(filters));
        }
      });

    } else {

      User user = getCurrentUser();
      Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);
      if ((Boolean) request.getAttribute("isSurrogatedTenant")) {
        user = effectiveTenant.getOwner();
      }

      String userHandle = userService.getUserHandleByServiceInstanceUuid(user.getUuid(), serviceInstanceUuid)
          .getHandle();

      resourceComponents = ((CloudConnector) connectorManagementService.getServiceInstance(serviceInstanceUuid))
          .getMetadataRegistry().getResourceComponentValues(resourceType, componentType,
              tenantService.getTenantHandle(getTenant().getUuid(), serviceInstanceUuid).getHandle(), userHandle,
              createStringMap(contextString), createStringMap(filters));
    }
    return resourceComponents;
  }

  @RequestMapping(value = {
    "/getResourceComponentsForBundle"
  }, method = RequestMethod.GET)
  @ResponseBody
  public List<ResourceComponent> getResourceComponentsForBundle(@ModelAttribute("currentTenant") Tenant currentTenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "serviceInstanceUuid", required = true) String serviceInstanceUuid,
      @RequestParam(value = "resourceType", required = true) String resourceType,
      @RequestParam(value = "componentType", required = true) String componentType,
      @RequestParam(value = "effComponentType", required = false) String effComponentType,
      @RequestParam(value = "contextString", required = false) String contextString,
      @RequestParam(value = "filters", required = false) String filters,
      @RequestParam(value = "bundleId", required = true) Long bundleId, HttpServletRequest request)
      throws ConnectorManagementServiceException {
    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);

    List<ResourceComponent> resourceComponents = new ArrayList<ResourceComponent>();
    List<ResourceComponent> allResourceComponents = getResourceComponents(currentTenant, tenantParam,
        serviceInstanceUuid, resourceType, componentType, contextString, false, filters, request);

    // Get specified bundle by revision.
    ProductBundle productBundle = productBundleService.getProductBundleById(bundleId);
    ProductBundleRevision selectedProductBundleRevision = productBundleService
        .getCurrentProductBundleRevisionForTenant(productBundle, effectiveTenant);

    for (ResourceComponent rc : allResourceComponents) {

      String name = effComponentType;
      if (StringUtils.isBlank(name)) {
        name = componentType;
      }
      String value = rc.getValue();
      if (rc.getParent() != null && rc.getParent().getValue() != null) {
        value = rc.getParent().getValue();
      }

      Map<String, String> discriminatorMap = new HashMap<String, String>();
      discriminatorMap.put(name, value);

      if (productBundleService.isValidBundleForGivenDiscriminators(
          selectedProductBundleRevision.getProvisioningConstraints(), discriminatorMap)) {
        resourceComponents.add(rc);
      }
    }
    return resourceComponents;
  }

  @RequestMapping(value = {
    "/{customPageTag}/{serviceInstanceUuid}/{resourceType}"
  }, method = RequestMethod.GET)
  @ResponseBody
  public void getCustomSelector(@PathVariable String serviceInstanceUuid, @PathVariable String resourceType,
      @PathVariable String customPageTag, ModelMap map, HttpServletResponse response) {
    FileInputStream fileinputstream = null;
    try {
      Service service = connectorConfigurationManager.getInstance(serviceInstanceUuid).getService();
      ServiceResourceType selectedResourceType = null;
      for (ServiceResourceType serviceResourceType : service.getServiceResourceTypes()) {
        if (serviceResourceType.getResourceTypeName().equals(resourceType)) {
          selectedResourceType = serviceResourceType;
          break;
        }
      }
      String jspPath = "";
      String cssdkFilesDirectory = FilenameUtils.concat(
          config.getValue(Names.com_citrix_cpbm_portal_settings_services_datapath), service.getServiceName() + "_"
              + service.getVendorVersion());
      if (selectedResourceType != null) {
        if (customPageTag.equalsIgnoreCase("customComponentSelector")) {
          jspPath = selectedResourceType.getComponentSelector();
        } else if (customPageTag.equalsIgnoreCase("customEditorTag")) {
          jspPath = selectedResourceType.getEditor();
        }
        if (jspPath != null && !jspPath.trim().equals("")) {
          String absoluteJspPath = FilenameUtils.concat(cssdkFilesDirectory, jspPath);
          fileinputstream = new FileInputStream(absoluteJspPath);
          if (fileinputstream != null) {
            int numberBytes = fileinputstream.available();
            byte bytearray[] = new byte[numberBytes];
            fileinputstream.read(bytearray);
            response.setContentType("text/html");
            OutputStream outputStream = response.getOutputStream();
            response.setContentLength(numberBytes);
            outputStream.write(bytearray);
            outputStream.flush();
            outputStream.close();
            fileinputstream.close();
            return;
          }
        }
      }
    } catch (FileNotFoundException e) {
      logger.debug("###File not found in retrieving custom ui contribution.");
    } catch (IOException e) {
      logger.debug("###IO Error in retrieving custom ui contribution.");
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
  }

  private void addValuesForRenderingConnectorValues(ModelMap map, ServiceInstance serviceInstance,
      String resourceTypeName) {
    try {
      Service service = serviceInstance.getService();
      map.addAttribute("prefix", service.getServiceName());
      ServiceResourceType serviceResourceType = connectorConfigurationManager.getServiceResourceType(
          serviceInstance.getUuid(), resourceTypeName);
      if (resourceTypeName.equals(SERVICEBUNDLE) || serviceResourceType != null) {
        if (serviceResourceType != null) {
          String finalEditorTag = serviceResourceType.getEditor();
          String componentSelector = serviceResourceType.getComponentSelector();
          List<ServiceResourceTypeProperty> serviceResourceTypePropertyList = serviceResourceType
              .getServiceResourceTypeProperty();
          map.addAttribute("customEditorTag", finalEditorTag);
          map.addAttribute("customComponentSelector", componentSelector);
          map.addAttribute("resourceProperties", serviceResourceTypePropertyList);
        }
      } else {
        throw new ConnectorManagementServiceException(
            "No Service Resource Type Configured for the provided Resource Type", new RuntimeException(
                "No Service Resource Type Configured for the provided Resource Type"));
      }
    } catch (ConnectorManagementServiceException e) {
      throw new RuntimeException(e);
    }

  }

  @RequestMapping(value = ("/view_catalog"), method = RequestMethod.GET)
  public String previewCatalog(@RequestParam(value = "channelParam", required = false) String channelParam,
      ModelMap map, @RequestParam(value = "tenant", required = false) final String tenantParam,
      @RequestParam(value = "serviceInstanceUUID", required = false) final String serviceInstanceUUID,
      @RequestParam(value = "subscriptionId", required = false) String subscriptionId,
      @RequestParam(value = "revision", required = false) String revision,
      @RequestParam(value = "revisionDate", required = false) String revisionDate,
      @RequestParam(value = "dateFormat", required = false) String dateFormat,
      @RequestParam(value = "currencyCode", required = false) String currencyCode,
      @RequestParam(value = "resourceType", required = false) final String resourceType,
      final HttpServletRequest request) throws ConnectorManagementServiceException {
    logger.debug("### viewCatalog method starting...(GET)");
    Channel channel = null;
    String successView = "channels.catalog.view";
    if (channelParam != null && !channelParam.equals("null") && channelParam != "") {
      channel = channelService.getChannelById(channelParam);
    } else {
      channel = channelService.getDefaultServiceProviderChannel();
    }
    final Catalog catalog = channel.getCatalog();
    List<CurrencyValue> currencies = catalog.getSupportedCurrencyValuesByOrder();
    map.addAttribute("channel", channel);
    map.addAttribute("currencies", currencies);
    map.addAttribute("viewChannelCatalog", true);
    map.addAttribute("revision", revision);
    map.addAttribute("revisionDate", revisionDate);
    map.addAttribute("dateFormat", dateFormat);
    if (StringUtils.isNotBlank(currencyCode)) {
      CurrencyValue currency = currencyValueService.locateBYCurrencyCode(currencyCode);
      map.addAttribute("selectedCurrency", currency);
    } else {
      map.addAttribute("selectedCurrency", currencies.get(0));
    }

    final Tenant tenant = tenantService.getSystemTenant();
    final String finalView = successView;
    final Channel catalogChannel = channel;
    Map<String, Object> finalMap = privilegeService.runAsPortal(new PrivilegedAction<Map<String, Object>>() {

      @Override
      public Map<String, Object> run() {
        ModelMap modelMap = new ModelMap();
        try {
          getResourceComponentsAndFilterData(tenant, tenantParam, serviceInstanceUUID, null, resourceType, modelMap,
              request, finalView, catalogChannel.getName());
        } catch (ConnectorManagementServiceException e) {
          logger.debug("Error occured ", e);
        }
        return modelMap;
      }
    });
    map.addAllAttributes(finalMap);
    // preview catalog will have default UI Because in cutom UI SSO happens which can leads to
    // security threats
    map.addAttribute("customEditorTag", null);
    map.addAttribute("customComponentSelector", null);
    return finalView;
  }

  @RequestMapping(value = ("/getUniqueResourceComponents"), method = RequestMethod.GET)
  @ResponseBody
  public List<String> getUniqueResourceComponents(
      @RequestParam(value = "serviceInstanceUUID", required = true) String serviceInstanceUUID,
      @RequestParam(value = "resourceType", required = true) String resourceType) {
    List<String> uniqueResourceComponentNames = new ArrayList<String>();
    List<ServiceResourceTypeGroupComponent> uniqueResourceTypeComponents = connectorConfigurationManager
        .getResourceComponents(serviceInstanceUUID, resourceType);
    for (ServiceResourceTypeGroupComponent serviceResourceTypeGroupComponent : uniqueResourceTypeComponents) {
      String resourceComponentName = serviceResourceTypeGroupComponent.getResourceComponentName();
      if (!uniqueResourceComponentNames.contains(resourceComponentName)) {
        uniqueResourceComponentNames.add(serviceResourceTypeGroupComponent.getResourceComponentName());
      }
    }
    return uniqueResourceComponentNames;
  }

  @RequestMapping(value = ("/browse_catalog"), method = RequestMethod.GET)
  public String anonymousCatalog(ModelMap map,
      @RequestParam(value = "serviceInstanceUUID", required = false) final String serviceInstanceUUID,
      @RequestParam(value = "currencyCode", required = false) String currencyCode,
      @RequestParam(value = "resourceType", required = false) final String resourceType,
      @RequestParam(value = "channelCode", required = false) final String channelCode, final HttpServletRequest request) {
    logger.debug("### anonymousCatalog method starting...(GET)");
    final String successView = "anonymous.catalog";
    if (config.getValue(Names.com_citrix_cpbm_public_catalog_display).equals("true")) {

      Channel channel = null;
      if (StringUtils.isNotBlank(channelCode)) {
        channel = channelService.locateByChannelCode(channelCode);
      } else {
        channel = channelService.getDefaultServiceProviderChannel();
      }

      final Catalog catalog = channel.getCatalog();
      List<CurrencyValue> currencies = catalog.getSupportedCurrencyValuesByOrder();
      map.addAttribute("channel", channel);
      map.addAttribute("currencies", currencies);
      map.addAttribute("anonymousBrowsing", true);
      if (StringUtils.isNotBlank(currencyCode)) {
        CurrencyValue currency = currencyValueService.locateBYCurrencyCode(currencyCode);
        map.addAttribute("selectedCurrency", currency);
      } else {
        map.addAttribute("selectedCurrency", currencies.get(0));
      }

      final Tenant tenant = tenantService.getSystemTenant();
      final Channel finalChannel = channel;
      Map<String, Object> finalMap = privilegeService.runAsPortal(new PrivilegedAction<Map<String, Object>>() {

        @Override
        public Map<String, Object> run() {
          ModelMap modelMap = new ModelMap();
          try {
            getResourceComponentsAndFilterData(tenant, null, serviceInstanceUUID, null, resourceType, modelMap,
                request, successView, finalChannel.getName());
          } catch (ConnectorManagementServiceException e) {
            logger.debug("Error occured ", e);
          }
          return modelMap;
        }
      });
      map.addAllAttributes(finalMap);
      map.addAttribute("supportedLocaleList", this.getLocaleDisplayName(listSupportedLocales()));
      // anonymousBrowsing and preview catalog will have default UI Because in cutom UI SSO happens which can leads to
      // security threats
      map.addAttribute("customEditorTag", null);
      map.addAttribute("customComponentSelector", null);
    } else {
      throw new NoSuchDefinitionException();
    }
    return successView;
  }

  /**
   * @param currentTenant the current tenant
   * @return resource Handle
   */
  @RequestMapping(value = "/subscribe_resource", method = RequestMethod.POST)
  @ResponseBody
  public Map<String, String> provisionOrReconfigureSubscription(
      @ModelAttribute("subscriptionForm") SubscriptionForm subscriptionForm, BindingResult result,
      @RequestParam(value = "tenant", required = true) String tenantParam,
      @RequestParam(value = "productBundleId", required = false) String productBundleId,
      @RequestParam(value = "isProvision", required = false, defaultValue = "false") boolean isProvision,
      @RequestParam(value = "configurationData", required = false) String configurationData,
      @RequestParam(value = "serviceInstaceUuid", required = false) String serviceInstaceUuid,
      @RequestParam(value = "resourceType", required = false) String resourceType,
      @RequestParam(value = "filters", required = false) String filters,
      @RequestParam(value = "context", required = false) String context,
      @RequestParam(value = "subscriptionId", required = false) String subscriptionId,
      @RequestParam(value = "newSubscriptionId", required = false) String newSubscriptionId, ModelMap map,
      HttpServletResponse response, HttpServletRequest request) {

    Map<String, String> responseMap = new HashMap<String, String>();
    if (!resourceType.equals(SERVICEBUNDLE)) {
      Service service = connectorConfigurationManager.getInstance(serviceInstaceUuid).getService();
      for (ServiceResourceType serviceResourceType : service.getServiceResourceTypes()) {
        if (serviceResourceType.getResourceTypeName().equals(resourceType)) {
          for (ServiceResourceTypeProperty serviceResourceTypeProperty : serviceResourceType
              .getServiceResourceTypeProperty()) {
            JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(configurationData);
            String fieldValue = (String) jsonObject.get(serviceResourceTypeProperty.getName());
            String validationResult = CssdkConstants.FAILURE;
            try {
              String propertyName = service.getServiceName() + ".ResourceType." + resourceType + "."
                  + serviceResourceTypeProperty.getName() + ".name";
              validationResult = ValidationUtil.valid(serviceResourceTypeProperty.getValidation(),
                  messageSource.getMessage(propertyName, null, getSessionLocale(request)), fieldValue, messageSource);
            } catch (Exception e) {
              logger.error(e);
            }
            if (!CssdkConstants.SUCCESS.equals(validationResult)) {
              responseMap.put("validationResult", validationResult);
              response.setStatus(AJAX_FORM_VALIDATION_FAILED_CODE);
              return responseMap;
            }

          }
          break;
        }
      }
    }

    Map<String, String> filterMap = createStringMap(filters);
    Map<String, String> contextMap = createStringMap(context);

    User user = getCurrentUser();
    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);

    User effectiveUser = user;
    if ((Boolean) request.getAttribute("isSurrogatedTenant")) {
      effectiveUser = effectiveTenant.getOwner();
    }
    Map<String, String> configurationJsonMap = getConfigurationJsonString(filterMap, contextMap, configurationData);
    Subscription subscription = null;
    ArrayList<String> subscriptionAction = new ArrayList<String>();
    try {
      subscription = subscriptionService.provisionOrReconfigureSubscription(subscriptionForm.getSubscription(), result,
          isProvision, serviceInstaceUuid, resourceType, subscriptionId, newSubscriptionId, productBundleId,
          effectiveUser, configurationJsonMap, subscriptionAction);
    } catch (Exception e) {
      String subscriptionActionMessage = "";
      if (subscriptionAction.size() == 1) {
        subscriptionActionMessage = subscriptionAction.get(0);
      }
      throw new SubscriptionServiceException(subscriptionActionMessage + ":" + e.getMessage());
    }

    if (result.hasErrors()) {
      throw new AjaxFormValidationException(result);
    }

    if (subscription != null) {
      responseMap.put("subscriptionId", subscription.getUuid());
      String resourceHandle = "";
      if (subscription.getActiveHandle() != null) {
        resourceHandle = subscription.getActiveHandle().getResourceHandle();
      }
      responseMap.put("jobId", resourceHandle);
      if (subscriptionAction.size() == 1) {
        responseMap.put("subscriptionResultMessage", subscriptionAction.get(0));
      }
    }
    return responseMap;
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> getConfigurationJsonString(Map<String, String> filterMap, Map<String, String> contextMap,
      String configurationData) {
    JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(configurationData);
    for (String key : filterMap.keySet()) {
      jsonObject.put(key, filterMap.get(key));
    }
    for (String key : contextMap.keySet()) {
      jsonObject.put(key, contextMap.get(key));
    }
    return jsonObject;
  }

}
