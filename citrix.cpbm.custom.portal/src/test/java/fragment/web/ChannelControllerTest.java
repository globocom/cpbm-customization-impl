/*
 * Copyright © 2013 Citrix Systems, Inc. You may not use, copy, or modify this file except pursuant to a valid license
 * agreement from Citrix Systems, Inc.
 */
package fragment.web;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import web.WebTestsBase;

import com.citrix.cpbm.platform.admin.service.ConnectorConfigurationManager;
import com.citrix.cpbm.portal.fragment.controllers.ChannelController;
import com.citrix.cpbm.portal.fragment.controllers.ProductBundlesController;
import com.citrix.cpbm.portal.fragment.controllers.ProductsController;
import com.vmops.model.AccountType;
import com.vmops.model.Address;
import com.vmops.model.Catalog;
import com.vmops.model.Channel;
import com.vmops.model.ChannelRevision;
import com.vmops.model.Configuration;
import com.vmops.model.CurrencyValue;
import com.vmops.model.Product;
import com.vmops.model.ProductBundle;
import com.vmops.model.ProductBundleRevision;
import com.vmops.model.ProductCharge;
import com.vmops.model.ProductRevision;
import com.vmops.model.RateCard;
import com.vmops.model.RateCardCharge;
import com.vmops.model.Revision;
import com.vmops.model.ServiceInstance;
import com.vmops.model.ServiceResourceType;
import com.vmops.model.ServiceResourceType.ResourceConstraint;
import com.vmops.model.SupportedCurrency;
import com.vmops.model.Tenant;
import com.vmops.model.User;
import com.vmops.persistence.CatalogProductBundleDAO;
import com.vmops.persistence.ChannelDAO;
import com.vmops.persistence.ChargeRecurrenceFrequencyDAO;
import com.vmops.persistence.RateCardComponentDAO;
import com.vmops.persistence.RevisionDAO;
import com.vmops.persistence.ServiceConfigurationMetaDataDAO;
import com.vmops.persistence.ServiceInstanceDao;
import com.vmops.persistence.ServiceResourceTypeDAO;
import com.vmops.service.ChannelService;
import com.vmops.service.ConfigurationService;
import com.vmops.service.CurrencyValueService;
import com.vmops.service.ProductBundleService;
import com.vmops.service.ProductService;
import com.vmops.service.exceptions.ChannelEditFailedException;
import com.vmops.service.exceptions.CurrencyPrecisionException;
import com.vmops.utils.DateUtils;
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.forms.ChannelLogoForm;
import com.vmops.web.forms.ChannelServiceSetting;
import com.vmops.web.forms.ChannelServiceSettingsForm;
import com.vmops.web.forms.ProductBundleForm;
import com.vmops.web.forms.ProductForm;
import com.vmops.web.forms.RateCardChargesForm;
import com.vmops.web.forms.RateCardComponentChargesForm;

@SuppressWarnings({
    "deprecation", "unchecked"
})
public class ChannelControllerTest extends WebTestsBase {

  private ModelMap map;

  private HttpServletResponse response;

  private HttpServletRequest request;

  @Autowired
  private ConnectorConfigurationManager connectorConfigurationManager;

  @Autowired
  private ChannelService channelService;

  @Autowired
  ChannelController channelController;

  @Autowired
  ProductBundleService bundleService;

  @Autowired
  CurrencyValueService currencyService;

  @Autowired
  CatalogProductBundleDAO catalogBundleDAO;

  @Autowired
  ProductService productService;

  @Autowired
  private RateCardComponentDAO rateCardComponentDAO;

  @Autowired
  ChannelDAO channelDAO;

  @Autowired
  RevisionDAO revisionDAO;

  @Autowired
  private ProductBundlesController bundleController;

  @Autowired
  ProductBundlesController productBundlesController;

  @Autowired
  ProductsController productsController;

  @Autowired
  ServiceInstanceDao serviceInstanceDAO;

  @Autowired
  ServiceResourceTypeDAO serviceResourceTypeDAO;

  @Autowired
  ChargeRecurrenceFrequencyDAO chargeRecurrenceFrequencyDAO;

  @Autowired
  ConfigurationService configurationService;

  @Autowired
  ServiceConfigurationMetaDataDAO serviceConfigurationMetaDataDAO;

  @Before
  public void init() throws Exception {
    map = new ModelMap();
    response = new MockHttpServletResponse();
    request = new MockHttpServletRequest();
  }

