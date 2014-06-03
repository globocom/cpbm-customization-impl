/*
 * Copyright © 2013 Citrix Systems, Inc. You may not use, copy, or modify this file except pursuant to a valid license
 * agreement from Citrix Systems, Inc.
 */
package com.citrix.cpbm.portal.fragment.controllers;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.citrix.cpbm.platform.admin.service.ConnectorConfigurationManager;
import com.citrix.cpbm.platform.spi.CloudConnectorFactory.ConnectorType;
import com.google.gson.JsonObject;
import com.vmops.model.Catalog;
import com.vmops.model.Channel;
import com.vmops.model.Channel.ChannelType;
import com.vmops.model.ChannelSettingServiceConfigMetadata;
import com.vmops.model.CurrencyValue;
import com.vmops.model.Product;
import com.vmops.model.ProductBundle;
import com.vmops.model.ProductBundleRevision;
import com.vmops.model.ProductCharge;
import com.vmops.model.ProductRevision;
import com.vmops.model.RateCardCharge;
import com.vmops.model.Revision;
import com.vmops.model.Service;
import com.vmops.model.ServiceInstance;
import com.vmops.model.ServiceInstanceConfig;
import com.vmops.model.SupportedCurrency;
import com.vmops.portal.config.Configuration.Names;
import com.vmops.service.ChannelService;
import com.vmops.service.ConfigurationService;
import com.vmops.service.CurrencyValueService;
import com.vmops.service.ProductBundleService;
import com.vmops.service.ProductService;
import com.vmops.service.exceptions.CurrencyPrecisionException;
import com.vmops.service.exceptions.InvalidAjaxRequestException;
import com.vmops.service.exceptions.NoSuchChannelException;
import com.vmops.service.exceptions.ServiceException;
import com.vmops.utils.DateUtils;
import com.vmops.utils.JSONUtils;
import com.vmops.web.controllers.AbstractAuthenticatedController;
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.forms.ChannelLogoForm;
import com.vmops.web.forms.ChannelServiceSetting;
import com.vmops.web.forms.ChannelServiceSettingsForm;
import com.vmops.web.forms.ServiceList;
import com.vmops.web.validators.ChannelLogoFormValidator;

public class AbstractChannelController extends AbstractAuthenticatedController {

  @Autowired
  ChannelService channelService;

  @Autowired
  CurrencyValueService currencyValueService;

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductBundleService productBundleService;

  @Autowired
  private ConfigurationService configurationService;

  @Autowired
  protected ConnectorConfigurationManager connectorConfigurationManager;

  private final int BUNDLES_PER_PAGE = 5;

  class ProductSortOrderSort implements Comparator<Product> {

    @Override
    public int compare(Product prod1, Product prod2) {
      if (prod1.getSortOrder() > prod2.getSortOrder()) {
        return 1;
      } else if (prod1.getSortOrder() == prod2.getSortOrder()) {
        return Integer.valueOf(prod1.getId().toString()) - Integer.valueOf(prod2.getId().toString());
      } else {
        return -1;
      }
    }
  }

  class CurrencyValueSort implements Comparator<CurrencyValue> {

    @Override
    public int compare(CurrencyValue currVal1, CurrencyValue currVal2) {
      if (currVal1.getRank() == currVal2.getRank()) {
        return 0;
      } else if (currVal1.getRank() < currVal2.getRank()) {
        return 1;
      } else {
        return -1;
      }
    }
  }

  private List<ProductBundleRevision> getProductBundles(List<ProductBundleRevision> productBundleRevisions,
      String currentPage, String perPageCount) {
    int pageNo;
    int perPage;

    try {
      pageNo = Integer.parseInt(currentPage);
    } catch (NumberFormatException nFE) {
      pageNo = 1;
    }

    try {
      perPage = Integer.parseInt(perPageCount);
      if (perPage > BUNDLES_PER_PAGE) {
        perPage = BUNDLES_PER_PAGE;
      }
    } catch (NumberFormatException nFE) {
      perPage = BUNDLES_PER_PAGE;
    }

    List<ProductBundleRevision> productBundleRevisionList = new ArrayList<ProductBundleRevision>();
    int count = 0;
    int toStartFrom = (pageNo - 1) * perPage;
    for (ProductBundleRevision productBundleRevision : productBundleRevisions) {
      ProductBundle productBundle = productBundleRevision.getProductBundle();
      if (productBundle.getRemoved() != null) {
        continue;
      }
      if (count >= toStartFrom) {
        productBundleRevisionList.add(productBundleRevision);
        if (productBundleRevisionList.size() == perPage || count == productBundleRevisionList.size()) {
          break;
        }
      }
      count += 1;
    }

    logger.debug("Leaving listProductBundlesByCatalog");
    return productBundleRevisionList;
  }

  private Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> getProductChargeMap(Channel channel,
      String timeline, Date date, boolean forUpdate) {

    // Structure is of the form::
    //
    // "Product":{ "CurrencyVal(forUSD)":{ "catalog": ProductCharge, "rpb": ProductCharge },
    // "CurrencyVal(forINR)": ...

    Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> fullProductPricingMap = new TreeMap<Product, Map<CurrencyValue, Map<String, ProductCharge>>>(
        new ProductSortOrderSort());

    List<ProductRevision> catalogProductRevisions = new ArrayList<ProductRevision>();
    Map<Product, ProductRevision> rpbProductRevisionMap = new HashMap<Product, ProductRevision>();
    if (timeline.equals("current")) {
      catalogProductRevisions = channelService.getChannelRevision(channel,
          channelService.getCurrentRevision(channel).getStartDate(), false).getProductRevisions();

      rpbProductRevisionMap = channelService.getChannelRevision(null,
          channelService.getCurrentRevision(channel).getStartDate(), false).getProductRevisionsMap();

    } else if (timeline.equals("planned")) {
      catalogProductRevisions = channelService.getChannelRevision(channel,
          channelService.getFutureRevision(channel).getStartDate(), false).getProductRevisions();

      rpbProductRevisionMap = channelService.getChannelRevision(null,
          channelService.getFutureRevision(channel).getStartDate(), false).getProductRevisionsMap();

    } else if (timeline.equals("history")) {
      catalogProductRevisions = channelService.getChannelRevision(channel, date, false).getProductRevisions();

      rpbProductRevisionMap = channelService.getChannelRevision(null, date, false).getProductRevisionsMap();
    }

    for (ProductRevision productRevision : catalogProductRevisions) {
      ProductRevision rpbProductRevision = rpbProductRevisionMap.get(productRevision.getProduct());
      Map<CurrencyValue, Map<String, ProductCharge>> currencyProductPriceMap = new TreeMap<CurrencyValue, Map<String, ProductCharge>>(
          new CurrencyValueSort());
      for (ProductCharge productCharge : productRevision.getProductCharges()) {
        if (currencyProductPriceMap.get(productCharge.getCurrencyValue()) == null) {
          currencyProductPriceMap.put(productCharge.getCurrencyValue(), new LinkedHashMap<String, ProductCharge>());
        }
        if (forUpdate) {
          try {
            productCharge.setPrice(productCharge.getPrice().setScale(
                Integer.parseInt(config.getValue(Names.com_citrix_cpbm_portal_appearance_currency_precision)),
                BigDecimal.ROUND_UNNECESSARY));
          } catch (ArithmeticException aex) {
            logger.error("ArithmeticException while editing the product charge, Possible Cause- "
                + "the currency precision level was reduced " + aex);
            throw new CurrencyPrecisionException(aex);
          }
        }
        currencyProductPriceMap.get(productCharge.getCurrencyValue()).put("catalog", productCharge);
        if (rpbProductRevision == null) {
          currencyProductPriceMap.get(productCharge.getCurrencyValue()).put("rpb", null);
        }
      }
      if (rpbProductRevision != null) {
        for (ProductCharge productCharge : rpbProductRevision.getProductCharges()) {
          if (currencyProductPriceMap.get(productCharge.getCurrencyValue()) != null) {
            if (forUpdate) {
              try {
                productCharge.setPrice(productCharge.getPrice().setScale(
                    Integer.parseInt(config.getValue(Names.com_citrix_cpbm_portal_appearance_currency_precision)),
                    BigDecimal.ROUND_UNNECESSARY));
              } catch (ArithmeticException aex) {
                logger.error("ArithmeticException while editing the product charge, Possible Cause- "
                    + "the currency precision level was reduced " + aex);
                throw new CurrencyPrecisionException(aex);
              }
            }
            currencyProductPriceMap.get(productCharge.getCurrencyValue()).put("rpb", productCharge);
          }
        }
      }
      fullProductPricingMap.put(productRevision.getProduct(), currencyProductPriceMap);
    }

    return fullProductPricingMap;
  }

