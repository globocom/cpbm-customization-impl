/*
 * Copyright © 2013 Citrix Systems, Inc. You may not use, copy, or modify this file except pursuant to a valid license
 * agreement from Citrix Systems, Inc.
 */
package fragment.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.tiles.definition.NoSuchDefinitionException;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;

import web.WebTestsBase;

import com.citrix.cpbm.access.proxy.CustomProxy;
import com.citrix.cpbm.platform.admin.service.exceptions.ConnectorManagementServiceException;
import com.citrix.cpbm.platform.spi.CloudConnector;
import com.citrix.cpbm.platform.spi.DynamicResourceTypeMetadataRegistry;
import com.citrix.cpbm.platform.spi.FilterComponent;
import com.citrix.cpbm.platform.spi.ResourceComponent;
import com.citrix.cpbm.portal.forms.SubscriptionForm;
import com.citrix.cpbm.portal.fragment.controllers.SubscriptionController;
import com.vmops.internal.service.SubscriptionService;
import com.vmops.model.Channel;
import com.vmops.model.ChannelRevision;
import com.vmops.model.ChargeRecurrenceFrequency;
import com.vmops.model.Configuration;
import com.vmops.model.Product;
import com.vmops.model.ProductBundle;
import com.vmops.model.ProductCharge;
import com.vmops.model.ServiceInstance;
import com.vmops.model.ServiceResourceType;
import com.vmops.model.ServiceResourceTypeGeneratedUsage;
import com.vmops.model.Subscription;
import com.vmops.model.Tenant;
import com.vmops.persistence.ProductDAO;
import com.vmops.persistence.ServiceInstanceDao;
import com.vmops.persistence.SubscriptionDAO;
import com.vmops.portal.config.Configuration.Names;
import com.vmops.service.ChannelService;
import com.vmops.service.ConfigurationService;
import com.vmops.service.ProductBundleService;
import com.vmops.service.TenantService;
import com.vmops.web.interceptors.UserContextInterceptor;
import common.MockCloudInstance;

public class AbstractSubscriptionControllerTest extends WebTestsBase {

  @Autowired
  SubscriptionController controller;

  @Autowired
  TenantService service;

  @Autowired
  SubscriptionDAO subscriptionDAO;

  @Autowired
  private ProductBundleService bundleservice;

  @Autowired
  private SubscriptionService subscriptionService;

  @Autowired
  ServiceInstanceDao serviceInstanceDAO;

  @Autowired
  ChannelService channelService;

  @Autowired
  ConfigurationService configurationService;

  private ModelMap map;

  private MockHttpServletRequest request;

  private MockHttpServletResponse response;

  FilterComponent component = new FilterComponent("zone", "122");

  ArrayList<FilterComponent> componentsList = new ArrayList<FilterComponent>();

  ResourceComponent resourceComponent = new ResourceComponent("VirtualMachine", "Template", "1");

  ArrayList<ResourceComponent> resourceComponentList = new ArrayList<ResourceComponent>();

  @Autowired
  ProductDAO productdao;

  @Override
  public void prepareMock() {
    componentsList.add(component);
    resourceComponentList.add(resourceComponent);
    MockCloudInstance mock = this.getMockCloudInstance();
    CloudConnector connector = mock.getCloudConnector();
    DynamicResourceTypeMetadataRegistry metadataRegistry = (DynamicResourceTypeMetadataRegistry) mock
        .getMetadataRegistry();
    List<String> resourceTypes = new ArrayList<String>();
    resourceTypes.add("resource_type_name");
    resourceTypes.add("VirtualMachine");
    resourceTypes.add("Network");
    resourceTypes.add("dummyResource");
    resourceTypes.add("MRD_RT1");
    resourceTypes.add("MRD_RT2");
    resourceTypes.add("MRD_RT4");
    resourceTypes.add("PSI_RT1");
    resourceTypes.add("PSI_RT2");
    resourceTypes.add("PSI_RT3");
    resourceTypes.add("PSI_RT4");
    resourceTypes.add("PSI_RT5");
    resourceTypes.add("PSI_RT6");
    resourceTypes.add("PSI_RT7");
    resourceTypes.add("PSI_RT8");
    resourceTypes.add("MRD_RT5");
    resourceTypes.add("PSI_RT41000");
    Map<String, String> discriminatorMap = new HashMap<String, String>();
    EasyMock.expect(metadataRegistry.getDiscriminatorValues(EasyMock.anyObject(String.class)))
        .andReturn(discriminatorMap).anyTimes();
    EasyMock.expect(connector.getServiceInstanceUUID()).andReturn("1111111").anyTimes();
    EasyMock.expect(connector.getStatus()).andReturn(true).anyTimes();
    EasyMock
        .expect(
            metadataRegistry.getFilterValues(EasyMock.anyObject(String.class), EasyMock.anyObject(String.class),
                EasyMock.anyObject(String.class))).andReturn(componentsList).anyTimes();
    EasyMock
        .expect(
            metadataRegistry.getResourceComponentValues(EasyMock.anyObject(String.class),
                EasyMock.anyObject(String.class), EasyMock.anyObject(String.class), EasyMock.anyObject(String.class),
                EasyMock.<Map<String, String>> anyObject(), EasyMock.<Map<String, String>> anyObject()))
        .andReturn(resourceComponentList).anyTimes();

    EasyMock
        .expect(
            metadataRegistry.getResourceComponentValues(EasyMock.anyObject(String.class),
                EasyMock.anyObject(String.class))).andReturn(resourceComponentList).anyTimes();
    EasyMock
        .expect(metadataRegistry.getResourceTypes(EasyMock.anyObject(String.class), EasyMock.anyObject(String.class)))
        .andReturn(resourceTypes).anyTimes();
    EasyMock.replay(connector, metadataRegistry);

  }

