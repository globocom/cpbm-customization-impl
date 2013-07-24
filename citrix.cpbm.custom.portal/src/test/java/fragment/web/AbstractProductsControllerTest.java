/* Copyright (C) 2011 Cloud.com, Inc. All rights reserved. */
package fragment.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import web.WebTestsBase;
import citrix.cpbm.portal.fragment.controllers.ProductsController;

import com.vmops.model.Category;
import com.vmops.model.CurrencyValue;
import com.vmops.model.MediationRule;
import com.vmops.model.MediationRuleDiscriminator;
import com.vmops.model.Product;
import com.vmops.model.ProductCharge;
import com.vmops.model.Service;
import com.vmops.model.ServiceInstance;
import com.vmops.model.ServiceUsageType;
import com.vmops.model.ServiceUsageTypeDiscriminator;
import com.vmops.model.ServiceUsageTypeUom;
import com.vmops.model.User;
import com.vmops.persistence.EntitlementDAO;
import com.vmops.persistence.MediationRuleDAO;
import com.vmops.persistence.RevisionDAO;
import com.vmops.persistence.ServiceDAO;
import com.vmops.persistence.ServiceInstanceDao;
import com.vmops.service.ChannelService;
import com.vmops.service.ProductService;
import com.vmops.web.forms.ProductForm;
import com.vmops.web.forms.ProductLogoForm;

public class AbstractProductsControllerTest extends WebTestsBase {

  @Autowired
  ProductsController productsController;

  @Autowired
  ProductService productsService;

  @Autowired
  ChannelService channelService;

  @Autowired
  private EntitlementDAO entitlementDAO;

  @Autowired
  ServiceInstanceDao serviceInstanceDAO;

  @Autowired
  ServiceDAO serviceDAO;

  @Autowired
  RevisionDAO revisionDAO;

  @Autowired
  MediationRuleDAO mediationRuleDAO;

  private ModelMap map;

  private MockHttpServletResponse response;

  private MockHttpServletRequest request;

  @Before
  public void init() throws Exception {

    map = new ModelMap();
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  /*
   * Description: Test to create a product with the set of mediation rules passed and returns the newly created product
   * Author: Vinayv
   */
  private Product productCreation(String productCode, boolean hasCharges, Long serviceUsageTypeID,
      Long serviceInstanceId, String conversionFactor, boolean hasDiscriminator, String discriminator,
      String discriminatorDisplayName) throws Exception {

    Product product = new Product("New", "New_Prod", "New_Prod", "", getRootUser());
    product.setCode(productCode);
    product.setServiceInstance(serviceInstanceDAO.find(serviceInstanceId));
    product.setUom("Hours");

    Category category = productsService.getAllCategories().get(0);
    product.setCategory(category);

    ProductForm form = new ProductForm(product);
    form.setStartDate(new Date());
    form.setServiceUUID(serviceDAO.find(6000L).getUuid());
    form.setServiceInstanceUUID(serviceInstanceDAO.find(1L).getUuid());
    form.setCategoryID(category.getId().toString());
    form.setConversionFactor("1");

    List<ProductCharge> productCharges = new ArrayList<ProductCharge>();
    if (hasCharges) {
      ProductCharge productCharge = new ProductCharge();
      productCharge.setRevision(revisionDAO.getCurrentRevision(null));
      productCharge.setCatalog(null);
      productCharge.setCurrencyValue(currencyValueDAO.find(1L));
      productCharge.setPrice(BigDecimal.valueOf(100));
      productCharge.setCreatedAt(new Date());
      productCharge.setCreatedBy(getRootUser());
      productCharge.setUpdatedBy(getRootUser());
      productsService.saveProductCharge(productCharge);
      productCharges.add(productCharge);
    }

    form.setProductCharges(productCharges);
    form.setIsReplacementProduct(false);

    ServiceInstance serviceInstance = serviceInstanceDAO.find(serviceInstanceId);
    Service service = serviceInstance.getService();
    Set<ServiceUsageTypeDiscriminator> sutdSet = service.getServiceUsageTypeDiscriminator();
    Long discriminatorId = 1L;
    for (ServiceUsageTypeDiscriminator sutd : sutdSet) {
      if (sutd.getDiscriminatorName().equalsIgnoreCase(discriminator)) {
        discriminatorId = sutd.getId();
        break;
      }
    }
    String jsonString = "{conversionFactor: " + conversionFactor + ", operator: COMBINE, usageTypeId: "
        + serviceUsageTypeID;
    if (hasDiscriminator) {
      jsonString = jsonString + ", discriminatorVals: [ {discriminatorId: " + discriminatorId
          + ", discriminatorValue: 125, operator: EQUAL_TO, discriminatorValueName: " + discriminatorDisplayName
          + " } ]";
    }
    jsonString = jsonString + " }";
    org.json.JSONObject mediationJason = new org.json.JSONObject(jsonString);
    String productMediationRules = "[" + mediationJason.toString() + "]";
    form.setProductMediationRules(productMediationRules);

    BindingResult result = validate(form);
    Product obtainedProduct = productsController.createProduct(form, result, map, response, request);
    Assert.assertNotNull(obtainedProduct);
    Assert.assertEquals(obtainedProduct.getName(), product.getName());
    return obtainedProduct;
  }

  /*
   * Description: Test to create Discriminator under specific usage type Author: Vinay
   */
  private ServiceUsageType createDiscriminator(ServiceInstance serviceInstance, String usageType, String discriminator) {

    ServiceUsageType serviceUsageType = null;
    List<ServiceUsageType> sutlist = serviceInstance.getService().getServiceUsageTypes();
    for (ServiceUsageType sut : sutlist) {
      if (sut.getUsageTypeName().equalsIgnoreCase(usageType)) {
        serviceUsageType = sut;
        break;
      }
    }
    Service service = serviceInstance.getService();
    Set<ServiceUsageTypeDiscriminator> serviceUsageTypeDiscriminator = service.getServiceUsageTypeDiscriminator();
    ServiceUsageTypeDiscriminator e = new ServiceUsageTypeDiscriminator(discriminator, serviceUsageType);
    e.setTargetServiceId(service);
    serviceUsageTypeDiscriminator.add(e);
    serviceDAO.save(service);
    serviceDAO.flush();
    return serviceUsageType;
  }

  /*
   * Description: Test to create a product with Mediation Rule(Running_VM usage type) and
   * Discriminators(ServiceOfferingUUID, TemplateUUID, IsoUUID, HypervisorType, RamSize, Speed, GuestOSName) as
   * ProductManager Author: Vinayv
   */
  @Test
  public void testCreatProductWithRunningVM() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    String[] discriminators = {
        "ServiceOfferingUUID", "TemplateUUID", "IsoUUID", "HypervisorType", "RAMSize", "Speed", "GuestOSName"
    };
    for (int i = 0; i < discriminators.length; i++) {

      ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
      ServiceUsageType serviceUsageType = createDiscriminator(serviceInstance, "RUNNING_VM", discriminators[i]);
      int beforeProductsCount = productsService.getProductsCount();
      Product obtainedProduct = productCreation("Code" + i, true, serviceUsageType.getId(), 1L, "1.00", true,
          discriminators[i], "Test_Discri" + i);
      Assert.assertNotNull(obtainedProduct);
      Assert.assertEquals(obtainedProduct.getName(), "New_Prod");
      int afterProductCount = productsService.getProductsCount();
      Assert.assertEquals(beforeProductsCount + 1, afterProductCount);
      List<ProductCharge> obtainedProductCharges = productsService.getProductCharges(obtainedProduct,
          obtainedProduct.getCreatedAt());
      for (ProductCharge pc : obtainedProductCharges) {
        Assert.assertEquals(BigDecimal.valueOf(100), pc.getPrice());
        Assert.assertEquals(currencyValueDAO.find(1L), pc.getCurrencyValue());
      }
      Map<ServiceUsageType, List<Product>> productUsageMap = productsService.getProductsByUsageType(revisionDAO
          .getCurrentRevision(null));
      List<Product> prodlist = productUsageMap.get(serviceUsageType);
      Assert.assertEquals(true, prodlist.contains(obtainedProduct));
      List<MediationRule> mrlist = mediationRuleDAO.findAllMediationRules(serviceUsageType, serviceInstance,
          revisionDAO.getCurrentRevision(null));
      for (MediationRule mr : mrlist) {
        if (mr.getProduct().equals(obtainedProduct)) {
          Set<MediationRuleDiscriminator> mrdset = mr.getMediationRuleDiscriminators();
          for (MediationRuleDiscriminator mrd : mrdset) {
            Assert.assertEquals("Test_Discri" + i, mrd.getDiscriminatorValueDisplayName());
          }
        }
      }
    }
  }