  private Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> getBundlePricingMap(
      Channel channel, String timeline, Date date, boolean forUpdate) {
    // Structure is of the form::
    //
    // "ProductBundleRevision":{ "CurrencyVal(forUSD)":{ "catalog-onetime": RateCardCharge,
    // "rpb-onetime":RateCardCharge,
    // "catalog-recurring": RateCardCharge,
    // "rpb-recurring": RateCardCharge},
    // "CurrencyVal(forINR)": ...
    // ....

    Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = new LinkedHashMap<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>>();

    List<ProductBundleRevision> catalogProductBundleRevisions = new ArrayList<ProductBundleRevision>();
    Map<ProductBundle, ProductBundleRevision> rpbProductBundleRevisionMap = new HashMap<ProductBundle, ProductBundleRevision>();
    if (timeline.equals("current")) {
      catalogProductBundleRevisions = channelService.getChannelRevision(channel,
          channelService.getCurrentRevision(channel).getStartDate(), false).getProductBundleRevisions();

      rpbProductBundleRevisionMap = channelService.getChannelRevision(null,
          channelService.getCurrentRevision(channel).getStartDate(), false).getProductBundleRevisionsMap();

    } else if (timeline.equals("planned")) {
      catalogProductBundleRevisions = channelService.getChannelRevision(channel,
          channelService.getFutureRevision(channel).getStartDate(), false).getProductBundleRevisions();

      rpbProductBundleRevisionMap = channelService.getChannelRevision(null,
          channelService.getFutureRevision(channel).getStartDate(), false).getProductBundleRevisionsMap();
    } else if (timeline.equals("history")) {
      catalogProductBundleRevisions = channelService.getChannelRevision(channel, date, false)
          .getProductBundleRevisions();

      rpbProductBundleRevisionMap = channelService.getChannelRevision(null, date, false).getProductBundleRevisionsMap();
    }

    for (ProductBundleRevision catalaogProductBundleRevision : catalogProductBundleRevisions) {

      Map<CurrencyValue, Map<String, RateCardCharge>> currencyProductBundlePriceMap = new TreeMap<CurrencyValue, Map<String, RateCardCharge>>(
          new CurrencyValueSort());

      ProductBundleRevision rpbProductBundleRevision = rpbProductBundleRevisionMap.get(catalaogProductBundleRevision
          .getProductBundle());

      for (RateCardCharge rcc : catalaogProductBundleRevision.getRateCardCharges()) {
        if (currencyProductBundlePriceMap.get(rcc.getCurrencyValue()) == null) {
          currencyProductBundlePriceMap.put(rcc.getCurrencyValue(), new LinkedHashMap<String, RateCardCharge>());
        }

        if (forUpdate) {
          try {
            rcc.setPrice(rcc.getPrice().setScale(
                Integer.parseInt(config.getValue(Names.com_citrix_cpbm_portal_appearance_currency_precision)),
                BigDecimal.ROUND_UNNECESSARY));
          } catch (ArithmeticException aex) {
            logger.error("ArithmeticException while editing the product charge, Possible Cause- "
                + "the currency precision level was reduced " + aex);
            throw new CurrencyPrecisionException(aex);

          }
        }
        if (rcc.getRateCardComponent().isRecurring()) {
          currencyProductBundlePriceMap.get(rcc.getCurrencyValue()).put("catalog-recurring", rcc);
        } else {
          currencyProductBundlePriceMap.get(rcc.getCurrencyValue()).put("catalog-onetime", rcc);
        }
      }
      for (RateCardCharge rcc : rpbProductBundleRevision.getRateCardCharges()) {
        if (currencyProductBundlePriceMap.get(rcc.getCurrencyValue()) != null) {
          if (forUpdate) {
            try {
              rcc.setPrice(rcc.getPrice().setScale(
                  Integer.parseInt(config.getValue(Names.com_citrix_cpbm_portal_appearance_currency_precision)),
                  BigDecimal.ROUND_UNNECESSARY));
            } catch (ArithmeticException aex) {
              logger.error("ArithmeticException while editing the product charge, Possible Cause- "
                  + "the currency precision level was reduced " + aex);
              throw new CurrencyPrecisionException(aex);
            }
          }
          if (rcc.getRateCardComponent().isRecurring()) {
            currencyProductBundlePriceMap.get(rcc.getCurrencyValue()).put("rpb-recurring", rcc);
          } else {
            currencyProductBundlePriceMap.get(rcc.getCurrencyValue()).put("rpb-onetime", rcc);
          }
        }
      }
      fullBundlePricingMap.put(catalaogProductBundleRevision, currencyProductBundlePriceMap);
    }
    return fullBundlePricingMap;
  }

  @RequestMapping(value = "/list", method = RequestMethod.GET)
  public String list(@RequestParam(value = "page", required = false, defaultValue = "1") String currentPage,
      @RequestParam(value = "namePattern", required = false, defaultValue = "") String namePattern, ModelMap map) {
    logger.debug("### list method starting...(GET)");
    int page;
    int perPage;
    try {
      page = Integer.parseInt(currentPage);
    } catch (NumberFormatException nFE) {
      page = 1;
    }
    try {
      perPage = getDefaultPageSize();
      if (perPage > 14) {
        perPage = 14;
      }
    } catch (NumberFormatException nFE) {
      perPage = 14;
    }

    setPage(map, Page.CHANNELS);

    List<Channel> channels = channelService.getChannels(page, perPage, namePattern);
    map.addAttribute("channels", channels);
    map.addAttribute("channelsize", channels.size());

    int totalSize = channelService.count(null);
    if (totalSize - (page * perPage) > 0) {
      map.addAttribute("enable_next", true);
    } else {
      map.addAttribute("enable_next", false);
    }
    map.addAttribute("current_page", page);

    map.addAttribute("channelCreationAllowed", true);
    if (!channelService.isChannelCreationAllowed()) {
      map.addAttribute("channelCreationAllowed", false);
    }
    map.addAttribute("defaultChannel", channelService.getDefaultServiceProviderChannel());
    logger.debug("### list method ending...(GET)");
    return "channels.list";
  }

  /**
   * List all the products
   * 
   * @return
   */
  @RequestMapping(value = "/searchchannel", method = RequestMethod.GET)
  public String searchChannelByPattern(
      @RequestParam(value = "currentPage", required = false, defaultValue = "1") String currentPage,
      @RequestParam(value = "namePattern", required = false) String namePattern, ModelMap map) {
    logger.debug("### searchChannelByPattern method starting...(GET)");

    list(currentPage, namePattern, map);

    logger.debug("### searchChannelByPattern method ending...(GET)");
    return "channels.search";
  }

