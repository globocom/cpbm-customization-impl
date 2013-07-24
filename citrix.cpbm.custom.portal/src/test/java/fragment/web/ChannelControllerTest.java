/* Copyright (C) 2011 Cloud.com, Inc. All rights reserved. */
package fragment.web;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONException;
import org.junit.Assert;
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
import citrix.cpbm.portal.fragment.controllers.ChannelController;
import citrix.cpbm.portal.fragment.controllers.ProductBundlesController;
import citrix.cpbm.portal.fragment.controllers.ProductsController;

import com.vmops.model.AccountType;
import com.vmops.model.Address;
import com.vmops.model.Channel;
import com.vmops.model.ChannelRevision;
import com.vmops.model.CurrencyValue;
import com.vmops.model.ProductBundle;
import com.vmops.model.ProductBundleRevision;
import com.vmops.model.RateCardCharge;
import com.vmops.model.Tenant;
import com.vmops.model.User;
import com.vmops.persistence.CatalogProductBundleDAO;
import com.vmops.persistence.ChannelDAO;
import com.vmops.persistence.ChargeRecurrenceFrequencyDAO;
import com.vmops.persistence.RateCardComponentDAO;
import com.vmops.persistence.ServiceInstanceDao;
import com.vmops.persistence.ServiceResourceTypeDAO;
import com.vmops.service.ChannelService;
import com.vmops.service.CurrencyValueService;
import com.vmops.service.ProductBundleService;
import com.vmops.service.ProductService;
import com.vmops.utils.DateUtils;
import com.vmops.web.forms.ChannelLogoForm;
import com.vmops.web.forms.RateCardChargesForm;
import com.vmops.web.forms.RateCardComponentChargesForm;

public class ChannelControllerTest extends WebTestsBase {

  private ModelMap map;

  private HttpServletResponse response;

  private HttpServletRequest request;

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
  ProductBundlesController productBundlesController;

  @Autowired
  ProductsController productsController;

  @Autowired
  ServiceInstanceDao serviceInstanceDAO;

  @Autowired
  ServiceResourceTypeDAO serviceResourceTypeDAO;

