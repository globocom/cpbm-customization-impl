/* Copyright (C) 2011 Citrix Systems, Inc. All rights reserved */
package citrix.cpbm.portal.fragment.controllers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


import com.vmops.model.CampaignPromotion;
import com.vmops.model.CampaignPromotionDiscountAmount;
import com.vmops.model.CampaignPromotionsInChannels;
import com.vmops.model.Catalog;
import com.vmops.model.Channel;
import com.vmops.model.CurrencyValue;
import com.vmops.model.PromotionSignup;
import com.vmops.model.PromotionToken;
import com.vmops.model.SupportedCurrency;
import com.vmops.model.CampaignPromotion.DiscountType;
import com.vmops.model.CampaignPromotion.State;
import com.vmops.service.ChannelService;
import com.vmops.service.CurrencyValueService;
import com.vmops.service.ProductBundleService;
import com.vmops.service.PromotionService;
import com.vmops.service.exceptions.AjaxFormValidationException;
import com.vmops.service.exceptions.TokenGenerationException;
import com.vmops.web.controllers.AbstractAuthenticatedController;
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.forms.CampaignPromotionsForm;
import com.vmops.web.forms.TokenRequestForm;
import com.vmops.web.validators.CampaignPromotionsFormValidator;

public abstract class AbstractCampaignPromotionsController extends AbstractAuthenticatedController {

  private static final BigDecimal MAX_PERCENT_OFF = new BigDecimal(100);

  @Autowired
  PromotionService promotionService;

  @Autowired
  ProductBundleService productBundleService;

  @Autowired
  ChannelService channelService;

  @Autowired
  private CurrencyValueService currencyValueService;

  @RequestMapping(value = "/show", method = RequestMethod.GET)
  public String show(@RequestParam(value = "Id", required = true) String promotionId, ModelMap map) {
    logger.debug("### show method starting...(GET)");
    CampaignPromotion campaignPromotion = promotionService.locateCampaignById(promotionId);
    map.addAttribute("campaign", campaignPromotion);
    logger.debug("### show method ending...(GET)");
    return "promotions.view";
  }

  @RequestMapping(value = "/create", method = RequestMethod.GET)
  public String createCampaigns(ModelMap map) {
    logger.debug("### createPromotions method starting...(GET)");
    setPage(map, Page.CAMPAIGNS);
    CampaignPromotion cp = new CampaignPromotion();
    CampaignPromotionsForm campaignPromotionsForm = new CampaignPromotionsForm();
    campaignPromotionsForm.setCampaignPromotion(cp);
    map.addAttribute("campaignPromotionsForm", campaignPromotionsForm);
    map.addAttribute("channels", channelService.getChannels());
    map.addAttribute("PERCENTAGE", DiscountType.PERCENTAGE);
    map.addAttribute("FIXED_AMOUNT", DiscountType.FIXED_AMOUNT);
    List<CurrencyValue> supportedCurrencies = currencyValueService.listActiveCurrencies();
    map.addAttribute("supportedCurrencies", supportedCurrencies);
    logger.debug("### createPromotions method ending...(GET)");
    return "promotions.new";
  }

  @RequestMapping(value = "/getCurrencies", method = RequestMethod.GET)
  @ResponseBody
  public List<CurrencyValue> createCampaigns(@RequestParam(value = "catalogId", required = false) Long catalogId,
      ModelMap map) {
    logger.debug("### createPromotions method starting...(GET)");
    Catalog catalog = productBundleService.getCatalog(catalogId);
    List<CurrencyValue> supportedCurrencies;
    if (catalog != null) {
      supportedCurrencies = catalog.getSupportedCurrencyValuesByOrder();
    } else {
      supportedCurrencies = currencyValueService.listActiveCurrencies();
    }
    map.addAttribute("supportedCurrencies", supportedCurrencies);
    logger.debug("### createPromotions method ending...(GET)");
    return supportedCurrencies;
  }