  @RequestMapping(value = ("/viewchannel"), method = RequestMethod.GET)
  public String viewChannel(@RequestParam(value = "Id", required = true) String ID, ModelMap map) {
    logger.debug("### viewchannel method starting...(GET)");

    Channel channel = channelService.getChannelById(ID);
    map.addAttribute("channel", channel);

    boolean currenciestoadd = true;
    if (currencyValueService.listActiveCurrencies().size() == channel.getCatalog().getSupportedCurrencies().size()) {
      currenciestoadd = false;
    }

    List<CurrencyValue> listSupportedCurrencies = channel.getCatalog().getSupportedCurrencyValuesByOrder();

    map.addAttribute("isHistoryThere", false);
    List<Date> historyDatesForCatalog = productService.getHistoryDates(channel.getCatalog());
    if (historyDatesForCatalog != null && historyDatesForCatalog.size() > 0) {
      map.addAttribute("isHistoryThere", true);
    }

    map.addAttribute("isCurrentThere", false);
    Revision currentRevision = channelService.getCurrentRevision(channel);
    if (currentRevision != null && currentRevision.getStartDate() != null
        && (currentRevision.getStartDate().getTime() <= (new Date()).getTime())) {
      map.addAttribute("isCurrentThere", true);
    }

    map.addAttribute("supportedCurrencies", listSupportedCurrencies);
    map.addAttribute("currenciestoadd", currenciestoadd);
    map.addAttribute("futureRevisionDate", channelService.getFutureRevision(channel).getStartDate());
    map.addAttribute("effectiveDate", channelService.getFutureRevision(channel).getStartDate());
    map.addAttribute("isChannelDeletionAllowed", channelService.isChannelDeletionAllowed(channel));
    map.addAttribute("cloudService", getServiceAndServiceInstanceList().getServices());
    List<Service> services = connectorConfigurationManager.getAllServicesByType(ConnectorType.CLOUD.toString());
    if (CollectionUtils.isNotEmpty(services)) {
      boolean foundInstance = false;
      map.addAttribute("services", true);
      for (Service service : services) {
        if (CollectionUtils.isNotEmpty(service.getServiceInstances())) {
          foundInstance = true;
          break;
        }
      }
      if (foundInstance) {
        map.addAttribute("instances", true);
      } else {
        map.addAttribute("instances", false);
      }
    } else {
      map.addAttribute("services", false);
      map.addAttribute("instances", false);
    }

    logger.debug("### viewchannel method end...(GET)");
    return "channels.view";
  }

  @RequestMapping(value = ("/editchannel"), method = RequestMethod.GET)
  public String editChannel(@RequestParam(value = "Id", required = true) String ID, ModelMap map) {
    logger.debug("### editChannel method starting...(GET)");

    Channel channel = channelService.getChannelById(ID);
    map.addAttribute("channel", channel);
    map.addAttribute("currencies", channel.getCatalog().getSupportedCurrencyValuesByOrder());

    logger.debug("### editChannel method ending...(GET)");
    return "channels.edit";
  }

  @RequestMapping(value = ("/editchannel"), method = RequestMethod.POST)
  public String editChannel(@RequestParam(value = "Id", required = true) String ID,
      @RequestParam(value = "channelName", required = true) String channelName,
      @RequestParam(value = "description", required = true) String description,
      @RequestParam(value = "code", required = true) String code, ModelMap map) {
    logger.debug("### editChannel method starting...(POST)");

    Channel channel = channelService.getChannelById(ID);

    String oldName = channel.getName();

    channel.setName(channelName);
    channel.setDescription(description);
    channel.setCode(code);
    channelService.updateChannel(channel);

    // If the channel name is changed then update that in the
    // com.citrix.cpbm.accountManagement.onboarding.default.channel config also
    // TODO Following code should be moved to service layer update method. Not feasible now as only updated channel
    // object is being passed in update.
    // So no way to get the old name of channel for compare.

    if (!oldName.equals(channelName)) {
      com.vmops.model.Configuration defaultChannelConfiguration = configurationService
          .locateConfigurationByName("com.citrix.cpbm.accountManagement.onboarding.default.channel");
      if (defaultChannelConfiguration.getValue().equals(oldName)) {
        defaultChannelConfiguration.setValue(channelName);
        configurationService.update(defaultChannelConfiguration);
      }
    }

    logger.debug("### editChannel method ending...(POST)");
    return viewChannel(String.valueOf(channel.getId()), map);
  }

  @RequestMapping(value = ("/createchannel"), method = RequestMethod.GET)
  public String createChannel(ModelMap map, HttpServletResponse response) {
    logger.debug("### createChannel method starting...(GET)");
    // Add precondition
    if (!channelService.isChannelCreationAllowed()) {
      response.setStatus(HttpStatus.PRECONDITION_FAILED.value());
      logger.debug("### createChannel(GET) method ending. PreConditions failed.");
      return null;
    }
    map.addAttribute("channels", channelService.getChannels(null, null, null));
    map.addAttribute("currencies", currencyValueService.listActiveCurrencies());

    logger.debug("### createChannel method end...(GET)");
    return "channels.create";
  }

  @RequestMapping(value = ("/createchannel"), method = RequestMethod.POST)
  @ResponseBody
  public Channel createChannel(@RequestParam(value = "channelName", required = true) String channelName,
      @RequestParam(value = "description", required = true) String description,
      @RequestParam(value = "code", required = true) String code,
      @RequestParam(value = "currencyList[]", required = true) String[] currencyValueList, ModelMap map,
      HttpServletResponse response) {
    logger.debug("### createChannel method starting...(POST)");
    // Check if a channel with the name already exists in the db.
    Channel existingChannel = channelService.locateByChannelCode(code);
    if (existingChannel != null) {
      response.setStatus(CODE_NOT_UNIQUE_ERROR_CODE);
      return null;
    }

    Channel channel = new Channel(channelName, ChannelType.valueOf("CHANNEL"));
    channel.setDescription(description);
    channel.setCode(code);

    List<CurrencyValue> currencyValues = new ArrayList<CurrencyValue>();
    for (String currency : currencyValueList) {
      currencyValues.add(currencyValueService.locateBYCurrencyCode(currency));
    }
    channel = channelService.createChannel(channel, currencyValues);
    logger.debug("### createChannel method end...(POST)");
    return channel;
  }

  @RequestMapping(value = "/validate_channelname")
  @ResponseBody
  public String validateChannelName(@RequestParam("channelName") final String channelName) {
    logger.debug("### validateChannelName start and channelname is : " + channelName);

    try {
      channelService.locateChannel(channelName);
    } catch (NoSuchChannelException ex) {
      logger.debug(channelName + ": doesn't exist in the db channel table");
      if (config.getBooleanValue(Names.com_citrix_cpbm_portal_directory_service_enabled)
          && config.getValue(Names.com_citrix_cpbm_directory_mode).equals("push")) {
        try {
          channelService.locateChannelInDirectoryService(channelName);
          return Boolean.FALSE.toString();
        } catch (NoSuchChannelException exc) {
          logger.debug(channelName + ": doesn't exist in the directory server names in channel table");
          return Boolean.TRUE.toString();
        }
      }
      return Boolean.TRUE.toString();
    }

    logger.debug("### validateChannelName method end");
    return Boolean.FALSE.toString();
  }

  @RequestMapping(value = ("/deletechannel"), method = RequestMethod.POST)
  @ResponseBody
  public String deletechannel(@RequestParam(value = "Id", required = true) String channelId, ModelMap map) {
    logger.debug("### deletechannel method starting...(POST)");

    boolean status = false;
    try {
      status = channelService.removeChannelById(channelId);
    } catch (ServiceException ex) {
      throw new InvalidAjaxRequestException(ex.getMessage());
    }

    logger.debug("### deletechannel method end...(POST)");
    return status == true ? "success" : "failure";
  }

  @RequestMapping(value = ("/editchannelcurrency"), method = RequestMethod.GET)
  public String editChannelCurrency(@RequestParam(value = "channelId", required = true) String channelId, ModelMap map) {
    logger.debug("### editcurrency method starting...(GET)");

    Channel channel = channelService.getChannelById(channelId);
    Set<SupportedCurrency> supportedCurrencies = channel.getCatalog().getSupportedCurrencies();
    List<CurrencyValue> availableCurrencies = currencyValueService.listActiveCurrencies();
    if (supportedCurrencies != null) {
      for (SupportedCurrency supportedCurrency : supportedCurrencies) {
        availableCurrencies.remove(supportedCurrency.getCurrency());
      }
    }
    map.addAttribute("availableCurrencies", availableCurrencies);

    logger.debug("### editcurrency method end...(GET)");
    return "channels.currency.edit";
  }

