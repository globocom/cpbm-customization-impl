/* Copyright (C) 2011 Citrix Systems, Inc. All rights reserved. */
package citrix.cpbm.portal.fragment.controllers;

import java.io.File;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.citrix.cpbm.platform.admin.service.ConnectorConfigurationManager;
import com.citrix.cpbm.platform.admin.service.ConnectorManagementService;
import com.citrix.cpbm.platform.admin.service.exceptions.ConnectorManagementServiceException;
import com.citrix.cpbm.platform.spi.CloudConnector;
import com.citrix.cpbm.platform.spi.CloudConnectorFactory.ConnectorType;
import com.citrix.cpbm.platform.spi.ResourceComponent;
import com.vmops.internal.service.PrivilegeService;
import com.vmops.internal.service.SubscriptionService;
import com.vmops.model.Catalog;
import com.vmops.model.Channel;
import com.vmops.model.ChargeRecurrenceFrequency;
import com.vmops.model.CurrencyValue;
import com.vmops.model.Entitlement;
import com.vmops.model.Product;
import com.vmops.model.ProductBundle;
import com.vmops.model.ProductBundleRevision;
import com.vmops.model.ProductRevision;
import com.vmops.model.ProvisioningConstraint;
import com.vmops.model.ProvisioningConstraint.AssociationType;
import com.vmops.model.RateCard;
import com.vmops.model.RateCardCharge;
import com.vmops.model.RateCardComponent;
import com.vmops.model.Revision;
import com.vmops.model.ServiceInstance;
import com.vmops.model.ServiceResourceType;
import com.vmops.model.ServiceResourceType.ResourceConstraint;
import com.vmops.model.ServiceResourceTypeGroup;
import com.vmops.model.ServiceResourceTypeGroupComponent;
import com.vmops.model.Subscription;
import com.vmops.model.Subscription.State;
import com.vmops.model.Tenant;
import com.vmops.model.User;
import com.vmops.portal.config.Configuration.Names;
import com.vmops.service.ChannelService;
import com.vmops.service.CurrencyValueService;
import com.vmops.service.ProductBundleService;
import com.vmops.service.ProductService;
import com.vmops.service.TenantService;
import com.vmops.service.exceptions.AjaxFormValidationException;
import com.vmops.service.exceptions.InvalidAjaxRequestException;
import com.vmops.service.exceptions.NoSuchProductBundleException;
import com.vmops.utils.DateUtils;
import com.vmops.utils.JSONUtils;
import com.vmops.web.controllers.AbstractAuthenticatedController;
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.forms.EntitlementForm;
import com.vmops.web.forms.ProductBundleForm;
import com.vmops.web.forms.ProductBundleLogoForm;
import com.vmops.web.forms.RateCardChargesForm;
import com.vmops.web.forms.RateCardComponentChargesForm;
import com.vmops.web.forms.RateCardForm;
import com.vmops.web.interceptors.UserContextInterceptor;
import com.vmops.web.validators.ProductBundleLogoFormValidator;
import com.vmops.web.validators.ProductBundleValidator;

public abstract class AbstractProductBundlesController extends AbstractAuthenticatedController {

  @Autowired
  private ProductBundleService productBundleService;

  @Autowired
  private ProductService productService;

  @Autowired
  protected SubscriptionService subscriptionService;

  @Autowired
  private CurrencyValueService currencyValueService;

  @Autowired
  protected ChannelService channelService;

  @Autowired
  private TenantService tenantService;

  @Autowired
  private ConnectorConfigurationManager connectorConfigurationManager;

  @Autowired
  private ConnectorManagementService connectorManagementService;

  @Autowired
  protected PrivilegeService privilegeService;

  private static final List<ResourceConstraint> ORDERED_CONSTRAINTS = new ArrayList<ServiceResourceType.ResourceConstraint>() {

    private static final long serialVersionUID = -6231001441117555544L;

    {
      add(ResourceConstraint.NONE);
      add(ResourceConstraint.PER_USER);
      add(ResourceConstraint.ACCOUNT);
      add(ResourceConstraint.SINGLETON);
    }
  };

  Logger logger = Logger.getLogger(AbstractProductBundlesController.class);

  class ProductBundleSortOrderSort implements Comparator<ProductBundle> {

    @Override
    public int compare(ProductBundle bundle1, ProductBundle bundle2) {
      if ((Long) bundle1.getSortOrder() > (Long) bundle2.getSortOrder()) {
        return 1;
      } else if ((Long) bundle1.getSortOrder() == (Long) bundle2.getSortOrder()) {
        return Integer.valueOf(bundle1.getId().toString()) - Integer.valueOf(bundle2.getId().toString());
      } else {
        return -1;
      }
    }
  }

