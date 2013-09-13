/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
package fragment.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;

import web.WebTestsBase;

import com.citrix.cpbm.portal.fragment.controllers.ProductBundlesController;
import com.citrix.cpbm.portal.fragment.controllers.SubscriptionController;
import com.vmops.internal.service.SubscriptionService;
import com.vmops.model.Catalog;
import com.vmops.model.CatalogProductBundle;
import com.vmops.model.Channel;
import com.vmops.model.Channel.ChannelType;
import com.vmops.model.Entitlement;
import com.vmops.model.Product;
import com.vmops.model.ProductBundle;
import com.vmops.model.ProductBundleRevision;
import com.vmops.model.ProvisioningConstraint;
import com.vmops.model.RateCard;
import com.vmops.model.RateCardCharge;
import com.vmops.model.RateCardComponent;
import com.vmops.model.Revision;
import com.vmops.model.ServiceInstance;
import com.vmops.model.ServiceResourceType;
import com.vmops.model.ServiceResourceType.ResourceConstraint;
import com.vmops.model.User;
import com.vmops.persistence.ChargeRecurrenceFrequencyDAO;
import com.vmops.persistence.RateCardComponentDAO;
import com.vmops.persistence.RateCardDAO;
import com.vmops.persistence.RevisionDAO;
import com.vmops.persistence.ServiceInstanceDao;
import com.vmops.persistence.ServiceResourceTypeDAO;
import com.vmops.persistence.TenantDAO;
import com.vmops.service.ChannelService;
import com.vmops.service.ProductBundleService;
import com.vmops.service.ProductService;
import com.vmops.service.exceptions.AjaxFormValidationException;
import com.vmops.utils.DateUtils;
import com.vmops.web.forms.EntitlementForm;
import com.vmops.web.forms.ProductBundleForm;
import com.vmops.web.forms.RateCardChargesForm;
import com.vmops.web.forms.RateCardComponentChargesForm;
import com.vmops.web.forms.RateCardForm;

public class AbstractProductBundlesControllerTest extends WebTestsBase {

  @Autowired
  ProductBundlesController bundleController;

  @Autowired
  ServiceResourceTypeDAO serviceResourceTypeDAO;

  @Autowired
  ServiceInstanceDao serviceInstanceDAO;

  @Autowired
  ProductBundleService bundleService;

  @Autowired
  SubscriptionService subscriptionService;

  @Autowired
  ChargeRecurrenceFrequencyDAO chargeRecurrenceFrequencyDAO;

  @Autowired
  ChannelService channelService;

  @Autowired
  SubscriptionController subscriptionController;

  @Autowired
  ProductService productService;

  @Autowired
  RateCardComponentDAO rateCardComponentDAO;

  @Autowired
  RateCardDAO rateCardDAO;

  @Autowired
  RevisionDAO revisionDAO;

  @Autowired
  TenantDAO tenantDAO;

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
   * Description: Private Test to create Bundles based on the parameters Author: Vinayv
   */
  private ProductBundle testCreateProductBundle(String serviceInstanceID, String resourceTypeID, String chargeType,
      String BundleName, String currencyCode, BigDecimal currencyValue, Date startDate, String jsonString,
      ResourceConstraint businessConstraint, boolean trialEligible) throws Exception {

    ServiceInstance serviceInstance = serviceInstanceDAO.find(serviceInstanceID);
    ServiceResourceType resourceType = null;
    if (!resourceTypeID.equalsIgnoreCase("ServiceBundle"))
      resourceType = serviceResourceTypeDAO.find(resourceTypeID);
    List<RateCardCharge> rateCardChargeList = new ArrayList<RateCardCharge>();
    RateCardCharge rcc = new RateCardCharge(currencyValueService.locateBYCurrencyCode(currencyCode),
        catalogDAO.find(1L), currencyValue, "RateCharge", getRootUser(), getRootUser(),
        channelService.getCurrentRevision(null));
    rateCardChargeList.add(rcc);
    String chargeTypeName = chargeType;
    if (chargeType.equalsIgnoreCase("Invalid"))
      chargeTypeName = "NONE";
    RateCard rateCard = new RateCard("Rate", chargeRecurrenceFrequencyDAO.findByName(chargeTypeName), new Date(),
        getRootUser(), getRootUser());
    String compAssociationJson = jsonString;

    ProductBundle bundle = new ProductBundle(BundleName, BundleName, "", startDate, startDate, getRootUser());
    bundle.setBusinessConstraint(businessConstraint);
    bundle.setCode(BundleName);
    bundle.setPublish(true);
    if (!resourceTypeID.equalsIgnoreCase("ServiceBundle"))
      bundle.setResourceType(resourceType);
    bundle.setServiceInstanceId(serviceInstance);
    bundle.setTrialEligibility(trialEligible);
    bundle.setRateCard(rateCard);

    ProductBundleForm form = new ProductBundleForm(bundle);
    form.setChargeType(chargeType);
    if (!resourceTypeID.equalsIgnoreCase("ServiceBundle"))
      form.setResourceType(resourceType.getId().toString());
    else
      form.setResourceType("sb");
    form.setServiceInstanceUUID(serviceInstance.getUuid());
    form.setBundleOneTimeCharges(rateCardChargeList);
    if (!chargeType.equalsIgnoreCase("NONE"))
      form.setBundleRecurringCharges(rateCardChargeList);
    form.setCompAssociationJson(compAssociationJson);

    BindingResult result = validate(form);
    ProductBundle obtainedBundle = bundleController.createProductBundle(form, result, map, response);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals(bundle.getName(), obtainedBundle.getName());

    return obtainedBundle;
  }

  /*
   * Description: Test to create Service Bundles with None, Monthly, Quarterly and Annual charge frequency Author:
   * Vinayv
   */
  @Test
  public void testCreateServiceBundleForDifferentChargeFrequency() throws Exception {

    String[] chargeFrequency = {
        "NONE", "MONTHLY", "QUARTERLY", "ANNUAL"
    };
    for (int i = 0; i < chargeFrequency.length; i++) {

      String compAssociationJson = "[{association: INCLUDES, compName: ISO, compValues:[ {compName: DOS-ISO, compValueName: 10 } ] }]";
      int noOfdays = 3;
      Calendar createdAt = Calendar.getInstance();
      createdAt.add(Calendar.DATE, 0 - noOfdays);
      int beforeBundleCount = bundleService.getBundlesCount();
      ProductBundle obtainedBundle = testCreateProductBundle("1", "ServiceBundle", chargeFrequency[i],
          chargeFrequency[i] + "Service", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
          ResourceConstraint.PER_USER, true);
      Assert.assertNotNull(obtainedBundle);
      Assert.assertEquals(chargeFrequency[i] + "Service", obtainedBundle.getName());
      Assert.assertEquals(null, obtainedBundle.getResourceType());
      Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
      int afterBundleCount = bundleService.getBundlesCount();
      Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
    }
  }