  @Before
  public void init() throws Exception {
    map = new ModelMap();
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testutilityrates_lightbox() {
    Tenant tenant = service.getTenantByParam("id", "1", false);
    Product product = productdao.find(1L);

    request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
    request.setAttribute("isSurrogatedTenant", false);
    ServiceInstance instance = serviceInstanceDAO.find(1L);

    String contextString = "PSI_UD1=10";
    controller.utilityrates_lightbox(instance.getUuid(), "PSI_RT1", contextString, null, map, request);
    Assert.assertEquals("PSI_RT1", map.get("resourceTypeName"));
    Assert.assertEquals(tenant, map.get("tenant"));
    Assert.assertEquals(tenant.getCurrency(), map.get("currency"));

    Map<Object, Object> retMap = (Map<Object, Object>) map.get("retMap");

    Map<Object, Object> retVal = (Map<Object, Object>) retMap.get(instance.getService());
    Map<Product, ProductCharge> chargeMap = (Map<Product, ProductCharge>) retVal.get(instance);
    ProductCharge charge = chargeMap.get(product);
    Assert.assertEquals("USD", charge.getCurrencyValue().getCurrencyCode());
    Assert.assertEquals("20.0000", charge.getPrice().toString());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUtilityrates_table() {
    try {
      Tenant tenant = tenantDAO.find(4L);
      ServiceInstance instance = serviceInstanceDAO.find(1L);
      Channel channel = channelDAO.find(1L);
      String[] generatedUsage = {
          "RUNNING_VM", "ALLOCATED_VM"
      };

      controller.utilityrates_table(tenant.getParam(), instance.getUuid(), "VirtualMachine", "PSI_UD1=10", "USD",
          "false", null, null, null, null, map, request);

      Assert.assertEquals(channelService.getCurrentRevision(channel).getStartDate(), map.get("startDate"));
      int i = 0;

      List<ServiceResourceTypeGeneratedUsage> usageList = (List<ServiceResourceTypeGeneratedUsage>) map
          .get("generatedUsageListForServiceResourceType");
      for (ServiceResourceTypeGeneratedUsage serviceResourceTypeGeneratedUsage : usageList) {
        Assert.assertEquals(generatedUsage[i], serviceResourceTypeGeneratedUsage.getUsageTypeName());
        i++;
      }
      Assert.assertNull(map.get("tenant"));
      Assert.assertEquals(currencyValueService.locateBYCurrencyCode("USD"), map.get("currency"));

      Product product = productdao.find(1L);

      Map<Object, Object> retMap = (Map<Object, Object>) map.get("retMap");
      Map<Object, Object> retVal = (Map<Object, Object>) retMap.get(instance.getService());
      Map<Product, ProductCharge> chargeMap = (Map<Product, ProductCharge>) retVal.get(instance);
      ProductCharge charge = chargeMap.get(product);
      Assert.assertEquals("USD", charge.getCurrencyValue().getCurrencyCode());
      Assert.assertEquals("20.0000", charge.getPrice().toString());

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUtilityrates_tableForATenant() {
    try {
      Tenant tenant = tenantDAO.find(4L);
      ServiceInstance instance = serviceInstanceDAO.find(1L);
      Channel channel = channelDAO.find(4L);
      String[] generatedUsage = {
          "RUNNING_VM", "ALLOCATED_VM"
      };

      request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
      controller.utilityrates_table(tenant.getParam(), instance.getUuid(), "VirtualMachine", "PSI_UD1=10", "JPY",
          "false", null, null, null, null, map, request);

      Assert.assertEquals(channelService.getCurrentRevision(channel).getStartDate(), map.get("startDate"));
      int i = 0;

      List<ServiceResourceTypeGeneratedUsage> usageList = (List<ServiceResourceTypeGeneratedUsage>) map
          .get("generatedUsageListForServiceResourceType");
      for (ServiceResourceTypeGeneratedUsage serviceResourceTypeGeneratedUsage : usageList) {
        Assert.assertEquals(generatedUsage[i], serviceResourceTypeGeneratedUsage.getUsageTypeName());
        i++;
      }
      Assert.assertEquals(tenant, map.get("tenant"));
      Assert.assertEquals(currencyValueService.locateBYCurrencyCode("JPY"), map.get("currency"));
      Product product = productdao.find(1L);

      Map<Object, Object> retMap = (Map<Object, Object>) map.get("retMap");
      Map<Object, Object> retVal = (Map<Object, Object>) retMap.get(instance.getService());
      Map<Product, ProductCharge> chargeMap = (Map<Product, ProductCharge>) retVal.get(instance);
      ProductCharge charge = chargeMap.get(product);
      Assert.assertEquals("JPY", charge.getCurrencyValue().getCurrencyCode());
      Assert.assertEquals("11.0000", charge.getPrice().toString());

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testGetTaxableAmount() {
    try {
      String amount = "100.00";
      String taxableamount = controller.getTaxableAmount(amount);
      Assert.assertEquals(taxableamount, "10.0000");
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testGetTaxableAmountNegative() {
    try {
      String amount = "-100.00";
      String taxableamount = controller.getTaxableAmount(amount);
      Assert.assertEquals(BigDecimal.ZERO.toString(), taxableamount);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testCreateSubscription() {
    try {
      Tenant tenant = tenantDAO.find(4L);
      String configurationData = "{\"hostName\":\"anusha-VM\",\"displayName\":\"\",\"group\":\"\"}";
      ServiceInstance instance = serviceInstanceDAO.find(1L);
      Subscription subscription = subscriptionDAO.find(1L);
      subscription.setConfigurationData(configurationData);
      subscriptionDAO.save(subscription);
      String[] frequencyDisplayNames = {
          "None", "Monthly", "Quarterly", "Annual"
      };

      request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
      request.setAttribute("isSurrogatedTenant", false);

      controller.createSubscription(tenant, tenant.getParam(), instance.getUuid(), subscription.getId().toString(),
          "VirtualMachine", map, request);

      Assert.assertEquals(true, map.get("userHasCloudServiceAccount"));
      Assert.assertEquals(tenant, map.get("tenant"));
      Assert.assertEquals(instance.getUuid(), map.get("selectedCloudServiceInstance"));
      Assert.assertEquals(instance.getService().getCategory(), map.get("selectedCategory"));
      Assert.assertEquals(instance.getService(), map.get("service"));
      Assert.assertEquals(instance.getUuid(), map.get("serviceInstanceUuid"));
      Assert.assertEquals(instance.getService().getServiceResourceTypes().size(),
          ((List<ServiceResourceType>) map.get("resourceTypes")).size());
      Assert.assertEquals("VirtualMachine", map.get("resourceType"));
      Assert.assertEquals("[IAAS]", map.get("serviceCategoryList").toString());
      Assert.assertEquals(subscription, map.get("subscription"));
      Assert.assertEquals(configurationData, map.get("configurationData").toString());
      Assert.assertEquals(tenant.getSourceChannel().getName(), map.get("sourceChannelName"));

      int i = 0;
      @SuppressWarnings("unchecked")
      List<ChargeRecurrenceFrequency> chargeRecurrenceFrequencyList = (List<ChargeRecurrenceFrequency>) map
          .get("chargeRecurrenceFrequencyList");
      for (ChargeRecurrenceFrequency frequency : chargeRecurrenceFrequencyList) {
        Assert.assertEquals(frequencyDisplayNames[i], frequency.getDisplayName());
        i++;
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testCreateSubscriptionWithNoId() {
    try {
      Tenant tenant = tenantDAO.find(4L);
      ServiceInstance instance = serviceInstanceDAO.find(1L);

      String[] frequencyDisplayNames = {
          "None", "Monthly", "Quarterly", "Annual"
      };

      request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
      request.setAttribute("isSurrogatedTenant", false);

      controller.createSubscription(tenant, tenant.getParam(), instance.getUuid(), "", "VirtualMachine", map, request);

      Assert.assertEquals(true, map.get("userHasCloudServiceAccount"));
      Assert.assertEquals(tenant, map.get("tenant"));
      Assert.assertEquals(instance.getUuid(), map.get("selectedCloudServiceInstance"));
      Assert.assertEquals(instance.getService().getCategory(), map.get("selectedCategory"));
      Assert.assertEquals(instance.getService(), map.get("service"));
      Assert.assertEquals(instance.getUuid(), map.get("serviceInstanceUuid"));
      Assert.assertEquals(instance.getService().getServiceResourceTypes().size(),
          ((List<ServiceResourceType>) map.get("resourceTypes")).size());
      Assert.assertEquals("VirtualMachine", map.get("resourceType"));
      Assert.assertEquals("[IAAS]", map.get("serviceCategoryList").toString());
      Assert.assertEquals(tenant.getSourceChannel().getName(), map.get("sourceChannelName"));

      int i = 0;
      @SuppressWarnings("unchecked")
      List<ChargeRecurrenceFrequency> chargeRecurrenceFrequencyList = (List<ChargeRecurrenceFrequency>) map
          .get("chargeRecurrenceFrequencyList");
      for (ChargeRecurrenceFrequency frequency : chargeRecurrenceFrequencyList) {
        Assert.assertEquals(frequencyDisplayNames[i], frequency.getDisplayName());
        i++;
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test(expected = ConnectorManagementServiceException.class)
  public void testCreateSubscriptionWithInvalidInstanceforTenant() throws ConnectorManagementServiceException {

    Tenant tenant = tenantDAO.find(4L);
    ServiceInstance instance = serviceInstanceDAO.find(2L);
    request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
    request.setAttribute("isSurrogatedTenant", false);

    controller.createSubscription(tenant, tenant.getParam(), instance.getUuid(), "", "VirtualMachine", map, request);

  }

  @Test
  public void testCreateSubscriptionWithNullServiceInstance() {
    try {
      Tenant tenant = tenantDAO.find(4L);
      ServiceInstance instance = serviceInstanceDAO.find(1L);

      String[] frequencyDisplayNames = {
          "None", "Monthly", "Quarterly", "Annual"
      };

      request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
      request.setAttribute("isSurrogatedTenant", false);

      controller.createSubscription(tenant, tenant.getParam(), null, "", "VirtualMachine", map, request);

      Assert.assertEquals(true, map.get("userHasCloudServiceAccount"));
      Assert.assertEquals(tenant, map.get("tenant"));
      Assert.assertEquals(instance.getService(), map.get("service"));
      Assert.assertEquals(instance.getUuid(), map.get("serviceInstanceUuid"));
      Assert.assertEquals(instance.getService().getServiceResourceTypes().size(),
          ((List<ServiceResourceType>) map.get("resourceTypes")).size());
      Assert.assertEquals("VirtualMachine", map.get("resourceType"));
      Assert.assertEquals("[IAAS]", map.get("serviceCategoryList").toString());
      Assert.assertEquals(tenant.getSourceChannel().getName(), map.get("sourceChannelName"));

      int i = 0;
      @SuppressWarnings("unchecked")
      List<ChargeRecurrenceFrequency> chargeRecurrenceFrequencyList = (List<ChargeRecurrenceFrequency>) map
          .get("chargeRecurrenceFrequencyList");
      for (ChargeRecurrenceFrequency frequency : chargeRecurrenceFrequencyList) {
        Assert.assertEquals(frequencyDisplayNames[i], frequency.getDisplayName());
        i++;
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testCreateSubscriptionWithNullServiceInstanceID() {
    try {
      Tenant tenant = tenantDAO.find(4L);
      ServiceInstance instance = serviceInstanceDAO.find(1L);
      String[] frequencyDisplayNames = {
          "None", "Monthly", "Quarterly", "Annual"
      };

      request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
      request.setAttribute("isSurrogatedTenant", false);

      controller.createSubscription(tenant, tenant.getParam(), null, "", "VirtualMachine", map, request);

      Assert.assertEquals(true, map.get("userHasCloudServiceAccount"));
      Assert.assertEquals(tenant, map.get("tenant"));
      Assert.assertEquals(instance.getService(), map.get("service"));
      Assert.assertEquals(instance.getUuid(), map.get("serviceInstanceUuid"));
      Assert.assertEquals(instance.getService().getServiceResourceTypes().size(),
          ((List<ServiceResourceType>) map.get("resourceTypes")).size());
      Assert.assertEquals("VirtualMachine", map.get("resourceType"));
      Assert.assertEquals("[IAAS]", map.get("serviceCategoryList").toString());
      Assert.assertEquals(tenant.getSourceChannel().getName(), map.get("sourceChannelName"));

      int i = 0;
      @SuppressWarnings("unchecked")
      List<ChargeRecurrenceFrequency> chargeRecurrenceFrequencyList = (List<ChargeRecurrenceFrequency>) map
          .get("chargeRecurrenceFrequencyList");
      for (ChargeRecurrenceFrequency frequency : chargeRecurrenceFrequencyList) {
        Assert.assertEquals(frequencyDisplayNames[i], frequency.getDisplayName());
        i++;
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testGetFilterComponents() {
    try {
      Tenant tenant = tenantDAO.find(4L);
      ServiceInstance instance = serviceInstanceDAO.find(1L);

      request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, getRootUser().getTenant());
      request.setAttribute("isSurrogatedTenant", true);

      List<FilterComponent> componentList = controller.getFilterComponents(tenant, tenant.getParam(),
          instance.getUuid(), "zone", true, request);
      for (FilterComponent component : componentList) {
        Assert.assertEquals(component.getName(), "zone");
        Assert.assertEquals(component.getValue(), "122");
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testgetFilterComponentsForBundle() {
    try {
      Tenant tenant = tenantDAO.find(4L);
      ServiceInstance instance = serviceInstanceDAO.find(1L);
      ProductBundle bundle = bundleservice.locateProductBundleById("2");

      request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
      request.setAttribute("isSurrogatedTenant", false);

      List<FilterComponent> componentList = controller.getFilterComponentsForBundle(tenant, tenant.getParam(),
          instance.getUuid(), "zone", bundle.getId(), request);
      for (FilterComponent component : componentList) {
        Assert.assertEquals(component.getName(), "zone");
        Assert.assertEquals(component.getValue(), "122");
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testGetResourceComponents() {
    try {
      Tenant tenant = tenantDAO.find(4L);
      ServiceInstance instance = serviceInstanceDAO.find(1L);
      List<ResourceComponent> resourceComponentList = controller.getResourceComponents(tenant, tenant.getParam(),
          instance.getUuid(), "VirtualMachine", "Template", "Template=1", true, "", request);
      for (ResourceComponent component : resourceComponentList) {
        Assert.assertEquals(component.getName(), "Template");
        Assert.assertEquals(component.getValue(), "1");
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testGetResourceComponentsForBundle() {
    try {
      Tenant tenant = tenantDAO.find(4L);
      ServiceInstance instance = serviceInstanceDAO.find(1L);
      ProductBundle bundle = bundleservice.locateProductBundleById("2");
      request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
      request.setAttribute("isSurrogatedTenant", false);

      List<ResourceComponent> resourceComponentList = controller.getResourceComponentsForBundle(tenant,
          tenant.getParam(), instance.getUuid(), "VirtualMachine", "PSI_C1", null, "Template=1", null, bundle.getId(),
          request);
      for (ResourceComponent component : resourceComponentList) {
        Assert.assertEquals(component.getName(), "Template");
        Assert.assertEquals(component.getValue(), "1");
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  // TODO ChannelParam wants channel ID! tenanParam and sub subscriptionid do
  @Test
  public void testPreviewCatalog() {
    try {
      Channel channel = channelDAO.find(4L);
      Tenant tenant = tenantDAO.find(4L);
      ServiceInstance instance = serviceInstanceDAO.find(1L);
      Subscription subscription = subscriptionDAO.find(1L);
      ChannelRevision revision = channelService.getCurrentChannelRevision(channel, false);
      String[] frequencyDisplayNames = {
          "None", "Monthly", "Quarterly", "Annual"
      };

      request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, getRootUser().getTenant());
      request.setAttribute("isSurrogatedTenant", true);

      controller.previewCatalog(channel.getId().toString(), map, tenant.getParam(), instance.getUuid(), subscription
          .getId().toString(), revision.getRevision().toString(), null, null, "JPY", "VirtualMachine", request);

      Assert.assertEquals(channel, map.get("channel"));
      Assert.assertEquals(channelService.listCurrencies(channel.getParam()), map.get("currencies"));
      Assert.assertEquals(true, map.get("viewChannelCatalog"));
      Assert.assertEquals(revision.getRevision().toString(), map.get("revision"));
      Assert.assertEquals(currencyValueService.locateBYCurrencyCode("JPY"), map.get("selectedCurrency"));
      Assert.assertEquals(getRootUser().getTenant(), map.get("tenant"));

      int i = 0;
      @SuppressWarnings("unchecked")
      List<ChargeRecurrenceFrequency> chargeRecurrenceFrequencyList = (List<ChargeRecurrenceFrequency>) map
          .get("chargeRecurrenceFrequencyList");
      for (ChargeRecurrenceFrequency frequency : chargeRecurrenceFrequencyList) {
        Assert.assertEquals(frequencyDisplayNames[i], frequency.getDisplayName());
        i++;
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testPreviewCatalogWithNullChannel() {
    try {
      Channel channel = channelDAO.find(1L);
      ServiceInstance instance = serviceInstanceDAO.find(1L);
      ChannelRevision revision = channelService.getCurrentChannelRevision(channel, false);
      String[] frequencyDisplayNames = {
          "None", "Monthly", "Quarterly", "Annual"
      };

      request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, getRootUser().getTenant());
      request.setAttribute("isSurrogatedTenant", true);

      controller.previewCatalog(null, map, null, instance.getUuid(), null, revision.getRevision().toString(), null,
          null, "JPY", "VirtualMachine", request);

      Assert.assertEquals(channel, map.get("channel"));
      Assert.assertEquals(channelService.listCurrencies(channel.getParam()), map.get("currencies"));
      Assert.assertEquals(true, map.get("viewChannelCatalog"));
      Assert.assertEquals(revision.getRevision().toString(), map.get("revision"));
      Assert.assertEquals(currencyValueService.locateBYCurrencyCode("JPY"), map.get("selectedCurrency"));
      Assert.assertEquals(getRootUser().getTenant(), map.get("tenant"));

      int i = 0;
      @SuppressWarnings("unchecked")
      List<ChargeRecurrenceFrequency> chargeRecurrenceFrequencyList = (List<ChargeRecurrenceFrequency>) map
          .get("chargeRecurrenceFrequencyList");
      for (ChargeRecurrenceFrequency frequency : chargeRecurrenceFrequencyList) {
        Assert.assertEquals(frequencyDisplayNames[i], frequency.getDisplayName());
        i++;
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testPreviewCatalogWithNullServiceInstance() {
    try {
      Channel channel = channelDAO.find(1L);
      ChannelRevision revision = channelService.getCurrentChannelRevision(channel, false);
      String[] frequencyDisplayNames = {
          "None", "Monthly", "Quarterly", "Annual"
      };

      request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, getRootUser().getTenant());
      request.setAttribute("isSurrogatedTenant", true);

      controller.previewCatalog(null, map, null, null, null, revision.getRevision().toString(), null, null, "JPY",
          "VirtualMachine", request);

      Assert.assertEquals(channel, map.get("channel"));
      Assert.assertEquals(channelService.listCurrencies(channel.getParam()), map.get("currencies"));
      Assert.assertEquals(true, map.get("viewChannelCatalog"));
      Assert.assertEquals(revision.getRevision().toString(), map.get("revision"));
      Assert.assertEquals(currencyValueService.locateBYCurrencyCode("JPY"), map.get("selectedCurrency"));
      Assert.assertEquals(getRootUser().getTenant(), map.get("tenant"));

      int i = 0;
      @SuppressWarnings("unchecked")
      List<ChargeRecurrenceFrequency> chargeRecurrenceFrequencyList = (List<ChargeRecurrenceFrequency>) map
          .get("chargeRecurrenceFrequencyList");
      for (ChargeRecurrenceFrequency frequency : chargeRecurrenceFrequencyList) {
        Assert.assertEquals(frequencyDisplayNames[i], frequency.getDisplayName());
        i++;
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testPreviewCatalogWithNullCurrency() {
    try {
      Channel channel = channelDAO.find(4L);
      Tenant tenant = tenantDAO.find(4L);
      ServiceInstance instance = serviceInstanceDAO.find(1L);
      Subscription subscription = subscriptionDAO.find(1L);
      ChannelRevision revision = channelService.getCurrentChannelRevision(channel, false);
      String[] frequencyDisplayNames = {
          "None", "Monthly", "Quarterly", "Annual"
      };

      request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, getRootUser().getTenant());
      request.setAttribute("isSurrogatedTenant", true);

      controller.previewCatalog(channel.getId().toString(), map, tenant.getParam(), instance.getUuid(), subscription
          .getId().toString(), revision.getRevision().toString(), null, null, null, "VirtualMachine", request);

      Assert.assertEquals(channel, map.get("channel"));
      Assert.assertEquals(channelService.listCurrencies(channel.getParam()), map.get("currencies"));
      Assert.assertEquals(currencyValueDAO.findByCurrencyCode("JPY"), map.get("selectedCurrency"));
      Assert.assertEquals(true, map.get("viewChannelCatalog"));
      Assert.assertEquals(revision.getRevision().toString(), map.get("revision"));
      Assert.assertEquals(currencyValueService.locateBYCurrencyCode("JPY"), map.get("selectedCurrency"));
      Assert.assertEquals(getRootUser().getTenant(), map.get("tenant"));

      int i = 0;
      @SuppressWarnings("unchecked")
      List<ChargeRecurrenceFrequency> chargeRecurrenceFrequencyList = (List<ChargeRecurrenceFrequency>) map
          .get("chargeRecurrenceFrequencyList");
      for (ChargeRecurrenceFrequency frequency : chargeRecurrenceFrequencyList) {
        Assert.assertEquals(frequencyDisplayNames[i], frequency.getDisplayName());
        i++;
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testGetUniqueResourceComponents() {
    try {
      String[] componentArray = {
          "SO", "Template", "hypervisor"
      };
      ServiceInstance instance = serviceInstanceDAO.find(1L);

      List<String> componentsList = controller.getUniqueResourceComponents(instance.getUuid(), "VirtualMachine");

      Assert.assertEquals(componentArray.length, componentsList.size());
      for (int i = 0; i < componentArray.length; i++) {
        Assert.assertTrue(componentsList.contains(componentArray[i]));
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testAnonymousCatalog() {
    try {
      Channel channel = channelDAO.find(4L);
      String[] frequencyDisplayNames = {
          "None", "Monthly", "Quarterly", "Annual"
      };
      ServiceInstance instance = serviceInstanceDAO.find(1L);

      request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, getRootUser().getTenant());
      request.setAttribute("isSurrogatedTenant", true);

      Configuration config = configurationService
          .locateConfigurationByName(Names.com_citrix_cpbm_public_catalog_display);
      config.setValue("true");
      configurationService.update(config);

      controller.anonymousCatalog(map, instance.getUuid(), "JPY", "VirtualMachine", channel.getCode(), request);

      Assert.assertEquals(channel, map.get("channel"));
      Assert.assertEquals(channelService.listCurrencies(channel.getParam()), map.get("currencies"));
      Assert.assertEquals(true, map.get("anonymousBrowsing"));
      Assert.assertEquals(currencyValueService.locateBYCurrencyCode("JPY"), map.get("selectedCurrency"));
      int i = 0;
      @SuppressWarnings("unchecked")
      List<ChargeRecurrenceFrequency> chargeRecurrenceFrequencyList = (List<ChargeRecurrenceFrequency>) map
          .get("chargeRecurrenceFrequencyList");
      for (ChargeRecurrenceFrequency frequency : chargeRecurrenceFrequencyList) {
        Assert.assertEquals(frequencyDisplayNames[i], frequency.getDisplayName());
        i++;
      }

      // For default channel
      channel = channelDAO.find(1L);
      controller.anonymousCatalog(map, instance.getUuid(), "JPY", "VirtualMachine", null, request);

      Assert.assertEquals(channel, map.get("channel"));
      Assert.assertEquals(channelService.listCurrencies(channel.getParam()), map.get("currencies"));
      Assert.assertEquals(true, map.get("anonymousBrowsing"));
      Assert.assertEquals(currencyValueService.locateBYCurrencyCode("JPY"), map.get("selectedCurrency"));

      Assert.assertTrue(map.containsKey(UserContextInterceptor.MIN_FRACTION_DIGITS));
      Assert.assertEquals(Currency.getInstance("JPY").getDefaultFractionDigits(), map.get(UserContextInterceptor.MIN_FRACTION_DIGITS));
      
      i = 0;
      @SuppressWarnings("unchecked")
      List<ChargeRecurrenceFrequency> frequencyList = (List<ChargeRecurrenceFrequency>) map
          .get("chargeRecurrenceFrequencyList");
      for (ChargeRecurrenceFrequency chargeFrequency : frequencyList) {
        Assert.assertEquals(frequencyDisplayNames[i], chargeFrequency.getDisplayName());
        i++;
      }
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testprovisionOrReconfigureSubscription() {
    try {
      Tenant tenant = tenantDAO.find(4L);
      String configurationData = "{\"hostName\":\"anusha-VM\",\"displayName\":\"\",\"group\":\"\"}";
      ServiceInstance instance = serviceInstanceDAO.find(1L);
      Subscription subscription = subscriptionDAO.find(1L);
      Subscription newSubscription = new Subscription();
      subscription.setConfigurationData(configurationData);

      request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
      request.setAttribute("isSurrogatedTenant", false);

      List<Subscription> subscriptionListBefore = subscriptionDAO.findByTenant(tenant, null, null);

      ProductBundle bundle = bundleservice.locateProductBundleById("2");
      SubscriptionForm form = new SubscriptionForm();
      newSubscription.setResourceType(instance.getService().getServiceResourceTypes().get(0));
      newSubscription.setConfigurationData(configurationData);
      newSubscription.setProductBundle(bundle);
      newSubscription.setCreatedBy(tenant.getOwner());
      newSubscription.setServiceInstance(instance);
      newSubscription.setTenant(tenant);
      newSubscription.setUpdatedBy(tenant.getOwner());
      newSubscription.setUser(tenant.getOwner());
      subscriptionDAO.save(newSubscription);
      com.citrix.cpbm.access.Subscription subscriptionObj = (com.citrix.cpbm.access.Subscription) CustomProxy
          .newInstance(newSubscription);

      form.setSubscription(subscriptionObj);
      BindingResult result = validate(form);

      asUser(tenant.getOwner());
      Map<String, String> responseMap = controller.provisionOrReconfigureSubscription(form, result, tenant.getParam(),
          bundle.getId().toString(), false, configurationData,
          "{\"zone_name\":\"Advanced-Zone\", \"TemplateId_name\":\"Dos\"}", instance
              .getUuid().toString(), "VirtualMachine", "zone=122", "TemplateId=56", subscription.getId().toString(),
          null, map, response, request);

      Assert.assertEquals(subscriptionListBefore.size() + 1, subscriptionDAO.findByTenant(tenant, null, null).size());
      Assert.assertEquals("RECONFIGURED", responseMap.get("subscriptionResultMessage"));

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testprovisionOrReconfigureSubscriptionwithNullBundle() {
    try {
      Tenant tenant = tenantDAO.find(4L);
      String configurationData = "{\"hostName\":\"anusha-VM\",\"displayName\":\"\",\"group\":\"\"}";
      ServiceInstance instance = serviceInstanceDAO.find(1L);
      Subscription newSubscription = new Subscription();

      request.setAttribute(UserContextInterceptor.EFFECTIVE_TENANT_KEY, tenant);
      request.setAttribute("isSurrogatedTenant", false);

      List<Subscription> subscriptionListBefore = subscriptionDAO.findByTenant(tenant, null, null);

      SubscriptionForm form = new SubscriptionForm();
      newSubscription.setResourceType(instance.getService().getServiceResourceTypes().get(0));
      newSubscription.setConfigurationData(configurationData);
      newSubscription.setProductBundle(null);
      newSubscription.setCreatedBy(tenant.getOwner());
      newSubscription.setServiceInstance(instance);
      newSubscription.setTenant(tenant);
      newSubscription.setUpdatedBy(tenant.getOwner());
      newSubscription.setUser(tenant.getOwner());

      com.citrix.cpbm.access.Subscription subscriptionObj = (com.citrix.cpbm.access.Subscription) CustomProxy
          .newInstance(newSubscription);

      form.setSubscription(subscriptionObj);
      BindingResult result = validate(form);

      asUser(tenant.getOwner());
      Map<String, String> responseMap = controller.provisionOrReconfigureSubscription(form, result, tenant.getParam(),
          null, false, configurationData, "{\"zone_name\":\"Advanced-Zone\", \"TemplateId_name\":\"Dos\"}",
          instance.getUuid().toString(), "VirtualMachine", "zone=122",
          "TemplateId=56", null, null, map, response, request);

      Assert.assertEquals("NEWLY_CREATED", responseMap.get("subscriptionResultMessage"));

      String subscriptionUuid = responseMap.get("subscriptionId");
      Subscription subscriptionObject = subscriptionService.get(subscriptionUuid);
      Assert.assertEquals(null, subscriptionObject.getProductBundle());
      Assert.assertEquals(subscriptionListBefore.size() + 1, subscriptionDAO.findByTenant(tenant, null, null).size());

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }
  
  @Test
  @ExpectedException(NoSuchDefinitionException.class)
  public void testAnonymousCatalogFail(){
    controller.anonymousCatalog(map, null, "JPY", null, null, request);
  }

}
