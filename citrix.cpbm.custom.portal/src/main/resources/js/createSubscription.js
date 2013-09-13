/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */

var currentBundleTemplate = null;
var isPayAsYouGoChosen = false;
var lastRCFilterSelection = "";

var pageNumber = 0;
var pageLimit = 10;
var pageNumberForActive = 0;
var pageLimitForActive = 10;
var enableMore = 0;
var enableMoreForActive = 0;
var moreClick = 0;
var moreClickForActive = 0;
var newSubsBundlesList = null;
var pricingFiltersCountUpToDate = false;
var activeSubsBundlesList = null;

$(document).ready(function() {
  initCreateSubscription();
});

function initCreateSubscription() {

  isPayAsYouGoChosen = ($("#isPayAsYouGoChosen").val() == "true");
  initDialogWithOK("tncDialog");
  $("#tncLink").click(function (e) {
    e.preventDefault();
    dialogButtonsLocalizer($("#tncDialog"), {'OK':g_dictionary.dialogOK}); 
    $("#tncDialog").dialog("open");
  });
  
  prepareSelectedCategory();

  if(isNotBlank($("#cloudServiceException").val())) {
    return;
  }
  
  var newSubscriptionId = null;
  var customEditorPage = $("#customEditorTag").val();
  var customComponentSelector = $("#customComponentSelector").val();
  var productIdsWithEntitlements = {};
  var resourceTypeSelection = $("#resourceType").val();
  var tenantParam = $("#tenantParam").val();
  var serviceInstaceUuid= $("#serviceInstanceUuid").val();
  var $bundleContainer = $("#bundle_container");
  var $bundleTemplate = $bundleContainer.find("#bundle_template");
  var bundleObj_id;
  var pricingFilter = 'ALL';
  var subscriptionId = $("#subscriptionId").val();
  var selectedBundleTab = isNotBlank(subscriptionId) ? "view_subscriptions_tab" : "view_bundles_tab";
  var currentPage = 1;
  var SERVICE_RESOURCE_TYPE = "__SERVICE__";
  var isReconfigure = $("#isReconfigure").val();
  var isStep1Ready = false;
  var isStep2Ready = false;
  var isReprovision = isNotBlank(subscriptionId) && isBlank(isReconfigure);
  
  var selectedResourceComponentsForStep2 = uniqueResourceComponents;
  
  $("#spinning_wheel").show();

  $(".js_resourceType").unbind("change").bind("change", function() {
    changeResourceType($(this).val());
  });
  
  $(".js_resource_type_default").unbind("click").bind("click", function() {
    changeResourceType($(this).attr('id'));
  });
  $("#show_more").unbind("click").bind("click", function() {
	  getMoreBundles();
	  });

  $(window).scroll(function() {
    var pricing_div = $('#pricing');
    var sliding_wrapper = $('.sliding_statswrapper');
    var section2 = $('#SECTION_2');
    var section3 = $('#SECTION_3');
    var section4 = $('#SECTION_4');
    var pricingFilters= $("#pricing");
    var section2Top = section2.top;
    var footerTop = $("#bundle_container").top + $("#bundle_container").height();
    $("#bundle_container").css('min-height',section2.height()-pricingFilters.height());
    var footer = $('#footer');
    if(footer.offset()!=null){
      footerTop = footer.offset().top;
    }
    if(currentPage == 1){
      section2.css({'position':'fixed','top':'5px'});
      var stopHeight = pricing_div.offset().top;
      var section2Foot = section2.offset().top + section2.height();
      if(section2Foot > footerTop -10){
        section2.css({position:'absolute',top: (footerTop - 10) - section2.height()});
      } else {
        if ( stopHeight > section2.offset().top) {
          section2.css({position:'absolute',top: stopHeight});
        }
      }
    }else if(currentPage == 2){/*
      var stopHeight = sliding_wrapper.offset().top + sliding_wrapper.height();
      var section3Foot = section3.offset().top + section3.height();
      var section4Foot = section4.offset().top + section4.height();
      var sticker = (section3Foot<section4Foot)? section3 : section4;
      var stickerFoot = (section3Foot<section4Foot)? section3Foot : section4Foot;
      sticker.css({'position':'fixed','top':'5px'});
      if(stickerFoot > footerTop -10){
        sticker.css({position:'absolute',top: (footerTop - 10) - sticker.height()});
      } else {
        if ( stopHeight > sticker.offset().top) {
          sticker.css({position:'absolute',top: stopHeight});
        }
      }
    */}
  });
  
  $("#back_to_catalog").unbind("click").bind("click", function() {
    
  if(isReprovision) {
    window.location = "/portal/portal/billing/subscriptions?tenant="+tenantParam;
    return;
  }
  
  $("#spinning_wheel").show();
  currentPage = 1;
  isStep1Ready = false;
  isStep2Ready = false;
  currentBundleTemplate = null;
  
  $("#filterSectionProvisionPage").hide();
  $("#configure_subscribe").hide();
  $("#componentsSectionProvisionPage").hide();
  $("#SECTION_4").hide();
  $("#headerSectionProvisionPage").hide();
  $("#customComponentSelectorContent").show();
  if(resourceTypeSelection === SERVICE_RESOURCE_TYPE) {
    $("#componentselector").show();
  }
  if(resourceTypeSelection != SERVICE_RESOURCE_TYPE && !isPayAsYouGoChosen) {
    loadFiltersAndRCs();
  }
  $("#bundle_with_selection_summary_div").show();
  $("#bundle_container").show();
  $("#pricing").show();
  $("#filter").show();
  $("#SECTION_3").removeClass("pull-left").addClass("pull-right");
  $("#SECTION_3").show();
  $("#SECTION_2").show();
  $("#componentselector").show();
  if(isPayAsYouGoChosen) {
    showPAYGTab();
  }
  $("#spinning_wheel").hide();
});
  
  
  for(var i=0; i < uniqueResourceComponents.length; i++) {
    $("input[name='" + uniqueResourceComponents[i] + "']").unbind("change").bind("change", function() {
      if((currentPage == 1 && isStep1Ready) || (currentPage == 2 && isStep2Ready)) {
        if(isBlank($(this).val())) {
          resetInputField($(this));
        }
        refreshBundlesListing();
      }
    });
  }
  
  for(var i=0; i < productProperties.length; i++) {
    $("input[name='" + productProperties[i] + "']").unbind("change").bind("change", function() {
      if(currentPage == 2 && isStep2Ready) {
        if(isBlank($(this).val())) {
          resetInputField($(this));
        }
        refreshYourSelectionSummary();
      }
    });
  }
  
  $('input[name^="prop_"]').unbind("change").bind("change", function(event){
    updateProperty(event.target);
  });
  
  $(".js_bundle_subscriptions_tabs").unbind("click").bind("click", function(event) {
    resetPricingFilterReccurrenceCount();
    selectedBundleTab = $(this).attr("id");
    refreshBundlesListingForSelectedTab();
  });
  
  function refreshBundlesListingForSelectedTab() {
    if(isNotBlank(subscriptionId)) {
      $("#view_subscriptions_tab").show();
    }
    $selectedBundleTab = $("#" + selectedBundleTab);
    if($selectedBundleTab.length > 0) {
      $(".js_bundle_subscriptions_tabs").removeClass('active');
      $(".js_bundle_subscriptions_tabs").addClass('nonactive');
      $selectedBundleTab.removeClass('nonactive');
      $selectedBundleTab.addClass('active');
      pageNumber = 0;
      enableMore = 0;
      pageNumberForActive = 0;
      enableMoreForActive = 0;
    }
    $bundleContainer.empty();
    $("#no_screen_wrapper_div").hide();
    if(selectedBundleTab == 'view_subscriptions_tab') {
      moreClick = 0;
      moreClickForActive = 1;
      populateActiveSubscriptions();
    } else {
      moreClick = 1;
      moreClickForActive = 0;
      populateBundles();
    }
    $bundleContainer.show();
  }
  
  $(".js_pay_as_you_go_action, .js_pay_as_you_go_dropdown").bind("mouseover", function(e) {
    e.preventDefault();
    $("#spinning_wheel2").show();
    resetInputFields();
    decideOnGroupChoice($(this).parents(".btn-group"));
    $("#spinning_wheel2").hide();
  });
  
  $(".utility_rates_link").unbind("click").bind("click", function() {        
    viewUtilityPricing();
  });
  
  $("#launchResourcePrimaryMenu, #launchResource").unbind("click").bind("click", function(event) {
    launchVM(true);
  });
  $("#launchResourceSecondaryMenu").unbind("click").bind("click", function(event) {
    launchVM(false);
  });
  
  $("#currency_selector").bind('mouseover',function(event)
      {
    $("#catalog_currencybox_dropdown").show();
      });

  $("#currency_selector").bind('mouseout',function(event)
      {
    $("#catalog_currencybox_dropdown").hide();
      });

  $(".currencyLi").unbind('click').bind('click',function(event){
    $("#spinning_wheel2").show();
    $("#selectedcurrencytext").html($(this).attr('id'));
    $("#selectedcurrencyflag").html('<img src="/portal/images/flags/'+$(this).attr('id')+'.gif"/>');
    $("#catalog_currencybox_dropdown").hide();
    $("#selectedCurrencySign").val($(this).attr('sign'));
    if(currentPage==1 && isPayAsYouGoChosen){
      populateUtilityRatesTable(tenantParam, serviceInstaceUuid);
    }
    else {
      refreshBundlesListing();
    }
    $("#spinning_wheel2").hide();
  });
  
  currentPage = 1;
  
  if(isPayAsYouGoChosen) {
    showPAYGTab();
    $("#spinning_wheel").hide();
    return;
  }

  showBundlesViewTab();

  if(resourceTypeSelection != SERVICE_RESOURCE_TYPE) {
    if(isNotBlank(subscriptionId)) {
      var subscriptionConfJsonStr = $("#subscriptionConfJson").val();
      var subscriptionConfJsonObj = jQuery.parseJSON(subscriptionConfJsonStr);
      
      for(var i=0; i<allConfigurationProperties.length; i++) {
        var value = subscriptionConfJsonObj[allConfigurationProperties[i]];
        if(isNotBlank(value)) {
          $("input[name='" + allConfigurationProperties[i] + "']").val(value);
        }
      }
    }
    
    loadFiltersAndRCs();
    
  } else {
    refreshBundlesListing();
  }
  
  if(isReprovision) {
    var possibleGroups = getGroupPossibilities();
    setAndSubscribeSelectedResourceComponentsForStep2($bundleContainer.find('div[id^=bundle_]').filter(":first"), possibleGroups[0]);
  }
  
  
  $("#spinning_wheel").hide();
  
  function showPAYGTab() {
    $("#spinning_wheel2").show();
    $("#SECTION_1").show();
    $("#SECTION_2").hide();
    $("#SECTION_3").hide();
    $("#SECTION_4").hide();
    $("#effective_date_box").text('');
    populateUtilityRatesTable($("#tenantParam").val(),$("#serviceInstanceUuid").val());
    $("#effective_date_box").text(effective_date_str);
    $("#pay_as_you_go_action_container_2").show();
    $("#spinning_wheel2").hide();
  }

  function showBundlesViewTab() {
    $("#spinning_wheel2").show();
    $("#pay_as_you_go_action_container_2").hide();
    $("#SECTION_1").hide();
    $("#SECTION_2").show();
    $("#SECTION_3").show();
    $("#SECTION_4").hide();
    $("#spinning_wheel2").hide();
  }

  function refreshYourSelectionSummary() {
    if(typeof(bundleObj) == "undefined" && !isPayAsYouGoChosen) {
      return;
    }
    
    $("#RHS_your_selection_contentArea").html('');
    
    if(!isPayAsYouGoChosen) {
      var html = $("#RHS_your_selection").clone();
      html.find("#RHS_your_selection_header").text(dictionary.label_Bundle);
      html.find("#RHS_your_selection_value").text(bundleObj.name);
      $("#RHS_your_selection_contentArea").append(html.show());  
    }
    
    for(var i=0; i<allConfigurationProperties.length; i++) {
      var $inputField = $("input[name='" + allConfigurationProperties[i] + "']");
      var fieldDisplayName = $inputField.data('fieldDisplayName');
      var valueDisplayName = $inputField.data('valueDisplayName'); 
      var fieldValue = $inputField.val();
      if(isNotBlank(fieldValue) && isBlank(valueDisplayName)) {
        valueDisplayName = fieldValue;
      }
      if(isBlank(fieldDisplayName)) {
        fieldDisplayName = l10dict[allConfigurationProperties[i] + "-name"];
      }
      if(isNotBlank(fieldValue)) {
        var html = $("#RHS_your_selection").clone();
        html.find("#RHS_your_selection_header").text(fieldDisplayName);
        html.find("#RHS_your_selection_value").text(valueDisplayName);
        $("#RHS_your_selection_contentArea").append(html.show());
      }
    }
  }
  
  function populateEntitlements() {
    var bundleRevision = $(currentBundleTemplate).data("bundleRevisionObj");
    $("#msg_RHS_entitlements").hide();
    if(typeof(bundleRevision) == "undefined" || bundleRevision == null || isPayAsYouGoChosen) {
      return;
    }
    
    var productRatesMap = {};
    $.ajax({
      type : "GET",
      async : true,
      cache : true,
      url : "/portal/portal/products/listProductsForSelectedContext",
      data : {
        serviceInstanceUuid : $("#serviceInstanceUuid").val(),
        resourceType : resourceTypeSelection,
        contextString : "",
        filters : "",
        listAll : "true",
        currencyCode : $("#selectedcurrencytext").text()
      },
      dataType : "json",
      success : function(productCharges) {
        var $listBox = $("#included_usage").empty();
        if(productCharges != undefined && productCharges != null && productCharges.length>0) {
          for(var i=0; i<productCharges.length; i++) {
            var productCharge = productCharges[i];
            productRatesMap[productCharge.product.id] = productCharge;
          }
        } 
        var bundle = bundleRevision.productBundle;
        var entitlements = bundleRevision.entitlements;
        if (entitlements != null && entitlements.length > 0) {
          $("#msg_RHS_entitlements").show();
          for (var a = 0; a < entitlements.length; a++) {
            var entitlement = entitlements[a];
            productIdsWithEntitlements[entitlement.product.id] = "true";
            var usage = entitlement.includedUnits;
            var entitlementText = "<strong>" + entitlement.product.name + "</strong><br/>"
            if (entitlement.includedUnits == -1) {
              entitlementText += dictionary.unlimited + " ";
            } else {
              entitlementText += dictionary.msg_no_extra_charge_upto + " " + usage;
              entitlementText += " " + i18nUomText(entitlement.product.uom);
            }
            var productCharge = productRatesMap[entitlement.product.id];
            var chargeText = "";
            if(entitlement.includedUnits == -1) {
              chargeText += dictionary.msg_overages_charges + " ";
              chargeText += dictionary.label_Not_Applicable;
            } else {
              chargeText += dictionary.msg_overages_charged_at + " ";
              chargeText += $('#selectedCurrencySign').val()+formatNumber(roundNumber((productCharge.price), $("#minFractionDigits").val()));
              chargeText += " / " + i18nUomText(entitlement.product.uom);
            }
            entitlementText += "<br />" + chargeText;
            $listBox.append("<li>" + entitlementText + "</li>");
          }
        }
      }
      
    });
  }
  
  function populateEntitlementsTable(bundleRevision, table_container_id) {
	    
	    if(typeof(bundleRevision) == "undefined" || bundleRevision == null || isPayAsYouGoChosen) {
	      return;
	    }
	    
	    var productRatesMap = {};
	    $.ajax({
	      type : "GET",
	      async : true,
	      cache : true,
	      url : "/portal/portal/products/listProductsForSelectedContext",
	      data : {
	        serviceInstanceUuid : $("#serviceInstanceUuid").val(),
	        resourceType : resourceTypeSelection,
	        contextString : "",
	        filters : "",
	        listAll : "true",
	        currencyCode : $("#selectedcurrencytext").text() 
	      },
	      dataType : "json",
	      success : function(productCharges) {
	        var $table_body = $("#"+table_container_id).find('#totalentitlments');
	        $table_body.empty();
	        if(productCharges != undefined && productCharges != null && productCharges.length>0) {
	          for(var i=0; i<productCharges.length; i++) {
	            var productCharge = productCharges[i];
	            productRatesMap[productCharge.product.id] = productCharge;
	          }
	        } 
	        var bundle = bundleRevision.productBundle;
	        var entitlements = bundleRevision.entitlements;
	        if (entitlements != null && entitlements.length > 0) {
	          
	          for (var a = 0; a < entitlements.length; a++) {
	            var entitlement = entitlements[a];
	            productIdsWithEntitlements[entitlement.product.id] = "true";
	            var usage = "";
	            if (entitlement.includedUnits == -1) {
	              usage = dictionary.unlimited;
	            } else {
	              usage = entitlement.includedUnits + " " + i18nUomText(entitlement.product.uom);
	            }
	            var entitlementText = "<td style='width:200px;'>"+ usage + "</td>";
	            
	            entitlementText += "<td style='width:240px;'>" + entitlement.product.name + "</td>";
	            
	            var productCharge = productRatesMap[entitlement.product.id];
	            var chargeText = "";
	            if(entitlement.includedUnits == -1) {
	              chargeText += dictionary.label_Not_Applicable;
	            } else {
	              chargeText += $('#selectedCurrencySign').val()+formatNumber(roundNumber((productCharge.price), $("#minFractionDigits").val()));
	              chargeText += " / " + i18nUomText(entitlement.product.uom);
	            }
	            entitlementText += "<td style='width:200px;'>" + chargeText+"</td>";
	            
	            $table_body.append("<tr class='hover_enabled'>" + entitlementText + "</tr>");
	          }
	        } else{
	        	var noEntitlementText = "<td style='width:656px;'>"+dictionary.label_no_entitlements+"</td>";
	        	$table_body.append("<tr class='hover_enabled'>" + noEntitlementText + "</tr>");
	        }
	      }
	      
	    });
	  }
  
  function populateProductsForGeneratedUsage() {
    var returnValues;
    $("#msg_RHS_generated_usage").hide();
    $.ajax({
      type : "GET",
      async : true,
      cache : true,
      url : "/portal/portal/products/listProductsForSelectedContext",
      data : {
        serviceInstanceUuid : $("#serviceInstanceUuid").val(),
        resourceType : resourceTypeSelection,
        contextString : getContextStringOverrideEffective(),
        filters : getSelectedFilterString(),
        currencyCode : $("#selectedcurrencytext").text()
      },
      dataType : "json",
      success : function(productCharges) {
    	 
        var $listBox = $("#generated_products_div").empty();
        if(productCharges != undefined && productCharges != null && productCharges.length>0) {
          for(var i=0; i<productCharges.length; i++) {
            var productCharge = productCharges[i];
            if(typeof(productIdsWithEntitlements[productCharge.product.id]) != "undefined" && productIdsWithEntitlements[productCharge.product.id] == "true") {
              continue;
            }
            $("#msg_RHS_generated_usage").show();
            var chargeText = "";
            chargeText += dictionary.label_charged_at + " ";
            chargeText += $('#selectedCurrencySign').val()+formatNumber(roundNumber((productCharge.price), $("#minFractionDigits").val()));
            chargeText += " / " + i18nUomText(productCharge.product.uom);
            $listBox.append("<li><strong>"+ productCharge.product.name + "</strong><br />" + chargeText + "</li>");
          }
        }
      }
    });
  }

  function subscribePAYG() {
    $("#SECTION_1").hide();
    $("#no_screen_wrapper_div").hide();
    $("#msg_RHS_entitlements").hide();
    $("#pricing").hide();
    $("#SECTION_2").hide();
    $("#one_time_charges_content_area").hide();
    $("#recurring_charges_content_area").hide();
    $("#subtotal_content_area").hide();
    $("#total_content_area").hide();
    $("#tax_content_area").hide();
    $("#provision_acceptBoxAndText").hide();
    $("#bundle_with_selection_summary_div").hide();
    productIdsWithEntitlements = {};
    
    currentPage = 2;
    
    populateRCsAndFiltersStep2();
    
    if(isNotBlank(customEditorPage)) {
      initializeEditor();
    }
    isStep2Ready = true;
    
    $("#SECTION_3").removeClass("pull-right").addClass("pull-left");
    $("#SECTION_3").show();
    $("#componentsSectionProvisionPage").show();
    $("#headerSectionProvisionPage").show();
    $("#configure_subscribe").show();
    $("#SECTION_4").show();
    $("#filterSectionProvisionPage").show();
  }
  
  function setAndSubscribeSelectedResourceComponentsForStep2(current, resourceComponents) {
    selectedResourceComponentsForStep2 = resourceComponents;
    if(isPayAsYouGoChosen) {
      subscribePAYG();
    } else {
      currentBundleTemplate = current;
      actionSubscribe();
    }
  }
  
  function decideOnGroupChoice(current) {
    
    var possibleGroups = getGroupPossibilities();
    
    if(resourceTypeSelection == SERVICE_RESOURCE_TYPE || possibleGroups.length == 0) {
      $(current).find(".configure_subscribe_button").unbind("click").bind("click", function() {
        setAndSubscribeSelectedResourceComponentsForStep2(current, []);
      });
      return;
    }
    
    if(possibleGroups.length==1) {
      $(current).find(".configure_subscribe_button").unbind("click").bind("click", function() {
        setAndSubscribeSelectedResourceComponentsForStep2(current, possibleGroups[0]);
      });
      return;
    } 
    $(current).find(".configure_subscribe_button").unbind("click");
    
    var selectedGroup = null;
      
    $(current).find("#group_choice_radios").empty();
    
    for(var i=0; i<possibleGroups.length; i++) {
      var componentNames = [];
      for(var j=0; j<possibleGroups[i].length; j++) {
        var componentName = l10dict[possibleGroups[i][j] + '-name'];
        componentNames.push(componentName);
      }
      
      var $newGroupRow = $("#group_row_template_payg").clone();
      $newGroupRow.data("group", possibleGroups[i]);
      $newGroupRow.find("#group_components_text").text(dictionary.label_Using + " " + componentNames.join(", "));
      
      $newGroupRow.attr("title", dictionary.label_Using + " " + componentNames.join(", "));
      $newGroupRow.unbind("click").bind("click", function() {
        setAndSubscribeSelectedResourceComponentsForStep2(current, $(this).data("group"));
      });
      $(current).find("#group_choice_radios").append($newGroupRow.show()).show();
      $(current).find("#configure_button_group_choice_div").unbind("mouseleave").bind("mouseleave", function () {
        $(this).find("#group_choice_radios").hide();
      });
    }
  }
  
  function actionSubscribe() {
    currentPage = 2;
    $("#spinning_wheel").show();
    $("#SECTION_3").removeClass("pull-right").addClass("pull-left");
    $("#SECTION_1").hide();
    $("#SECTION_2").hide();
    $("#SECTION_2").css({top: '289px'});
    $("#bundle_with_selection_summary_div").hide();
    $("#pricing").hide();
    $("#filterSectionProvisionPage").hide();
    productIdsWithEntitlements = {};

    if(resourceTypeSelection != SERVICE_RESOURCE_TYPE) {
    //Reset those resource components which do not belong to the selected group
      var nonSelectedResourceComponents = arr_diff(uniqueResourceComponents, selectedResourceComponentsForStep2);
      for(var i=0; i<nonSelectedResourceComponents.length; i++) {
        resetInputFieldByName(nonSelectedResourceComponents[i]);
      }
    }
    
    var bundleRevisionObj = $(currentBundleTemplate).data("bundleRevisionObj");
    bundleObj = bundleRevisionObj.productBundle;
    subscriptionId = $(currentBundleTemplate).data("subscriptionId");
    bundleObj_id = bundleRevisionObj.pbid;
    populateEntitlements();
    
    if(resourceTypeSelection != SERVICE_RESOURCE_TYPE) {
      populateRCsAndFiltersStep2();
      if(isNotBlank(customEditorPage)) {
        initializeEditor();
      }
      isStep2Ready = true;
    }
    
    refreshYourSelectionSummary();

    var selected_bundle_ribbon_class = $(currentBundleTemplate).find("#entitlmentsribbon").attr('class');
    $('#bundle_entitlements_ribbon').removeClass().addClass(selected_bundle_ribbon_class);
    $('#bundle_entitlements_ribbon').html("");
    $('#bundle_entitlements_ribbon').html($(currentBundleTemplate).find("#entitlmentsribbon").html());
    
    if ($(currentBundleTemplate).data('link_added')) {
      $('#included_usage').find("[id^=more_link]").attr('id', 'include_usage_more_link').parent().css('background','none');
      $("#include_usage_more_link").unbind("click").bind("click", function(event){
        actionMoreLink(event, 'from_subscribe');
      });
    }
    
    $("#provision_acceptBoxAndText").show();
    $("#subtotal_content_area").show();
    $("#total_content_area").show();
    $("#tax_content_area").show();
    $("#one_time_charges_content_area").show();
    $("#recurring_charges_content_area").show();
    
    if(subscriptionId) {
      newSubscriptionId = subscriptionId;
      if(isNotBlank(isReconfigure)) {
        for(var i=0; i < productProperties.length; i++) {
          $("input[name='prop_" + productProperties[i] + "']").prop('disabled', true);
        }
        $("#accept_checkbox").prop('checked', true);
        $("#provision_accept").prop('checked', true);
        $("#accept_checkbox").prop('disabled', true);
        $("#provision_accept").prop('disabled', true);
      }
    }
    
    if(resourceTypeSelection === SERVICE_RESOURCE_TYPE) {
      $("#msg_RHS_generated_usage").hide();
      $("#servicebundle-info-description").text(bundleObj.name);
      if(bundleObj.description) {
        $("#servicebundle-info-description").append(" : " + bundleObj.description);
      }
      $("#provision_accept").prop('checked', false);
      $("#provision_acceptBoxAndText").hide();
    }
    
    update_price_summary();
    
    subscriptionId = $("#subscriptionId").val();
    isReconfigure = $("#isReconfigure").val();
    
    if(isNotBlank($("#isReconfigure").val())) {
      $("#componentsSectionProvisionPage").find('input:radio').each(function() {
        var componentId = $(this).data('componentId');
        if(isBlank(componentId) || reconfigurableMap[componentId] == "false" || reconfigurableMap[componentId] == undefined) {
          $(this).prop('disabled', true);
        }
      });
      $("#filterSectionProvisionPage").find('input:radio').each(function() {
          $(this).prop('disabled', true);
      });
    }
    
    $("#headerSectionProvisionPage").show();
    $("#SECTION_4").show();
    
    $("#configure_subscribe").show();
    $("#SECTION_3").show();
    
    $("#componentsSectionProvisionPage").show();
    $("#filterSectionProvisionPage").show();
    $("#spinning_wheel").hide();
  }
  
  function viewUtilityPricing() {
    var utility_url = "/portal/portal/subscription/utilityrates_table?tenant="+tenantParam+"&currencyCode=" + $("#selectedcurrencytext").text();
    if(serviceInstaceUuid != null){
      var filterString = getSelectedFilterString();
      var contextString = getContextString();
      utility_url = utility_url+ "&serviceInstanceUuid="+serviceInstaceUuid+"&contextString="+contextString+"&filters="+filterString;
    }
    viewUtilitRates(tenantParam,"utilityrates_lightbox",utility_url);
  } 
  
  function update_price_summary() {   
    
    var bundle_activation_charges = parseFloat($(currentBundleTemplate).data("activationCharges"));
    var bundle_recurring_charges = parseFloat($(currentBundleTemplate).data("recurringCharges"));
    
    var bundle_price_before_tax = bundle_recurring_charges + bundle_activation_charges;
    var subscription_total_amount_before_tax = parseFloat(bundle_price_before_tax);
    var num = new Number(subscription_total_amount_before_tax);
    var tax_amount = Number(getTaxableAmount(num));
    var chargeWithTax = num + tax_amount;
    var subscription_total_amount_after_tax = roundNumber(chargeWithTax,$("#minFractionDigits").val());  
    var new_sub_total = (subscription_total_amount_before_tax);
    
    $('#pricing_reccurence_frequency').text($(currentBundleTemplate).data("pricingFilterDisplay"));
    if($(currentBundleTemplate).data("bundleRevisionObj").productBundle.rateCard.chargeType.name == "NONE") {
      $("#recurring_charges_content_area").hide();
    } else {
      $("#recurring_charges_content_area").show();
    }
    
    $('#one_time_charges').text($('#selectedCurrencySign').val()+formatNumber(roundNumber((bundle_activation_charges),$("#minFractionDigits").val())));
    $('#recurring_charges').text($('#selectedCurrencySign').val()+formatNumber(roundNumber((bundle_recurring_charges),$("#minFractionDigits").val())));
    $('#sub_total_amount').text($('#selectedCurrencySign').val()+formatNumber(roundNumber(new_sub_total,$("#minFractionDigits").val())));
    $('#subscription_total_amount').text($('#selectedCurrencySign').val()+formatNumber(roundNumber((subscription_total_amount_after_tax),$("#minFractionDigits").val())));
    $('#subscription_tax_amount').text($('#selectedCurrencySign').val()+formatNumber(roundNumber((tax_amount),$("#minFractionDigits").val())));
    $('#confirm_activation_charges').find('#price_before_tax').text($('#selectedCurrencySign').val()+formatNumber(roundNumber((bundle_activation_charges),$("#minFractionDigits").val())));
    $('#confirm_selected_bundle').find('#price_before_tax').text($('#selectedCurrencySign').val()+formatNumber(roundNumber((bundle_recurring_charges),$("#minFractionDigits").val())));    
  }
  
    $("#pricing_filters").find('a').unbind('click').bind('click', function(event) {
      
      if($(this).attr('disabled') == 'disabled' || $(this).attr('disabled') == true) {
        return;
      }
      
      $("#spinning_wheel").show();
      pageNumber = 0;
      enableMore = 0;
      pageNumberForActive = 0;
      enableMoreForActive = 0;
      pricingFilter = $(event.target).attr('id')
      $("#pricing_filters").find('li').each(function() {
        $(this).removeClass("nonactive");
        $(this).removeClass("active");
      });
      $(event.target).parent('li').addClass("active");
      refreshBundlesListingForSelectedTab();
      $("#spinning_wheel").hide();
    });
    
   function getActionUrlForListBundle(){
     var actionUrl;
     var resourceType = $("#resourceType").val();
     var SERVICE_RESOURCE_TYPE = "__SERVICE__";
     actionUrl = "/portal/portal/productBundles/list.json?&tenant="
       + tenantParam + "&serviceInstaceUuid=" + serviceInstaceUuid;
     if (resourceType !== SERVICE_RESOURCE_TYPE) {
       actionUrl += "&resourceType=" + resourceType + "&filters="
         + getSelectedFilterString() + "&context="
         + getContextStringOverrideEffective();
     }
     if($("#viewChannelCatalog").val() == 'true'){
       var channelId = $("#channelId").val();
       var revision = $("#revision").val();
       var dateFormat = $("#dateFormat").val();
       var revisionDate = $("#revisionDate").val();
       var currencyCode = $("#selectedcurrencytext").html();
       actionUrl = actionUrl + "&viewCatalog=true&revision="+revision+"&channelParam="+channelId+"&currencyCode="+currencyCode+"&revisionDate="+revisionDate+"&dateFormat="+dateFormat
     }
     else if($("#anonymousBrowsing").val() == 'true'){
       var currencyCode = $("#selectedcurrencytext").html();
       var channelId = $("#channelId").val();
       actionUrl = actionUrl + "&currencyCode="+currencyCode+"&channelParam="+channelId;
     }
     return actionUrl;
   }
   
   function populateCurrentSubscription() {
     var currentListed = false;
     if($("#subscriptionId").length>0){
       var subscriptionBundleInfo = null;
       var subscriptionBundle = $.ajax({
         type: "GET",
         url: "/portal/portal/productBundles/getBundleBySubscription.json?&tenant="+tenantParam+"&serviceInstaceUuid="+serviceInstaceUuid+"&resourceType="+resourceTypeSelection +"&filters="+getSelectedFilterString()+"&context="+ getContextStringOverrideEffective() + "&subscriptionId="+$("#subscriptionId").val(),
         async: false,
         dataType: "json",
         success: function(subscriptionBundleInfo) {
           if(subscriptionBundleInfo != null) {
             var bundleRevision = subscriptionBundleInfo.bundleRevision;
             if (!isValidForCurrentPricingFilter(bundleRevision)) {
               return currentListed;
             }
             visibleSubscription = true;
             var $newBundle;
             if(subscriptionBundleInfo.isCompatible) {
               $newBundle = buildBundleRow(bundleRevision, l10dict['Provision'], null, subscriptionBundleInfo.subscriptionId, true);
             }
             else{
               $newBundle = buildBundleRow(bundleRevision, l10dict['Incompatible'], "incompatible", null, false);
             }
             if(isNotBlank($("#isReconfigure").val())) {
               $newBundle.find(".subscribebutton").text(dictionary.label_Reconfigure);
             }
             $bundleContainer.append($newBundle.show());
             currentListed = true;
           }
         }
       });
     }
     return currentListed;
   }
   
   function populateActiveSubscriptions() {
     var currentListed = populateCurrentSubscription();
     var visibleSubscriptions = 0;
     if(activeSubsBundlesList == null){
	     $.ajax({
	       type: "GET",
	       url:  "/portal/portal/productBundles/listValidSubscriptions.json?&tenant="+tenantParam+"&serviceInstaceUuid="+serviceInstaceUuid+"&resourceType="+resourceTypeSelection +"&filters="+getSelectedFilterString()+"&context="+getContextStringOverrideEffective(),
	       async: false,
	       dataType: "json",
	       success: function(bundleRevisions) {
	         if (!$.isEmptyObject(bundleRevisions)) {
	        	 activeSubsBundlesList = bundleRevisions;
	         }
	       }
	     });
     }
     var filteredBundles = getActiveBundlesForCurrentPricingFilter();
     paginateActiveSubsBundle(filteredBundles);
     if(filteredBundles.length == 0 && !currentListed) {
       $("#no_screen_wrapper_div").find("#msg_no_subscriptions").show();
       $("#no_screen_wrapper_div").find("#msg_no_product_bundles").hide();
       $("#no_screen_wrapper_div").show();
       $("#infinite_scrollbarbox_div").hide();
     }else{
    	if(enableMoreForActive == 1){
    		$("#infinite_scrollbarbox_div").show();
    	}else{
    		$("#infinite_scrollbarbox_div").hide();
    	}
    }
   }
   function getActiveBundlesForCurrentPricingFilter(){
	  var count = 0;
	  var activeFilteredBundles = new Array();
	  var subscriptionId = null;
	  if(activeSubsBundlesList != null){
		  for(subscriptionId in activeSubsBundlesList) {
		    updatePricingFilterReccurrenceCount(activeSubsBundlesList[subscriptionId]);
	           if (!isValidForCurrentPricingFilter(activeSubsBundlesList[subscriptionId]))  {
	             continue;
	           }
			  activeFilteredBundles[count] = subscriptionId;
		      count = count + 1;
		    }
		  enableOrDisableRedundantPricingFilters();
	  }
	  if(activeFilteredBundles.length != 0 && (activeFilteredBundles.length - ((pageNumberForActive +1) * pageLimitForActive) > 0)){
		  enableMoreForActive = 1;
	  }else{
		  enableMoreForActive = 0;
	  }
	  return activeFilteredBundles;
   }
   function paginateActiveSubsBundle(bundleRevisions, currentListed){
	      var start = pageNumberForActive * pageLimitForActive;
	      var limit = (start + pageLimitForActive) < bundleRevisions.length ? (start + pageLimitForActive) : bundleRevisions.length
	      var subscriptionId = null;		  
	      for(var index = start; index < limit; index++) {
	    	  if(currentListed) {
	              $bundleContainer.append("<div class='catalog_current_header'>" + l10dict['subscriptionBundles'] + "</div>");
	            }
	    	  subscriptionId = bundleRevisions[index];
	    	  var $newBundle = buildBundleRow(activeSubsBundlesList[subscriptionId],  l10dict['Provision'], null, subscriptionId, true);
	           $bundleContainer.append($newBundle.show());
	        }
   }
  function populateBundles() {
    if(newSubsBundlesList == null){
	    var actionUrl = getActionUrlForListBundle();
	    $.ajax({
	      type : "GET",
	      url : actionUrl,  
	      async: false,
	      dataType : "json",
	      success : function(bundleRevisions) {   
	        if (bundleRevisions != null && bundleRevisions.length > 0) {
	        	newSubsBundlesList = bundleRevisions;
	        	if(isBlank(subscriptionId) && $("#anonymousBrowsing").val() != 'true') {
	        	  $.ajax({
	              type: "GET",
	              url:  "/portal/portal/productBundles/getValidSubscriptionsCount.json?&tenant="+tenantParam+"&serviceInstaceUuid="+serviceInstaceUuid+"&resourceType="+resourceTypeSelection +"&filters="+getSelectedFilterString()+"&context="+getContextStringOverrideEffective(),
	              async: true,
	              dataType: "json",
	              success: function(bundlesCount) {
	                if(bundlesCount > 0) {
	                  $("#view_subscriptions_tab").show();
	                } else {
	                  $("#view_subscriptions_tab").hide();
	                }
	              }
	            });
	        	} else {
	        	  $("#view_subscriptions_tab").show();
	        	}
	        }
	      }
	    }); 
    }
    var filteredBundles = getBundlesForCurrentPricingFilter(newSubsBundlesList);
    paginateBundlesList(filteredBundles);
    if(filteredBundles.length == 0 ) {
      $("#no_screen_wrapper_div").find("#msg_no_subscriptions").hide();
      $("#no_screen_wrapper_div").find("#msg_no_product_bundles").show();
      $("#no_screen_wrapper_div").show();
	  $("#infinite_scrollbarbox_div").hide();
    }else{
    	if(enableMore == 1){
    		$("#infinite_scrollbarbox_div").show();
    	}else{
    		$("#infinite_scrollbarbox_div").hide();
    	}
    }
  }
  
  function enableOrDisableRedundantPricingFilters() {
    pricingFiltersCountUpToDate = true;
    for(var data in pricingReccurenceFrequencyList) {
      if(pricingReccurenceFrequencyList[data] == 0) {
        $("#" + data).attr("disabled", true);
        $("#" + data).addClass("is_disabled");
      } else {
        $("#" + data).attr("disabled", false);
        $("#" + data).removeClass("is_disabled");
      }
    }
  }
  
  function resetPricingFilterReccurrenceCount() {
    for(var data in pricingReccurenceFrequencyList) {
      pricingReccurenceFrequencyList[data] = 0;
    }
    pricingFiltersCountUpToDate = false;
  }
  
  function updatePricingFilterReccurrenceCount(bundleRevision) {
    if(!pricingFiltersCountUpToDate) {
      pricingReccurenceFrequencyList[bundleRevision.productBundle.rateCard.chargeType.name] += 1;
      if(bundleRevision.productBundle.rateCard.chargeType.name == "NONE") {
        pricingReccurenceFrequencyList["MONTHLY"] += 1;
      }
    }
  }
  
  function getBundlesForCurrentPricingFilter(bundleList){
      var count = 0;
      var filteredBundles = new Array();
      if(bundleList != null){
		  for (var p = 0; p < bundleList.length; p++) {
		      var bundleRevision = bundleList[p];
		      updatePricingFilterReccurrenceCount(bundleRevision);
		      if (!isValidForCurrentPricingFilter(bundleRevision)) {
		        continue;
		      }
		      filteredBundles[count] = bundleRevision;
		      count = count + 1;
		    }
		  enableOrDisableRedundantPricingFilters();
      }
      if(filteredBundles.length != 0 && (filteredBundles.length - ((pageNumber +1) * pageLimit) > 0)){
      	enableMore = 1;
      }else{
      	enableMore = 0;
      }
      return filteredBundles;
  }
  function paginateBundlesList(productBundles){
      var start = pageNumber * pageLimit;
      var limit = (start + pageLimit) < productBundles.length ? (start + pageLimit) : productBundles.length
      for(var index = start; index < limit; index++){
          var $newBundle = buildBundleRow(productBundles[index], null, null, null, true);
          $bundleContainer.append($newBundle.show());
        }

  }
  function getMoreBundles(){
	  if(moreClick == 1 && moreClickForActive == 0){
		  pageNumber = pageNumber + 1
		  populateBundles();
	  }else if(moreClick == 0 && moreClickForActive == 1){
		  pageNumberForActive = pageNumberForActive + 1
		  populateActiveSubscriptions();
	  }
  }
  
  function isValidForCurrentPricingFilter(bundleRevision) {
    if(pricingFilter == 'ALL') {
      return true;
    } else if (bundleRevision.productBundle.rateCard.chargeType.name == pricingFilter) {
      return true;
    } else if (bundleRevision.productBundle.rateCard.chargeType.name == 'NONE' && pricingFilter == 'MONTHLY') {
      return true;
    }
    return false;
  }
  
  function buildBundleRow(bundleRevision, buttonLabel, buttonClass, subscriptionId, isCompatible){
	  // bundleRevision is a light-weight object.
    // so bundle id can be found using bundleRevision.pbid
    var bundle = bundleRevision.productBundle;
    
    var entitlements = bundleRevision.entitlements;
    var entitlementHtml = "";
    var totalentitlementhtml = "";
    var link_added = false;
    for (var a = 0; a < entitlements.length; a++) {
      var entitlement = entitlements[a];
      var usage = "";
      if (entitlement.includedUnits == -1) {
        usage = dictionary.unlimited;
      } else {
        usage = entitlement.includedUnits + "&nbsp;" + i18nUomText(entitlement.product.uom);
      }
      if (a >= 3) {
        if (link_added == false) {
          entitlementHtml = entitlementHtml + '<li>' + '<a id="more_link' + bundleRevision.pbid + '" value="' + dictionary.more + '" href="javascript:void(0);">' + dictionary.view_details + '</a>' + '</li>';
          link_added = true;
        }
        
        continue;
      }
      
      entitlementHtml = entitlementHtml + '<li style="color: #000"><span class="navicon ' + entitlement.product.name + '"></span><span class="text ellipsis" style="margin-top: 14px;">' + usage + "&nbsp;" + dictionary.of + "&nbsp;" + entitlement.product.name + "</span></li>";
    }
    if (!link_added) {
      entitlementHtml = entitlementHtml + '<li>' + '<a id="more_link' + bundleRevision.pbid + '" value="' + dictionary.more + '" href="javascript:void(0);">' + dictionary.view_details + '</a>' + '</li>';
      link_added = true;
    }
    var $newBundle = $bundleTemplate.clone(false);
    //Add service offering details.
    $newBundle.find("#bundle_name").text(bundle.name);
    $newBundle.find("#bundle_description").text(bundle.description);
    $newBundle.find("#bundleDescription").html(bundle.description);
    var ribbonId = Math.floor((Math.random()*7)+1);
    $newBundle.find("#entitlmentsribbon").addClass("col" + ribbonId);
    $newBundle.find("#totalentitlmentsribbon").addClass("col" + ribbonId);
    $newBundle.find("#entitlements").html(entitlementHtml);
    $newBundle.find("#entitlements").attr("title", bundle.name);
   
    //$newBundle.find("#totalentitlments").html(totalentitlementhtml);
    $newBundle.attr("id", "bundle_" + bundleRevision.pbid + "vmproduct_" + bundleRevision.pbid);
    $newBundle.data("bundleRevisionObj", bundleRevision);
    if(subscriptionId!=null){
      $newBundle.data("subscriptionId", subscriptionId);
    }
    $newBundle.data("link_added", link_added);
    $newBundle.find("#name").text(bundle.name);
    $newBundle.find("#name").attr("title", bundle.name);
    $newBundle.find("#currencySign").text($("#selectedCurrencySign").val());
    $newBundle.find("#recurrenceType").text("/" + $("#" + bundle.rateCard.chargeType.name).attr('desc'));
    $newBundle.data("pricingFilterDisplay", $("#" + bundle.rateCard.chargeType.name).text());
    $newBundle.find("#more_link" + bundleRevision.pbid).attr("id", "more_link" + "bundle_" + bundleRevision.pbid + "vmproduct_" + bundleRevision.pbid);
    $newBundle.find("#totalentitlmentsdiv").attr("id", "totalentitlmentsdiv" + "bundle_" + bundleRevision.pbid + "vmproduct_" + bundleRevision.pbid);
    var charges = bundleRevision.rateCardCharges;
    var activationCharge = new Number(0);
    var recurringCharge = new Number(0);
    for (var n = 0; n < charges.length; n++) {
      var rateCardPrice = charges[n];
      if (rateCardPrice == null) {
        continue;
      }
      if (rateCardPrice.rateCardComponent.isRecurring == true) {
        recurringCharge = recurringCharge + rateCardPrice.price;
      } else {
        activationCharge = activationCharge + rateCardPrice.price;
      }
    }
    $newBundle.find("#activationCharges").text("+ " + $("#selectedCurrencySign").val() + activationCharge + " " + dictionary.oneTimeChargeType);
    if (bundle.rateCard.chargeType.name == 'NONE') {
      $newBundle.find("#currencySign").remove();

      $newBundle.find("#recurringCharges").text(dictionary.label_na);
      $newBundle.find("#recurrenceType").html("<a href='javascript:void(0);' class='view_utility_pricing_link'>(" + dictionary.view_utility_pricing + ")</a>");
      $newBundle.find(".view_utility_pricing_link").unbind("click").bind("click", function() {
        viewUtilityPricing();
      });
    }else{
    	$newBundle.find("#recurringCharges").text(recurringCharge);
    }
    
    $newBundle.data('recurringCharges', recurringCharge);
    $newBundle.data('activationCharges', activationCharge);
    $newBundle.find("#more_link" + "bundle_" + bundleRevision.pbid + "vmproduct_" + bundleRevision.pbid).bind("click", function (event) {
      actionMoreLink(event, $(this));
    });
    $newBundle.find("#subscribe").attr("id", "subscribe" + bundleRevision.pbid);
    if(buttonLabel!=null){
      $newBundle.find("#subscribe" + bundleRevision.pbid).text(buttonLabel);
    }
    if(isCompatible){
      $newBundle.find("#subscribe" + bundleRevision.pbid).unbind("mouseover").bind("mouseover", function () {
        decideOnGroupChoice($(this).closest("div[id^='bundle_']"));
      });
    }
    if(buttonClass){
      $newBundle.find(".subscribebutton").removeClass("subscribebutton").addClass(buttonClass); 
    }
    return $newBundle;
  }
  
  function actionMoreLink(event, current, from_page){
	  var current_bundle=$(current).closest("div[id^='bundle_']");
	  var bundle_revision = $(current_bundle).data("bundleRevisionObj");
	  
    var dialogId = "totalentitlmentsdiv";
    var targetId;
    if (from_page=='from_subscribe')
      targetId = $(current).attr("id");
    else
      targetId = $(current).attr("id").substr(9);
    
    dialogId = dialogId + targetId;
    populateEntitlementsTable(bundle_revision, dialogId);
    initDialog(dialogId, 700);
    var $thisDialog = $("#" + dialogId);
    $thisDialog.dialog('option', 'reSizable', false);
    $thisDialog.dialog('option', 'minWidth', 700);
    $thisDialog.dialog('option', 'minHeight', 400);
    $thisDialog.dialog('option', 'height', "auto");
    $thisDialog.dialog('option', 'buttons', {
      "Close": function () {
	    $thisDialog.find(".js_view_utility_link").removeClass("more_up").addClass("more_down");
	    $thisDialog.find(".js_extra_usage_div").hide();
        $(this).dialog("close");
      }
    });
    dialogButtonsLocalizer($thisDialog, {
      'Close': g_dictionary.dialogClose
    });
    $thisDialog.dialog("open");
    
    $thisDialog.find(".js_extra_usage_tab").unbind("click").bind("click", function(e) {
        var html = populateUtilityRatesTable($("#tenantParam").val(),$("#serviceInstanceUuid").val());
        $(this).find(".js_view_utility_link").toggleClass("more_down").toggleClass("more_up");
        $thisDialog.find("#utilityrate_table_bundle_details").html(html);
        $thisDialog.find(".js_extra_usage_div").toggle();
      });
  }

  function openSubscribeDialog(message, messageType){
    initDialog("launch_vm_dialog", 700);
    var $launchVmDialog = $("#launch_vm_dialog");
    $launchVmDialog.dialog("option",{ height: "auto", width : 700,  "resizable":false, dialogClass: 'no-close', closeOnEscape: false });
    $launchVmDialog.dialog('option', 'buttons', {
        "Go to Subscriptions": function () {
          window.location = "/portal/portal/billing/subscriptions?tenant="+tenantParam;
          },
        "Go to Catalog": function () {
            window.location = "/portal/portal/subscription/createsubscription?tenant="+tenantParam;
          },
        "Go to My Resources":function () {
            $launchVmDialog.dialog("close");
            $(window).scrollTop(0);
            showResourcesIFrameWithServiceInstanceUUID($("#serviceInstanceUuid").val());
        },
        "Go to DashBoard": function (){
            window.location = "/portal/portal/home?tenant="+tenantParam+"&secondLevel=true";
          }
      });
      dialogButtonsLocalizer($launchVmDialog, {'Go to Subscriptions':dictionary.goToSubscriptions, 'Go to Catalog': dictionary.goToCatalog, "Go to My Resources": dictionary.goToMyResources, 'Go to DashBoard':dictionary.goToDashboard}); 
      $launchVmDialog.dialog("open");
      $("#launchingVm_template").find("#message2").html(message);
      
      var messageClass="success";
      if(messageType=="error"){
        messageClass="error";
      }
      $("#launchingVm_template").addClass(messageClass);
      $("#launchingVm_template").show();
    }
  
    function launchVM(provision){
      $("#top_message_panel").find("#msg").text("");
      $("#top_message_panel").hide();

      if(!checkGroupFullFillment()) {
        popUpDialogForAlerts("dialog_info", dictionary.error_Group_Not_Satisfied);
        return;
      }
      
      if($("#launchvm_button").attr('disabled') == true || $("#launchvm_button").attr('disabled') == 'disabled') {
        msg = $("#launchvm_button").data('message');
        if(msg != null) {
          popUpDialogForAlerts("dialog_info", msg);
        }
        return;
      }
      
      var productPropertyObj = new Object();
      for(var i=0; i < productProperties.length; i++) {
        target = $("input[name='" + productProperties[i] + "']");
        var eleName = target.attr("name");
        var eleValue = target.val();
        productPropertyObj[eleName]=eleValue;
      }
        
      if($("#accept_checkbox").is(":checked") == false) {
        popUpDialogForAlerts("dialog_info", g_dictionary.youCanNotSubscribeUntilYouAcceptTheTermsAndConditions);
        $("#accept_checkbox").focus();
        $("#spinning_wheel").hide();
        return false;
      }

      var deployVmUrl = "/portal/portal/subscription/subscribe_resource?tenant="+tenantParam+"&productBundleId="+bundleObj_id;

      var propConfigs = JSON.stringify(productPropertyObj);
      
      propConfigs = encodeURIComponent(propConfigs);
      //console.log('...Prop Values..',propConfigs);
      deployVmUrl+= "&configurationData="+propConfigs;
      deployVmUrl+= "&serviceInstaceUuid="+serviceInstaceUuid;
      deployVmUrl+= "&resourceType="+resourceTypeSelection;
      deployVmUrl+= "&filters="+getSelectedFilterString();
      deployVmUrl+= "&context="+getContextString();
      if(provision) {
        deployVmUrl+= "&isProvision=true";
      }
      else{
        deployVmUrl+= "&isProvision=false";
      }
      if($("#subscriptionId").length>0){
        deployVmUrl+= "&subscriptionId=" + $("#subscriptionId").val();
      }
      if(newSubscriptionId != null && newSubscriptionId != "" && typeof(newSubscriptionId) != "undefined"){
        deployVmUrl+= "&newSubscriptionId=" + newSubscriptionId;
      }
        $.ajax({
        type : "POST",
        url : deployVmUrl,
        data: $("#subscriptionForm").serialize(),
        success : function(returnVal) {
          var message = dictionary.subscriptionSuccess;
          if(returnVal.subscriptionResultMessage == "RECONFIGURED"){
            message = dictionary.subscriptionReconfigured;
          }if(returnVal.subscriptionResultMessage == "PROVISIONED"){
            message = dictionary.subscriptionProvisioned;
          }
          openSubscribeDialog(message, "success");
        },
        error : function(jqXHR) {
          var responseText = jqXHR.responseText.trim();
          if(jqXHR.status != AJAX_FORM_VALIDATION_FAILED_CODE){
            var message = "";
            if(responseText.startsWith("RECONFIGURED:")){
              message = dictionary.subscriptionReconfiguredFailure;
              responseText = responseText.substring(13);
            }
            else if(responseText.startsWith("PROVISIONED:")){
              message = dictionary.subscriptionProvisionedFailure;
              responseText = responseText.substring(12);
            }else if(responseText.startsWith("NEWLY_CREATED:")){
              message = dictionary.subscriptionFailure;
              responseText = responseText.substring(14);
            }else{
              message = dictionary.subscriptionFailure;
            }
            message = message+"<br /> "+responseText;
            openSubscribeDialog(message, "error");
          }else{
              var message = "";
              if(responseText.startsWith("RECONFIGURED:")){
                message = dictionary.subscriptionReconfiguredFailure;
                responseText = responseText.substring(13);
              }
              else if(responseText.startsWith("PROVISIONED:")){
                message = dictionary.subscriptionProvisionedFailure;
                responseText = responseText.substring(12);
              }else if(responseText.startsWith("NEWLY_CREATED:")){
                message = dictionary.subscriptionFailure;
                responseText = responseText.substring(14);
              }else{
                message = dictionary.subscriptionFailure;
              }
            message= message +": "+$.parseJSON(responseText)["validationResult"];
            $("#top_message_panel").find("#msg").html(message);
                $("#top_message_panel").find("#status_icon").removeClass("successicon").addClass("erroricon");
                $("#top_message_panel").removeClass("success").addClass("error").show();
          }
        }
      });
    }
    
    function refreshBundlesListing() {
      if(currentPage == 2) {
        refreshYourSelectionSummary();
        populateProductsForGeneratedUsage();
      } else {
        var selectedRCsFiltersString = getContextStringOverrideEffective() + " : " + getSelectedFilterString() + " : " + $("#selectedcurrencytext").html();
        if(lastRCFilterSelection == selectedRCsFiltersString) {
          return;
        }
        lastRCFilterSelection = selectedRCsFiltersString;
        newSubsBundlesList = null;
        resetPricingFilterReccurrenceCount();
        activeSubsBundlesList = null;
        refreshBundlesListingForSelectedTab();
      }
    }
    
    function prepareSelectedCategory() {
      
      var refreshPage = function rePopulateCreateSubscription(serviceInstanceUuid, tenantParam) {
        window.location = getPrefixedLocation($("#resourceType").val());
      };
      
      var selectedCategory = $("#selectedCategory").val();

      if (isBlank(selectedCategory)) {
        selectedCategory = $('#service_category_list_container li')
            .first().attr("category");
      }

      populateServiceInstances(selectedCategory, $("#tenantParam").val(), refreshPage);
      var serviceCategoryListItems = $("#service_category_list_container li");
      serviceCategoryListItems.each(function() {
        $(this).unbind("click").bind("click",
                function() {
                  $(".categorytabs").removeClass("current user");
                  $(this).removeClass().addClass(
                      "categorytabs current user");
                  selectedCategory = $(this).attr("category");
                  populateServiceInstances(selectedCategory, $(
                      "#tenantParam").val(), refreshPage, refreshPage);
                });
      });
    }

    function loadFiltersAndRCs() {
      loadServiceFilters();
      refreshRCsListing();
      isStep1Ready = true;
    }

    function loadServiceFilters() {
      var $container = $("#filters_SECTION_2").empty();
      for(i=0; i<serviceFilterNames.length; i++) {
        var html = $("#list_box_container").clone();
        html.find("#filterBoxTitle").text(l10dict[serviceFilterNames[i] + "-name"]);
        html.find("#filterBoxTitle").parent().attr('title', l10dict[serviceFilterNames[i] + "-desc"]);
        var $listBox = html.find("#filterBoxSelection").empty();
        $listBox.attr("name", serviceFilterNames[i]);
        var serviceFilterValues = getValuesForFilter(serviceFilterNames[i]);
        for(j=0; j<serviceFilterValues.length; j++) {
          var serviceFilterValue = serviceFilterValues[j];
          if(isBlank(JSON.stringify(serviceFilterValue)) || JSON.stringify(serviceFilterValue) == "undefined") {
            continue;
          }

          var name = serviceFilterValue["name"];
          var value = serviceFilterValue["value"];
          
          var extras = getl10nComponentDetails("Filter", serviceFilterValue["attributes"], serviceFilterValue["displayAttributes"], i);

          $listBox.append('<li id="'+value+'" fieldDisplayName="'+ l10dict[serviceFilterNames[i] + "-name"] +'"><span class="catalog_rc_list ellipsis js_displaytext">'+name+'</span><span class="catalog_rc_list description ellipsis">'+extras+'</span> </li>');
          var $itemDesc = $listBox.find('#'+value + " .description");
          $itemDesc.parent('li').attr('title', $itemDesc.text());

        }
        $listBox.find('li').unbind("click").bind("click", function() {
          var $target = $(this);
          
          if($target.attr('disabled') == true || $target.attr('disabled') == 'disabled') {
            return;
          }
          
          $target.siblings(".active").removeClass('active');
          $target.addClass('active');
          refreshFilterInputField($target);
          refreshRCsListing();
        });
        
        var currentInput = $("input[name='" + serviceFilterNames[i] + "']").val();
        var found = false;
        if(isNotBlank(currentInput)) {
          html.find('li').each(function(){
            if($(this).attr('id') == currentInput) {
              found = true;
              $(this).siblings(".active").removeClass('active');
              $(this).addClass('active');
              refreshFilterInputField($(this));
            }
          });
        } 
        
        if(!found) {
          var target = html.find('li').first();
          target.siblings(".active").removeClass('active');
          target.addClass('active');
          refreshFilterInputField(target);
        }
        $container.append(html.show());
      }
    }

    function refreshInputField(target) {
      var $inputField = $("input[name='" + target.parent().attr('name') + "']");
      $inputField.val(target.attr("id"));
      $inputField.data('valueDisplayName', target.find('.js_displaytext').text());
      $inputField.data('fieldDisplayName', target.attr('fieldDisplayName'));
      $inputField.data('effectiveValue', target.attr("effectiveValue"));
    }

    function refreshFilterInputField($target) {
      var $inputField = $("input[name='" + $target.parent().attr('name') + "']");
      $inputField.val($target.attr("id"));
      $inputField.data('valueDisplayName', $target.find('.js_displaytext').text());
      $inputField.data('fieldDisplayName', $target.attr("fieldDisplayName"));
    }

    function getl10nComponentDetails(type, attributes, displayAttributes, index) {
      var compKey;
      var compValue;
      if (type == "ResourceComponent") {
        compKey = uniqueResourceComponentsKey[index];
        compValue = uniqueResourceComponentsDescl10dict[index];
      } else if (type == "Filter") {
        compKey = serviceFilterDescKey[index];
        compValue = serviceFilterDescl10dict[index];
      }
      var extras = "";
      var extras = getFormattedDisplayAttribtutesString(displayAttributes);
      if(isNotBlank(extras)) {
        if (compKey != compValue) {
          extras = getFormattedDisplayAttribtutesDescription(compValue, displayAttributes);
        }
      } else {
        extras = getFormattedAttribtutesString(attributes);
      }
      return extras;
    }
    
    function loadServiceResourceComponents() {
      var $leftFilterPanel = $("#defaultUIRCsListingStep1").clone();
      $leftFilterPanel.empty();
      for(var i=0; i < uniqueResourceComponents.length; i++) {
        var data = getValuesForComponent(uniqueResourceComponents[i]);

        if(data.length == 0) {
          resetInputField($("input[name='" + uniqueResourceComponents[i] + "']"));
          continue;
        }
        
        var html = $("#list_box_container").clone();
        html.find("#filterBoxTitle").text(l10dict[uniqueResourceComponents[i] + "-name"]);
        html.find("#filterBoxTitle").parent().attr('title', l10dict[uniqueResourceComponents[i] + "-desc"]);
        var $listBox = html.find("#filterBoxSelection").empty();
        $listBox.attr("name", uniqueResourceComponents[i]);

        $listBox.append('<li class="active"><span class="catalog_rc_list ellipsis">'+dictionary.label_Any+'</span><span class="catalog_rc_list description ellipsis"></span></li>');
        
        for (var component in data) {
          
          if(isBlank(JSON.stringify(data[component])) || JSON.stringify(data[component]) == "undefined") {
            continue;
          }
          
          var effectiveValue = data[component]["value"];
          if(data[component]["parent"] != null){
            effectiveValue = data[component]["parent"]["value"];
          }
          
          var extras = getl10nComponentDetails("ResourceComponent", data[component]["attributes"], data[component]["displayAttributes"], i);
          
          $listBox.append('<li id="'+data[component]["value"]+'" effectiveValue="'+effectiveValue+'" fieldDisplayName="'+ l10dict[uniqueResourceComponents[i] + "-name"] +'"><span class="catalog_rc_list ellipsis js_displaytext">'+data[component]["name"]+'</span><span class="catalog_rc_list description ellipsis">'+extras+'</span> </li>');
          var $itemDesc = $listBox.find('#'+data[component]["value"] + " .description");
          $itemDesc.parent('li').attr('title', $itemDesc.text());
        }
        
        $listBox.find('li').unbind("click").bind("click", function() {
          var target = $(this);
          if(target.attr('disabled') == true || target.attr('disabled') == 'disabled') {
            return;
          }
          target.siblings(".active").removeClass("active");
          target.addClass("active");
          refreshInputField(target);
          refreshRCsListing();
        });
        
        var currentInput = $("input[name='" + uniqueResourceComponents[i] + "']").val();
        var found = false;
        
        if(isNotBlank(currentInput)) {
          html.find('li').each(function() {
            if($(this).attr('id') == currentInput) {
              found = true;
              $(this).siblings(".active").removeClass("active");
              $(this).addClass("active");
              refreshInputField($(this));
            }
          });
        } 
        
        if(!found) {
          var target = html.find("li").first();
          target.siblings(".active").removeClass("active");
          target.addClass("active");
          refreshInputField(target);
        }
        $leftFilterPanel.append(html.show());
      }
      $("#components_SECTION_2").html($leftFilterPanel.show());
    }

    function getSelectedFiltersAndComponents(filtersOnly, componentsOnly) {
      var selected = [];
      for(var i=0; i < serviceFilterNames.length; i++) {
        var filterValue = $("input[name='" + serviceFilterNames[i] + "']").data('valueDisplayName');
        if(isNotBlank(filterValue) && filterValue != dictionary.all) {
          selected.push(filterValue);
        }
      }
      if(filtersOnly != null && filtersOnly == true) {
        return selected;
      }
      if(componentsOnly != null && componentsOnly == true) {
        selected = [];
      }
      for(var i=0; i < uniqueResourceComponents.length; i++) {
        var compInputVal = $("input[name='" + uniqueResourceComponents[i] + "']").data('valueDisplayName');
        if(isNotBlank(compInputVal) && compInputVal != dictionary.all) {
          selected.push(compInputVal);
        }
      }
      if(selected.length == 0) {
        selected.push(dictionary.label_None);
      }
      return selected;
    }

    function changeResourceType(selectedResourceType) {
      window.location = getPrefixedLocation(selectedResourceType);
    }

    function refreshRCsListing() {
      $("#spinning_wheel").show();
      if(isNotBlank($("#customComponentSelector").val())) {
        initializeComponentSelector(false, 1);
      } else {
        loadServiceResourceComponents();
      }

      refreshBundlesListing();
      
      if(isNotBlank($("#isReconfigure").val())) {
        $("#SECTION_2").find('.catalogfilter_filterlist').each(function() {
          var componentId = $(this).attr('name');
          if(isBlank(componentId) || reconfigurableMap[componentId] == "false" || reconfigurableMap[componentId] == undefined) {
            $(this).find('li').attr('disabled', true);
          }
        });
      }
      
      $("#spinning_wheel").hide();
    }

    function populateUtilityRatesTable(tenant, serviceInstanceUuid) {
      var currencyCode = $("#selectedcurrencytext").text();
      var returnHtml = null;
      $.ajax({
            type : "GET",
            async : false,
            url : "/portal/portal/subscription/utilityrates_table",
            data : {
              tenant:tenant,
              serviceInstanceUuid :serviceInstanceUuid,
              currencyCode:currencyCode
            },
            dataType : "html",
            cache : false,
            success : function(html) {
              $("#utilityrate_table").empty();
              $("#utilityrate_table").html(html);
              returnHtml = html
            }
          });
      return returnHtml;
    }

    function getGroupPossibilities() {
      if(resourceTypeSelection == SERVICE_RESOURCE_TYPE) {
        return true;
      }
      var currentComponents = getCurrentSelectedComponentsNames();
      var isAnySelectedComponentInOnlyOneGroup = false;
      var nonAmbiguousGroups = {};
      for (var currentComponent in currentComponents) {
        var thisComponentOccurence = 0;
        var applicableGroupId = null;
        for (var i = 0; i < groups.length; i++) {
          for(var j=0; j<groups[i].length; j++) {
            if(groups[i][j] === currentComponents[currentComponent]) {
              thisComponentOccurence++;
              applicableGroupId = i;
              break;
            }
          }
        }
        if(thisComponentOccurence == 1) {
          isAnySelectedComponentInOnlyOneGroup = true;
          nonAmbiguousGroups[applicableGroupId] = groups[applicableGroupId];
        }
      }
      
      var possibleGroups = [];
      for(data in nonAmbiguousGroups) {
        var group = nonAmbiguousGroups[data];
        possibleGroups.push(group);
      }
      
      if(possibleGroups.length == 0) {
        possibleGroups = groups;
      }
      
      return possibleGroups;
    }
    
    function getCurrentSelectedComponentsNames() {
      var currentComponents = [];
      for(var i=0; i < uniqueResourceComponents.length; i++) {
        var compInputVal = $("input[name='" + uniqueResourceComponents[i] + "']").val();
        if(isNotBlank(compInputVal) && compInputVal != dictionary.all) {
          currentComponents.push(uniqueResourceComponents[i]);
        }
      }
      return currentComponents;
    }
    
    function checkGroupFullFillment() {
           
      var currentComponents = getCurrentSelectedComponentsNames();
      
     
      
	  var possibleGroups = getGroupPossibilities();
	  if(possibleGroups == true || possibleGroups.length == 0 || (possibleGroups.length == 1 && possibleGroups[0].length == currentComponents.length)) {
		return true;
	  }
	  
      return false;
    }

    function refreshFiltersInputStep2(target) {
      var $inputField = $("input[name='" + target.data('filterId') + "']");
      $inputField.val(target.val());
      $inputField.data('valueDisplayName', target.data('valueDisplayName'));
      $inputField.data('fieldDisplayName', target.data('fieldDisplayName'));
      $inputField.change();
    }

    function refreshRCsInputStep2(target) {
      var $inputField = $("input[name='" + target.data('componentId') + "']");
      $inputField.val(target.val());
      $inputField.data('valueDisplayName', target.data('valueDisplayName'));
      $inputField.data('fieldDisplayName', target.data('fieldDisplayName'));
      $inputField.data('effectiveValue', target.data("effectiveValue"));
      $inputField.change();
    }

    function populateRCsAndFiltersStep2() {
      loadServiceFiltersStep2();
      if(!checkGroupFullFillment() || isReprovision) {
        refreshRCsListingStep2();
      }
      else {
        $("#customComponentSelectorContent").hide();
      }
    }

    function refreshRCsListingStep2(hidePreselectedFields) {
      isStep2Ready = false;
      if(isNotBlank($("#customComponentSelector").val())) {
        initializeComponentSelector(!isPayAsYouGoChosen, 2, selectedResourceComponentsForStep2);
      } else {
        loadServiceResourceComponentsStep2(hidePreselectedFields);
      }
      refreshBundlesListing();
      isStep2Ready = true;
    }


    function loadServiceFiltersStep2() {
      
      // Default Filters will be selected on step1 for subscribe bundles, so no need to repopulate in step2
      if(!isPayAsYouGoChosen) {
        return;
      }
      
      var $container = $("#filterSectionProvisionPage").empty();
      for(i=0; i<serviceFilterNames.length; i++) {
        
        var fcName = serviceFilterNames[i];
        
        var html = $("#componentSelectionContainer").clone();
        html.find("#componentSelectionHeader").text(l10dict[fcName + "-name"]);
        var $listBox = html.find("#component_values_container").empty();
        $listBox.attr("name", fcName);
        var serviceFilterValues = getValuesForFilter(fcName);
        if(serviceFilterValues == null || serviceFilterValues.length == 0) {
          continue;
        }
        for(j=0; j<serviceFilterValues.length; j++) {
          var serviceFilterValue = serviceFilterValues[j];
          
          if(isBlank(JSON.stringify(serviceFilterValue)) || JSON.stringify(serviceFilterValue) == "undefined") {
            continue;
          }

          var name = serviceFilterValue["name"];
          var value = serviceFilterValue["value"];
          
          
          var extras = getl10nComponentDetails("Filter", serviceFilterValue["attributes"], serviceFilterValue["displayAttributes"], i);

          var $newResourceValue = $("#component_value_template").clone();

          $newResourceValue.find("#name").text(name);
          $newResourceValue.find("#attributes_description").html(extras);
          $newResourceValue.find("#attributes_description").attr('title', $newResourceValue.find("#attributes_description").text());
          $newResourceValue.find(".radio").val(value);
          $newResourceValue.find(".radio").attr('id', value);
          $newResourceValue.find(".radio").data('fieldDisplayName', l10dict[fcName + "-name"]);
          $newResourceValue.find(".radio").data('valueDisplayName', name);
          $newResourceValue.find(".radio").data('filterId', fcName);
          $newResourceValue.find(".radio").data('text', name);
          $newResourceValue.find(".radio").prop('checked', false);
          $listBox.append($newResourceValue.show());
        }
        html.find('.radio').unbind("click").bind("click", function(event) {
          $("#spinning_wheel").show();
          refreshFiltersInputStep2($(event.target));
          refreshRCsListingStep2(false);
          $("#spinning_wheel").hide();
        });
        
        var currentInput = $("input[name='" + fcName + "']").val();
        var found = false;
        if(isNotBlank(currentInput)) {
          html.find("input:radio").each(function() {
            if($(this).val() == currentInput) {
              found = true;
              $(this).prop('checked', true);
              refreshFiltersInputStep2($(this));
            }
          });
        } 
        
        if(!found) {
          var target = html.find("input:radio:first");
          target.prop('checked', true);
          refreshFiltersInputStep2(target);
        }
        
        $container.append(html.show());
      }
    }

    function loadServiceResourceComponentsStep2(hidePreselectedFields) {
      
      var resourceSelectionHTML = $("#defaultRCsContent").clone();
      resourceSelectionHTML.empty();
      for(var i=0; i < selectedResourceComponentsForStep2.length; i++) {
        
        var rcName = selectedResourceComponentsForStep2[i];
        
        var currentInput = $("input[name='" + rcName + "']").val();
        if (isNotBlank(currentInput)) {
          if (isNotBlank(isReconfigure)) {
              if (reconfigurableMap[rcName] == "false"
                  || reconfigurableMap[rcName] == undefined) {
                continue;
              }
          } else if((hidePreselectedFields == null || hidePreselectedFields == true)){
            continue;
          }
        }
        
        var html = $("#componentSelectionContainer").clone();
        html.find("#componentSelectionHeader").text(l10dict[rcName + "-name"]);
        var $listBox = html.find("#component_values_container").empty();
        $listBox.attr("name", rcName);
        var data = getValuesForComponent(rcName);
        if(data == null || data.length == 0) {
          resetInputField($("input[name='" + rcName + "']"));
          continue;
        }
        for (var component in data) {
          
          if(isBlank(JSON.stringify(data[component])) || JSON.stringify(data[component]) == "undefined") {
            continue;
          }
          
          var effectiveValue = data[component]["value"];
          if(data[component]["parent"] != null){
            effectiveValue = data[component]["parent"]["value"];
          }
          
          var $newResourceValue = $("#component_value_template").clone();

          var extras = getl10nComponentDetails("ResourceComponent", data[component]["attributes"], data[component]["displayAttributes"], i);

          $newResourceValue.find("#name").text(data[component]["name"]);
          $newResourceValue.find("#attributes_description").html(extras);
          $newResourceValue.find("#attributes_description").attr('title', $newResourceValue.find("#attributes_description").text());
          $newResourceValue.find(".radio").val(data[component]["value"]);
          $newResourceValue.find(".radio").attr('id', data[component]["value"]);
          $newResourceValue.find(".radio").data('componentId', rcName);
          $newResourceValue.find(".radio").data('fieldDisplayName', l10dict[rcName + "-name"]);
          $newResourceValue.find(".radio").data('valueDisplayName', data[component]["name"]);
          $newResourceValue.find(".radio").data('effectiveValue', effectiveValue);
          $newResourceValue.find(".radio").prop('checked', false);
          $listBox.append($newResourceValue.show());
        }
        
        html.find('.radio').unbind("click").bind("click", function(event) {
          refreshRCsInputStep2($(event.target));
          refreshRCsListingStep2(false);
        });
        
        var currentInput = $("input[name='" + rcName + "']").val();
        
        var found = false;
        if(isNotBlank(currentInput)) {
          html.find("input:radio").each(function(){
            if($(this).val() == currentInput) {
              found = true;
              $(this).prop('checked', true);
              refreshRCsInputStep2($(this));
            }
          });
        }
        if(!found) {
          resetInputFieldByName(rcName);
        }
        
        if($listBox.find('.radio').length == 1) {
          refreshRCsInputStep2($listBox.find('.radio').prop('checked','true').first());
        }
        
        resourceSelectionHTML.append(html.show());
      }
      $("#componentsSectionProvisionPage").html(resourceSelectionHTML.show());
    }
    
    function resetInputFields() {
      for(var i=0; i < allConfigurationProperties.length; i++) {
        var $inputField = $("input[name='" + allConfigurationProperties[i] + "']");
        $inputField.val(null);
        $inputField.data('valueDisplayName', null);
        $inputField.data('fieldDisplayName', null);
        $inputField.data('effectiveValue', null);
      }
    }
    
    function resetInputField($inputField) {
        $inputField.val(null);
        $inputField.data('valueDisplayName', null);
        $inputField.data('fieldDisplayName', null);
        $inputField.data('effectiveValue', null);
    }
    
    function resetInputFieldByName(name) {
      var $inputField = $('input[name="'+name+'"]');
      $inputField.val(null);
      $inputField.data('valueDisplayName', null);
      $inputField.data('fieldDisplayName', null);
      $inputField.data('effectiveValue', null);
      $inputField.change();
  }

    function updateProperty(visibleField) {
      var $hiddenField = $("#conf_" + $(visibleField).attr('name'));
      $hiddenField.val($(visibleField).val());
      $hiddenField.change();
    }
}

