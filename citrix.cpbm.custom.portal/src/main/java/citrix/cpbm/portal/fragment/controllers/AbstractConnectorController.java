/* Copyright (C) 2012 Citrix Systems, Inc. All rights reserved. */
package citrix.cpbm.portal.fragment.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.citrix.cpbm.platform.admin.service.ConnectorConfigurationManager;
import com.citrix.cpbm.platform.admin.service.ConnectorManagementService;
import com.citrix.cpbm.platform.admin.service.utils.BootstrapServiceXmlBean;
import com.citrix.cpbm.platform.admin.service.utils.BootstrapServiceXmlBean.Roles;
import com.citrix.cpbm.platform.admin.service.utils.BootstrapServiceXmlBean.Roles.Role;
import com.citrix.cpbm.platform.admin.service.utils.ServiceResponse;
import com.citrix.cpbm.platform.admin.service.utils.ServiceResponse.Status;
import com.citrix.cpbm.platform.spi.BaseConnector;
import com.citrix.cpbm.platform.spi.CloudConnectorFactory;
import com.citrix.cpbm.platform.spi.CloudConnectorFactory.ConnectorType;
import com.citrix.cpbm.platform.spi.View;
import com.citrix.cpbm.platform.util.CssdkConstants;
import com.vmops.model.AccountConfigurationServiceConfigMetadata;
import com.vmops.model.BaseServiceConfigurationMetadata;
import com.vmops.model.Category;
import com.vmops.model.CurrencyValue;
import com.vmops.model.MediationRule;
import com.vmops.model.Product;
import com.vmops.model.ProductCharge;
import com.vmops.model.Profile;
import com.vmops.model.Revision;
import com.vmops.model.SecurityContextScope;
import com.vmops.model.SecurityContextScope.Scope;
import com.vmops.model.Service;
import com.vmops.model.ServiceConfiguration;
import com.vmops.model.ServiceConfigurationMetadata;
import com.vmops.model.ServiceInstance;
import com.vmops.model.ServiceInstanceConfig;
import com.vmops.model.ServiceUsageType;
import com.vmops.model.Tenant;
import com.vmops.model.User;
import com.vmops.portal.config.Configuration.Names;
import com.vmops.service.ConfigurerService;
import com.vmops.service.CurrencyValueService;
import com.vmops.service.ProfileService;
import com.vmops.service.UserService.Handle;
import com.vmops.utils.FileUtils;
import com.vmops.web.controllers.AbstractAuthenticatedController;
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.forms.ServiceInstanceLogoForm;
import com.vmops.web.validators.ServiceInstanceLogoFormValidator;
import com.vmops.web.validators.ValidationUtil;

public abstract class AbstractConnectorController extends AbstractAuthenticatedController {

  @Autowired
  protected ConnectorManagementService connectorManagementService;

  @Autowired
  protected ConnectorConfigurationManager connectorConfigurationManager;

  @Autowired
  protected ProfileService profileService;

  @Autowired
  protected ConfigurerService configurerService;

  @Autowired
  protected CurrencyValueService currencyValueService;

  @RequestMapping(value = "/cs", method = RequestMethod.GET)
  public String showCloudServices(@RequestParam(value = "id", required = false) String serviceUuid,
      @RequestParam(value = "instanceId", required = false) String instanceUuId,
      @RequestParam(value = "action", required = false) String action, ModelMap map) {
    return getView(serviceUuid, instanceUuId, action, map, false);
  }

