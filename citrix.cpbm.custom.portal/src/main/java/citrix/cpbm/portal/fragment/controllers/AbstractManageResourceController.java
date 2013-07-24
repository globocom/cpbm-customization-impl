/* Copyright (C) 2011 Citrix Systems, Inc. All rights reserved. */
package citrix.cpbm.portal.fragment.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import citrix.cpbm.access.proxy.CustomProxy;
import citrix.cpbm.portal.forms.SubscriptionForm;

import com.citrix.cpbm.platform.admin.service.ConnectorConfigurationManager;
import com.citrix.cpbm.platform.admin.service.ConnectorManagementService;
import com.citrix.cpbm.platform.admin.service.exceptions.ConnectorManagementServiceException;
import com.citrix.cpbm.platform.model.SsoObject;
import com.citrix.cpbm.platform.spi.CloudConnector;
import com.citrix.cpbm.platform.spi.SsoHandler;
import com.citrix.cpbm.platform.spi.View;
import com.citrix.cpbm.platform.spi.ViewResolver;
import com.citrix.cpbm.platform.util.CssdkConstants;
import com.vmops.internal.service.CustomFieldService;
import com.vmops.internal.service.SubscriptionService;
import com.vmops.model.ProductBundle;
import com.vmops.model.Service;
import com.vmops.model.ServiceResourceType;
import com.vmops.model.ServiceResourceTypeProperty;
import com.vmops.model.Subscription;
import com.vmops.model.SubscriptionHandle;
import com.vmops.model.Tenant;
import com.vmops.model.User;
import com.vmops.service.ChannelService;
import com.vmops.service.ProductBundleService;
import com.vmops.service.exceptions.AjaxFormValidationException;
import com.vmops.service.exceptions.CloudServiceException;
import com.vmops.service.exceptions.ProvisionResourceFailedException;
import com.vmops.web.controllers.AbstractAuthenticatedController;
import com.vmops.web.interceptors.UserContextInterceptor;
import com.vmops.web.validators.ValidationUtil;

public abstract class AbstractManageResourceController extends AbstractAuthenticatedController {

  @Autowired
  private ConnectorManagementService connectorManagementService;

  @Autowired
  private ConnectorConfigurationManager connectorConfigurationManager;

  private SsoHandler ssoHandler;

  @Autowired
  private SubscriptionService subscriptionService;

  @Autowired
  private CustomFieldService customFieldService;

  @Autowired
  private ProductBundleService productBundleService;

  @Autowired
  private ChannelService channelService;

  private String SERVICEBUNDLE = "__SERVICE__";

  Logger logger = Logger.getLogger(AbstractManageResourceController.class);

