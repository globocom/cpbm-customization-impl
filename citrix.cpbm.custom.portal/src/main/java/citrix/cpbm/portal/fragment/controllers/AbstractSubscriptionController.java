/* Copyright (C) 2011 Citrix Systems, Inc. All rights reserved. */
package citrix.cpbm.portal.fragment.controllers;

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
import java.util.ListIterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import citrix.cpbm.access.proxy.CustomProxy;
import citrix.cpbm.portal.forms.SubscriptionForm;

import com.citrix.cpbm.platform.admin.service.ConnectorConfigurationManager;
import com.citrix.cpbm.platform.admin.service.ConnectorManagementService;
import com.citrix.cpbm.platform.admin.service.exceptions.ConnectorManagementServiceException;
import com.citrix.cpbm.platform.spi.CloudConnector;
import com.citrix.cpbm.platform.spi.ResourceComponent;
import com.vmops.internal.service.SubscriptionService;
import com.vmops.model.Catalog;
import com.vmops.model.Channel;
import com.vmops.model.CurrencyValue;
import com.vmops.model.Product;
import com.vmops.model.ProductBundle;
import com.vmops.model.ProductBundleRevision;
import com.vmops.model.ProductCharge;
import com.vmops.model.ProductRevision;
import com.vmops.model.Service;
import com.vmops.model.ServiceFilter;
import com.vmops.model.ServiceInstance;
import com.vmops.model.ServiceResourceType;
import com.vmops.model.ServiceResourceTypeGeneratedUsage;
import com.vmops.model.ServiceResourceTypeGroup;
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
import com.vmops.service.billing.BillingAdminService;
import com.vmops.service.exceptions.CloudServiceException;
import com.vmops.web.controllers.AbstractAuthenticatedController;
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.interceptors.UserContextInterceptor;

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
  private ConnectorConfigurationManager connectorConfigurationManagerService;

  @Autowired
  private BillingAdminService billingAdminService;

  @Autowired
  private ChannelService channelService;

  @Autowired
  private CurrencyValueService currencyValueService;;

  Logger logger = Logger.getLogger(AbstractSubscriptionController.class);


  private String getTermsAndConditions() {
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
      return sb.toString();
    } catch (Exception e) {
      logger.debug("Failed to read Terms and conditions file" + e.getMessage());
      return "";
    }
  }

  private String subscribeBundleGet(HttpServletRequest request, Tenant effectiveTenant, String tenantParam,
      String successView, ModelMap map,
      String serviceInstanceUuid, String resourceType) {
    map.addAttribute("showUserProfile", true);
    map.addAttribute("tenant", effectiveTenant);
    User user = getCurrentUser(true);
    if (effectiveTenant.getState() == State.NEW) {
      return "redirect:/portal/home?tenant=" + tenantParam + "&secondLevel=true";
    } else if (!userIsRoot(user) || (Boolean) request.getAttribute("isSurrogatedTenant")) {
      boolean isDelinquent = effectiveTenant.isDelinquent();
      if (isDelinquent && hasFullBillingView(user)) {
        return "redirect:/portal/billing/history?tenant=" + tenantParam + "&action=launchvm";
      }
    }
    addValuesForRenderingConnectorValues(map, serviceInstanceUuid, resourceType);
    return successView;
  }

  @RequestMapping(value = "/utilityrates", method = RequestMethod.GET)
  public String utilityrates(@ModelAttribute("currentTenant") Tenant currentTenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "serviceInstanceUuid", required = false) String serviceInstanceUuid,
      @RequestParam(value = "resourceTypeName", required = false) String resourceTypeName,
      @RequestParam(value = "contextString", required = false) String contextString,
      @RequestParam(value = "filters", required = false) String filters, ModelMap map, HttpServletRequest request) {
    return utilityrates_lightbox(currentTenant, tenantParam, serviceInstanceUuid, resourceTypeName, contextString,
        filters, map, request);
  }

  @RequestMapping(value = "/utilityrates.json", method = RequestMethod.GET)
  @ResponseBody
  public Product listUtilityRates(@RequestParam(value = "tenant", required = true) String tenantParam,
      @RequestParam(value = "productName", required = true) String productName) {
    logger.debug("### utilityrates method starting...(GET)");
    Tenant tenant = tenantService.get(tenantParam);
    List<Product> products = productService.listProducts(productName, null, null,
        channelService.getCurrentRevision(tenant.getSourceChannel()));
    Product selectedProduct = null;
    for (Product product : products) {
      ProductCharge charge = productService.getProductChannelCharges(product, tenant.getSourceChannel(),
          tenant.getCurrency(), new Date());
      if (charge != null) {
        product.setPrice(charge.getPrice());
      }
      selectedProduct = product;
      break;
    }
    return selectedProduct;
  }

  @RequestMapping(value = "/utilityrates_lightbox", method = RequestMethod.GET)
  public String utilityrates_lightbox(@ModelAttribute("currentTenant") Tenant currentTenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "serviceInstanceUuid", required = false) String serviceInstanceUuid,
      @RequestParam(value = "resourceTypeName", required = false) String resourceTypeName,
      @RequestParam(value = "contextString", required = false) String contextString,
      @RequestParam(value = "filters", required = false) String filters, ModelMap map, HttpServletRequest request) {
    logger.debug("### utilityrates_lightbox method starting...(GET)");
    Tenant tenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);
    setPage(map, Page.UTILITY_RATE_VIEW);
    List<ServiceResourceTypeGeneratedUsage> generatedUsageListForServiceResourceType = null;
    ServiceResourceType serviceResourceType = null;
    List<ProductRevision> revisions = null;
    Date startDate = productService.getCurrentRevision(tenant.getCatalog()).getStartDate();
    if (StringUtils.isNotBlank(serviceInstanceUuid) && StringUtils.isNotBlank(resourceTypeName)) {
      ServiceInstance serviceInstance = connectorConfigurationManager.getInstanceByUUID(serviceInstanceUuid);
      com.vmops.model.Service service = serviceInstance.getService();
      List<ServiceResourceType> resourceTypes = service.getServiceResourceTypes();
      for (ServiceResourceType resType : resourceTypes) {
        if (resType.getResourceTypeName().equals(resourceTypeName)) {
          serviceResourceType = resType;
          break;
        }
      }
      Map<String, String> discriminators = createStringMap(contextString);
      discriminators.putAll(createStringMap(filters));
      if (!resourceTypeName.equals(SERVICEBUNDLE)) {
        revisions = productService.listProductRevisions(serviceInstance, serviceResourceType, discriminators,
            getCurrentUser(), startDate);
      }
    }
    if (serviceResourceType != null) {
      generatedUsageListForServiceResourceType = serviceResourceType.getServiceResourceGenerate();
    }
    map.addAttribute("generatedUsageListForServiceResourceType", generatedUsageListForServiceResourceType);
    CurrencyValue currency = tenant.getCurrency();
    Channel channel = tenant.getSourceChannel();
    map.addAttribute("tenant", tenant);
    map.addAttribute("currency", currency);
    Map<Object, Object> retMap = productService.getCurrentUtilityChargesMap(channel, currency, serviceInstanceUuid,
        null);
    map.addAttribute("retMap", retMap);
    map.addAttribute("startDate", startDate);

    Map<Object, Object> topReturnMap = null;
    if (revisions != null && revisions.size() > 0) {
      topReturnMap = productService.getCurrentUtilityChargesMap(channel, currency, serviceInstanceUuid, revisions);
    }
    map.addAttribute("topReturnMap", topReturnMap);
    map.addAttribute("resourceTypeName", resourceTypeName);

    logger.debug("### utilityrates_lightbox method ending...(GET)");
    return "catalog.utilityrates.lightbox";
  }
  
  
  @RequestMapping(value = "/utilityrates_table", method = RequestMethod.GET)
  public String utilityrates_table(@ModelAttribute("currentTenant") Tenant currentTenant,
      @RequestParam(value = "tenant", required = true) String tenantParam,
      @RequestParam(value = "serviceInstanceUuid", required = true) String serviceInstanceUuid,
      @RequestParam(value = "resourceTypeName", required = false) String resourceTypeName,
      @RequestParam(value = "contextString", required = false) String contextString,
      @RequestParam(value = "currencyCode", required = false) String currencyCode,
      @RequestParam(value = "filters", required = false) String filters, ModelMap map, HttpServletRequest request) {
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
    List<ServiceResourceTypeGeneratedUsage> generatedUsageListForServiceResourceType = null;
    ServiceResourceType serviceResourceType = null;
    Date startDate = productService.getCurrentRevision(channel.getCatalog()).getStartDate();
    if (StringUtils.isNotBlank(serviceInstanceUuid) && StringUtils.isNotBlank(resourceTypeName)) {
      ServiceInstance serviceInstance = connectorConfigurationManager.getInstanceByUUID(serviceInstanceUuid);
      com.vmops.model.Service service = serviceInstance.getService();
      List<ServiceResourceType> resourceTypes = service.getServiceResourceTypes();
      for (ServiceResourceType resType : resourceTypes) {
        if (resType.getResourceTypeName().equals(resourceTypeName)) {
          serviceResourceType = resType;
          break;
        }
      }
      Map<String, String> discriminators = createStringMap(contextString);
      discriminators.putAll(createStringMap(filters));
    }
    if (serviceResourceType != null) {
      generatedUsageListForServiceResourceType = serviceResourceType.getServiceResourceGenerate();
    }
    map.addAttribute("generatedUsageListForServiceResourceType", generatedUsageListForServiceResourceType);

    map.addAttribute("tenant", tenant);
    map.addAttribute("currency", currency);
    Map<Object, Object> retMap = productService.getCurrentUtilityChargesMap(channel, currency, serviceInstanceUuid,
        null);
    map.addAttribute("retMap", retMap);
    map.addAttribute("startDate", startDate);

    map.addAttribute("resourceTypeName", resourceTypeName);

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
      @RequestParam(value = "isPayAsYouGoChosen", required = false, defaultValue = "false") boolean isPayAsYouGoChosen,
      HttpServletRequest request) throws ConnectorManagementServiceException {
    logger.debug("### createsubscription method starting...(GET)");
    map.addAttribute("isPayAsYouGoChosen", isPayAsYouGoChosen);
    setPage(map, Page.VM_BUNDLES_SUBSCRIPTION);
    String successView = "subscriptions.new";
    Tenant tenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);
    String sourceChannelName = tenant.getSourceChannel().getName();
    return getBundleList(tenant, tenantParam, serviceInstanceUUID, subscriptionId, resourceType, map, request, successView,
        sourceChannelName);
  }

  private String getBundleList(Tenant tenant, String tenantParam, String serviceInstanceUUID, String subscriptionId,
      String resourceType, ModelMap map, HttpServletRequest request, String successView, String sourceChannelName)
      throws ConnectorManagementServiceException {
    User user = getCurrentUser();
    List<String> currentUserServiceCategoryList = userService.getAllAccessibleCloudServiceCategories(user);
    if ((Boolean) request.getAttribute("isSurrogatedTenant")) {
      map.addAttribute("showUserProfile", true); // if service provider
      user = tenant.getOwner();
    }
    map.addAttribute("userHasCloudServiceAccount", userService.isUserHasAnyActiveCloudService(user));
    map.addAttribute("userParam", user.getUuid());
    map.addAttribute("tenant", tenant);
    ServiceInstance serviceInstance = null;
    List<String> serviceCategoryList = userService.getAllAccessibleCloudServiceCategories(user);
    serviceCategoryList.retainAll(currentUserServiceCategoryList);
    map.addAttribute("serviceCategoryList", serviceCategoryList);
    if (subscriptionId != null && !subscriptionId.equals("")) {
      Subscription subscription = subscriptionService.locateSubscriptionById(Long.parseLong(subscriptionId));
      resourceType = subscription.getResourceType().getResourceTypeName();
      serviceInstance = subscription.getServiceInstance();
      serviceInstanceUUID = subscription.getServiceInstance().getUuid();
      map.addAttribute("selectedCloudServiceInstance", serviceInstanceUUID);
      map.addAttribute("selectedCategory", serviceInstance.getService().getCategory());
    } else {
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
        serviceInstance = connectorConfigurationManager.getInstanceByUUID(serviceInstanceUUID);
        map.addAttribute("selectedCloudServiceInstance", serviceInstanceUUID);
        map.addAttribute("selectedCategory", serviceInstance.getService().getCategory());
      }
    }
    if (serviceInstanceUUID != null) {
      try {
        String tenantDataJsonStr = tenantService.getTenantHandle(tenant.getUuid(), serviceInstanceUUID).getData();
        map.addAttribute("tenantDataJsonStr", tenantDataJsonStr);

        Service service = serviceInstance.getService();

        map.addAttribute("service", service);
        map.addAttribute("serviceInstanceUuid", serviceInstanceUUID);
        map.addAttribute("resourceTypes", service.getServiceResourceTypes());
        if (resourceType == null || resourceType.equals("")) {
          // No resource type is given. Enable the first in the list.
          resourceType = service.getServiceResourceTypes().get(0).getResourceTypeName();
        }
        map.addAttribute("resourceType", resourceType);
        map.addAttribute("serviceBundleResourceType", SERVICEBUNDLE);

        List<String> uniqueResourceComponentNames = new ArrayList<String>();
        List<ServiceResourceTypeGroup> groups = new ArrayList<ServiceResourceTypeGroup>();
        if (!resourceType.equals(SERVICEBUNDLE)) {
          for (ServiceResourceType serviceResourceType : service.getServiceResourceTypes()) {
            if (serviceResourceType.getResourceTypeName().equals(resourceType)) {
              for (ServiceResourceTypeGroup serviceResourceTypeGroup : serviceResourceType.getServiceResourceGroups()) {
                groups.add(serviceResourceTypeGroup);
                for (ServiceResourceTypeGroupComponent serviceResourceTypeGroupComponent : serviceResourceTypeGroup
                    .getServiceResourceGroupComponents()) {
                  if (!uniqueResourceComponentNames.contains(serviceResourceTypeGroupComponent
                      .getResourceComponentName())) {
                    uniqueResourceComponentNames.add(serviceResourceTypeGroupComponent.getResourceComponentName());
                  }
                }
              }
            }
          }
        }
        map.addAttribute("uniqueResourceComponentNames", uniqueResourceComponentNames);
        if (subscriptionId != null && !subscriptionId.equals("")) {
          Subscription subscription = subscriptionService.locateSubscriptionById(Long.parseLong(subscriptionId));
          map.addAttribute("subscription", subscription);
          JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(subscription.getConfigurationData());
          map.addAttribute("configurationData", jsonObject);
          Map<String, String> contextMap = new HashMap<String, String>();
          Map<String, String> filterMap = new HashMap<String, String>();
          groups = new ArrayList<ServiceResourceTypeGroup>();
          for (ServiceResourceTypeGroup srtg : subscription.getResourceType().getServiceResourceGroups()) {
            boolean groupSatisfied = true;
            for (ServiceResourceTypeGroupComponent srgc : srtg.getServiceResourceGroupComponents()) {
              if (jsonObject.get(srgc.getResourceComponentName()) == null) {
                groupSatisfied = false;
                break;
              } else {
                contextMap.put(srgc.getResourceComponentName(), jsonObject.get(srgc.getResourceComponentName())
                    .toString());
              }
            }
            if (groupSatisfied) {
              groups.add(srtg);
              String tenantHandleString = tenantService.getTenantHandle(getTenant().getUuid(), serviceInstanceUUID)
                  .getHandle();
              for (ServiceFilter serviceFilter : service.getServiceFilters()) {
                filterMap.put(serviceFilter.getDiscriminatorName(), jsonObject
                    .get(serviceFilter.getDiscriminatorName()).toString());
              }
              List<ServiceResourceTypeGroupComponent> srgcs = srtg.getServiceResourceGroupComponents();
              ListIterator<ServiceResourceTypeGroupComponent> iterator = srgcs.listIterator(srgcs.size());
              Map<String, List<ResourceComponent>> resourceComponentMap = new HashMap<String, List<ResourceComponent>>();
              while (iterator.hasPrevious()) {
                ServiceResourceTypeGroupComponent srgc = iterator.previous();
                contextMap.remove(srgc.getResourceComponentName());
                List<ResourceComponent> resourceComponents = ((CloudConnector) connectorManagementService
                    .getServiceInstance(serviceInstanceUUID)).getMetadataRegistry().getResourceComponentValues(
                    resourceType, srgc.getResourceComponentName(), tenantHandleString, null, contextMap, filterMap);
                resourceComponentMap.put(srgc.getResourceComponentName(), resourceComponents);
              }
              map.addAttribute("resourceComponentMap", resourceComponentMap);
              break;
            }

          }
          citrix.cpbm.access.Subscription subscriptionProxy = (citrix.cpbm.access.Subscription) CustomProxy
              .newInstance(subscription);
          SubscriptionForm subscriptionForm = new SubscriptionForm(subscriptionProxy);
          map.addAttribute("subscriptionForm", subscriptionForm);
        } else {
          citrix.cpbm.access.Subscription subscription = (citrix.cpbm.access.Subscription) CustomProxy
              .newInstance(new Subscription());
          SubscriptionForm subscriptionForm = new SubscriptionForm(subscription);
          map.addAttribute("subscriptionForm", subscriptionForm);
        }
        map.addAttribute("groups", groups);

        HashMap<Object, Object> filterValues = new HashMap<Object, Object>();
        List<ServiceFilter> filters = service.getServiceFilters();
        List<String> serviceFilterNames = new ArrayList<String>();
        for (ServiceFilter filter : filters) {
          Map<String, String> serviceFilterValues = ((CloudConnector) connectorManagementService
              .getServiceInstance(serviceInstance.getUuid())).getMetadataRegistry().getFilterValues(null, null,
              filter.getDiscriminatorName());
          filterValues.put(filter.getDiscriminatorName(), serviceFilterValues);
          serviceFilterNames.add(filter.getDiscriminatorName());
        }
        map.addAttribute("serviceFilterNames", serviceFilterNames);
        map.addAttribute("filter_list", filterValues);
        addValuesForRenderingConnectorValues(map, serviceInstanceUUID, resourceType);
      } catch (CloudServiceException cse) {
        map.addAttribute("cloudServiceException", true);
        map.addAttribute("cloudServiceExceptionStr", cse.getMessage());
      } catch (Exception e) {
        logger.error("Error in creating the Service Instance Map...", e);
        throw new ConnectorManagementServiceException("Error in creating the Service Instance Map...", e);
      }
      map.addAttribute("tnc", getTermsAndConditions());
      map.addAttribute("provision", false);
      map.addAttribute("sourceChannelName", sourceChannelName);
      String result = subscribeBundleGet(request, tenant, tenantParam, successView, map, serviceInstanceUUID,
          resourceType);
      return result;
    } else {
      map.addAttribute("cloudServiceException", true);
      map.addAttribute("cloudServiceExceptionStr", "");
      return successView;
    }
  }

  @RequestMapping(value = {
    "/getResourceComponents"
  }, method = RequestMethod.GET)
  @ResponseBody
  public List<ResourceComponent> getResourceComponents(
      @RequestParam(value = "serviceInstanceUuid", required = true) final String serviceInstanceUuid,
      @RequestParam(value = "resourceType", required = true) final String resourceType,
      @RequestParam(value = "componentType", required = true) final String componentType,
      @RequestParam(value = "contextString", required = true) final String contextString,
      @RequestParam(value = "viewCatalog", required = false, defaultValue = "false") Boolean viewCatalog,
      @RequestParam(value = "filters", required = false) final String filters)
      throws ConnectorManagementServiceException {
    // TODO: Set user handle and tenant handle
    List<ResourceComponent> resourceComponents = new ArrayList<ResourceComponent>();
    if (getCurrentUser() == null
        || (viewCatalog == true && getCurrentUser().getTenant().equals(tenantService.getSystemTenant()))) {
      resourceComponents = privilegeService.runAsPortal(new PrivilegedAction<List<ResourceComponent>>() {
        public List<ResourceComponent> run() {
          return ((CloudConnector) connectorManagementService.getServiceInstance(serviceInstanceUuid))
              .getMetadataRegistry().getResourceComponentValues(resourceType, componentType,
                  tenantService.getTenantHandle(getTenant().getUuid(), serviceInstanceUuid).getHandle(), null,
                  createStringMap(contextString), createStringMap(filters));
        }
      });

    } else {
      resourceComponents = ((CloudConnector) connectorManagementService.getServiceInstance(serviceInstanceUuid))
          .getMetadataRegistry().getResourceComponentValues(resourceType, componentType,
              tenantService.getTenantHandle(getTenant().getUuid(), serviceInstanceUuid).getHandle(), null,
              createStringMap(contextString), createStringMap(filters));
    }
    return resourceComponents;
  }
  
  @RequestMapping(value = {
      "/getResourceComponentsForBundle"
    }, method = RequestMethod.GET)
    @ResponseBody
    public List<ResourceComponent> getResourceComponentsForBundle(
        @RequestParam(value = "serviceInstanceUuid", required = true) String serviceInstanceUuid,
        @RequestParam(value = "resourceType", required = true) String resourceType,
        @RequestParam(value = "componentType", required = true) String componentType,
        @RequestParam(value = "effComponentType", required = false) String effComponentType,
        @RequestParam(value = "contextString", required = false) String contextString,
        @RequestParam(value = "filters", required = false) String filters,
        @RequestParam(value = "bundleId", required = true) Long bundleId, 
        @RequestParam(value = "revisionId", required = true) Long revisionId, HttpServletRequest request)
            throws ConnectorManagementServiceException {
    // TODO: Set user handle and tenant handle

    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);
    User user = actorService.getActor();
    if (!user.getTenant().equals(effectiveTenant)) {
      user = effectiveTenant.getOwner();
    }
    List<ResourceComponent> resourceComponents = new ArrayList<ResourceComponent>();
    List<ResourceComponent> allResourceComponents = ((CloudConnector) connectorManagementService
        .getServiceInstance(serviceInstanceUuid)).getMetadataRegistry().getResourceComponentValues(resourceType,
        componentType, tenantService.getTenantHandle(getTenant().getUuid(), serviceInstanceUuid).getHandle(), null,
        createStringMap(contextString), createStringMap(filters));

    
    //Get specified bundle by revision.
    ProductBundle productBundle = productBundleService.getProductBundleById(bundleId);
    ProductBundleRevision selectedProductBundleRevision = productBundleService.getCurrentProductBundleRevisionForTenant(productBundle, effectiveTenant);
    
    for (ResourceComponent rc : allResourceComponents) {
      
      String name = effComponentType;
      if(StringUtils.isBlank(name)) {
        name = componentType;
      }
      String value = rc.getValue();
      if(rc.getParent() != null && rc.getParent().getValue() != null) {
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

  /**
   * @param serviceInstaceUuid
   * @return
   * @throws ConnectorManagementServiceException
   */
  @RequestMapping(value = {
    "/getFilterValues"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Map<Object, Object> getFilterValues(
      @RequestParam(value = "serviceInstaceUuid", required = true) String serviceInstaceUuid)
      throws ConnectorManagementServiceException {
    try {
      HashMap<Object, Object> filterValues = new HashMap<Object, Object>();
      ServiceInstance serviceInstance = connectorConfigurationManager.getInstanceByUUID(serviceInstaceUuid);
      Service service = serviceInstance.getService();
      List<ServiceFilter> filters = service.getServiceFilters();
      for (ServiceFilter filter : filters) {
        Map<String, String> serviceFilterValues = ((CloudConnector) connectorManagementService
            .getServiceInstance(serviceInstance.getUuid())).getMetadataRegistry().getFilterValues(null, null,
            filter.getDiscriminatorName());
        filterValues.put(filter.getDiscriminatorName(), serviceFilterValues);
      }
      return filterValues;
    } catch (Exception e) {
      logger.error("Error in creating the Service Instance Map...", e);
      throw new ConnectorManagementServiceException("Error in creating the Service Instance Map...", e);
    }
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

  private void addValuesForRenderingConnectorValues(ModelMap map, String serviceInstanceUuid, String inputResourceType) {
    try {
      String serviceUuid = connectorManagementService.getServiceInstance(serviceInstanceUuid)
          .getServiceInstanceConfiguration().getServiceUUID();
      Service service = connectorConfigurationManager.getService(serviceUuid);
      map.addAttribute("prefix", service.getServiceName());
      List<ServiceResourceType> serviceResourceTypeList = service.getServiceResourceTypes();
      ServiceResourceType targetedServiceResourceType = null;
      if (CollectionUtils.isNotEmpty(serviceResourceTypeList)) {
        for (ServiceResourceType serviceResourceType : serviceResourceTypeList) {
          if (inputResourceType.equals(serviceResourceType.getResourceTypeName())) {
            targetedServiceResourceType = serviceResourceType;
            break;
          }
        }
        if (!inputResourceType.equals(SERVICEBUNDLE)) {
          if (targetedServiceResourceType == null) {
            throw new ConnectorManagementServiceException(
                "No Service Resource Type Configured for the provided Resource Type", new RuntimeException(
                    "No Service Resource Type Configured for the provided Resource Type"));
          } else {
            String finalEditorTag = targetedServiceResourceType.getEditor();
            String componentSelector = targetedServiceResourceType.getComponentSelector();
            List<ServiceResourceTypeProperty> serviceResourceTypePropertyList = targetedServiceResourceType
                .getServiceResourceTypeProperty();
            map.addAttribute("customEditorTag", finalEditorTag);
            map.addAttribute("customComponentSelector", componentSelector);
            map.addAttribute("productProperties", serviceResourceTypePropertyList);
          }
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
      ModelMap map,
      @RequestParam(value = "tenant", required = false) final String tenantParam,
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

      public Map<String, Object> run() {
        ModelMap modelMap = new ModelMap();
        try {
          getBundleList(tenant, tenantParam, serviceInstanceUUID, null, resourceType, modelMap, request, finalView,
              catalogChannel.getName());
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
    Service service = connectorConfigurationManager.getInstanceByUUID(serviceInstanceUUID).getService();
    List<String> uniqueResourceComponentNames = new ArrayList<String>();
    List<ServiceResourceTypeGroup> groups = new ArrayList<ServiceResourceTypeGroup>();
    if (!resourceType.equals(SERVICEBUNDLE)) {
      for (ServiceResourceType serviceResourceType : service.getServiceResourceTypes()) {
        if (serviceResourceType.getResourceTypeName().equals(resourceType)) {
          for (ServiceResourceTypeGroup serviceResourceTypeGroup : serviceResourceType.getServiceResourceGroups()) {
            groups.add(serviceResourceTypeGroup);
            for (ServiceResourceTypeGroupComponent serviceResourceTypeGroupComponent : serviceResourceTypeGroup
                .getServiceResourceGroupComponents()) {
              if (!uniqueResourceComponentNames.contains(serviceResourceTypeGroupComponent.getResourceComponentName())) {
                uniqueResourceComponentNames.add(serviceResourceTypeGroupComponent.getResourceComponentName());
              }
            }
          }
        }
      }
    }
    return uniqueResourceComponentNames;
  }

  @RequestMapping(value = ("/browse_catalog"), method = RequestMethod.GET)
  public String anonymousCatalog(ModelMap map,
      @RequestParam(value = "serviceInstanceUUID", required = false) final String serviceInstanceUUID,
      @RequestParam(value = "currencyCode", required = false) String currencyCode,
      @RequestParam(value = "resourceType", required = false) final String resourceType,
      final HttpServletRequest request) {
    logger.debug("### anonymousCatalog method starting...(GET)");
    final Channel channel = channelService.getDefaultServiceProviderChannel();
    final String successView = "anonymous.catalog";
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
    Map<String, Object> finalMap = privilegeService.runAsPortal(new PrivilegedAction<Map<String, Object>>() {

      public Map<String, Object> run() {
        ModelMap modelMap = new ModelMap();
        try {
          getBundleList(tenant, null, serviceInstanceUUID, null, resourceType, modelMap, request, successView,
              channel.getName());
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
    return successView;
  }
}