function getValuesForComponent(componentTypeName, effComponentType) {
  var url;
  if(currentBundleTemplate != null) {
    var bundleRevisionObj = $(currentBundleTemplate).data("bundleRevisionObj");
  }
  if(bundleRevisionObj != null && bundleRevisionObj != undefined && !isPayAsYouGoChosen) {
    url = "/portal/portal/subscription/getResourceComponentsForBundle?bundleId="+bundleRevisionObj.pbid;
  } else {
    url = "/portal/portal/subscription/getResourceComponents";
  }
  if($("#viewChannelCatalog").val() == 'true'){
    url = url +"?viewCatalog=true"
  }
  var returnValues;
  $.ajax({
    type : "GET",
    async : false,
    url : url,
    data : {
      serviceInstanceUuid : $("#serviceInstanceUuid").val(),
      resourceType : $("#resourceType").val(),
      componentType : componentTypeName,
      effComponentType : effComponentType,
      filters : getSelectedFilterString(),
      contextString : getContextString(),
      tenant: $("#tenantParam").val()
    },
    dataType : "json",
    success : function(data) {
      returnValues = data;
    }
  });
  return returnValues;
}

function getValuesForFilter(filterType) {
  var url;
  if(currentBundleTemplate != null) {
    var bundleRevisionObj = $(currentBundleTemplate).data("bundleRevisionObj");
  }
  if(bundleRevisionObj != null && bundleRevisionObj != undefined && !isPayAsYouGoChosen) {
    url = "/portal/portal/subscription/getFilterComponentsForBundle?bundleId="+bundleRevisionObj.pbid;
  } else {
    url = "/portal/portal/subscription/getFilterComponents";
  }
  if($("#viewChannelCatalog").val() == 'true'){
    url = url +"?viewCatalog=true"
  }
  var returnValues;
  $.ajax({
    type : "GET",
    async : false,
    url : url,
    data : {
      serviceInstanceUuid : $("#serviceInstanceUuid").val(),
      filterType : filterType,
      tenant: $("#tenantParam").val()
    },
    dataType : "json",
    success : function(data) {
      returnValues = data;
    }, 
    error : function(error) {
      $("#spinning_wheel").hide();
      $("#catalog_content_area").hide();
    }
  });
  return returnValues;
}