  @RequestMapping(value = "/getresourceviews", method = RequestMethod.GET)
  @ResponseBody
  public List<View> getRecourceViews(@ModelAttribute("currentTenant") Tenant currentTenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "serviceInstanceUUID", required = false) String serviceInstanceUUID, ModelMap map,
      HttpServletRequest request) {
    ViewResolver viewResolver = ((CloudConnector) connectorManagementService.getServiceInstance(serviceInstanceUUID))
        .getViewResolver();
    User user = getCurrentUser();
    if ((Boolean) request.getAttribute("isSurrogatedTenant")) {
      if (userService.getUserHandleByServiceInstanceUuid(user.getUuid(), serviceInstanceUUID) != null) {
        user = tenantService.get(tenantParam).getOwner();
      } else {
        throw new CloudServiceException(messageSource.getMessage("message.no.cloud.account", new String[] {
          user.getUsername()
        }, getSessionLocale(request)));
      }
    }
    List<View> resourceViewList = viewResolver.listResourceViews(user);
    map.addAttribute("resourceViews", resourceViewList);
    return resourceViewList;
  }

  @RequestMapping(value = "/getSSOCmdString", method = RequestMethod.POST)
  @ResponseBody
  public Map<String, String> getSSOCmdString(@ModelAttribute("currentTenant") Tenant tenant,
      @RequestParam(value = "tenant", required = false) String tenantParam,
      @RequestParam(value = "serviceInstanceUUID", required = true) String serviceInstanceUUID, ModelMap map,
      HttpServletRequest request, HttpServletResponse response) {
    logger.debug("### In getSSOCmdString  start method (POST)...");
    SsoObject ssoObject = new SsoObject();
    Tenant userTenant = tenant;
    Map<String, String> responseMap = new HashMap<String, String>();
    if (tenantParam != null) {
      userTenant = tenantService.get(tenantParam);
    }
    User currentUser = getCurrentUser();
    if (userTenant.getState() == Tenant.State.NEW) {
      responseMap.put("status", "fail");
      if (!userTenant.getOwner().equals(currentUser)) {
        responseMap.put("error_message",
            messageSource.getMessage("message.user.no.billing", null, getSessionLocale(request)));
        responseMap.put("url", "/portal/portal/home");
      } else {
        responseMap.put("url", "/portal/portal/tenants/editcurrent");
      }
    } else {
      // if you login as root and try to launch VM under specific account then we have pass specific account and account
      // owner.
      ssoHandler = ((CloudConnector) connectorManagementService.getServiceInstance(serviceInstanceUUID))
          .getSSOHandler();
      if (ssoHandler != null) {
        if (!userTenant.equals(tenant)) {
          ssoObject = ssoHandler.handleLogin(userTenant.getOwner());
        } else {
          ssoObject = ssoHandler.handleLogin(getCurrentUser());
        }
        if (ssoObject != null && ssoObject.getCookies().size() > 0) {
          for (Cookie cookie : ssoObject.getCookies()) {
            response.addCookie(cookie);
            logger.debug("Sending cookies:" + cookie.getName());
          }
        }
        responseMap.put("callback", ssoObject.getCallBack());
        responseMap.put("status", "success");
        responseMap.put("cmdString", ssoObject.getSsoString());
      } else {
        responseMap.put("status", "success");
      }
    }
    logger.debug("### getSSOCmdString method ending...(POST)");
    return responseMap;
  }

  /**
   * @param currentTenant the current tenant
   * @return jobId = job ID from cloudstack If user doesn't have billing info yet, show billing edit page, then redirect
   *         user back here (create VM page). 1. make sure user has enough $ in their accounts 2. create subscription 3.
   *         start VM, get an job ID back (should be saved somewhere for page reload or session restart) 4. return job
   *         ID back which will be used by the client to check VM launching status directly with cloudstack
   * @throws ConnectorManagementServiceException
   */
  @RequestMapping(value = "/deployVm", method = RequestMethod.POST)
  @ResponseBody
  public Map<String, String> deployVm(@ModelAttribute("subscriptionForm") SubscriptionForm subscriptionForm,
      BindingResult result, @RequestParam(value = "tenant", required = true) String tenantParam,
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
    ProductBundle productBundle = productBundleService.locateProductBundleById(productBundleId);

    User user = getCurrentUser();
    Tenant effectiveTenant = (Tenant) request.getAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY);

    User effectiveUser = user;
    if ((Boolean) request.getAttribute("isSurrogatedTenant")) {
      effectiveUser = effectiveTenant.getOwner();
    }
    Subscription currentSubscription = null;
    try {
      currentSubscription = subscriptionService.locateSubscriptionById(Long.parseLong(subscriptionId));
    } catch (NumberFormatException e) {

    }
    SubscriptionHandle subscriptionHandle = null;
    if (currentSubscription != null) {
      subscriptionHandle = currentSubscription.getActiveHandle();
    }
    Subscription newSubscription = currentSubscription;
    boolean isReconfigure = false;
    boolean isValid = false;
    Subscription subscriptionForUse = null;
    String configurationJsonString = getConfigurationJsonString(filterMap, contextMap, configurationData);
    String jobId = "";
    // Handle newSubscriptionId case. If its not null then code flow will never create subscription.
    if (newSubscriptionId != null) {
      try {
        subscriptionForUse = subscriptionService.locateSubscriptionById(Long.parseLong(newSubscriptionId));
      } catch (NumberFormatException e) {
        // TODO: Raise error
        return responseMap;
      }
      if (subscriptionForUse == null) {
        // TODO: Raise error
        return responseMap;
      }
      isValid = subscriptionService.isCurrentSubscriptionValid(
          (citrix.cpbm.access.Subscription) CustomProxy.newInstance(subscriptionForUse), productBundle, result);
      if (!isValid) {
        // TODO: Raise error
        return responseMap;
      }
      if (result.hasErrors()) {
        throw new AjaxFormValidationException(result);
      }
      subscriptionForUse.setConfigurationData(configurationJsonString);
      if (subscriptionHandle != null) {
        // Reconfigure.
        if (!subscriptionForUse.equals(currentSubscription)) {
          subscriptionForUse.setDerivedFrom(currentSubscription);
        }
        try {
          jobId = subscriptionService.updateResource(subscriptionForUse);
        } catch (ProvisionResourceFailedException e) {
          throw new RuntimeException(e);
        }
      } else {
        // Provision on new subscription
        try {
          jobId = subscriptionService.provisionResource(subscriptionForUse);
        } catch (ProvisionResourceFailedException e) {
          throw new RuntimeException(e);
        }
      }
      responseMap.put("jobId", jobId.toString());
      if (currentSubscription != null) {
        currentSubscription.setCustomFieldInfo(subscriptionForm.getSubscription().getCustomFieldInfo());
        subscriptionService.updateSubscription(currentSubscription);
      }
      subscriptionService.updateSubscription(subscriptionForUse);
      return responseMap;
    }
    isValid = subscriptionService.isCurrentSubscriptionValid(currentSubscription, productBundle);
    if (isValid) {
      // Only components have been edited.
      if (currentSubscription.getActiveHandle() != null) {
        isProvision = false;
        isReconfigure = true;
      } else {
        isProvision = true;
      }
      currentSubscription.setConfigurationData(configurationJsonString);
      currentSubscription.setCustomFieldInfo(subscriptionForm.getSubscription().getCustomFieldInfo());
      subscriptionService.updateSubscription(currentSubscription);
      responseMap.put("subscriptionId", currentSubscription.getUuid());
    } else {
      // Case of new resource provision
      newSubscription = subscriptionService.createSubscription(effectiveUser, productBundle, serviceInstaceUuid,
          resourceType, isProvision, true, subscriptionForm.getSubscription(), result);
      // New subscription got created , so provision will be called by workflow.
      isProvision = false;
      if (!result.hasErrors()) {
        newSubscription.setConfigurationData(configurationJsonString);
        responseMap.put("subscriptionId", newSubscription.getUuid());
        if (currentSubscription != null && subscriptionHandle != null) {
          // Case of reconfigure with new bundle.
          newSubscription.setDerivedFrom(currentSubscription);
        }
      }
    }
    if (!result.hasErrors()) {
      if (isReconfigure && !resourceType.equals(SERVICEBUNDLE)) {
        try {
          jobId = subscriptionService.updateResource(newSubscription);
        } catch (ProvisionResourceFailedException e) {
          throw new RuntimeException(e);
        }
      }
      if (isProvision && !resourceType.equals(SERVICEBUNDLE)) {
        try {
          jobId = subscriptionService.provisionResource(newSubscription);
        } catch (ProvisionResourceFailedException e) {
          throw new RuntimeException(e);
        }
      }
      responseMap.put("jobId", jobId.toString());
      subscriptionService.updateSubscription(newSubscription);
    } else {
      throw new AjaxFormValidationException(result);
    }
    return responseMap;
  }

  private String getConfigurationJsonString(Map<String, String> filterMap, Map<String, String> contextMap,
      String configurationData) {
    JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(configurationData);
    for (String key : filterMap.keySet()) {
      jsonObject.put(key, filterMap.get(key));
    }
    for (String key : contextMap.keySet()) {
      jsonObject.put(key, contextMap.get(key));
    }
    return jsonObject.toString();
  }

  // @ModelAttribute("subscriptionForm")
  public SubscriptionForm getSubscriptionForm(ModelMap map, HttpServletRequest request) {
    Object form = map.get("subscriptionForm");
    if (form == null) {
      form = request.getAttribute("subscriptionForm");
    }
    return (SubscriptionForm) form;
  }
}
