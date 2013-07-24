/* Copyright (C) 2011 Citrix Systems, Inc. All rights reserved. */
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;

import web.WebTestsBase;

import citrix.cpbm.portal.fragment.controllers.CampaignPromotionsController;

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
  
  private MockHttpServletRequest request;

  private MockHttpServletResponse response;

  @Before
  public void init() throws Exception {
    map = new ModelMap();
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();

  }
  
  /* Method to generate Campaign Promotion object based
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  private CampaignPromotion generateCampaignPromotion(int days, boolean trial, boolean fixedAmount, boolean percentage){
		
		CampaignPromotion campaignProm  = new CampaignPromotion();
		if(fixedAmount){
			campaignProm.setDiscountType(DiscountType.FIXED_AMOUNT);
		}
		if(percentage){
			campaignProm.setDiscountType(DiscountType.PERCENTAGE);
			campaignProm.setPercentOff(BigDecimal.TEN);
		}
		campaignProm.setCode("NewCamp");
		campaignProm.setTitle("New");
		campaignProm.setCreateBy(getPortalUser());
		campaignProm.setCreateDate(new Date());
		int noOfdays = days;
		Calendar createdAt = Calendar.getInstance();
		createdAt.add(Calendar.DATE, 0 - noOfdays); 
		campaignProm.setStartDate(createdAt.getTime());
		campaignProm.setTrial(trial);
		return campaignProm;
  }
	
  /* Test to create Campaign for Non Trial users with percentage value as Root User
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test
  public void testCreateCampaignForNonTrialWithPercentageAsRoot() throws Exception{
	  
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
  
  /* Test to create Campaign for Trial users as Root User
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test
  public void testCreateCampaignForTrialAsRoot() throws Exception{
	  
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
  
  /* Test to create Campaign for Trial users as Product Manager User
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test
  public void testCreateCampaignForTrialAsProductManager() throws Exception{
	  
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
  
  /* Test to create Campaign for Trial users as Master User //master user is able to create product bundle which is an error
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test(expected=Exception.class)
  public void testCreateCampaignForTrialAsMaster() throws Exception{
	  
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
	  Assert.assertEquals(obtainedCampaign, campaignPromotion);
	  Assert.assertEquals(obtainedCampaign.isTrial(), true);
	  Assert.assertEquals(obtainedCampaign.getState().toString(), "ACTIVE");
	  Assert.assertEquals(obtainedCampaign.getCode(), "NewCamp");
	  Assert.assertEquals(obtainedCampaign.getTitle(), "New");
	  Assert.assertEquals(obtainedCampaign.getDiscountType().toString(), "PERCENTAGE");
	  Assert.assertEquals(obtainedCampaign.getPercentOff().toString(), "100");
  }
  
  /* Test to edit existing Campaign in Expired State as Root User
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test(expected=Exception.class)
  public void testEditExpiredStateCampaignAsRoot() throws Exception{
	  
	  CampaignPromotion cp = campaignPromotionDAO.find(1L);
	  cp.setTitle("New_Title");
	  cp.setCode("New_PromoCode");
	  Assert.assertEquals(cp.getState().toString(), "EXPIRED");
	  CampaignPromotionsForm form = new CampaignPromotionsForm(cp);
	  BindingResult result = validate(form);
	  CampaignPromotion obtainedCampaign = campaignController.edit(form, result, map);
	  Assert.assertNotNull(obtainedCampaign);
	  Assert.assertEquals(obtainedCampaign.getTitle(), "New_Title");
	  Assert.assertEquals(obtainedCampaign.getCode(), "New_PromoCode");
	  Assert.assertEquals(obtainedCampaign.getState().toString(), "EXPIRED");
  }
  
  /* Test to edit existing Campaign in Expired State as Product Manager User
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test(expected=Exception.class)
  public void testEditExpiredStateCampaignAsProductManager() throws Exception{
	  
	  User user = userDAO.find("3");
	  user.setProfile(profileDAO.find("7"));
	  userDAO.save(user);
	  asUser(user);
	  CampaignPromotion cp = campaignPromotionDAO.find(1L);
	  cp.setTitle("New_Title");
	  cp.setCode("New_PromoCode");
	  Assert.assertEquals(cp.getState().toString(), "EXPIRED");
	  CampaignPromotionsForm form = new CampaignPromotionsForm(cp);
	  BindingResult result = validate(form);
	  CampaignPromotion obtainedCampaign = campaignController.edit(form, result, map);
	  Assert.assertNotNull(obtainedCampaign);
	  Assert.assertEquals(obtainedCampaign.getTitle(), "New_Title");
	  Assert.assertEquals(obtainedCampaign.getCode(), "New_PromoCode");
	  Assert.assertEquals(cp.getState().toString(), "EXPIRED");
  }
  
  /* Test to edit existing Campaign in Active State as Root User
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test(expected=Exception.class)
  public void testEditActiveStateCampaignAsRoot() throws Exception{
	  
	  CampaignPromotion campaignPromotion = generateCampaignPromotion(1, true, false, false);
	  CampaignPromotionsForm form = new CampaignPromotionsForm(campaignPromotion);
	  form.setPromoCode("PromoCode");
	  form.setChannel(channelService.getChannelById("1"));
	  form.setUnlimited(true);	    
	  BindingResult result = validate(form);
	  CampaignPromotion cp = campaignController.createCampaigns(form, result, map, response);
	  cp.setTitle("New_Title");
	  cp.setCode("New_Code");
	  cp.setMaxAccounts(2);
	  Assert.assertEquals(cp.getState().toString(), "ACTIVE");
	  CampaignPromotionsForm cpform = new CampaignPromotionsForm(cp);
	  BindingResult cpresult = validate(cpform);
	  CampaignPromotion obtainedCampaign = campaignController.edit(cpform, cpresult, map);
	  Assert.assertNotNull(obtainedCampaign);
	  Assert.assertEquals(obtainedCampaign.getTitle(), "New_Title");
	  Assert.assertEquals(obtainedCampaign.getCode(), "New_Code");
	  Assert.assertEquals(obtainedCampaign.getMaxAccounts(), 2);
	  Assert.assertEquals(cp.getState().toString(), "ACTIVE");
  }
  
  /* Test to edit existing Campaign in Scheduled State as Root User
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test
  public void testEditScheduledStateCampaignAsRoot() throws Exception{
	  
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
	  CampaignPromotion obtainedCampaign = campaignController.edit(cpform, cpresult, map);
	  Assert.assertNotNull(obtainedCampaign);
	  Assert.assertEquals(obtainedCampaign.getTitle(), "New_Title");
	  Assert.assertEquals(obtainedCampaign.getCode(), "New_Code");
	  Assert.assertEquals(obtainedCampaign.getMaxAccounts(), 2);
	  Assert.assertEquals(cp.getState().toString(), "SCHEDULED");
  }
  
  /* Test to edit existing Campaign in Scheduled State as Product Manager User
   * @Author: Vinayv
   * @Reviewer: NageswaraP
   */
  @Test
  public void testEditScheduledStateCampaignAsProductManager() throws Exception{
	  
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
	  CampaignPromotion obtainedCampaign = campaignController.edit(cpform, cpresult, map);
	  Assert.assertNotNull(obtainedCampaign);
	  Assert.assertEquals(obtainedCampaign.getTitle(), "New_Title");
	  Assert.assertEquals(obtainedCampaign.getCode(), "New_Code");
	  Assert.assertEquals(obtainedCampaign.getMaxAccounts(), 2);
	  Assert.assertEquals(cp.getState().toString(), "SCHEDULED");
  }