  @RequestMapping(value = ("/editchannelcurrency"), method = RequestMethod.POST)
  @ResponseBody
  public String editChannelCurrency(@RequestParam(value = "channelId", required = true) String channelId,
      @RequestParam(value = "currencyCodeArray", required = true) String currencyCodeArray, ModelMap map)
      throws JSONException {
    logger.debug("### editcurrency method starting...(POST)");

    if (currencyCodeArray.equals("null")) {
      return "success";
    }

    boolean status = false;
    try {
      Channel channel = channelService.getChannelById(channelId);
      JSONArray jsonArray = new JSONArray(currencyCodeArray);
      for (int index = 0; index < jsonArray.length(); index++) {
        String currencyCode = jsonArray.getString(index);
        CurrencyValue currencyVal = currencyValueService.locateBYCurrencyCode(currencyCode);
        if (currencyVal.isActive()) {
          channelService.addCurrencyToChannel(channel, currencyVal);
        }
      }
      status = true;
    } catch (ServiceException ex) {
      throw new InvalidAjaxRequestException(ex.getMessage());
    }

    logger.debug("### editcurrency method end...(POST)");
    return status == true ? "success" : "failure";
  }

  @RequestMapping(value = "/listbundles", method = RequestMethod.GET)
  public String listbundles(@RequestParam(value = "channelId", required = true) String channelId, ModelMap map) {
    logger.debug("### listbundles method starting...(GET)");

    Channel channel = channelService.getChannelById(channelId);
    Revision futureRevision = channelService.getFutureRevision(channel);
    List<ProductBundleRevision> productBundleRevisions = channelService.getFutureChannelRevision(channel, false)
        .getProductBundleRevisions();
    List<ProductBundle> productBundlesInChannel = new ArrayList<ProductBundle>();
    for (ProductBundleRevision productBundleRevision : productBundleRevisions) {
      productBundlesInChannel.add(productBundleRevision.getProductBundle());
    }
    List<ProductBundleRevision> globalProductBundleRevisions = channelService.getChannelRevision(
        null,
        channelService.getChannelReferenceCatalogRevision(channel, futureRevision).getReferenceCatalogRevision()
            .getStartDate(), false).getProductBundleRevisions();

    List<ProductBundleRevision> bundlesToBeSent = new ArrayList<ProductBundleRevision>();
    for (ProductBundleRevision productBundleRevision : globalProductBundleRevisions) {
      if (productBundleRevision.getProductBundle().getPublish()
          && !productBundlesInChannel.contains(productBundleRevision.getProductBundle())) {
        bundlesToBeSent.add(productBundleRevision);
      }
    }

    map.addAttribute("productBundles", bundlesToBeSent);
    logger.debug("### listbundles method ending...(GET)");
    return "productbundle.add";
  }

  @RequestMapping(value = ("/attachproductbundles"), method = RequestMethod.POST)
  @ResponseBody
  public String attachProductBundles(@RequestParam(value = "channelId", required = true) String channelId,
      @RequestParam(value = "selectProductBundles", required = true) String selectedProductBundles, ModelMap map)
      throws JSONException {
    logger.debug("### attachProductBundles method starting...(POST)");

    if (selectedProductBundles.equals("null")) {
      return "success";
    }

    boolean status = false;
    try {
      Channel channel = channelService.getChannelById(channelId);
      JSONArray jsonArray = new JSONArray(selectedProductBundles);
      List<ProductBundle> productBundlesToAttach = new ArrayList<ProductBundle>();
      for (int index = 0; index < jsonArray.length(); index++) {
        String bundleId = jsonArray.getString(index);
        ProductBundle productBundle = productBundleService.getProductBundleById(Long.parseLong(bundleId));
        productBundlesToAttach.add(productBundle);
      }
      productBundleService.addBundlesToChannel(channel, productBundlesToAttach);
      status = true;
    } catch (ServiceException ex) {
      throw new InvalidAjaxRequestException(ex.getMessage());
    }
    logger.debug("### attachProductBundles method end...(POST)");
    return status == true ? "success" : "failure";
  }

  @RequestMapping(value = ("/editcatalogproductpricing"), method = RequestMethod.GET)
  public String editCatalogProductPricing(@RequestParam(value = "channelId", required = true) String channelId,
      ModelMap map) {
    logger.debug("### editCatalogProductPricing method starting...(GET)");

    Channel channel = channelService.getChannelById(channelId);
    Catalog catalog = channel.getCatalog();

    Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> fullProductPricingMap = getProductChargeMap(channel,
        "planned", null, true);

    map.addAttribute("planDate", channelService.getFutureRevision(channel).getStartDate());
    map.addAttribute("supportedCurrencies", catalog.getSupportedCurrencyValuesByOrder());
    map.addAttribute("fullProductPricingMap", fullProductPricingMap);

    logger.debug("### editCatalogProductPricing method end...(GET)");
    return "catalog.edit.channelpricing";
  }

  @RequestMapping(value = ("/editcatalogproductpricing"), method = RequestMethod.POST)
  public String editCatalogProductPricing(@RequestParam(value = "channelId", required = true) String channelId,
      @RequestParam(value = "currencyValData", required = true) String currencyValData, ModelMap map)
      throws JSONException {
    logger.debug("### editCatalogProductPricing method starting...(POST)");

    boolean status = false;

    try {
      Channel channel = channelService.getChannelById(channelId);
      JSONArray jsonArray = new JSONArray(currencyValData);
      List<ProductCharge> productCharges = new ArrayList<ProductCharge>();
      for (int index = 0; index < jsonArray.length(); index++) {
        JSONObject jsonObj = jsonArray.getJSONObject(index);
        BigDecimal previousVal = new BigDecimal(jsonObj.get("previousvalue").toString());
        BigDecimal newVal = new BigDecimal(jsonObj.get("value").toString());
        String currencyCode = jsonObj.get("currencycode").toString();
        String productId = jsonObj.get("productId").toString();
        if (!previousVal.equals(newVal)) {
          Product product = productService.locateProductById(productId);
          CurrencyValue currencyValue = currencyValueService.locateBYCurrencyCode(currencyCode);
          ProductCharge productCharge = new ProductCharge();
          productCharge.setProduct(product);
          productCharge.setCurrencyValue(currencyValue);
          productCharge.setPrice(newVal);
          productCharges.add(productCharge);
        }
      }
      productService.updatePricesForProducts(productCharges, channel);
      status = true;

    } catch (ServiceException ex) {
      throw new InvalidAjaxRequestException(ex.getMessage());
    }

    logger.debug("### editCatalogProductPricing method starting...(POST)");
    return status == true ? "success" : "failure";
  }

  @RequestMapping(value = ("/editcatalogproductbundlepricing"), method = RequestMethod.GET)
  public String editCatalogProductBundlePricing(@RequestParam(value = "channelId", required = true) String channelId,
      @RequestParam(value = "bundleId", required = true) String bundleId, ModelMap map) {
    logger.debug("### editCatalogProductBundlePricing method starting...(GET)");

    Channel channel = channelService.getChannelById(channelId);
    Catalog catalog = channel.getCatalog();

    Revision futureRevision = channelService.getFutureRevision(channel);
    Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = getBundlePricingMap(
        channel, "planned", futureRevision.getStartDate(), true);

    ProductBundle productBundle = productBundleService.locateProductBundleById(bundleId);
    ProductBundleRevision productBundleRevision = channelService.getFutureChannelRevision(channel, false)
        .getProductBundleRevisionsMap().get(productBundle);

    map.addAttribute("supportedCurrencies", catalog.getSupportedCurrencyValuesByOrder());
    map.addAttribute("channel", channel);
    map.addAttribute("productBundleRevision", productBundleRevision);
    map.addAttribute("fullBundlePricingMap", fullBundlePricingMap);

    logger.debug("### editCatalogProductBundlePricing method ending...(GET)");
    return "channels.bundle.price.edit";
  }