  /*
   * Description: createChannelWithNameAsBlank Author: VeeramaniT
   */
  @Test
  public void testCreateChannelWithNameAsBlank() {

    try {
      String[] currencyvaluelist = {
          "USD", "EUR"
      };
      channelController.createChannel(null, "Veera", "Veera", currencyvaluelist, map, response);
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("could not insert: [com.vmops.model.Catalog];"));
    }
  }

  /*
   * Description: createChannelWithCodeAsBlank Author: VeeramaniT
   */
  @Test(expected = Exception.class)
  public void testCreateChannelWithCodeAsBlank() {
    String[] currencyvaluelist = {
        "USD", "EUR"
    };
    channelController.createChannel("Veera", "Veera", null, currencyvaluelist, map, response);
  }

  /*
   * Description: Shouldn't able to Add channel with some Long Character(100) as code Author: VeeramaniT
   */
  @Test
  public void testCreateChannelWithCodeAsLongChar() {
    try {
      String[] currencyvaluelist = {
          "USD", "EUR"
      };
      String code = "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghij";
      channelController.createChannel("Veera", "Veera", code, currencyvaluelist, map, response);
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("could not insert: [com.vmops.model.Channel];"));
    }
  }

  /*
   * Description: User Should able to edit channel name field. Author: VeeramaniT
   */
  @Test
  public void testEditChannelName() {

    Channel existingChannel = channelDAO.find("3");
    String result = channelController.editChannel(existingChannel.getId().toString(), "NewChannelName",
        existingChannel.getDescription(), existingChannel.getCode(), map);
    Assert.assertNotNull(result);
    Assert.assertEquals("channels.view", result);
    Channel editedChannel = (Channel) map.get("channel");
    Assert.assertEquals("NewChannelName", editedChannel.getName());

  }

  @Test
  public void testEditChannelNameWithDefaultConfigSet() {
    Channel existingChannel = channelDAO.find("3");
    Configuration defaultChannConfiguration = configurationService
        .locateConfigurationByName("com.citrix.cpbm.accountManagement.onboarding.default.channel");
    defaultChannConfiguration.setValue(existingChannel.getName());
    configurationService.update(defaultChannConfiguration);
    String result = channelController.editChannel(existingChannel.getId().toString(), "NewChannelName",
        existingChannel.getDescription(), existingChannel.getCode(), map);

    defaultChannConfiguration = configurationService
        .locateConfigurationByName("com.citrix.cpbm.accountManagement.onboarding.default.channel");
    Assert.assertEquals("NewChannelName", defaultChannConfiguration.getValue());

    Assert.assertNotNull(result);
    Assert.assertEquals("channels.view", result);
    Channel editedChannel = (Channel) map.get("channel");
    Assert.assertEquals("NewChannelName", editedChannel.getName());

  }

  /*
   * Description : User should able to edit channel code field Author : VeeramaniT
   */
  @Test
  public void testEditchannelCode() {
    Channel existingChannel = channelDAO.find("3");
    String result = channelController.editChannel(existingChannel.getId().toString(), existingChannel.getName(),
        existingChannel.getDescription(), "NewCode", map);
    Assert.assertNotNull(result);
    Assert.assertEquals("channels.view", result);
    Channel codechannel = (Channel) map.get("channel");
    Assert.assertEquals("NewCode", codechannel.getCode());
  }

  /*
   * Description : User shouldn't be able to edit a channel with blank name. Author : VeeramaniT
   */
  @Test(expected = Exception.class)
  public void testEditChannelWithBlankName() {
    Channel existingChannel = channelDAO.find("3");
    String result = channelController.editChannel(existingChannel.getId().toString(), null, "Veera", "Veera", map);
    Assert.assertNotNull(result);
  }

  /*
   * Description : User shouldn't be able to edit a channel with blank name, code and description field while saving.
   * Author : VeeramaniT
   */
  @Test(expected = Exception.class)
  public void testEditChannelWithBlankValues() {
    Channel existingChannel = channelDAO.find("3");
    String result = channelController.editChannel(existingChannel.getId().toString(), "Veera", null, null, map);
    Assert.assertNotNull(result);
    Assert.assertEquals("channels.view", result);
  }

  /*
   * Description : User should be able to delete a channel which does not have any account Author : VeeramaniT
   */
  @Test
  public void testDeleteChannelWithNoAccount() {
    String[] currencyValueList = {
        "USD", "EUR"
    };
    Channel obtainedchannel = channelController.createChannel("Veera", "Veera", "Veera", currencyValueList, map,
        response);
    List<Channel> beforechannellist = channelService.getChannels();
    String deleteChannelID = obtainedchannel.getId().toString();
    String result = channelController.deletechannel(deleteChannelID, map);
    Assert.assertNotNull(result);
    Assert.assertEquals("success", result);
    List<Channel> afterchannellist = channelService.getChannels();
    Assert.assertEquals(beforechannellist.size() - 1, afterchannellist.size());

  }

  /*
   * Description : User should be able to edit valid Catalog image Author : VeeramaniT
   */
  @Test
  public void testEditValidCatalogImage() throws Exception {
    Channel existingChannel = channelDAO.find("3");
    ChannelLogoForm form = new ChannelLogoForm(existingChannel);
    MultipartFile logo = new MockMultipartFile("channelLogo.jpeg", "channelLogo.jpeg", "byte", "ChannelLogo".getBytes());
    form.setLogo(logo);
    BindingResult result = validate(form);
    String result1 = channelController.editChannelLogo(form, result, request, map);
    System.out.println(result1);
    Assert.assertTrue(result1.contains(existingChannel.getId().toString()));

  }

  /*
   * Description : /* Description : User shouldn't be able to edit catalog image in case user selects non-image file
   * Author : VeeramaniT
   */
  @Test
  public void testEditInvalidChannelImage() throws Exception {
    Channel existingChannel = channelDAO.find("3");
    ChannelLogoForm form = new ChannelLogoForm(existingChannel);
    MultipartFile logo = new MockMultipartFile("veer.jee", "veer.jee", "bytes", "veer.jee".getBytes());
    form.setLogo(logo);
    BindingResult result = validate(form);
    String result1 = channelController.editChannelLogo(form, result, request, map);
    Assert.assertNotNull(result1);
    Assert.assertEquals("{\"errormessage\":\"File should have either .jpeg/.jpg/.png/.gif/.bmp extension\"}", result1);

  }

  /*
   * Description : /* Description : User should be able to add one currency to an added channel Author : VeeramaniT
   */
  @Test
  public void testCreateChannelWithOneCurrency() {

    String[] currencyValueList = {
      "USD"
    };
    List<Channel> bchannel = channelService.getChannels();
    Channel obtainedchannel = channelController.createChannel("Veera", "Veera", "Veera", currencyValueList, map,
        response);
    List<Channel> achannel = channelService.getChannels();
    Assert.assertNotNull(obtainedchannel);
    Assert.assertEquals("Veera", obtainedchannel.getName());
    Assert.assertEquals(bchannel.size() + 1, achannel.size());

  }

  /*
   * Description : User should be able to add multiple currencies to an added channel. Author : VeeramaniT
   */
  @Test
  public void testCreateChannelWithMultipleCurrency() {

    String[] currencyValueList = {
        "USD", "EUR", "GBP", "INR"
    };
    List<Channel> bchannel = channelService.getChannels();
    Channel obtainedChannel = channelController.createChannel("Veera", "Veera", "Veera", currencyValueList, map,
        response);
    List<Channel> achannel = channelService.getChannels();
    Assert.assertNotNull(obtainedChannel);
    Assert.assertEquals("Veera", obtainedChannel.getName());
    Assert.assertEquals(bchannel.size() + 1, achannel.size());
  }

  /*
   * Description : Adding published bundle to the channel. Author : Rajkumart
   */
  @Test
  public void testAddpublishedBundleToChannel() throws JSONException {
    String[] currencyValueList = {
        "USD", "EUR", "GBP", "INR"
    };
    List<Channel> bchannel = channelService.getChannels();
    Channel obtainedChannel = channelController.createChannel("Veera", "Veera", "Veera", currencyValueList, map,
        response);
    List<Channel> achannel = channelService.getChannels();
    Assert.assertNotNull(obtainedChannel);
    Assert.assertEquals("Veera", obtainedChannel.getName());
    Assert.assertEquals(bchannel.size() + 1, achannel.size());

    ChannelRevision futureChannelRevision = channelService.getFutureChannelRevision(obtainedChannel, false);
    List<String> bundleIdsAlreadyAddedToChannel = new ArrayList<String>();
    for (ProductBundleRevision productBundleRevision : futureChannelRevision.getProductBundleRevisions()) {
      bundleIdsAlreadyAddedToChannel.add(productBundleRevision.getProductBundle().getId().toString());
    }
    Assert.assertEquals(0, bundleIdsAlreadyAddedToChannel.size());

    String result = channelController.attachProductBundles(obtainedChannel.getId().toString(), "[2]", map);
    Assert.assertEquals("success", result);

    ChannelRevision futureChannelRevision1 = channelService.getFutureChannelRevision(obtainedChannel, false);
    List<String> bundleIdsAlreadyAddedToChannel1 = new ArrayList<String>();
    for (ProductBundleRevision productBundleRevision : futureChannelRevision1.getProductBundleRevisions()) {
      bundleIdsAlreadyAddedToChannel1.add(productBundleRevision.getProductBundle().getId().toString());
    }
    Assert.assertEquals(1, bundleIdsAlreadyAddedToChannel1.size());

  }

  /*
   * Description : ChannelFlow. Author : VeeramaniT
   */

  @Test
  public void testChannelFlowAsRoot() throws JSONException {

    // Step1: Creating a new Channel

    String[] currencyValueList = {
        "USD", "EUR", "GBP"
    };
    int beforeChannelCount = channelService.getChannelCount();
    Channel obtainedChannel = channelController.createChannel("Veera", "Veera", "Veera", currencyValueList, map,
        response);
    Assert.assertNotNull(obtainedChannel);
    int afterChannelCount = channelService.getChannelCount();
    Assert.assertEquals(beforeChannelCount + 1, afterChannelCount);

    // Step2 : Attaching bundle to the channels

    String selectedProductBundles = "[2]";
    channelController.attachProductBundles(obtainedChannel.getId().toString(), selectedProductBundles, map);
    channelController.listbundles(obtainedChannel.getId().toString(), map);
    List<ProductBundleRevision> productbundles = (List<ProductBundleRevision>) map.get("productBundles");
    for (ProductBundleRevision productbundleRevision : productbundles) {
      ProductBundle productBundle = productbundleRevision.getProductBundle();
      Assert.assertTrue(productBundle.getId() != 2L);
    }

    // Step 3 : Edit the Bundle pricing in the channel for the added bundle

    String currencyValData = "[{\"previousvalue\":\"0.0000\",\"value\":\"5000\",\"currencycode\":\"EUR\",\"currencyId\":\"44\",\"isRecurring\":\"0\"}]";
    channelController.editCatalogProductBundlePricing(obtainedChannel.getId().toString(), "2", currencyValData, map);
    channelController.getFullChargeListing(obtainedChannel.getId().toString(), "planned", "2", null,
        DateUtils.getSimpleDateString(new java.util.Date()), map);
    Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = (Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>>) map
        .get("fullBundlePricingMap");

    Assert.assertNotNull(map.get("productBundleRevision"));
    for (Map.Entry<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> map1 : fullBundlePricingMap
        .entrySet()) {
      for (Map.Entry<CurrencyValue, Map<String, RateCardCharge>> map2 : map1.getValue().entrySet()) {
        CurrencyValue cv = map2.getKey();
        if (cv.getCurrencyCode().equalsIgnoreCase("EUR")) {
          for (Map.Entry<String, RateCardCharge> map3 : map2.getValue().entrySet()) {
            String str = map3.getKey();
            RateCardCharge rcc = map3.getValue();
            if (str.equalsIgnoreCase("catalog-onetime")) {
              Assert.assertEquals(BigDecimal.valueOf(5000), rcc.getPrice());
            }
          }
        }
      }
    }

    // Checking that newly edited price is not affected in bundle price list

    ProductBundle pb = bundleService.getProductBundleById(2L);
    productBundlesController.viewBundlePlannedCharges(pb.getCode(), map, "");
    RateCardChargesForm rateCardChargesForm = (RateCardChargesForm) map.get("rateCardChargesForm");
    List<RateCardComponentChargesForm> rateCardComponentChargesFormList = rateCardChargesForm
        .getNonRecurringRateCardChargesFormList();
    for (RateCardComponentChargesForm rcccform : rateCardComponentChargesFormList) {
      List<RateCardCharge> rccList = rcccform.getCharges();
      for (RateCardCharge rcc : rccList) {
        if (rcc.getCurrencyValue().getCurrencyCode().equalsIgnoreCase("EUR")) {
          Assert.assertTrue(rcc.getPrice() != BigDecimal.valueOf(5000));
        }
      }

    }
  }

  /*
   * Description : User shouldn't be able to edit "Next Planned Charges" date as previous date.. Author : VeeramaniT
   */
  @Test
  public void editNextPlannedChargesDateAsPreviousDate() throws ParseException {

    Channel ch = channelDAO.find("1");
    Date yesterday = DateUtils.minusOneDay(new Date());
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    String s = channelController.changePlanDate(ch.getId().toString(), sdf.format(yesterday), "yyyy-MM-dd", map);
    Assert.assertTrue(s.equalsIgnoreCase("failure"));
  }

  /*
   * Description :User shouldn't be able to edit "Next Planned Charges" date as null. Author : VeeramaniT
   */

  @Test(expected = NullPointerException.class)
  public void editNextPlannedChargesDateAsNull() throws ParseException {
    Channel ch1 = channelDAO.find("1");
    channelController.changePlanDate(ch1.getId().toString(), null, "yyyy-MM-dd", map);
  }

  /*
   * Description :User shouldn't be able to edit "Next Planned Charges" date with Invalid format e.g. 1/1/203 Author :
   * VeeramaniT
   */

  @Test
  public void editNextPlannedChargesDateAsInvalid() throws ParseException {
    Channel ch2 = channelDAO.find("1");
    SimpleDateFormat sdf = new SimpleDateFormat("mm-dd");
    try {
      channelController.changePlanDate(ch2.getId().toString(), sdf.format(new Date()), "yyyy-MM-dd", map);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      Assert.assertTrue(e.getMessage().contains("Unparseable date:"));
    }
  }

  /*
   * Description :User should be able to add multiple published bundles to a channel Author : VeeramaniT
   */
  @Test
  public void addMultipleBundleToChannel() throws JSONException {
    String selectedProductBundles = "[2,3,4]";
    Channel ch1 = channelDAO.find("1");
    channelController.attachProductBundles(ch1.getId().toString(), selectedProductBundles, map);
    channelController.listbundles(ch1.getId().toString(), map);
    List<ProductBundleRevision> productbundles = (List<ProductBundleRevision>) map.get("productBundles");
    for (ProductBundleRevision productbundleRevision : productbundles) {
      ProductBundle productBundle = productbundleRevision.getProductBundle();
      Assert.assertTrue(productBundle.getId() != 2L && productBundle.getId() != 3L && productBundle.getId() != 4L);
    }
  }

  /*
   * Description :User shouldn't be able to edit prices with blank price under catalog tab for a channel Author :
   * VeeramaniT
   */
  @Test(expected = NumberFormatException.class)
  public void editChannelPricingWithBlankPrice() throws JSONException {
    String currencyValData = "[{\"previousvalue\":\"0.0000\",\"value\":\"\",\"currencycode\":\"EUR\",\"currencyId\":\"44\",\"isRecurring\":\"0\"}]";

    Channel ch1 = channelDAO.find("1");
    channelController.editCatalogProductBundlePricing(ch1.getId().toString(), "2", currencyValData, map);
  }

  /*
   * Description :User shouldn't be able to edit prices with negative price under catalog tab for a channel Author :
   * VeeramaniT
   */
  @Test
  public void editChannelPricingWithNegativeprice() throws JSONException {

    String currencyValData = "[{\"previousvalue\":\"0.0000\",\"value\":\"-5\",\"currencycode\":\"USD\",\"currencyId\":\"149\",\"isRecurring\":\"0\"}]";

    Channel ch1 = channelDAO.find("3");
    channelController.editCatalogProductBundlePricing(ch1.getId().toString(), "3", currencyValData, map);

  }

  /*
   * Description :As a user I should not able to Add / edit duplicate channel name. Author : VeeramaniT
   */

  @Test(expected = ChannelEditFailedException.class)
  public void editDuplicateChannelName() {
    Channel ch1 = channelDAO.find("3");
    Channel ch2 = channelDAO.find("4");
    String channelName = ch2.getName();
    channelController.editChannel(ch1.getId().toString(), channelName, "Desc_Channel2", "Veera", map);
  }

  /*
   * Description :As a user I should not able to Add / edit duplicate channel Code. Author : VeeramaniT
   */
  @Test(expected = ChannelEditFailedException.class)
  public void editDuplicateChannelCode() {
    Channel ch1 = channelDAO.find("3");
    Channel ch2 = channelDAO.find("4");
    String channelCode = ch2.getCode();
    channelController.editChannel(ch1.getId().toString(), "Veera", "Desc_Channel2", channelCode, map);
  }

  /*
   * Description : ChannelFlowAsproductManager. Author : VeeramaniT
   */

  @Test
  public void testChannelFlowAsProductManager() throws JSONException {

    // Login as Product Manager
    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);

    // Step1: Creating a new Channel

    String[] currencyValueList = {
        "USD", "EUR", "GBP"
    };
    int beforeChannelCount = channelService.getChannelCount();
    Channel obtainedChannel = channelController.createChannel("Veera", "Veera", "Veera", currencyValueList, map,
        response);
    Assert.assertNotNull(obtainedChannel);
    int afterChannelCount = channelService.getChannelCount();
    Assert.assertEquals(beforeChannelCount + 1, afterChannelCount);
    // Step2 : Attaching bundle to the channels

    String selectedProductBundles = "[2]";
    channelController.attachProductBundles(obtainedChannel.getId().toString(), selectedProductBundles, map);
    channelController.listbundles(obtainedChannel.getId().toString(), map);
    List<ProductBundleRevision> productbundles = (List<ProductBundleRevision>) map.get("productBundles");
    for (ProductBundleRevision productbundleRevision : productbundles) {
      ProductBundle productBundle = productbundleRevision.getProductBundle();
      Assert.assertTrue(productBundle.getId() != 2L);
    }

    // Step 3 : Edit the Bundle pricing in the channel for the added bundle

    String currencyValData = "[{\"previousvalue\":\"0.0000\",\"value\":\"5000\",\"currencycode\":\"EUR\",\"currencyId\":\"44\",\"isRecurring\":\"0\"}]";
    channelController.editCatalogProductBundlePricing(obtainedChannel.getId().toString(), "2", currencyValData, map);
    channelController.getFullChargeListing(obtainedChannel.getId().toString(), "planned", "2", null,
        DateUtils.getSimpleDateString(new java.util.Date()), map);
    Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = (Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>>) map
        .get("fullBundlePricingMap");

    Assert.assertNotNull(map.get("productBundleRevision"));
    for (Map.Entry<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> map1 : fullBundlePricingMap
        .entrySet()) {
      for (Map.Entry<CurrencyValue, Map<String, RateCardCharge>> map2 : map1.getValue().entrySet()) {
        CurrencyValue cv = map2.getKey();
        if (cv.getCurrencyCode().equalsIgnoreCase("EUR")) {
          for (Map.Entry<String, RateCardCharge> map3 : map2.getValue().entrySet()) {
            String str = map3.getKey();
            RateCardCharge rcc = map3.getValue();
            if (str.equalsIgnoreCase("catalog-onetime")) {
              Assert.assertEquals(BigDecimal.valueOf(5000), rcc.getPrice());
            }
          }
        }
      }
    }

    // Checking that newly edited price is not affected in bundle price list

    ProductBundle pb = bundleService.getProductBundleById(2L);
    productBundlesController.viewBundlePlannedCharges(pb.getCode(), map, "");
    RateCardChargesForm rateCardChargesForm = (RateCardChargesForm) map.get("rateCardChargesForm");
    List<RateCardComponentChargesForm> rateCardComponentChargesFormList = rateCardChargesForm
        .getNonRecurringRateCardChargesFormList();
    for (RateCardComponentChargesForm rcccform : rateCardComponentChargesFormList) {
      List<RateCardCharge> rccList = rcccform.getCharges();
      for (RateCardCharge rcc : rccList) {
        if (rcc.getCurrencyValue().getCurrencyCode().equalsIgnoreCase("EUR")) {
          Assert.assertTrue(rcc.getPrice() != BigDecimal.valueOf(5000));
        }
      }

    }
  }

  /*
   * Description : As a PM, I should not able to Delete inuse channels. Author : VeeramaniT
   */
  @Test
  public void deleteInUseChannels() {
    // Login as Product Manager

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);

    // trying to delete a channel which is in use.

    User us = userDAO.find("3");
    Channel ch = us.getSourceChannel();
    int beforeChannelCount = channelService.getChannelCount();
    String s = channelController.deletechannel(ch.getId().toString(), map);
    int afterChannelCount = channelService.getChannelCount();
    Assert.assertNotNull(s);
    Assert.assertEquals(beforeChannelCount, afterChannelCount);
  }

  /*
   * Description : Newly created channel should list , while adding new tenant account as root user. Author : VeeramaniT
   */
  @Test
  public void newChannelShouldListOnNewAccount() {
    // Create a New Channel
    String[] currencyValueList = {
        "USD", "EUR", "GBP"
    };
    int beforeChannelCount = channelService.getChannelCount();
    Channel obtainedChannel = channelController.createChannel("Veera", "Veera", "Veera", currencyValueList, map,
        response);
    Assert.assertNotNull(obtainedChannel);
    int afterChannelCount = channelService.getChannelCount();
    Assert.assertEquals(beforeChannelCount + 1, afterChannelCount);

    // Create an Account through new Channel

    String masterUsername = "Veeramani";
    User masterUser = new User("Veeramani", " Thamizharasan", " veeramani.thamizharasan@citrix.com", masterUsername,
        "Portal123#", "9535113532", "UTC", null, null, null);
    AccountType accountType = accountTypeDAO.getAccountTypeByName("RETAIL");
    Address address = new Address("123", "street2", "city", "state", "postalCode", "country");
    Tenant tenant = new Tenant(masterUsername, accountType, masterUser, address, true,
        currencyValueService.locateBYCurrencyCode("USD"), masterUser);
    String channelParam = obtainedChannel.getParam();
    tenant.setCreatedAt(new Date());
    tenant.setCreatedBy(userService.getUserByParam("id", 2, false));
    tenant.setUpdatedBy(userService.getUserByParam("id", 2, false));
    String accountTypeId = accountType.getId().toString();
    Tenant tn = tenantService.createAccount(tenant, masterUser, channelParam, accountTypeId);
    Assert.assertEquals(obtainedChannel.getCatalog(), tn.getCatalog());
  }

  /*
   * Description : Add new channel should fail with "no currency" Author : VeeramaniT
   */
  @Test(expected = NullPointerException.class)
  public void createChannelWithNoCurrency() {
    channelController.createChannel("Veera", "Veera", "Veera", null, map, response);
  }

  /*
   * Description : User shouldn't be able to add Channel with blank channel name. Author : VeeramaniT
   */
  @Test
  public void addChannelWithBlankChannelName() {
    try {
      String[] currencyValueList = {
          "USD", "EUR"
      };
      channelController.createChannel(null, "Veera", "Veera", currencyValueList, map, response);
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("could not insert:"));

    }
  }

  /*
   * Description : Product Manager shouldn't be able to add unpublished product bundle to an active channel Author :
   * VeeramaniT
   */
  @Test
  public void testProductManagerShouldNotAddUnpublishedBundleToChannel() throws Exception {
    // Login as a product Manager
    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    Channel obtainedChannel = channelDAO.find(4L);
    // Unpublishing a Bundle
    ProductBundle pb = bundleService.locateProductBundleById("2");
    String result = productBundlesController.publishBundle(pb.getCode(), "false", map);
    Assert.assertNotNull(result);
    Assert.assertEquals("success", result);
    Assert.assertEquals(false, pb.getPublish().booleanValue());

    // Attaching unpublished product bundle to the new channels
    String selectedProductBundles = "[" + pb.getId().toString() + "]";
    String result1 = channelController.attachProductBundles(obtainedChannel.getId().toString(), selectedProductBundles,
        map);
    Assert.assertNotNull(result1);
    String Listbundles = channelController.listbundles(obtainedChannel.getId().toString(), map);
    Assert.assertNotNull(Listbundles);
    List<ProductBundleRevision> productbundles = (List<ProductBundleRevision>) map.get("productBundles");
    for (ProductBundleRevision productbundleRevision : productbundles) {
      ProductBundle productBundle = productbundleRevision.getProductBundle();
      Assert.assertTrue(productBundle.getId() != pb.getId());
    }
  }

  @Test
  public void testEditCatalogProductBundlePricing() {
    Channel channel = channelDAO.find(4L);
    ProductBundle bundle = bundleService.getProductBundleById(2L);
    Boolean found = false;

    channelController.editCatalogProductBundlePricing(channel.getId().toString(), bundle.getId().toString(), map);

    List<CurrencyValue> actualCurrencyList = (List<CurrencyValue>) map.get("supportedCurrencies");
    List<CurrencyValue> supportedCurrencyList = channelService.listCurrencies(channel.getParam());

    for (int i = 0; i < supportedCurrencyList.size(); i++) {
      Assert.assertEquals(supportedCurrencyList.get(i).getCurrencyCode(), actualCurrencyList.get(i).getCurrencyCode());
    }

    Assert.assertEquals(channel, map.get("channel"));

    Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = (Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>>) map
        .get("fullBundlePricingMap");
    Map<String, RateCardCharge> rateCardMap = fullBundlePricingMap.get(
        bundleService.getProductBundleRevision(bundle, channelService.getFutureRevision(channel).getStartDate(),
            channel)).get(currencyValueService.locateBYCurrencyCode("JPY"));
    for (Map.Entry<String, RateCardCharge> entry : rateCardMap.entrySet()) {
      if (rateCardComponentDAO.find(12L).getId().toString()
          .equals(entry.getValue().getRateCardComponent().getId().toString())) {
        BigDecimal charge = new BigDecimal("400.0000");
        Assert.assertEquals(charge, entry.getValue().getPrice());
        found = true;
        break;
      }
    }
    Assert.assertTrue(found);

    ProductBundleRevision productBundleRevision = (ProductBundleRevision) map.get("productBundleRevision");
    Assert.assertEquals(
        channelService.getFutureChannelRevision(channel, false).getProductBundleRevisionsMap().get(bundle).getId(),
        productBundleRevision.getId());

  }

  @Test
  public void testViewCatalogCurrent() {
    boolean found = false;
    Channel channel = channelDAO.find(4L);
    Product product = productDAO.find(4L);
    CurrencyValue currencyValue = currencyService.locateBYCurrencyCode("JPY");
    ProductBundle bundle = bundleService.getProductBundleById(2L);

    channelController.viewCatalogCurrent(channel.getId().toString(), "1", "5", map);

    Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> fullProductPricingMap = (Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>>) map
        .get("fullProductPricingMap");
    Map<String, ProductCharge> productChargeMap = fullProductPricingMap.get(product).get(currencyValue);
    for (Map.Entry<String, ProductCharge> entry : productChargeMap.entrySet()) {
      ProductCharge productCharge = entry.getValue();
      if (productCharge.getProduct().compareTo(product) == 0) {
        Assert.assertEquals(productCharge.getProduct().getPrice(), product.getPrice());
        found = true;
      }
    }
    Assert.assertTrue(found);
    found = false;

    Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = (Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>>) map
        .get("fullBundlePricingMap");
    Map<String, RateCardCharge> rateCardMap = fullBundlePricingMap.get(
        bundleService.getProductBundleRevision(bundle, channelService.getCurrentRevision(channel).getStartDate(),
            channel)).get(currencyValueService.locateBYCurrencyCode("JPY"));
    for (Map.Entry<String, RateCardCharge> entry : rateCardMap.entrySet()) {
      if (rateCardComponentDAO.find(12L).getId().toString()
          .equals(entry.getValue().getRateCardComponent().getId().toString())) {
        BigDecimal charge = new BigDecimal("4000.0000");
        Assert.assertEquals(charge, entry.getValue().getPrice());
        found = true;
        break;
      }
    }
    Assert.assertTrue(found);

    List<CurrencyValue> supportedCurrencies = (List<CurrencyValue>) map.get("supportedCurrencies");
    int index = 0;
    for (CurrencyValue c : channelService.listCurrencies(channel.getParam())) {
      Assert.assertEquals(c.getCurrencyCode(), supportedCurrencies.get(index).getCurrencyCode());
      index++;
    }

    List<ProductBundleRevision> productBundleRevisionList = channelService.getChannelRevision(channel,
        channelService.getCurrentRevision(channel).getStartDate(), false).getProductBundleRevisions();
    List<ProductBundleRevision> actualProductBundleRevisionList = (List<ProductBundleRevision>) map
        .get("productBundleRevisions");
    Assert.assertEquals(productBundleRevisionList.get(0).getId(), actualProductBundleRevisionList.get(0).getId());

    Assert.assertEquals(channel, map.get("channel"));

  }

  @Test
  public void testViewCatalogPlanned() {
    Boolean found = false;
    Channel channel = channelDAO.find(4L);
    Product product = productDAO.find(4L);
    ProductBundle bundle = bundleService.getProductBundleById(2L);
    CurrencyValue currencyValue = currencyService.locateBYCurrencyCode("JPY");

    channelController.viewCatalogPlanned(channel.getId().toString(), "1", "5", null, map);

    List<CurrencyValue> supportedCurrencies = (List<CurrencyValue>) map.get("supportedCurrencies");
    int index = 0;
    for (CurrencyValue c : channelService.listCurrencies(channel.getParam())) {
      Assert.assertEquals(c.getCurrencyCode(), supportedCurrencies.get(index).getCurrencyCode());
      index++;
    }
    Assert.assertEquals(channel, map.get("channel"));

    Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> fullProductPricingMap = (Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>>) map
        .get("fullProductPricingMap");
    Map<String, ProductCharge> productChargeMap = fullProductPricingMap.get(product).get(currencyValue);
    for (Map.Entry<String, ProductCharge> entry : productChargeMap.entrySet()) {
      ProductCharge productCharge = entry.getValue();
      if (productCharge.getProduct().compareTo(product) == 0) {
        Assert.assertEquals(productCharge.getProduct().getPrice(), product.getPrice());
        found = true;
      }
    }
    Assert.assertTrue(found);

    Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = (Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>>) map
        .get("fullBundlePricingMap");
    Map<String, RateCardCharge> rateCardMap = fullBundlePricingMap.get(
        bundleService.getProductBundleRevision(bundle, channelService.getFutureRevision(channel).getStartDate(),
            channel)).get(currencyValueService.locateBYCurrencyCode("JPY"));
    for (Map.Entry<String, RateCardCharge> entry : rateCardMap.entrySet()) {
      if (rateCardComponentDAO.find(12L).getId().toString()
          .equals(entry.getValue().getRateCardComponent().getId().toString())) {
        BigDecimal charge = new BigDecimal("400.0000");
        Assert.assertEquals(charge, entry.getValue().getPrice());
        found = true;
        break;
      }
    }
    Assert.assertTrue(found);

  }

  @Test
  public void testList() {
    try {

      String channelName = channelDAO.find(4L).getName();
      org.apache.commons.configuration.Configuration config = new PropertiesConfiguration("pagination.properties");
      int perPage = config.getInt("DEFAULT_PAGE_SIZE");
      List<Channel> channels = channelService.getChannels(1, perPage, channelName.substring(0, 4));

      channelController.list("1", channelName.substring(0, 4), map);

      Assert.assertEquals(channels, map.get("channels"));
      Assert.assertEquals(channels.size(), map.get("channelsize"));
      Assert.assertEquals(1, map.get("current_page"));

      int totalSize = channelService.count(null);
      if (totalSize - (1 * perPage) > 0) {
        Assert.assertTrue((Boolean) map.get("enable_next"));
      } else {
        Assert.assertFalse((Boolean) map.get("enable_next"));
      }

      channelController.searchChannelByPattern("1", channelName.substring(0, 4), map);
      Assert.assertEquals(channels, map.get("channels"));
      Assert.assertEquals(channels.size(), map.get("channelsize"));
      Assert.assertEquals(1, map.get("current_page"));

      if (totalSize - (1 * perPage) > 0) {
        Assert.assertTrue((Boolean) map.get("enable_next"));
      } else {
        Assert.assertFalse((Boolean) map.get("enable_next"));
      }

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testEditChannel() {
    try {
      Channel channel = channelDAO.find(4L);
      channelController.editChannel(channel.getId().toString(), map);
      Assert.assertEquals(channel, map.get("channel"));
      Assert.assertEquals(channelService.listCurrencies(channel.getParam()), map.get("currencies"));
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testEditChannelPost() {
    try {
      Channel channel = channelDAO.find(4L);
      String channelStr = "channel988";
      channelController.editChannel(channel.getId().toString(), channelStr, channelStr, channelStr, map);
      channel = channelDAO.find(4L);
      Assert.assertEquals(channelStr, channel.getName());
      Assert.assertEquals(channelStr, channel.getCode());
      Assert.assertEquals(channelStr, channel.getDescription());
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testCreateChannelGet() {
    try {
      Boolean found = false;
      channelController.createChannel(map, response);

      Assert.assertNotSame(HttpStatus.PRECONDITION_FAILED.value(), response.getStatus());

      List<Channel> channelList = (List<Channel>) map.get("channels");
      for (Channel channel : channelList) {
        if (channelDAO.find(4L).getCode().equals(channel.getCode())) {
          found = true;
        }

      }
      Assert.assertTrue(found);
      Assert.assertEquals(currencyValueService.listActiveCurrencies(), map.get("currencies"));
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testCreateChannelWithExistingChannelCode() {
    try {
      Channel channel = channelDAO.find(4L);
      String[] currencyValues = {
          currencyValueService.locateBYCurrencyCode("USD").getCurrencyName(),
          currencyValueService.locateBYCurrencyCode("INR").getCurrencyName()
      };
      channelController.createChannel(channel.getCode(), channel.getCode(), channel.getCode(), currencyValues, map,
          response);
      Assert.assertEquals(601, response.getStatus());

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testValidateChannelName() {
    try {
      Channel channel = channelDAO.find(4L);
      String result = channelController.validateChannelName(channel.getName());
      Assert.assertFalse(Boolean.valueOf(result));

      String channelStr = "channel9";
      result = channelController.validateChannelName(channelStr);
      Assert.assertTrue(Boolean.valueOf(result));

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testEditChannelCurrencyGet() {
    try {
      Channel channel = channelDAO.find(4L);
      channelController.editChannelCurrency(channel.getId().toString(), map);

      List<CurrencyValue> availableCurrencies = (List<CurrencyValue>) map.get("availableCurrencies");
      int size = availableCurrencies.size();
      List<CurrencyValue> currencyList = channelService.listCurrencies(channel.getParam());
      availableCurrencies.removeAll(currencyList);
      Assert.assertEquals(size, availableCurrencies.size());

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testEditChannelCurrencyPost() {
    try {
      Channel channel = channelDAO.find(4L);

      channelController.editChannelCurrency(channel.getId().toString(), map);
      List<CurrencyValue> availableCurrencies = (List<CurrencyValue>) map.get("availableCurrencies");
      String currencyCode = availableCurrencies.get(0).getCurrencyCode();
      String currencyCodeArray = "['" + currencyCode + "']";

      String status = channelController.editChannelCurrency(channel.getId().toString(), currencyCodeArray, map);
      Assert.assertEquals("success", status);
      Assert.assertTrue(channel.getCatalog().getSupportedCurrencyValuesByOrder()
          .contains(currencyValueService.locateBYCurrencyCode(currencyCode)));

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testEditChannelCurrencyPostNegative() {
    try {
      Channel channel = channelDAO.find(4L);

      channelController.editChannelCurrency(channel.getId().toString(), map);
      List<CurrencyValue> availableCurrencies = (List<CurrencyValue>) map.get("availableCurrencies");
      String activeCurrencyCode = availableCurrencies.get(0).getCurrencyCode();

      List<CurrencyValue> activeCurrencyList = currencyService.listActiveCurrencies();
      List<CurrencyValue> currencyList = currencyService.getCurrencyValues();
      currencyList.removeAll(activeCurrencyList);
      String currencyCodeArray = "['" + currencyList.get(0).getCurrencyCode() + "','" + activeCurrencyCode + "']";

      String status = channelController.editChannelCurrency(channel.getId().toString(), currencyCodeArray, map);
      Assert.assertEquals("success", status);
      Assert.assertFalse(channel.getCatalog().getSupportedCurrencyValuesByOrder()
          .contains(currencyValueService.locateBYCurrencyCode(currencyList.get(0).getCurrencyCode())));
      Assert.assertTrue(channel.getCatalog().getSupportedCurrencyValuesByOrder()
          .contains(currencyValueService.locateBYCurrencyCode(activeCurrencyCode)));

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testEditCatalogProductPricing() {
    try {
      Channel channel = channelDAO.find(4L);
      Boolean found = false;
      Product product = productDAO.find(4L);
      CurrencyValue currencyValue = currencyService.locateBYCurrencyCode("JPY");

      channelController.editCatalogProductPricing(channel.getId().toString(), map);

      Assert.assertEquals(channelService.getFutureRevision(channel).getStartDate(), map.get("planDate"));
      Assert.assertEquals(channel.getCatalog().getSupportedCurrencyValuesByOrder(), map.get("supportedCurrencies"));

      Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> fullProductPricingMap = (Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>>) map
          .get("fullProductPricingMap");
      Map<String, ProductCharge> productChargeMap = fullProductPricingMap.get(product).get(currencyValue);
      for (Map.Entry<String, ProductCharge> entry : productChargeMap.entrySet()) {
        ProductCharge productCharge = entry.getValue();
        if (productCharge.getProduct().compareTo(product) == 0) {
          Assert.assertEquals(productCharge.getProduct().getPrice(), product.getPrice());
          found = true;
        }
      }
      Assert.assertTrue(found);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @ExpectedException(CurrencyPrecisionException.class)
  @Test
  public void testEditCatalogProductPricingWithUnCompatibleprecision() {
    Channel channel = channelDAO.find(4L);
    Configuration precision = configurationDAO.findByName("com_citrix_cpbm_portal_appearance_currency_precision"
        .replace('_', '.'));
    precision.setValue("3");
    configurationDAO.save(precision);

    List<ProductRevision> catalogProductRevisions = channelService.getChannelRevision(channel,
        channelService.getFutureRevision(channel).getStartDate(), false).getProductRevisions();

    for (ProductRevision productRevision : catalogProductRevisions) {
      for (ProductCharge productCharge : productRevision.getProductCharges()) {
        productCharge.setPrice(new BigDecimal("3.1234"));

      }
    }
    channelController.editCatalogProductPricing(channel.getId().toString(), map);
  }

  @ExpectedException(CurrencyPrecisionException.class)
  @Test
  public void testEditCatalogProductBundlePricingWithUnCompatibleprecision() {
    Channel channel = channelDAO.find(4L);
    Configuration precision = configurationDAO.findByName("com_citrix_cpbm_portal_appearance_currency_precision"
        .replace('_', '.'));
    precision.setValue("3");
    configurationDAO.save(precision);

    ProductBundle bundle = bundleService.getProductBundleById(2L);
    List<ProductBundleRevision> catalogProductBundleRevisions = new ArrayList<ProductBundleRevision>();

    catalogProductBundleRevisions = channelService.getChannelRevision(channel,
        channelService.getFutureRevision(channel).getStartDate(), false).getProductBundleRevisions();

    ProductBundleRevision catalaogProductBundleRevision = catalogProductBundleRevisions.get(0);

    for (RateCardCharge rcc : catalaogProductBundleRevision.getRateCardCharges()) {
      rcc.setPrice(new BigDecimal("4.1234"));
    }
    channelController.editCatalogProductBundlePricing(channel.getId().toString(), bundle.getId().toString(), map);

  }

  @Test
  public void testEditCatalogProductPricingPost() {
    try {
      Channel channel = channelDAO.find(4L);
      Product product = productDAO.find(4L);
      String currencyValData = "[{'previousvalue':'14.0000','value':'200.0000','currencycode':'JPY','currencyId':'71','productId':'4'}]";
      channelController.editCatalogProductPricing(channel.getId().toString(), currencyValData, map);

      boolean productChargeSet = false;
      List<ProductCharge> prodCharges = productService.getCatalogPlannedChargesForAllProducts(channel.getCatalog());
      for (ProductCharge productCharge : prodCharges) {
        if (productCharge.getProduct().equals(product)
            && productCharge.getCurrencyValue().getCurrencyCode().equals("JPY")) {
          productChargeSet = true;
          Assert.assertEquals(0, productCharge.getPrice().compareTo(BigDecimal.valueOf(200.00)));
        }
      }
      Assert.assertTrue(productChargeSet);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testShowDatePicker() {
    try {
      Channel channel = channelDAO.find(4L);
      Revision futureRevision = channelService.getFutureRevision(channel);

      channelController.showDatePicker(channel.getId().toString(), map);

      if (futureRevision != null && futureRevision.getStartDate() != null
          && futureRevision.getStartDate().after(new Date())) {
        Assert.assertEquals(true, map.get("planDateInFuture"));
        Assert.assertEquals(futureRevision.getStartDate(), map.get("plan_date"));
      }

      Assert.assertTrue(DateUtils.isSameDay(DateUtils.addOneDay(new Date()), (Date) map.get("date_tomorrow")));
      Assert.assertEquals(new Date().getTime() / 1000, ((Date) map.get("date_today")).getTime() / 1000);
      Assert.assertEquals(channel, map.get("channel"));
      Assert.assertEquals(true, map.get("isTodayAllowed"));

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testEditChannelLogo() {
    try {
      Channel channel = channelDAO.find(4L);

      channelController.editChannelLogo(channel.getId().toString(), map);
      ChannelLogoForm channelLogoForm = (ChannelLogoForm) map.get("channelLogoForm");
      Assert.assertEquals(channel.getId(), channelLogoForm.getChannel().getId());
      Assert.assertEquals(Page.CHANNELS, map.get("page"));
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testGetNextSetOfBundles() {
    boolean found = false;
    Channel channel = channelDAO.find(4L);
    ProductBundle bundle = bundleService.getProductBundleById(2L);

    channelController.getNextSetOfBundles(channel.getId().toString(), "5", "planned", "1", map);

    List<CurrencyValue> currencyList = channelService.listCurrencies(channel.getParam());

    List<SupportedCurrency> supportedCurrencyList = (List<SupportedCurrency>) map.get("supportedCurrencies");
    Assert.assertEquals(currencyList.size(), supportedCurrencyList.size());

    Assert.assertTrue((Boolean) map.get("toalloweditprices"));
    Assert.assertEquals(1, map.get("actiontoshow"));
    Assert.assertEquals(channel, map.get("channel"));

    Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = (Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>>) map
        .get("fullBundlePricingMap");
    ProductBundleRevision productBundleRevision = bundleService.getProductBundleRevision(bundle, channelService
        .getFutureRevision(channel).getStartDate(), channel);
    Map<String, RateCardCharge> rateCardMap = fullBundlePricingMap.get(productBundleRevision).get(
        currencyValueService.locateBYCurrencyCode("JPY"));
    for (Map.Entry<String, RateCardCharge> entry : rateCardMap.entrySet()) {
      if (rateCardComponentDAO.find(12L).getId().toString()
          .equals(entry.getValue().getRateCardComponent().getId().toString())) {
        BigDecimal charge = new BigDecimal("400.0000");
        Assert.assertEquals(charge, entry.getValue().getPrice());
        found = true;
        break;
      }
    }
    Assert.assertTrue(found);
  }

  @Test
  public void testViewCatalogHistory() {
    try {
      boolean found = false;
      Channel channel = channelDAO.find(4L);
      Product product = productDAO.find(4L);
      CurrencyValue currencyValue = currencyService.locateBYCurrencyCode("JPY");

      channelController.viewCatalogHistory(channel.getId().toString(), "30/04/2012", "MM/dd/yyyy", "true", map);

      Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> fullProductPricingMap = (Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>>) map
          .get("fullProductPricingMap");
      Map<String, ProductCharge> productChargeMap = fullProductPricingMap.get(product).get(currencyValue);
      for (Map.Entry<String, ProductCharge> entry : productChargeMap.entrySet()) {
        ProductCharge productCharge = entry.getValue();
        if (productCharge.getProduct().compareTo(product) == 0) {
          Assert.assertEquals(productCharge.getProduct().getPrice(), product.getPrice());
          found = true;
        }
      }
      Assert.assertTrue(found);
      found = false;

      Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = (Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>>) map
          .get("fullBundlePricingMap");
      BigDecimal charge = new BigDecimal("4000.0000");
      for (Map.Entry<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> bundlepricemap : fullBundlePricingMap
          .entrySet()) {
        for (Map.Entry<CurrencyValue, Map<String, RateCardCharge>> map2 : bundlepricemap.getValue().entrySet()) {
          CurrencyValue cv = map2.getKey();
          if (cv.getCurrencyCode().equalsIgnoreCase("JPY")) {
            for (Map.Entry<String, RateCardCharge> entry : map2.getValue().entrySet()) {
              if (rateCardComponentDAO.find(12L).getId().toString()
                  .equals(entry.getValue().getRateCardComponent().getId().toString())) {
                Assert.assertEquals(charge, entry.getValue().getPrice());
                found = true;
                break;
              }
            }
          }
        }
      }
      Assert.assertTrue(found);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testsyncChannel() {
    try {
      Channel channel = channelDAO.find(4L);
      Product product = productDAO.find(4L);

      productService.removeProductById(product.getId().toString(), true);

      productService.setReferencePriceBookFutureRevisionDate(new Date());
      Assert.assertTrue(DateUtils.isSameDay(new Date(), channelService.getCurrentRevision(null).getStartDate()));

      channelController.syncChannel(channel.getId().toString(), map);

      Boolean Found = false;
      List<ProductRevision> catalogProductRevisions = channelService.getFutureChannelRevision(channel, false)
          .getProductRevisions();
      for (ProductRevision productRevision : catalogProductRevisions) {
        for (ProductCharge productCharge : productRevision.getProductCharges()) {
          if (productCharge.getProduct().getCode().equals(product.getCode())) {
            Assert.fail();
            Found = true;
          }
        }
      }
      Assert.assertFalse(Found);

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testCurrentGetFullChargeListing() {
    try {
      boolean found = false;
      Channel channel = channelDAO.find(4L);
      Product product = productDAO.find(4L);
      CurrencyValue currencyValue = currencyService.locateBYCurrencyCode("JPY");

      channelController.getFullChargeListing(channel.getId().toString(), "current", null, "MM/dd/yyyy", "05/02/2012",
          map);

      Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> fullProductPricingMap = (Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>>) map
          .get("fullProductPricingMap");
      Map<String, ProductCharge> productChargeMap = fullProductPricingMap.get(product).get(currencyValue);
      for (Map.Entry<String, ProductCharge> entry : productChargeMap.entrySet()) {
        ProductCharge productCharge = entry.getValue();
        if (productCharge.getProduct().compareTo(product) == 0) {
          Assert.assertEquals(product.getPrice(), productCharge.getProduct().getPrice());
          found = true;
        }
      }
      Assert.assertTrue(found);
      Assert.assertNull(map.get("fullBundlePricingMap"));
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testPlannedGetFullChargeListing() {
    try {
      boolean found = false;
      Channel channel = channelDAO.find(4L);
      Product product = productDAO.find(4L);
      CurrencyValue currencyValue = currencyService.locateBYCurrencyCode("JPY");

      String currencyValData = "[{'previousvalue':'14.0000','value':'200.0000','currencycode':'JPY','currencyId':'71','productId':'4'}]";
      channelController.editCatalogProductPricing(channel.getId().toString(), currencyValData, map);

      channelController.getFullChargeListing(channel.getId().toString(), "planned", null, "MM/dd/yyyy", "05/02/2012",
          map);

      Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> fullProductPricingMap = (Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>>) map
          .get("fullProductPricingMap");
      Map<String, ProductCharge> productChargeMap = fullProductPricingMap.get(product).get(currencyValue);
      for (Map.Entry<String, ProductCharge> entry : productChargeMap.entrySet()) {
        ProductCharge productCharge = entry.getValue();
        if (productCharge.getProduct().compareTo(product) == 0
            && productCharge.getCurrencyValue().getCurrencyCode().equals("JPY")
            && channel.getCatalog().equals(productCharge.getCatalog())) {
          Assert.assertEquals(new BigDecimal("200.0000"), productCharge.getPrice());
          found = true;
        }
      }
      Assert.assertTrue(found);
      Assert.assertNull(map.get("fullBundlePricingMap"));
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testSaveChannelSettings() {
    Channel channel = channelDAO.find(13L);
    ChannelServiceSettingsForm channelServiceSettingsForm = new ChannelServiceSettingsForm();
    channelServiceSettingsForm.setChannelId(channel.getId().toString());
    ChannelServiceSetting channelServiceSetting1 = new ChannelServiceSetting("channelSettings2", "values1", 81L);
    ChannelServiceSetting channelServiceSetting2 = new ChannelServiceSetting("channelSettings1", "values2", 82L);
    List<ChannelServiceSetting> channelServiceSettings = new ArrayList<ChannelServiceSetting>();
    channelServiceSettings.add(channelServiceSetting1);
    channelServiceSettings.add(channelServiceSetting2);
    channelServiceSettingsForm.setChannelServiceSettings(channelServiceSettings);
    channelServiceSettingsForm.setMode("create");
    channelServiceSettingsForm.setServiceInstanceUUID("3c34c33e-7a28-4791-b31e-417925ede02a");
    String result = channelController.saveChannelSettings(channelServiceSettingsForm, map);
    Assert.assertTrue(StringUtils.isNotEmpty(result));
    Assert.assertEquals("success", result);
    channelServiceSettingsForm.setMode("NotCreate");
    result = channelController.saveChannelSettings(channelServiceSettingsForm, map);
    Assert.assertTrue(StringUtils.isNotEmpty(result));
    Assert.assertEquals("success", result);

  }

  @Test
  public void testEditChannelSettings() {
    Channel channel = channelDAO.find(13L);
    ChannelServiceSettingsForm channelServiceSettingsForm = new ChannelServiceSettingsForm();
    channelServiceSettingsForm.setChannelId(channel.getId().toString());
    ChannelServiceSetting channelServiceSetting1 = new ChannelServiceSetting("channelSettings2", "values1", 81L);
    ChannelServiceSetting channelServiceSetting2 = new ChannelServiceSetting("channelSettings1", "values2", 82L);
    List<ChannelServiceSetting> channelServiceSettings = new ArrayList<ChannelServiceSetting>();
    channelServiceSettings.add(channelServiceSetting1);
    channelServiceSettings.add(channelServiceSetting2);
    channelServiceSettingsForm.setChannelServiceSettings(channelServiceSettings);
    channelServiceSettingsForm.setMode("create");
    channelServiceSettingsForm.setServiceInstanceUUID("3c34c33e-7a28-4791-b31e-417925ede02a");
    String result = channelController.saveChannelSettings(channelServiceSettingsForm, map);
    Assert.assertTrue(StringUtils.isNotEmpty(result));
    Assert.assertEquals("success", result);
    channelServiceSetting1 = new ChannelServiceSetting("channelSettings2", "changevalues1", 81L);
    channelServiceSetting2 = new ChannelServiceSetting("channelSettings1", "changevalues2", 82L);
    result = channelController.editChannelSettings(channelServiceSettingsForm, channel.getId().toString(),
        "3c34c33e-7a28-4791-b31e-417925ede02a", map);
    Assert.assertTrue(StringUtils.isNotEmpty(result));
    Assert.assertEquals("channel.service.edit.settings", result);
  }

  @Test
  public void testGetChannelSettings() {
    Channel channel = channelDAO.find(13L);
    ChannelServiceSettingsForm channelServiceSettingsForm = new ChannelServiceSettingsForm();
    channelServiceSettingsForm.setChannelId(channel.getId().toString());
    ChannelServiceSetting channelServiceSetting1 = new ChannelServiceSetting("channelSettings2", "values1", 81L);
    ChannelServiceSetting channelServiceSetting2 = new ChannelServiceSetting("channelSettings1", "values2", 82L);
    List<ChannelServiceSetting> channelServiceSettings = new ArrayList<ChannelServiceSetting>();
    channelServiceSettings.add(channelServiceSetting1);
    channelServiceSettings.add(channelServiceSetting2);
    channelServiceSettingsForm.setChannelServiceSettings(channelServiceSettings);
    channelServiceSettingsForm.setMode("create");
    channelServiceSettingsForm.setServiceInstanceUUID("3c34c33e-7a28-4791-b31e-417925ede02a");
    String result = channelController.saveChannelSettings(channelServiceSettingsForm, map);
    Assert.assertTrue(StringUtils.isNotEmpty(result));
    Assert.assertEquals("success", result);
    channelServiceSettingsForm.setMode("NotCreate");
    result = channelController.saveChannelSettings(channelServiceSettingsForm, map);
    Assert.assertTrue(StringUtils.isNotEmpty(result));
    Assert.assertEquals("success", result);
    result = channelController.getChannelSettings(channelServiceSettingsForm, channel.getId().toString(),
        "3c34c33e-7a28-4791-b31e-417925ede02a", map);
    Assert.assertTrue(StringUtils.isNotEmpty(result));
    Assert.assertEquals("channel.service.settings", result);
    Assert.assertTrue(MapUtils.isNotEmpty(map));
    Assert.assertEquals(1, map.get("serviceSettingsCount"));
    Assert.assertEquals(channel.getId().toString(), map.get("serviceSettingsChanelID"));
    Assert.assertEquals("3c34c33e-7a28-4791-b31e-417925ede02a", map.get("serviceSettingsInstanceUUID"));

  }

  @Test
  public void testNegativeGetChannelSettings() {
    Channel channel = channelDAO.find(13L);
    ChannelServiceSettingsForm channelServiceSettingsForm = new ChannelServiceSettingsForm();
    channelServiceSettingsForm.setChannelId(channel.getId().toString());
    ChannelServiceSetting channelServiceSetting1 = new ChannelServiceSetting("channelSettings2", "values1", 81L);
    ChannelServiceSetting channelServiceSetting2 = new ChannelServiceSetting("channelSettings1", "values2", 82L);
    List<ChannelServiceSetting> channelServiceSettings = new ArrayList<ChannelServiceSetting>();
    channelServiceSettings.add(channelServiceSetting1);
    channelServiceSettings.add(channelServiceSetting2);
    channelServiceSettingsForm.setChannelServiceSettings(channelServiceSettings);
    channelServiceSettingsForm.setMode("create");
    channelServiceSettingsForm.setServiceInstanceUUID("3c34c33e-7a28-4791-b31e-417925ede02a");
    String result = channelController.saveChannelSettings(channelServiceSettingsForm, map);
    Assert.assertTrue(StringUtils.isNotEmpty(result));
    Assert.assertEquals("success", result);
    channelServiceSettingsForm.setMode("NotCreate");
    result = channelController.saveChannelSettings(channelServiceSettingsForm, map);
    Assert.assertTrue(StringUtils.isNotEmpty(result));
    Assert.assertEquals("success", result);
    ChannelServiceSettingsForm channelServiceSettingsNewForm = new ChannelServiceSettingsForm();
    result = channelController.getChannelSettings(channelServiceSettingsNewForm, channel.getId().toString(),
        "003fa8ee-fba3-467f-a517-ed806dae8a89", map);
    Assert.assertTrue(StringUtils.isNotEmpty(result));
    Assert.assertEquals("channel.service.settings", result);
    Assert.assertTrue(MapUtils.isNotEmpty(map));
    Assert.assertEquals(0, map.get("serviceSettingsCount"));
  }

  /**
   * @Desc Test to check channel creation fails with Duplicate channel code
   * @author vinayv
   */
  @Test
  public void testCreateChannelWithDuplicateCode() {
    logger.info("Entering testCreateChannelWithDuplicateCode test");
    int beforeCount = channelDAO.count();
    String[] currencyvaluelist = {
        "USD", "EUR"
    };
    Channel channel = channelController.createChannel("channelName", "channelDesc", channelService.getChannelById("1")
        .getCode(), currencyvaluelist, map, response);
    Assert.assertNull(channel);
    Assert.assertEquals(601, response.getStatus());
    int afterCount = channelDAO.count();
    Assert.assertEquals(beforeCount, afterCount);
    logger.info("Exiting testCreateChannelWithDuplicateCode test");
  }

  /**
   * Description : Test to verify the product bundles in channels current revision/ current tab
   * 
   * @author nageswarap
   */
  @Ignore
  @Test
  public void testViewBundlesInCatalogCurrentRevision() {
    try {

      String expectedChannelName = "Veera" + random;

      // Creating a Channel
      String[] currencyValueList = {
        "USD"
      };

      Channel channel = channelController.createChannel(expectedChannelName, "Veera", "Veera", currencyValueList, map,
          response);
      Assert.assertNotNull("createChannel returned null", channel);
      Assert.assertEquals("The Channnle not created with the given name", expectedChannelName, channel.getName());

      ServiceResourceType resourceType = connectorConfigurationManager.getServiceResourceTypeById(1L);
      String chargeFrequency = "MONTHLY";
      String compAssociationJson = "[]";
      int noOfdays = 3;
      Calendar createdAt = Calendar.getInstance();
      createdAt.add(Calendar.DATE, 0 - noOfdays);
      int beforeBundleCount = bundleService.getBundlesCount();
      boolean trialEligible = false;

      // Create a Bundle
      ProductBundle obtainedBundle = testCreateProductBundle("1", resourceType.getId().toString(), chargeFrequency,
          chargeFrequency + "Compute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
          ResourceConstraint.NONE, trialEligible);
      Assert.assertNotNull("The bundle is null ", obtainedBundle);
      Assert.assertEquals("the expected bundle name and the actual bundle name is not matching", chargeFrequency
          + "Compute", obtainedBundle.getName());
      Assert.assertEquals("Bundle resource type is not matching", resourceType, obtainedBundle.getResourceType());

      int afterBundleCount = bundleService.getBundlesCount();
      Assert.assertEquals("bundle count not incremented after creating the bundle", beforeBundleCount + 1,
          afterBundleCount);

      // Schedule Activating the Bundle
      int noOfdays1 = 0;
      Calendar scheduleActivatedAt = Calendar.getInstance();
      scheduleActivatedAt.add(Calendar.DATE, 0 - noOfdays1);
      ProductForm postForm = new ProductForm();
      postForm.setStartDate(new Date());
      BindingResult result = validate(postForm);
      String scheduleActivationStatus = productsController.setPlanDate(postForm, result, map);
      Assert.assertNotNull(" scheduleActivationStatus is null ", scheduleActivationStatus);
      Assert.assertEquals("scheduleActivationStatus status is not success", "success", scheduleActivationStatus);

      // Sync channel with reference price book
      String syncStatus = channelController.syncChannel(channel.getId().toString(), map);
      Assert.assertNotNull(" syncStatus is null ", syncStatus);
      Assert.assertEquals("sync status is not success", "success", syncStatus);

      // Attaching product bundle to the channel
      String selectedProductBundles = "[" + obtainedBundle.getId().toString() + "]";
      String attachBundleStatus = channelController.attachProductBundles(channel.getId().toString(),
          selectedProductBundles, map);
      Assert.assertNotNull(" attachBundleStatus is null ", attachBundleStatus);
      Assert.assertEquals("attachBundleStatus status is not success", "success", attachBundleStatus);

      // Schedule Activate Channel
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
      String currentdate = sdf.format(new Date());
      String cres = channelController.changePlanDate(channel.getId().toString(), currentdate, "MM/dd/yyyy", map);
      Assert.assertNotNull("changePlanDate returned null", cres);
      Assert.assertEquals("checking status of schudeactivation for a channel", "success", cres);

      channelController.viewCatalogCurrent(channel.getId().toString(), "1", "10", map);

      boolean found = false;
      List<ProductBundleRevision> productBundleRevisions = (List<ProductBundleRevision>) map
          .get("productBundleRevisions");
      for (ProductBundleRevision productbundleRevision : productBundleRevisions) {
        if (productbundleRevision.getProductBundle().getName().equalsIgnoreCase(obtainedBundle.getName())) {
          found = true;
        }
      }

      Assert.assertTrue("The Given bundle not found in viewCatalogCurrent", found);

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  /**
   * Description : Test to verify the product bundles in channels planned revision/ planned tab
   * 
   * @author nageswarap
   */
  @Test
  public void testViewBundlesInCatalogPlannedRevision() {
    try {

      String expectedChannelName = "Veera";

      // Creating a Channel
      String[] currencyValueList = {
        "USD"
      };

      Channel channel = channelController.createChannel(expectedChannelName, "Veera", "Veera", currencyValueList, map,
          response);
      Assert.assertNotNull("createChannel returned null", channel);
      Assert.assertEquals("The Channnle not created with the given name", expectedChannelName, channel.getName());

      ServiceResourceType resourceType = connectorConfigurationManager.getServiceResourceTypeById(1L);
      String chargeFrequency = "MONTHLY";
      String compAssociationJson = "[]";
      int noOfdays = 3;
      Calendar createdAt = Calendar.getInstance();
      createdAt.add(Calendar.DATE, 0 - noOfdays);
      int beforeBundleCount = bundleService.getBundlesCount();
      boolean trialEligible = false;

      // Create a Bundle
      ProductBundle obtainedBundlecurrent = testCreateProductBundle("1", resourceType.getId().toString(),
          chargeFrequency, "CurrentCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
          ResourceConstraint.NONE, trialEligible);
      Assert.assertNotNull("The bundle is null ", obtainedBundlecurrent);

      ProductBundle obtainedBundleplanned = testCreateProductBundle("1", resourceType.getId().toString(),
          chargeFrequency, "PlannedCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
          ResourceConstraint.NONE, trialEligible);
      Assert.assertNotNull("The bundle is null ", obtainedBundleplanned);

      int afterBundleCount = bundleService.getBundlesCount();
      Assert.assertEquals("bundle count not incremented after creating the bundle", beforeBundleCount + 2,
          afterBundleCount);

      // Schedule Activating the Bundle
      int noOfdays1 = 0;
      Calendar scheduleActivatedAt = Calendar.getInstance();
      scheduleActivatedAt.add(Calendar.DATE, 0 - noOfdays1);
      ProductForm postForm = new ProductForm();
      postForm.setStartDate(new Date());
      BindingResult result = validate(postForm);
      String scheduleActivationStatus = productsController.setPlanDate(postForm, result, map);
      Assert.assertNotNull(" scheduleActivationStatus is null ", scheduleActivationStatus);
      Assert.assertEquals("scheduleActivationStatus is not success", "success", scheduleActivationStatus);

      // Sync channel with reference price book
      String syncStatus = channelController.syncChannel(channel.getId().toString(), map);
      Assert.assertNotNull(" syncStatus is null ", syncStatus);
      Assert.assertEquals("sync status is not success", "success", syncStatus);

      // Attaching product bundle to the channel
      String selectedProductBundles = "[" + obtainedBundlecurrent.getId().toString() + "]";
      String attachBundleStatus = channelController.attachProductBundles(channel.getId().toString(),
          selectedProductBundles, map);
      Assert.assertNotNull(" attachBundleStatus is null ", attachBundleStatus);
      Assert.assertEquals("attachBundleStatus is not success", "success", attachBundleStatus);

      // schedule activating the channel
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
      String currentdate = sdf.format(new Date());
      String cres = channelController.changePlanDate(channel.getId().toString(), currentdate, "MM/dd/yyyy", map);
      Assert.assertNotNull("changePlanDate returned null", cres);
      Assert.assertEquals("checking status of schudeactivation for a channel", "success", cres);

      channelController.viewCatalogCurrent(channel.getId().toString(), "1", "10", map);

      boolean currentFound = false;
      boolean plannedFound = false;

      List<ProductBundleRevision> productBundleRevisions = (List<ProductBundleRevision>) map
          .get("productBundleRevisions");
      for (ProductBundleRevision productbundleRevision : productBundleRevisions) {
        ProductBundle productBundle = productbundleRevision.getProductBundle();
        if (productBundle.getId() == obtainedBundlecurrent.getId()) {
          currentFound = true;
        }
        if (productBundle.getId() == obtainedBundleplanned.getId()) {
          plannedFound = true;
        }

      }

      Assert.assertTrue("current product bundle not found in current tab", currentFound);
      Assert.assertFalse("planned product bundle found in current tab", plannedFound);

      // Sync channel with reference price book
      String syncStatus1 = channelController.syncChannel(channel.getId().toString(), map);
      Assert.assertNotNull(" syncStatus is null ", syncStatus1);
      Assert.assertEquals("sync status is not success", "success", syncStatus1);

      // Attaching product bundle to the channel
      String selectedProductBundles1 = "[" + obtainedBundleplanned.getId().toString() + "]";
      String result11 = channelController
          .attachProductBundles(channel.getId().toString(), selectedProductBundles1, map);
      Assert.assertNotNull(" attachProductBundles is null ", result11);
      Assert.assertEquals("attachProductBundles status is not success", "success", result11);

      // schedule activating channel to future date
      int noOfdays11 = 3;
      Calendar createdAt11 = Calendar.getInstance();
      createdAt11.add(Calendar.DATE, noOfdays11);
      SimpleDateFormat sdf1 = new SimpleDateFormat("MM/dd/yyyy");
      String currentdate1 = sdf1.format(createdAt11.getTime());
      String cres1 = channelController.changePlanDate(channel.getId().toString(), currentdate1, "MM/dd/yyyy", map);
      Assert.assertNotNull("changePlanDate returned null", cres1);
      Assert.assertEquals("checking status of schudeactivation for a channel", "success", cres1);

      channelController.viewCatalogPlanned(channel.getId().toString(), "1", "10", "true", map);

      currentFound = false;
      plannedFound = false;

      productBundleRevisions = (List<ProductBundleRevision>) map.get("productBundleRevisions");
      for (ProductBundleRevision productbundleRevision : productBundleRevisions) {
        ProductBundle productBundle = productbundleRevision.getProductBundle();
        if (productBundle.getId() == obtainedBundlecurrent.getId()) {
          currentFound = true;
        }
        if (productBundle.getId() == obtainedBundleplanned.getId()) {
          plannedFound = true;
        }

      }

      Assert.assertTrue("current product bundle not found in planned tab", currentFound);
      Assert.assertTrue("planned product bundle not found in planned tab", plannedFound);

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  /**
   * Description : Test to verify the product bundles in channels history revision/ history tab
   * 
   * @author nageswarap
   */

  @Test
  public void testViewBundlesInCatalogHistory() {
    try {

      String expectedChannelName = "Veera";

      // Creating a Channel
      String[] currencyValueList = {
        "USD"
      };

      Channel channel = channelController.createChannel(expectedChannelName, "Veera", "Veera", currencyValueList, map,
          response);
      Assert.assertNotNull("createChannel returned null", channel);
      Assert.assertEquals("The Channnle not created with the given name", expectedChannelName, channel.getName());

      ServiceResourceType resourceType = connectorConfigurationManager.getServiceResourceTypeById(1L);
      String chargeFrequency = "MONTHLY";
      String compAssociationJson = "[]";
      int noOfdays = 3;
      Calendar createdAt = Calendar.getInstance();
      createdAt.add(Calendar.DATE, 0 - noOfdays);
      int beforeBundleCount = bundleService.getBundlesCount();
      boolean trialEligible = false;

      // Create a Bundle

      ProductBundle obtainedBundlehistory = testCreateProductBundle("1", resourceType.getId().toString(),
          chargeFrequency, "HistoryCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
          ResourceConstraint.NONE, trialEligible);
      Assert.assertNotNull("The bundle is null ", obtainedBundlehistory);

      ProductBundle obtainedBundlecurrent = testCreateProductBundle("1", resourceType.getId().toString(),
          chargeFrequency, "CurrentCompute", "USD", BigDecimal.valueOf(100), createdAt.getTime(), compAssociationJson,
          ResourceConstraint.NONE, trialEligible);
      Assert.assertNotNull("The bundle is null ", obtainedBundlecurrent);

      int afterBundleCount = bundleService.getBundlesCount();
      Assert.assertEquals("bundle count not incremented after creating the bundle", beforeBundleCount + 2,
          afterBundleCount);

      // Schedule Activating the Bundle
      int noOfdays1 = 0;
      Calendar scheduleActivatedAt = Calendar.getInstance();
      scheduleActivatedAt.add(Calendar.DATE, 0 - noOfdays1);
      ProductForm postForm = new ProductForm();
      postForm.setStartDate(new Date());
      BindingResult result = validate(postForm);
      String scheduleActivationStatus = productsController.setPlanDate(postForm, result, map);

      // Sync channel with reference price book
      String syncStatus = channelController.syncChannel(channel.getId().toString(), map);
      Assert.assertNotNull(" syncStatus is null ", syncStatus);
      Assert.assertEquals("sync status is success", "success", syncStatus);

      // Attaching product bundle to the channel
      String selectedProductBundles = "[" + obtainedBundlehistory.getId().toString() + "]";
      String result1 = channelController.attachProductBundles(channel.getId().toString(), selectedProductBundles, map);

      // schedule activating channel
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
      String currentdate = sdf.format(new Date());
      String cres = channelController.changePlanDate(channel.getId().toString(), currentdate, "MM/dd/yyyy HH:mm:ss",
          map);
      Assert.assertNotNull("changePlanDate returned null", cres);
      Assert.assertEquals("checking status of schudeactivation for a channel", "success", cres);

      ArrayList<Revision> revisions = (ArrayList<Revision>) revisionDAO.locateAllCatalogRevisions(channel.getCatalog());
      for (Revision r : revisions) {
        System.out.println("start date" + r.getStartDate());
      }
      Date currentRevisionStartDate = channelService.getCurrentRevision(channel).getStartDate();
      System.out.println("Current revi" + currentRevisionStartDate);
      Thread.sleep(2000);

      // Sync channel with reference price book
      String syncStatus1 = channelController.syncChannel(channel.getId().toString(), map);
      Assert.assertNotNull(" syncStatus is null ", syncStatus1);
      Assert.assertEquals("sync status is success", "success", syncStatus1);

      // Attaching product bundle to the channel
      String selectedProductBundles1 = "[" + obtainedBundlecurrent.getId().toString() + "]";
      String result11 = channelController
          .attachProductBundles(channel.getId().toString(), selectedProductBundles1, map);

      String currentdate1 = sdf.format(new Date());
      String cres1 = channelController.changePlanDate(channel.getId().toString(), currentdate1, "MM/dd/yyyy HH:mm:ss",
          map);
      channelDAO.refresh(channel);
      catalogDAO.refresh(channel.getCatalog());
      Assert.assertNotNull("changePlanDate returned null", cres1);
      Assert.assertEquals("checking status of schudeactivation for a channel", "success", cres1);
      Thread.sleep(2000);
      revisions = (ArrayList<Revision>) revisionDAO.locateAllCatalogRevisions(channel.getCatalog());
      for (Revision r : revisions) {
        System.out.println("start date" + r.getStartDate());
      }
      Catalog catalog = channel.getCatalog();
      currentRevisionStartDate = channelService.getCurrentRevision(catalog.getChannel()).getStartDate();
      System.out.println("Current revi exact" + currentRevisionStartDate);

      channelController
          .viewCatalogHistory(channel.getId().toString(), currentdate, "MM/dd/yyyy HH:mm:ss", "false", map);

      Date currentRevisionStartDate1 = channelService.getCurrentRevision(channel).getStartDate();
      System.out.println("Current revi1" + currentRevisionStartDate1);
      boolean historyFound = false;
      boolean currentFound = false;

      Set<ProductBundleRevision> productBundleRevisions = (Set<ProductBundleRevision>) map
          .get("productBundleRevisions");
      for (ProductBundleRevision productbundleRevision : productBundleRevisions) {
        ProductBundle productBundle = productbundleRevision.getProductBundle();
        if (productBundle.getId() == obtainedBundlecurrent.getId()) {
          currentFound = true;
        }
        if (productBundle.getId() == obtainedBundlehistory.getId()) {
          historyFound = true;
        }

      }

      Assert.assertTrue("history product bundle not found in history tab", historyFound);
      Assert.assertFalse("current product bundle found in history tab", currentFound);

    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  /**
   * Description : Test to verify that editing the product price in catalog will effect the product pricing
   * 
   * @author nageswarap
   */

  @Test
  public void testEditCatalogProductPricingnotEffectProductPricing() {

    Channel channel = channelDAO.find(4L);
    Product product = productDAO.find(4L);

    List<ProductCharge> pcharges = productService.getProductCharges(product, new Date());
    BigDecimal previousCharge = BigDecimal.ZERO;
    BigDecimal expectedNewCharge = BigDecimal.valueOf(200.0000);

    for (ProductCharge pcharge : pcharges) {
      if (pcharge.getCurrencyValue().getCurrencyCode().equals("JPY")) {
        if (pcharge.getProduct().getId() == 4L) {
          previousCharge = pcharge.getPrice();
        }
      }

    }

    try {

      String currencyValData = "[{'previousvalue':'" + previousCharge + "','value':'" + expectedNewCharge
          + "','currencycode':'JPY','currencyId':'71','productId':'4'}]";
      channelController.editCatalogProductPricing(channel.getId().toString(), currencyValData, map);

      boolean productChargeSet = false;
      List<ProductCharge> prodCharges = productService.getCatalogPlannedChargesForAllProducts(channel.getCatalog());

      for (ProductCharge productCharge : prodCharges) {
        if (productCharge.getProduct().equals(product)
            && productCharge.getCurrencyValue().getCurrencyCode().equals("JPY")) {
          Assert.assertEquals("Product price is not set in catalog", expectedNewCharge, productCharge.getPrice());
          productChargeSet = true;
        }
      }
      Assert.assertTrue("Product price is not set in catalog", productChargeSet);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }

    List<ProductCharge> pcharges1 = productService.getProductCharges(product, new Date());

    for (ProductCharge pcharge : pcharges1) {
      if (pcharge.getCurrencyValue().getCurrencyCode().equals("JPY")) {
        if (pcharge.getProduct().getId() == 4L) {
          Assert.assertEquals("Product price changed after editing in catalog", previousCharge, pcharge.getPrice());
        }
      }

    }

  }

  /**
   * Description : Test to verify that editing the product price in catalog can be done when channel caching is enabled
   * 
   * @author nageswarap
   */

  @Test
  public void testEditCatalogProductPricingEnableCashing() {

    Channel channel = channelDAO.find(4L);
    Product product = productDAO.find(4L);
    BigDecimal expectedCharge = BigDecimal.valueOf(200.0000);

    Configuration conf = configurationService.locateConfigurationByName("com.citrix.cpbm.channel.enable.caching");
    if (conf.getValue().equalsIgnoreCase("FALSE")) {
      conf.setValue("TRUE");
      configurationService.update(conf);
    }
    Assert.assertEquals("com.citrix.cpbm.channel.enable.caching configuration is false", "TRUE", conf.getValue());

    try {

      String currencyValData = "[{'previousvalue':'14.0000','value':'" + expectedCharge
          + "','currencycode':'JPY','currencyId':'71','productId':'4'}]";
      channelController.editCatalogProductPricing(channel.getId().toString(), currencyValData, map);

      boolean productChargeSet = false;
      List<ProductCharge> prodCharges = productService.getCatalogPlannedChargesForAllProducts(channel.getCatalog());

      System.out.println("prodCharges list size : " + prodCharges.size());
      for (ProductCharge productCharge : prodCharges) {
        if (productCharge.getProduct().equals(product)
            && productCharge.getCurrencyValue().getCurrencyCode().equals("JPY")) {
          Assert.assertEquals("Product price is not set in catalog", expectedCharge, productCharge.getPrice());
          productChargeSet = true;
        }
      }
      Assert.assertTrue("Product price is not set in catalog", productChargeSet);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  /**
   * Description : Test to verify that editing the product bundle in catalog can be done when channel caching is enabled
   * 
   * @author nageswarap
   */

  @Test
  public void testEditCatalogProductBundlePricingWithenablecashing() throws JSONException {

    Channel obtainedChannel = channelDAO.find(4L);

    Configuration conf = configurationService.locateConfigurationByName("com.citrix.cpbm.channel.enable.caching");
    if (conf.getValue().equalsIgnoreCase("FALSE")) {
      conf.setValue("TRUE");
      configurationService.update(conf);
    }
    Assert.assertEquals("com.citrix.cpbm.channel.enable.caching configuration is false", "TRUE", conf.getValue());

    String currencyValData = "[{\"previousvalue\":\"0.0000\",\"value\":\"5000\",\"currencycode\":\"EUR\",\"currencyId\":\"44\",\"isRecurring\":\"0\"}]";
    channelController.editCatalogProductBundlePricing(obtainedChannel.getId().toString(), "2", currencyValData, map);
    channelController.getFullChargeListing(obtainedChannel.getId().toString(), "planned", "2", null,
        DateUtils.getSimpleDateString(new java.util.Date()), map);
    Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = (Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>>) map
        .get("fullBundlePricingMap");

    Assert.assertNotNull(map.get("productBundleRevision"));
    for (Map.Entry<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> map1 : fullBundlePricingMap
        .entrySet()) {
      for (Map.Entry<CurrencyValue, Map<String, RateCardCharge>> map2 : map1.getValue().entrySet()) {
        CurrencyValue cv = map2.getKey();
        if (cv.getCurrencyCode().equalsIgnoreCase("EUR")) {
          for (Map.Entry<String, RateCardCharge> map3 : map2.getValue().entrySet()) {
            String str = map3.getKey();
            RateCardCharge rcc = map3.getValue();
            if (str.equalsIgnoreCase("catalog-onetime")) {
              Assert.assertEquals(BigDecimal.valueOf(5000), rcc.getPrice());
            }
          }
        }
      }
    }

  }

  /*
   * Description: Private Test to create Bundles based on the parameters Author: Vinayv
   */
  private ProductBundle testCreateProductBundle(String serviceInstanceID, String resourceTypeID, String chargeType,
      String bundleName, String currencyCode, BigDecimal currencyValue, Date startDate, String jsonString,
      ResourceConstraint businessConstraint, boolean trialEligible) throws Exception {

    ServiceInstance serviceInstance = null;
    List<ServiceInstance> serviceInstanceList = connectorConfigurationManager.getAllServiceInstances();
    for (ServiceInstance si : serviceInstanceList) {
      if (si.getId().toString().equalsIgnoreCase(serviceInstanceID)) {
        serviceInstance = si;
      }
    }

    ServiceResourceType resourceType = null;
    if (!resourceTypeID.equalsIgnoreCase("ServiceBundle")) {
      resourceType = connectorConfigurationManager.getServiceResourceTypeById(Long.parseLong(resourceTypeID));
    }
    List<RateCardCharge> rateCardChargeList = new ArrayList<RateCardCharge>();
    RateCardCharge rcc = new RateCardCharge(currencyValueService.locateBYCurrencyCode(currencyCode), null,
        currencyValue, "RateCharge", getRootUser(), getRootUser(), channelService.getFutureRevision(null));
    rateCardChargeList.add(rcc);
    String chargeTypeName = chargeType;
    if (chargeType.equalsIgnoreCase("Invalid")) {
      chargeTypeName = "NONE";
    }
    RateCard rateCard = new RateCard("Rate", bundleService.getChargeRecurrencyFrequencyByName(chargeTypeName),
        new Date(), getRootUser(), getRootUser());
    String compAssociationJson = jsonString;

    ProductBundle bundle = new ProductBundle(bundleName, bundleName, "", startDate, startDate, getRootUser());
    bundle.setBusinessConstraint(businessConstraint);
    bundle.setCode(bundleName);
    bundle.setPublish(true);
    if (!resourceTypeID.equalsIgnoreCase("ServiceBundle")) {
      bundle.setResourceType(resourceType);
    }
    bundle.setServiceInstanceId(serviceInstance);
    bundle.setTrialEligibility(trialEligible);
    bundle.setRateCard(rateCard);

    ProductBundleForm form = new ProductBundleForm(bundle);
    form.setChargeType(chargeType);
    if (!resourceTypeID.equalsIgnoreCase("ServiceBundle")) {
      form.setResourceType(resourceType.getId().toString());
    } else {
      form.setResourceType("sb");
    }
    form.setServiceInstanceUUID(serviceInstance.getUuid());
    form.setBundleOneTimeCharges(rateCardChargeList);
    if (!chargeType.equalsIgnoreCase("NONE")) {
      form.setBundleRecurringCharges(rateCardChargeList);
    }
    form.setCompAssociationJson(compAssociationJson);

    BindingResult result = validate(form);
    ProductBundle obtainedBundle = bundleController.createProductBundle(form, result, map, response);

    return obtainedBundle;
  }

}