function enableProvisionButton(enable, msg) {
  if (enable) {
    $("#launchvm_button").css({opacity: 1.0, visibility: "visible"});
    $("#launchvm_button").attr('disabled', false);
  } else {
    $("#launchvm_button").css({opacity: 0.5, visibility: "visible"});
    $("#launchvm_button").attr('disabled', true);
    
    if(msg != null && msg != "") {
      $("#launchvm_button").data('message', msg);
    }
  }
}

function getPrefixedLocation(selectedResourceType){
  var locationPrefix = "/portal/portal/subscription/createsubscription";
  if($("#anonymousBrowsing").val() == 'true'){
    locationPrefix = "/portal/portal/catalog/browse_catalog";
    var currencyCode = $("#selectedcurrencytext").html();
    var channelCode = $("#channelCode").val();
    locationPrefix = locationPrefix + "?&currencyCode="+currencyCode
    if(channelCode!=null && channelCode!='undefined'){
      locationPrefix = locationPrefix + "&channelCode="+channelCode;
    }
  }
  else if($("#viewChannelCatalog").val() == 'true'){
    locationPrefix = "/portal/portal/channel/catalog/view_catalog";
    var channelId = $("#channelId").val();
    var revision = $("#revision").val();
    var dateFormat = $("#dateFormat").val();
    var revisionDate = $("#revisionDate").val();
    var currencyCode = $("#selectedcurrencytext").html();
    locationPrefix = locationPrefix + "?viewCatalog=true&revision="+revision+"&channelParam="+channelId+"&currencyCode="+currencyCode+"&revisionDate="+revisionDate+"&dateFormat="+dateFormat
    locationPrefix = locationPrefix + "&tenant="+ $("#tenantParam").val()
  }else{
    locationPrefix = locationPrefix+ "?tenant="+ $("#tenantParam").val()
    if(isPayAsYouGoChosen) {
      locationPrefix += "&isPayAsYouGoChosen=true";
    }
  }
  if(selectedResourceType!=null ){
    locationPrefix = locationPrefix + "&resourceType="+ selectedResourceType;
  }
  locationPrefix = locationPrefix  + "&serviceInstanceUUID=" + $("#serviceInstanceUuid").val()
  
  return locationPrefix;
}

