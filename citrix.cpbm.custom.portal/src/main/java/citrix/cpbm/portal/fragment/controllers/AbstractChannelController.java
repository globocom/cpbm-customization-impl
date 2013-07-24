package citrix.cpbm.portal.fragment.controllers;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
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

import com.vmops.model.Catalog;
import com.vmops.model.CatalogProductBundle;
import com.vmops.model.Channel;
import com.vmops.model.Channel.ChannelType;
import com.vmops.model.ChannelRevision;
import com.vmops.model.CurrencyValue;
import com.vmops.model.Product;
import com.vmops.model.ProductBundle;
import com.vmops.model.ProductBundleRevision;
import com.vmops.model.ProductCharge;
import com.vmops.model.ProductRevision;
import com.vmops.model.RateCardCharge;
import com.vmops.model.Revision;
import com.vmops.model.SupportedCurrency;
import com.vmops.portal.config.Configuration.Names;
import com.vmops.service.ChannelService;
import com.vmops.service.CurrencyValueService;
import com.vmops.service.ProductBundleService;
import com.vmops.service.ProductService;
import com.vmops.service.exceptions.InvalidAjaxRequestException;
import com.vmops.service.exceptions.NoSuchChannelException;
import com.vmops.service.exceptions.ServiceException;
import com.vmops.utils.DateUtils;
import com.vmops.utils.JSONUtils;
import com.vmops.web.controllers.AbstractAuthenticatedController;
import com.vmops.web.controllers.menu.Page;
import com.vmops.web.forms.ChannelLogoForm;
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

  private String KEY_SEPARATOR = "::";

  private int BUNDLES_PER_PAGE = 5;

  class ProductSortOrderSort implements Comparator<Product> {

    @Override
    public int compare(Product prod1, Product prod2) {
      if ((Long) prod1.getSortOrder() > (Long) prod2.getSortOrder()) {
        return 1;
      } else if ((Long) prod1.getSortOrder() == (Long) prod2.getSortOrder()) {
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
      } else if (currVal1.getRank() < currVal2.getRank())
        return 1;
      else {
        return -1;
      }
    }
  }

  private void copyBundleCharges(Catalog catalog, ProductBundleRevision productBundleRevision,
      List<CurrencyValue> cvList) {
    List<RateCardCharge> rateCardCharges = productBundleRevision.getRateCardCharges();
    Revision revision = channelService.getFutureRevision(catalog.getChannel());
    for (RateCardCharge rcc : rateCardCharges) {
      RateCardCharge newRateCardCharge = new RateCardCharge();
      newRateCardCharge.setCatalog(catalog);
      newRateCardCharge.setRateCardComponent(rcc.getRateCardComponent());
      newRateCardCharge.setCurrencyValue(rcc.getCurrencyValue());
      newRateCardCharge.setPrice(rcc.getPrice());
      newRateCardCharge.setRevision(revision);
      newRateCardCharge.setUpdatedAt(new Date());
      productBundleService.saveRateCardCharge(newRateCardCharge);
    }
  }

  private void copyAllProductCharges(Channel channel, List<CurrencyValue> cvList) {
    Revision refPriceBookCurrentRevision = channelService.getCurrentRevision(null);
    Revision channelFutureRevision = channelService.getFutureRevision(channel);
    List<ProductRevision> rpbProductRevisions = channelService.getChannelRevision(null,
        refPriceBookCurrentRevision.getStartDate(), false).getProductRevisions();

    for (ProductRevision rpbProdcutRevision : rpbProductRevisions) {
      // Ignore only those products that were removed in a revision prior to the current rpb revision
      if (rpbProdcutRevision.getProduct().getRemoved() != null
          && refPriceBookCurrentRevision.getId() >= rpbProdcutRevision.getProduct().getRemovedInRevision().getId()) {
        continue;
      }
      for (ProductCharge productCharge : rpbProdcutRevision.getProductCharges()) {
        if (cvList.contains(productCharge.getCurrencyValue())) {
          ProductCharge newCatalogProductCharge = new ProductCharge();
          newCatalogProductCharge.setCatalog(channel.getCatalog());
          newCatalogProductCharge.setProduct(productCharge.getProduct());
          newCatalogProductCharge.setCurrencyValue(productCharge.getCurrencyValue());
          newCatalogProductCharge.setPrice(productCharge.getPrice());
          newCatalogProductCharge.setUpdatedAt(new Date());
          newCatalogProductCharge.setRevision(channelFutureRevision);
          productService.saveProductCharge(newCatalogProductCharge);
        }
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

  private void planDateChangeBeforeActivation(Catalog catalog, Date newPlanDate) {
    Date planDate = getPlanDate(newPlanDate);
    Revision revision = channelService.getFutureRevision(catalog.getChannel());

    if (revision.getStartDate() == null) {
      Revision newRevision = new Revision(catalog, null, 1L, new Date(), catalog.getCreatedBy());
      channelService.createRevision(newRevision);
    }

    revision.setStartDate(planDate);
    channelService.updateRevision(revision);

    catalog.setPlanDate(planDate);
    channelService.updateCatalog(catalog);
  }

  private Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> getProductChargeMap(Channel channel,
      String timeline, Date date) {

    // Structure is of the form::
    //
    // "Product":{ "CurrencyVal(forUSD)":{ "catalog": ProductCharge, "rpb": ProductCharge },
    // "CurrencyVal(forINR)": ...

    Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> fullProductPricingMap = new TreeMap<Product, Map<CurrencyValue, Map<String, ProductCharge>>>(
        new ProductSortOrderSort());

    List<ProductRevision> catalogProductRevisions = new ArrayList<ProductRevision>();
    List<ProductRevision> rpbProductRevisions = new ArrayList<ProductRevision>();
    if (timeline.equals("current")) {
      catalogProductRevisions = channelService.getChannelRevision(channel,
          channelService.getCurrentRevision(channel).getStartDate(), false).getProductRevisions();

      rpbProductRevisions = channelService.getChannelRevision(null,
          channelService.getCurrentRevision(channel).getStartDate(), false).getProductRevisions();

    } else if (timeline.equals("planned")) {
      catalogProductRevisions = channelService.getChannelRevision(channel,
          channelService.getFutureRevision(channel).getStartDate(), false).getProductRevisions();

      rpbProductRevisions = channelService.getChannelRevision(null,
          channelService.getFutureRevision(channel).getStartDate(), false).getProductRevisions();

    } else if (timeline.equals("history")) {
      catalogProductRevisions = channelService.getChannelRevision(channel, date, false).getProductRevisions();

      rpbProductRevisions = channelService.getChannelRevision(null, date, false).getProductRevisions();
    }

    Map<Product, ProductRevision> rpbProductRevisionMap = new HashMap<Product, ProductRevision>();
    for (ProductRevision productRevision : rpbProductRevisions) {
      rpbProductRevisionMap.put(productRevision.getProduct(), productRevision);
    }

    for (ProductRevision productRevision : catalogProductRevisions) {
      ProductRevision rpbProductRevision = rpbProductRevisionMap.get(productRevision.getProduct());
      Map<CurrencyValue, Map<String, ProductCharge>> currencyProductPriceMap = new TreeMap<CurrencyValue, Map<String, ProductCharge>>(
          new CurrencyValueSort());
      for (ProductCharge productCharge : productRevision.getProductCharges()) {
        if (currencyProductPriceMap.get(productCharge.getCurrencyValue()) == null) {
          currencyProductPriceMap.put(productCharge.getCurrencyValue(), new LinkedHashMap<String, ProductCharge>());
        }
        currencyProductPriceMap.get(productCharge.getCurrencyValue()).put("catalog", productCharge);
        if (rpbProductRevision == null) {
          currencyProductPriceMap.get(productCharge.getCurrencyValue()).put("rpb", null);
        }
      }
      if (rpbProductRevision != null) {
        for (ProductCharge productCharge : rpbProductRevision.getProductCharges()) {
          if (currencyProductPriceMap.get(productCharge.getCurrencyValue()) != null) {
            currencyProductPriceMap.get(productCharge.getCurrencyValue()).put("rpb", productCharge);
          }
        }
      }
      fullProductPricingMap.put(productRevision.getProduct(), currencyProductPriceMap);
    }

    return fullProductPricingMap;
  }

  private Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> getBundlePricingMap(
      Channel channel, String timeline, Date date) {
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
    List<ProductBundleRevision> rpbProductBundleRevisions = new ArrayList<ProductBundleRevision>();
    if (timeline.equals("current")) {
      catalogProductBundleRevisions = channelService.getChannelRevision(channel,
          channelService.getCurrentRevision(channel).getStartDate(), false).getProductBundleRevisions();

      rpbProductBundleRevisions = channelService.getChannelRevision(null,
          channelService.getCurrentRevision(channel).getStartDate(), false).getProductBundleRevisions();

    } else if (timeline.equals("planned")) {
      catalogProductBundleRevisions = channelService.getChannelRevision(channel,
          channelService.getFutureRevision(channel).getStartDate(), false).getProductBundleRevisions();

      rpbProductBundleRevisions = channelService.getChannelRevision(null,
          channelService.getFutureRevision(channel).getStartDate(), false).getProductBundleRevisions();

    } else if (timeline.equals("history")) {
      catalogProductBundleRevisions = channelService.getChannelRevision(channel, date, false)
          .getProductBundleRevisions();

      rpbProductBundleRevisions = channelService.getChannelRevision(null, date, false).getProductBundleRevisions();
    }

    Map<ProductBundle, ProductBundleRevision> rpbProductBundleRevisionMap = new HashMap<ProductBundle, ProductBundleRevision>();
    for (ProductBundleRevision productBundleRevision : rpbProductBundleRevisions) {
      rpbProductBundleRevisionMap.put(productBundleRevision.getProductBundle(), productBundleRevision);
    }

    for (ProductBundleRevision ctalaogProductBundleRevision : catalogProductBundleRevisions) {

      Map<CurrencyValue, Map<String, RateCardCharge>> currencyProductBundlePriceMap = new TreeMap<CurrencyValue, Map<String, RateCardCharge>>(
          new CurrencyValueSort());

      ProductBundleRevision rpbProductBundleRevision = rpbProductBundleRevisionMap.get(ctalaogProductBundleRevision
          .getProductBundle());

      for (RateCardCharge rcc : ctalaogProductBundleRevision.getRateCardCharges()) {
        if (currencyProductBundlePriceMap.get(rcc.getCurrencyValue()) == null) {
          currencyProductBundlePriceMap.put(rcc.getCurrencyValue(), new LinkedHashMap<String, RateCardCharge>());
        }
        if (rcc.getRateCardComponent().isRecurring()) {
          currencyProductBundlePriceMap.get(rcc.getCurrencyValue()).put("catalog-recurring", rcc);
        } else {
          currencyProductBundlePriceMap.get(rcc.getCurrencyValue()).put("catalog-onetime", rcc);
        }
      }
      for (RateCardCharge rcc : rpbProductBundleRevision.getRateCardCharges()) {
        if (currencyProductBundlePriceMap.get(rcc.getCurrencyValue()) != null) {
          if (rcc.getRateCardComponent().isRecurring()) {
            currencyProductBundlePriceMap.get(rcc.getCurrencyValue()).put("rpb-recurring", rcc);
          } else {
            currencyProductBundlePriceMap.get(rcc.getCurrencyValue()).put("rpb-onetime", rcc);
          }
        }
      }
      fullBundlePricingMap.put(ctalaogProductBundleRevision, currencyProductBundlePriceMap);
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

    int totalSize = channelService.getChannelCount();
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
    if (currentRevision != null
        && currentRevision.getStartDate() != null
        && (currentRevision.getStartDate().getTime() <= (new Date()).getTime())) {
      map.addAttribute("isCurrentThere", true);
    }

    map.addAttribute("supportedCurrencies", listSupportedCurrencies);
    map.addAttribute("currenciestoadd", currenciestoadd);
    map.addAttribute("futureRevisionDate", channelService.getFutureRevision(channel).getStartDate());
    map.addAttribute("effectiveDate", channelService.getFutureRevision(channel).getStartDate());

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
    channel.setName(channelName);
    channel.setDescription(description);
    channel.setCode(code);
    channelService.updateChannel(channel);

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
    map.addAttribute("channels", channelService.getChannels());
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
    try {
      channelService.locateChannel(channelName);
      response.setStatus(CODE_NOT_UNIQUE_ERROR_CODE);
      return null;
    } catch (NoSuchChannelException ex) {
      // Do nothing
    }

    Channel channel = new Channel(channelName, ChannelType.valueOf("CHANNEL"));
    Catalog catalog = new Catalog(channelName, description, null, null, getCurrentUser());
    for (String currency : currencyValueList) {
      for (CurrencyValue cv : currencyValueService.listActiveCurrencies()) {
        if (currency.equals(cv.getCurrencyCode())) {
          SupportedCurrency supportedCurrency = new SupportedCurrency(catalog, cv, getCurrentUser());
          catalog.getSupportedCurrencies().add(supportedCurrency);
        }
      }
    }
    Catalog catalogSaved = channelService.save(catalog);
    channel.setCatalog(catalogSaved);
    channel.setDescription(description);
    channel.setCode(code);
    channel = channelService.createChannel(channel);

    // Copy all the product charges that are active in the current revision
    copyAllProductCharges(channel, catalogSaved.getSupportedCurrencyValuesByOrder());

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
      Catalog catalog = channel.getCatalog();

      JSONArray jsonArray = new JSONArray(currencyCodeArray);

      Set<SupportedCurrency> supportedCurrencies = catalog.getSupportedCurrencies();
      List<CurrencyValue> listNewCurrenciesAdded = new ArrayList<CurrencyValue>();

      for (int index = 0; index < jsonArray.length(); index++) {
        String currencyCode = jsonArray.getString(index);
        CurrencyValue currencyVal = currencyValueService.locateBYCurrencyCode(currencyCode);
        listNewCurrenciesAdded.add(currencyVal);

        SupportedCurrency new_currency = new SupportedCurrency(catalog, currencyVal, getCurrentUser());
        supportedCurrencies.add(new_currency);
      }

      // Copy all the charges for products and bundles in the catalog
      if (listNewCurrenciesAdded != null && listNewCurrenciesAdded.size() > 0) {
        copyAllProductCharges(channel, listNewCurrenciesAdded);

        for (ProductBundleRevision productBundleRevision : channelService.getFutureChannelRevision(channel, false)
            .getProductBundleRevisions()) {
          copyBundleCharges(catalog, productBundleRevision, listNewCurrenciesAdded);
        }
      }

      catalog.setSupportedCurrencies(supportedCurrencies);
      channelService.updateCatalog(catalog);

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

    List<ProductBundleRevision> productBundleRevisions = channelService.getFutureChannelRevision(channel, false)
        .getProductBundleRevisions();
    List<ProductBundle> productBundlesInChannel = new ArrayList<ProductBundle>();
    for (ProductBundleRevision productBundleRevision : productBundleRevisions) {
      productBundlesInChannel.add(productBundleRevision.getProductBundle());
    }
    List<ProductBundleRevision> globalProductBundleRevisions = channelService.getChannelRevision(null,
        channelService.getCurrentChannelReferenceCatalogRevision(channel).getReferenceCatalogRevision().getStartDate(),
        false).getProductBundleRevisions();

    List<ProductBundleRevision> bundlesToBeSent = new ArrayList<ProductBundleRevision>();
    for (ProductBundleRevision productBundleRevision : globalProductBundleRevisions) {
      if (productBundleRevision.getProductBundle().getPublish()
          && !productBundlesInChannel.contains(productBundleRevision.getProductBundle()))
        bundlesToBeSent.add(productBundleRevision);
    }

    map.addAttribute("productBundles", bundlesToBeSent);
    logger.debug("### listbundles method ending...(GET)");
    return "productbundle.add";
  }

  @RequestMapping(value = ("/attachProductBundles"), method = RequestMethod.POST)
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
      Catalog catalog = channel.getCatalog();

      // Get the list of all the bundles already attached with the catalog so that we can prevent
      // multiple additions of the bundle in the channel.
      ChannelRevision futureChannelRevision = channelService.getFutureChannelRevision(channel, false);
      List<String> bundleIdsAlreadyAddedToChannel = new ArrayList<String>();
      for (ProductBundleRevision productBundleRevision : futureChannelRevision.getProductBundleRevisions()) {
        bundleIdsAlreadyAddedToChannel.add(productBundleRevision.getProductBundle().getId().toString());
      }

      List<CurrencyValue> listSupportedCurrencies = channel.getCatalog().getSupportedCurrencyValuesByOrder();
      Revision futureRevision = channelService.getFutureRevision(channel);

      JSONArray jsonArray = new JSONArray(selectedProductBundles);
      for (int index = 0; index < jsonArray.length(); index++) {
        String bundleId = jsonArray.getString(index);

        // If bundle is already added, then don't add
        if (bundleIdsAlreadyAddedToChannel.contains(bundleId)) {
          continue;
        }

        ProductBundle productBundle = productBundleService.getProductBundleById(Long.parseLong(bundleId));

        // Set the start date of the catalog product bundle to the plan date of the channel catalog
        ProductBundleRevision productBundleRevision = productBundleService.getProductBundleRevision(productBundle,
            channelService.getCurrentChannelReferenceCatalogRevision(channel).getReferenceCatalogRevision()
                .getStartDate(), null);

        // Save new catalog product bundle
        CatalogProductBundle catalogProductBundle = new CatalogProductBundle(catalog, productBundle,
            actorService.getActor());
        catalogProductBundle.setRevision(futureRevision);
        channelService.save(catalogProductBundle);

        // Copy bundle charges
        copyBundleCharges(catalog, productBundleRevision, listSupportedCurrencies);
      }
      channelService.updateCatalog(catalog);

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
        "planned", catalog.getPlanDate());

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
      Catalog catalog = channel.getCatalog();

      Revision futureRevision = channelService.getFutureRevision(channel);
      List<ProductCharge> productCharges = productService.getCatalogPlannedChargesForAllProducts(catalog);

      Map<String, ProductCharge> idCurrencyProductChargeMap = new HashMap<String, ProductCharge>();
      for (ProductCharge productCharge : productCharges) {
        idCurrencyProductChargeMap.put(productCharge.getProduct().getId().toString() + KEY_SEPARATOR
            + productCharge.getCurrencyValue().getCurrencyCode(), productCharge);
      }

      JSONArray jsonArray = new JSONArray(currencyValData);
      for (int index = 0; index < jsonArray.length(); index++) {
        JSONObject jsonObj = jsonArray.getJSONObject(index);
        BigDecimal previousVal = new BigDecimal(jsonObj.get("previousvalue").toString());
        BigDecimal newVal = new BigDecimal(jsonObj.get("value").toString());
        String currencyCode = jsonObj.get("currencycode").toString();
        String productId = jsonObj.get("productId").toString();

        ProductCharge oldProductCharge = idCurrencyProductChargeMap.get(productId + KEY_SEPARATOR + currencyCode);
        if (!previousVal.equals(newVal)) {
          // Cases:
          // 1. Old Product Charge is already pointing to a future revision.
          // Here we just need to update the price.
          // 2. Old Product Charge is pointing to a revision prior to the future revision.
          // Here we need to create a new product charge entry with future revision.

          // Case 1
          if (oldProductCharge.getRevision().getId().equals(futureRevision.getId())) {
            oldProductCharge.setPrice(newVal);
            oldProductCharge.setUpdatedAt(new Date());
            productService.saveProductCharge(oldProductCharge);
          } else {
            // Case 2
            CurrencyValue currencyValue = currencyValueService.locateBYCurrencyCode(currencyCode);
            Product product = productService.locateProductById(productId);

            ProductCharge newProductCharge = new ProductCharge();
            newProductCharge.setCatalog(catalog);
            newProductCharge.setProduct(product);
            newProductCharge.setCurrencyValue(currencyValue);
            newProductCharge.setPrice(newVal);
            newProductCharge.setUpdatedAt(new Date());
            newProductCharge.setRevision(futureRevision);
            productService.saveProductCharge(newProductCharge);
          }
        }
      }
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
        channel, "planned", futureRevision.getStartDate());

    ProductBundleRevision productBundleRevision = null;
    for (ProductBundleRevision prodcutBundleRevision : fullBundlePricingMap.keySet()) {
      if (prodcutBundleRevision.getProductBundle().getId().toString().equals(bundleId)) {
        productBundleRevision = prodcutBundleRevision;
      }
    }

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
      Catalog catalog = channel.getCatalog();
      ProductBundle productBundle = productBundleService.locateProductBundleById(bundleId);

      List<RateCardCharge> rateCardCharges = productBundleService.getCatalogProductBundlePlannedCharges(catalog,
          productBundle);

      Map<String, RateCardCharge> rateCardCurrencyChargeMap = new HashMap<String, RateCardCharge>();
      for (RateCardCharge rateCardCharge : rateCardCharges) {
        Long isRecurring = 0L;
        if (rateCardCharge.getRateCardComponent().getIsRecurring()) {
          isRecurring = 1L;
        }
        rateCardCurrencyChargeMap.put(
            rateCardCharge.getCurrencyValue().getCurrencyCode() + KEY_SEPARATOR + isRecurring.toString(),
            rateCardCharge);
      }

      JSONArray jsonArray = new JSONArray(currencyValData);

      Revision futureRevision = channelService.getFutureRevision(channel);
      for (int index = 0; index < jsonArray.length(); index++) {
        JSONObject jsonObj = jsonArray.getJSONObject(index);
        BigDecimal previousVal = new BigDecimal(jsonObj.get("previousvalue").toString());
        BigDecimal newVal = new BigDecimal(jsonObj.get("value").toString());
        String currencyCode = jsonObj.get("currencycode").toString();
        String isRecurring = jsonObj.getString("isRecurring").toString();

        if (!previousVal.equals(newVal)) {
          // Cases:
          // 1. Old Rate Card Charge is already pointing to a future revision.
          // Here we just need to update the price.
          // 2. Old Rate Card Charge is pointing to a revision prior to the future revision.
          // Here we need to create a new rate card charge entry with future revision.

          RateCardCharge oldRateCardCharge = rateCardCurrencyChargeMap.get(currencyCode + KEY_SEPARATOR + isRecurring);
          // Case 1
          if (oldRateCardCharge.getRevision().getId().equals(futureRevision.getId())) {
            // Case 1
            oldRateCardCharge.setPrice(newVal);
            oldRateCardCharge.setUpdatedAt(new Date());
            productBundleService.saveRateCardCharge(oldRateCardCharge);
          } else {
            // Case 2
            CurrencyValue currencyValue = currencyValueService.locateBYCurrencyCode(currencyCode);

            RateCardCharge newRateCardCharge = new RateCardCharge();
            newRateCardCharge.setCatalog(catalog);
            newRateCardCharge.setRateCardComponent(oldRateCardCharge.getRateCardComponent());
            newRateCardCharge.setCurrencyValue(currencyValue);
            newRateCardCharge.setPrice(newVal);
            newRateCardCharge.setRevision(futureRevision);
            newRateCardCharge.setUpdatedAt(new Date());
            productBundleService.saveRateCardCharge(newRateCardCharge);
          }
        }
      }

      status = true;
    } catch (ServiceException ex) {
      throw new InvalidAjaxRequestException(ex.getMessage());
    }
    logger.debug("### editCatalogProductBundlePricing method ending...(POST)");
    return status == true ? "success" : "failure";
  }

  @RequestMapping(value = ("/detachproductbundle"), method = RequestMethod.POST)
  @ResponseBody
  public String detachProductBundle(@RequestParam(value = "channelId", required = true) String channelId,
      @RequestParam(value = "bundleId", required = true) String bundleId, ModelMap map) {
    logger.debug("### detachproductbundle method starting...(POST)");
    Channel channel = channelService.getChannelById(channelId);
    Catalog catalog = channel.getCatalog();

    ChannelRevision channelRevision = channelService.getFutureChannelRevision(channel, false);
    boolean status = true;
    try {
      List<ProductBundleRevision> newProductBundleRevisions = new ArrayList<ProductBundleRevision>();
      for (ProductBundleRevision productBundleRevision : channelRevision.getProductBundleRevisions()) {
        if (productBundleRevision.getProductBundle().getId().toString().equals(bundleId)) {
          // channelService.detachProductBundleFromCatalog(catalogProductBundle);
          // TODO: Detach this entry.
        } else {
          newProductBundleRevisions.add(productBundleRevision);
        }
      }
      channelService.updateCatalog(catalog);
    } catch (Exception e) {
      status = false;
    }
    logger.debug("### detachproductbundle method ending...(POST)");
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
        "current", currentRevision.getStartDate());

    Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = getBundlePricingMap(
        channel, "current", null);

    List<ProductBundleRevision> productBundleRevisions = getProductBundles(new ArrayList<ProductBundleRevision>(
        fullBundlePricingMap.keySet()), currentPage, perPageCount);

    map.addAttribute("supportedCurrencies", catalog.getSupportedCurrencyValuesByOrder());
    map.addAttribute("fullProductPricingMap", fullProductPricingMap);
    map.addAttribute("noOfProducts", fullProductPricingMap.size());
    map.addAttribute("productBundleRevisions", productBundleRevisions);
    map.addAttribute("fullBundlePricingMap", fullBundlePricingMap);
    map.addAttribute("channel", channel);

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
        "planned", futureRevision.getStartDate());

    Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = getBundlePricingMap(
        channel, "planned", null);

    List<ProductBundleRevision> productBundleRevisions = getProductBundles(new ArrayList<ProductBundleRevision>(
        fullBundlePricingMap.keySet()), currentPage, perPageCount);

    int sizeOfPublishedProductBundles = 0;
    for (ProductBundleRevision productBundleRevision : channelService.getChannelRevision(null,
        channelService.getCurrentChannelReferenceCatalogRevision(channel).getReferenceCatalogRevision().getStartDate(),
        false).getProductBundleRevisions()) {
      if (productBundleRevision.getProductBundle().getPublish()) {
        ++sizeOfPublishedProductBundles;
      }
    }

    boolean bundlesaddedtocatalog = false;
    int sizeOfPublishedBundlesAddedToCatalog = 0;
    for (ProductBundleRevision productBundleRevision : fullBundlePricingMap.keySet()) {
      bundlesaddedtocatalog = true;
      if (productBundleRevision.getProductBundle().getPublish()) {
        sizeOfPublishedBundlesAddedToCatalog += 1;
      }
    }

    boolean bundlestoadd = true;
    if (sizeOfPublishedBundlesAddedToCatalog == sizeOfPublishedProductBundles) {
      bundlestoadd = false;
    }

    List<CurrencyValue> listSupportedCurrencies = catalog.getSupportedCurrencyValuesByOrder();

    map.addAttribute("supportedCurrencies", listSupportedCurrencies);
    map.addAttribute("channel", channel);
    map.addAttribute("fullProductPricingMap", fullProductPricingMap);
    map.addAttribute("noOfProducts", fullProductPricingMap.size());
    map.addAttribute("productBundleRevisions", productBundleRevisions);
    map.addAttribute("bundlestoadd", bundlestoadd);
    map.addAttribute("fullBundlePricingMap", fullBundlePricingMap);
    map.addAttribute("bundlesaddedtocatalog", bundlesaddedtocatalog);
    map.addAttribute("toalloweditprices", true);

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
          historyDateObj = (Date) formatter.parse(historyDate);
        } catch (ParseException e) {
          throw new InvalidAjaxRequestException(e.getMessage());
        }
      } else {
        historyDateObj = historyDatesForCatalog.get(0);
      }

      Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> fullProductPricingMap = getProductChargeMap(channel,
          "history", historyDateObj);

      Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = getBundlePricingMap(
          channel, "history", historyDateObj);

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
      Date planDate = getPlanDate((Date) formatter.parse(newDate));
      Revision revision = channelService.getFutureRevision(channel);
      if (revision.getStartDate() == null) {
        Revision newRevision = new Revision(channel.getCatalog(), null, 1L, new Date(), channel.getCatalog()
            .getCreatedBy());
        newRevision = channelService.createRevision(newRevision);
        // Need to update the channel ref catalog revision too
        channelService.createAndSaveChannelRefCatRevision(channel, newRevision);
      }
      revision.setStartDate(planDate);
      channelService.updateRevision(revision);

      channel.getCatalog().setPlanDate(planDate);
      channelService.updateCatalog(channel.getCatalog());
      status = true;
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

  @RequestMapping(value = ("/activatecatalog"), method = RequestMethod.POST)
  @ResponseBody
  public String activatecatalog(@RequestParam(value = "Id", required = true) String channelId, ModelMap map) {
    logger.debug("### activatecatalog method starting...(POST)");

    boolean status = false;
    try {
      Channel channel = channelService.getChannelById(channelId);
      planDateChangeBeforeActivation(channel.getCatalog(), new Date());
      status = true;
    } catch (ServiceException ex) {
      throw new InvalidAjaxRequestException(ex.getMessage());
    }
    logger.debug("### activatecatalog method end...(POST)");
    return status == true ? "success" : "failure";
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

    String rootImageDir = config.getValue(Names.com_citrix_cpbm_portal_settings_images_uploadPath);
    if (rootImageDir != null && !rootImageDir.trim().equals("")) {
      Channel channel = channelService.getChannelById(form.getChannel().getId().toString());
      ChannelLogoFormValidator validator = new ChannelLogoFormValidator();
      validator.validate(form, result);
      if (result.hasErrors()) {
        setPage(map, Page.CHANNELS);
        return messageSource.getMessage(result.getFieldError("logo").getCode(), null, request.getLocale());
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
      logger.debug("### editChannelLogo method ending (No Image Logo Dir Defined)...(POST)");
      return messageSource.getMessage(result.getFieldError("logo").getCode(), null, request.getLocale());
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
        fullBundlePricingMap = getBundlePricingMap(channel, "planned", null);
        actiontoshow = 1;
      } else if (which.equals("current")) {
        fullBundlePricingMap = getBundlePricingMap(channel, "current", null);
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
        historyDate = (Date) formatter.parse(date);
      } catch (ParseException e) {
        throw new InvalidAjaxRequestException(e.getMessage());
      }
    }

    if (bundleId == null || bundleId.equals("")) {
      List<Product> productsList = new ArrayList<Product>();
      Map<Product, Map<CurrencyValue, Map<String, ProductCharge>>> fullProductPricingMap = null;
      if (currentHistoryPlanned.equals("current")) {
        fullProductPricingMap = getProductChargeMap(channel, "current", null);
        productsList = productService.listProducts(null, null, channelService.getCurrentRevision(channel));
      } else if (currentHistoryPlanned.equals("planned")) {
        fullProductPricingMap = getProductChargeMap(channel, "planned", null);
        productsList = productService.listProducts(null, null, channelService.getFutureRevision(channel));
      } else if (currentHistoryPlanned.equals("history")) {
        fullProductPricingMap = getProductChargeMap(channel, "history", historyDate);
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
      Map<ProductBundleRevision, Map<CurrencyValue, Map<String, RateCardCharge>>> fullBundlePricingMap = null;
      if (currentHistoryPlanned.equals("current")) {
        fullBundlePricingMap = getBundlePricingMap(channel, "current", null);
      } else if (currentHistoryPlanned.equals("planned")) {
        fullBundlePricingMap = getBundlePricingMap(channel, "planned", null);
      } else if (currentHistoryPlanned.equals("history")) {
        fullBundlePricingMap = getBundlePricingMap(channel, "current", historyDate);
      }

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

  // Return truncated date if it is in funture other wise new date with timestamp
  private Date getPlanDate(Date planDate) {
    long currentDateMaxTime = DateUtils.setMaxTime(new Date()).getTime();
    if (planDate.getTime() > currentDateMaxTime) {
      planDate = DateUtils.truncate(planDate);
    }else {
      planDate = new Date();
    }

    return planDate;
  }
}