  // This method will return the union of supported currencies for the list of channels.
  @RequestMapping(value = "/getSupportedCurrencies", method = RequestMethod.GET)
  public String getSupportedCurrencyForChannels(@ModelAttribute("campaignPromotionsForm") CampaignPromotionsForm form,
      ModelMap map) {
    logger.debug("### getSupportedCurrencyForChannels method starting...(GET)");
    Set<CurrencyValue> currValSet = null;
    List<Catalog> lstCatalog = new ArrayList<Catalog>();

    if (form.getChannelIdLst() != null && form.getChannelIdLst().size() > 0) {
      Channel channel = null;
      for (String channelId : form.getChannelIdLst()) {

        channel = channelService.getChannelById(channelId);
        lstCatalog.add(channel.getCatalog());
      }
    } else { // If "All" is selected then it should bring the supported currency of all the channels
      List<Channel> channelLst = channelService.getChannels();
      for (Channel channel : channelLst) {
        lstCatalog.add(channel.getCatalog());
      }
    }

    if (lstCatalog != null && lstCatalog.size() > 0) {
      currValSet = new HashSet<CurrencyValue>();
      for (Catalog catalog : lstCatalog) {
        for (SupportedCurrency suppCurr : catalog.getSupportedCurrencies())
          currValSet.add(suppCurr.getCurrency());
      }
    }
    if (form.getDiscountAmountMap() != null && form.getDiscountAmountMap().size() > 0) {
      form.getDiscountAmountMap().clear();
    }
    map.put("supportedCurrenciesForChannel", currValSet);
    logger.debug("### getSupportedCurrencyForChannels method ending...(GET)");
    return "promotions.new.supportedCurrency";
  }

  @RequestMapping(value = "/create", method = RequestMethod.POST)
  @ResponseBody
  public CampaignPromotion createCampaigns(@ModelAttribute("campaignPromotionsForm") CampaignPromotionsForm form,
      BindingResult result, ModelMap map, HttpServletResponse response) {

    logger.debug("### createPromotions method starting...(POST)");

    CampaignPromotionsFormValidator validator = new CampaignPromotionsFormValidator();
    validator.validate(form, result);
    if (result.hasErrors()) {
      throw new AjaxFormValidationException(result);
    }

    CampaignPromotion promotion = getCampaignPromotionFromForm(form, "create");

    if (promotion.isTrial()) {
      promotion.setDiscountType(DiscountType.PERCENTAGE);
      promotion.setPercentOff(MAX_PERCENT_OFF);
    }
    if (promotion.getDiscountType() == DiscountType.FIXED_AMOUNT) {
      Map<String, BigDecimal> currencyDiscountMap = form.getDiscountAmountMap();

      Set<CampaignPromotionDiscountAmount> campaignPromotionDiscountAmount = new HashSet<CampaignPromotionDiscountAmount>();
      for (Entry<String, BigDecimal> entry : currencyDiscountMap.entrySet()) {
        BigDecimal discount = entry.getValue();
        if (discount.compareTo(BigDecimal.ZERO) >=  0) {
          CurrencyValue currencyValue = currencyValueService.locateBYCurrencyCode(entry.getKey());
          CampaignPromotionDiscountAmount discountAmount = new CampaignPromotionDiscountAmount(promotion, discount,
              currencyValue);
          campaignPromotionDiscountAmount.add(discountAmount);
        }
      }
      promotion.setCampaignPromotionDiscountAmount(campaignPromotionDiscountAmount);
      promotion.setPercentOff(null);
    }

    promotion = promotionService.createCampain(promotion);
    return promotion;
  }