  @RequestMapping(value = ("/editcatalogproductbundlepricing"), method = RequestMethod.POST)
  public String editCatalogProductBundlePricing(@RequestParam(value = "channelId", required = true) String channelId,
      @RequestParam(value = "bundleId", required = true) String bundleId,
      @RequestParam(value = "currencyValData", required = true) String currencyValData, ModelMap map)
      throws JSONException {
    logger.debug("### editCatalogProductBundlePricing method starting...(POST)");

    boolean status = false;
    try {
      Channel channel = channelService.getChannelById(channelId);
      ProductBundle productBundle = productBundleService.locateProductBundleById(bundleId);

      JSONArray jsonArray = new JSONArray(currencyValData);

      Revision futureRevision = channelService.getFutureRevision(channel);
      for (int index = 0; index < jsonArray.length(); index++) {
        JSONObject jsonObj = jsonArray.getJSONObject(index);
        BigDecimal previousVal = new BigDecimal(jsonObj.get("previousvalue").toString());
        BigDecimal newVal = new BigDecimal(jsonObj.get("value").toString());
        String currencyCode = jsonObj.get("currencycode").toString();
        String isRecurring = jsonObj.getString("isRecurring").toString();
        boolean isRecurringCharge = false;
        if (isRecurring.equals("1")) {
          isRecurringCharge = true;
        }

        if (!previousVal.equals(newVal)) {
          CurrencyValue currencyValue = currencyValueService.locateBYCurrencyCode(currencyCode);
          productBundleService.updatePriceForBundle(productBundle, channel, currencyValue, newVal, futureRevision,
              isRecurringCharge);
        }
      }

      status = true;
    } catch (ServiceException ex) {
      throw new InvalidAjaxRequestException(ex.getMessage());
    }
    logger.debug("### editCatalogProductBundlePricing method ending...(POST)");
    return status == true ? "success" : "failure";
  }

  @RequestMapping(value = ("/viewcatalogcurrent"), method = RequestMethod.GET)
  public String viewCatalogCurrent(@RequestParam(value = "channelId", required = true) String channelId,
      @RequestParam(value = "page", required = false, defaultValue = "1") String currentPage,
      @RequestParam(value = "perPage", required = false, defaultValue = "5") String perPageCount, ModelMap map) {
    logger.debug("### viewCatalogCurrent method starting...(GET)");

    Channel channel = channelService.getChannelById(channelId);
    Catalog catalog = channel.getCatalog();

    Revision currentRevision = channelService.getCurrentRevision(channel);
    Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> fullProductPricingMap = getProductChargeMap(channel,
        "current", currentRevision.getStartDate(), false);

    Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = getBundlePricingMap(
        channel, "current", null, false);

    List<ProductBundleRevision> productBundleRevisions = getProductBundles(new ArrayList<ProductBundleRevision>(
        fullBundlePricingMap.keySet()), currentPage, perPageCount);

    Date lastSyncDate = null;
    if (channelService.getChannelReferenceCatalogRevision(channel, currentRevision) != null) {
      lastSyncDate = channelService.getChannelReferenceCatalogRevision(channel, currentRevision)
          .getReferenceCatalogRevision().getStartDate();
    }
    map.addAttribute("supportedCurrencies", catalog.getSupportedCurrencyValuesByOrder());
    map.addAttribute("fullProductPricingMap", fullProductPricingMap);
    map.addAttribute("noOfProducts", fullProductPricingMap.size());
    map.addAttribute("productBundleRevisions", productBundleRevisions);
    map.addAttribute("fullBundlePricingMap", fullBundlePricingMap);
    map.addAttribute("channel", channel);
    map.addAttribute("effectiveDate", currentRevision.getStartDate());
    map.addAttribute("lastSyncDate", lastSyncDate);
    logger.debug("### viewCatalogCurrent method ending...(GET)");
    return "catalog.current";
  }

  @RequestMapping(value = ("/viewcatalogplanned"), method = RequestMethod.GET)
  public String viewCatalogPlanned(@RequestParam(value = "channelId", required = true) String channelId,
      @RequestParam(value = "page", required = false, defaultValue = "1") String currentPage,
      @RequestParam(value = "perPage", required = false, defaultValue = "5") String perPageCount,
      @RequestParam(value = "editpriceisvalid", required = false, defaultValue = "0") String editpriceisvalid,
      ModelMap map) {
    logger.debug("### viewCatalogPlanned method starting...(GET)");
    Channel channel = channelService.getChannelById(channelId);
    Catalog catalog = channel.getCatalog();

    Revision futureRevision = channelService.getFutureRevision(channel);
    Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> fullProductPricingMap = getProductChargeMap(channel,
        "planned", futureRevision.getStartDate(), false);

    Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = getBundlePricingMap(
        channel, "planned", null, false);

    List<ProductBundleRevision> productBundleRevisions = getProductBundles(new ArrayList<ProductBundleRevision>(
        fullBundlePricingMap.keySet()), currentPage, perPageCount);

    int sizeOfPublishedProductBundles = 0;
    for (ProductBundleRevision productBundleRevision : channelService.getChannelRevision(
        null,
        channelService.getChannelReferenceCatalogRevision(channel, futureRevision).getReferenceCatalogRevision()
            .getStartDate(), false).getProductBundleRevisions()) {
      if (productBundleRevision.getProductBundle().getPublish()) {
        ++sizeOfPublishedProductBundles;
      }
    }

    int sizeOfPublishedBundlesAddedToCatalog = 0;
    for (ProductBundleRevision productBundleRevision : fullBundlePricingMap.keySet()) {
      if (productBundleRevision.getProductBundle().getPublish()) {
        sizeOfPublishedBundlesAddedToCatalog += 1;
      }
    }

    boolean bundlestoadd = true;
    if (sizeOfPublishedBundlesAddedToCatalog == sizeOfPublishedProductBundles) {
      bundlestoadd = false;
    }

    List<CurrencyValue> listSupportedCurrencies = catalog.getSupportedCurrencyValuesByOrder();
    Date lastSyncDate = null;
    if (channelService.getChannelReferenceCatalogRevision(channel, futureRevision) != null) {
      lastSyncDate = channelService.getChannelReferenceCatalogRevision(channel, futureRevision)
          .getReferenceCatalogRevision().getStartDate();
    }
    map.addAttribute("supportedCurrencies", listSupportedCurrencies);
    map.addAttribute("channel", channel);
    map.addAttribute("fullProductPricingMap", fullProductPricingMap);
    map.addAttribute("noOfProducts", fullProductPricingMap.size());
    map.addAttribute("productBundleRevisions", productBundleRevisions);
    map.addAttribute("bundlestoadd", bundlestoadd);
    map.addAttribute("fullBundlePricingMap", fullBundlePricingMap);
    map.addAttribute("toalloweditprices", true);
    map.addAttribute("effectiveDate", futureRevision.getStartDate());
    map.addAttribute("lastSyncDate", lastSyncDate);
    logger.debug("### viewCatalogPlanned method ending...(GET)");
    return "catalog.planned";
  }

  @RequestMapping(value = ("/viewcataloghistory"), method = RequestMethod.GET)
  public String viewCatalogHistory(@RequestParam(value = "channelId", required = true) String channelId,
      @RequestParam(value = "historyDate", required = false, defaultValue = "") String historyDate,
      @RequestParam(value = "dateFormat", required = false) String dateFormat,
      @RequestParam(value = "showProductHistory", required = false) String showProductHistory, ModelMap map) {
    logger.debug("### viewCatalogHistory method starting...(GET)");

    Channel channel = channelService.getChannelById(channelId);
    Catalog catalog = channel.getCatalog();

    List<Date> historyDatesForCatalog = productService.getHistoryDates(catalog);

    map.addAttribute("noHistory", false);
    map.addAttribute("supportedCurrencies", catalog.getSupportedCurrencyValuesByOrder());
    map.addAttribute("catalogHistoryDates", historyDatesForCatalog);

    if (historyDatesForCatalog == null || historyDatesForCatalog.size() == 0) {
      map.addAttribute("noHistory", true);
    } else {
      Date historyDateObj = null;
      if (historyDate != null && !historyDate.isEmpty()) {
        DateFormat formatter = new SimpleDateFormat(dateFormat);
        try {
          historyDateObj = formatter.parse(historyDate);
        } catch (ParseException e) {
          throw new InvalidAjaxRequestException(e.getMessage());
        }
      } else {
        historyDateObj = historyDatesForCatalog.get(0);
      }

      Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> fullProductPricingMap = getProductChargeMap(channel,
          "history", historyDateObj, false);

      Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = getBundlePricingMap(
          channel, "history", historyDateObj, false);

      map.addAttribute("noOfProducts", fullProductPricingMap.size());
      map.addAttribute("fullProductPricingMap", fullProductPricingMap);
      map.addAttribute("productBundleRevisions", fullBundlePricingMap.keySet());
      map.addAttribute("fullBundlePricingMap", fullBundlePricingMap);
      map.addAttribute("chosenHistoryDate", historyDateObj);
    }
    if (showProductHistory != null) {
      map.addAttribute("showProductHistory", true);
    } else {
      map.addAttribute("showProductHistory", false);
    }
    logger.debug("### viewCatalogHistory method ending...(GET)");
    return "catalog.history";
  }