/*
  @Test
  public void testShow() {
    CampaignPromotion campaignPromotion = promotionService.locateCampaignById("1");
    String promotionview = controller.show("1", map);
    Assert.assertNotNull(promotionview);
    Assert.assertEquals(promotionview, new String("promotions.view"));
    Assert.assertTrue(map.containsAttribute("campaign"));
    Assert.assertEquals(map.get("campaign"), campaignPromotion);
  }

  @Test
  public void testcreateCampaigns() {
    List<CurrencyValue> supportedCurrencies = currencyValueService.listActiveCurrencies();
    String newPromotion = controller.createCampaigns(map);
    Assert.assertNotNull(newPromotion);
    Assert.assertEquals(newPromotion, new String("promotions.new"));
    Assert.assertTrue(map.containsAttribute("campaignPromotionsForm"));
    Assert.assertTrue(map.containsAttribute("channels"));
    Assert.assertTrue(map.containsAttribute("PERCENTAGE"));
    Assert.assertTrue(map.containsAttribute("FIXED_AMOUNT"));
    Assert.assertTrue(map.containsAttribute("supportedCurrencies"));
    Assert.assertNotNull(map.get("campaignPromotionsForm"));
    Assert.assertEquals(map.get("channels"), channelService.getChannels());
    Assert.assertEquals(map.get("PERCENTAGE"), DiscountType.PERCENTAGE);
    Assert.assertEquals(map.get("supportedCurrencies"), supportedCurrencies);

  }

  @Test
  public void test_createCampaigns() {
    Catalog catalog = bundleservice.getCatalog(1L);
    List<CurrencyValue> list = controller.createCampaigns(catalog.getId(), map);
    Assert.assertNotNull(list);
    Assert.assertTrue(list.size() > 0);
    Assert.assertEquals(list, catalog.getSupportedCurrencyValuesByOrder());
    Assert.assertTrue(map.containsAttribute("supportedCurrencies"));
    Assert.assertEquals(map.get("supportedCurrencies"), list);
    map.clear();
    list = controller.createCampaigns(null, map);
    Assert.assertEquals(list, currencyValueService.listActiveCurrencies());
    Assert.assertEquals(map.get("supportedCurrencies"), list);
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

    String newsupportedCurrency = controller.getSupportedCurrencyForChannels(form, map);
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
    controller.getSupportedCurrencyForChannels(form, map);
    Set<SupportedCurrency> set = ((Set<SupportedCurrency>) map.get("supportedCurrenciesForChannel"));
    List<Catalog> lstCatalog = new ArrayList<Catalog>();
    List<Channel> channelLst = channelService.getChannels();
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
    String promtionlist = controller.list("Test", 1, map);
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
  public void testedit() {
    CampaignPromotion campaign = promotionService.locateCampaignById("1");
    
     * System.out.println("check---"+campaign.getCampaignPromotionDiscountAmount ());
     * campaign.getCampaignPromotionDiscountAmount().clear(); DAO.save(campaign); CampaignPromotionDiscountAmount
     * promotionDiscountAmount = new CampaignPromotionDiscountAmount();
     * promotionDiscountAmount.setDiscount(BigDecimal.valueOf(100));
     * promotionDiscountAmount.setCampaignPromotion(campaign);
     * promotionDiscountAmount.setCurrencyValue(currencyDAO.find(1L)); Set<CampaignPromotionDiscountAmount>
     * campaignPromotionDiscountAmount = new HashSet<CampaignPromotionDiscountAmount>();
     * campaignPromotionDiscountAmount.add(promotionDiscountAmount); campaign
     * .setCampaignPromotionDiscountAmount(campaignPromotionDiscountAmount); DAO.save(campaign);
     
    String edit = controller.edit("1", map);
    Assert.assertNotNull(edit);
    Assert.assertEquals(edit, new String("promotions.edit"));
    Assert.assertTrue(map.containsAttribute("campaignPromotionsForm"));
    Assert.assertTrue(map.containsAttribute("channels"));
    Assert.assertTrue(map.containsAttribute("restrictedEdit"));
    Assert.assertTrue(map.containsAttribute("PERCENTAGE"));
    Assert.assertTrue(map.containsAttribute("FIXED_AMOUNT"));
    Assert.assertEquals(map.get("channels"), channelService.getChannels());
    Assert.assertEquals(map.get("PERCENTAGE"), DiscountType.PERCENTAGE);
    CampaignPromotionsForm campaignPromotionsForm = ((CampaignPromotionsForm) map.get("campaignPromotionsForm"));
    Assert.assertEquals(campaignPromotionsForm.getPromoCode(), campaign.getPromoCode());
    Assert.assertNotNull(campaignPromotionsForm.getDiscountAmountMap());

  }

  
   * @Test public void testeditCampaignPromotion () { List<String> channelIdLst = new ArrayList<String>();
   * channelIdLst.add("1"); channelIdLst.add("3"); Map<String, BigDecimal> discountAmountMap = new HashMap<String,
   * BigDecimal>(); discountAmountMap.put("Amount", BigDecimal.valueOf(30)); CampaignPromotion campaign =
   * promotionService.locateCampaignById("1"); campaign.setDiscountType(DiscountType.FIXED_AMOUNT);
   * campaign.setTrial(false); promotionService.createCampain(campaign); CampaignPromotionsForm form = new
   * CampaignPromotionsForm(); form.setCampaignPromotion(campaign); form.setPromoCode(campaign.getPromoCode());
   * form.setUnlimited(true); form.setChannelIdLst(channelIdLst); form.setDiscountAmountMap(discountAmountMap);
   * CampaignPromotion promotionEdit = controller.edit(form, null, map);
   * //Assert.assertEquals(promotionEdit.getDiscountType(), DiscountType.PERCENTAGE);
   * //Assert.assertEquals(promotionEdit.getPercentOff(), new BigDecimal(100));
   * Assert.assertNotNull(promotionEdit.getCampaignPromotionDiscountAmount());
   * Assert.assertNull(promotionEdit.getPercentOff()); }
   
  @Test
  public void testvalidateCampaignCode() {
    String valid = controller.validateCampaignCode("trial_camp");
    Assert.assertEquals(valid, new String("false"));

  }

  @Test
  public void testInvalidCampaignCode() {
    String valid = controller.validateCampaignCode("TEST_INVALID_CODE");
    Assert.assertEquals(valid, new String("true"));

  }

  @Test
  public void testvalidateCampaignPromoCode() {
    String valid = controller.validateCampaignPromoCode("tria_camp");
    Assert.assertEquals(valid, "true");
  }

  @Test
  public void testcreateTrialToken() {
    List<CampaignPromotion> campaignPromotions = promotionService.listCampaignPromotions(0, 0);
    String token = controller.createTrialToken(map);
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
    String token = controller.createTrialToken(form, result, map);
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
      CampaignPromotion promotion = controller.createCampaigns(form, result, map, null);
      Assert.assertNotNull(promotion);
      Assert.assertEquals(promotion.getDiscountType(), DiscountType.PERCENTAGE);
      Assert.assertEquals(promotion.getPercentOff(), new BigDecimal(100));
      Assert.assertEquals(promotion.getPromoCode(), "trial_camp");
      Iterator itr = promotion.getCampaignPromotionsInChannels().iterator();
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
      CampaignPromotion promotion = controller.createCampaigns(form, result, map, null);
      Assert.fail();
    } catch (AjaxFormValidationException e) {
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CampaignPromotion createCamp() {
    CampaignPromotion campaignPromotion = new CampaignPromotion();

    campaignPromotion.setCode("trial_camp");
    campaignPromotion.setTitle("trial_camp");
    campaignPromotion.setCreateBy(getPortalUser());
    campaignPromotion.setUpdateBy(null);
    campaignPromotion.setStartDate(new Date());
    campaignPromotion.setEndDate(DateUtils.addOneDay(new Date()));
    campaignPromotion.setDiscountType(DiscountType.FIXED_AMOUNT);
    campaignPromotion.setTrial(false);
    campaignPromotionDAO.save(campaignPromotion);
    return campaignPromotion;
  }

  @Test
  public void testCreateCampaignsAddDiscountAmount() throws Exception {
    try {
      CampaignPromotion campaignPromotion = createCamp();
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
      CampaignPromotion promotion = controller.createCampaigns(form, result, map, null);
      Assert.assertNull(promotion.getPercentOff());
      Assert.assertNotNull(promotion.getCampaignPromotionDiscountAmount());
      Iterator itr = promotion.getCampaignPromotionDiscountAmount().iterator();
      CampaignPromotionDiscountAmount CPDA = (CampaignPromotionDiscountAmount) itr.next();
      Assert.assertEquals(CPDA.getDiscount(), new BigDecimal(100));
      Assert.assertEquals(CPDA.getCurrencyValue(), currency);

    } catch (Exception e) {
      // TODO Auto-generated catch block
      Assert.fail();
    }

  }*/

}