  @RequestMapping(value = "/list", method = RequestMethod.GET)
  public String list(@RequestParam(value = "editedId", required = false) String editedId,
      @RequestParam(value = "page", required = false, defaultValue = "1") Integer page, ModelMap map) {
    logger.debug("### list method starting...");
    Integer perPage = getDefaultPageSize();
    map.addAttribute("pageSize", getDefaultPageSize());

    setPage(map, Page.CAMPAIGNS);
    List<CampaignPromotion> campaignsList = promotionService.listCampaignPromotions(0, 0);

    int totalCampaigns = campaignsList != null && campaignsList.size() > 0 ? campaignsList.size() : 0;

    campaignsList = promotionService.listCampaignPromotions(page, perPage);
    map.addAttribute("campaignsList", campaignsList);
    map.addAttribute("campaignsListSize", campaignsList.size());
    map.addAttribute("idToBeSelected", editedId);

    map.addAttribute("enable_next", "False");

    if (totalCampaigns - (page * perPage) > 0) {
      map.addAttribute("enable_next", "True");
    } else
      map.addAttribute("enable_next", "False");
    map.addAttribute("current_page", page);

    logger.debug("### list method ending...");
    return "promotions.list";
  }

  @RequestMapping(value = "/editcampaign", method = RequestMethod.GET)
  public String edit(@RequestParam(value = "Id", required = true) String ID, ModelMap map) {
    logger.debug("### edit method starting...");
    CampaignPromotion promotion = promotionService.locateCampaignById(ID);
    CampaignPromotionsForm campaignPromotionsForm = new CampaignPromotionsForm(promotion);
    campaignPromotionsForm.setPromoCode(promotion.getPromoCode());

    List<CurrencyValue> supportedCurrencies;

    // Need to change the logicto bring the list of supported currencies for the selected channel

    // Channel channel = promotion.getChannel();
    // Iterator<CampaignPromotionsInChannels> channelIterator = promotion.getCampaignPromotionsInChannels() != null ? :
    // promotion.getCampaignPromotionsInChannels().iterator() : null;
    /*
     * if (channel != null) { supportedCurrencies = channel.getCatalog().getSupportedCurrencyValuesByOrder(); } else {
     */
    supportedCurrencies = currencyValueService.listActiveCurrencies();
    // }

    // map.addAttribute("supportedCurrencies", supportedCurrencies);
    Map<String, BigDecimal> discountAmountMap = new HashMap<String, BigDecimal>();
    if (promotion.getCampaignPromotionDiscountAmount() != null
        && promotion.getCampaignPromotionDiscountAmount().size() > 0) {
      for (CampaignPromotionDiscountAmount discountAmount : promotion.getCampaignPromotionDiscountAmount()) {
        discountAmountMap.put(discountAmount.getCurrencyValue().getCurrencyCode(), discountAmount.getDiscount());
      }
    } else {
      for (CurrencyValue currencyValue : supportedCurrencies) {
        discountAmountMap.put(currencyValue.getCurrencyCode(), new BigDecimal(0));
      }
    }
    campaignPromotionsForm.setDiscountAmountMap(discountAmountMap);
    map.addAttribute("campaignPromotionsForm", campaignPromotionsForm);
    map.addAttribute("channels", channelService.getChannels());
    map.addAttribute("restrictedEdit", promotion.getState() == State.ACTIVE);
    map.addAttribute("PERCENTAGE", DiscountType.PERCENTAGE);
    map.addAttribute("FIXED_AMOUNT", DiscountType.FIXED_AMOUNT);
    logger.debug("### edit method ending...");
    return "promotions.edit";
  }