  @RequestMapping(value = ("/changeplandate"), method = RequestMethod.POST)
  @ResponseBody
  public String changePlanDate(@RequestParam(value = "channelId", required = true) String channelId,
      @RequestParam(value = "newDate", required = true) String newDate,
      @RequestParam(value = "dateFormat", required = true) String dateFormat, ModelMap map) throws ParseException {
    logger.debug("### changePlanDate method starting...(POST)");
    boolean status = false;
    try {
      Channel channel = channelService.getChannelById(channelId);
      DateFormat formatter = new SimpleDateFormat(dateFormat);
      Date planDate = formatter.parse(newDate);
      Revision revision = channelService.setRevisionDate(planDate, channel);
      if (revision != null) {
        status = true;
      }
    } catch (ServiceException ex) {
      throw new InvalidAjaxRequestException(ex.getMessage());
    }

    logger.debug("### changePlanDate method ending...(POST)");
    return status == true ? "success" : "failure";
  }

  /**
   * Pops up the datepicker
   * 
   * @return
   */
  @RequestMapping(value = "/showdatepicker", method = RequestMethod.GET)
  public String showDatePicker(@RequestParam(value = "channelId", required = true) String channelId, ModelMap map) {
    logger.debug("### showDatePicker method starting...(GET)");

    Channel channel = channelService.getChannelById(channelId);

    Revision futureRevision = channelService.getFutureRevision(channel);
    map.addAttribute("plan_date", null);
    map.addAttribute("planDateInFuture", false);
    if (futureRevision != null && futureRevision.getStartDate() != null
        && futureRevision.getStartDate().after(new Date())) {
      map.addAttribute("planDateInFuture", true);
      map.addAttribute("plan_date", futureRevision.getStartDate());
    }
    map.addAttribute("date_tomorrow", DateUtils.addOneDay(new Date()));
    map.addAttribute("date_today", new Date());

    map.addAttribute("channel", channel);
    // always allow today
    map.addAttribute("isTodayAllowed", true);

    logger.debug("### showDatePicker method ending...(GET)");
    return "channels.datepicker";
  }

  /**
   * Pops up the logo set page
   * 
   * @param channelId
   * @param map
   * @return
   */
  @RequestMapping(value = ("/editlogo"), method = RequestMethod.GET)
  public String editChannelLogo(@RequestParam(value = "channelId", required = true) String channelId, ModelMap map) {
    logger.debug("### editchannellogo method starting...(GET)");

    ChannelLogoForm channelLogoForm = new ChannelLogoForm(channelService.getChannelById(channelId));

    map.addAttribute("channelLogoForm", channelLogoForm);
    setPage(map, Page.CHANNELS);

    logger.debug("### editchannellogo method end...(GET)");
    return "channel.editlogo";
  }

  @RequestMapping(value = ("/editlogo"), method = RequestMethod.POST)
  @ResponseBody
  public String editChannelLogo(@ModelAttribute("channelLogoForm") ChannelLogoForm form, BindingResult result,
      HttpServletRequest request, ModelMap map) {
    logger.debug("### editChannelLogo method starting...(POST)");
    String fileSize = checkFileUploadMaxSizeException(request);
    if (fileSize != null) {
      result.rejectValue("logo", "error.image.max.upload.size.exceeded");
      JsonObject error = new JsonObject();
      error.addProperty("errormessage", messageSource.getMessage(result.getFieldError("logo").getCode(), new Object[] {
        fileSize
      }, request.getLocale()));
      return error.toString();
    }
    String rootImageDir = config.getValue(Names.com_citrix_cpbm_portal_settings_images_uploadPath);
    if (rootImageDir != null && !rootImageDir.trim().equals("")) {
      Channel channel = channelService.getChannelById(form.getChannel().getId().toString());
      ChannelLogoFormValidator validator = new ChannelLogoFormValidator();
      validator.validate(form, result);
      if (result.hasErrors()) {
        JsonObject error = new JsonObject();
        setPage(map, Page.CHANNELS);
        error.addProperty("errormessage",
            messageSource.getMessage(result.getFieldError("logo").getCode(), null, request.getLocale()));
        return error.toString();
      } else {
        String channelsDir = "channels";
        File file = new File(FilenameUtils.concat(rootImageDir, channelsDir));
        if (!file.exists()) {
          file.mkdir();
        }
        String channelsAbsoluteDir = FilenameUtils.concat(rootImageDir, channelsDir);
        String relativeImageDir = FilenameUtils.concat(channelsDir, channel.getId().toString());
        File file1 = new File(FilenameUtils.concat(channelsAbsoluteDir, channel.getId().toString()));
        if (!file1.exists()) {
          file1.mkdir();
        }

        MultipartFile logoFile = form.getLogo();
        try {
          if (!logoFile.getOriginalFilename().trim().equals("")) {
            String logoFileRelativePath = writeMultiPartFileToLocalFile(rootImageDir, relativeImageDir, logoFile);
            channel.setImagePath(logoFileRelativePath);
          }
          channelService.updateChannel(channel);
        } catch (IOException e) {
          logger.debug("###IO Exception in writing custom image file");
        }
      }
      String response = null;
      try {
        response = JSONUtils.toJSONString(channelService.getChannelById(form.getChannel().getId().toString()));
      } catch (JsonGenerationException e) {
        logger.debug("###IO Exception in writing custom image file");
      } catch (JsonMappingException e) {
        logger.debug("###IO Exception in writing custom image file");
      } catch (IOException e) {
        logger.debug("###IO Exception in writing custom image file");
      }
      logger.debug("### editChannelLogo method ending (Success)...(POST)");
      return response;
    } else {
      result.rejectValue("logo", "error.custom.image.upload.dir");
      setPage(map, Page.CHANNELS);
      JsonObject error = new JsonObject();
      error.addProperty("errormessage",
          messageSource.getMessage(result.getFieldError("logo").getCode(), null, request.getLocale()));
      logger.debug("### editChannelLogo method ending (No Image Logo Dir Defined)...(POST)");
      return error.toString();
    }
  }

  @RequestMapping(value = ("/getnextsetofbundles"), method = RequestMethod.GET)
  public String getNextSetOfBundles(@RequestParam(value = "channelId", required = true) String channelId,
      @RequestParam(value = "lastBundleNo", required = true) String lastBundleNo,
      @RequestParam(value = "which", required = true) String which,
      @RequestParam(value = "editpriceisvalid", required = false, defaultValue = "0") String editpriceisvalid,
      ModelMap map) {
    logger.debug("### getNextSetOfBundles method starting...(GET)");

    Channel channel = channelService.getChannelById(channelId);

    int pageNo = -1;
    boolean nothingtosend = false;
    try {
      pageNo = Integer.parseInt(lastBundleNo) / BUNDLES_PER_PAGE;
      if (pageNo <= 0) {
        nothingtosend = true;
      }
    } catch (NumberFormatException nFE) {
      nothingtosend = true;
    }

    // Case 1: Case of say 12 being the last bundle no, means that we already have reached the end, otherwise, we would
    // have got last bundle no as a multiple of bundlesPerPage.
    // Case 2: Now suppose we get 10 as the last bundle no, and it is a multiple of bundlesPerPage and suppose 10 is the
    // total count, then we will get size of the productbundles from the call to get list of the same as 0.

    // Case 1
    if (Integer.parseInt(lastBundleNo) % BUNDLES_PER_PAGE != 0) {
      nothingtosend = true;
    }

    Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = new HashMap<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>>();
    int actiontoshow = 0;
    if (!nothingtosend) {
      if (which.equals("planned")) {
        fullBundlePricingMap = getBundlePricingMap(channel, "planned", null, false);
        actiontoshow = 1;
      } else if (which.equals("current")) {
        fullBundlePricingMap = getBundlePricingMap(channel, "current", null, false);
      }
    }

    boolean toalloweditprices = false;
    if (which.equals("planned")) {
      toalloweditprices = true;
    }

    List<ProductBundleRevision> productBundleRevisions = getProductBundles(new ArrayList<ProductBundleRevision>(
        fullBundlePricingMap.keySet()), new Integer(pageNo + 1).toString(), new Integer(BUNDLES_PER_PAGE).toString());

    map.addAttribute("supportedCurrencies", channel.getCatalog().getSupportedCurrencyValuesByOrder());
    map.addAttribute("toalloweditprices", toalloweditprices);
    map.addAttribute("productBundleRevisions", productBundleRevisions);
    map.addAttribute("fullBundlePricingMap", fullBundlePricingMap);
    map.addAttribute("actiontoshow", actiontoshow);
    map.addAttribute("channel", channel);
    map.addAttribute("lastBundleNo", Integer.parseInt(lastBundleNo));
    setPage(map, Page.CHANNELS);

    logger.debug("### getNextSetOfBundles method end...(GET)");
    return "channel.bundle.scroll.view";
  }