  @RequestMapping(value = {
    "list.json"
  }, method = RequestMethod.GET)
  @ResponseBody
  public List<ProductBundleRevision> listProductBundles(@ModelAttribute("currentTenant") Tenant currentTenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "serviceInstaceUuid", required = false) String serviceInstaceUuid,
      @RequestParam(value = "resourceType", required = false) String resourceType,
      @RequestParam(value = "filters", required = false) String filters,
      @RequestParam(value = "context", required = false) String context,
      @RequestParam(value = "viewCatalog", required = false, defaultValue = "false") Boolean viewCatalog,
      @RequestParam(value = "revision", required = false) String revision,
      @RequestParam(value = "channelParam", required = false) String channelId,
      @RequestParam(value = "currencyCode", required = false) String currencyCode,
      @RequestParam(value = "revisionDate", required = false, defaultValue = "") String historyDate,
      @RequestParam(value = "dateFormat", required = false) String dateFormat, HttpServletRequest request) {
    logger.debug("# listProductBundles method entered...(list.json)");

    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);
    User user = actorService.getActor();
    boolean anonymousBrowsing = false;
    if (getCurrentUser() == null) {
      anonymousBrowsing = true;
    }
    if (!anonymousBrowsing && !user.getTenant().equals(effectiveTenant)) {
      user = effectiveTenant.getOwner();
    }

    ServiceInstance serviceInstance = connectorConfigurationManager.getInstanceByUUID(serviceInstaceUuid);
    com.vmops.model.Service service = serviceInstance.getService();
    List<ServiceResourceType> resourceTypes = service.getServiceResourceTypes();
    ServiceResourceType serviceResourceType = null;
    for (ServiceResourceType resType : resourceTypes) {
      if (resType.getResourceTypeName().equals(resourceType)) {
        serviceResourceType = resType;
        break;
      }
    }

    List<ProductBundleRevision> filteredBundleRevisions = new ArrayList<ProductBundleRevision>();
    Date historyDateObj = null;
    Channel channel = null;
    CurrencyValue currency = currencyValueService.locateBYCurrencyCode(currencyCode);
    if (viewCatalog == true && user.getTenant().equals(tenantService.getSystemTenant())) {
      Revision channelRevision = null;
      Boolean includeFutureBundle = false;

      channel = channelService.getChannelById(channelId);
      if (StringUtils.isBlank(revision) || revision.equalsIgnoreCase("current")) {
        channelRevision = channelService.getCurrentRevision(channel);
      } else if (revision.equalsIgnoreCase("planned")) {
        includeFutureBundle = true;
        channelRevision = channelService.getFutureRevision(channel);
      } else if (revision.equalsIgnoreCase("history")) {

        if (historyDate != null && !historyDate.isEmpty()) {
          dateFormat = "MM/dd/yyyy HH:mm:ss";
          DateFormat formatter = new SimpleDateFormat(dateFormat);
          try {
            historyDateObj = (Date) formatter.parse(historyDate);
          } catch (ParseException e) {
            throw new InvalidAjaxRequestException(e.getMessage());
          }
        } else {
          List<Date> historyDatesForCatalog = productService.getHistoryDates(channel.getCatalog());
          historyDateObj = historyDatesForCatalog.get(0);
        }
        channelRevision = channelService.getRevisionForTheDateGiven(historyDateObj, channel);
      }
      filteredBundleRevisions = productBundleService.getProductBundleRevisions(serviceInstance, serviceResourceType,
          createStringMap(context), createStringMap(filters), user, getSessionLocale(request), channel, currency,
          channelRevision, includeFutureBundle);
    } else if (anonymousBrowsing) {
      channel = channelService.getDefaultServiceProviderChannel();
      Revision channelRevision = channelService.getCurrentRevision(channel);
      filteredBundleRevisions = productBundleService.getProductBundleRevisions(serviceInstance, serviceResourceType,
          createStringMap(context), createStringMap(filters), user, getSessionLocale(request), channel, currency,
          channelRevision, false);
    } else {
      filteredBundleRevisions = productBundleService.getProductBundleRevisions(serviceInstance, serviceResourceType,
          createStringMap(context), createStringMap(filters), user, getSessionLocale(request));
    }

    logger.debug("# listProductBundles method leaving...(list.json)");
    return filteredBundleRevisions;
  }

  private List<ProductBundle> getProductBundleList(List<ProductBundle> productBundles, int pageNo, int perPage) {
    logger.debug("Entering getProductBundlesList...");

    int count = 0;
    int toStartFrom = (pageNo - 1) * perPage;
    List<ProductBundle> productBundlesList = new ArrayList<ProductBundle>();
    for (ProductBundle productBundle : productBundles) {
      if (count >= toStartFrom) {
        productBundlesList.add(productBundle);
        if (productBundlesList.size() == perPage) {
          break;
        }
      }
      count += 1;
    }

    logger.debug("Leaving getProductBundlesList....");
    return productBundlesList;
  }

  private List<ProductBundle> getProductBundleListByFilters(List<ProductBundleRevision> productBundleRevisions,
      String filterBy) {
    logger.debug("Entering getProductBundlesListByFilter...");
    List<ProductBundle> productBundlesList = new ArrayList<ProductBundle>();
    for (ProductBundleRevision productBundleRevision : productBundleRevisions) {
      if ((StringUtils.isEmpty(filterBy)) || ("All".equalsIgnoreCase(filterBy))
          || ("publish".equalsIgnoreCase(filterBy) && productBundleRevision.getProductBundle().getPublish())
          || ("unpublish".equalsIgnoreCase(filterBy) && !productBundleRevision.getProductBundle().getPublish())) {
        productBundlesList.add(productBundleRevision.getProductBundle());
      }
    }

    logger.debug("Leaving getProductBundlesListByFilter....");
    return productBundlesList;
  }

  @RequestMapping(value = {
    "listValidSubscriptions.json"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Map<Long, ProductBundleRevision> listValidSubscriptions(@ModelAttribute("currentTenant") Tenant currentTenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "serviceInstaceUuid", required = false) String serviceInstaceUuid,
      @RequestParam(value = "resourceType", required = false) String resourceType,
      @RequestParam(value = "filters", required = false) String filters,
      @RequestParam(value = "context", required = false) String context, HttpServletRequest request) {
    logger.debug("# listValidSubscriptions method entered...(listValidSubscriptions.json)");
    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);
    User effectiveUser = getCurrentUser();
    if ((Boolean) request.getAttribute("isSurrogatedTenant")) {
      effectiveUser = effectiveTenant.getOwner();
    }
    ServiceInstance serviceInstance = connectorConfigurationManager.getInstanceByUUID(serviceInstaceUuid);
    List<Subscription> subscriptions = subscriptionService.findAllSubscriptionsByState(null, effectiveUser,
        serviceInstance, null, 0, 0, State.ACTIVE);
    Map<Long, ProductBundleRevision> subMap = new HashMap<Long, ProductBundleRevision>();
    for (Subscription subscription : subscriptions) {
      if (subscription.getProductBundle() != null && subscription.getProductBundle().getResourceType() != null
          && subscription.getActiveHandle() == null
          && subscription.getResourceType().getResourceTypeName().equalsIgnoreCase(resourceType)) {
        ProductBundleRevision productBundleRevision = productBundleService.getCurrentProductBundleRevisionForTenant(
            subscription.getProductBundle(), effectiveTenant);
        if (productBundleService.isValidBundleForGivenDiscriminators(
            productBundleRevision.getProvisioningConstraints(), createStringMap(context))) {
          subMap.put(subscription.getId(), productBundleRevision);
        }
      }
    }
    logger.debug("# listValidSubscriptions method leaving...(listValidSubscriptions.json)");
    return subMap;
  }

  @RequestMapping(value = {
    "getBundleBySubscription.json"
  }, method = RequestMethod.GET)
  @ResponseBody
  public Map<String, Object> getBundleRevisionBySubscription(@ModelAttribute("currentTenant") Tenant currentTenant,
      @RequestParam(value = "subscriptionId", required = true) String subscriptionId,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "serviceInstaceUuid", required = true) String serviceInstanceUuid,
      @RequestParam(value = "filters", required = false) String filters,
      @RequestParam(value = "context", required = false) String context, HttpServletRequest request) {
    logger.debug("# getBundleRevisionBySubscription method entered...(getBundleBySubscription.json)");
    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);
    boolean isCompatible = false;
    ProductBundleRevision productBundleRevision = null;
    Map<String, Object> subMap = new HashMap<String, Object>();
    Subscription subscription = subscriptionService.locateSubscriptionById(new Long(subscriptionId));
    if (subscription.getProductBundle() != null && subscription.getProductBundle().getResourceType() != null) {
      productBundleRevision = productBundleService.getCurrentProductBundleRevisionForTenant(
          subscription.getProductBundle(), effectiveTenant);
      if (productBundleService.isValidBundleForGivenDiscriminators(productBundleRevision.getProvisioningConstraints(),
          createStringMap(context))) {
        isCompatible = true;
      }
    } else if (subscription.getProductBundle() == null && subscription.getResourceType() != null) {
      // Add Utility Bundle Which is Usage Based Bundle this is bundle which is shown only for UI purpose there is no
      // bundle as such
      isCompatible = true;
      productBundleRevision = productBundleService.getUtilityBundle(subscription.getServiceInstance(),
          getSessionLocale(request));
    }
    logger.debug("# getBundleRevisionBySubscription method leaving...(getBundleBySubscription.json)");
    subMap.put("isCompatible", isCompatible);
    subMap.put("bundleRevision", productBundleRevision);
    subMap.put("subscriptionId", subscriptionId);
    return subMap;
  }

  @RequestMapping(value = {
      "showbundles", "/", ""
  }, method = RequestMethod.GET)
  public String showBundles(ModelMap map,
      @RequestParam(value = "whichPlan", required = false, defaultValue = "planned") String whichPlan,
      @RequestParam(value = "revisionDate", required = false) String revisionDate) {
    logger.debug("### showBundles method starting...");

    setPage(map, Page.PRODUCTS_BUNDLES);
    map.addAttribute("whichPlan", whichPlan);
    if (whichPlan.equals("history")) {
      map.addAttribute("historyDates", productService.getHistoryDates(null));
    }
    map.addAttribute("tenant", getCurrentUser().getTenant());

    logger.debug("### showBundles method ending...");
    return "bundles.show";
  }

  /**
   * List all the productBundles
   * 
   * @return
   */
  @RequestMapping(value = {
    "list"
  }, method = RequestMethod.GET)
  public String listProductBundles(@RequestParam(value = "serviceUUID", required = false) String serviceUUID,
      @RequestParam(value = "serviceInstanceUUID", required = false) String serviceInstanceUUID,
      @RequestParam(value = "currentPage", required = false, defaultValue = "1") String currentPage,
      @RequestParam(value = "size", required = false) String size,
      @RequestParam(value = "namePattern", required = false) String namePattern,
      @RequestParam(value = "whichPlan", required = false, defaultValue = "planned") String whichPlan,
      @RequestParam(value = "revisionDate", required = false) String revisionDate,
      @RequestParam(value = "filterBy", required = false) String filterBy, ModelMap map) {
    logger.debug("### listProductBundles method starting...");

    setPage(map, Page.PRODUCTS_BUNDLES);
    int currentPageValue = currentPage != null ? Integer.parseInt(currentPage) : 1;
    int perPageValue = getDefaultPageSize();
    int sizeInt = 0;
    if (size == null || size.equals("")) {
      sizeInt = productBundleService.getBundlesCount();
    } else {
      sizeInt = Integer.parseInt(size);
    }

    try {
      List<String> serviceCategoryList = connectorConfigurationManager.getAllCategories(ConnectorType.CLOUD.toString());
      map.addAttribute("serviceCategoryList", serviceCategoryList);
      if (serviceUUID == null || serviceUUID.trim().equals("")) {
        serviceUUID = serviceCategoryList.get(0);
      }
    } catch (Exception e) {
      logger.error("Error in getting the Service Categories list...", e);
      return "";
    }
    map.addAttribute("serviceUUID", serviceUUID);

    try {
      List<ServiceInstance> serviceInstances = connectorManagementService.getServiceInstanceByCategory(serviceUUID);
      map.addAttribute("serviceInstances", serviceInstances);
      if (serviceInstanceUUID == null || serviceInstanceUUID.trim().equals("")) {
        serviceInstanceUUID = serviceInstances.get(0).getUuid();
      }
    } catch (Exception e) {
      logger.error("Caught Exception while getting Service instance list", e);
    }
    map.addAttribute("serviceInstanceUUID", serviceInstanceUUID);

    List<ProductBundleRevision> allProductBundleRevisions = new ArrayList<ProductBundleRevision>();
    if (whichPlan.equals("planned")) {
      allProductBundleRevisions = channelService.getChannelRevision(null,
          channelService.getFutureRevision(null).getStartDate(), false).getProductBundleRevisions();
    } else if (whichPlan.equals("current")) {
      allProductBundleRevisions = channelService.getChannelRevision(null,
          channelService.getCurrentRevision(null).getStartDate(), false).getProductBundleRevisions();
    } else if (whichPlan.equals("history")) {
      if (revisionDate == null || revisionDate.trim().equals("")) {
        List<Revision> revisions = productService.getReferencePriceBookHistoryRevisions();
        revisionDate = DateUtils.getStringForCalendar(revisions.get(0).getStartDate(),
            DateUtils.getDateFormatter("MM/dd/yyyy HH:mm:ss"));
      }

      Date historyDate = DateUtils.getCalendarForDate(revisionDate, DateUtils.getDateFormatter("MM/dd/yyyy HH:mm:ss"))
          .getTime();
      allProductBundleRevisions = channelService.getChannelRevision(null, historyDate, false)
          .getProductBundleRevisions();
    }

    // Use of contains(namePattern) will list entities with the pattern string
    // anywhere, but resorting to startsWith to reflect the current behaviour
    List<ProductBundleRevision> productBundleRevisions = new ArrayList<ProductBundleRevision>();
    for (ProductBundleRevision productBundleRevision : allProductBundleRevisions) {
      if (productBundleRevision.getProductBundle().getServiceInstanceId().getUuid().equals(serviceInstanceUUID)) {
        if (namePattern != null && !namePattern.trim().equals("")
            && !productBundleRevision.getProductBundle().getName().toLowerCase().startsWith(namePattern.toLowerCase())) {
          continue;
        } else {
          productBundleRevisions.add(productBundleRevision);
        }
      }
    }
    List<ProductBundle> productBundleListByFilter = getProductBundleListByFilters(productBundleRevisions, filterBy);
    List<ProductBundle> productBundleList = getProductBundleList(productBundleListByFilter, currentPageValue,
        perPageValue);
    int totalCount = productBundleListByFilter.size();

    Collections.sort(productBundleList, new ProductBundleSortOrderSort());

    map.addAttribute("namePattern", namePattern);
    map.addAttribute("productBundlesList", productBundleList);
    map.addAttribute("bundlesLen", productBundleList.size());
    setPaginationValues(map, perPageValue, currentPageValue, totalCount, null);

    map.addAttribute("tenant", getCurrentUser().getTenant());
    map.addAttribute("whichPlan", whichPlan);

    logger.debug("### listProductBundles method ending..." + sizeInt);
    return "bundles.list";
  }

  /**
   * List all the productBundles based on the Search Pattern by product bundle name
   * 
   * @return
   */
  @RequestMapping(value = {
    "searchlist"
  }, method = RequestMethod.GET)
  public String searchByName(@RequestParam(value = "serviceUUID", required = false) String serviceUUID,
      @RequestParam(value = "serviceInstanceUUID", required = false) String serviceInstanceUUID,
      @RequestParam(value = "currentPage", required = false, defaultValue = "1") String currentPage,
      @RequestParam(value = "size", required = false) String size,
      @RequestParam(value = "namePattern", required = false) String namePattern,
      @RequestParam(value = "whichPlan", required = false, defaultValue = "planned") String whichPlan,
      @RequestParam(value = "historyDate", required = false) String historyDate,
      @RequestParam(value = "filterBy", required = false) String filterBy, ModelMap map) {
    logger.debug("### searchByName method starting...");

    listProductBundles(serviceUUID, serviceInstanceUUID, currentPage, size, namePattern, whichPlan, historyDate,
        filterBy, map);

    logger.debug("### searchByName method ending...");
    return "bundles.search.list";
  }

  /**
   * List all the productBundles
   * 
   * @return
   */
  @RequestMapping(value = ("/{catalogId}/listbundles"), method = RequestMethod.GET)
  public String listProductBundlesByCatalog(@PathVariable String catalogId, ModelMap map) {
    logger.debug("### listProductBundles method starting...");

    setPage(map, Page.PRODUCTS_CATALOGS);
    Catalog catalog = productBundleService.getCatalog(Long.valueOf(catalogId));
    map.addAttribute("catalog", catalog);

    List<ProductBundleRevision> productBundlesList = productBundleService.listProductBundlesByCatalog(catalog, null, 0,
        0, null);

    map.addAttribute("productBundlesList", productBundlesList);
    map.addAttribute("size", productBundlesList.size());

    logger.debug("### listProductBundles method ending...");
    return "bundles.list";
  }

  @RequestMapping(value = ("/getComponents"), method = RequestMethod.GET)
  @ResponseBody
  public Collection<String> getServiceResourceComponents(
      @RequestParam(value = "resourceType", required = true) long resourceTypeId) {
    logger.debug("### getServiceResourceComponents method starting...");

    // TODO: So current implementation of ServiceResourceTypeGroupComponent
    // differentiates this object by group, which should not happen.
    // So somebody needs to look into it, somebody but me.
    Set<String> components = new HashSet<String>();
    ServiceResourceType resourceTypeObj = connectorConfigurationManager.getServiceResourceTypeById(resourceTypeId);
    List<ServiceResourceTypeGroup> groups = resourceTypeObj.getServiceResourceGroups();

    for (ServiceResourceTypeGroup group : groups) {
      for (ServiceResourceTypeGroupComponent groupComponent : group.getServiceResourceGroupComponents())
        components.add(groupComponent.getResourceComponentName());
    }

    logger.debug("### getServiceResourceComponents method starting...");
    return components;
  }

  @RequestMapping(value = ("/getComponentValue"), method = RequestMethod.GET)
  @ResponseBody
  public Collection<ResourceComponent> getServiceResourceComponentValues(
      @RequestParam(value = "serviceInstance", required = true) final String serviceInstanceUUID,
      @RequestParam(value = "resourceType", required = true) long resourceTypeId,
      @RequestParam(value = "component", required = true) final String component)
      throws ConnectorManagementServiceException {
    logger.debug("### getServiceResourceComponentValues method starting...");

    final ServiceResourceType resourceTypeObj = connectorConfigurationManager
        .getServiceResourceTypeById(resourceTypeId);

    return privilegeService.runAsPortal(new PrivilegedAction<Collection<ResourceComponent>>() {

      public Collection<ResourceComponent> run() {
        return ((CloudConnector) connectorManagementService.getServiceInstance(serviceInstanceUUID))
            .getMetadataRegistry().getResourceComponentValues(resourceTypeObj.getResourceTypeName(), component);
      }
    });
  }

  @RequestMapping(value = ("/getServiceResources"), method = RequestMethod.GET)
  @ResponseBody
  public Collection<ServiceResourceType> getServiceResources(
      @RequestParam(value = "serviceInstance", required = true) String serviceInstanceUUID) {
    logger.debug("### getServiceResources method starting...");
    return connectorConfigurationManager.getInstance(serviceInstanceUUID).getService().getServiceResourceTypes();
  }

  @RequestMapping(value = ("/getAssociationTypes"), method = RequestMethod.GET)
  @ResponseBody
  public Collection<AssociationType> getAssociationTypes() {
    return Arrays.asList(AssociationType.values());
  }

  @RequestMapping(value = ("/getApplicableBusinessConstraints"), method = RequestMethod.GET)
  @ResponseBody
  public Collection<ResourceConstraint> getApplicableBusinessConstraints(
      @RequestParam(value = "resourceType", required = true) String serviceResourceTypeStr) {
    Long serviceResourceType = null;

    try {
      serviceResourceType = Long.parseLong(serviceResourceTypeStr);
    } catch (NumberFormatException e) {
      return ORDERED_CONSTRAINTS;
    }

    ServiceResourceType resType = connectorConfigurationManager.getServiceResourceTypeById(serviceResourceType);
    ResourceConstraint constraint = resType.getConstraint();

    return ORDERED_CONSTRAINTS.subList(ORDERED_CONSTRAINTS.indexOf(constraint), ORDERED_CONSTRAINTS.size());
  }

  /**
   * Create new productBundle
   * 
   * @param map
   * @return
   */
  @RequestMapping(value = ("/create"), method = RequestMethod.GET)
  public String createProductBundle(
      @RequestParam(value = "serviceInstanceUUID", required = true) String serviceInstanceUUID, ModelMap map) {
    logger.debug("### createProductBundle method starting...(GET)");

    ProductBundle productBundle = new ProductBundle();
    ProductBundleForm productBundleForm = new ProductBundleForm(productBundle);
    productBundleForm.setChargeRecurrenceFrequencyList(productBundleService.getChargeRecurrenceFrequencyList());

    productBundleForm.createChargeEntriesForTheProductBundle(currencyValueService.listActiveCurrencies(),
        channelService.getFutureRevision(null));

    map.addAttribute("productBundleForm", productBundleForm);
    List<ServiceInstance> instances = null;

    instances = connectorManagementService.getCloudTypeServiceInstances();
    Revision currentChargeRevision = productService.getCurrentRevision(null);

    map.addAttribute("date", currentChargeRevision.getStartDate());
    map.addAttribute("instances", instances);
    map.addAttribute("constraints", ResourceConstraint.values());
    map.addAttribute("activeCurrencies", currencyValueService.listActiveCurrencies());

    Map<String, Set<String>> serviceResourceTypesAndComponentsMap = new HashMap<String, Set<String>>();
    ServiceInstance serviceInstance = connectorConfigurationManager.getInstance(serviceInstanceUUID);
    for (ServiceResourceType serviceResourceType : serviceInstance.getService().getServiceResourceTypes()) {
      Set<String> resourceComponents = new HashSet<String>();
      for (ServiceResourceTypeGroup group : serviceResourceType.getServiceResourceGroups()) {
        for (ServiceResourceTypeGroupComponent groupComponent : group.getServiceResourceGroupComponents())
          resourceComponents.add(groupComponent.getResourceComponentName());
      }
      serviceResourceTypesAndComponentsMap.put(serviceResourceType.getResourceTypeName(), resourceComponents);
    }

    map.addAttribute("serviceName", serviceInstance.getService().getServiceName());
    map.addAttribute("serviceResourceTypesAndComponentsMap", serviceResourceTypesAndComponentsMap);

    logger.debug("### createProductBundle method end");
    return "bundles.new";
  }

  @RequestMapping(value = ("/create"), method = RequestMethod.POST)
  @ResponseBody
  public ProductBundle createProductBundle(@ModelAttribute("productBundleForm") ProductBundleForm productBundleForm,
      BindingResult result, ModelMap map, HttpServletResponse response) throws JSONException {
    logger.debug("### createProductBundle method starting...(POST)");
    ProductBundle productBundle = productBundleForm.getProductBundle();

    // for components which includes nothing, no constraint row created
    // Add name validation check
    if (productBundle.getName() == null) {
      result.rejectValue("productBundle.name", "js.errors.bundle.bundle.form.provide.name");
      throw new AjaxFormValidationException(result);
    }

    if (!productService.isCodeUnique(productBundle.getCode())) {
      logger.debug("### bundle code is NOT unique ");
      response.setStatus(CODE_NOT_UNIQUE_ERROR_CODE);
      return null;
    }

    ChargeRecurrenceFrequency chargeFrequency = productBundleService
        .getChargeRecurrencyFrequencyByName(productBundleForm.getChargeType());
    RateCard rc = new RateCard();
    rc.setChargeType(chargeFrequency);
    rc.setStartDate(new Date());
    rc.setCreatedBy(getCurrentUser());
    rc.setProductBundle(productBundle);
    rc.setUpdatedBy(getCurrentUser());
    rc.setDescription("Initial version");

    RateCardComponent oneTimeRCC = new RateCardComponent();
    oneTimeRCC.setCreatedBy(getCurrentUser());
    oneTimeRCC.setUpdatedBy(getCurrentUser());
    oneTimeRCC.setDescription("Inital version");
    oneTimeRCC.setRateCard(rc);
    oneTimeRCC.setIsRecurring(false);
    rc.getRateCardComponents().add(oneTimeRCC);

    if (!chargeFrequency.equals("NONE")) {
      RateCardComponent recurringRCC = new RateCardComponent();
      recurringRCC.setCreatedBy(getCurrentUser());
      recurringRCC.setUpdatedBy(getCurrentUser());
      recurringRCC.setDescription("Inital version");
      recurringRCC.setRateCard(rc);
      recurringRCC.setIsRecurring(true);
      rc.getRateCardComponents().add(recurringRCC);
    }
    productBundle.setRateCard(rc);

    try {
      productBundle.setResourceType(connectorConfigurationManager.getServiceResourceTypeById(Long
          .parseLong(productBundleForm.getResourceType())));
    } catch (NumberFormatException e) {
      if (productBundleForm.getResourceType().equals("sb")) {
        productBundle.setResourceType(null);
      } else {
        logger.error("resource type could not be set for bundle");
        // TODO and raise here i suppose
      }
    }

    ServiceInstance instance = null;

    instance = connectorConfigurationManager.getInstance(productBundleForm.getServiceInstanceUUID());

    productBundle.setServiceInstanceId(instance);
    ProductBundle prodBundle = productBundleService.createBundle(productBundle);

    Revision futureRevision = channelService.getFutureRevision(null);

    JSONArray compAssociations = new JSONArray(productBundleForm.getCompAssociationJson());
    for (int index = 0; index < compAssociations.length(); index++) {
      JSONObject compAssociationObj = compAssociations.getJSONObject(index);

      AssociationType associationType = AssociationType.valueOf(compAssociationObj.getString("association")
          .toUpperCase());
      String componentName = compAssociationObj.getString("compName");
      JSONArray componentVals = new JSONArray(compAssociationObj.getString("compValues"));
      for (int i = 0; i < componentVals.length(); i++) {
        JSONObject nameValDict = componentVals.getJSONObject(i);
        String compVal = nameValDict.getString("compName");
        String compValueName = nameValDict.getString("compValueName");

        ProvisioningConstraint provisioningConstraint = new ProvisioningConstraint(componentName, associationType,
            compVal, prodBundle);
        provisioningConstraint.setComponentValueDisplayName(compValueName);
        provisioningConstraint.setRevision(futureRevision);
        productBundleService.save(provisioningConstraint);
      }
    }

    if (prodBundle.getRateCard().getChargeType().getName().toUpperCase().equals("NONE")) {
      productBundleService.saveProductBundleChargesOnCreate(prodBundle, productBundleForm.getBundleOneTimeCharges(),
          new ArrayList<RateCardCharge>());
    } else {
      productBundleService.saveProductBundleChargesOnCreate(prodBundle, productBundleForm.getBundleOneTimeCharges(),
          productBundleForm.getBundleRecurringCharges());
    }

    logger.debug("### createProductBundle method end");
    return productBundle;
  }

  @RequestMapping(value = ("/view"), method = RequestMethod.GET)
  public String viewProductBundle(@RequestParam(value = "Id", required = true) String ID,
      @RequestParam(value = "whichPlan", required = false, defaultValue = "planned") String whichPlan, ModelMap map) {
    logger.debug("### viewProductBundle method starting...(POST)");

    ProductBundle productBundle = productBundleService.locateProductBundleById(ID);
    map.addAttribute("productBundle", productBundle);
    map.addAttribute("whichPlan", whichPlan);

    logger.debug("### viewProductBundle method end...(POST)");
    return "bundles.view";
  }

  @RequestMapping(value = ("/edit"), method = RequestMethod.GET)
  public String editProductBundle(@RequestParam(value = "Id", required = true) String ID, ModelMap map) {
    logger.debug("### editProductBundle method starting...(GET)");

    ProductBundle productBundle = productBundleService.locateProductBundleById(ID);
    ProductBundleForm productBundleForm = new ProductBundleForm(productBundle);
    productBundleForm.setChargeRecurrenceFrequencyList(productBundleService.getChargeRecurrenceFrequencyList());
    map.addAttribute("productBundleForm", productBundleForm);

    Revision futureRevision = channelService.getFutureRevision(null);
    ProductBundleRevision productBundleRevision = productBundleService.getProductBundleRevision(
        productBundleForm.getProductBundle(), futureRevision.getStartDate(), null);

    String jsonProvisionalConstraints = "";
    try {
      jsonProvisionalConstraints = JSONUtils.toJSONString(productBundleRevision.getProvisioningConstraints());
    } catch (Exception e) {
      logger.error("Error in creating json from ProvisionalConstraints ...", e);
    }
    map.addAttribute("jsonProvisionalConstraints", jsonProvisionalConstraints);

    Map<String, Set<String>> serviceResourceTypesAndComponentsMap = new HashMap<String, Set<String>>();
    List<ServiceResourceType> serviceResourceTypes = productBundle.getServiceInstanceId().getService()
        .getServiceResourceTypes();
    for (ServiceResourceType serviceResourceType : serviceResourceTypes) {
      Set<String> resourceComponents = new HashSet<String>();
      for (ServiceResourceTypeGroup group : serviceResourceType.getServiceResourceGroups()) {
        for (ServiceResourceTypeGroupComponent groupComponent : group.getServiceResourceGroupComponents())
          resourceComponents.add(groupComponent.getResourceComponentName());
      }
      serviceResourceTypesAndComponentsMap.put(serviceResourceType.getResourceTypeName(), resourceComponents);
    }

    map.addAttribute("serviceName", productBundle.getServiceInstanceId().getService().getServiceName());
    map.addAttribute("serviceResourceTypesAndComponentsMap", serviceResourceTypesAndComponentsMap);

    logger.debug("### editProductBundle method end...(GET)");
    return "bundles.edit";
  }

  @RequestMapping(value = ("/edit"), method = RequestMethod.POST)
  @ResponseBody
  public ProductBundle editProductBundle(@ModelAttribute("productBundleForm") ProductBundleForm productBundleForm,
      BindingResult result, ModelMap map) throws JSONException {
    logger.debug("### editProductBundle method starting...(POST)");

    ProductBundleValidator validator = new ProductBundleValidator();
    validator.validate(productBundleForm, result);
    if (result.hasErrors()) {
      throw new AjaxFormValidationException(result);
    }

    ProductBundle productBundle = productBundleForm.getProductBundle();
    productBundleService.updateProductBundle(productBundle, false);

    Revision futureRevision = channelService.getFutureRevision(null);
    List<ProvisioningConstraint> provisioningConstraints = productBundleService.getProductBundleRevision(
        productBundleForm.getProductBundle(), futureRevision.getStartDate(), null).getProvisioningConstraints();

    JSONArray compAssociations = new JSONArray(productBundleForm.getCompAssociationJson());
    // Following are the cases we might end up with
    // -------- PC IS CHECKED -----------------------
    // 1-> An existing PC is retained with no change in association. Retain it.
    // 2-> A new PC is checked. Here create a new PC referring to the future revision
    // 3-> An existing PC is retained, but association is changed.
    // Check the revision being referred
    // ____3-a) If it is not the future revision, then clone the existing PC and
    // ________set its revision to future revision and also removedBy,
    // ________plus clone this PC again, but with new association and revision as future one
    // ____3-b) If it is future revision, then just change the association
    //
    // -------- PC IS UNCHECKED ---------------------
    // Check the revision of the PC being unchecked ( Change of association doesn't matter here since
    // we are removing the entries)
    // 4-> If it is referring to future revision, then just set removedBy
    // 5-> Clone the existing PC and set removedBy and revision to future one.

    List<String> dbIdsRetained = new ArrayList<String>();
    for (int index = 0; index < compAssociations.length(); index++) {
      JSONObject compAssociationObj = compAssociations.getJSONObject(index);
      String dbId = compAssociationObj.getString("compDbId");
      AssociationType associationType = AssociationType.valueOf(compAssociationObj.getString("association")
          .toUpperCase());
      String componentName = compAssociationObj.getString("compName");
      String componentVal = compAssociationObj.getString("compValue");
      String componentValName = compAssociationObj.getString("compValueName");

      // ---------------- CASE OF CHECKED COMPONENETS ----------------
      // An existing PC is being dealt with
      if (!dbId.equals("-1")) {
        ProvisioningConstraint provisioningConstraint = productBundleService.findProvisioningConstraint(dbId);
        if (!provisioningConstraint.getAssociation().equals(associationType)) {

          // ----------------Case 3-a----------------
          if (!provisioningConstraint.getRevision().equals(futureRevision)) {
            // Create a new PC and set the new association and all
            ProvisioningConstraint newProvisioningConstraint = new ProvisioningConstraint(componentName,
                associationType, componentVal, provisioningConstraint.getProductBundle());
            newProvisioningConstraint.setComponentValueDisplayName(componentValName);
            newProvisioningConstraint.setRevision(futureRevision);
            productBundleService.save(newProvisioningConstraint);

            // Clone the existing PC, and set its removedBy and revision as the future one
            ProvisioningConstraint cloneOfOldProvisioningConstraint = new ProvisioningConstraint(
                provisioningConstraint.getComponentName(), provisioningConstraint.getAssociation(),
                provisioningConstraint.getValue(), provisioningConstraint.getProductBundle());
            cloneOfOldProvisioningConstraint.setComponentValueDisplayName(provisioningConstraint
                .getComponentValueDisplayName());
            cloneOfOldProvisioningConstraint.setRevision(futureRevision);
            cloneOfOldProvisioningConstraint.setRemovedBy(actorService.getActor());
            productBundleService.save(cloneOfOldProvisioningConstraint);
          } else {
            // ------------------Case 3-b----------------
            // Just change the association of the PC
            provisioningConstraint.setAssociation(associationType);
            productBundleService.save(provisioningConstraint);
          }

        }
        // ----------------Case 1----------------
        dbIdsRetained.add(dbId);
        continue;
      }

      // ----------------Case 2----------------
      ProvisioningConstraint provisioningConstraint = new ProvisioningConstraint(componentName, associationType,
          componentVal, productBundle);
      provisioningConstraint.setRevision(futureRevision);
      provisioningConstraint.setComponentValueDisplayName(componentValName);
      productBundleService.save(provisioningConstraint);
    }

    for (ProvisioningConstraint provisioningConstraint : provisioningConstraints) {
      if (dbIdsRetained.contains(provisioningConstraint.getId().toString())) {
        continue;
      }
      // ---------------- CASE OF UNCHECKED COMPONENETS ----------------
      // Case 4
      if (provisioningConstraint.getRevision().equals(futureRevision)) {
        provisioningConstraint.setRemovedBy(actorService.getActor());
        productBundleService.save(provisioningConstraint);
      } else {
        // Case 5
        ProvisioningConstraint newProvisioningConstraint = new ProvisioningConstraint(
            provisioningConstraint.getComponentName(), provisioningConstraint.getAssociation(),
            provisioningConstraint.getValue(), productBundle);
        newProvisioningConstraint.setComponentValueDisplayName(provisioningConstraint.getComponentValueDisplayName());
        newProvisioningConstraint.setRevision(futureRevision);
        newProvisioningConstraint.setRemovedBy(actorService.getActor());
        productBundleService.save(newProvisioningConstraint);
      }
    }

    logger.debug("### editProductBundle method ending...(POST)");
    return productBundle;
  }

  @RequestMapping(value = ("/publish"), method = RequestMethod.POST)
  @ResponseBody
  public String publishBundle(@RequestParam(value = "bundleCode", required = true) String bundleCode,
      @RequestParam(value = "publish", required = true) String publish, ModelMap map) {
    logger.debug("### publishBundle method starting...(POST)");

    ProductBundle bundle = productBundleService.locateProductBundleByCode(bundleCode);
    Boolean publishflag = new Boolean(publish);
    bundle.setPublish(publishflag);
    productBundleService.updateProductBundle(bundle, true);

    logger.debug("### publishBundle method ending...(POST)");
    return "success";
  }

  @RequestMapping(value = {
    "editproductbundleorder"
  }, method = RequestMethod.POST)
  @ResponseBody
  public String editproductbundleorder(
      @RequestParam(value = "productBundleOrderData", required = true) String productBundleOrderData, ModelMap map) {
    logger.debug("### editproductbundleorder method starting...(POST)");

    String[] productRowIds;
    productRowIds = productBundleOrderData.split(",");
    String[] productIds = productBundleOrderData.split(",");
    for (int i = 0; i < productRowIds.length; i++)
      productIds[i] = productRowIds[i].split("row")[1];
    productBundleService.updateProductBundleSortOrder(productIds);

    logger.debug("### editproductbundleorder method ending...(POST)");
    return "success";
  }

  private void mapBundleEntitlements(ProductBundle bundle, ModelMap map, Date filterDate, Integer currentPage,
      Integer perPage) {
    List<Entitlement> entitlementSet = productBundleService.getProductBundleRevision(bundle, filterDate, null)
        .getEntitlements();

    List<Entitlement> entitlements = new ArrayList<Entitlement>();
    if ((currentPage != null && currentPage > 0 && perPage > 0)) {
      if (entitlementSet.size() > (currentPage - 1) * perPage) {
        int startIndex = (currentPage - 1) * perPage;
        int endIndex;
        if (entitlementSet.size() >= (currentPage) * perPage) {
          endIndex = (currentPage * perPage);
        } else {
          endIndex = entitlementSet.size();
        }
        entitlements = new ArrayList<Entitlement>(entitlementSet).subList(startIndex, endIndex);
      }
    } else {
      entitlements = new ArrayList<Entitlement>(entitlementSet);
    }

    map.addAttribute("entitlements", entitlements);

    EntitlementForm entitlementForm = new EntitlementForm();
    Entitlement entitlement = new Entitlement();
    entitlement.setProductBundle(bundle);
    entitlement.setCreatedBy(getCurrentUser());
    entitlement.setUpdatedBy(getCurrentUser());
    entitlementForm.setEntitlement(entitlement);

    map.addAttribute("entitlementForm", entitlementForm);
    map.addAttribute("entitlementFilterDate", filterDate);
    map.addAttribute("products", filterProductsForNewEntitlement(entitlementSet, filterDate));
    map.addAttribute("currentPage", currentPage);
    map.addAttribute("perPage", perPage);
    map.addAttribute("currentPageRecords", entitlements != null ? entitlements.size() : 0); // Just for safer side to
                                                                                            // avoid NPE
    map.addAttribute("pages", getNumberofPages(entitlementSet.size(), perPage));
  }

  private List<Product> filterProductsForNewEntitlement(List<Entitlement> entitlements, Date filterDate) {

    List<Product> products = new ArrayList<Product>();
    for (ProductRevision productRevision : channelService.getChannelRevision(null, filterDate, false)
        .getProductRevisions()) {
      // Remove the retired products
      if (productRevision.getProduct().getRemoved() == null) {
        products.add(productRevision.getProduct());
      }
    }

    if (entitlements != null) {
      for (Entitlement entitlement : entitlements) {
        products.remove(entitlement.getProduct());
      }
    }
    return products;
  }

  @RequestMapping(value = "/{bundleCode}/entitlement/create", method = RequestMethod.GET)
  public String createEntitlement(@PathVariable String bundleCode, HttpServletResponse response, ModelMap map) {
    logger.debug("# getEditFutureRateCard method starting...(GET)");

    ProductBundle bundle = productBundleService.findProductBundleByCode(bundleCode);
    map.put("productBundle", bundle);
    Revision futureRevision = channelService.getFutureRevision(null);
    mapBundleEntitlements(bundle, map, futureRevision.getStartDate(), null, null);

    logger.debug("# getEditFutureRateCard method ending...(GET)");
    return "bundle.entitlement.add";
  }

  @RequestMapping(value = "/{bundleCode}/entitlement/create", method = RequestMethod.POST)
  @ResponseBody
  public String createEntitlement(@PathVariable String bundleCode,
      @ModelAttribute("entitlementForm") EntitlementForm entitlementForm, BindingResult result,
      HttpServletResponse response, ModelMap map) {
    logger.debug("### createEntitlement method starting...(POST) " + entitlementForm.getProductId());

    Product product = productService.locateProductById(entitlementForm.getProductId());
    Entitlement entitlement = entitlementForm.getEntitlement();
    entitlement.setProduct(product);

    if (entitlementForm.isUnlimitedUsage()) {
      entitlement.setIncludedUnits(-1);
    }
    Revision futureRevision = channelService.getFutureRevision(null);

    List<Entitlement> entitlements = productBundleService.getProductBundleRevision(entitlement.getProductBundle(),
        futureRevision.getStartDate(), null).getEntitlements();
    for (Entitlement entitlement2 : entitlements) {
      if (entitlement2.getProduct().equals(entitlement.getProduct())) {
        try {
          response.sendError(HttpStatus.SC_MULTIPLE_CHOICES, "error due to already added");
        } catch (IOException e) {
          logger.error(e, e);
        }
        return "failure";
      }
    }
    entitlement = productBundleService.createEntitlement(entitlement);

    map.addAttribute("entitlement", entitlement);
    populateEntitlementForm(bundleCode, map, futureRevision.getStartDate());

    logger.debug("### createEntitlement method ending...(POST)");
    return "success";
  }

  private void populateEntitlementForm(String bundleCode, ModelMap map, Date revisionDate) {
    EntitlementForm entitlementForm = new EntitlementForm();
    ProductBundle bundle = productBundleService.findProductBundleByCode(bundleCode);

    Entitlement entitlement = new Entitlement();
    entitlement.setProductBundle(bundle);
    entitlement.setCreatedBy(getCurrentUser());
    entitlement.setUpdatedBy(getCurrentUser());
    entitlementForm.setEntitlement(entitlement);

    map.addAttribute("entitlementForm", entitlementForm);
    map.addAttribute("products", filterProductsForNewEntitlement(null, revisionDate));
    map.addAttribute("currency",
        Currency.getInstance(config.getValue(Names.com_citrix_cpbm_portal_settings_default_currency)));
  }

  @RequestMapping(value = ("/entitlement/{entitlementId}/save"), method = RequestMethod.POST)
  @ResponseBody
  public Entitlement saveEntitlement(@PathVariable String entitlementId,
      @RequestParam(value = "includedUnits", required = true) int includetedUnits, ModelMap map) {
    logger.debug("Entering saveEntitlement");

    Entitlement entitlement = productBundleService.saveEntitlement(entitlementId, includetedUnits);

    logger.debug("saveEntitlement end...");
    return entitlement;
  }

  @RequestMapping(value = "/entitlement/{entitlementId}/delete", method = RequestMethod.GET)
  @ResponseBody
  public Product deleteEntitlement(@PathVariable String entitlementId,
      @RequestParam(value = "bundleId", required = true) String bundleId, ModelMap map) {
    logger.debug("### deleteEntitlement method start...(GET)");

    Product product = productBundleService.deleteEntitlement(entitlementId);

    logger.debug("### deleteEntitlement method end...(GET)");
    return product;

  }

  @RequestMapping(value = "/{bundleCode}/entitlements/view_filtered", method = RequestMethod.GET)
  public String getFilteredEntitlements(@PathVariable String bundleCode,
      @RequestParam(value = "whichPlan", required = false, defaultValue = "planned") String whichPlan,
      @RequestParam(value = "filterDate", required = false) String filterDate,
      @RequestParam(value = "currentPage", required = false, defaultValue = "1") int currentPage,
      @RequestParam(value = "perPage", required = false, defaultValue = "8") int perPage, ModelMap map,
      HttpServletRequest request) {
    logger.debug("### getFilteredEntitlements method start...(GET) : " + bundleCode);

    map.addAttribute("filterDateInFuture", false);

    Date date = null;
    if (whichPlan.equals("planned")) {
      map.addAttribute("filterDateInFuture", true);
      date = channelService.getFutureRevision(null).getStartDate();
    } else if (whichPlan.equals("current")) {
      date = channelService.getCurrentRevision(null).getStartDate();
    } else if (whichPlan.equals("history")) {
      if (filterDate != null && !filterDate.trim().equals("") && !filterDate.equals("null")) {
        date = DateUtils.getCalendarForDate(filterDate, DateUtils.getDateFormatter("MM/dd/yyyy HH:mm:ss")).getTime();
      }
    }
    ProductBundle bundle = productBundleService.findProductBundleByCode(bundleCode);
    map.addAttribute("productBundle", bundle);

    map.addAttribute("whichPlan", whichPlan);
    mapBundleEntitlements(bundle, map, date, currentPage > 0 ? currentPage : 1, perPage);

    logger.debug("### getFilteredEntitlements method end...(GET) " + map.get("entitlementFilterDate"));
    return "bundle.entitlement.list";
  }

  @RequestMapping(value = "/validate_bundle")
  @ResponseBody
  public String validateBundle(@RequestParam("productBundle.name") final String bundleName) {
    logger.debug("In validateBundle() method start and bundleName is : " + bundleName);

    try {
      productBundleService.locateProductBundleByName(bundleName);
    } catch (NoSuchProductBundleException ex) {
      logger.debug(bundleName + ": not exits.");
      return Boolean.TRUE.toString();
    }

    logger.debug("In validateBundle() method end");
    return Boolean.FALSE.toString();
  }

  @RequestMapping(value = ("/editlogo"), method = RequestMethod.GET)
  public String editBundleLogo(@RequestParam(value = "Id", required = true) String Id, ModelMap map) {
    logger.debug("### editBundleLogo method starting...(GET)");

    ProductBundleLogoForm bundleLogoForm = new ProductBundleLogoForm(productBundleService.locateProductBundleById(Id));
    map.addAttribute("bundleLogoForm", bundleLogoForm);
    setPage(map, Page.PRODUCTS_BUNDLES);

    logger.debug("### edit product logo method end...(GET)");
    return "bundles.editlogo";
  }

  @RequestMapping(value = ("/editlogo"), method = RequestMethod.POST)
  @ResponseBody
  public String editBundleLogo(@ModelAttribute("bundleLogoForm") ProductBundleLogoForm form, BindingResult result,
      HttpServletRequest request, ModelMap map) {
    logger.debug("### editBundleLogo method starting...(POST)");

    String rootImageDir = config.getValue(Names.com_citrix_cpbm_portal_settings_images_uploadPath);
    if (rootImageDir != null && !rootImageDir.trim().equals("")) {
      ProductBundle bundle = form.getBundle();
      ProductBundleLogoFormValidator validator = new ProductBundleLogoFormValidator();
      validator.validate(form, result);
      if (result.hasErrors()) {
        return messageSource.getMessage(result.getFieldError("logo").getCode(), null, request.getLocale());
      } else {
        String bundlesDir = "productbundles";
        File file = new File(FilenameUtils.concat(rootImageDir, bundlesDir));
        if (!file.exists()) {
          file.mkdir();
        }
        String bundlesAbsoluteDir = FilenameUtils.concat(rootImageDir, bundlesDir);
        String relativeImageDir = FilenameUtils.concat(bundlesDir, bundle.getId().toString());
        File file1 = new File(FilenameUtils.concat(bundlesAbsoluteDir, bundle.getId().toString()));
        if (!file1.exists()) {
          file1.mkdir();
        }

        MultipartFile logoFile = form.getLogo();
        try {
          if (!logoFile.getOriginalFilename().trim().equals("")) {
            String logoFileRelativePath = writeMultiPartFileToLocalFile(rootImageDir, relativeImageDir, logoFile);
            bundle.setImagePath(logoFileRelativePath);
          }
          bundle = productBundleService.updateProductBundle(bundle, false);
        } catch (IOException e) {
          logger.debug("###IO Exception in writing custom image file");
        }
      }
      String response = null;
      try {
        response = JSONUtils.toJSONString(bundle);
      } catch (JsonGenerationException e) {
        logger.debug("###IO Exception in writing custom image file");
      } catch (JsonMappingException e) {
        logger.debug("###IO Exception in writing custom image file");
      } catch (IOException e) {
        logger.debug("###IO Exception in writing custom image file");
      }
      return response;
    } else {
      result.rejectValue("logo", "error.custom.image.upload.dir");
      return messageSource.getMessage(result.getFieldError("logo").getCode(), null, request.getLocale());
    }
  }

  /**
   * List all the products
   * 
   * @return
   */
  @RequestMapping(value = "/sortbundles", method = RequestMethod.GET)
  public String sortBundles(@RequestParam(value = "serviceInstanceUUID", required = true) String serviceInstanceUUID,
      ModelMap map) {
    logger.debug("### sortBundles method starting...");

    List<ProductBundle> bundlesList = new ArrayList<ProductBundle>();
    if (!(serviceInstanceUUID == null || serviceInstanceUUID.trim().equals(""))) {
      for (ProductBundle productBundle : productBundleService.listProductBundles(0, 0)) {
        if (productBundle.getServiceInstanceId().getUuid().toString().equals(serviceInstanceUUID.trim())) {
          bundlesList.add(productBundle);
        }
      }
    }
    map.addAttribute("bundlesList", bundlesList);

    logger.debug("### sortBundles method ending...");
    return "bundles.sort";
  }

  @RequestMapping(value = {
    "editbundlesorder"
  }, method = RequestMethod.POST)
  @ResponseBody
  public String editproductsorder(@RequestParam(value = "bundlesOrderData", required = true) String bundlesOrderData,
      ModelMap map) {
    logger.debug("### editProductsOrder method starting...(POST)");

    String[] bundleIds = bundlesOrderData.split(",");
    productBundleService.updateProductBundleSortOrder(bundleIds);
    logger.info("IDs are : " + Arrays.asList(bundleIds));

    logger.debug("### editProductsOrder method ending...(POST)");
    return "success";
  }

  @RequestMapping(value = "/plancharges", method = RequestMethod.GET)
  public String planCharges(ModelMap map) {
    logger.debug("### planCharges method starting...");

    RateCardForm rateCardForm = new RateCardForm();

    Revision futureRevision = channelService.getFutureRevision(null);
    Map<ProductBundle, Map<RateCardComponent, List<RateCardCharge>>> currentBundleChargesMap = productBundleService
        .getChargesForAllBundles(futureRevision.getStartDate());
    rateCardForm.setCurrentBundleChargesMap(currentBundleChargesMap);

    List<CurrencyValue> activeCurrencies = currencyValueService.listActiveCurrencies();
    Set<Entry<ProductBundle, Map<RateCardComponent, List<RateCardCharge>>>> entrySet = currentBundleChargesMap
        .entrySet();
    for (Entry<ProductBundle, Map<RateCardComponent, List<RateCardCharge>>> entry : entrySet) {
      // if current price exists then clone otherwise create new product charge object for each currency.
      if (entry.getValue() != null && entry.getValue().size() > 0) {
        rateCardForm.updateRateCardChargesFormList(entry.getKey(), entry.getValue(), futureRevision, activeCurrencies,
            false);
      } else {
        rateCardForm.updateRateCardChargesFormList(entry.getKey(), entry.getValue(), futureRevision, activeCurrencies,
            true);
      }
    }
    Date startDate = productService.hasPlanndedProductRevisions();
    if (startDate == null) {
      startDate = new Date();
    }
    rateCardForm.setStartDate(startDate);
    map.addAttribute("rateCardForm", rateCardForm);
    map.addAttribute("currencieslist", activeCurrencies);
    map.addAttribute("currencieslistsize", activeCurrencies.size());
    logger.debug("### planCharges method ending...");
    return "plan.bundle.charges";
  }

  @RequestMapping(value = "/plancharges", method = RequestMethod.POST)
  @ResponseBody
  public String planCharges(@ModelAttribute("rateCardForm") RateCardForm form, BindingResult result, ModelMap map) {
    logger.debug("### planCharges(POST) method starting...");

    productBundleService.planProductBundleCharges(form.getCurrentBundleChargesMap(), prepareBundleChargesMap(form),
        form.getStartDate());

    logger.debug("### planCharges(POST) method ending...");
    return "success";
  }

  @RequestMapping(value = "/{bundleCode}/viewbundlecurrentcharges", method = RequestMethod.GET)
  public String viewBundleCurrentCharges(@PathVariable String bundleCode, ModelMap map,
      @RequestParam(value = "currenciesToDisplay", required = false, defaultValue = "") String currenciesToDisplay) {
    logger.debug("### viewBundleCurrentCharges method starting...");

    Revision currentChargeRevision = productService.getCurrentRevision(null);
    ProductBundle bundle = productBundleService.findProductBundleByCode(bundleCode);

    Map<RateCardComponent, List<RateCardCharge>> rateCardComponentChargesMap = productBundleService
        .getReferencePriceBookBundleCharges(bundle, currentChargeRevision.getStartDate());

    RateCardChargesForm rateCardChargesForm = getRateCardChargesForm(rateCardComponentChargesMap);
    rateCardChargesForm.setBundle(bundle);

    map.addAttribute("rateCardChargesForm", rateCardChargesForm);
    List<CurrencyValue> activeCurrencies = currencyValueService.listActiveCurrencies();
    map.addAttribute("currencieslist", activeCurrencies);
    if (currenciesToDisplay == null || currenciesToDisplay == "") {
      currenciesToDisplay = Integer.toString(activeCurrencies.size());
    }

    map.addAttribute("currenciesToDisplay", currenciesToDisplay);
    map.addAttribute("hasHistoricalRevisions", productService.doesReferencePriceBookHaveHistoricalRevisions());
    map.addAttribute("date", currentChargeRevision.getStartDate());

    rateCardChargesForm.setPlanForCurrentCharges(rateCardComponentChargesMap.size() == 0);

    logger.debug("### viewBundleCurrentCharges method ending...");
    return "view.bundle.current.charges";
  }

  private RateCardChargesForm getRateCardChargesForm(
      Map<RateCardComponent, List<RateCardCharge>> rateCardComponentChargesMap) {

    RateCardChargesForm rateCardChargesForm = new RateCardChargesForm();
    Set<Entry<RateCardComponent, List<RateCardCharge>>> entrySet = rateCardComponentChargesMap.entrySet();

    for (Entry<RateCardComponent, List<RateCardCharge>> entry : entrySet) {
      RateCardComponentChargesForm rateCardComponentChargesForm = new RateCardComponentChargesForm();
      RateCardComponent rcc = entry.getKey();
      rateCardComponentChargesForm.setRcc(rcc);
      List<RateCardCharge> chargesList = entry.getValue();
      if (chargesList != null && chargesList.size() > 0) {
        rateCardChargesForm.setPlanForCurrentCharges(false);
      }
      rateCardComponentChargesForm.getCharges().addAll(chargesList);
      if (rcc.isRecurring()) {
        rateCardChargesForm.getRecurringRateCardChargesFormList().add(rateCardComponentChargesForm);
      } else {
        rateCardChargesForm.getNonRecurringRateCardChargesFormList().add(rateCardComponentChargesForm);
      }
    }

    return rateCardChargesForm;
  }

  @RequestMapping(value = "/{bundleCode}/addbundlecurrentcharges", method = RequestMethod.GET)
  public String addBundleCurrentCharges(@PathVariable String bundleCode, ModelMap map) {
    logger.debug("### addBundleCurrentCharges method starting...(GET)");

    ProductBundle bundle = productBundleService.findProductBundleByCode(bundleCode);
    List<CurrencyValue> currencyValueList = currencyValueService.listActiveCurrencies();

    RateCardChargesForm rateCardChargesForm = new RateCardChargesForm();
    rateCardChargesForm.setBundle(bundle);

    List<RateCardComponent> rccList = bundle.getRateCard().getRateCardComponents();

    if (rccList != null && rccList.size() > 0) {
      // if rate card components were already added to rate card as part of plan rate card, no need to create new again
      rateCardChargesForm.addRateCardComponentChargesForm(rccList, currencyValueList);
    } else {
      Revision currentRevision = channelService.getCurrentRevision(null);
      rateCardChargesForm.addRateCardComponentChargesForm(false, currencyValueList, currentRevision);
      if (bundle.getRateCard().getChargeType().getFrequencyInMonths().longValue() != 0) {
        rateCardChargesForm.addRateCardComponentChargesForm(true, currencyValueList, currentRevision);
      }
    }

    map.addAttribute("rateCardChargesForm", rateCardChargesForm);
    map.addAttribute("currencyValueList", currencyValueList);
    map.addAttribute("currencieslistsize", currencyValueList.size());

    logger.debug("### addBundleCurrentCharges method ending...(GET)");
    return "add.bundle.current.charges";
  }

  @RequestMapping(value = "/addbundlecurrentcharges", method = RequestMethod.POST)
  @ResponseBody
  public String addBundleCurrentCharges(@ModelAttribute("rateCardChargesForm") RateCardChargesForm form,
      BindingResult result, ModelMap map) {
    logger.debug("### addBundleCurrentCharges(POST) method starting...");

    ProductBundle bundle = productBundleService.findProductBundleByCode(form.getBundle().getCode());
    Map<RateCardComponent, List<RateCardCharge>> rateCardcomponentChargesMap = new HashMap<RateCardComponent, List<RateCardCharge>>();
    for (RateCardComponentChargesForm componentChargesForm : form.getRecurringRateCardChargesFormList()) {
      rateCardcomponentChargesMap.put(componentChargesForm.getRcc(), componentChargesForm.getCharges());
    }
    for (RateCardComponentChargesForm componentChargesForm : form.getNonRecurringRateCardChargesFormList()) {
      rateCardcomponentChargesMap.put(componentChargesForm.getRcc(), componentChargesForm.getCharges());
    }
    productBundleService.addBundleRateCardComponentsCharge(bundle, rateCardcomponentChargesMap);

    logger.debug("### addBundleCurrentCharges(POST) method ending...");
    return "success";
  }

  @RequestMapping(value = "/{bundleCode}/channelPricing", method = RequestMethod.GET)
  public String viewBundleChannelPricing(@PathVariable String bundleCode,
      @RequestParam(value = "currentPage", required = false, defaultValue = "1") String currentPage,
      @RequestParam(value = "perPage", required = false, defaultValue = "2") String perPage,
      @RequestParam(value = "currenciesToDisplay", required = false, defaultValue = "") String currenciesToDisplay,
      @RequestParam(value = "size", required = false) String size,
      @RequestParam(value = "whichPlan", required = false, defaultValue = "planned") String whichPlan,
      @RequestParam(value = "historyDate", required = false) String historyDate, ModelMap map) {
    logger.debug("### Entering channelPricing");

    ProductBundle bundle = productBundleService.findProductBundleByCode(bundleCode);
    map.put("productBundle", bundle);

    int currentPageValue = currentPage != null ? Integer.parseInt(currentPage) : 1;
    int perPageValue = perPage != null ? Integer.parseInt(perPage) : 10;
    int sizeInt = 0;
    if (size == null || size.equals("")) {
      sizeInt = channelService.listChannelsByProductBundle(bundle, 0, 0).size();
    } else {
      sizeInt = Integer.parseInt(size);
    }

    int totalpages = 1;
    if (perPageValue > 0) {
      if (sizeInt != 0 && sizeInt % perPageValue == 0) {
        totalpages = sizeInt / perPageValue;
      } else {
        totalpages = (sizeInt / perPageValue) + 1;
      }
    }

    // necessary values for pagination
    map.addAttribute("channelsCurrentPage", currentPageValue);
    map.addAttribute("channelstotalpages", sizeInt);
    map.addAttribute("channelsPerPage", perPageValue);
    map.addAttribute("prevPage", currentPageValue > 1 ? currentPageValue - 1 : 1);
    map.addAttribute("nextPage", currentPageValue < totalpages ? currentPageValue + 1 : totalpages);

    if (sizeInt - (currentPageValue * perPageValue) > 0) {
      map.addAttribute("enable_next", "True");
    } else {
      map.addAttribute("enable_next", "False");
    }

    List<Channel> channelList = channelService.listChannelsByProductBundle(bundle, currentPageValue, perPageValue);
    Map<Channel, RateCardChargesForm> channelRateCardChargesFormMap = new HashMap<Channel, RateCardChargesForm>();

    Map<Channel, List<CurrencyValue>> channelCurrencyMap = new HashMap<Channel, List<CurrencyValue>>();

    int maxChannelCurrenciesSize = 1;
    for (Channel channel : channelList) {
      List<CurrencyValue> currencies = channelService.listCurrencies(channel.getParam());
      if (currencies.size() > maxChannelCurrenciesSize) {
        maxChannelCurrenciesSize = currencies.size();
      }
      channelCurrencyMap.put(channel, currencies);

      Revision channelRevision = null;
      if (whichPlan.equals("planned")) {
        channelRevision = channelService.getFutureRevision(channel);
      } else if (whichPlan.equals("current")) {
        channelRevision = channelService.getCurrentRevision(channel);
      } else if (whichPlan.equals("history")) {
        if (historyDate == null || historyDate.trim().equals("")) {
          List<Revision> revisions = productService.getReferencePriceBookHistoryRevisions();
          historyDate = DateUtils.getStringForCalendar(revisions.get(0).getStartDate(),
              DateUtils.getDateFormatter("MM/dd/yyyy HH:mm:ss"));
        }
        Date date = DateUtils.getCalendarForDate(historyDate, DateUtils.getDateFormatter("MM/dd/yyyy HH:mm:ss"))
            .getTime();
        channelRevision = channelService.getRevisionForTheDateGiven(date, channel);
      }

      Map<RateCardComponent, List<RateCardCharge>> defaultRateCardCharges = new HashMap<RateCardComponent, List<RateCardCharge>>();
      Map<RateCardComponent, List<RateCardCharge>> overriddenRateCardCharges = new HashMap<RateCardComponent, List<RateCardCharge>>();

      if (channelRevision != null) {
        Date channelRevisionDate = channelRevision.getStartDate();
        Date rpbRevisionDate = channelService.getChannelReferenceCatalogRevision(channel, channelRevision)
            .getReferenceCatalogRevision().getStartDate();
        defaultRateCardCharges = productBundleService.getReferencePriceBookBundleCharges(bundle, channelRevisionDate);
        overriddenRateCardCharges = productBundleService.getProductBundleChargesForTheCatalogAndDateGiven(
            channel.getCatalog(), bundle, rpbRevisionDate);
      }

      RateCardChargesForm rccForm = new RateCardChargesForm();
      rccForm.setBundle(bundle);
      for (Entry<RateCardComponent, List<RateCardCharge>> rccEntry : defaultRateCardCharges.entrySet()) {
        RateCardComponent rcc = rccEntry.getKey();
        RateCardComponentChargesForm rccChargesForm = new RateCardComponentChargesForm();
        rccChargesForm.setRcc(rcc);
        rccChargesForm.setCharges(new ArrayList<RateCardCharge>());

        Map<CurrencyValue, Boolean> isCurrencyAdded = new HashMap<CurrencyValue, Boolean>();
        if (overriddenRateCardCharges.get(rcc) != null) {
          for (RateCardCharge charge : overriddenRateCardCharges.get(rcc)) {
            isCurrencyAdded.put(charge.getCurrencyValue(), true);
            rccChargesForm.getCharges().add(charge);
          }
        }

        if (defaultRateCardCharges.get(rcc) != null) {
          for (RateCardCharge charge : defaultRateCardCharges.get(rcc)) {
            if (isCurrencyAdded.get(charge.getCurrencyValue()) != null
                && isCurrencyAdded.get(charge.getCurrencyValue())) {
              continue;
            } else {
              rccChargesForm.getCharges().add(charge);
            }
          }
        }

        if (rccChargesForm.getRcc().isRecurring()) {
          rccForm.getRecurringRateCardChargesFormList().add(rccChargesForm);
        } else {
          rccForm.getNonRecurringRateCardChargesFormList().add(rccChargesForm);
        }
      }
      channelRateCardChargesFormMap.put(channel, rccForm);
    }
    if (currenciesToDisplay == null || currenciesToDisplay == "") {
      currenciesToDisplay = Integer.toString(maxChannelCurrenciesSize);
    }
    map.addAttribute("currenciesToDisplay", currenciesToDisplay);
    map.addAttribute("channelCurrencyMap", channelCurrencyMap);
    map.addAttribute("channelRateCardChargesFormMap", channelRateCardChargesFormMap);
    logger.debug("### Exiting channelPricing");
    return "bundle.channelpricing";
  }

  @RequestMapping(value = "/{bundleCode}/provisioningconstraints", method = RequestMethod.GET)
  public String viewProvisioningConstraints(@PathVariable String bundleCode,
      @RequestParam(value = "whichPlan", required = false, defaultValue = "planned") String whichPlan,
      @RequestParam(value = "historyDate", required = false) String historyDate, ModelMap map) {
    logger.debug("### Entering provisioningconstraints");

    ProductBundle bundle = productBundleService.findProductBundleByCode(bundleCode);
    map.put("productBundle", bundle);

    List<ProvisioningConstraint> provisioningConstarints = new ArrayList<ProvisioningConstraint>();
    if (whichPlan.equals("planned")) {
      provisioningConstarints = productBundleService.getProductBundleRevision(bundle,
          channelService.getFutureRevision(null).getStartDate(), null).getProvisioningConstraints();
    } else if (whichPlan.equals("current")) {
      provisioningConstarints = productBundleService.getProductBundleRevision(bundle,
          channelService.getCurrentRevision(null).getStartDate(), null).getProvisioningConstraints();
    } else if (whichPlan.equals("history")) {
      if (historyDate == null || historyDate.trim().equals("")) {
        List<Revision> revisions = productService.getReferencePriceBookHistoryRevisions();
        historyDate = DateUtils.getStringForCalendar(revisions.get(0).getStartDate(),
            DateUtils.getDateFormatter("MM/dd/yyyy HH:mm:ss"));
      }

      Date historyRevDate = DateUtils
          .getCalendarForDate(historyDate, DateUtils.getDateFormatter("MM/dd/yyyy HH:mm:ss")).getTime();
      provisioningConstarints = productBundleService.getProductBundleRevision(bundle, historyRevDate, null)
          .getProvisioningConstraints();
    }

    map.addAttribute("constraints", provisioningConstarints);
    logger.debug("### Exiting constraints");
    return "bundle.provisioningconstraints";
  }

  /**
   * View charges for all bundles.
   * 
   * @param map
   * @return String
   */
  @RequestMapping(value = "/viewplannedcharges", method = RequestMethod.GET)
  public String viewPlannedCharges(ModelMap map) {
    logger.debug("### viewPlannedCharges method starting...");

    RateCardForm rateCardForm = new RateCardForm();
    Revision futureRevision = channelService.getFutureRevision(null);
    rateCardForm.setStartDate(futureRevision.getStartDate());

    Map<ProductBundle, Map<RateCardComponent, List<RateCardCharge>>> plannedBundleChargesMap = productBundleService
        .getPlannedChargesForAllBundles(futureRevision.getStartDate());

    Set<Entry<ProductBundle, Map<RateCardComponent, List<RateCardCharge>>>> entrySet = plannedBundleChargesMap
        .entrySet();
    for (Entry<ProductBundle, Map<RateCardComponent, List<RateCardCharge>>> entry : entrySet) {
      RateCardChargesForm rateCardChargesForm = new RateCardChargesForm();
      rateCardChargesForm.setBundle(entry.getKey());
      Map<RateCardComponent, List<RateCardCharge>> rateCardComponentChargesMap = entry.getValue();
      Set<Entry<RateCardComponent, List<RateCardCharge>>> rateCardComponentEntrySet = rateCardComponentChargesMap
          .entrySet();

      for (Entry<RateCardComponent, List<RateCardCharge>> rateCardComponentEntry : rateCardComponentEntrySet) {
        RateCardComponentChargesForm rateCardComponentChargesForm = new RateCardComponentChargesForm();
        RateCardComponent rcc = rateCardComponentEntry.getKey();
        rateCardComponentChargesForm.setRcc(rcc);
        rateCardComponentChargesForm.getCharges().addAll(rateCardComponentEntry.getValue());

        if (rcc.isRecurring()) {
          rateCardChargesForm.getRecurringRateCardChargesFormList().add(rateCardComponentChargesForm);
        } else {
          rateCardChargesForm.getNonRecurringRateCardChargesFormList().add(rateCardComponentChargesForm);
        }
      }
      rateCardForm.getRateCardChargesList().add(rateCardChargesForm);
    }

    List<CurrencyValue> activeCurrencies = currencyValueService.listActiveCurrencies();
    map.addAttribute("currencieslist", activeCurrencies);
    map.addAttribute("currencieslistsize", activeCurrencies.size());
    map.addAttribute("rateCardForm", rateCardForm);
    return "view.all.bundle.planned.charges";
  }

  @RequestMapping(value = "/editplannedcharges", method = RequestMethod.GET)
  public String editPlannedCharges(ModelMap map) {
    logger.debug("### editPlannedCharges method starting...");

    RateCardForm rateCardForm = new RateCardForm();

    Date startDate = productBundleService.hasPlanndedBundleRevisions();
    if (startDate == null) {
      startDate = new Date();
    }

    rateCardForm.setStartDate(startDate);
    Map<ProductBundle, Map<RateCardComponent, List<RateCardCharge>>> plannedBundleChargesMap = productBundleService
        .getPlannedChargesForAllBundles(channelService.getFutureRevision(null).getStartDate());

    rateCardForm.setCurrentBundleChargesMap(plannedBundleChargesMap);
    List<CurrencyValue> activeCurrencies = currencyValueService.listActiveCurrencies();
    Revision futureRevision = channelService.getFutureRevision(null);
    Set<Entry<ProductBundle, Map<RateCardComponent, List<RateCardCharge>>>> entrySet = plannedBundleChargesMap
        .entrySet();
    for (Entry<ProductBundle, Map<RateCardComponent, List<RateCardCharge>>> entry : entrySet) {
      // if current price exists then clone otherwise create new product charge object for each currency.
      if (entry.getValue() != null && entry.getValue().size() > 0) {
        rateCardForm.updateRateCardChargesFormList(entry.getKey(), entry.getValue(), futureRevision, activeCurrencies,
            false);
      } else {
        rateCardForm.updateRateCardChargesFormList(entry.getKey(), entry.getValue(), futureRevision, activeCurrencies,
            true);
      }
    }
    map.addAttribute("rateCardForm", rateCardForm);
    map.addAttribute("currencieslist", activeCurrencies);
    map.addAttribute("currencieslistsize", activeCurrencies.size());

    logger.debug("### viewPlannedCharges method ending...");
    return "edit.bundle.planned.charges";
  }

  @RequestMapping(value = "/editplannedcharges", method = RequestMethod.POST)
  @ResponseBody
  public String editPlannedCharges(@ModelAttribute("rateCardForm") RateCardForm form, BindingResult result, ModelMap map) {
    logger.debug("### editPlannedCharges(POST) method starting...");

    productBundleService.editPlannedProductBundleCharges(form.getCurrentBundleChargesMap(),
        prepareBundleChargesMap(form), channelService.getFutureRevision(null));

    logger.debug("### editPlannedCharges(POST) method ending...");
    return "success";
  }

  private Map<ProductBundle, Map<RateCardComponent, List<RateCardCharge>>> prepareBundleChargesMap(RateCardForm form) {
    Map<ProductBundle, Map<RateCardComponent, List<RateCardCharge>>> newBundleChargesMap = new HashMap<ProductBundle, Map<RateCardComponent, List<RateCardCharge>>>();
    for (RateCardChargesForm chargeForm : form.getRateCardChargesList()) {

      Map<RateCardComponent, List<RateCardCharge>> rateCardComponentChargeMap = new HashMap<RateCardComponent, List<RateCardCharge>>();
      for (RateCardComponentChargesForm recurringRateCardComponentCharge : chargeForm
          .getRecurringRateCardChargesFormList()) {
        rateCardComponentChargeMap.put(recurringRateCardComponentCharge.getRcc(),
            recurringRateCardComponentCharge.getCharges());
      }

      for (RateCardComponentChargesForm nonRecurringRateCardComponentCharge : chargeForm
          .getNonRecurringRateCardChargesFormList()) {
        rateCardComponentChargeMap.put(nonRecurringRateCardComponentCharge.getRcc(),
            nonRecurringRateCardComponentCharge.getCharges());
      }

      ProductBundle bundle = productBundleService.findProductBundleByCode(chargeForm.getBundle().getCode());
      newBundleChargesMap.put(bundle, rateCardComponentChargeMap);
    }

    return newBundleChargesMap;
  }

  @RequestMapping(value = "/{bundleCode}/viewbundleplannedcharges", method = RequestMethod.GET)
  public String viewBundlePlannedCharges(@PathVariable String bundleCode, ModelMap map,
      @RequestParam(value = "currenciesToDisplay", required = false, defaultValue = "") String currenciesToDisplay) {
    logger.debug("### viewBundlePlannedCharges method starting...");

    ProductBundle bundle = productBundleService.findProductBundleByCode(bundleCode);
    Revision futureRevision = channelService.getFutureRevision(null);

    Map<RateCardComponent, List<RateCardCharge>> rateCardComponentChargesMap = productBundleService
        .getReferencePriceBookPlannedBundleCharges(bundle, futureRevision.getStartDate());

    RateCardChargesForm rateCardChargesForm = getRateCardChargesForm(rateCardComponentChargesMap);
    rateCardChargesForm.setBundle(bundle);

    map.addAttribute("rateCardChargesForm", rateCardChargesForm);

    List<CurrencyValue> activeCurrencies = currencyValueService.listActiveCurrencies();
    map.addAttribute("currencieslist", activeCurrencies);
    if (currenciesToDisplay == null || currenciesToDisplay == "") {
      currenciesToDisplay = Integer.toString(activeCurrencies.size());
    }
    map.addAttribute("currenciesToDisplay", currenciesToDisplay);

    map.addAttribute("date", futureRevision.getStartDate());
    map.addAttribute("hasHistoricalRevisions", productService.doesReferencePriceBookHaveHistoricalRevisions());

    logger.debug("### viewBundlePlannedCharges method ending...");
    return "view.bundle.planned.charges";
  }

  @RequestMapping(value = "/{productBundleCode}/viewbundlechargeshistorydata", method = RequestMethod.GET)
  public String viewBundleChargesHistoryData(@PathVariable String productBundleCode,
      @RequestParam(value = "revisionDate", required = true) String revisionDate, ModelMap map,
      @RequestParam(value = "currenciesToDisplay", required = false, defaultValue = "") String currenciesToDisplay) {

    logger.debug("### viewproductbundlechargeshistorydata method starting...");

    ProductBundle productBundle = productBundleService.locateProductBundleByCode(productBundleCode);

    if (revisionDate == null || revisionDate.equals("")) {
      List<Revision> revisions = productService.getReferencePriceBookHistoryRevisions();
      revisionDate = DateUtils.getStringForCalendar(revisions.get(0).getStartDate(),
          DateUtils.getDateFormatter("MM/dd/yyyy HH:mm:ss"));
    }

    Date historyDate = DateUtils.getCalendarForDate(revisionDate, DateUtils.getDateFormatter("MM/dd/yyyy HH:mm:ss"))
        .getTime();
    Map<RateCardComponent, List<RateCardCharge>> chargesHistoryMap = productBundleService
        .getReferencePriceBookBundleCharges(productBundle, historyDate);

    RateCardChargesForm rateCardChargesForm = getRateCardChargesForm(chargesHistoryMap);
    rateCardChargesForm.setBundle(productBundle);
    map.addAttribute("rateCardChargesForm", rateCardChargesForm);

    List<CurrencyValue> activeCurrencies = currencyValueService.listActiveCurrencies();
    if (currenciesToDisplay == null || currenciesToDisplay == "") {
      currenciesToDisplay = Integer.toString(activeCurrencies.size());
    }
    map.addAttribute("currencieslist", activeCurrencies);
    map.addAttribute("currenciesToDisplay", currenciesToDisplay);
    map.addAttribute("date", historyDate);

    logger.debug("### viewproductbundlechargeshistorydata method ending...");
    return "productbundle.charges.history";
  }

  @RequestMapping(value = {
    "getBundle.json"
  }, method = RequestMethod.GET)
  @ResponseBody
  public ProductBundle getProductBundle(@ModelAttribute("currentTenant") Tenant currentTenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "bundleId", required = true) String bundleId) {
    logger.debug("### getProductBundle method starting...");

    ProductBundle bundle = productBundleService.locateProductBundleById(bundleId);

    logger.debug("### getProductBundle method ending...");
    return bundle;
  }

}