  @RequestMapping(value = "/editcampaign", method = RequestMethod.POST)
  @ResponseBody
  public CampaignPromotion edit(@ModelAttribute("campaignPromotionsForm") CampaignPromotionsForm form,
      BindingResult result, ModelMap map) {
    logger.debug("### edit method starting...");

    CampaignPromotion promotion = getCampaignPromotionFromEditForm(form, "edit");
    if (promotion.isTrial()) {
      promotion.setDiscountType(DiscountType.PERCENTAGE);
      promotion.setPercentOff(MAX_PERCENT_OFF);
    }
    if (promotion.getCampaignPromotionDiscountAmount() != null
        && promotion.getCampaignPromotionDiscountAmount().size() > 0) {
      promotion.getCampaignPromotionDiscountAmount().clear();
    }
    if (promotion.getDiscountType() == DiscountType.FIXED_AMOUNT) {
      Map<String, BigDecimal> currencyDiscountMap = form.getDiscountAmountMap();
      Set<CampaignPromotionDiscountAmount> campaignPromotionDiscountAmount = new HashSet<CampaignPromotionDiscountAmount>();
      for (Entry<String, BigDecimal> entry : currencyDiscountMap.entrySet()) {
        BigDecimal discount = entry.getValue();
        if (discount.compareTo(BigDecimal.ZERO) >= 0) {
          CurrencyValue currencyValue = currencyValueService.locateBYCurrencyCode(entry.getKey());
          CampaignPromotionDiscountAmount discountAmount = new CampaignPromotionDiscountAmount(promotion, discount,
              currencyValue);
          campaignPromotionDiscountAmount.add(discountAmount);
        }
      }
      promotion.setCampaignPromotionDiscountAmount(campaignPromotionDiscountAmount);
      promotion.setPercentOff(null);
    }
    promotion = promotionService.mergeCampaignPromotion(promotion);
    long editedId = promotion.getId();
    logger.debug("### edit method ending...");
    map.clear();
    String title = promotion.getTitle();
    return promotion;
    // return "redirect:/portal/promotions/list?editedId=" + editedId;

  }

  @RequestMapping(value = "/validate_campaign")
  @ResponseBody
  public String validateCampaignCode(@RequestParam("campaignPromotion.code") final String campaignCode) {
    logger.debug("In validateCampaignCode() method start and campaignCode is : " + campaignCode);
    try {
      promotionService.locateCampaignByCode(campaignCode);
    } catch (Exception e) {
      logger.debug(campaignCode + ": not exits.");
      return Boolean.TRUE.toString();
    }
    return Boolean.FALSE.toString();
  }

  @RequestMapping("/validate_promoCode")
  @ResponseBody
  public String validateCampaignPromoCode(@RequestParam("promoCode") final String promoCode) {
    logger.debug("In validateCampaignPromoCode() method start and campaignCode is : " + promoCode);

    if (!productService.isCodeUnique(promoCode)) {
      logger.debug("###  promo code is NOT unique ");
      return Boolean.FALSE.toString();
    }
    return Boolean.TRUE.toString();
  }

  private CampaignPromotion getCampaignPromotionFromEditForm(CampaignPromotionsForm form, String action) {
    CampaignPromotion cp = form.getCampaignPromotion();

    if (form.isUnlimited()) {
      cp.setMaxAccounts(0);
    }
    Set<CampaignPromotionsInChannels> campPromoChanelSet = new HashSet<CampaignPromotionsInChannels>();
    CampaignPromotionsInChannels objCampPromo = null;
    if (cp.getSupportedChannels() != null && cp.getSupportedChannels().size() > 0) {
      for (Channel channel : cp.getSupportedChannels()) {
        String channelId = channel.getParam();
        objCampPromo = new CampaignPromotionsInChannels(cp, channelService.getChannel(channelId));
        campPromoChanelSet.add(objCampPromo);
      }
    } else {
      List<Channel> channelLst = channelService.getChannels();
      if (channelLst != null && channelLst.size() > 0) {

        campPromoChanelSet = new HashSet<CampaignPromotionsInChannels>();
        for (Channel channel : channelLst) {

          objCampPromo = new CampaignPromotionsInChannels(cp, channel);
          campPromoChanelSet.add(objCampPromo);
        }
      }
    }
    cp.setCampaignPromotionsInChannels(campPromoChanelSet);
    cp.setCreateBy(getCurrentUser());
    cp.setPromoCode(form.getPromoCode());
    return cp;
  }