  @RequestMapping(value = "/getfulllistingofcharges", method = RequestMethod.GET)
  public String getFullChargeListing(@RequestParam(value = "channelId", required = true) String channelId,
      @RequestParam(value = "currentHistoryPlanned", required = true, defaultValue = "") String currentHistoryPlanned,
      @RequestParam(value = "bundleId", required = false, defaultValue = "") String bundleId,
      @RequestParam(value = "dateFormat", required = false, defaultValue = "") String dateFormat,
      @RequestParam(value = "historyDate", required = false, defaultValue = "") String date, ModelMap map) {
    logger.debug("### getfulllistingofcharges method starting...(GET)");

    Channel channel = channelService.getChannelById(channelId);
    Catalog catalog = channel.getCatalog();
    Date historyDate = null;
    if (currentHistoryPlanned.equals("history")) {
      DateFormat formatter = new SimpleDateFormat(dateFormat);
      try {
        historyDate = formatter.parse(date);
      } catch (ParseException e) {
        throw new InvalidAjaxRequestException(e.getMessage());
      }
    }

    if (bundleId == null || bundleId.equals("")) {
      List<Product> productsList = new ArrayList<Product>();
      Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> fullProductPricingMap = null;
      if (currentHistoryPlanned.equals("current")) {
        fullProductPricingMap = getProductChargeMap(channel, "current", null, false);
        productsList = productService.listProducts(null, null, channelService.getCurrentRevision(channel));
      } else if (currentHistoryPlanned.equals("planned")) {
        fullProductPricingMap = getProductChargeMap(channel, "planned", null, false);
        productsList = productService.listProducts(null, null, channelService.getFutureRevision(channel));
      } else if (currentHistoryPlanned.equals("history")) {
        fullProductPricingMap = getProductChargeMap(channel, "history", historyDate, false);
        productsList = productService.listProducts(null, null,
            channelService.getRevisionForTheDateGiven(historyDate, channel));
      }

      map.addAttribute("fullProductPricingMap", fullProductPricingMap);
      map.addAttribute("currencies", catalog.getSupportedCurrencyValuesByOrder());
      map.addAttribute("totalproducts", productsList.size());
      map.addAttribute("noDialog", false);

      logger.debug("### getfulllistingofcharges method end...(GET)");
      return "catalog.utilities";

    } else {

      ProductBundle bundle = productBundleService.getProductBundleById(Long.parseLong(bundleId));
      ProductBundleRevision productBundleRevision = null;
      Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = null;
      if (currentHistoryPlanned.equals("current")) {
        fullBundlePricingMap = getBundlePricingMap(channel, "current", null, false);
        productBundleRevision = channelService.getCurrentChannelRevision(channel, false).getProductBundleRevisionsMap()
            .get(bundle);
      } else if (currentHistoryPlanned.equals("planned")) {
        fullBundlePricingMap = getBundlePricingMap(channel, "planned", null, false);
        productBundleRevision = channelService.getFutureChannelRevision(channel, false).getProductBundleRevisionsMap()
            .get(bundle);
      } else if (currentHistoryPlanned.equals("history")) {
        fullBundlePricingMap = getBundlePricingMap(channel, "history", historyDate, false);
        channelService.getChannelRevision(channel, historyDate, false);
        productBundleRevision = channelService.getChannelRevision(channel, historyDate, false)
            .getProductBundleRevisionsMap().get(bundle);
      }

      map.addAttribute("productBundleRevision", productBundleRevision);
      map.addAttribute("fullBundlePricingMap", fullBundlePricingMap);
      map.addAttribute("productBundle", bundle);
      map.addAttribute("currencies", channel.getCatalog().getSupportedCurrencyValuesByOrder());
      map.addAttribute("noDialog", false);

      logger.debug("### getfulllistingofcharges method end...(GET)");
      return "catalog.bundle";
    }
  }

  @RequestMapping(value = ("/syncchannel"), method = RequestMethod.POST)
  @ResponseBody
  public String syncChannel(@RequestParam(value = "channelId", required = true) String channelId, ModelMap map) {
    logger.debug("### syncChannel method starting...(POST)");

    boolean status = true;
    Channel channel = channelService.getChannelById(channelId);
    try {
      channelService.syncChannel(channel);
    } catch (ServiceException ex) {
      status = false;
      throw new InvalidAjaxRequestException(ex.getMessage());
    }

    logger.debug("### syncChannel method end...(POST)");
    return status == true ? "success" : "failure";
  }

  @RequestMapping(value = ("/servicesettings"), method = RequestMethod.GET)
  public String getChannelSettings(
      @ModelAttribute("viewChannelServiceSettingsForm") ChannelServiceSettingsForm channelServiceSettingsForm,
      @RequestParam(value = "channelId", required = true) String channelId,
      @RequestParam(value = "instanceUUID", required = true) String instanceUUID, ModelMap map) {
    logger.debug("### getChannelSettings method starting...(GET)");
    ServiceInstance serviceInstance = connectorConfigurationManager.getInstanceByUUID(instanceUUID);
    if (serviceInstance != null) {
      Channel channel = channelService.getChannelById(channelId);
      List<ServiceInstanceConfig> serviceInstanceConfigList = channelService.getChannelSettingsList(channel,
          instanceUUID);

      channelServiceSettingsForm.setChannelServiceSettings(getChannelSettings(serviceInstanceConfigList,
          channelServiceSettingsForm, serviceInstance));
      map.addAttribute("viewChannelServiceSettingsForm", channelServiceSettingsForm);
      map.addAttribute("serviceSettingsChanelID", channelId);
      map.addAttribute("serviceSettingsInstanceUUID", instanceUUID);
      if (serviceInstance.getService().getChannelSettingServiceConfigMetadata().isEmpty()) {
        map.addAttribute("serviceSettingsCount", 0);
      } else {
        map.addAttribute("serviceSettingsCount", 1);
      }
    }
    logger.debug("### getChannelSettings method ending...(GET)");
    return "channel.service.settings";
  }

  @RequestMapping(value = ("/editservicesettings"), method = RequestMethod.GET)
  public String editChannelSettings(
      @ModelAttribute("channelServiceSettingsForm") ChannelServiceSettingsForm channelServiceSettingsForm,
      @RequestParam(value = "channelId", required = true) String channelId,
      @RequestParam(value = "instanceUUID", required = true) String instanceUUID, ModelMap map) {
    logger.debug("### editChannelSettings method starting...(GET)");
    ServiceInstance serviceInstance = connectorConfigurationManager.getInstanceByUUID(instanceUUID);
    Channel channel = channelService.getChannelById(channelId);
    List<ServiceInstanceConfig> serviceInstanceConfigList = channelService
        .getChannelSettingsList(channel, instanceUUID);
    channelServiceSettingsForm.setChannelServiceSettings(getChannelSettings(serviceInstanceConfigList,
        channelServiceSettingsForm, serviceInstance));
    channelServiceSettingsForm.setChannelId(channelId);
    channelServiceSettingsForm.setServiceInstanceUUID(instanceUUID);
    map.addAttribute("channelServiceSettingsForm", channelServiceSettingsForm);
    logger.debug("### editChannelSettings method ending...(GET)");
    return "channel.service.edit.settings";
  }