  @RequestMapping(value = "/csinstances", method = RequestMethod.GET)
  public String showCloudServices(@RequestParam(value = "tenant", required = false) String tenantUuid, ModelMap map,
      HttpServletRequest request) {
    Tenant tenant = tenantService.get(tenantUuid);
    if (tenant.equals(tenantService.getSystemTenant())) {
      return "redirect:/portal/home";
    }
    map.addAttribute("tenant", tenant);
    String uiView = null;
    User user = getCurrentUser();
    if ((Boolean) request.getAttribute("isSurrogatedTenant")) {
      setPage(map, Page.CRM_SERVICES);
      map.addAttribute("showUserProfile", true);
      uiView = "company_setup.connector_cs_admin";
      user = tenant.getOwner();
    } else {
      setPage(map, Page.COMPANY_CONNECTORS_CS);
      map.addAttribute("showUserProfile", false);
      uiView = "company_setup.connector_cs";
    }

    prepareServiceViewForUser(map, tenant.getOwner());
    List<Service> services = connectorConfigurationManager.getAllServicesByType(CssdkConstants.CLOUD);
    map.addAttribute("services", services);
    map.addAttribute("categories", connectorConfigurationManager.getAllCategories(CssdkConstants.CLOUD));

    map.addAttribute("userHasCloudServiceAccount", userService.isUserHasAnyActiveCloudService(user));
    map.addAttribute("effectiveUser", user);
    @SuppressWarnings("unchecked")
    Map<ServiceInstance, Boolean> serviceInstanceMap = (Map<ServiceInstance, Boolean>) map.get("serviceInstanceMap");
    Map<ServiceInstance, Boolean> serviceInstanceViewMap = new HashMap<ServiceInstance, Boolean>();
    Map<String, Integer> serviceInstancesCountPerCategory = new HashMap<String, Integer>();

    for (Map.Entry<ServiceInstance, Boolean> entry : serviceInstanceMap.entrySet()) {
      serviceInstanceViewMap.put(entry.getKey(), false);
      Integer count = serviceInstancesCountPerCategory.get(entry.getKey().getCategory());
      if (count == null) {
        serviceInstancesCountPerCategory.put(entry.getKey().getCategory(), 1);
      } else {
        serviceInstancesCountPerCategory.put(entry.getKey().getCategory(), count + 1);
      }
      try {
        if (entry.getValue()) {
          View view = resolveViewForAccountSettingsFromServiceInstance(tenantUuid, entry.getKey().getUuid());
          if (view != null) {
            serviceInstanceViewMap.put(entry.getKey(), true);
          }
        }
      } catch (Exception e) {
        logger.error("Error in resolving the view.. ", e);
      }
    }
    serviceInstancesCountPerCategory.put("All", serviceInstanceMap.entrySet() != null ? serviceInstanceMap.entrySet()
        .size() : 0);
    map.addAttribute("countPerCategory", serviceInstancesCountPerCategory);
    map.addAttribute("serviceInstanceViewMap", serviceInstanceViewMap);

    return uiView;
  }

  @RequestMapping(value = "/oss", method = RequestMethod.GET)
  public String showOSSServices(@RequestParam(value = "id", required = false) String serviceUuid,
      @RequestParam(value = "instanceId", required = false) String instanceUuId,
      @RequestParam(value = "action", required = false) String action, ModelMap map) {
    return getView(serviceUuid, instanceUuId, action, map, true);
  }

  protected String getView(String serviceUuid, String instanceUuId, String action, ModelMap map, boolean isOss) {

    logger.debug("### Inside view.");
    String view = "";
    map.addAttribute("tenant", getCurrentUser().getTenant());
    setPage(map, isOss ? Page.HOME_CONNECTORS_OSS : Page.HOME_CONNECTORS_CS);
    prepareServiceViewForUser(map, getCurrentUser());

    if (StringUtils.isNotBlank(serviceUuid)) {
      Service service = connectorConfigurationManager.getService(serviceUuid);
      map.addAttribute("service", service);
      if ("view".equals(action)) {
        map.addAttribute("viewServiceDetails", true);
        view = isOss ? "main.home_oss.instance.edit" : "main.home_service.details";
      } else {
        Set<ServiceConfigurationMetadata> properties = service.getServiceConfigurationMetadata();
        List<BaseServiceConfigurationMetadata> propertiesList = new ArrayList<BaseServiceConfigurationMetadata>(
            properties);
        Collections.sort(propertiesList);
        map.addAttribute("service_config_properties", propertiesList);
        setQuickProductProperties(map);
        view = isOss ? "main.home_oss.instance.edit" : "main.home_cs.instance.add";
      }
    } else if (StringUtils.isNotBlank(instanceUuId)) {
      ServiceInstance instance = connectorConfigurationManager.getInstanceByUUID(instanceUuId);
      List<ServiceInstanceConfig> instanceConfiguration = new ArrayList<ServiceInstanceConfig>(
          connectorConfigurationManager.getInstanceConfiguration(instance, instance.getService()));
      Collections.sort(instanceConfiguration, new Comparator<ServiceInstanceConfig>() {

        @Override
        public int compare(ServiceInstanceConfig o1, ServiceInstanceConfig o2) {
          return o1.getServiceConfigMetadata().getPropertyOrder() - o2.getServiceConfigMetadata().getPropertyOrder();
        }
      });

      map.addAttribute("instance", instance);
      map.addAttribute("service", instance.getService());
      map.addAttribute("instance_properties", instanceConfiguration);
      setQuickProductProperties(map);
      List<ServiceUsageType> usageTypeListWithNoProduct = getUsageTypesWithNoProduct(instance);
      map.addAttribute("serviceUsageTypes", usageTypeListWithNoProduct);
      view = isOss ? "main.home_oss.instance.edit" : "main.home_cs.instance.edit";
    } else {
      List<Service> services = connectorConfigurationManager.getAllServicesByType(isOss ? CssdkConstants.OSS
          : CssdkConstants.CLOUD);
      map.addAttribute("services", services);
      map.addAttribute("categories",
          connectorConfigurationManager.getAllCategories(isOss ? CssdkConstants.OSS : CssdkConstants.CLOUD));
      map.addAttribute("countPerCategory", connectorConfigurationManager.getServiceCountPerCategory(services));

      if (isOss) {
        boolean isSystemActive = false; // get from RegistrationController
        map.addAttribute("isSystemActive", isSystemActive);
      }

      BaseConnector connector = null;
      Map<String, Boolean> isAliveMap = new HashMap<String, Boolean>();
      for (Service service : services) {
        for (ServiceInstance si : service.getServiceInstances()) {
          if (si.getService().getType().equals(CloudConnectorFactory.ConnectorType.CLOUD.toString())) {
            connector = connectorManagementService.getServiceInstance(si.getUuid());
          } else {
            connector = connectorManagementService.getOssServiceInstancebycategory(ConnectorType.valueOf(si
                .getService().getCategory()));
          }
          isAliveMap.put(si.getUuid(), connector != null ? connector.getStatus() : false);
        }
      }
      map.addAttribute("isAliveMap", isAliveMap);

      view = isOss ? "main.home_connector_oss" : "main.home_connector_cs_admin";

    }
    map.addAttribute("userHasCloudServiceAccount", userService.isUserHasAnyActiveCloudService(getCurrentUser()));
    logger.debug("### Exiting view.");
    return view;
  }