  /*
   * Description: Test to create a product with Mediation Rule(Allocated VM usage type) and
   * Discriminators(ServiceOfferingUUID, TemplateUUID, IsoUUID, HypervisorType, RamSize, Speed, GuestOSName) as
   * ProductManager Author: Vinayv
   */
  @Test
  public void testCreatProductWithAllocatedVM() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    String[] discriminators = {
        "ServiceOfferingUUID", "TemplateUUID", "IsoUUID", "HypervisorType", "RAMSize", "Speed", "GuestOSName"
    };
    for (int i = 0; i < discriminators.length; i++) {

      ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
      ServiceUsageType serviceUsageType = createDiscriminator(serviceInstance, "ALLOCATED_VM", discriminators[i]);
      int beforeProductsCount = productsService.getProductsCount();
      Product obtainedProduct = productCreation("Code" + i, true, serviceUsageType.getId(), 1L, "1.00", true,
          discriminators[i], "Test_Discri" + i);
      Assert.assertNotNull(obtainedProduct);
      Assert.assertEquals(obtainedProduct.getName(), "New_Prod");
      int afterProductCount = productsService.getProductsCount();
      Assert.assertEquals(beforeProductsCount + 1, afterProductCount);
      List<ProductCharge> obtainedProductCharges = productsService.getProductCharges(obtainedProduct,
          obtainedProduct.getCreatedAt());
      for (ProductCharge pc : obtainedProductCharges) {
        Assert.assertEquals(BigDecimal.valueOf(100), pc.getPrice());
        Assert.assertEquals(currencyValueDAO.find(1L), pc.getCurrencyValue());
      }
      Map<ServiceUsageType, List<Product>> productUsageMap = productsService.getProductsByUsageType(revisionDAO
          .getCurrentRevision(null));
      List<Product> prodlist = productUsageMap.get(serviceUsageType);
      Assert.assertEquals(true, prodlist.contains(obtainedProduct));
      List<MediationRule> mrlist = mediationRuleDAO.findAllMediationRules(serviceUsageType, serviceInstance,
          revisionDAO.getCurrentRevision(null));
      for (MediationRule mr : mrlist) {
        if (mr.getProduct().equals(obtainedProduct)) {
          Set<MediationRuleDiscriminator> mrdset = mr.getMediationRuleDiscriminators();
          for (MediationRuleDiscriminator mrd : mrdset) {
            Assert.assertEquals("Test_Discri" + i, mrd.getDiscriminatorValueDisplayName());
          }
        }
      }
    }
  }

  /*
   * Description: Test to create a product with Mediation Rule(NETWORK_BYTES_SENT usage type) and
   * Discriminators(NetworkOfferingUUID, isRedundantRouterServiceEnabled, isVlanEnabled) as ProductManager Author:
   * Vinayv
   */
  @Test
  public void testCreatProductWithNetworkBytesSent() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    String[] discriminators = {
        "NetworkOfferingUUID", "isRedundantRouterServiceEnabled", "isVlanEnabled"
    };
    for (int i = 0; i < discriminators.length; i++) {

      ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
      ServiceUsageType serviceUsageType = createDiscriminator(serviceInstance, "NETWORK_BYTES_OUT", discriminators[i]);
      int beforeProductsCount = productsService.getProductsCount();
      Product obtainedProduct = productCreation("Code" + i, true, serviceUsageType.getId(), 1L, "1.00", true,
          discriminators[i], "Test_Discri" + i);
      Assert.assertNotNull(obtainedProduct);
      Assert.assertEquals(obtainedProduct.getName(), "New_Prod");
      int afterProductCount = productsService.getProductsCount();
      Assert.assertEquals(beforeProductsCount + 1, afterProductCount);
      List<ProductCharge> obtainedProductCharges = productsService.getProductCharges(obtainedProduct,
          obtainedProduct.getCreatedAt());
      for (ProductCharge pc : obtainedProductCharges) {
        Assert.assertEquals(BigDecimal.valueOf(100), pc.getPrice());
        Assert.assertEquals(currencyValueDAO.find(1L), pc.getCurrencyValue());
      }
      Map<ServiceUsageType, List<Product>> productUsageMap = productsService.getProductsByUsageType(revisionDAO
          .getCurrentRevision(null));
      List<Product> prodlist = productUsageMap.get(serviceUsageType);
      Assert.assertEquals(true, prodlist.contains(obtainedProduct));
      List<MediationRule> mrlist = mediationRuleDAO.findAllMediationRules(serviceUsageType, serviceInstance,
          revisionDAO.getCurrentRevision(null));
      for (MediationRule mr : mrlist) {
        if (mr.getProduct().equals(obtainedProduct)) {
          Set<MediationRuleDiscriminator> mrdset = mr.getMediationRuleDiscriminators();
          for (MediationRuleDiscriminator mrd : mrdset) {
            Assert.assertEquals("Test_Discri" + i, mrd.getDiscriminatorValueDisplayName());
          }
        }
      }
    }
  }

  /*
   * Description: Test to create a product with Mediation Rule(NETWORK_BYTES_RECEIVED usage type) and
   * Discriminators(NetworkOfferingUUID, isRedundantRouterServiceEnabled, isVlanEnabled) as ProductManager Author:
   * Vinayv
   */
  @Test
  public void testCreatProductWithNetworkBytesReceived() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    String[] discriminators = {
        "NetworkOfferingUUID", "isRedundantRouterServiceEnabled", "isVlanEnabled"
    };
    for (int i = 0; i < discriminators.length; i++) {

      ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
      ServiceUsageType serviceUsageType = createDiscriminator(serviceInstance, "NETWORK_BYTES_IN", discriminators[i]);
      int beforeProductsCount = productsService.getProductsCount();
      Product obtainedProduct = productCreation("Code" + i, true, serviceUsageType.getId(), 1L, "1.00", true,
          discriminators[i], "Test_Discri" + i);
      Assert.assertNotNull(obtainedProduct);
      Assert.assertEquals(obtainedProduct.getName(), "New_Prod");
      int afterProductCount = productsService.getProductsCount();
      Assert.assertEquals(beforeProductsCount + 1, afterProductCount);
      List<ProductCharge> obtainedProductCharges = productsService.getProductCharges(obtainedProduct,
          obtainedProduct.getCreatedAt());
      for (ProductCharge pc : obtainedProductCharges) {
        Assert.assertEquals(BigDecimal.valueOf(100), pc.getPrice());
        Assert.assertEquals(currencyValueDAO.find(1L), pc.getCurrencyValue());
      }
      Map<ServiceUsageType, List<Product>> productUsageMap = productsService.getProductsByUsageType(revisionDAO
          .getCurrentRevision(null));
      List<Product> prodlist = productUsageMap.get(serviceUsageType);
      Assert.assertEquals(true, prodlist.contains(obtainedProduct));
      List<MediationRule> mrlist = mediationRuleDAO.findAllMediationRules(serviceUsageType, serviceInstance,
          revisionDAO.getCurrentRevision(null));
      for (MediationRule mr : mrlist) {
        if (mr.getProduct().equals(obtainedProduct)) {
          Set<MediationRuleDiscriminator> mrdset = mr.getMediationRuleDiscriminators();
          for (MediationRuleDiscriminator mrd : mrdset) {
            Assert.assertEquals("Test_Discri" + i, mrd.getDiscriminatorValueDisplayName());
          }
        }
      }
    }
  }

  /*
   * Description: Test to create a product with Mediation Rule(VOLUME usage type) and Discriminators(diskOfferingUuid,
   * volumeType, poolType, size, volumeTags) as ProductManager Author: Vinayv
   */
  @Test
  public void testCreatProductWithVolume() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    String[] discriminators = {
        "diskOfferingUuid", "volumeType", "poolType", "size", "volumeTags"
    };
    for (int i = 0; i < discriminators.length; i++) {

      ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
      ServiceUsageType serviceUsageType = createDiscriminator(serviceInstance, "VOLUME", discriminators[i]);
      int beforeProductsCount = productsService.getProductsCount();
      Product obtainedProduct = productCreation("Code" + i, true, serviceUsageType.getId(), 1L, "1.00", true,
          discriminators[i], "Test_Discri" + i);
      Assert.assertNotNull(obtainedProduct);
      Assert.assertEquals(obtainedProduct.getName(), "New_Prod");
      int afterProductCount = productsService.getProductsCount();
      Assert.assertEquals(beforeProductsCount + 1, afterProductCount);
      List<ProductCharge> obtainedProductCharges = productsService.getProductCharges(obtainedProduct,
          obtainedProduct.getCreatedAt());
      for (ProductCharge pc : obtainedProductCharges) {
        Assert.assertEquals(BigDecimal.valueOf(100), pc.getPrice());
        Assert.assertEquals(currencyValueDAO.find(1L), pc.getCurrencyValue());
      }
      Map<ServiceUsageType, List<Product>> productUsageMap = productsService.getProductsByUsageType(revisionDAO
          .getCurrentRevision(null));
      List<Product> prodlist = productUsageMap.get(serviceUsageType);
      Assert.assertEquals(true, prodlist.contains(obtainedProduct));
      List<MediationRule> mrlist = mediationRuleDAO.findAllMediationRules(serviceUsageType, serviceInstance,
          revisionDAO.getCurrentRevision(null));
      for (MediationRule mr : mrlist) {
        if (mr.getProduct().equals(obtainedProduct)) {
          Set<MediationRuleDiscriminator> mrdset = mr.getMediationRuleDiscriminators();
          for (MediationRuleDiscriminator mrd : mrdset) {
            Assert.assertEquals("Test_Discri" + i, mrd.getDiscriminatorValueDisplayName());
          }
        }
      }
    }
  }

  /*
   * Description: Test to create a product with Mediation Rule(TEMPLATE usage type) and Discriminators(isPublic,
   * isFeatured, isSshKeyEnabled, isCrossZonesEnabled) as ProductManager Author: Vinayv
   */
  @Test
  public void testCreatProductWithTemplate() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    String[] discriminators = {
        "isPublic", "isFeatured", "isSshKeyEnabled", "isCrossZonesEnabled"
    };
    for (int i = 0; i < discriminators.length; i++) {

      ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
      ServiceUsageType serviceUsageType = createDiscriminator(serviceInstance, "TEMPLATE", discriminators[i]);
      int beforeProductsCount = productsService.getProductsCount();
      Product obtainedProduct = productCreation("Code" + i, true, serviceUsageType.getId(), 1L, "1.00", true,
          discriminators[i], "Test_Discri" + i);
      Assert.assertNotNull(obtainedProduct);
      Assert.assertEquals(obtainedProduct.getName(), "New_Prod");
      int afterProductCount = productsService.getProductsCount();
      Assert.assertEquals(beforeProductsCount + 1, afterProductCount);
      List<ProductCharge> obtainedProductCharges = productsService.getProductCharges(obtainedProduct,
          obtainedProduct.getCreatedAt());
      for (ProductCharge pc : obtainedProductCharges) {
        Assert.assertEquals(BigDecimal.valueOf(100), pc.getPrice());
        Assert.assertEquals(currencyValueDAO.find(1L), pc.getCurrencyValue());
      }
      Map<ServiceUsageType, List<Product>> productUsageMap = productsService.getProductsByUsageType(revisionDAO
          .getCurrentRevision(null));
      List<Product> prodlist = productUsageMap.get(serviceUsageType);
      Assert.assertEquals(true, prodlist.contains(obtainedProduct));
      List<MediationRule> mrlist = mediationRuleDAO.findAllMediationRules(serviceUsageType, serviceInstance,
          revisionDAO.getCurrentRevision(null));
      for (MediationRule mr : mrlist) {
        if (mr.getProduct().equals(obtainedProduct)) {
          Set<MediationRuleDiscriminator> mrdset = mr.getMediationRuleDiscriminators();
          for (MediationRuleDiscriminator mrd : mrdset) {
            Assert.assertEquals("Test_Discri" + i, mrd.getDiscriminatorValueDisplayName());
          }
        }
      }
    }
  }

  /*
   * Description: Test to create a product with Mediation Rule(SNAPSHOT usage type) and
   * Discriminator's(diskOfferingUuid, poolType, snapshotSize) as ProductManager Author: Vinayv
   */
  @Test
  public void testCreatProductWithSnapshot() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    String[] discriminators = {
        "diskOfferingUuid", "poolType", "snapshotSize"
    };
    ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
    Service service = serviceInstance.getService();
    ServiceUsageType sut = new ServiceUsageType(service, "SNAPSHOT",
        new ServiceUsageTypeUom("GB-Months", service, true));
    service.getServiceUsageTypes().add(sut);
    serviceDAO.save(service);
    serviceDAO.flush();
    for (int i = 0; i < discriminators.length; i++) {

      serviceInstance = serviceInstanceDAO.find("1");
      ServiceUsageType serviceUsageType = createDiscriminator(serviceInstance, "SNAPSHOT", discriminators[i]);
      int beforeProductsCount = productsService.getProductsCount();
      Product obtainedProduct = productCreation("Code" + i, true, serviceUsageType.getId(), 1L, "1.00", true,
          discriminators[i], "Test_Discri" + i);
      Assert.assertNotNull(obtainedProduct);
      Assert.assertEquals(obtainedProduct.getName(), "New_Prod");
      int afterProductCount = productsService.getProductsCount();
      Assert.assertEquals(beforeProductsCount + 1, afterProductCount);
      List<ProductCharge> obtainedProductCharges = productsService.getProductCharges(obtainedProduct,
          obtainedProduct.getCreatedAt());
      for (ProductCharge pc : obtainedProductCharges) {
        Assert.assertEquals(BigDecimal.valueOf(100), pc.getPrice());
        Assert.assertEquals(currencyValueDAO.find(1L), pc.getCurrencyValue());
      }
      Map<ServiceUsageType, List<Product>> productUsageMap = productsService.getProductsByUsageType(revisionDAO
          .getCurrentRevision(null));
      List<Product> prodlist = productUsageMap.get(serviceUsageType);
      Assert.assertEquals(true, prodlist.contains(obtainedProduct));
      List<MediationRule> mrlist = mediationRuleDAO.findAllMediationRules(serviceUsageType, serviceInstance,
          revisionDAO.getCurrentRevision(null));
      for (MediationRule mr : mrlist) {
        if (mr.getProduct().equals(obtainedProduct)) {
          Set<MediationRuleDiscriminator> mrdset = mr.getMediationRuleDiscriminators();
          for (MediationRuleDiscriminator mrd : mrdset) {
            Assert.assertEquals("Test_Discri" + i, mrd.getDiscriminatorValueDisplayName());
          }
        }
      }
    }
  }

  /*
   * Description: Test to create a product with Usage Types having no discriminator's (IP_ADDRESS, ISO, SECURITY_GROUP,
   * LOAD_BALANCER_POLICY, PORT_FORWARDING_RULE, VPN_USERS usage type having No discriminator's) as ProductManager
   * Author: Vinayv
   */
  @Test
  public void testCreatProductWithUsageTypesHavingNoDiscriminators() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    String[] usageTypes = {
        "IP_ADDRESS", "ISO", "SECURITY_GROUP", "LOAD_BALANCER_POLICY", "PORT_FORWARDING_RULE", "VPN_USERS"
    };
    for (int i = 0; i < usageTypes.length; i++) {

      ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
      Service service = serviceInstance.getService();
      ServiceUsageType serviceUsageType = new ServiceUsageType(service, usageTypes[i], new ServiceUsageTypeUom(
          "GB-Months", service, true));
      service.getServiceUsageTypes().add(serviceUsageType);
      serviceDAO.save(service);
      serviceDAO.flush();
      List<ServiceUsageType> sutlist = serviceInstance.getService().getServiceUsageTypes();
      for (ServiceUsageType sut : sutlist) {
        if (sut.getUsageTypeName().equalsIgnoreCase(usageTypes[i])) {
          serviceUsageType = sut;
          break;
        }
      }
      List<ServiceUsageTypeDiscriminator> sutdlist = serviceUsageType.getServiceUsageTypeDiscriminators();
      Assert.assertEquals(0, sutdlist.size());

      int beforeProductsCount = productsService.getProductsCount();
      Product obtainedProduct = productCreation("Code" + i, true, serviceUsageType.getId(), 1L, "1.00", false, "", "");
      Assert.assertNotNull(obtainedProduct);
      Assert.assertEquals(obtainedProduct.getName(), "New_Prod");
      int afterProductCount = productsService.getProductsCount();
      Assert.assertEquals(beforeProductsCount + 1, afterProductCount);
      List<ProductCharge> obtainedProductCharges = productsService.getProductCharges(obtainedProduct,
          obtainedProduct.getCreatedAt());
      for (ProductCharge pc : obtainedProductCharges) {
        Assert.assertEquals(BigDecimal.valueOf(100), pc.getPrice());
        Assert.assertEquals(currencyValueDAO.find(1L), pc.getCurrencyValue());
      }
      Map<ServiceUsageType, List<Product>> productUsageMap = productsService.getProductsByUsageType(revisionDAO
          .getCurrentRevision(null));
      List<Product> prodlist = productUsageMap.get(serviceUsageType);
      Assert.assertEquals(true, prodlist.contains(obtainedProduct));
      List<MediationRule> mrlist = mediationRuleDAO.findAllMediationRules(serviceUsageType, serviceInstance,
          revisionDAO.getCurrentRevision(null));
      for (MediationRule mr : mrlist) {
        if (mr.getProduct().equals(obtainedProduct)) {
          Set<MediationRuleDiscriminator> mrdset = mr.getMediationRuleDiscriminators();
          Assert.assertEquals(0, mrdset.size());
        }
      }
    }
  }

  /*
   * Description: Test to create a product with Multiple Usage Types as ProductManager Author: Vinayv
   */
  @Test
  public void testCreatProductWithMultipleUsageTypes() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    String[] usageTypes = {
        "VOLUME", "TEMPLATE", "ISO", "SECURITY_GROUP", "VPN_USERS"
    };
    ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
    Service service = serviceInstance.getService();
    ServiceUsageType serviceUsageType = null;
    for (int i = 0; i < usageTypes.length; i++) {

      serviceInstance = serviceInstanceDAO.find("1");
      service = serviceInstance.getService();
      serviceUsageType = new ServiceUsageType(service, usageTypes[i], new ServiceUsageTypeUom("GB-Months", service,
          true));
      service.getServiceUsageTypes().add(serviceUsageType);
      serviceDAO.save(service);
      serviceDAO.flush();
    }
    int beforeProductsCount = productsService.getProductsCount();
    Product product = new Product("New", "New_Prod", "New_Prod", "", getRootUser());
    product.setCode("New_Prod_Code");
    product.setServiceInstance(serviceInstanceDAO.find(serviceInstance.getId()));
    product.setUom("Hours");

    Category category = productsService.getCategory(1L);
    product.setCategory(category);

    ProductForm form = new ProductForm(product);
    form.setStartDate(new Date());
    form.setServiceUUID(service.getUuid());
    form.setServiceInstanceUUID(serviceInstanceDAO.find(1L).getUuid());
    form.setCategoryID(category.getId().toString());
    form.setConversionFactor("1");

    List<ProductCharge> productCharges = new ArrayList<ProductCharge>();
    ProductCharge productCharge = new ProductCharge();
    productCharge.setRevision(revisionDAO.getCurrentRevision(null));
    productCharge.setCatalog(null);
    productCharge.setCurrencyValue(currencyValueDAO.find(1L));
    productCharge.setPrice(BigDecimal.valueOf(100));
    productCharge.setCreatedAt(new Date());
    productCharge.setCreatedBy(getRootUser());
    productCharge.setUpdatedBy(getRootUser());
    productsService.saveProductCharge(productCharge);
    productCharges.add(productCharge);

    form.setProductCharges(productCharges);
    form.setIsReplacementProduct(false);

    String jsonString = "";
    String operator = "COMBINE";
    for (int i = 0; i < usageTypes.length; i++) {

      List<ServiceUsageType> sutlist = serviceInstance.getService().getServiceUsageTypes();
      for (ServiceUsageType sut : sutlist) {
        if (sut.getUsageTypeName().equalsIgnoreCase(usageTypes[i])) {
          serviceUsageType = sut;
          break;
        }
      }
      if (i == 2)
        operator = "EXCLUDE";
      else
        operator = "COMBINE";
      jsonString = "{conversionFactor: 1.00, operator: " + operator + ", usageTypeId: " + serviceUsageType.getId()
          + "}";
      jsonString = jsonString + ",";
    }
    jsonString = jsonString.substring(0, jsonString.length() - 1);
    String productMediationRules = "[" + jsonString + "]";
    form.setProductMediationRules(productMediationRules);
    BindingResult result = validate(form);
    Product obtainedProduct = productsController.createProduct(form, result, map, response, request);
    Assert.assertNotNull(obtainedProduct);
    Assert.assertEquals(obtainedProduct.getName(), product.getName());
    int afterProductCount = productsService.getProductsCount();
    Assert.assertEquals(beforeProductsCount + 1, afterProductCount);
    List<ProductCharge> obtainedProductCharges = productsService.getProductCharges(obtainedProduct,
        obtainedProduct.getCreatedAt());
    for (ProductCharge pc : obtainedProductCharges) {
      Assert.assertEquals(BigDecimal.valueOf(100), pc.getPrice());
      Assert.assertEquals(currencyValueDAO.find(1L), pc.getCurrencyValue());
    }
    Map<ServiceUsageType, List<Product>> productUsageMap = productsService.getProductsByUsageType(revisionDAO
        .getCurrentRevision(null));
    boolean cond = false;
    for (int i = 0; i < usageTypes.length; i++) {
      List<ServiceUsageType> sutlist = serviceInstance.getService().getServiceUsageTypes();
      for (ServiceUsageType sut : sutlist) {
        if (sut.getUsageTypeName().equalsIgnoreCase(usageTypes[i])) {
          serviceUsageType = sut;
          break;
        }
      }
      List<Product> prodlist = productUsageMap.get(serviceUsageType);
      if (prodlist != null && !prodlist.isEmpty() && prodlist.contains(obtainedProduct)) {
        cond = true;
      }
    }
    Assert.assertTrue(cond);
  }

  /*
   * Description: Test to create a product with Mediation Rule(Running_VM usage type) and
   * Discriminators(ServiceOfferingUUID, TemplateUUID, IsoUUID) as Root Author: Vinayv
   */
  @Test
  public void testCreatProductAsRoot() throws Exception {

    String[] discriminators = {
        "ServiceOfferingUUID", "TemplateUUID", "IsoUUID"
    };
    for (int i = 0; i < discriminators.length; i++) {

      ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
      ServiceUsageType serviceUsageType = createDiscriminator(serviceInstance, "RUNNING_VM", discriminators[i]);
      int beforeProductsCount = productsService.getProductsCount();
      Product obtainedProduct = productCreation("Code" + i, true, serviceUsageType.getId(), 1L, "1.00", true,
          discriminators[i], "Test_Discri" + i);
      Assert.assertNotNull(obtainedProduct);
      Assert.assertEquals(obtainedProduct.getName(), "New_Prod");
      int afterProductCount = productsService.getProductsCount();
      Assert.assertEquals(beforeProductsCount + 1, afterProductCount);
      List<ProductCharge> obtainedProductCharges = productsService.getProductCharges(obtainedProduct,
          obtainedProduct.getCreatedAt());
      for (ProductCharge pc : obtainedProductCharges) {
        Assert.assertEquals(BigDecimal.valueOf(100), pc.getPrice());
        Assert.assertEquals(currencyValueDAO.find(1L), pc.getCurrencyValue());
      }
      Map<ServiceUsageType, List<Product>> productUsageMap = productsService.getProductsByUsageType(revisionDAO
          .getCurrentRevision(null));
      List<Product> prodlist = productUsageMap.get(serviceUsageType);
      Assert.assertEquals(true, prodlist.contains(obtainedProduct));
      List<MediationRule> mrlist = mediationRuleDAO.findAllMediationRules(serviceUsageType, serviceInstance,
          revisionDAO.getCurrentRevision(null));
      for (MediationRule mr : mrlist) {
        if (mr.getProduct().equals(obtainedProduct)) {
          Set<MediationRuleDiscriminator> mrdset = mr.getMediationRuleDiscriminators();
          for (MediationRuleDiscriminator mrd : mrdset) {
            Assert.assertEquals("Test_Discri" + i, mrd.getDiscriminatorValueDisplayName());
          }
        }
      }
    }
  }

  /*
   * Description: Test to create a product with Product Name as Null Author: Vinayv
   */
  @Test
  public void testCreatProductWithNameAsNull() throws Exception {

    ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
    Service service = serviceInstance.getService();
    ServiceUsageType serviceUsageType = null;
    List<ServiceUsageType> sutlist = serviceInstance.getService().getServiceUsageTypes();
    for (ServiceUsageType sut : sutlist) {
      if (sut.getUsageTypeName().equalsIgnoreCase("RUNNING_VM")) {
        serviceUsageType = sut;
        break;
      }
    }
    Product product = new Product("New", null, "New_Prod", "", getRootUser());
    product.setCode("New_Prod_Code");
    product.setServiceInstance(serviceInstanceDAO.find(serviceInstance.getId()));
    product.setUom("Hours");
    Category category = productsService.getCategory(1L);
    product.setCategory(category);

    ProductForm form = new ProductForm(product);
    form.setStartDate(new Date());
    form.setServiceUUID(service.getUuid());
    form.setServiceInstanceUUID(serviceInstanceDAO.find(1L).getUuid());
    form.setCategoryID(category.getId().toString());

    List<ProductCharge> productCharges = new ArrayList<ProductCharge>();
    ProductCharge productCharge = new ProductCharge();
    productCharge.setRevision(revisionDAO.getCurrentRevision(null));
    productCharge.setCatalog(null);
    productCharge.setCurrencyValue(currencyValueDAO.find(1L));
    productCharge.setPrice(BigDecimal.valueOf(100));
    productCharge.setCreatedAt(new Date());
    productCharge.setCreatedBy(getRootUser());
    productCharge.setUpdatedBy(getRootUser());
    productsService.saveProductCharge(productCharge);
    productCharges.add(productCharge);

    form.setProductCharges(productCharges);
    form.setIsReplacementProduct(false);

    String jsonString = "[ {conversionFactor: 1.00, operator: COMBINE, usageTypeId: " + serviceUsageType.getId()
        + "} ]";
    form.setProductMediationRules(jsonString);
    BindingResult result = validate(form);
    try {
      productsController.createProduct(form, result, map, response, request);
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains(
          "not-null property references a null or transient value: com.vmops.model.Product.name"));
    }
  }

  /*
   * Description: Test to create a product with Product Code as Null. Author: Vinayv
   */
  @Test(expected = Exception.class)
  public void testCreatProductWithCodeAsNull() throws Exception {

    ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
    Service service = serviceInstance.getService();
    ServiceUsageType serviceUsageType = null;
    List<ServiceUsageType> sutlist = serviceInstance.getService().getServiceUsageTypes();
    for (ServiceUsageType sut : sutlist) {
      if (sut.getUsageTypeName().equalsIgnoreCase("RUNNING_VM")) {
        serviceUsageType = sut;
        break;
      }
    }
    Product product = new Product("New", "New_Prod", "New_Prod", "", getRootUser());
    product.setCode(null);
    product.setServiceInstance(serviceInstanceDAO.find(serviceInstance.getId()));
    product.setUom("Hours");

    ProductForm form = new ProductForm(product);
    form.setStartDate(new Date());
    form.setServiceUUID(service.getUuid());
    form.setServiceInstanceUUID(serviceInstanceDAO.find(1L).getUuid());

    List<ProductCharge> productCharges = new ArrayList<ProductCharge>();
    ProductCharge productCharge = new ProductCharge();
    productCharge.setRevision(revisionDAO.getCurrentRevision(null));
    productCharge.setCatalog(null);
    productCharge.setCurrencyValue(currencyValueDAO.find(1L));
    productCharge.setPrice(BigDecimal.valueOf(100));
    productCharge.setCreatedAt(new Date());
    productCharge.setCreatedBy(getRootUser());
    productCharge.setUpdatedBy(getRootUser());
    productsService.saveProductCharge(productCharge);
    productCharges.add(productCharge);

    form.setProductCharges(productCharges);
    form.setIsReplacementProduct(false);

    String jsonString = "[ {conversionFactor: 1.00, operator: COMBINE, usageTypeId: " + serviceUsageType.getId()
        + "} ]";
    form.setProductMediationRules(jsonString);
    BindingResult result = validate(form);
    productsController.createProduct(form, result, map, response, request);
  }

  /*
   * Description: Test to create a product with Product Code containing special characters. Author: Vinayv
   */
  @Test(expected = Exception.class)
  public void testCreatProductWithCodeHavingSplCharacters() throws Exception {

    ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
    Service service = serviceInstance.getService();
    ServiceUsageType serviceUsageType = null;
    List<ServiceUsageType> sutlist = serviceInstance.getService().getServiceUsageTypes();
    for (ServiceUsageType sut : sutlist) {
      if (sut.getUsageTypeName().equalsIgnoreCase("RUNNING_VM")) {
        serviceUsageType = sut;
        break;
      }
    }
    Product product = new Product("New", "New_Prod", "New_Prod", "", getRootUser());
    product.setCode("@Code<>");
    product.setServiceInstance(serviceInstanceDAO.find(serviceInstance.getId()));
    product.setUom("Hours");

    ProductForm form = new ProductForm(product);
    form.setStartDate(new Date());
    form.setServiceUUID(service.getUuid());
    form.setServiceInstanceUUID(serviceInstanceDAO.find(1L).getUuid());

    List<ProductCharge> productCharges = new ArrayList<ProductCharge>();
    ProductCharge productCharge = new ProductCharge();
    productCharge.setRevision(revisionDAO.getCurrentRevision(null));
    productCharge.setCatalog(null);
    productCharge.setCurrencyValue(currencyValueDAO.find(1L));
    productCharge.setPrice(BigDecimal.valueOf(100));
    productCharge.setCreatedAt(new Date());
    productCharge.setCreatedBy(getRootUser());
    productCharge.setUpdatedBy(getRootUser());
    productsService.saveProductCharge(productCharge);
    productCharges.add(productCharge);

    form.setProductCharges(productCharges);
    form.setIsReplacementProduct(false);

    String jsonString = "[ {conversionFactor: 1.00, operator: COMBINE, usageTypeId: " + serviceUsageType.getId()
        + "} ]";
    form.setProductMediationRules(jsonString);
    BindingResult result = validate(form);
    productsController.createProduct(form, result, map, response, request);
  }

  /*
   * Description: Test to create a product with Charges as Zero. Author: Vinayv
   */
  @Test
  public void testCreatProductWithChargesAsZero() throws Exception {

    ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
    Service service = serviceInstance.getService();
    ServiceUsageType serviceUsageType = null;
    List<ServiceUsageType> sutlist = serviceInstance.getService().getServiceUsageTypes();
    for (ServiceUsageType sut : sutlist) {
      if (sut.getUsageTypeName().equalsIgnoreCase("RUNNING_VM")) {
        serviceUsageType = sut;
        break;
      }
    }
    Product product = new Product("New", "New_Prod", "New_Prod", "", getRootUser());
    product.setCode("New_Prod_Code");
    product.setServiceInstance(serviceInstanceDAO.find(serviceInstance.getId()));
    product.setUom("Hours");
    Category category = productsService.getCategory(1L);
    product.setCategory(category);

    ProductForm form = new ProductForm(product);
    form.setStartDate(new Date());
    form.setServiceUUID(service.getUuid());
    form.setServiceInstanceUUID(serviceInstanceDAO.find(1L).getUuid());
    form.setCategoryID(category.getId().toString());
    form.setConversionFactor("1");

    List<ProductCharge> productCharges = new ArrayList<ProductCharge>();
    ProductCharge productCharge = new ProductCharge();
    productCharge.setRevision(revisionDAO.getCurrentRevision(null));
    productCharge.setCatalog(null);
    productCharge.setCurrencyValue(currencyValueDAO.find(1L));
    productCharge.setPrice(BigDecimal.valueOf(0.00));
    productCharge.setCreatedAt(new Date());
    productCharge.setCreatedBy(getRootUser());
    productCharge.setUpdatedBy(getRootUser());
    productsService.saveProductCharge(productCharge);
    productCharges.add(productCharge);

    form.setProductCharges(productCharges);
    form.setIsReplacementProduct(false);
    String jsonString = "[ {conversionFactor: 1.00, operator: COMBINE, usageTypeId: " + serviceUsageType.getId()
        + "} ]";
    form.setProductMediationRules(jsonString);
    BindingResult result = validate(form);
    Product obtainedProduct = productsController.createProduct(form, result, map, response, request);
    Assert.assertNotNull(obtainedProduct);
    List<ProductCharge> obtainedProductCharges = productsService.getProductCharges(obtainedProduct,
        obtainedProduct.getCreatedAt());
    for (ProductCharge pc : obtainedProductCharges) {
      Assert.assertEquals(BigDecimal.valueOf(0.00), pc.getPrice());
      Assert.assertEquals(currencyValueDAO.find(1L), pc.getCurrencyValue());
    }
  }

  /*
   * Description: Test to create a product without specifying Usage Type. Author: Vinayv
   */
  @Test
  public void testCreatProductWithoutUsageTypes() throws Exception {

    ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
    Service service = serviceInstance.getService();
    Product product = new Product("New", "New_Prod", "New_Prod", "", getRootUser());
    product.setCode("New_Prod_Code");
    product.setServiceInstance(serviceInstanceDAO.find(serviceInstance.getId()));
    product.setUom("Hours");
    Category category = productsService.getCategory(1L);
    product.setCategory(category);

    ProductForm form = new ProductForm(product);
    form.setStartDate(new Date());
    form.setServiceUUID(service.getUuid());
    form.setServiceInstanceUUID(serviceInstanceDAO.find(1L).getUuid());
    form.setCategoryID(category.getId().toString());
    form.setConversionFactor("1");

    List<ProductCharge> productCharges = new ArrayList<ProductCharge>();
    ProductCharge productCharge = new ProductCharge();
    productCharge.setRevision(revisionDAO.getCurrentRevision(null));
    productCharge.setCatalog(null);
    productCharge.setCurrencyValue(currencyValueDAO.find(1L));
    productCharge.setPrice(BigDecimal.valueOf(0.00));
    productCharge.setCreatedAt(new Date());
    productCharge.setCreatedBy(getRootUser());
    productCharge.setUpdatedBy(getRootUser());
    productsService.saveProductCharge(productCharge);
    productCharges.add(productCharge);
    form.setProductCharges(productCharges);
    form.setIsReplacementProduct(false);
    String jsonString = "[ { } ]";
    form.setProductMediationRules(jsonString);
    BindingResult result = validate(form);
    try {
      productsController.createProduct(form, result, map, response, request);
    } catch (Exception e) {
      Assert.assertEquals("JSONObject[\"usageTypeId\"] not found.", e.getMessage());
    }
  }

  /*
   * Description: Test to add an image to a product. Author: Vinayv
   */
  @Test
  public void testEditProductLogoValid() throws Exception {

    Product product = productDAO.find(1L);
    ProductLogoForm form = new ProductLogoForm(product);
    MultipartFile logo = new MockMultipartFile("Product.jpeg", "Product.jpeg", "bytes", "ProductLogo".getBytes());
    form.setLogo(logo);
    BindingResult result = validate(form);
    String resultString = productsController.editProductLogo(form, result, request, map);
    Assert.assertNotNull(resultString);
    Assert.assertTrue(resultString.contains("\"id\":" + product.getId() + ",\"name\":\"" + product.getName()
        + "\",\"code\":\"" + product.getCode() + "\""));
  }

  /*
   * Description: Test to edit Product Logo form passing Invalid Logo. Author: Vinayv
   */
  @Test
  public void testEditProductLogoInValid() throws Exception {

    Product product = productDAO.find(1L);
    ProductLogoForm form = new ProductLogoForm(product);
    MultipartFile logo = new MockMultipartFile("Product.jpe", "Product.jpe", "bytes", "ProductLogo".getBytes());
    form.setLogo(logo);
    BindingResult result = validate(form);
    String resultString = productsController.editProductLogo(form, result, request, map);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("File should have either .jpeg/.jpg/.png/.gif/.bmp extension", resultString);
  }

  /*
   * Description: Test to change Product's sort Order. Author: Vinayv
   */
  @Test
  public void testChangeProductSortOrder() throws Exception {

    Product product1 = productDAO.find(1L);
    Product product2 = productDAO.find(2L);
    Long beforeSortP1 = product1.getSortOrder();
    Long beforeSortP2 = product2.getSortOrder();
    String productOrderData = product2.getId() + "," + product1.getId();
    String result = productsController.editproductsorder(productOrderData, map);
    Assert.assertNotNull(result);
    Assert.assertEquals("success", result);
    Long afterSortP1 = product1.getSortOrder();
    Long afterSortP2 = product2.getSortOrder();
    Assert.assertEquals(beforeSortP1, afterSortP2);
    Assert.assertEquals(beforeSortP2, afterSortP1);
  }

  /*
   * Description: Test to create a product and edit charges Author: Vinayv
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testEditProductCharges() throws Exception {

    Product product = productsService.locateProductById("1");
    String viewResult = productsController.viewProductPlannedCharges(product.getCode(), map);
    Assert.assertNotNull(viewResult);
    Assert.assertEquals("view.product.planned.charges", viewResult);
    List<ProductCharge> productChargesOriginal = (List<ProductCharge>) map.get("productChargesList");
    List<ProductCharge> productChargesEdited = (List<ProductCharge>) map.get("productChargesList");
    List<CurrencyValue> currenciesList = new ArrayList<CurrencyValue>();
    for (ProductCharge pc : productChargesEdited) {
      // pc.setPrice(BigDecimal.valueOf(500.00));
      pc.setPrice(BigDecimal.valueOf(500.00).setScale(4, BigDecimal.ROUND_HALF_UP));
      currenciesList.add(pc.getCurrencyValue());
    }
    ProductForm form = new ProductForm(product);
    Map<Product, List<ProductCharge>> currentProductChargesMap = new HashMap<Product, List<ProductCharge>>();
    currentProductChargesMap.put(product, productChargesOriginal);
    form.setCurrentProductChargesMap(currentProductChargesMap);
    form.updateProductChargesFormList(product, productChargesEdited, currenciesList,
        channelService.getFutureRevision(null), false);
    BindingResult result = validate(form);
    String editResult = productsController.editPlannedCharges(form, result, map);
    Assert.assertNotNull(editResult);
    Assert.assertEquals("success", editResult);

    String viewResult1 = productsController.viewProductPlannedCharges(product.getCode(), map);
    Assert.assertNotNull(viewResult1);
    Assert.assertEquals("view.product.planned.charges", viewResult1);
    List<ProductCharge> productChargesafter = (List<ProductCharge>) map.get("productChargesList");
    for (ProductCharge pc : productChargesafter) {
      Assert.assertEquals(BigDecimal.valueOf(500.00).setScale(4, BigDecimal.ROUND_HALF_UP), pc.getPrice());
    }
  }

  /*
   * Description: Test to View Product Planned charges for future dates Author: Vinayv
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testViewProductPlannedChargesForFuture() throws Exception {

    Product product = productsService.locateProductById("1");
    List<ProductCharge> productChargesList = productsService.getProductCharges(product, channelService
        .getFutureRevision(null).getStartDate());
    String viewResult = productsController.viewProductPlannedCharges(product.getCode(), map);
    Assert.assertNotNull(viewResult);
    Assert.assertEquals("view.product.planned.charges", viewResult);
    List<ProductCharge> productCharges = (List<ProductCharge>) map.get("productChargesList");
    Assert.assertEquals(productChargesList.size(), productCharges.size());
  }

  /*
   * Description: Test to edit Plan Date to today's date for a product having current revision set Author: Vinayv
   */
  @Test
  public void testEditPlanDateToTodayForExistingProduct() throws Exception {

    Product product = productsService.locateProductById("1");
    ProductForm form = new ProductForm(product);
    form.setStartDate(new Date());
    BindingResult result = validate(form);
    String resultString = productsController.editPlanDate(form, result, map);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("false", resultString);
  }

  /*
   * Description: Test to edit Plan Date to Future date for a product having plan date in History Author: Vinayv
   */
  @Test
  public void testEditPlanDateToFutureForExistingProduct() throws Exception {

    Product product = productsService.locateProductById("1");
    ProductForm form = new ProductForm(product);
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 2);
    form.setStartDate(createdAt.getTime());
    BindingResult result = validate(form);
    String resultString = productsController.editPlanDate(form, result, map);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("success", resultString);
  }

  /*
   * Description: Test to edit Product with duplicate code Author: Vinayv
   * @Test public void testEditProduct() throws Exception{ ServiceInstance serviceInstance =
   * serviceInstanceDAO.find("1"); Service service = serviceInstance.getService(); ServiceUsageType serviceUsageType =
   * null; List<ServiceUsageType> sutlist = serviceInstance.getService().getServiceUsageTypes(); for(ServiceUsageType
   * sut : sutlist){ if(sut.getUsageTypeName().equalsIgnoreCase("RUNNING_VM")){ serviceUsageType = sut; break; } }
   * Product product = productsService.locateProductById("1");
   * product.setCode(productsService.locateProductById("2").getCode());
   * System.out.println("CODE1----------------"+product.getCode()); ProductForm form = new ProductForm(product); String
   * jsonString ="[ {conversionFactor: 1.00, operator: COMBINE, usageTypeId: "+serviceUsageType.getId()+"} ]";
   * form.setProductMediationRules(jsonString); BindingResult result = validate(form);
   * System.out.println("CODE----------------"+form.getProduct().getCode()); Product editedProduct =
   * productsController.editProduct(form, result, map); }
   */

  /*
   * Description: Test to search Product Author: Vinayv
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testSearchProduct() throws Exception {

    ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
    String productName = productsService.locateProductById("1").getName();
    String result = productsController.searchListByName(serviceInstance.getUuid(), "1", "100", productName, "current",
        new Date().toString(), "All", "All", map);
    Assert.assertNotNull(result);
    Assert.assertEquals("products.search.list", result);
    List<Product> obtainedProductList = (List<Product>) map.get("productsList");
    boolean flag = false;
    for (Product p : obtainedProductList) {
      if ((p.getName().equalsIgnoreCase(productName)) && (p.getId() == 1L)) {
        flag = true;
        break;
      }
    }
    Assert.assertEquals(true, flag);

    productName = productsService.locateProductById("1").getName();
    result = productsController.searchListByName(serviceInstance.getUuid(), "1", "100", productName.substring(0, 3),
        "current", new Date().toString(), "All", "All", map);
    Assert.assertNotNull(result);
    Assert.assertEquals("products.search.list", result);
    obtainedProductList = (List<Product>) map.get("productsList");
    flag = false;
    for (Product p : obtainedProductList) {
      if ((p.getName().equalsIgnoreCase(productName)) && (p.getId() == 1L)) {
        flag = true;
        break;
      }
    }
    Assert.assertEquals(true, flag);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "All" link and select Category is "All" for view current tab
   */
  @Test
  public void testAllFilterWithAllCategoryInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "All", "All", map);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "All" link and select Category value for view current tab
   */

  @Test
  public void testAllFilterWithSelectedCategoryInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "All", "Categorized1", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(2, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "All" link and select Category is "Null" for view current tab
   */

  @Test
  public void testAllFilterWithNullCategoryInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "All", null, map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "All" link and select Category is "Empty" for view current tab
   */

  @Test
  public void testAllFilterWithEmptyCategoryInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "All", "", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "All" link and select Category is Invalid for view current tab
   */

  @Test
  public void testAllFilterWithInvalidCategoryInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "All", "Invalid", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter Null link and select Category is Null for view current tab
   */

  @Test
  public void testNullFilterWithInvalidCategoryInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, null, null, map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "All" link and select Category is "All" for view History tab
   */

  @Test
  public void testAllFilterWithAllCategoryInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "All", "All", map);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "All" link and select Category value for view History tab
   */

  @Test
  public void testAllFilterWithSelectedCategoryInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "All", "Categorized1", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(2, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "All" link and select Category is "Null" for view History tab
   */

  @Test
  public void testAllFilterWithNullCategoryInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "All", null, map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "All" link and select Category is "Empty" for view History tab
   */

  @Test
  public void testAllFilterWithEmptyCategoryInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "All", "", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "All" link and select Category is Invalid for view History tab
   */

  @Test
  public void testAllFilterWithInvalidCategoryInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "All", "Invalid", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "All" link and select Category is Invalid for view History tab
   */

  @Test
  public void testNullFilterWithInvalidCategoryInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, null, null, map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "All" link and select Category is "All" for view planned tab
   */

  @Test
  public void testAllFilterWithAllCategoryInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "All", "All", map);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "All" link and select Category value for view planned tab
   */

  @Test
  public void testAllFilterWithSelectedCategoryInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "All", "Categorized1", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(2, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "All" link and select Category is "Null" for view planned tab
   */

  @Test
  public void testAllFilterWithNullCategoryInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "All", null, map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "All" link and select Category is "Empty" for view planned tab
   */

  @Test
  public void testAllFilterWithEmptyCategoryInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "All", "", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "All" link and select Category is Invalid for view planned tab
   */

  @Test
  public void testAllFilterWithInvalidCategoryInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "All", "Invalid", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Null" link and select Category is Invalid for view planned tab
   */

  @Test
  public void testNullFilterWithInvalidCategoryInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, null, null, map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Active" link and select Category is "All" for view current tab
   */

  @Test
  public void testActiveFilterWithAllCategoryInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "Active", "All", map);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Active" link and select Category value for view current tab
   */

  @Test
  public void testActiveFilterWithSelectedCategoryInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "Active", "Categorized1", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(2, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Active" link and select Category is "Null" for view current tab
   */

  @Test
  public void testActiveFilterWithNullCategoryInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "Active", null, map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Active" link and select Category is "Empty" for view current tab
   */

  @Test
  public void testActiveFilterWithEmptyCategoryInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "Active", "", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Active" link and select Category is Invalid for view current tab
   */

  @Test
  public void testActiveFilterWithInvalidCategoryInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "Active", "Invalid", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Active" link and select Category is "All" for view history tab
   */

  @Test
  public void testActiveFilterWithAllCategoryInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "Active", "All", map);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Active" link and select Category value for view history tab
   */

  @Test
  public void testActiveFilterWithSelectedCategoryInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "Active", "Categorized1", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(2, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Active" link and select Category is "Null" for view history tab
   */

  @Test
  public void testActiveFilterWithNullCategoryInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "Active", null, map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Active" link and select Category is "Empty" for view history tab
   */

  @Test
  public void testActiveFilterWithEmptyCategoryInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "Active", "", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Active" link and select Category is Invalid for view history tab
   */

  @Test
  public void testActiveFilterWithInvalidCategoryInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "Active", "Invalid", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Active" link and select Category is "All" for view planned tab
   */

  @Test
  public void testActiveFilterWithAllCategoryInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "Active", "All", map);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Active" link and select Category value for view planned tab
   */

  @Test
  public void testActiveFilterWithSelectedCategoryInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "Active", "Categorized1", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(2, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Active" link and select Category is "Null" for view planned tab
   */

  @Test
  public void testActiveFilterWithNullCategoryInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "Active", null, map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Active" link and select Category is "Empty" for view planned tab
   */

  @Test
  public void testActiveFilterWithEmptyCategoryInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "Active", "", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(35, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Active" link and select Category is Invalid for view planned tab
   */

  @Test
  public void testActiveFilterWithInvalidCategoryInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "Active", "Invalid", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Retire" link and select Category is "All" for view current tab
   */

  @Test
  public void testRetireFilterWithAllCategoryInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "Retire", "All", map);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Retire" link and select Category value for view current tab but product is
   * not Retire
   */

  @Test
  public void testRetireFilterWithSelectedCategoryInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "Retire", "Categorized1", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Retire" link and select Category is "Null" for view current tab
   */

  @Test
  public void testRetireFilterWithNullCategoryInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "Retire", null, map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Retire" link and select Category is "Empty" for view current tab
   */

  @Test
  public void testRetireFilterWithEmptyCategoryInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "Retire", "", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Retire" link and select Category is Invalid for view current tab
   */

  @Test
  public void testRetireFilterWithInvalidCategoryInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "Retire", "Invalid", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Retire" link and select Category is "All" for view history tab
   */

  @Test
  public void testRetireFilterWithAllCategoryInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "Retire", "All", map);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Retire" link and select Category value for view history tab but product is
   * not Retire
   */

  @Test
  public void testRetireFilterWithSelectedCategoryInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "Retire", "Categorized1", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Retire" link and select Category is "Null" for view history tab
   */

  @Test
  public void testRetireFilterWithNullCategoryInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "Retire", null, map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Retire" link and select Category is "Empty" for view history tab
   */

  @Test
  public void testRetireFilterWithEmptyCategoryInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "Retire", "", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Retire" link and select Category is Invalid for view history tab
   */

  @Test
  public void testRetireFilterWithInvalidCategoryInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "Retire", "Invalid", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Retire" link and select Category is "All" for view planned tab
   */

  @Test
  public void testRetireFilterWithAllCategoryInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "Retire", "All", map);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Retire" link and select Category value for view planned tab but product is
   * not Retire
   */

  @Test
  public void testRetireFilterWithSelectedCategoryInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "Retire", "Categorized1", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Retire" link and select Category is "Null" for view planned tab
   */

  @Test
  public void testRetireFilterWithNullCategoryInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "Retire", null, map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Retire" link and select Category is "Empty" for view planned tab
   */

  @Test
  public void testRetireFilterWithEmptyCategoryInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "Retire", "", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products filter "Retire" link and select Category is Invalid for view planned tab
   */

  @Test
  public void testRetireFilterWithInvalidCategoryInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "Retire", "Invalid", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products is Retired filter "Retire" link and select Category is All for view current tab
   */

  @Test
  public void testRetiredProductRetireFilterWithAllCategoryInCurrentViewForProducts() {

    String product = productsController.retireProduct("11", "false", map);
    Assert.assertNotNull(product);
    Assert.assertEquals("success", product);
    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "Retire", "All", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(1, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products is Retired filter "Retire" link and select Category is All for view History tab
   */

  @Test
  public void testRetiredProductRetireFilterWithAllCategoryInHistoryViewForProducts() {

    String product = productsController.retireProduct("11", "false", map);
    Assert.assertNotNull(product);
    Assert.assertEquals("success", product);
    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "Retire", "All", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(1, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products is Retired filter "Retire" link and select Category is All for view planned tab
   */

  @Test
  public void testRetiredProductRetireFilterWithAllCategoryInPlannedViewForProducts() {

    String product = productsController.retireProduct("11", "false", map);
    Assert.assertNotNull(product);
    Assert.assertEquals("success", product);
    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "Retire", "All", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products is Retired filter "Retire" link and select Category is All for view current tab
   */

  @Test
  public void testRetiredProductRetireFilterWithSelectedCategoryInCurrentViewForProducts() {

    String product = productsController.retireProduct("11", "false", map);
    Assert.assertNotNull(product);
    Assert.assertEquals("success", product);
    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "current", null, "Retire", "Categorized4", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(1, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products is Retired filter "Retire" link and select Category is All for view History tab
   */

  @Test
  public void testRetiredProductRetireFilterWithSelectedCategoryInHistoryViewForProducts() {

    String product = productsController.retireProduct("11", "false", map);
    Assert.assertNotNull(product);
    Assert.assertEquals("success", product);
    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "history", null, "Retire", "Categorized4", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(1, count);
  }

  /*
   * @author Abhaik
   * @description : Verify Products is Retired filter "Retire" link and select Category is All for view planned tab
   */

  @Test
  public void testRetiredProductRetireFilterWithSelectedCategoryInPlannedViewForProducts() {

    String product = productsController.retireProduct("11", "false", map);
    Assert.assertNotNull(product);
    Assert.assertEquals("success", product);
    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
          null, null, "planned", null, "Retire", "Categorized4", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("products.list", result);
      List<Product> productList = (List<Product>) map1.get("productsList");
      count = count + productList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  @Test
  public void testProductOrderBasedOnSameCategory() {
    int page = 1;
    ModelMap map1 = new ModelMap();
    String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
        null, null, "current", null, "All", "Categorized4", map1);
    Assert.assertNotNull(result);
    Assert.assertNotNull(result);
    Assert.assertEquals("products.list", result);
    List<Product> productList = (List<Product>) map1.get("productsList");
    Assert.assertEquals(2, productList.size());
    long diff = productList.get(1).getSortOrder() - productList.get(0).getSortOrder();
    Assert.assertTrue(diff > 0);
  }

  @Test
  public void testProductOrderBasedOnDifferentCategory() {
    int page = 3;
    ModelMap map1 = new ModelMap();
    String result = productsController.listProdcuts("003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page),
        null, null, "current", null, "All", "All", map1);
    Assert.assertNotNull(result);
    Assert.assertNotNull(result);
    Assert.assertEquals("products.list", result);
    List<Product> productList = (List<Product>) map1.get("productsList");
    Assert.assertEquals(7, productList.size());
    Assert.assertTrue(productList.get(0).getSortOrder() < productList.get(1).getSortOrder());
    Assert.assertTrue(productList.get(3).getSortOrder() < productList.get(4).getSortOrder());
  }
}