  /*
   * Description: Test to create Compute Bundles with None, Monthly, Quarterly and Annual charge frequency Author:
   * Vinayv
   */
  @Test
  public void testCreateComputeBundleForDifferentChargeFrequency() throws Exception {

    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    String[] chargeFrequency = {
        "NONE", "MONTHLY", "QUARTERLY", "ANNUAL"
    };
    for (int i = 0; i < chargeFrequency.length; i++) {

      String compAssociationJson = "[{association: INCLUDES, compName: ISO, compValues:[ {compName: DOS-ISO, compValueName: 10 } ] }]";
      int noOfdays = 3;
      Calendar createdAt = Calendar.getInstance();
      createdAt.add(Calendar.DATE, 0 - noOfdays);
      int beforeBundleCount = bundleService.getBundlesCount();
      boolean trialEligible = false;
      if (i == 1)
        trialEligible = true;
      ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), chargeFrequency[i],
          chargeFrequency[i] + "Compute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
          ResourceConstraint.PER_USER, trialEligible);
      Assert.assertNotNull(obtainedBundle);
      Assert.assertEquals(chargeFrequency[i] + "Compute", obtainedBundle.getName());
      Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
      Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
      int afterBundleCount = bundleService.getBundlesCount();
      Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
    }
  }

  /*
   * Description: Test to create Compute Bundles with Invalid charge frequency(other than None, Monthly, Quarterly and
   * Annual) Author: Vinayv
   */
  @Test(expected = NullPointerException.class)
  public void testCreateComputeBundleWithInvalidChargeFrequency() throws Exception {

    String compAssociationJson = "[{association: INCLUDES, compName: ISO, compValues:[ {compName: DOS-ISO, compValueName: 10 } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 - noOfdays);
    testCreateProductBundle("1", "1", "Invalid", "InvalidCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(),
        compAssociationJson, ResourceConstraint.PER_USER, true);
  }

  /*
   * Description: Test to create Compute Bundles with Null charge frequency Author: Vinayv
   */
  @Test(expected = NullPointerException.class)
  public void testCreateComputeBundleWithNullChargeFrequency() throws Exception {

    ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    List<RateCardCharge> rateCardChargeList = new ArrayList<RateCardCharge>();
    RateCardCharge rcc = new RateCardCharge(currencyValueService.locateBYCurrencyCode("USD"), catalogDAO.find(1L),
        BigDecimal.TEN, "RateCharge", getRootUser(), getRootUser(), channelService.getCurrentRevision(null));
    rateCardChargeList.add(rcc);
    RateCard rateCard = new RateCard("Rate", chargeRecurrenceFrequencyDAO.findByName("NONE"), new Date(),
        getRootUser(), getRootUser());
    String compAssociationJson = "[{association: INCLUDES, compName: ISO, compValues:[ {compName: DOS-ISO, compValueName: 10 } ] }]";

    ProductBundle bundle = new ProductBundle("NullCompute", "NullCompute", "", new Date(), new Date(), getRootUser());
    bundle.setBusinessConstraint(ResourceConstraint.NONE);
    bundle.setCode("NullCompute");
    bundle.setPublish(true);
    bundle.setResourceType(resourceType);
    bundle.setServiceInstanceId(serviceInstance);
    bundle.setTrialEligibility(true);
    bundle.setRateCard(rateCard);

    ProductBundleForm form = new ProductBundleForm(bundle);
    form.setChargeType(null);
    form.setResourceType(resourceType.getId().toString());
    form.setServiceInstanceUUID(serviceInstance.getUuid());
    form.setBundleOneTimeCharges(rateCardChargeList);
    form.setBundleRecurringCharges(rateCardChargeList);
    form.setCompAssociationJson(compAssociationJson);

    BindingResult result = validate(form);
    ProductBundle obtainedBundle = bundleController.createProductBundle(form, result, map, response);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals(bundle.getName(), obtainedBundle.getName());
  }

  /*
   * Description: Test to create Compute Bundles with Bundle Name as Null Author: Vinayv
   */
  @Test
  public void testCreateComputeBundleWithNullName() throws Exception {

    try {
      String compAssociationJson = "[{association: INCLUDES, compName: ISO, compValues:[ {compName: DOS-ISO, compValueName: 10 } ] }]";
      testCreateProductBundle("1", "1", "MONTHLY", null, "USD", BigDecimal.valueOf(100), new Date(),
          compAssociationJson, ResourceConstraint.PER_USER, true);
    } catch (AjaxFormValidationException e) {
      Assert.assertEquals("Ajax Form Validation Error", e.getMessage());
    }
  }

  /*
   * Description: Test to create Compute Bundles with Code As Null Author: Vinayv
   */
  @Test(expected = Exception.class)
  public void testCreateComputeBundleWithNullCode() throws Exception {

    ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    List<RateCardCharge> rateCardChargeList = new ArrayList<RateCardCharge>();
    RateCardCharge rcc = new RateCardCharge(currencyValueService.locateBYCurrencyCode("USD"), catalogDAO.find(1L),
        BigDecimal.TEN, "RateCharge", getRootUser(), getRootUser(), channelService.getCurrentRevision(null));
    rateCardChargeList.add(rcc);
    RateCard rateCard = new RateCard("Rate", chargeRecurrenceFrequencyDAO.findByName("MONTHLY"), new Date(),
        getRootUser(), getRootUser());
    String compAssociationJson = "[{association: INCLUDES, compName: ISO, compValues:[ {compName: DOS-ISO, compValueName: 10 } ] }]";

    ProductBundle bundle = new ProductBundle("MonthlyCompute", "MonthlyCompute", "", new Date(), new Date(),
        getRootUser());
    bundle.setBusinessConstraint(ResourceConstraint.NONE);
    bundle.setCode(null);
    bundle.setPublish(true);
    bundle.setResourceType(resourceType);
    bundle.setServiceInstanceId(serviceInstance);
    bundle.setTrialEligibility(true);
    bundle.setRateCard(rateCard);

    ProductBundleForm form = new ProductBundleForm(bundle);
    form.setChargeType("MONTHLY");
    form.setResourceType(resourceType.getId().toString());
    form.setServiceInstanceUUID(serviceInstance.getUuid());
    form.setBundleOneTimeCharges(rateCardChargeList);
    form.setBundleRecurringCharges(rateCardChargeList);
    form.setCompAssociationJson(compAssociationJson);

    BindingResult result = validate(form);
    ProductBundle obtainedBundle = bundleController.createProductBundle(form, result, map, response);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals(bundle.getCode(), obtainedBundle.getCode());
  }

  /*
   * Description: Test to create Compute Bundles with Special Characters in Code Author: Vinayv
   */
  @Test(expected = Exception.class)
  public void testCreateComputeBundleWithSplCharsInCode() throws Exception {

    ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    List<RateCardCharge> rateCardChargeList = new ArrayList<RateCardCharge>();
    RateCardCharge rcc = new RateCardCharge(currencyValueService.locateBYCurrencyCode("USD"), catalogDAO.find(1L),
        BigDecimal.TEN, "RateCharge", getRootUser(), getRootUser(), channelService.getCurrentRevision(null));
    rateCardChargeList.add(rcc);
    RateCard rateCard = new RateCard("Rate", chargeRecurrenceFrequencyDAO.findByName("MONTHLY"), new Date(),
        getRootUser(), getRootUser());
    String compAssociationJson = "[{association: INCLUDES, compName: ISO, compValues:[ {compName: DOS-ISO, compValueName: 10 } ] }]";

    ProductBundle bundle = new ProductBundle("MonthlyCompute", "MonthlyCompute", "", new Date(), new Date(),
        getRootUser());
    bundle.setBusinessConstraint(ResourceConstraint.NONE);
    bundle.setCode("$@!#%^");
    bundle.setPublish(true);
    bundle.setResourceType(resourceType);
    bundle.setServiceInstanceId(serviceInstance);
    bundle.setTrialEligibility(true);
    bundle.setRateCard(rateCard);

    ProductBundleForm form = new ProductBundleForm(bundle);
    form.setChargeType("MONTHLY");
    form.setResourceType(resourceType.getId().toString());
    form.setServiceInstanceUUID(serviceInstance.getUuid());
    form.setBundleOneTimeCharges(rateCardChargeList);
    form.setBundleRecurringCharges(rateCardChargeList);
    form.setCompAssociationJson(compAssociationJson);

    BindingResult result = validate(form);
    ProductBundle obtainedBundle = bundleController.createProductBundle(form, result, map, response);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals(bundle.getName(), obtainedBundle.getName());
  }

  /*
   * Description: Test to create Compute Bundles with duplicate Bundle Name Author: Vinayv
   */
  @Test(expected = Exception.class)
  public void testCreateComputeBundleWithDuplicateName() throws Exception {

    ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    ProductBundle existingBundle = bundleService.locateProductBundleById("2");
    List<RateCardCharge> rateCardChargeList = new ArrayList<RateCardCharge>();
    RateCardCharge rcc = new RateCardCharge(currencyValueService.locateBYCurrencyCode("USD"), catalogDAO.find(1L),
        BigDecimal.TEN, "RateCharge", getRootUser(), getRootUser(), channelService.getCurrentRevision(null));
    rateCardChargeList.add(rcc);
    RateCard rateCard = new RateCard("Rate", chargeRecurrenceFrequencyDAO.findByName("MONTHLY"), new Date(),
        getRootUser(), getRootUser());
    String compAssociationJson = "[{association: INCLUDES, compName: ISO, compValues:[ {compName: DOS-ISO, compValueName: 10 } ] }]";

    ProductBundle bundle = new ProductBundle(existingBundle.getName(), "MonthlyCompute", "", new Date(), new Date(),
        getRootUser());
    bundle.setBusinessConstraint(ResourceConstraint.NONE);
    bundle.setCode("BundleCode");
    bundle.setPublish(true);
    bundle.setResourceType(resourceType);
    bundle.setServiceInstanceId(serviceInstance);
    bundle.setTrialEligibility(true);
    bundle.setRateCard(rateCard);

    ProductBundleForm form = new ProductBundleForm(bundle);
    form.setChargeType("MONTHLY");
    form.setResourceType(resourceType.getId().toString());
    form.setServiceInstanceUUID(serviceInstance.getUuid());
    form.setBundleOneTimeCharges(rateCardChargeList);
    form.setBundleRecurringCharges(rateCardChargeList);
    form.setCompAssociationJson(compAssociationJson);

    BindingResult result = validate(form);
    ProductBundle obtainedBundle = bundleController.createProductBundle(form, result, map, response);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals(existingBundle.getName(), obtainedBundle.getName());
  }

  /*
   * Description: Test to create Compute Bundles with duplicate Bundle Code Author: Vinayv
   */
  @Test
  public void testCreateComputeBundleWithDuplicateCode() throws Exception {

    ServiceInstance serviceInstance = serviceInstanceDAO.find("1");
    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    ProductBundle existingBundle = bundleService.locateProductBundleById("2");
    List<RateCardCharge> rateCardChargeList = new ArrayList<RateCardCharge>();
    RateCardCharge rcc = new RateCardCharge(currencyValueService.locateBYCurrencyCode("USD"), catalogDAO.find(1L),
        BigDecimal.TEN, "RateCharge", getRootUser(), getRootUser(), channelService.getCurrentRevision(null));
    rateCardChargeList.add(rcc);
    RateCard rateCard = new RateCard("Rate", chargeRecurrenceFrequencyDAO.findByName("MONTHLY"), new Date(),
        getRootUser(), getRootUser());
    String compAssociationJson = "[{association: INCLUDES, compName: ISO, compValues:[ {compName: DOS-ISO, compValueName: 10 } ] }]";

    ProductBundle bundle = new ProductBundle("MonthlyCompute", "MonthlyCompute", "", new Date(), new Date(),
        getRootUser());
    bundle.setBusinessConstraint(ResourceConstraint.NONE);
    bundle.setCode(existingBundle.getCode());
    bundle.setPublish(true);
    bundle.setResourceType(resourceType);
    bundle.setServiceInstanceId(serviceInstance);
    bundle.setTrialEligibility(true);
    bundle.setRateCard(rateCard);

    ProductBundleForm form = new ProductBundleForm(bundle);
    form.setChargeType("MONTHLY");
    form.setResourceType(resourceType.getId().toString());
    form.setServiceInstanceUUID(serviceInstance.getUuid());
    form.setBundleOneTimeCharges(rateCardChargeList);
    form.setBundleRecurringCharges(rateCardChargeList);
    form.setCompAssociationJson(compAssociationJson);

    BindingResult result = validate(form);
    ProductBundle obtainedBundle = bundleController.createProductBundle(form, result, map, response);
    Assert.assertNull(obtainedBundle);
  }

  /*
   * Description: Test to Add Entitlement to a Bundle Author: Vinayv
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testAddEntitlement() throws Exception {

    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 - noOfdays);
    ProductBundle obtainedBundle = bundleService.locateProductBundleById("3");
    String result1 = bundleController.getFilteredEntitlements(obtainedBundle.getCode(), "planned",
        new Date().toString(), 1, 100, map, request);
    Assert.assertNotNull(result1);
    Assert.assertEquals("bundle.entitlement.list", result1);
    List<Entitlement> eList = (List<Entitlement>) map.get("entitlements");
    int beforeEntitlementSize = eList.size();
    Product product = productService.locateProductById("2");
    Entitlement entitlement = new Entitlement();
    entitlement.setCreatedAt(createdAt.getTime());
    entitlement.setCreatedBy(getRootUser());
    entitlement.setIncludedUnits(0);
    entitlement.setProduct(product);
    entitlement.setProductBundle(obtainedBundle);
    entitlement.setRevision(channelService.getFutureRevision(null));
    entitlement.setUpdatedBy(getRootUser());

    EntitlementForm entitlementForm = new EntitlementForm();
    entitlementForm.setProductId(product.getId().toString());
    entitlementForm.setUnlimitedUsage(false);
    entitlementForm.setEntitlement(entitlement);

    BindingResult result = validate(entitlementForm);
    String resultString = bundleController.createEntitlement(obtainedBundle.getCode(), entitlementForm, result,
        response, map);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("success", resultString);
    Entitlement obtainedEntitlement = (Entitlement) map.get("entitlement");
    Assert.assertEquals(obtainedBundle, obtainedEntitlement.getProductBundle());

    String result2 = bundleController.getFilteredEntitlements(obtainedBundle.getCode(), "planned",
        new Date().toString(), 1, 100, map, request);
    Assert.assertNotNull(result2);
    Assert.assertEquals("bundle.entitlement.list", result2);
    List<Entitlement> eList1 = (List<Entitlement>) map.get("entitlements");
    int afterEntitlementSize = eList1.size();
    Assert.assertEquals(beforeEntitlementSize + 1, afterEntitlementSize);
    for (Entitlement e : eList1) {
      Assert.assertEquals(entitlement, e);
    }
  }

  /*
   * Description: Test to Add Unlimited Entitlement to a Bundle Author: Vinayv
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testAddUnlimitedEntitlement() throws Exception {

    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 - noOfdays);
    ProductBundle obtainedBundle = bundleService.locateProductBundleById("3");
    String result1 = bundleController.getFilteredEntitlements(obtainedBundle.getCode(), "planned",
        new Date().toString(), 1, 100, map, request);
    Assert.assertNotNull(result1);
    Assert.assertEquals("bundle.entitlement.list", result1);
    List<Entitlement> eList = (List<Entitlement>) map.get("entitlements");
    int beforeEntitlementSize = eList.size();
    Product product = productService.locateProductById("2");
    Entitlement entitlement = new Entitlement();
    entitlement.setCreatedAt(createdAt.getTime());
    entitlement.setCreatedBy(getRootUser());
    entitlement.setIncludedUnits(-1);
    entitlement.setProduct(product);
    entitlement.setProductBundle(obtainedBundle);
    entitlement.setRevision(channelService.getFutureRevision(null));
    entitlement.setUpdatedBy(getRootUser());

    EntitlementForm entitlementForm = new EntitlementForm();
    entitlementForm.setProductId(product.getId().toString());
    entitlementForm.setUnlimitedUsage(true);
    entitlementForm.setEntitlement(entitlement);

    BindingResult result = validate(entitlementForm);
    String resultString = bundleController.createEntitlement(obtainedBundle.getCode(), entitlementForm, result,
        response, map);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("success", resultString);
    Entitlement obtainedEntitlement = (Entitlement) map.get("entitlement");
    Assert.assertEquals(obtainedBundle, obtainedEntitlement.getProductBundle());

    String result2 = bundleController.getFilteredEntitlements(obtainedBundle.getCode(), "planned",
        new Date().toString(), 1, 100, map, request);
    Assert.assertNotNull(result2);
    Assert.assertEquals("bundle.entitlement.list", result2);
    List<Entitlement> eList1 = (List<Entitlement>) map.get("entitlements");
    int afterEntitlementSize = eList1.size();
    Assert.assertEquals(beforeEntitlementSize + 1, afterEntitlementSize);
    for (Entitlement e : eList1) {
      Assert.assertEquals(-1, e.getIncludedUnits());
    }
  }

  /*
   * Description: Test to Update valid Entitlement to Unlimited Entitlement for a Bundle Author: Vinayv
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testUpdateValidToUnlimitedEntitlement() throws Exception {

    ProductBundle obtainedBundle = bundleService.locateProductBundleById("2");
    String result1 = bundleController.getFilteredEntitlements(obtainedBundle.getCode(), "planned",
        new Date().toString(), 1, 100, map, request);
    Assert.assertNotNull(result1);
    Assert.assertEquals("bundle.entitlement.list", result1);
    List<Entitlement> eList = (List<Entitlement>) map.get("entitlements");
    int beforeEntitlementSize = eList.size();
    Entitlement entitlement = null;
    for (Entitlement e : eList) {
      if (e.getIncludedUnits() != -1) {
        entitlement = e;
        break;
      }
    }
    Entitlement obtainedEntitlement = bundleController.saveEntitlement(entitlement.getId().toString(), -1, map);
    Assert.assertNotNull(obtainedEntitlement);
    Assert.assertEquals(-1, obtainedEntitlement.getIncludedUnits());
    String result2 = bundleController.getFilteredEntitlements(obtainedBundle.getCode(), "planned",
        new Date().toString(), 1, 100, map, request);
    Assert.assertNotNull(result2);
    Assert.assertEquals("bundle.entitlement.list", result2);
    List<Entitlement> eList1 = (List<Entitlement>) map.get("entitlements");
    int afterEntitlementSize = eList1.size();
    Assert.assertEquals(beforeEntitlementSize, afterEntitlementSize);
  }

  /*
   * Description: Test to Update Unlimited Entitlement to Valid Entitlement for a Bundle Author: Vinayv
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testUpdateUnlimitedToValidEntitlement() throws Exception {

    ProductBundle obtainedBundle = bundleService.locateProductBundleById("2");
    String result1 = bundleController.getFilteredEntitlements(obtainedBundle.getCode(), "planned",
        new Date().toString(), 1, 100, map, request);
    Assert.assertNotNull(result1);
    Assert.assertEquals("bundle.entitlement.list", result1);
    List<Entitlement> eList = (List<Entitlement>) map.get("entitlements");
    int beforeEntitlementSize = eList.size();
    Entitlement entitlement = null;
    for (Entitlement e : eList) {
      if (e.getIncludedUnits() == -1) {
        entitlement = e;
        break;
      }
    }
    Entitlement obtainedEntitlement = bundleController.saveEntitlement(entitlement.getId().toString(), 10, map);
    Assert.assertNotNull(obtainedEntitlement);
    Assert.assertEquals(10, obtainedEntitlement.getIncludedUnits());
    String result2 = bundleController.getFilteredEntitlements(obtainedBundle.getCode(), "planned",
        new Date().toString(), 1, 100, map, request);
    Assert.assertNotNull(result2);
    Assert.assertEquals("bundle.entitlement.list", result2);
    List<Entitlement> eList1 = (List<Entitlement>) map.get("entitlements");
    int afterEntitlementSize = eList1.size();
    Assert.assertEquals(beforeEntitlementSize, afterEntitlementSize);
  }

  /*
   * Description: Test to Delete a valid Entitlement from a Bundle Author: Vinayv
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testDeleteValidEntitlement() throws Exception {

    ProductBundle obtainedBundle = bundleService.locateProductBundleById("2");
    String result1 = bundleController.getFilteredEntitlements(obtainedBundle.getCode(), "planned",
        new Date().toString(), 1, 100, map, request);
    Assert.assertNotNull(result1);
    Assert.assertEquals("bundle.entitlement.list", result1);
    List<Entitlement> eList = (List<Entitlement>) map.get("entitlements");
    int beforeEntitlementSize = eList.size();
    Entitlement entitlement = null;
    for (Entitlement e : eList) {
      if (e.getIncludedUnits() != -1) {
        entitlement = e;
        break;
      }
    }
    Product obtainedProduct = bundleController.deleteEntitlement(entitlement.getId().toString(), obtainedBundle.getId()
        .toString(), map);
    Assert.assertNotNull(obtainedProduct);
    Assert.assertEquals(entitlement.getProduct(), obtainedProduct);
    String result2 = bundleController.getFilteredEntitlements(obtainedBundle.getCode(), "planned",
        new Date().toString(), 1, 100, map, request);
    Assert.assertNotNull(result2);
    Assert.assertEquals("bundle.entitlement.list", result2);
    List<Entitlement> eList1 = (List<Entitlement>) map.get("entitlements");
    int afterEntitlementSize = eList1.size();
    Assert.assertEquals(beforeEntitlementSize - 1, afterEntitlementSize);
  }

  /*
   * Description: Test to Delete Unlimited Entitlement from a Bundle Author: Vinayv
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testDeleteUnlimitedEntitlement() throws Exception {

    ProductBundle obtainedBundle = bundleService.locateProductBundleById("2");
    String result1 = bundleController.getFilteredEntitlements(obtainedBundle.getCode(), "planned",
        new Date().toString(), 1, 100, map, request);
    Assert.assertNotNull(result1);
    Assert.assertEquals("bundle.entitlement.list", result1);
    List<Entitlement> eList = (List<Entitlement>) map.get("entitlements");
    int beforeEntitlementSize = eList.size();
    Entitlement entitlement = null;
    for (Entitlement e : eList) {
      if (e.getIncludedUnits() == -1) {
        entitlement = e;
        break;
      }
    }
    Product obtainedProduct = bundleController.deleteEntitlement(entitlement.getId().toString(), obtainedBundle.getId()
        .toString(), map);
    Assert.assertNotNull(obtainedProduct);
    Assert.assertEquals(entitlement.getProduct(), obtainedProduct);
    String result2 = bundleController.getFilteredEntitlements(obtainedBundle.getCode(), "planned",
        new Date().toString(), 1, 100, map, request);
    Assert.assertNotNull(result2);
    Assert.assertEquals("bundle.entitlement.list", result2);
    List<Entitlement> eList1 = (List<Entitlement>) map.get("entitlements");
    int afterEntitlementSize = eList1.size();
    Assert.assertEquals(beforeEntitlementSize - 1, afterEntitlementSize);
  }

  /*
   * Description: Test to unpublish a bundle Author: Vinayv
   */
  @Test
  public void testUnpublishBundle() throws Exception {

    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    String compAssociationJson = "[{association: INCLUDES, compName: ISO, compValues:[ {compName: DOS-ISO, compValueName: 10 } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 - noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
    Assert.assertEquals(true, obtainedBundle.getPublish().booleanValue());
    String result = bundleController.publishBundle(obtainedBundle.getCode(), "false", map);
    Assert.assertNotNull(result);
    Assert.assertEquals("success", result);
    Assert.assertEquals(false, obtainedBundle.getPublish().booleanValue());
  }

  /*
   * Description: Test to Set Plan Date for a bundle Author: Vinayv
   */
  @Test
  public void testSetPlanDateForBundle() throws Exception {

    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    String compAssociationJson = "[{association: INCLUDES, compName: ISO, compValues:[ {compName: DOS-ISO, compValueName: 10 } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
    productService.setReferencePriceBookFutureRevisionDate(createdAt.getTime());
    List<Revision> allRPBRevisions = productService.getAllRevisions(null);
    Revision pbr = allRPBRevisions.get(1);
    Assert.assertEquals(DateUtils.truncate(createdAt.getTime()), pbr.getStartDate());
  }

  /*
   * Description: Test to create Compute Bundles with Null Resource Type Author: Vinayv
   */
  @Test(expected = NullPointerException.class)
  public void testCreateComputeBundleWithNullResourceType() throws Exception {

    String compAssociationJson = "[{association: INCLUDES, compName: ISO, compValues:[ {compName: DOS-ISO, compValueName: 10 } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 - noOfdays);
    testCreateProductBundle("1", null, "MONTHLY", "MONTHLYCompute", "USD", BigDecimal.valueOf(100),
        createdAt.getTime(), compAssociationJson, ResourceConstraint.PER_USER, true);
  }

  /*
   * Description: Test to create Compute Bundles with Null Business Constraint Author: Vinayv
   */
  @Test
  public void testCreateComputeBundleWithNullBusinessConstraint() throws Exception {

    try {
      ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
      int noOfdays = 3;
      Calendar createdAt = Calendar.getInstance();
      createdAt.add(Calendar.DATE, 0 - noOfdays);
      testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY", "MONTHLYCompute", "USD",
          BigDecimal.valueOf(100), createdAt.getTime(), "", ResourceConstraint.PER_USER, true);
    } catch (Exception e) {
      Assert.assertEquals("A JSONArray text must start with '[' at character 0 of ", e.getMessage());
    }
  }

  /*
   * Description: Test to create a Bundle with Template Includes Author: Vinayv
   */
  @Test
  public void testCreateBundleWithTemplateIncludes() throws Exception {

    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    String compAssociationJson = "[{association: INCLUDES, compName: Template, compValues:[ {compName: DOS-VM, compValueName: 10 } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
  }

  /*
   * Description: Test to create a Bundle with Template Excludes Author: Vinayv
   */
  @Test
  public void testCreateBundleWithTemplateExcludes() throws Exception {

    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    String compAssociationJson = "[{association: EXCLUDES, compName: Template, compValues:[ {compName: DOS-VM, compValueName: 10 } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
  }

  /*
   * Description: Test to create a Bundle with Hypervisor Includes Author: Vinayv
   */
  @Test
  public void testCreateBundleWithHypervisorIncludes() throws Exception {

    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    String compAssociationJson = "[{association: INCLUDES, compName: Hypervisor, compValues:[ {compName: VMWare, compValueName: 10 } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
  }

  /*
   * Description: Test to create a Bundle with Hypervisor Excludes Author: Vinayv
   */
  @Test
  public void testCreateBundleWithHypervisorExcludes() throws Exception {

    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    String compAssociationJson = "[{association: EXCLUDES, compName: Hypervisor, compValues:[ {compName: VMWare, compValueName: 10 } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
  }

  /*
   * Description: Test to create a Bundle with ServiceOffering Includes Author: Vinayv
   */
  @Test
  public void testCreateBundleWithServiceOfferingIncludes() throws Exception {

    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    String compAssociationJson = "[{association: INCLUDES, compName: ServiceOffering, compValues:[ {compName: SO, compValueName: Small Instance } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
  }

  /*
   * Description: Test to create a Bundle with ServiceOffering Excludes Author: Vinayv
   */
  @Test
  public void testCreateBundleWithServiceOfferingExcludes() throws Exception {

    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    String compAssociationJson = "[{association: EXCLUDES, compName: ServiceOffering, compValues:[ {compName: SO, compValueName: Small Instance } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
  }

  /*
   * Description: Test to create a Bundle with NetworkOffering Includes Author: Vinayv
   */
  @Test
  public void testCreateBundleWithNewtworkOfferingIncludes() throws Exception {

    ServiceResourceType resourceType = serviceResourceTypeDAO.find("2");
    String compAssociationJson = "[{association: INCLUDES, compName: NetworkOffering, compValues:[ {compName: NetworkOffering, compValueName: DefaultSharedNetworkOffering } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
  }

  /*
   * Description: Test to create a Bundle with NetworkOffering Excludes Author: Vinayv
   */
  @Test
  public void testCreateBundleWithNetworkOfferingExcludes() throws Exception {

    ServiceResourceType resourceType = serviceResourceTypeDAO.find("2");
    String compAssociationJson = "[{association: EXCLUDES, compName: NetworkOffering, compValues:[ {compName: NetworkOffering, compValueName: DefaultSharedNetworkOffering } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
  }

  /*
   * Description: Test to create Compute Bundles with None, Monthly, Quarterly and Annual charge frequency As Product
   * Manager Author: Vinayv
   */
  @Test
  public void testCreateComputeBundlesAsProductManager() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    String[] chargeFrequency = {
        "NONE", "MONTHLY", "QUARTERLY", "ANNUAL"
    };
    for (int i = 0; i < chargeFrequency.length; i++) {

      String compAssociationJson = "[{association: INCLUDES, compName: ISO, compValues:[ {compName: DOS-ISO, compValueName: 10 } ] }, {association: EXCLUDES, compName: SO, compValues:[ {compName: SO, compValueName: SmallInstance } ] }]";
      int noOfdays = 3;
      Calendar createdAt = Calendar.getInstance();
      createdAt.add(Calendar.DATE, 0 - noOfdays);
      int beforeBundleCount = bundleService.getBundlesCount();
      boolean trialEligible = false;
      if (i == 1)
        trialEligible = true;
      ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), chargeFrequency[i],
          chargeFrequency[i] + "Compute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
          ResourceConstraint.PER_USER, trialEligible);
      Assert.assertNotNull(obtainedBundle);
      Assert.assertEquals(chargeFrequency[i] + "Compute", obtainedBundle.getName());
      Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
      Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
      int afterBundleCount = bundleService.getBundlesCount();
      Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
    }
  }

  /*
   * Description: Test to create a Bundle with Template Includes As Product Manager Author: Vinayv
   */
  @Test
  public void testCreateBundleWithTemplateIncludesAsProductManager() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    String compAssociationJson = "[{association: INCLUDES, compName: Template, compValues:[ {compName: Template, compValueName: DOS-VM } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
  }

  /*
   * Description: Test to create a Bundle with Template Excludes As Product Manager Author: Vinayv
   */
  @Test
  public void testCreateBundleWithTemplateExcludesAsProductManager() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    String compAssociationJson = "[{association: EXCLUDES, compName: Template, compValues:[ {compName: Template, compValueName: DOS-VM } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
  }

  /*
   * Description: Test to create a Bundle with Hypervisor Includes As Product Manager Author: Vinayv
   */
  @Test
  public void testCreateBundleWithHypervisorIncludesAsProductManager() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    String compAssociationJson = "[{association: INCLUDES, compName: Hypervisor, compValues:[ {compName: Hypervisor, compValueName: VMWare } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
  }

  /*
   * Description: Test to create a Bundle with Hypervisor Excludes As Product Manager Author: Vinayv
   */
  @Test
  public void testCreateBundleWithHypervisorExcludesAsProductManager() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    String compAssociationJson = "[{association: EXCLUDES, compName: Hypervisor, compValues:[ {compName: Hypervisor, compValueName: VMWare } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
  }

  /*
   * Description: Test to create a Bundle with ServiceOffering Includes As Product Manager Author: Vinayv
   */
  @Test
  public void testCreateBundleWithServiceOfferingIncludesAsProductManager() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    String compAssociationJson = "[{association: INCLUDES, compName: ServiceOffering, compValues:[ {compName: SO, compValueName: Small Instance } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
  }

  /*
   * Description: Test to create a Bundle with ServiceOffering Excludes As Product Manager Author: Vinayv
   */
  @Test
  public void testCreateBundleWithServiceOfferingExcludesAsProductManager() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");
    String compAssociationJson = "[{association: EXCLUDES, compName: ServiceOffering, compValues:[ {compName: SO, compValueName: Small Instance } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
  }

  /*
   * Description: Test to create a Bundle with NetworkOffering Includes As Product Manager Author: Vinayv
   */
  @Test
  public void testCreateBundleWithNewtworkOfferingIncludesAsProductManager() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    ServiceResourceType resourceType = serviceResourceTypeDAO.find("2");
    String compAssociationJson = "[{association: INCLUDES, compName: NetworkOffering, compValues:[ {compName: NetworkOffering, compValueName: DefaultSharedNetworkOffering } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
  }

  /*
   * Description: Test to create a Bundle with NetworkOffering Excludes As Product Manager Author: Vinayv
   */
  @Test
  public void testCreateBundleWithNetworkOfferingExcludesAsProductManager() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    ServiceResourceType resourceType = serviceResourceTypeDAO.find("2");
    String compAssociationJson = "[{association: EXCLUDES, compName: NetworkOffering, compValues:[ {compName: NetworkOffering, compValueName: DefaultSharedNetworkOffering } ] }]";
    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    int beforeBundleCount = bundleService.getBundlesCount();
    ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "MONTHLYCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
        ResourceConstraint.PER_USER, true);
    Assert.assertNotNull(obtainedBundle);
    Assert.assertEquals("MONTHLYCompute", obtainedBundle.getName());
    Assert.assertEquals(resourceType, obtainedBundle.getResourceType());
    Assert.assertEquals(ResourceConstraint.PER_USER, obtainedBundle.getBusinessConstraint());
    int afterBundleCount = bundleService.getBundlesCount();
    Assert.assertEquals(beforeBundleCount + 1, afterBundleCount);
  }

  /*
   * Description: Test to Set and View Planned Charges for a Bundle Author: Vinayv
   */
  @Test
  public void testSetAndViewPlannedCharges() throws Exception {

    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    ProductBundle bundle = bundleService.locateProductBundleById("3");

    List<RateCardComponentChargesForm> nonRecurringRateCardChargesFormList = new ArrayList<RateCardComponentChargesForm>();
    RateCardComponent rcc = rateCardComponentDAO.find("18");
    List<RateCardCharge> charges = bundleService.getCatalogProductBundlePlannedCharges(null, bundle);
    RateCardComponentChargesForm nonrcccform = new RateCardComponentChargesForm();
    nonrcccform.setRcc(rcc);
    nonrcccform.setCharges(charges);
    nonRecurringRateCardChargesFormList.add(nonrcccform);
    List<RateCardComponentChargesForm> recurringRateCardChargesFormList = new ArrayList<RateCardComponentChargesForm>();
    RateCardComponent rrcc = rateCardComponentDAO.find("17");
    RateCardComponentChargesForm rcccform = new RateCardComponentChargesForm();
    rcccform.setRcc(rrcc);
    rcccform.setCharges(charges);
    recurringRateCardChargesFormList.add(rcccform);

    List<RateCardChargesForm> rateCardChargesList = new ArrayList<RateCardChargesForm>();
    RateCardChargesForm rccform = new RateCardChargesForm();
    rccform.setBundle(bundle);
    rccform.setNonRecurringRateCardChargesFormList(nonRecurringRateCardChargesFormList);
    rccform.setRecurringRateCardChargesFormList(recurringRateCardChargesFormList);
    rateCardChargesList.add(rccform);

    Map<ProductBundle, Map<RateCardComponent, List<RateCardCharge>>> currentBundleChargesMap = bundleService
        .getChargesForAllBundles(new Date());
    RateCardForm rcform = new RateCardForm();
    rcform.setPlannedDate(new Date());
    rcform.setStartDate(new Date());
    rcform.setCurrentBundleChargesMap(currentBundleChargesMap);
    rcform.setRateCardChargesList(rateCardChargesList);

    BindingResult result = validate(rcform);
    String resultString = bundleController.planCharges(rcform, result, map);
    Assert.assertNotNull(resultString);
    Assert.assertEquals("success", resultString);

    // Viewing Plannned Charges
    String result1 = bundleController.viewBundlePlannedCharges(bundle.getCode(), map, "");
    Assert.assertNotNull(result1);
    Assert.assertEquals("view.bundle.planned.charges", result1);
    RateCardChargesForm obtainedRCCFORM = (RateCardChargesForm) map.get("rateCardChargesForm");
    List<RateCardComponentChargesForm> rcccformList = obtainedRCCFORM.getRecurringRateCardChargesFormList();
    for (RateCardComponentChargesForm obtainedrcccform : rcccformList) {
      Assert.assertEquals(rrcc, obtainedrcccform.getRcc());
    }
    List<RateCardComponentChargesForm> nonrcccformList = obtainedRCCFORM.getNonRecurringRateCardChargesFormList();
    for (RateCardComponentChargesForm obtainedrcccform : nonrcccformList) {
      Assert.assertEquals(rcc, obtainedrcccform.getRcc());
    }

  }

  /*
   * Description: Test to Set, Edit and View Planned Charges for a Bundle Author: Vinayv
   */
  @Test
  public void testEditAndViewPlannedCharges() throws Exception {

    int noOfdays = 3;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 + noOfdays);
    ProductBundle bundle = bundleService.locateProductBundleById("3");

    // Setting Planned Charges
    List<RateCardComponentChargesForm> nonRecurringRateCardChargesFormList = new ArrayList<RateCardComponentChargesForm>();
    RateCardComponent rcc = rateCardComponentDAO.find("18");
    List<RateCardCharge> charges = bundleService.getCatalogProductBundlePlannedCharges(null, bundle);
    RateCardComponentChargesForm nonrcccform = new RateCardComponentChargesForm();
    nonrcccform.setRcc(rcc);
    nonrcccform.setCharges(charges);
    nonRecurringRateCardChargesFormList.add(nonrcccform);
    List<RateCardComponentChargesForm> recurringRateCardChargesFormList = new ArrayList<RateCardComponentChargesForm>();
    RateCardComponent rrcc = rateCardComponentDAO.find("17");
    RateCardComponentChargesForm rcccform = new RateCardComponentChargesForm();
    rcccform.setRcc(rrcc);
    rcccform.setCharges(charges);
    recurringRateCardChargesFormList.add(rcccform);

    List<RateCardChargesForm> rateCardChargesList = new ArrayList<RateCardChargesForm>();
    RateCardChargesForm rccform = new RateCardChargesForm();
    rccform.setBundle(bundle);
    rccform.setNonRecurringRateCardChargesFormList(nonRecurringRateCardChargesFormList);
    rccform.setRecurringRateCardChargesFormList(recurringRateCardChargesFormList);
    rateCardChargesList.add(rccform);

    Map<ProductBundle, Map<RateCardComponent, List<RateCardCharge>>> currentBundleChargesMap = bundleService
        .getChargesForAllBundles(new Date());
    RateCardForm rcform = new RateCardForm();
    rcform.setPlannedDate(new Date());
    rcform.setStartDate(new Date());
    rcform.setCurrentBundleChargesMap(currentBundleChargesMap);
    rcform.setRateCardChargesList(rateCardChargesList);

    BindingResult bindlingResultSet = validate(rcform);
    String resultStringSet = bundleController.planCharges(rcform, bindlingResultSet, map);
    Assert.assertNotNull(resultStringSet);
    Assert.assertEquals("success", resultStringSet);

    // Viewing Planned Charges Before Editing
    String result1 = bundleController.viewBundlePlannedCharges(bundle.getCode(), map, "");
    Assert.assertNotNull(result1);
    Assert.assertEquals("view.bundle.planned.charges", result1);
    RateCardChargesForm obtainedRCCFORM = (RateCardChargesForm) map.get("rateCardChargesForm");
    List<RateCardComponentChargesForm> rcccformList = obtainedRCCFORM.getRecurringRateCardChargesFormList();
    for (RateCardComponentChargesForm obtainedrcccform : rcccformList) {
      Assert.assertEquals(rrcc, obtainedrcccform.getRcc());
    }
    List<RateCardComponentChargesForm> nonrcccformList = obtainedRCCFORM.getNonRecurringRateCardChargesFormList();
    for (RateCardComponentChargesForm obtainedrcccform : nonrcccformList) {
      Assert.assertEquals(rcc, obtainedrcccform.getRcc());
    }

    // Editing Planned Charges
    List<RateCardComponentChargesForm> editNonRecurringRateCardChargesFormList = new ArrayList<RateCardComponentChargesForm>();
    RateCardComponent editrcc = rateCardComponentDAO.find("18");

    List<RateCardCharge> editcharges = bundleService.getCatalogProductBundlePlannedCharges(null, bundle);
    for (RateCardCharge rateCardCharge : editcharges) {
      if (rateCardCharge.getRateCardComponent().equals(editrcc)) {
        if (rateCardCharge.getCurrencyValue().getCurrencyCode().equalsIgnoreCase("USD")) {
          rateCardCharge.setPrice(BigDecimal.TEN);
        }
      }
    }
    RateCardComponentChargesForm editnonrcccform = new RateCardComponentChargesForm();
    editnonrcccform.setRcc(editrcc);
    editnonrcccform.setCharges(editcharges);
    editNonRecurringRateCardChargesFormList.add(editnonrcccform);

    List<RateCardComponentChargesForm> editRecurringRateCardChargesFormList = new ArrayList<RateCardComponentChargesForm>();
    RateCardComponent editrrcc = rateCardComponentDAO.find("17");
    for (RateCardCharge rateCardCharge : editcharges) {
      if (rateCardCharge.getRateCardComponent().equals(editrrcc)) {
        if (rateCardCharge.getCurrencyValue().getCurrencyCode().equalsIgnoreCase("USD")) {
          rateCardCharge.setPrice(BigDecimal.valueOf(500));
        }
      }
    }
    RateCardComponentChargesForm editrcccform = new RateCardComponentChargesForm();
    editrcccform.setRcc(editrrcc);
    editrcccform.setCharges(editcharges);
    editRecurringRateCardChargesFormList.add(editrcccform);

    List<RateCardChargesForm> editRateCardChargesList = new ArrayList<RateCardChargesForm>();
    RateCardChargesForm editrccform = new RateCardChargesForm();
    editrccform.setBundle(bundle);
    editrccform.setNonRecurringRateCardChargesFormList(editNonRecurringRateCardChargesFormList);
    editrccform.setRecurringRateCardChargesFormList(editRecurringRateCardChargesFormList);
    editRateCardChargesList.add(editrccform);

    Map<ProductBundle, Map<RateCardComponent, List<RateCardCharge>>> editCurrentBundleChargesMap = bundleService
        .getChargesForAllBundles(new Date());
    RateCardForm editrcform = new RateCardForm();
    rcform.setPlannedDate(new Date());
    rcform.setStartDate(new Date());
    rcform.setCurrentBundleChargesMap(editCurrentBundleChargesMap);
    rcform.setRateCardChargesList(editRateCardChargesList);

    BindingResult bindlingResultEdit = validate(editrcform);
    String resultStringEdit = bundleController.editPlannedCharges(editrcform, bindlingResultEdit, map);
    Assert.assertNotNull(resultStringEdit);
    Assert.assertEquals("success", resultStringEdit);

    // Viewing Planned Charges After Editing
    String result2 = bundleController.viewBundlePlannedCharges(bundle.getCode(), map, "");
    Assert.assertNotNull(result2);
    Assert.assertEquals("view.bundle.planned.charges", result2);
    RateCardChargesForm editedRCCFORM = (RateCardChargesForm) map.get("rateCardChargesForm");
    List<RateCardComponentChargesForm> editedrcccformList = editedRCCFORM.getRecurringRateCardChargesFormList();
    for (RateCardComponentChargesForm editedobtainedrcccform : editedrcccformList) {
      Assert.assertEquals(editrrcc, editedobtainedrcccform.getRcc());
      List<RateCardCharge> obtainedList = editedobtainedrcccform.getCharges();
      for (RateCardCharge obtainedRateCardCharge : obtainedList) {
        if (obtainedRateCardCharge.getCurrencyValue().getCurrencyCode().equalsIgnoreCase("USD")) {
          Assert.assertEquals(BigDecimal.valueOf(500), obtainedRateCardCharge.getPrice());
        }
      }
    }
    List<RateCardComponentChargesForm> editednonrcccformList = editedRCCFORM.getNonRecurringRateCardChargesFormList();
    for (RateCardComponentChargesForm editedobtainedrcccform : editednonrcccformList) {
      Assert.assertEquals(editrcc, editedobtainedrcccform.getRcc());
      List<RateCardCharge> obtainedList = editedobtainedrcccform.getCharges();
      for (RateCardCharge obtainedRateCardCharge : obtainedList) {
        if (obtainedRateCardCharge.getCurrencyValue().getCurrencyCode().equalsIgnoreCase("USD")) {
          Assert.assertEquals(BigDecimal.TEN, obtainedRateCardCharge.getPrice());
        }
      }
    }
  }

  /*
   * Description: Test for adding new bundle to the existing channel on the same bundle creation date. Author:Avinashg
   */

  @Test
  public void testAddBundletoExistingChannelOnCreationDay() throws Exception {
    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");

    String compAssociationJson = "[{association: INCLUDES, compName: ISO, compValues:[ {compName: DOS-ISO, compValueName: 10 } ] }]";

    ProductBundle productBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "testBundle", "USD", BigDecimal.valueOf(149), new Date(), compAssociationJson, ResourceConstraint.PER_USER,
        true);

    productBundle.setPublish(true);

    bundleService.updateProductBundle(productBundle, true);
    productService.setReferencePriceBookFutureRevisionDate(new Date());

    List<Channel> beforeChannelList = channelService.listChannelsByProductBundle(productBundle, 1, 50);
    Assert.assertEquals(0, beforeChannelList.size());

    Channel channel = channelService.locateChannel("Channel2");
    channelService.syncChannel(channel);

    List<Revision> allChannelRevisions = productService.getAllRevisions(channel.getCatalog());

    /*
     * Revision firstChannelRevision = allChannelRevisions .get(allChannelRevisions.size() - 1);
     */
    Revision firstChannelRevision = allChannelRevisions.get(0);

    CatalogProductBundle catalogProductBundle = new CatalogProductBundle(channel.getCatalog(), productBundle,
        getPortalUser());

    catalogProductBundle.setCreatedAt(new Date());
    catalogProductBundle.setRevision(firstChannelRevision);

    channelService.save(catalogProductBundle);

    List<Channel> afterChannelList = channelService.listChannelsByProductBundle(productBundle, 1, 50);
    Assert.assertEquals(1, afterChannelList.size());

  }

  /*
   * Description: Test for adding new bundle to the new channel on the same bundle creation date and tenant can
   * subscribe VM by using the new bundle. Author:Avinashg
   */

  @Test
  public void testAddBundletonNewChannelOnCreationDay() throws Exception {
    ServiceResourceType resourceType = serviceResourceTypeDAO.find("1");

    String compAssociationJson = "[{association: INCLUDES, compName: ISO, compValues:[ {compName: DOS-ISO, compValueName: 10 } ] }]";

    ProductBundle productBundle = testCreateProductBundle("1", resourceType.getId().toString(), "MONTHLY",
        "testBundle", "USD", BigDecimal.valueOf(149), new Date(), compAssociationJson, ResourceConstraint.PER_USER,
        true);

    productBundle.setPublish(true);

    bundleService.updateProductBundle(productBundle, true);
    productService.setReferencePriceBookFutureRevisionDate(new Date());

    List<Channel> beforeChannelList = channelService.listChannelsByProductBundle(productBundle, 1, 50);
    Assert.assertEquals(0, beforeChannelList.size());

    // Channel channel = channelService.locateChannel("Channel2");
    Channel channel = new Channel("newChannel", ChannelType.CHANNEL);
    Catalog catalog = new Catalog("Test Catalog" + new Date(), "Test Catalog", new Date(), new Date(), getPortalUser());
    catalogDAO.save(catalog);
    channel.setCatalog(catalog);
    Channel newchannel = channelService.createChannel(channel);
    channelService.syncChannel(newchannel);

    List<Revision> allChannelRevisions = productService.getAllRevisions(newchannel.getCatalog());

    Revision firstChannelRevision = allChannelRevisions.get(allChannelRevisions.size() - 1);

    CatalogProductBundle catalogProductBundle = new CatalogProductBundle(newchannel.getCatalog(), productBundle,
        getPortalUser());

    catalogProductBundle.setCreatedAt(new Date());
    catalogProductBundle.setRevision(firstChannelRevision);

    channelService.save(catalogProductBundle);

    List<Channel> afterChannelList = channelService.listChannelsByProductBundle(productBundle, 1, 50);
    Assert.assertEquals(1, afterChannelList.size());
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "All" in view current tab
   */

  @Test
  public void testAllProductBundleFilterInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "current", null, "All", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(20, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "publish" in view current tab
   */

  @Test
  public void testPublishProductBundleFilterInCurrentViewForProducts() {

    ProductBundle bundle = bundleService.getProductBundleById(2L);
    bundle.setPublish(false);
    bundleService.updateProductBundle(bundle, true);
    bundle = bundleService.getProductBundleById(2L);
    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "current", null, "publish", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(19, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "Unpublish" in view current tab
   */

  @Test
  public void testUnpublishProductBundleFilterInCurrentViewForProducts() {

    ProductBundle bundle = bundleService.getProductBundleById(2L);
    bundle.setPublish(false);
    bundleService.updateProductBundle(bundle, true);
    bundle = bundleService.getProductBundleById(2L);
    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "current", null, "unpublish",
          map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(1, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "Null" in view current tab
   */

  @Test
  public void testNullProductBundleFilterInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "current", null, null, map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(20, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "Empty" in view current tab
   */

  @Test
  public void testEmptyProductBundleFilterInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "current", null, "", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(20, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "Invalid" in view current tab
   */

  @Test
  public void testInvalidProductBundleFilterInCurrentViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "current", null, "Invalid", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "All" in view history tab
   */

  @Test
  public void testAllProductBundleFilterInhistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "history", null, "All", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(20, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "publish" in view history tab
   */

  @Test
  public void testPublishProductBundleFilterInhistoryViewForProducts() {

    ProductBundle bundle = bundleService.getProductBundleById(2L);
    bundle.setPublish(false);
    bundleService.updateProductBundle(bundle, true);
    bundle = bundleService.getProductBundleById(2L);
    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "history", null, "publish", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(19, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "Unpublish" in view history tab
   */

  @Test
  public void testUnpublishProductBundleFilterInhistorytViewForProducts() {

    ProductBundle bundle = bundleService.getProductBundleById(2L);
    bundle.setPublish(false);
    bundleService.updateProductBundle(bundle, true);
    bundle = bundleService.getProductBundleById(2L);
    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "history", null, "unpublish",
          map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(1, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "Null" in view History tab
   */

  @Test
  public void testNullProductBundleFilterInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "history", null, null, map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(20, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "Empty" in view History tab
   */

  @Test
  public void testEmptyProductBundleFilterInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "history", null, "", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(20, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "Invalid" in view History tab
   */

  @Test
  public void testInvalidProductBundleFilterInHistoryViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "history", null, "Invalid", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "All" in view PlanNext tab
   */

  @Test
  public void testAllProductBundleFilterInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "planned", null, "All", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(20, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "publish" in view PlanNext tab
   */

  @Test
  public void testPublishProductBundleFilterInPlanNextViewForProducts() {

    ProductBundle bundle = bundleService.getProductBundleById(2L);
    bundle.setPublish(false);
    bundleService.updateProductBundle(bundle, true);
    bundle = bundleService.getProductBundleById(2L);
    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "planned", null, "publish", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(19, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "All" in view PlanNext tab
   */

  @Test
  public void testUnpublishProductBundleFilterInPlanNextViewForProducts() {

    ProductBundle bundle = bundleService.getProductBundleById(2L);
    bundle.setPublish(false);
    bundleService.updateProductBundle(bundle, true);
    bundle = bundleService.getProductBundleById(2L);
    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "planned", null, "unpublish",
          map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(1, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "Null" in view PlanNext tab
   */

  @Test
  public void testNullProductBundleFilterInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "planned", null, null, map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(20, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "Empty" in view PlanNext tab
   */

  @Test
  public void testEmptyProductBundleFilterInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "planned", null, "", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(20, count);
  }

  /*
   * @author Abhaik
   * @description : Verify filter for Product Bundles by State "Invalid" in view PlanNext tab
   */

  @Test
  public void testInvalidProductBundleFilterInPlanNextViewForProducts() {

    int count = 0;
    boolean hasNext = true;
    int page = 1;
    ModelMap map1 = new ModelMap();
    while (hasNext) {
      String result = bundleController.listProductBundles("b1c9fbb0-8dab-42dc-ae0a-ce1384a1e6",
          "003fa8ee-fba3-467f-a517-fd806dae8a80", Integer.toString(page), null, null, "planned", null, "Invalid", map1);
      Assert.assertNotNull(result);
      Assert.assertEquals("bundles.list", result);
      List<ProductBundle> productBundleList = (List<ProductBundle>) map1.get("productBundlesList");
      count = count + productBundleList.size();
      hasNext = (Boolean) map1.get("enableNext");
      page = page + 1;
    }
    Assert.assertEquals(0, count);
  }

  @Test
  public void testEditProductBundle() throws Exception {

    User user = userService.get("2ee0d598-00c8-4e12-b307-1cab5305e867");
    ServiceInstance serviceInstance = serviceInstanceDAO.find(1L);
    ServiceResourceType serviceResourceType = serviceResourceTypeDAO.find(1L);
    Map<String, String> serviceDiscriminatorMap = new HashMap<String, String>();

    serviceDiscriminatorMap.clear();
    serviceDiscriminatorMap.put("Template", "210");
    List<ProductBundleRevision> pbRevisions = bundleService.getProductBundleRevisions(serviceInstance,
        serviceResourceType, serviceDiscriminatorMap, null, user, null);
    Assert.assertNotNull("product bundle revision list cannot be null", pbRevisions);

    // Verifying presence of a Bundle in which INCLUDES constrains matches
    ProductBundle productBundle = bundleService.getProductBundleById(2L);

    Revision futureRevision = channelService.getFutureRevision(null);
    List<ProvisioningConstraint> provisioningConstraints = bundleService.getProductBundleRevision(productBundle,
        futureRevision.getStartDate(), null).getProvisioningConstraints();

    Assert.assertNotNull(provisioningConstraints);
    Assert.assertFalse(provisioningConstraints.isEmpty());

    String compAssociationJson = "[{compDbId: 1, association: INCLUDES, compName: ISO, compValue: DOS-ISO, compValueName: 10 }]";

    ProductBundleForm form = new ProductBundleForm(productBundle);
    form.setCompAssociationJson(compAssociationJson);
    BindingResult result = validate(form);

    ProductBundle obtainedProductBundle = bundleController.editProductBundle(form, result, map);
    Assert.assertNotNull(obtainedProductBundle);
    provisioningConstraints = bundleService.getProductBundleRevision(obtainedProductBundle, new Date(), null)
        .getProvisioningConstraints();
    Assert.assertNotNull(provisioningConstraints);
    Assert.assertFalse(provisioningConstraints.isEmpty());

  }

  @Test
  public void testGetComponentsContainsFilters() {
    Map<String, List<String>> componentCollection = bundleController.getServiceResourceComponents(1L);
    Assert.assertNotNull(componentCollection);
    Assert.assertTrue(componentCollection.get("filters").contains("zone"));
  }
}