  private List<ServiceUsageType> getUsageTypesWithNoProduct(ServiceInstance instance) {
    List<Revision> allRPBRevisions = productService.getAllRevisions(null);
    // Size of the list should never be 0, as there always is a revision with
    // null start date, if not any other
    Revision productRevision = allRPBRevisions.get(allRPBRevisions.size() - 1);
    Map<ServiceUsageType, List<Product>> usageTypeProductMap = productService.getProductsByUsageType(productRevision,
        instance);
    List<ServiceUsageType> usageTypeListWithNoProduct = new ArrayList<ServiceUsageType>();
    for (ServiceUsageType serviceUsageType : instance.getService().getServiceUsageTypes()) {
      if (usageTypeProductMap.containsKey(serviceUsageType)) {
        if (usageTypeProductMap.get(serviceUsageType) == null || usageTypeProductMap.get(serviceUsageType).size() == 0) {
          usageTypeListWithNoProduct.add(serviceUsageType);
        }
      } else {
        usageTypeListWithNoProduct.add(serviceUsageType);
      }
    }
    return usageTypeListWithNoProduct;
  }

  private void setQuickProductProperties(ModelMap map) {
    List<CurrencyValue> activeCurrencies = currencyValueService.listActiveCurrencies();
    map.addAttribute("activeCurrencies", activeCurrencies);
    Revision currentRevision = productService.getCurrentRevision(null);
    List<ProductCharge> productCharges = getProductCharges(activeCurrencies, currentRevision);
    map.addAttribute("productCharges", productCharges);
    List<Category> categories = productService.getAllCategories();
    map.addAttribute("categories", categories);
  }

  private List<ProductCharge> getProductCharges(List<CurrencyValue> activeCurrencies, Revision currentRevision) {
    List<ProductCharge> productCharges = new ArrayList<ProductCharge>();
    for (CurrencyValue cv : activeCurrencies) {
      ProductCharge newCharge = new ProductCharge();
      newCharge.setPrice(BigDecimal.ZERO.setScale(
          Integer.parseInt(config.getValue(Names.com_citrix_cpbm_portal_appearance_currency_precision)),
          BigDecimal.ROUND_HALF_UP));
      newCharge.setCurrencyValue(cv);
      newCharge.setRevision(currentRevision);
      productCharges.add(newCharge);
    }
    return productCharges;
  }

