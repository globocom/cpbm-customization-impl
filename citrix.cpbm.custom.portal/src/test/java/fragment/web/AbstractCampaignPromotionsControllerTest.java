/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
package fragment.web;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;

import web.WebTestsBase;

import com.citrix.cpbm.portal.fragment.controllers.CampaignPromotionsController;
import com.vmops.model.CampaignPromotion;
import com.vmops.model.CampaignPromotion.DiscountType;
import com.vmops.model.CampaignPromotionDiscountAmount;
import com.vmops.model.CampaignPromotionsInChannels;
import com.vmops.model.Catalog;
import com.vmops.model.Channel;
import com.vmops.model.CurrencyValue;
import com.vmops.model.PromotionSignup;
import com.vmops.model.PromotionToken;
import com.vmops.model.SupportedCurrency;
import com.vmops.model.User;
import com.vmops.persistence.CampaignPromotionDAO;
import com.vmops.persistence.CurrencyValueDAO;
import com.vmops.persistence.PromotionSignupDAO;
import com.vmops.persistence.PromotionTokenDAO;
import com.vmops.service.ChannelService;
import com.vmops.service.ProductBundleService;
import com.vmops.service.PromotionService;
import com.vmops.service.exceptions.AjaxFormValidationException;
import com.vmops.service.exceptions.InvalidAjaxRequestException;
import com.vmops.utils.DateUtils;
import com.vmops.web.forms.CampaignPromotionsForm;
import com.vmops.web.forms.DepositRecordForm;
import com.vmops.web.forms.TokenRequestForm;

public class AbstractCampaignPromotionsControllerTest extends WebTestsBase {

  @Autowired
  CampaignPromotionsController campaignController;

  @Autowired
  PromotionService promotionService;

  @Autowired
  ChannelService channelService;

  @Autowired
  private ProductBundleService bundleservice;

  @Autowired
  private CurrencyValueDAO currencyDAO;

  @Autowired
  private PromotionSignupDAO SignUpdao;

  @Autowired
  private PromotionTokenDAO tokendao;

  @Autowired
  CampaignPromotionDAO campaignPromotionDAO;

  private ModelMap map;

  private MockHttpServletResponse response;

  @Before
  public void init() throws Exception {
    map = new ModelMap();
    response = new MockHttpServletResponse();

  }