  private CampaignPromotion getCampaignPromotionFromForm(CampaignPromotionsForm form, String action) {
    CampaignPromotion cp = form.getCampaignPromotion();
    if (form.isUnlimited()) {
      cp.setMaxAccounts(0);
    }
    Set<CampaignPromotionsInChannels> campPromoChanelSet = new HashSet<CampaignPromotionsInChannels>();
    CampaignPromotionsInChannels objCampPromo = null;
    if (form.getChannelIdLst() != null && form.getChannelIdLst().size() > 0) {
      for (String channelId : form.getChannelIdLst()) {

        objCampPromo = new CampaignPromotionsInChannels(cp, channelService.getChannelById(channelId));
        campPromoChanelSet.add(objCampPromo);
      }
    } else {
      List<Channel> channelLst = channelService.getChannels();
      if (channelLst != null && channelLst.size() > 0) {

        campPromoChanelSet = new HashSet<CampaignPromotionsInChannels>();
        for (Channel channel : channelLst) {

          objCampPromo = new CampaignPromotionsInChannels(cp, channel);
          campPromoChanelSet.add(objCampPromo);
        }
      }
    }
    cp.setCampaignPromotionsInChannels(campPromoChanelSet);
    cp.setCreateBy(getCurrentUser());
    cp.setPromoCode(form.getPromoCode());
    return cp;
  }

  @RequestMapping(value = "/createToken", method = RequestMethod.GET)
  public String createTrialToken(ModelMap map) {
    logger.debug("### createTrialToken method starting...(GET)");
    setPage(map, Page.TRIAL_TOKEN);
    TokenRequestForm tokenRequestForm = new TokenRequestForm();
    map.addAttribute("tokenRequest", tokenRequestForm);
    List<CampaignPromotion> campaignPromotions = promotionService.listCampaignPromotions(0, 0);
    map.addAttribute("campaignPromotions", campaignPromotions);
    logger.debug("### createTrialToken method ending...(GET)");
    return "campaign.token.create";
  }

  @RequestMapping(value = "/createToken", method = RequestMethod.POST)
  public String createTrialToken(@ModelAttribute("tokenRequestForm") TokenRequestForm form, BindingResult result,
      ModelMap map) {
    logger.debug("### createTrialToken method starting...(POST)");
    setPage(map, Page.TRIAL_TOKEN);
    if (result.hasErrors()) {
      logger.error("### requestToken Binding result had some errors");
      displayErrors(result);
      return "campaign.token.create";
    }
    List<CampaignPromotion> campaignPromotions = promotionService.listCampaignPromotions(0, 0);
    map.addAttribute("campaignPromotions", campaignPromotions);
    PromotionToken token = null;
    try {
      token = promotionService.generatePromotionalToken(form.getCampaignCode(), form.getPromotionSignup());
    } catch (TokenGenerationException e) {
      logger.error(e);
      map.addAttribute("emailValidationFailed", e.getMessage());
      map.addAttribute("tokenRequest", form);
      return "campaign.token.create";
    }
    if (token != null) {
      map.addAttribute("isTokenAvailable", "Y");
      PromotionSignup signup = promotionService.locatePromotionSignUpByCode(token.getCode());
      map.addAttribute("currency", signup.getCurrency());
      // CampaignPromotion promotion = promotionService.locatePromotionByToken(token.getCode());
      // if (promotion.isMoneyOff()) {
      // //map.addAttribute("amount", promotion.getCampaignAmountCurrency(signup.getCurrency()));
      // }
      // if (promotion.isIndefinite()) {
      // map.addAttribute("isIndefinite", "Y");
      // }
      // map.addAttribute("trialPeriodDays", promotion.getDurationDays());
    } else {
      map.addAttribute("isTokenAvailable", "N");
    }
    map.addAttribute("tokenRequest", form);
    logger.debug("### createTrialToken method end...(POST)");
    return "campaign.token.create";
  }
}