  @RequestMapping(value = {
    "/createInstance"
  }, method = RequestMethod.POST)
  public @ResponseBody
  ModelMap saveInstance(@RequestParam("id") final String id, @RequestParam("action") final String action,
      @RequestParam("configProperties") final String configProperties,
      @RequestParam(value = "quickProducts", required = false) final String quickProducts, ModelMap map,
      HttpServletRequest request) {

    ServiceResponse response = new ServiceResponse();
    try {

      JSONArray jsonArray = new JSONArray(configProperties);
      ServiceInstance instance = null;
      boolean add = true;
      Service service = null;
      Map<String, ServiceInstanceConfig> mapOfFieldNameVsInstanceConfig = new HashMap<String, ServiceInstanceConfig>();
      Set<ServiceInstanceConfig> instanceConfigurationList = new HashSet<ServiceInstanceConfig>();
      Map<String, ServiceConfigurationMetadata> mapOfServiceMetaData = new HashMap<String, ServiceConfigurationMetadata>();

      if ("update".equals(action)) {
        add = false;
        instance = connectorConfigurationManager.getInstance(id);
        service = instance.getService();
        Set<ServiceInstanceConfig> instanceConfig = instance.getServiceInstanceConfig();
        for (ServiceInstanceConfig serviceInstanceConfig : instanceConfig) {
          mapOfFieldNameVsInstanceConfig.put(serviceInstanceConfig.getName(), serviceInstanceConfig);
        }
      } else {
        instance = new ServiceInstance();
        service = connectorConfigurationManager.getService(id);
        instance.setServiceInstanceConfig(instanceConfigurationList);
        instance.setService(service);
      }
      Set<ServiceConfigurationMetadata> properties = service.getServiceConfigurationMetadata();
      for (ServiceConfigurationMetadata serviceConfigurationMetadata : properties) {
        mapOfServiceMetaData.put(serviceConfigurationMetadata.getName(), serviceConfigurationMetadata);
      }

      for (int index = 0; index < jsonArray.length(); index++) {
        JSONObject jsonObj = jsonArray.getJSONObject(index);
        String fieldName = jsonObj.get("name").toString();
        String fieldValue = jsonObj.get("value").toString();

        if ("instancename".equals(fieldName)) {
          instance.setName(fieldValue);
          continue;
        } else if ("instancedescription".equals(fieldName)) {
          instance.setDescription(fieldValue);
          continue;
        } else if ("instancecode".equals(fieldName)) {
          instance.setCode(fieldValue);
          continue;
        }

        ServiceConfigurationMetadata configurationMetadata = mapOfServiceMetaData.get(fieldName);
        map.addAttribute("validationResult", CssdkConstants.SUCCESS);
        String validationJson = configurationMetadata.getValidation();

        String validationResult = ValidationUtil.valid(validationJson, fieldName, fieldValue, messageSource);
        if (!CssdkConstants.SUCCESS.equals(validationResult)) {
          map.addAttribute("validationResult", validationResult);
          return map;
        }

        if (add) {
          ServiceInstanceConfig instanceConfiguration = new ServiceInstanceConfig();
          instanceConfiguration.setService(service);
          instanceConfiguration.setServiceConfigMetadata(configurationMetadata);
          instanceConfiguration.setServiceInstance(instance);
          instanceConfiguration.setName(fieldName);
          instanceConfiguration.setValue(fieldValue);
          instanceConfiguration.setServiceInstanceConfigurer(service);
          instanceConfigurationList.add(instanceConfiguration);
        } else {
          mapOfFieldNameVsInstanceConfig.get(fieldName).setValue(fieldValue);
        }
      }

      if (service.getType().equals(CssdkConstants.OSS)) {
        instance.setName(service.getCategory());
        instance.setDescription("Instance for " + service.getCategory() + " Category");
        instance.setCode(instance.getName() + "." + service.getUuid().substring(0, 8));
      }

      List<HashMap<String, Object>> quickProductList = new ArrayList<HashMap<String, Object>>();
      if (service.getType().equals(CssdkConstants.CLOUD) && StringUtils.isNotEmpty(quickProducts)) {
        JSONArray productJsonArray = new JSONArray(quickProducts);
        List<Revision> allRPBRevisions = productService.getAllRevisions(null);
        // Size of the list should never be 0, as there always is a revision with
        // null start date, if not any other
        Revision productRevision = allRPBRevisions.get(allRPBRevisions.size() - 1);

        for (int index = 0; index < productJsonArray.length(); index++) {
          JSONObject jsonObj = productJsonArray.getJSONObject(index);
          HashMap<String, Object> productMap = new HashMap<String, Object>();

          String productName = jsonObj.get("name").toString();
          String code = jsonObj.get("code").toString();
          String scale = jsonObj.get("scale").toString();
          String uom = jsonObj.get("uom").toString();
          String usageTypeId = jsonObj.get("usageTypeId").toString();

          JSONArray priceList = new JSONArray(jsonObj.getString("price"));

          ServiceUsageType serviceUsageType = connectorConfigurationManager.getServiceUsageTypeById(Long
              .parseLong(usageTypeId));
          Category category = productService.getCategory(jsonObj.getLong("category"));

          Product product = new Product();
          product.setName(productName);
          product.setCode(code);
          product.setUom(uom);
          product.setCategory(category);

          productMap.put("product", product);

          MediationRule mediationRule = createMediationRule(product, serviceUsageType, instance, null,
              BigDecimal.valueOf(Double.parseDouble(scale)), "", productRevision);

          productMap.put("mediationRule", mediationRule);

          List<ProductCharge> productCharges = new ArrayList<ProductCharge>();
          for (int priceIndex = 0; priceIndex < priceList.length(); priceIndex++) {
            JSONObject price = priceList.getJSONObject(priceIndex);
            ProductCharge productCharge = new ProductCharge();
            productCharge.setPrice((new BigDecimal(price.getString("currencyVal"))).setScale(4,
                BigDecimal.ROUND_HALF_UP));
            CurrencyValue cv = currencyValueService.locateBYCurrencyCode(price.getString("currencyCode"));
            productCharge.setCurrencyValue(cv);
            productCharge.setRevision(productRevision);
            productCharges.add(productCharge);
          }

          productMap.put("productCharges", productCharges);
          quickProductList.add(productMap);
        }
      }

      if (add) {
        connectorManagementService.addInstance(instance, response, quickProductList);
      } else {
        connectorManagementService.updateInstance(instance, response, quickProductList);
      }
      if (response.getStatus().equals(Status.SUCCESS)) {
        map.addAttribute("instanceid", instance.getUuid());
        map.addAttribute("result", CssdkConstants.SUCCESS);
        map.addAttribute("message",
            add ? this.messageSource.getMessage("connector.instance.add.success", null, getSessionLocale(request))
                : this.messageSource.getMessage("connector.instance.update.success", null, getSessionLocale(request)));
      } else {
        map.addAttribute("result", CssdkConstants.FAILURE);
        map.addAttribute("message", response.getMessage());
      }
    } catch (Exception e) {
      logger.error("Exception..", e);
      if (response.getStatus().equals(Status.FAILURE)) {
        map.addAttribute("message", response.getMessage());
      } else {
        map.addAttribute("message",
            this.messageSource.getMessage("connector.instance.add.failure", null, null) + e.getMessage());
      }
      map.addAttribute("result", CssdkConstants.FAILURE);
    }

    return map;
  }