  @Autowired
  ChargeRecurrenceFrequencyDAO chargeRecurrenceFrequencyDAO;

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
   * Description: Shouldn't able to Add channel with special character as code Author: VeeramaniT
   */
  @Test(expected = Exception.class)
  public void testCreateChannelWithCodeAsSplChar() {

    String[] currencyvaluelist = {
        "USD", "EUR"
    };
    channelController.createChannel("Veera", "Veera", "<HTML>", currencyvaluelist, map, response);

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
  @Test
  public void testEditChannelWithBlankName() {
    try {
      Channel existingChannel = channelDAO.find("3");
      String result = channelController.editChannel(existingChannel.getId().toString(), null, "Veera", "Veera", map);
      Assert.assertNotNull(result);
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("could not update: [com.vmops.model.Channel#3];"));
    }
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
   * Description : User shouldn't be able to edit channel code and description field with Special characters Author :
   * VeeramaniT
   */
  @Test(expected = Exception.class)
  public void testEditChannelWithSplChar() {
    Channel existingChannel = channelDAO.find("3");
    String code = "<HTML>";
    String result = channelController.editChannel(existingChannel.getId().toString(), "Veera", "<HTML>", code, map);
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
    Assert.assertEquals("File should have either .jpeg/.jpg/.png/.gif/.bmp extension", result1);

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
    ChannelRevision futureChannelRevision = channelService.getFutureChannelRevision(obtainedChannel, false);

    // Step2 : Attaching bundle to the channels

    String selectedProductBundles = "[2]";
    String result = channelController.attachProductBundles(obtainedChannel.getId().toString(), selectedProductBundles,
        map);
    String Listbundles = channelController.listbundles(obtainedChannel.getId().toString(), map);
    List<ProductBundleRevision> productbundles = (List<ProductBundleRevision>) map.get("productBundles");
    for (ProductBundleRevision productbundleRevision : productbundles) {
      ProductBundle productBundle = productbundleRevision.getProductBundle();
      Assert.assertTrue(productBundle.getId() != 2L);
    }

    // Step 3 : Edit the Bundle pricing in the channel for the added bundle

    String currencyValData = "[{\"previousvalue\":\"0.0000\",\"value\":\"5000\",\"currencycode\":\"EUR\",\"currencyId\":\"44\",\"isRecurring\":\"0\"}]";
    String editBundlePrice = channelController.editCatalogProductBundlePricing(obtainedChannel.getId().toString(), "2",
        currencyValData, map);
    String listprice = channelController.getFullChargeListing(obtainedChannel.getId().toString(), "planned", "2", null,
        DateUtils.getSimpleDateString(new java.util.Date()), map);
    Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = (Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>>) map
        .get("fullBundlePricingMap");
    for (Map.Entry<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> map1 : fullBundlePricingMap
        .entrySet()) {
      ProductBundleRevision pbr = map1.getKey();
      for (Map.Entry<CurrencyValue, Map<String, RateCardCharge>> map2 : map1.getValue().entrySet()) {
        CurrencyValue cv = map2.getKey();
        if (cv.getCurrencyCode().equalsIgnoreCase("EUR")) {
          for (Map.Entry<String, RateCardCharge> map3 : map2.getValue().entrySet()) {
            String str = map3.getKey();
            RateCardCharge rcc = map3.getValue();
            if (str.equalsIgnoreCase("catalog-onetime"))
              Assert.assertEquals(BigDecimal.valueOf(5000), rcc.getPrice());
          }
        }
      }
    }

    // Checking that newly edited price is not affected in bundle price list

    ProductBundle pb = bundleService.getProductBundleById(2L);
    String pc = productBundlesController.viewBundlePlannedCharges(pb.getCode(), map, "");
    RateCardChargesForm rateCardChargesForm = (RateCardChargesForm) map.get("rateCardChargesForm");
    List<RateCardComponentChargesForm> rateCardComponentChargesFormList = rateCardChargesForm
        .getNonRecurringRateCardChargesFormList();
    for (RateCardComponentChargesForm rcccform : rateCardComponentChargesFormList) {
      List<RateCardCharge> rccList = rcccform.getCharges();
      for (RateCardCharge rcc : rccList) {
        if (rcc.getCurrencyValue().getCurrencyCode().equalsIgnoreCase("EUR"))
          Assert.assertTrue(rcc.getPrice() != BigDecimal.valueOf(5000));
      }

      // Step 4 : Activate the new channel

      String ac = channelController.activatecatalog(obtainedChannel.getId().toString(), map);
      Assert.assertNotNull(ac);
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
    String s = channelController.attachProductBundles(ch1.getId().toString(), selectedProductBundles, map);
    String lb = channelController.listbundles(ch1.getId().toString(), map);
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
  @Test(expected = NullPointerException.class)
  public void editChannelPricingWithBlankPrice() throws JSONException {
    String currencyValData = "[{\"previousvalue\":\"0.0000\",\"value\":\"\",\"currencycode\":\"EUR\",\"currencyId\":\"44\",\"isRecurring\":\"0\"}]";

    Channel ch1 = channelDAO.find("1");
    String se = channelController.editCatalogProductBundlePricing(ch1.getId().toString(), "2", currencyValData, map);

  }

  /*
   * Description :User shouldn't be able to edit prices with negative price under catalog tab for a channel Author :
   * VeeramaniT
   */
  @Test
  public void editChannelPricingWithNegativeprice() throws JSONException {

    String currencyValData = "[{\"previousvalue\":\"0.0000\",\"value\":\"-5\",\"currencycode\":\"USD\",\"currencyId\":\"149\",\"isRecurring\":\"0\"}]";

    Channel ch1 = channelDAO.find("3");
    String se = channelController.editCatalogProductBundlePricing(ch1.getId().toString(), "3", currencyValData, map);

  }

  /*
   * Description :As a user I should not able to Add / edit duplicate channel name. Author : VeeramaniT
   */

  @Test
  public void editDuplicateChannelName() {
    Channel ch1 = channelDAO.find("3");
    Channel ch2 = channelDAO.find("4");
    String channelName = ch2.getName();
    System.out.println(" The channelname is " + channelName);
    String s = channelController.editChannel(ch1.getId().toString(), channelName, "Desc_Channel2", "Veera", map);
  }

  /*
   * Description :As a user I should not able to Add / edit duplicate channel Code. Author : VeeramaniT
   */
  @Test
  public void editDuplicateChannelCode() {
    Channel ch1 = channelDAO.find("3");
    Channel ch2 = channelDAO.find("4");
    String channelCode = ch2.getCode();
    String s = channelController.editChannel(ch1.getId().toString(), "Veera", "Desc_Channel2", channelCode, map);
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
    ChannelRevision futureChannelRevision = channelService.getFutureChannelRevision(obtainedChannel, false);

    // Step2 : Attaching bundle to the channels

    String selectedProductBundles = "[4]";
    String result = channelController.attachProductBundles(obtainedChannel.getId().toString(), selectedProductBundles,
        map);
    String Listbundles = channelController.listbundles(obtainedChannel.getId().toString(), map);
    List<ProductBundleRevision> productbundles = (List<ProductBundleRevision>) map.get("productBundles");
    for (ProductBundleRevision productbundleRevision : productbundles) {
      ProductBundle productBundle = productbundleRevision.getProductBundle();
      Assert.assertTrue(productBundle.getId() != 4L);
    }

    // Step 3 : Edit the Bundle pricing in the channel for the added bundle

    String currencyValData = "[{\"previousvalue\":\"0.0000\",\"value\":\"5000\",\"currencycode\":\"EUR\",\"currencyId\":\"44\",\"isRecurring\":\"0\"}]";
    String editBundlePrice = channelController.editCatalogProductBundlePricing(obtainedChannel.getId().toString(), "2",
        currencyValData, map);
    String listprice = channelController.getFullChargeListing(obtainedChannel.getId().toString(), "planned", "2", null,
        DateUtils.getSimpleDateString(new java.util.Date()), map);
    Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = (Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>>) map
        .get("fullBundlePricingMap");
    for (Map.Entry<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> map1 : fullBundlePricingMap
        .entrySet()) {
      ProductBundleRevision pbr = map1.getKey();
      for (Map.Entry<CurrencyValue, Map<String, RateCardCharge>> map2 : map1.getValue().entrySet()) {
        CurrencyValue cv = map2.getKey();
        if (cv.getCurrencyCode().equalsIgnoreCase("EUR")) {
          for (Map.Entry<String, RateCardCharge> map3 : map2.getValue().entrySet()) {
            String str = map3.getKey();
            RateCardCharge rcc = map3.getValue();
            if (str.equalsIgnoreCase("catalog-onetime"))
              Assert.assertEquals(BigDecimal.valueOf(5000), rcc.getPrice());
          }
        }
      }
    }

    // Checking that newly edited price is not affected in bundle price list

    ProductBundle pb = bundleService.getProductBundleById(2L);
    String pc = productBundlesController.viewBundlePlannedCharges(pb.getCode(), map, "");
    RateCardChargesForm rateCardChargesForm = (RateCardChargesForm) map.get("rateCardChargesForm");
    List<RateCardComponentChargesForm> rateCardComponentChargesFormList = rateCardChargesForm
        .getNonRecurringRateCardChargesFormList();
    for (RateCardComponentChargesForm rcccform : rateCardComponentChargesFormList) {
      List<RateCardCharge> rccList = rcccform.getCharges();
      for (RateCardCharge rcc : rccList) {
        if (rcc.getCurrencyValue().getCurrencyCode().equalsIgnoreCase("EUR"))
          Assert.assertTrue(rcc.getPrice() != BigDecimal.valueOf(5000));
      }

      // Step 4 : Activate the new channel

      String ac = channelController.activatecatalog(obtainedChannel.getId().toString(), map);
      Assert.assertNotNull(ac);

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
    String[] currencyValueList = {};
    Channel ch = channelController.createChannel("Veera", "Veera", "Veera", null, map, response);
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
      Channel ch = channelController.createChannel(null, "Veera", "Veera", currencyValueList, map, response);
    } catch (Exception e) {
      System.out.println(e.getMessage());
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

    // Creating a new Channel
    String[] currencyValueList = {
        "USD", "EUR", "GBP"
    };
    int beforeChannelCount = channelService.getChannelCount();
    Channel obtainedChannel = channelController.createChannel("Veera", "Veera", "Veera", currencyValueList, map,
        response);
    Assert.assertNotNull(obtainedChannel);
    int afterChannelCount = channelService.getChannelCount();
    Assert.assertEquals(beforeChannelCount + 1, afterChannelCount);

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

}