function getContextString() {
  var contextString = "";
  for(var i=0; i < uniqueResourceComponents.length; i++) {
    var compInputVal = $("input[name='" + uniqueResourceComponents[i] + "']").val();
    if (isNotBlank(compInputVal)) {
      if (contextString !== "") {
        contextString += ",";
      }
      contextString += uniqueResourceComponents[i] + "=" + compInputVal;
    }
  }
  return contextString;
}

function getContextStringOverrideEffective() {
var contextString = "";
for(var i=0; i < uniqueResourceComponents.length; i++) {
  var $inputField = $("input[name='" + uniqueResourceComponents[i] + "']");
  var compInputVal = $inputField.data('effectiveValue');
  if(isBlank(compInputVal)) {
    compInputVal = $inputField.val();
  }
  if (isNotBlank(compInputVal) && isNotBlank($inputField.val())) {
    if (contextString !== "") {
      contextString += ",";
    }
    contextString += uniqueResourceComponents[i] + "=" + compInputVal;
  }
}
return contextString;
}

function getSelectedFilterString() {
  var filterStr = "";
  for(var i=0; i < serviceFilterNames.length; i++) {
    var filterName = serviceFilterNames[i];
    var filterValue = $("input[name='" + serviceFilterNames[i] + "']").val();
    if (isNotBlank(filterValue)) {
      if (filterStr != "") {
        filterStr += ",";
      }
      filterStr += filterName + "=" + filterValue;
    }
  }
  return filterStr;
}