  @RequestMapping(value = ("/editservicesettings"), method = RequestMethod.POST)
  @ResponseBody
  public String saveChannelSettings(
      @ModelAttribute("channelServiceSettingsForm") ChannelServiceSettingsForm channelServiceSettingsForm, ModelMap map) {
    logger.debug("### editChannelSettings method starting...(POST)");

    ServiceInstance serviceInstance = connectorConfigurationManager.getInstanceByUUID(channelServiceSettingsForm
        .getServiceInstanceUUID());
    Channel channel = channelService.getChannelById(channelServiceSettingsForm.getChannelId());
    boolean create = "create".equals(channelServiceSettingsForm.getMode());
    for (ChannelServiceSetting channelServiceSetting : channelServiceSettingsForm.getChannelServiceSettings()) {
      if (create) {
        ServiceInstanceConfig serviceInstanceConfig = new ServiceInstanceConfig();
        ChannelSettingServiceConfigMetadata channelSettingServiceConfigMetadata = getMatchingChannelServiceConfigMetadata(
            channelServiceSetting.getServiceConfigMetaDataId(), serviceInstance.getService()
                .getChannelSettingServiceConfigMetadata());
        serviceInstanceConfig.setServiceConfigMetadata(channelSettingServiceConfigMetadata);
        serviceInstanceConfig.setName(channelSettingServiceConfigMetadata.getName());
        serviceInstanceConfig.setValue(channelServiceSetting.getValue());
        serviceInstanceConfig.setServiceInstanceConfigurer(channel);
        serviceInstanceConfig.setService(serviceInstance.getService());
        serviceInstanceConfig.setServiceInstance(serviceInstance);
        serviceInstance.getServiceInstanceConfig().add(serviceInstanceConfig);
      } else {
        for (ServiceInstanceConfig serviceInstanceConfig : serviceInstance.getServiceInstanceConfig()) {
          if (serviceInstanceConfig.getServiceConfigMetadata().getId()
              .equals(channelServiceSetting.getServiceConfigMetaDataId())
              && serviceInstanceConfig.getServiceInstanceConfigurer().equals(channel)) {
            serviceInstanceConfig.setValue(channelServiceSetting.getValue());
          }
        }
      }
    }
    connectorConfigurationManager.updateServiceInstance(serviceInstance);
    logger.debug("### editChannelSettings method ending...(GET)");
    return "success";
  }

  private ServiceList getServiceAndServiceInstanceList() {
    logger.debug("### editChannelSettings method starting...(POST)");
    ServiceList serviceListObj = new ServiceList();
    List<com.vmops.web.forms.Service> services = new ArrayList<com.vmops.web.forms.Service>();
    List<Service> serviceList = this.getEnabledCloudServices();
    if (CollectionUtils.isNotEmpty(serviceList)) {
      for (Service service : serviceList) {
        com.vmops.web.forms.Service serviceObj = new com.vmops.web.forms.Service();
        List<com.vmops.web.forms.ServiceInstance> serviceInstances = new ArrayList<com.vmops.web.forms.ServiceInstance>();
        serviceObj.setServicename(messageSource.getMessage(service.getServiceName() + ".service.name", null, null));
        for (ServiceInstance serviceInstance : service.getServiceInstances()) {
          com.vmops.web.forms.ServiceInstance serviceInsObj = new com.vmops.web.forms.ServiceInstance();
          serviceInsObj.setInstancename(serviceInstance.getName());
          serviceInsObj.setInstanceuuid(serviceInstance.getUuid());
          serviceInstances.add(serviceInsObj);
        }
        serviceObj.setInstances(serviceInstances);
        services.add(serviceObj);
      }
      serviceListObj.setServices(services);
    }
    logger.debug("### editChannelSettings method ending...(GET)");
    return serviceListObj;
  }

  private ChannelSettingServiceConfigMetadata getMatchingChannelServiceConfigMetadata(Long id,
      Set<ChannelSettingServiceConfigMetadata> channelSettingServiceConfigMetadatas) {
    for (ChannelSettingServiceConfigMetadata channelSettingServiceConfigMetadata : channelSettingServiceConfigMetadatas) {
      if (channelSettingServiceConfigMetadata.getId().equals(id)) {
        return channelSettingServiceConfigMetadata;
      }
    }
    return null;
  }

  private List<ChannelServiceSetting> getChannelSettings(List<ServiceInstanceConfig> serviceInstanceConfigList,
      ChannelServiceSettingsForm channelServiceSettingsForm, ServiceInstance serviceInstance) {
    ChannelServiceSetting channelSetting = null;
    List<ChannelServiceSetting> channelServiceSettings = new ArrayList<ChannelServiceSetting>();
    if (CollectionUtils.isEmpty(serviceInstanceConfigList)) {
      channelServiceSettingsForm.setMode("create");
      Set<ChannelSettingServiceConfigMetadata> serviceConfigMetadatas = serviceInstance.getService()
          .getChannelSettingServiceConfigMetadata();
      for (ChannelSettingServiceConfigMetadata channelSettingServiceConfigMetadata : serviceConfigMetadatas) {
        String value = StringUtils.isNotBlank(channelSettingServiceConfigMetadata.getDefaultVal()) ? channelSettingServiceConfigMetadata
            .getDefaultVal() : null;
        channelSetting = new ChannelServiceSetting(channelSettingServiceConfigMetadata.getName(), value,
            channelSettingServiceConfigMetadata.getId());
        channelSetting.setValidationClass(channelSettingServiceConfigMetadata.getValidations().getClassValidations());
        channelSetting.setServiceName(serviceInstance.getService().getServiceName());
        channelSetting.setPropertyType(channelSettingServiceConfigMetadata.getType());
        channelSetting.setPropertyOrder(channelSettingServiceConfigMetadata.getPropertyOrder());
        channelSetting.setReconfigurable(channelSettingServiceConfigMetadata.getReconfigurable());
        channelServiceSettings.add(channelSetting);
      }
    } else {
      for (ServiceInstanceConfig serviceInstanceConfig : serviceInstanceConfigList) {
        channelSetting = new ChannelServiceSetting(serviceInstanceConfig.getName(), serviceInstanceConfig.getValue(),
            serviceInstanceConfig.getServiceConfigMetadata().getId());
        channelSetting.setValidationClass(serviceInstanceConfig.getServiceConfigMetadata().getValidations()
            .getClassValidations());
        channelSetting.setServiceName(serviceInstance.getService().getServiceName());
        channelSetting.setPropertyType(serviceInstanceConfig.getServiceConfigMetadata().getType());
        channelSetting.setPropertyOrder(serviceInstanceConfig.getServiceConfigMetadata().getPropertyOrder());
        channelSetting.setReconfigurable(((ChannelSettingServiceConfigMetadata) serviceInstanceConfig
            .getServiceConfigMetadata()).getReconfigurable());
        channelServiceSettings.add(channelSetting);
      }
    }
    Collections.sort(channelServiceSettings, new ChannelServiceSettingComparator());
    return channelServiceSettings;
  }

  public class ChannelServiceSettingComparator implements Comparator<ChannelServiceSetting> {

    @Override
    public int compare(ChannelServiceSetting o1, ChannelServiceSetting o2) {
      return (o1.getPropertyOrder() < o2.getPropertyOrder() ? -1 : (o1.getPropertyOrder() == o2.getPropertyOrder() ? 0
          : 1));
    }
  }

  private List<Service> getEnabledCloudServices() {
    List<ServiceInstance> cloudTypeServiceInstances = connectorManagementService.getCloudTypeServiceInstances();
    Set<Service> services = new HashSet<Service>();
    List<Service> cloudServices = new ArrayList<Service>();
    for (ServiceInstance instance : cloudTypeServiceInstances) {
      services.add(instance.getService());
    }
    if (cloudServices.addAll(services)) {
      return cloudServices;
    }
    return null;
  }
}