  @RequestMapping(value = {
    "/viewInstances"
  }, method = RequestMethod.GET)
  public @ResponseBody
  ModelMap viewInstance(@RequestParam("id") final String id, ModelMap map) {
    try {
      List<ServiceInstance> instances = connectorConfigurationManager.getAllInstances(id);
      BaseConnector connector = null;
      Map<String, Boolean> isAliveMap = new HashMap<String, Boolean>();
      for (ServiceInstance si : instances) {
        if (si.getService().getType().equals(CloudConnectorFactory.ConnectorType.CLOUD.toString())) {
          connector = connectorManagementService.getServiceInstance(si.getUuid());
        } else {
          connector = connectorManagementService.getOssServiceInstancebycategory(ConnectorType.valueOf(si.getService()
              .getCategory()));
        }
        isAliveMap.put(si.getUuid(), connector != null ? connector.getStatus() : false);
      }
      map.addAttribute("instances", instances);
      map.addAttribute("isAliveMap", isAliveMap);
    } catch (Exception e) {
      logger.error("Exception..", e);
    }
    return map;
  }

  @RequestMapping(value = {
    "/enableService"
  }, method = RequestMethod.GET)
  public String enableService(@RequestParam("id") final String id, ModelMap map) {
    Service service = connectorConfigurationManager.getService(id);
    map.addAttribute("uuid", id);
    map.addAttribute("tnc", getTermsAndConditions(service));

    if (service.getType().equals(ConnectorType.CLOUD.toString())) {
      // TODO this may work now but this needs to be rewritten so that
      // actual program logic, including partitioning of roles into
      // scopes, resides here
      Collection<Profile> globalProfiles = profileService.listAllProfilesOfClass(Scope.GLOBAL);
      Collection<Profile> tenantProfiles = profileService.listAllProfilesOfClass(Scope.TENANT);

      Map<String, Boolean> subScopeMap = new HashMap<String, Boolean>();
      Scope scopes[] = SecurityContextScope.Scope.values();
      for (Scope scope : scopes) {
        Collection<Scope> subScopes = SecurityContextScope.getSubScopes(scope);
        for (Scope scope2 : subScopes) {
          subScopeMap.put(scope.name() + "." + scope2.getName(), true);
        }
      }
      map.addAttribute("subScopeMap", subScopeMap);
      map.addAttribute("opsProfiles", globalProfiles);
      map.addAttribute("nonOpsProfiles", tenantProfiles);
      addAndBiforcateCSRoles(map, service);
      map.addAttribute("cloudService", true);
      map.addAttribute("serviceName", service.getServiceName());
    } else {
      map.addAttribute("cloudService", false);
    }
    return "main.home_connector_enable";
  }