  /*
   * Method to generate Campaign Promotion object based
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  private CampaignPromotion generateCampaignPromotion(int days, boolean trial, boolean fixedAmount, boolean percentage) {

    CampaignPromotion campaignProm = new CampaignPromotion();
    if (fixedAmount) {
      campaignProm.setDiscountType(DiscountType.FIXED_AMOUNT);
    }
    if (percentage) {
      campaignProm.setDiscountType(DiscountType.PERCENTAGE);
      campaignProm.setPercentOff(BigDecimal.TEN);
    }
    campaignProm.setCode("NewCamp");
    campaignProm.setTitle("New");
    campaignProm.setCreateBy(getPortalUser());
    campaignProm.setUpdateBy(getPortalUser());
    campaignProm.setCreateDate(new Date());
    int noOfdays = days;
    Calendar createdAt = Calendar.getInstance();
    createdAt.add(Calendar.DATE, 0 - noOfdays);
    campaignProm.setStartDate(createdAt.getTime());
    campaignProm.setTrial(trial);
    return campaignProm;
  }

  /*
   * Test to create Campaign for Non Trial users with percentage value as Root User
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test
  public void testCreateCampaignForNonTrialWithPercentageAsRoot() throws Exception {

    CampaignPromotion campaignPromotion = generateCampaignPromotion(0, false, false, true);
    CampaignPromotionsForm form = new CampaignPromotionsForm(campaignPromotion);
    form.setPromoCode("PromoCode");
    form.setChannel(channelService.getChannelById("1"));
    form.setUnlimited(true);
    BindingResult result = validate(form);
    CampaignPromotion obtainedCampaign = campaignController.createCampaigns(form, result, map, response);
    Assert.assertNotNull(obtainedCampaign);
    Assert.assertEquals(obtainedCampaign, campaignPromotion);
    Assert.assertEquals(obtainedCampaign.isTrial(), false);
    Assert.assertEquals(obtainedCampaign.getState().toString(), "ACTIVE");
    Assert.assertEquals(obtainedCampaign.getCode(), "NewCamp");
    Assert.assertEquals(obtainedCampaign.getTitle(), "New");
    Assert.assertEquals(obtainedCampaign.getDiscountType().toString(), "PERCENTAGE");
    Assert.assertEquals(obtainedCampaign.getPercentOff().toString(), "10");
  }

  /*
   * Test to create Campaign for Trial users as Root User
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test
  public void testCreateCampaignForTrialAsRoot() throws Exception {

    CampaignPromotion campaignPromotion = generateCampaignPromotion(0, true, false, false);
    CampaignPromotionsForm form = new CampaignPromotionsForm(campaignPromotion);
    form.setPromoCode("PromoCode");
    form.setChannel(channelService.getChannelById("1"));
    form.setUnlimited(true);
    BindingResult result = validate(form);
    CampaignPromotion obtainedCampaign = campaignController.createCampaigns(form, result, map, response);
    Assert.assertNotNull(obtainedCampaign);
    Assert.assertEquals(obtainedCampaign, campaignPromotion);
    Assert.assertEquals(obtainedCampaign.isTrial(), true);
    Assert.assertEquals(obtainedCampaign.getState().toString(), "ACTIVE");
    Assert.assertEquals(obtainedCampaign.getCode(), "NewCamp");
    Assert.assertEquals(obtainedCampaign.getTitle(), "New");
    Assert.assertEquals(obtainedCampaign.getDiscountType().toString(), "PERCENTAGE");
    Assert.assertEquals(obtainedCampaign.getPercentOff().toString(), "100");
  }

  /*
   * Test to create Campaign for Trial users as Product Manager User
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test
  public void testCreateCampaignForTrialAsProductManager() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    CampaignPromotion campaignPromotion = generateCampaignPromotion(0, true, false, false);
    CampaignPromotionsForm form = new CampaignPromotionsForm(campaignPromotion);
    form.setPromoCode("PromoCode");
    form.setChannel(channelService.getChannelById("1"));
    form.setUnlimited(true);
    BindingResult result = validate(form);
    CampaignPromotion obtainedCampaign = campaignController.createCampaigns(form, result, map, response);
    Assert.assertNotNull(obtainedCampaign);
    Assert.assertEquals(obtainedCampaign, campaignPromotion);
    Assert.assertEquals(obtainedCampaign.isTrial(), true);
    Assert.assertEquals(obtainedCampaign.getState().toString(), "ACTIVE");
    Assert.assertEquals(obtainedCampaign.getCode(), "NewCamp");
    Assert.assertEquals(obtainedCampaign.getTitle(), "New");
    Assert.assertEquals(obtainedCampaign.getDiscountType().toString(), "PERCENTAGE");
    Assert.assertEquals(obtainedCampaign.getPercentOff().toString(), "100");
  }

  /*
   * Test to create Campaign for Trial users as Master User //master user is able to create product bundle which is an
   * error
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test(expected = Exception.class)
  public void testCreateCampaignForTrialAsMaster() throws Exception {

    User user = userDAO.find("3");
    asUser(user);
    CampaignPromotion campaignPromotion = generateCampaignPromotion(0, true, false, false);
    CampaignPromotionsForm form = new CampaignPromotionsForm(campaignPromotion);
    form.setPromoCode("PromoCode");
    form.setChannel(channelService.getChannelById("1"));
    form.setUnlimited(true);
    BindingResult result = validate(form);
    CampaignPromotion obtainedCampaign = campaignController.createCampaigns(form, result, map, response);

    Assert.assertNotNull(obtainedCampaign);
    Assert.assertEquals(obtainedCampaign.getCode(), campaignPromotion.getCode());
    Assert.assertEquals(obtainedCampaign.isTrial(), true);
    Assert.assertEquals(obtainedCampaign.getState().toString(), "ACTIVE");
    Assert.assertEquals(obtainedCampaign.getCode(), "NewCamp");
    Assert.assertEquals(obtainedCampaign.getTitle(), "New");
    Assert.assertEquals(obtainedCampaign.getDiscountType().toString(), "PERCENTAGE");
    Assert.assertEquals(obtainedCampaign.getPercentOff().toString(), "100");
  }

  /*
   * Test to edit existing Campaign in Expired State as Root User
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test(expected = InvalidAjaxRequestException.class)
  public void testEditExpiredStateCampaignAsRoot() throws Exception {

    CampaignPromotion cp = campaignPromotionDAO.find(1L);
    cp.setTitle("New_Title");
    cp.setCode("New_PromoCode");
    cp.setPromoCode("New_PromoCode");
    cp.setEndDate(DateUtils.minusOneDay(new Date()));
    campaignPromotionDAO.save(cp);

    cp = campaignPromotionDAO.find(1L);
    Assert.assertEquals("EXPIRED", cp.getState().getName());
    CampaignPromotionsForm form = new CampaignPromotionsForm(cp);
    form.setPromoCode(cp.getCode());
    BindingResult result = validate(form);
    CampaignPromotion obtainedCampaign = campaignController.edit(form, result, map);

    Assert.assertNotNull(obtainedCampaign);
    Assert.assertNotSame("New_Code", obtainedCampaign.getCode());
    Assert.assertEquals("EXPIRED", obtainedCampaign.getState().toString());
    Assert.assertEquals("New_Title", obtainedCampaign.getTitle());
    Assert.assertNotSame("New_PromoCode", obtainedCampaign.getPromoCode());
  }

  /*
   * Test to edit existing Campaign in Expired State as Product Manager User
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test(expected = InvalidAjaxRequestException.class)
  public void testEditExpiredStateCampaignAsProductManager() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    CampaignPromotion cp = campaignPromotionDAO.find(1L);
    cp.setTitle("New_Title");
    cp.setCode("New_Code");
    cp.setPromoCode("New_PromoCode");
    cp.setEndDate(DateUtils.minusOneDay(new Date()));
    campaignPromotionDAO.save(cp);

    cp = campaignPromotionDAO.find(1L);
    CampaignPromotionsForm form = new CampaignPromotionsForm(cp);
    BindingResult result = validate(form);
    form.setPromoCode(cp.getCode());
    CampaignPromotion obtainedCampaign = campaignController.edit(form, result, map);

    Assert.assertNotNull(obtainedCampaign);
    Assert.assertNotSame("New_PromoCode", obtainedCampaign.getCode());
    Assert.assertEquals("EXPIRED", obtainedCampaign.getState().toString());
    Assert.assertEquals("New_Title", obtainedCampaign.getTitle());
    Assert.assertNotSame("New_PromoCode", obtainedCampaign.getPromoCode());
  }

  /*
   * Test to edit existing Campaign in Active State as Root User
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test(expected = InvalidAjaxRequestException.class)
  public void testEditActiveStateCampaignAsRoot() throws Exception {

    CampaignPromotion campaignPromotion = generateCampaignPromotion(1, true, false, false);
    CampaignPromotionsForm form = new CampaignPromotionsForm(campaignPromotion);
    form.setPromoCode("PromoCode");
    form.setChannel(channelService.getChannelById("1"));
    form.setUnlimited(true);
    BindingResult result = validate(form);

    CampaignPromotion cp = campaignController.createCampaigns(form, result, map, response);

    cp.setTitle("New_Title");
    cp.setCode("New_Code");
    cp.setPromoCode("New_Promocode");
    cp.setEndDate(DateUtils.addDays(new Date(), 10));
    cp.setMaxAccounts(2);
    campaignPromotionDAO.save(cp);

    cp = campaignPromotionDAO.findByCode("New_Code");
    Assert.assertEquals(cp.getState().toString(), "ACTIVE");
    cp.setCode("New_Code2");
    CampaignPromotionsForm cpform = new CampaignPromotionsForm(cp);
    BindingResult cpresult = validate(cpform);
    cpform.setPromoCode(cp.getCode());
    CampaignPromotion obtainedCampaign = campaignController.edit(cpform, cpresult, map);

    Assert.assertNotNull(obtainedCampaign);
    Assert.assertNotSame("New_Code1", obtainedCampaign.getCode());
    Assert.assertNotSame("New_Promocode", obtainedCampaign.getPromoCode());
    Assert.assertNotSame(2, obtainedCampaign.getMaxAccounts());
    Assert.assertEquals("ACTIVE", cp.getState().toString());
    Assert.assertEquals("New_Title", obtainedCampaign.getTitle());
  }

  /*
   * Test to edit existing Campaign in Scheduled State as Root User
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test
  public void testEditScheduledStateCampaignAsRoot() throws Exception {

    CampaignPromotion campaignPromotion = generateCampaignPromotion(-2, true, false, false);
    CampaignPromotionsForm form = new CampaignPromotionsForm(campaignPromotion);
    form.setPromoCode("PromoCode");
    form.setChannel(channelService.getChannelById("1"));
    form.setUnlimited(true);
    BindingResult result = validate(form);
    CampaignPromotion cp = campaignController.createCampaigns(form, result, map, response);
    cp.setTitle("New_Title");
    cp.setCode("New_Code");
    cp.setMaxAccounts(2);
    Assert.assertEquals(cp.getState().toString(), "SCHEDULED");
    CampaignPromotionsForm cpform = new CampaignPromotionsForm(cp);
    cpform.setPromoCode("New_Code");
    cpform.setChannel(channelService.getChannelById("1"));
    cpform.setUnlimited(true);
    BindingResult cpresult = validate(cpform);
    CampaignPromotion obtainedCampaign = campaignController.edit(cpform, cpresult, map);
    Assert.assertNotNull(obtainedCampaign);
    Assert.assertEquals(obtainedCampaign.getTitle(), "New_Title");
    Assert.assertEquals(obtainedCampaign.getCode(), "New_Code");
    Assert.assertEquals(obtainedCampaign.getMaxAccounts(), 2);
    Assert.assertEquals(cp.getState().toString(), "SCHEDULED");
  }

  /*
   * Test to edit existing Campaign in Scheduled State as Product Manager User
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test
  public void testEditScheduledStateCampaignAsProductManager() throws Exception {

    User user = userDAO.find("3");
    user.setProfile(profileDAO.find("7"));
    userDAO.save(user);
    asUser(user);
    CampaignPromotion campaignPromotion = generateCampaignPromotion(-2, true, false, false);
    CampaignPromotionsForm form = new CampaignPromotionsForm(campaignPromotion);
    form.setPromoCode("PromoCode");
    form.setChannel(channelService.getChannelById("1"));
    form.setUnlimited(true);
    BindingResult result = validate(form);
    CampaignPromotion cp = campaignController.createCampaigns(form, result, map, response);
    cp.setTitle("New_Title");
    cp.setCode("New_Code");
    cp.setMaxAccounts(2);
    Assert.assertEquals(cp.getState().toString(), "SCHEDULED");
    CampaignPromotionsForm cpform = new CampaignPromotionsForm(cp);
    BindingResult cpresult = validate(cpform);
    cpform.setPromoCode("New_Code");
    cpform.setChannel(channelService.getChannelById("1"));
    cpform.setUnlimited(true);
    CampaignPromotion obtainedCampaign = campaignController.edit(cpform, cpresult, map);
    Assert.assertNotNull(obtainedCampaign);
    Assert.assertEquals(obtainedCampaign.getTitle(), "New_Title");
    Assert.assertEquals(obtainedCampaign.getCode(), "New_Code");
    Assert.assertEquals(obtainedCampaign.getMaxAccounts(), 2);
    Assert.assertEquals(cp.getState().toString(), "SCHEDULED");
  }

  @Test
  public void testShow() {
    CampaignPromotion campaignPromotion = promotionService.locateCampaignById("1");
    String promotionview = campaignController.show("1", map);
    Assert.assertNotNull(promotionview);
    Assert.assertEquals(promotionview, new String("promotions.view"));
    Assert.assertTrue(map.containsAttribute("campaign"));
    Assert.assertEquals(map.get("campaign"), campaignPromotion);
  }

  @Test
  public void testcreateCampaigns() {
    List<CurrencyValue> supportedCurrencies = currencyValueService.listActiveCurrencies();
    String newPromotion = campaignController.createCampaigns(map);
    Assert.assertNotNull(newPromotion);
    Assert.assertEquals(newPromotion, new String("promotions.new"));
    Assert.assertTrue(map.containsAttribute("campaignPromotionsForm"));
    Assert.assertTrue(map.containsAttribute("channels"));
    Assert.assertTrue(map.containsAttribute("PERCENTAGE"));
    Assert.assertTrue(map.containsAttribute("FIXED_AMOUNT"));
    Assert.assertTrue(map.containsAttribute("supportedCurrencies"));
    Assert.assertNotNull(map.get("campaignPromotionsForm"));
    Assert.assertEquals(map.get("channels"), channelService.getChannels(null, null, null));
    Assert.assertEquals(map.get("PERCENTAGE"), DiscountType.PERCENTAGE);
    Assert.assertEquals(map.get("supportedCurrencies"), supportedCurrencies);

  }

  @Test
  public void testgetSupportedCurrencyForChannels() {
    List<String> channelIdLst = new ArrayList<String>();
    channelIdLst.add("1");
    channelIdLst.add("3");
    Map<String, BigDecimal> discountAmountMap = new HashMap<String, BigDecimal>();
    discountAmountMap.put("Amount", BigDecimal.valueOf(30));
    CampaignPromotionsForm form = new CampaignPromotionsForm();
    form.setChannelIdLst(channelIdLst);
    form.setDiscountAmountMap(discountAmountMap);

    String newsupportedCurrency = campaignController.getSupportedCurrencyForChannels(form, map);
    Assert.assertNotNull(newsupportedCurrency);
    Assert.assertEquals(newsupportedCurrency, "promotions.new.supportedCurrency");
    Assert.assertTrue(map.containsAttribute("supportedCurrenciesForChannel"));

    Catalog catalog1 = bundleservice.getCatalog(1L);
    Catalog catalog2 = bundleservice.getCatalog(3L);

    Set<SupportedCurrency> set = ((Set<SupportedCurrency>) map.get("supportedCurrenciesForChannel"));
    Assert.assertTrue(set.contains(catalog1.getSupportedCurrencyValuesByOrder().get(0)));
    Assert.assertTrue(set.contains(catalog2.getSupportedCurrencyValuesByOrder().get(0)));
    Assert.assertTrue(set.contains(catalog2.getSupportedCurrencyValuesByOrder().get(0)));

  }

  @Test
  public void testSupportAllChannels() {
    Map<String, BigDecimal> discountAmountMap = new HashMap<String, BigDecimal>();
    discountAmountMap.put("Amount", BigDecimal.valueOf(30));
    CampaignPromotionsForm form = new CampaignPromotionsForm();
    form.setDiscountAmountMap(discountAmountMap);
    form.setChannelIdLst(null);
    campaignController.getSupportedCurrencyForChannels(form, map);
    Set<SupportedCurrency> set = ((Set<SupportedCurrency>) map.get("supportedCurrenciesForChannel"));
    List<Catalog> lstCatalog = new ArrayList<Catalog>();
    List<Channel> channelLst = channelService.getChannels(null, null, null);
    Set<CurrencyValue> currValSet = new HashSet<CurrencyValue>();
    for (Channel channel : channelLst) {
      lstCatalog.add(channel.getCatalog());
    }
    for (Catalog catalog : lstCatalog) {
      for (SupportedCurrency suppCurr : catalog.getSupportedCurrencies())
        currValSet.add(suppCurr.getCurrency());
    }

    Assert.assertEquals(set, currValSet);

  }

  @Test
  public void testlist() {

    List<CampaignPromotion> campaignsList = promotionService.listCampaignPromotions(1, 2);
    String promtionlist = campaignController.list("Test", 1, map);
    Assert.assertNotNull(promtionlist);
    Assert.assertEquals(promtionlist, new String("promotions.list"));
    Assert.assertTrue(map.containsAttribute("pageSize"));
    Assert.assertTrue(map.containsAttribute("campaignsList"));
    Assert.assertTrue(map.containsAttribute("campaignsListSize"));
    Assert.assertTrue(map.containsAttribute("idToBeSelected"));
    Assert.assertTrue(map.containsAttribute("enable_next"));
    Assert.assertEquals(map.get("campaignsListSize"), campaignsList.size());
    Assert.assertEquals(map.get("campaignsList"), campaignsList);
    Assert.assertEquals(map.get("idToBeSelected"), new String("Test"));
  }

  @Test
  public void testvalidateCampaignPromoCode() {
    String valid = campaignController.validateCampaignPromoCode("tria_camp");
    Assert.assertEquals(valid, "true");
  }

  @Test
  public void testcreateTrialToken() {
    List<CampaignPromotion> campaignPromotions = promotionService.listCampaignPromotions(0, 0);
    String token = campaignController.createTrialToken(map);
    Assert.assertNotNull(token);
    Assert.assertEquals(token, new String("campaign.token.create"));
    Assert.assertTrue(map.containsAttribute("tokenRequest"));
    Assert.assertTrue(map.containsAttribute("campaignPromotions"));
    Assert.assertEquals(map.get("campaignPromotions"), campaignPromotions);
    Assert.assertNotNull(map.get("tokenRequest"));

  }

  // check problen in generatePromotionToken() in PromotionServiceImpl
  @Test
  public void testemailValidationFailed() throws Exception {
    CampaignPromotion campaign = promotionService.locateCampaignById("1");

    PromotionSignup promotionSignup = new PromotionSignup("test" + random.nextInt(), "Citrix", "abcTest@citrix.com");
    promotionSignup.setCreateBy(getRootUser());
    promotionSignup.setCurrency(Currency.getInstance("USD"));
    promotionSignup.setPhone("9999999999");

    PromotionToken promotionToken = new PromotionToken(campaign, "USD" + random.nextInt());
    promotionToken.setCreateBy(getRootUser());
    tokendao.save(promotionToken);

    promotionSignup.setPromotionToken(promotionToken);
    SignUpdao.save(promotionSignup);
    List<CampaignPromotion> campaignPromotions = promotionService.listCampaignPromotions(0, 0);
    TokenRequestForm form = new TokenRequestForm();
    form.setCampaignCode("trial_camp");
    form.setPromotionSignup(promotionSignup);
    DepositRecordForm recordForm = new DepositRecordForm();
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
    recordForm.setReceivedOn(sdf.format(getDaysFromNow(-1)));
    recordForm.setAmount("1000.00");
    BindingResult result = validate(recordForm);
    String token = campaignController.createTrialToken(form, result, map);
    Assert.assertNotNull(token);
    Assert.assertEquals(token, new String("campaign.token.create"));
    Assert.assertTrue(map.containsAttribute("campaignPromotions"));
    Assert.assertTrue(map.containsAttribute("emailValidationFailed"));
    Assert.assertTrue(map.containsAttribute("tokenRequest"));
    Assert.assertEquals(map.get("tokenRequest"), form);
    Assert.assertEquals(map.get("campaignPromotions"), campaignPromotions);

  }

  @Test
  public void testGetCampaignPromotionFromForm() throws Exception {
    try {
      Channel channel = channelService.getChannelById("3");
      List<String> channelIdLst = new ArrayList<String>();
      channelIdLst.add("3");
      CampaignPromotion campaignPromotion = promotionService.locateCampaignById("1");
      CampaignPromotionsForm form = new CampaignPromotionsForm();
      form.setChannelIdLst(channelIdLst);
      form.setPromoCode("trial_camp");
      form.setUnlimited(true);
      form.setCampaignPromotion(campaignPromotion);
      BindingResult result = validate(form);
      CampaignPromotion promotion = campaignController.createCampaigns(form, result, map, null);
      Assert.assertNotNull(promotion);
      Assert.assertEquals(promotion.getDiscountType(), DiscountType.PERCENTAGE);
      Assert.assertEquals(promotion.getPercentOff(), new BigDecimal(100));
      Assert.assertEquals(promotion.getPromoCode(), "trial_camp");
      Iterator<CampaignPromotionsInChannels> itr = promotion.getCampaignPromotionsInChannels().iterator();
      CampaignPromotionsInChannels objCampPromo = null;
      objCampPromo = (CampaignPromotionsInChannels) itr.next();
      Assert.assertEquals(objCampPromo.getChannel(), channel);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      Assert.fail();
    }
  }

  @Test
  public void testThrowAjaxException() throws Exception {
    try {
      CampaignPromotionsForm form = new CampaignPromotionsForm();
      form.setPromoCode("trial_camp");
      BindingResult result = validate(form);
      campaignController.createCampaigns(form, result, map, null);
      Assert.fail();
    } catch (AjaxFormValidationException e) {
    }
  }

  @Test
  public void testCreateCampaignsAddDiscountAmount() throws Exception {
    try {
      CampaignPromotion campaignPromotion = generateCampaignPromotion(0, false, true, false);
      CurrencyValue currency = currencyValueService.locateBYCurrencyCode("INR");
      List<String> channelIdLst = new ArrayList<String>();
      channelIdLst.add("3");
      Map<String, BigDecimal> discountAmountMap = new HashMap<String, BigDecimal>();
      discountAmountMap.put(currency.getCurrencyCode(), new BigDecimal(100));
      CampaignPromotionsForm form = new CampaignPromotionsForm();
      form.setDiscountAmountMap(discountAmountMap);
      form.setChannelIdLst(channelIdLst);
      form.setPromoCode("trial_camp");
      form.setUnlimited(true);
      form.setCampaignPromotion(campaignPromotion);
      BindingResult result = validate(form);
      CampaignPromotion promotion = campaignController.createCampaigns(form, result, map, null);
      Assert.assertNull(promotion.getPercentOff());
      Assert.assertNotNull(promotion.getCampaignPromotionDiscountAmount());
      Iterator<CampaignPromotionDiscountAmount> itr = promotion.getCampaignPromotionDiscountAmount().iterator();
      Assert.assertTrue(itr.hasNext());
      CampaignPromotionDiscountAmount CPDA = (CampaignPromotionDiscountAmount) itr.next();
      Assert.assertEquals(CPDA.getDiscount(), new BigDecimal(100));
      Assert.assertEquals(CPDA.getCurrencyValue(), currency);

    } catch (Exception e) {
      // TODO Auto-generated catch block
      Assert.fail();
    }

  }

  @Test
  public void testEditForDiscountAmount() throws Exception {
    try {
      CampaignPromotion campaignPromotion = generateCampaignPromotion(0, false, true, false);
      CurrencyValue currency = currencyValueService.locateBYCurrencyCode("INR");
      List<String> channelIdLst = new ArrayList<String>();
      channelIdLst.add("3");
      Map<String, BigDecimal> discountAmountMap = new HashMap<String, BigDecimal>();
      discountAmountMap.put(currency.getCurrencyCode(), new BigDecimal(100));
      CampaignPromotionsForm form = new CampaignPromotionsForm();
      form.setDiscountAmountMap(discountAmountMap);
      form.setChannelIdLst(channelIdLst);
      form.setPromoCode("edit_camp");
      form.setUnlimited(true);
      form.setCampaignPromotion(campaignPromotion);
      BindingResult result = validate(form);
      CampaignPromotion promotion = campaignController.createCampaigns(form, result, map, null);
      Assert.assertNull(promotion.getPercentOff());
      Assert.assertNotNull(promotion.getCampaignPromotionDiscountAmount());

      ModelMap model = new ModelMap();

      campaignController.edit(promotion.getId().toString(), model);
      CampaignPromotionsForm campaignPromotionsForm = new CampaignPromotionsForm(promotion);
      campaignPromotionsForm.setPromoCode(promotion.getPromoCode());

      CampaignPromotionsForm form1 = (CampaignPromotionsForm) model.get("campaignPromotionsForm");
      List<Channel> channels = (List) model.get("channels");

      Assert.assertTrue(form1.getPromoCode().toString().equals(campaignPromotionsForm.getPromoCode()));
      Assert
          .assertTrue(channels.get(0).getName().equals(channelService.getChannels(null, null, null).get(0).getName()));

    } catch (Exception e) {
      // TODO Auto-generated catch block
      Assert.fail();
    }

  }

  @Test
  public void testEditForPercentage() throws Exception {
    try {
      CampaignPromotion campaignPromotion = generateCampaignPromotion(0, false, false, true);
      CurrencyValue currency = currencyValueService.locateBYCurrencyCode("INR");
      List<String> channelIdLst = new ArrayList<String>();
      channelIdLst.add("3");
      Map<String, BigDecimal> discountAmountMap = new HashMap<String, BigDecimal>();
      discountAmountMap.put(currency.getCurrencyCode(), new BigDecimal(100));
      CampaignPromotionsForm form = new CampaignPromotionsForm();
      form.setDiscountAmountMap(discountAmountMap);
      form.setChannelIdLst(channelIdLst);
      form.setPromoCode("edit_camp2");
      form.setUnlimited(true);
      form.setCampaignPromotion(campaignPromotion);
      BindingResult result = validate(form);
      CampaignPromotion promotion = campaignController.createCampaigns(form, result, map, null);

      ModelMap model = new ModelMap();

      campaignController.edit(promotion.getId().toString(), model);
      CampaignPromotionsForm campaignPromotionsForm = new CampaignPromotionsForm(promotion);
      campaignPromotionsForm.setPromoCode(promotion.getPromoCode());

      CampaignPromotionsForm form1 = (CampaignPromotionsForm) model.get("campaignPromotionsForm");
      List<Channel> channels = (List) model.get("channels");

      Assert.assertTrue(form1.getPromoCode().toString().equals(campaignPromotionsForm.getPromoCode()));
      Assert
          .assertTrue(channels.get(0).getName().equals(channelService.getChannels(null, null, null).get(0).getName()));

    } catch (Exception e) {
      // TODO Auto-generated catch block
      Assert.fail();
    }

  }

  @Test
  public void testlistNextEnable() throws Exception {

    for (int i = 0; i < 15; i++) {
      CampaignPromotion campaignPromotion = generateCampaignPromotion(0, false, false, true);
      CurrencyValue currency = currencyValueService.locateBYCurrencyCode("INR");
      List<String> channelIdLst = new ArrayList<String>();
      channelIdLst.add("3");
      Map<String, BigDecimal> discountAmountMap = new HashMap<String, BigDecimal>();
      discountAmountMap.put(currency.getCurrencyCode(), new BigDecimal(100));
      CampaignPromotionsForm form = new CampaignPromotionsForm();
      form.setDiscountAmountMap(discountAmountMap);
      form.setChannelIdLst(channelIdLst);
      form.setPromoCode("edit_camp_nxt" + i);
      form.setUnlimited(true);
      form.setCampaignPromotion(campaignPromotion);
      BindingResult result = validate(form);
      campaignController.createCampaigns(form, result, map, null);

    }

    List<CampaignPromotion> campaignsList = promotionService.listCampaignPromotions(1, 16);
    String promtionlist = campaignController.list("Test", 1, map);
    Assert.assertNotNull(promtionlist);
    Assert.assertEquals(promtionlist, new String("promotions.list"));
    Assert.assertTrue(map.containsAttribute("pageSize"));
    Assert.assertTrue(map.containsAttribute("campaignsList"));
    Assert.assertTrue(map.containsAttribute("campaignsListSize"));
    Assert.assertTrue(map.containsAttribute("idToBeSelected"));
    Assert.assertTrue(map.containsAttribute("enable_next"));
    Assert.assertNotSame(map.get("campaignsListSize"), campaignsList.size());
    Assert.assertNotSame(map.get("campaignsList"), campaignsList);
    Assert.assertEquals(map.get("idToBeSelected"), new String("Test"));
  }

  @Test
  public void testValidateNonUniquePromoCode() throws Exception {
    try {
      CampaignPromotion campaignPromotion = generateCampaignPromotion(0, false, true, false);
      CurrencyValue currency = currencyValueService.locateBYCurrencyCode("INR");
      List<String> channelIdLst = new ArrayList<String>();
      channelIdLst.add("3");
      Map<String, BigDecimal> discountAmountMap = new HashMap<String, BigDecimal>();
      discountAmountMap.put(currency.getCurrencyCode(), new BigDecimal(100));
      CampaignPromotionsForm form = new CampaignPromotionsForm();
      form.setDiscountAmountMap(discountAmountMap);
      form.setChannelIdLst(channelIdLst);
      form.setPromoCode("trial_camp_validate");
      form.setUnlimited(true);
      form.setCampaignPromotion(campaignPromotion);
      BindingResult result = validate(form);
      campaignController.createCampaigns(form, result, map, null);
      String validation = campaignController.validateCampaignPromoCode("trial_camp_validate");
      Assert.assertTrue(validation.equalsIgnoreCase(Boolean.FALSE.toString()));

    } catch (Exception e) {
      // TODO Auto-generated catch block
      Assert.fail();
    }
  }
}