  @RequestMapping(value = {
    "/enableService"
  }, method = RequestMethod.POST)
  public @ResponseBody
  String enableService(@RequestParam("id") final String id,
      @RequestParam("profiledetails") final String profiledetails, ModelMap map) {

    try {
      Service service = connectorConfigurationManager.getService(id);

      if (StringUtils.isNotEmpty(profiledetails)) {
        Map<String, List<String>> profileRoleMapping = new LinkedHashMap<String, List<String>>();
        JSONArray jsonArray = new JSONArray(profiledetails);

        for (int index = 0; index < jsonArray.length(); index++) {
          List<String> roleList = new ArrayList<String>();
          JSONObject jsonObj = jsonArray.getJSONObject(index);
          String profileid = jsonObj.get("profileid").toString();
          String roles = jsonObj.get("roles").toString();
          JSONArray rolesArray = new JSONArray(roles);
          for (int i = 0; i < rolesArray.length(); i++) {
            String role = rolesArray.getString(i);
            roleList.add(role);
          }
          profileRoleMapping.put(profileid, roleList);
        }

        Roles cloudServiceRoles = getRoles(service);
        if (cloudServiceRoles != null) {
          connectorConfigurationManager.persistCloudServiceRoles(cloudServiceRoles, profileRoleMapping);
        }
        // Refreshing object as it gets stale in persistRoles
        service = connectorConfigurationManager.getService(id);
      }

      service.setTermsAndConditionsAccepted(true);
      connectorManagementService.saveServiceType(service);
    } catch (Exception e) {
      logger.error("Exception..", e);
      return "failure";
    }
    return "success";
  }

  @RequestMapping(value = {
    "/status"
  }, method = RequestMethod.GET)
  public @ResponseBody
  boolean isAlive(@RequestParam("id") final String instanceUUID, ModelMap map) {
    BaseConnector connector = null;
    ServiceInstance si = connectorConfigurationManager.getInstance(instanceUUID);
    if (si.getService().getType().equals(CloudConnectorFactory.ConnectorType.CLOUD.toString())) {
      connector = connectorManagementService.getServiceInstance(instanceUUID);
    } else {
      connector = connectorManagementService.getOssServiceInstancebycategory(ConnectorType.valueOf(si.getService()
          .getCategory()));
    }
    return connector != null ? connector.getStatus() : false;
  }

  @RequestMapping(value = {
    "/enable"
  }, method = RequestMethod.POST)
  public @ResponseBody
  ModelMap enable(@RequestParam("id") final String id, @RequestParam("enable") final boolean enable, ModelMap map,
      HttpServletRequest request) {

    Service service = connectorConfigurationManager.getService(id);
    List<Service> services = connectorConfigurationManager.getAllEnabledServices(service.getCategory());
    map.addAttribute("result", "failure");
    if (enable && CollectionUtils.isNotEmpty(services)) {
      map.addAttribute("message",
          this.messageSource.getMessage("oss.instance.already.enabled", null, getSessionLocale(request)));
      return map;
    }
    boolean isSystemActive = false; // get from RegistrationController
    if (isSystemActive) {
      map.addAttribute("message",
          this.messageSource.getMessage("oss.enable.disable.freezed", null, getSessionLocale(request)));
      return map;
    }

    if (CollectionUtils.isEmpty(service.getServiceInstances())) {
      map.addAttribute("message",
          this.messageSource.getMessage("oss.service.not.configured", null, getSessionLocale(request)));
      return map;
    }

    // First manipulate the Map
    connectorManagementService.enableDisableOSS(service.getCategory(), enable);

    // then, update the DB.
    service.setEnabled(enable);
    connectorManagementService.saveServiceType(service);

    map.addAttribute("enabled", enable);
    map.addAttribute("result", "success");
    if (enable) {
      map.addAttribute("message",
          this.messageSource.getMessage("oss.service.enable.success", null, getSessionLocale(request)));
    } else {
      map.addAttribute("message",
          this.messageSource.getMessage("oss.service.disable.success", null, getSessionLocale(request)));
    }
    return map;
  }

  @RequestMapping(value = "/getServiceInstanceList", method = RequestMethod.GET)
  @ResponseBody
  public List<Map<String, String>> getServiceInstanceList(@ModelAttribute("currentTenant") Tenant currentTenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "viewCatalog", required = false, defaultValue = "false") Boolean viewCatalog,
      @RequestParam(value = "category", required = false) String category, HttpServletRequest request) {
    logger.debug("###Entering in getServiceInstanceList GET");
    List<Map<String, String>> serviceInstanceValues = new ArrayList<Map<String, String>>();
    User user = getCurrentUser();
    if (user == null || (viewCatalog && user.getTenant().equals(tenantService.getSystemTenant()))) {
      user = userService.getSystemUser(Handle.PORTAL);
    }
    List<ServiceInstance> serviceProviderCloudTypeServiceInstances = new ArrayList<ServiceInstance>();
    boolean isSurrogatedTenant = (Boolean) request.getAttribute("isSurrogatedTenant");
    if (isSurrogatedTenant) {
      serviceProviderCloudTypeServiceInstances = userService.getCloudServiceInstance(user, category);
      user = tenantService.get(tenantParam).getOwner();
    }
    List<ServiceInstance> cloudTypeServiceInstances = userService.getCloudServiceInstance(user, category);
    if (isSurrogatedTenant) {
      cloudTypeServiceInstances.retainAll(serviceProviderCloudTypeServiceInstances);
    }
    for (ServiceInstance currentInstance : cloudTypeServiceInstances) {
      Map<String, String> currentInstanceValues = new HashMap<String, String>();
      currentInstanceValues.put("uuid", currentInstance.getUuid());
      currentInstanceValues.put("name", currentInstance.getName());
      serviceInstanceValues.add(currentInstanceValues);
    }
    return serviceInstanceValues;
  }

  private List<Role> getRolesForService(Service service) {
    Roles role = this.getRoles(service);
    return (role != null) ? role.getRole() : null;
  }

  private void addAndBiforcateCSRoles(ModelMap map, Service service) {
    List<Role> roles = getRolesForService(service);
    List<Role> globalRoles = new ArrayList<Role>();
    List<Role> tenantRoles = new ArrayList<Role>();
    if (!CollectionUtils.isEmpty(roles)) {
      for (Role role : roles) {
        if (SecurityContextScope.isInScope(Scope.valueOf(role.getScope()), Scope.GLOBAL_ADMIN)) {
          globalRoles.add(role);
        } else { // need else if?
          tenantRoles.add(role);
        }
      }
    }

    map.addAttribute("globalRoles", globalRoles);
    map.addAttribute("tenantRoles", tenantRoles);
  }

  private Roles getRoles(Service service) {
    String filepath = this.getServicesDataDirectory(service) + CssdkConstants.SERVICE_DEFINITION_FILE_NAME;
    BootstrapServiceXmlBean bootstrapXmlBean = getBootstrapServiceXmlBeanObject(new File(filepath));
    return (bootstrapXmlBean != null) ? bootstrapXmlBean.getRoles() : null;
  }

  private BootstrapServiceXmlBean getBootstrapServiceXmlBeanObject(File serviceDefXml) {
    try {
      JAXBContext jc = connectorManagementService.getJc();
      Unmarshaller unmarshaller = jc.createUnmarshaller();
      BootstrapServiceXmlBean serviceXmlBean = (BootstrapServiceXmlBean) unmarshaller.unmarshal(serviceDefXml);
      return serviceXmlBean;
    } catch (JAXBException e) {
      logger.error("Unexpected error..", e);
    }
    return null;
  }

  private String getTermsAndConditions(Service service) {
    String file = this.getServicesDataDirectory(service) + CssdkConstants.TERMS_AND_CONDITIONS_FILENAME;
    return FileUtils.getFileContent(file);
  }

  private String getServicesDataDirectory(Service service) {
    return config.getValue(Names.com_citrix_cpbm_portal_settings_services_datapath) + File.separator
        + service.getServiceName() + "_" + service.getVendorVersion() + File.separator;
  }

  @RequestMapping(value = "/fetchAccountConfigurationsParams", method = RequestMethod.GET)
  public String fetchAccountConfigurationsParams(
      @RequestParam(value = "serviceInstanceUUID", required = false) String serviceInstanceUUID,
      @RequestParam(value = "tenant", required = false) String tenantParam, ModelMap map) {
    Set<AccountConfigurationServiceConfigMetadata> accountConfigurationServiceConfigMetadataList = null;
    String jspProvidedByService = null;
    map.addAttribute("serviceInstanceUUID", serviceInstanceUUID);
    StringBuilder propString = new StringBuilder();
    ServiceInstance serviceinstance = connectorConfigurationManager.getInstance(serviceInstanceUUID);
    if (serviceinstance != null) {
      Service service = serviceinstance.getService();
      accountConfigurationServiceConfigMetadataList = service.getAccountConfigServiceConfigMetadata();
      for (AccountConfigurationServiceConfigMetadata accountConfigurationServiceConfigMetadata : accountConfigurationServiceConfigMetadataList) {
        propString.append(accountConfigurationServiceConfigMetadata.getName()).append(",");
      }
      map.addAttribute("service", service);
      List<ServiceConfiguration> serviceConfigurationList = configurerService.findByConfigurer(service);
      if (CollectionUtils.isNotEmpty(serviceConfigurationList)) {
        for (ServiceConfiguration serviceConfiguration : serviceConfigurationList) {
          if (CssdkConstants.EDITOR_SERVICE_PROPERTY_CONSTANT.equals(serviceConfiguration.getName())) {
            jspProvidedByService = CssdkConstants.SUCCESS;
          }
        }
      }
      if (StringUtils.isNotBlank(jspProvidedByService)) {
        map.addAttribute("jspProvidedByService", jspProvidedByService);
      }
      map.addAttribute("service_account_config_properties", accountConfigurationServiceConfigMetadataList);
      map.addAttribute("service_account_config_properties_list", propString.toString());

    }
    return "service.account.config";
  }

  @RequestMapping(value = {
    "/loadPackagedJsp"
  }, method = RequestMethod.GET)
  @ResponseBody
  public void loadPackagedJspInConnector(
      @RequestParam(value = "serviceInstanceUUID", required = false) String serviceInstanceUUID,
      HttpServletResponse response) {
    FileInputStream fileinputstream = null;
    String jspPath = null;
    String jspProvidedByService = null;
    ServiceInstance serviceinstance = connectorConfigurationManager.getInstance(serviceInstanceUUID);
    if (serviceinstance != null) {
      Service service = serviceinstance.getService();
      List<ServiceConfiguration> serviceConfigurationList = configurerService.findByConfigurer(service);
      if (CollectionUtils.isNotEmpty(serviceConfigurationList)) {
        for (ServiceConfiguration serviceConfiguration : serviceConfigurationList) {
          if (CssdkConstants.EDITOR_SERVICE_PROPERTY_CONSTANT.equals(serviceConfiguration.getName())) {
            jspProvidedByService = serviceConfiguration.getValue();
          }
        }
        String cssdkFilesDirectory = FilenameUtils.concat(
            config.getValue(Names.com_citrix_cpbm_portal_settings_services_datapath), service.getServiceName() + "_"
                + service.getVendorVersion());
        jspPath = cssdkFilesDirectory + "/" + CssdkConstants.JSP_DIRECTORY + "/" + jspProvidedByService;
      }
    }
    try {
      if (jspPath != null && !jspPath.trim().equals("")) {
        fileinputstream = new FileInputStream(jspPath);
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
    } catch (FileNotFoundException e) {
      logger.error("FileNot Found...", e);
    } catch (IOException e) {
      logger.error("IOException Found...", e);
    }
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);

  }

  @RequestMapping(value = ("/uploadlogo"), method = RequestMethod.GET)
  public String uploadServiceInstanceLogo(@RequestParam(value = "Id", required = true) String Id, ModelMap map) {
    logger.debug("### upload service instance logo method starting...(GET)");

    ServiceInstanceLogoForm instanceLogoForm = new ServiceInstanceLogoForm(
        connectorConfigurationManager.getInstance(Id));
    map.addAttribute("serviceInstanceLogoForm", instanceLogoForm);
    // setPage(map, Page.PRODUCTS);

    logger.debug("### upload service instance logo method end...(GET)");
    return "service.instance.editlogo";
  }

  @RequestMapping(value = ("/uploadlogo"), method = RequestMethod.POST)
  @ResponseBody
  public String uploadServiceInstanceLogo(@ModelAttribute("serviceInstanceLogoForm") ServiceInstanceLogoForm form,
      BindingResult result, HttpServletRequest request, ModelMap map) {
    logger.debug("### upload service instance logo method starting...(POST)");

    String rootImageDir = config.getValue(Names.com_citrix_cpbm_portal_settings_images_uploadPath);
    if (rootImageDir != null && !rootImageDir.trim().equals("")) {
      ServiceInstance serviceInstance = form.getServiceInstance();
      ServiceInstanceLogoFormValidator validator = new ServiceInstanceLogoFormValidator();
      validator.validate(form, result);
      if (result.hasErrors()) {
        return messageSource.getMessage(result.getFieldError("logo").getCode(), null, request.getLocale());
      } else {
        String serviceInstanceDir = "serviceInstance";
        File file = new File(FilenameUtils.concat(rootImageDir, serviceInstanceDir));
        if (!file.exists()) {
          file.mkdir();
        }
        String serviceInstanceAbsoluteDir = FilenameUtils.concat(rootImageDir, serviceInstanceDir);
        String relativeImageDir = FilenameUtils.concat(serviceInstanceDir, serviceInstance.getId().toString());
        File file1 = new File(FilenameUtils.concat(serviceInstanceAbsoluteDir, serviceInstance.getId().toString()));
        if (!file1.exists()) {
          file1.mkdir();
        }

        MultipartFile logoFile = form.getLogo();
        try {
          if (!logoFile.getOriginalFilename().trim().equals("")) {
            String logoFileRelativePath = writeMultiPartFileToLocalFile(rootImageDir, relativeImageDir, logoFile);
            serviceInstance.setImagePath(logoFileRelativePath);
          }
          connectorConfigurationManager.updateServiceInstance(serviceInstance);
        } catch (IOException e) {
          logger.debug("###IO Exception in writing custom image file");
        }
      }
      return "success";
    } else {
      result.rejectValue("logo", "error.custom.image.upload.dir");
      return messageSource.getMessage(result.getFieldError("logo").getCode(), null, request.getLocale());
    }
  }
}
