/* Copyright (C) 2013 Citrix Systems, Inc. All rights reserved */

$(document).ready(function() {
  var activeBundleCount = 0;
  var monthlyBundlesCount = 0 ;
  var oneTimeBundlesCount = 0 ; 
  var annualBundlesCount = 0 ;
  var quaterlyBundlesCount = 0;
  var visibleBundles = 0 ;
  var $selectedBundle;
  var newSubscriptionId = null;
  var isReconfigure = $("#isReconfigure").val();
  var customEditorPage = $("#customEditorTag").val();
  var productIdsWithEntitlements = {};
  
  
  $('.js_pay_as_you_go_action').live("click", function(e){
    e.preventDefault();
    
    var $button_no = $(this).attr("id").substr(10);
    $("#resourceTypeSelection").val($("#utility_resource_selection_"+$button_no).val());
    $("#spinning_wheel2").show();
    resetInputFields();
    subscribePAYG();
    $("#spinning_wheel2").hide();
  })
  
  function i18ChargeTypeText(chargeType) {
    switch (chargeType) {
      case "None":
        if(dictionary.hoursChargeType != null && dictionary.hoursChargeType != "undefined"){
          chargeType = dictionary.hoursChargeType;
        }
          break;
      case "Hours":
      if(dictionary.hoursChargeType != null && dictionary.hoursChargeType != "undefined"){
        chargeType = dictionary.hoursChargeType;
      }
      break;
    case "Usage Based":
      if(dictionary.usageBasedChargeType != null && dictionary.usageBasedChargeType != "undefined"){
        chargeType = dictionary.usageBasedChargeType;
      }
      break;
    case "One Time":
      if(dictionary.oneTimeChargeType != null && dictionary.oneTimeChargeType != "undefined"){
        chargeType = dictionary.oneTimeChargeType;
      }
      break;
    case "Annual":
      if(dictionary.annualChargeType != null && dictionary.annualChargeType != "undefined"){
        chargeType = dictionary.annualChargeType;
      }
      break;
    case "Hourly":
      if(dictionary.hourlyChargeType != null && dictionary.hourlyChargeType != "undefined"){
        chargeType = dictionary.hourlyChargeType;
      }
      break;
    case "Monthly":
      if(dictionary.monthlyChargeType != null && dictionary.monthlyChargeType != "undefined"){
        chargeType = dictionary.monthlyChargeType;
      }
      break;
    case "Quarterly":
      if(dictionary.quarterlyChargeType != null && dictionary.quarterlyChargeType != "undefined"){
        chargeType = dictionary.quarterlyChargeType;
      }
      break;
    }
    return chargeType;
  }
  
  function refreshRCsInputStep2(target) {
    var $inputField = $("input[name='" + target.data('componentId') + "']");
    $inputField.val(target.val());
    $inputField.data('valueDisplayName', target.data('valueDisplayName'));
    $inputField.data('fieldDisplayName', target.data('fieldDisplayName'));
    $inputField.data('effectiveValue', target.data("effectiveValue"));
  }
  
  function loadServiceResourceComponentsForBundle() {
    
    var $componentsForSelectedBundle = $("#componentsSectionProvisionPage").empty();
    
    for(var i=0; i < uniqueResourceComponents.length; i++) {
      var html = $("#componentSelectionContainer").clone();
      html.find("#componentSelectionHeader").text(l10dict[uniqueResourceComponents[i] + "-name"]);
      var $listBox = html.find("#component_values_container").empty();
      $listBox.attr("name", uniqueResourceComponents[i]);
      var data = getValuesForComponent(uniqueResourceComponents[i]);
      if(data == null || data.length == 0) {
        continue;
      }
      
      var $newResourceValue = $("#component_value_template").clone();

      $newResourceValue.find("#name").text(dictionary.label_None);
      $newResourceValue.find(".radio").val(null);
      $newResourceValue.find(".radio").attr('id', dictionary.label_None);
      $newResourceValue.find(".radio").data('componentId', uniqueResourceComponents[i]);
      $newResourceValue.find(".radio").data('fieldDisplayName', null);
      $newResourceValue.find(".radio").data('valueDisplayName', null);
      $newResourceValue.find(".radio").data('effectiveValue', null);
      $listBox.append($newResourceValue.show());
      
      for (var component in data) {
        var effectiveValue = data[component]["value"];
        if(data[component]["parent"] != null){
          effectiveValue = data[component]["parent"]["value"];
        }
        
        var $newResourceValue = $("#component_value_template").clone();

        $newResourceValue.find("#name").text(data[component]["name"]);
        $newResourceValue.find(".radio").val(data[component]["value"]);
        $newResourceValue.find(".radio").attr('id', data[component]["value"]);
        $newResourceValue.find(".radio").data('componentId', uniqueResourceComponents[i]);
        $newResourceValue.find(".radio").data('fieldDisplayName', l10dict[uniqueResourceComponents[i] + "-name"]);
        $newResourceValue.find(".radio").data('valueDisplayName', data[component]["name"]);
        $newResourceValue.find(".radio").data('effectiveValue', effectiveValue);
        $newResourceValue.find(".radio").prop('checked', false);
        $listBox.append($newResourceValue.show());
      }
      
      html.find('.radio').unbind("click").bind("click", function(event) {
        refreshRCsInputStep2($(event.target));
        refreshRCsListingStep2();
      });
      
      var currentInput = $("input[name='" + uniqueResourceComponents[i] + "']").val();
      
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
        target = html.find("input:radio:first");
        target.prop('checked', true);
        refreshRCsInputStep2(target);
      }
      $componentsForSelectedBundle.append(html.show());
    }
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
        resourceType : $("#resourceTypeSelection").val(),
        contextString : getContextStringOverrideEffective(),
        filters : getSelectedFilterString()
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

  function populateRCsAndFiltersStep2() {
    loadFiltersForPayAsYouGo();
    refreshRCsListingStep2();
  }
  
  function refreshFiltersInputStep2(target) {
    var $inputField = $("input[name='" + target.data('filterId') + "']");
    $inputField.val(target.val());
    $inputField.data('valueDisplayName', target.text());
    $inputField.data('fieldDisplayName', target.data('fieldDisplayName'));
    $inputField.change();
  }
  
  function refreshRCsListingStep2() {
    if(isNotBlank($("#customComponentSelector").val())) {
      initializeComponentSelector(!isPayAsYouGoChosen, 2);
    } else {
      loadServiceResourceComponentsForBundle();
    }
    $("#get-pricing").click();
  }
  
  function loadFiltersForPayAsYouGo() {
    var $container = $("#filterSectionProvisionPage").empty();
    for(i=0; i<serviceFilterNames.length; i++) {
      
      var html = $("#componentSelectionContainer").clone();
      html.find("#componentSelectionHeader").text(l10dict[serviceFilterNames[i] + "-name"]);
      var $listBox = html.find("#component_values_container").empty();
      $listBox.attr("name", serviceFilterNames[i]);
      var serviceFilterValues = serviceFilters[i];
      if(serviceFilterValues == null || serviceFilterValues.length == 0) {
        continue;
      }
      for(j=0; j<serviceFilterValues.length; j++) {
        var serviceFilterValue = serviceFilterValues[j];
        var name = serviceFilterValue.split("=")[0];
        var value = serviceFilterValue.split("=")[1];
        var $newResourceValue = $("#component_value_template").clone();

        $newResourceValue.find("#name").text(name);
        $newResourceValue.find(".radio").val(value);
        $newResourceValue.find(".radio").attr('id', value);
        $newResourceValue.find(".radio").text(name);
        $newResourceValue.find(".radio").data('fieldDisplayName', l10dict[serviceFilterNames[i] + "-name"]);
        $newResourceValue.find(".radio").data('filterId', serviceFilterNames[i]);
        $newResourceValue.find(".radio").data('text', name);
        $newResourceValue.find(".radio").prop('checked', false);
        $listBox.append($newResourceValue.show());
      }
      html.find('.radio').unbind("click").bind("click", function(event) {
        $("#spinning_wheel").show();
        refreshFiltersInputStep2($(event.target));
        refreshRCsListingStep2();
        $("#spinning_wheel").hide();
      });
      
      var currentInput = $("input[name='" + serviceFilterNames[i] + "']").val();
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
  
  function subscribePAYG() {
    
    $("#SECTION_1").hide();
    $("#no_screen_wrapper_div").hide();
    $("#msg_RHS_entitlements").hide();
    $("#subscribe_payg_choice").hide();
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
    for(var i=0; i < productProperties.length; i++) {
      $("input[name='" + productProperties[i] + "']").unbind("change").bind("change", function() {refreshYourSelectionSummary();});
    }
    
    populateRCsAndFiltersStep2();
    
    if(isNotBlank(customEditorPage)) {
      initializeEditor();
    }
    
    $("#SECTION_3").removeClass("pull-right").addClass("pull-left");
    $("#SECTION_3").show();
    $("#componentsForSelectedBundle").show();
    $("#componentsSectionProvisionPage").show();
    $("#headerSectionProvisionPage").show();
    $("#configure_subscribe").show();
    $("#SECTION_4").show();
    $("#filter_options").show();
    $("#filterSectionProvisionPage").show();
    
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
        resourceType : $("#resourceTypeSelection").val(),
        contextString : "",
        filters : "",
        listAll : "true"
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
            if (entitlement.includedUnits == -1) {
              usage = dictionary.unlimited;
            }
            var entitlementText = "<strong>" + entitlement.product.name + "</strong><br />"
            entitlementText += dictionary.msg_no_extra_charge_upto + " ";
            entitlementText += usage + "&nbsp;" + i18nUomText(entitlement.product.uom);
            
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
  
  function actionSubscribe(event, current){
    currentPage = 2;
    $("#spinning_wheel").show();
    $("#customComponentSelectorContent").hide();
    $("#SECTION_3").removeClass("pull-right").addClass("pull-left");
    $("#SECTION_2").hide();
    $("#SECTION_2").css({top: '289px'});
    $("#narrow-your-search").hide();
    $("#bundle_with_selection_summary_div").hide();
    $("#subscribe_payg_choice").hide();
    $("#pricing").hide();
    $("#filterSectionProvisionPage").hide();
    currentBundleTemplate = current;
    productIdsWithEntitlements = {};
    
    bundleObj = $(current).data("jsonObj");
    subscriptionId = $(current).data("subscriptionId");
    bundleObj_id = bundleObj.id;
    $selectedBundle = $(current);
    populateEntitlements();
    
    if($("#resourceTypeSelection").val() != SERVICE_RESOURCE_TYPE) {
      populateRCsAndFiltersStep2();
      if(isNotBlank(customEditorPage)) {
        initializeEditor();
      }
      for(var i=0; i < productProperties.length; i++) {
        $("input[name='" + productProperties[i] + "']").unbind("change").bind("change", function() {refreshYourSelectionSummary();});
      }
    }
    
    refreshYourSelectionSummary();

    var selected_bundle_ribbon_class = $selectedBundle.find("#entitlmentsribbon").attr('class');
    $('#bundle_entitlements_ribbon').removeClass().addClass(selected_bundle_ribbon_class);
    $('#bundle_entitlements_ribbon').html("");
    $('#bundle_entitlements_ribbon').html($selectedBundle.find("#entitlmentsribbon").html());
    
    if ($selectedBundle.data('link_added')) {
      $('#included_usage').find("[id^=more_link]").attr('id', 'include_usage_more_link').parent().css('background','none');
      $("#include_usage_more_link").unbind("click").bind("click", function(event){
        actionMoreLink(event, $selectedBundle, 'from_subscribe');
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
          target = $("input[name='" + productProperties[i] + "']");
          target.prop('disabled', true);
        }
        $("#accept_checkbox").prop('checked', true);
        $("#provision_accept").prop('checked', true);
        $("#accept_checkbox").prop('disabled', true);
        $("#provision_accept").prop('disabled', true);
      }
    }
    
    if($("#resourceTypeSelection").val() === SERVICE_RESOURCE_TYPE) {
      $("#msg_RHS_generated_usage").hide();
      $("#servicebundle-info-description").text(bundleObj.name);
      if(bundleObj.description) {
        $("#servicebundle-info-description").append(" : " + bundleObj.description);
      }
      $("#provision_accept").prop('checked', false);
      $("#provision_acceptBoxAndText").hide();
    }
    
    update_price_summary(current);
    
    subscriptionId = $("#subscriptionId").val();
    isReconfigure = $("#isReconfigure").val();
    
    if(isNotBlank($("#isReconfigure").val())) {
      $("#componentsForSelectedBundle").find('input:radio').each(function() {
        var componentId = $(this).data('componentId');
        if(isBlank(componentId) || reconfigurableMap[componentId] == "false" || reconfigurableMap[componentId] == undefined) {
          $(this).prop('disabled', true);
        }
      });
    }
    
    $("#headerSectionProvisionPage").show();
    $("#SECTION_4").show();
    
    $("#configure_subscribe").show();
    $("#filter_options").show();
    $("#SECTION_3").show();
    
    $("#componentsSectionProvisionPage").show();
    $("#componentsForSelectedBundle").show();
    $("#filterSectionProvisionPage").show();
    $("#customComponentSelectorContent").show();
    $("#spinning_wheel").hide();
  }
  
  $(".utility_rates_link").unbind("click").bind("click", function(event) {        
    var utility_url = "/portal/portal/subscription/utilityrates_lightbox?tenant="+tenantParam;
    if(serviceInstaceUuid != null && $("#resourceTypeSelection").val() != null){
      var filterString = getSelectedFilterString();
      var contextString = getContextString();
      utility_url = utility_url+ "&serviceInstanceUuid="+serviceInstaceUuid+"&resourceTypeName="+$("#resourceTypeSelection").val()+"&contextString="+contextString+"&filters="+filterString;
    }
    viewUtilitRates(tenantParam,"utilityrates_lightbox",utility_url);
  });  
    
  var $bundleContainer = $("#bundle_container");
  var $bundleTemplate = $bundleContainer.find("#bundle_template");
  var bundleObj_id;
  var subscriptionId;
  
  $("#back_to_catalog").click(function(event) {
    currentPage = 1;
    $("#spinning_wheel").show();
    currentBundleTemplate = null;
    
    $("#filterSectionProvisionPage").hide();
    $("#configure_subscribe").hide();
    $("#componentsSectionProvisionPage").hide();
    $("#componentsForSelectedBundle").hide();
    $("#SECTION_4").hide();
    $("#headerSectionProvisionPage").hide();
    if($("#resourceTypeSelection").val() === SERVICE_RESOURCE_TYPE) {
      $("#componentselector-minified").hide();
      $("#componentselector").show();
    }
    if($("#resourceTypeSelection").val() != SERVICE_RESOURCE_TYPE) {
      loadFiltersAndRCs();
    }
    $("#bundle_with_selection_summary_div").show();
    $("#subscribe_payg_choice").show();
    $("#bundle_container").show();
    $("#pricing").show();
    $("#filter").show();
    $("#narrow-your-search").show();
    if($("#filter_options").css("display")!="none"){
    	$("#narrow_your_search_link").removeClass("expand").addClass("collapse");
    }
    $("#SECTION_3").removeClass("pull-left").addClass("pull-right");
    $("#SECTION_3").show();
    $("#SECTION_2").show();
    $("#componentselector").show();
    if(isPayAsYouGoChosen) {
      $('#tab_utility_view a').click();
    }
    $("#spinning_wheel").hide();
  });
  
  function update_price_summary(current) {   
    
    var bundle_activation_charges = parseFloat($(current).data("activationCharges"));
    var bundle_recurring_charges = parseFloat($(current).data("recurringCharges"));
    
    var bundle_price_before_tax = bundle_recurring_charges + bundle_activation_charges;
    var subscription_total_amount_before_tax = parseFloat(bundle_price_before_tax);
    var num = new Number(subscription_total_amount_before_tax);
    var tax_amount = Number(getTaxableAmount(num));
    var chargeWithTax = num + tax_amount;
    var subscription_total_amount_after_tax = roundNumber(chargeWithTax,$("#minFractionDigits").val());  
    var new_sub_total = (subscription_total_amount_before_tax);
    
    
    
    $('#pricing_reccurence_frequency').text($(current).data("pricingFilterDisplay"));
    if($(current).data("pricingFilterDisplay") == "None") {
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
  
  function highlightCurrentPricingFilter(current) {
    $("#NONE").removeClass("nonactive");
    $("#ALL").removeClass("nonactive");
    $("#MONTHLY").removeClass("nonactive");
    $("#QUARTERLY").removeClass("nonactive");
    $("#ANNUAL").removeClass("nonactive");
    $("#MONTHLY").removeClass("active");
    $("#QUARTERLY").removeClass("active");
    $("#ANNUAL").removeClass("active");
    $("#NONE").removeClass("active");
    $("#ALL").removeClass("active");
    
    $(current).addClass("active");
  }    
  
  
    function actionPricingFilter(event, current) {
      visibleBundles = 0 ; 
      $bundleContainer.empty();
      highlightCurrentPricingFilter(current);
      $("#pricingFilter").val($(current).attr('id'));
      refreshActive($(current).attr("id"));
      refreshBundle($(current).attr("id"));
    }
    
    $("#ACTIVE").bind("click", function(event){
      actionPricingFilter(event, $("#ACTIVE"));
    });
    $("#NONE").bind("click", function(event){
      actionPricingFilter(event, $("#NONE"));
    });
    $("#MONTHLY").bind("click", function(event){
      actionPricingFilter(event, $("#MONTHLY"));
    });
    $("#QUARTERLY").bind("click", function(event){
      actionPricingFilter(event, $("#QUARTERLY"));
    });
    $("#ALL").bind("click", function(event){
      actionPricingFilter(event, $("#ALL"));
    });
    $("#ANNUAL").bind("click", function(event){
      actionPricingFilter(event, $("#ANNUAL"));
    });
    
   var tenantParam= $("#tenantParam").val();
   var serviceInstaceUuid= $("#serviceInstanceUuid").val();
   
   function showBundleRevisions(pricingFilter, bundleRevisions) {
     if ($("#showServiceBundles").val() == "true") {
       if (bundleRevisions != null && bundleRevisions.length > 0) {
         for (var p = 0; p < bundleRevisions.length; p++) {
           var bundleRevision = bundleRevisions[p];
           switch (bundleRevision.productBundle.rateCard.chargeType.name) {
             case "NONE":
               oneTimeBundlesCount = oneTimeBundlesCount + 1;
               break;
             case "MONTHLY":
               monthlyBundlesCount = monthlyBundlesCount + 1;
               break;
             case "QUARTERLY":
               quaterlyBundlesCount = quaterlyBundlesCount + 1;
               break;
             case "ANNUAL":
               annualBundlesCount = annualBundlesCount + 1;
               break;
           }
         }
         if (pricingFilter == null) {
           pricingFilter = 'ALL';
           highlightCurrentPricingFilter("#" + pricingFilter);
         }
         var titleShown = false;
         for (var p = 0; p < bundleRevisions.length; p++) {
           var bundleRevision = bundleRevisions[p];
           var bundle = bundleRevision.productBundle;
           if (pricingFilter != 'ALL' && bundleRevision.productBundle.rateCard.chargeType.name != pricingFilter) continue;
           if(!titleShown) {
             $bundleContainer.append("<div class='catalog_current_header'>" + dictionary.view_bundles + "</div>");
             titleShown = true;
           }
           if (visibleBundles == 0) {
             visibleBundles = 1;
           }
           var entitlements = bundleRevision.entitlements;
           var entitlementHtml = "";
           var totalentitlementhtml = "";
           var link_added = false;
           if (entitlements != null && entitlements.length > 0) {
             for (var a = 0; a < entitlements.length; a++) {
               var entitlement = entitlements[a];
               var usage = entitlement.includedUnits;
               if (entitlement.includedUnits == -1) {
                 usage = dictionary.unlimited;
               }
               if (a >= 3) {
                 if (link_added == false) {
                   entitlementHtml = entitlementHtml + '<li>' + '<a id="more_link' + bundle.id + '" value="' + dictionary.more + '" href="javascript:void(0);">' + dictionary.more + '</a>' + '</li>';
                   link_added = true;
                 }
                 totalentitlementhtml = totalentitlementhtml + '<li style="color: #000"><span class="navicon ' + entitlement.product.name + '">' + '</span><span class="text" style="margin-top: 14px;"><strong>' + usage + "&nbsp;" + i18nUomText(entitlement.product.uom) + "</strong>&nbsp;" + dictionary.of + "&nbsp;" + entitlement.product.name + "</span></li>";
                 continue;
               }
               entitlementHtml = entitlementHtml + '<li style="color: #000"><span class="navicon ' + entitlement.product.name + '">' + '</span><span class="text" style="margin-top: 14px;"><strong>' + usage + "&nbsp;" + i18nUomText(entitlement.product.uom) + "</strong>&nbsp;" + dictionary.of + "&nbsp;" + entitlement.product.name + "</span></li>";
               totalentitlementhtml = entitlementHtml;
             }
           }
           if (bundle.description != null) {
             if (!link_added) {
               entitlementHtml = entitlementHtml + '<li>' + '<a id="more_link' + bundle.id + '" value="' + dictionary.more + '" href="javascript:void(0);">' + dictionary.more + '</a>' + '</li>';
               link_added = true;
             }
           }
           var $newBundle = $bundleTemplate.clone(true);
           $newBundle.find("#bundle_name").text(bundle.name);
           $newBundle.find("#bundle_description").text(bundle.description);
           $newBundle.find("#bundleDescription").html(bundle.description);
           $newBundle.find("#entitlmentsribbon").addClass("col1");
           $newBundle.find("#totalentitlmentsribbon").addClass("col1");
           $newBundle.find("#entitlements").html(entitlementHtml);
           $newBundle.find("#totalentitlments").html(totalentitlementhtml);
           $newBundle.attr("id", "bundle_" + bundle.id);
           $newBundle.data("jsonObj", bundle);
           $newBundle.data("bundleRevisionObj", bundleRevision);
           $newBundle.data("link_added", link_added);
           $newBundle.find("#name").text(bundle.name);
           $newBundle.find("#currencySign").text($("#selectedCurrencySign").val());
           $newBundle.find("#recurrenceType").text("/" + i18ChargeTypeText(bundle.rateCard.chargeType.displayName));
           $newBundle.data("pricingFilterDisplay", bundle.rateCard.chargeType.displayName);
           $newBundle.find("#more_link" + bundle.id).attr("id", "more_link" + "bundle_" + bundle.id);
           $newBundle.find("#totalentitlmentsdiv").attr("id", "totalentitlmentsdiv" + "bundle_" + bundle.id);
           var charges = bundleRevision.rateCardCharges;
           var activationCharge = new Number(0);
           var recurringCharge = new Number(0);
           for (var n = 0; n < charges.length; n++) {
             var rateCardPrice = charges[n];
             if (rateCardPrice == null) continue;
             if (rateCardPrice.rateCardComponent.isRecurring == true) {
               recurringCharge = recurringCharge + rateCardPrice.price;
             } else {
               activationCharge = activationCharge + rateCardPrice.price;
             }
           }
           $newBundle.find("#activationCharges").text("+ " + $("#selectedCurrencySign").val() + activationCharge + " " + dictionary.activationCharges);
           if (recurringCharge == 0) {
             $newBundle.find("#recurringCharges").text("0");
           } else {
             $newBundle.find("#recurringCharges").text(recurringCharge);
           }
           $newBundle.data('recurringCharges', recurringCharge);
           $newBundle.data('activationCharges', activationCharge);
           if (p == 0) {
             $selectedBundle = $newBundle;
           }
           $newBundle.bind("click", function (event) {
             $selectedBundle = $(this).closest("div[id^='bundle_']");
           });
           $newBundle.find("#more_link" + "bundle_" + bundle.id).bind("click", function (event) {
             actionMoreLink(event, $(this));
           });
           $newBundle.find("#subscribe").attr("id", "subscribe" + bundle.id);
           $newBundle.find("#subscribe" + bundle.id).bind("click", function (event) {
             redirect_to_login_page();
           });
           $bundleContainer.append($newBundle.show());
         }
         titleShown = false;
       }
     } else {
       if (bundleRevisions != null && bundleRevisions.length > 0) {
         
         for (var p = 0; p < bundleRevisions.length; p++) {
           var bundleRevision = bundleRevisions[p];
           var bundle = bundleRevision.productBundle;
           switch (bundle.rateCard.chargeType.name) {
             case "NONE":
               oneTimeBundlesCount = oneTimeBundlesCount + 1;
               break;
             case "MONTHLY":
               monthlyBundlesCount = monthlyBundlesCount + 1;
               break;
             case "QUARTERLY":
               quaterlyBundlesCount = quaterlyBundlesCount + 1;
               break;
             case "ANNUAL":
               annualBundlesCount = annualBundlesCount + 1;
               break;
           }
         }
         if (pricingFilter == null) {
           pricingFilter = 'ALL';
           highlightCurrentPricingFilter("#" + pricingFilter);
         }
         var titleShown = false;
         for (var p = 0; p < bundleRevisions.length; p++) {
           var bundleRevision = bundleRevisions[p];
           if (pricingFilter != 'ALL' && bundleRevision.productBundle.rateCard.chargeType.name != pricingFilter) continue;
           if(!titleShown) {
             $bundleContainer.append("<div class='catalog_current_header'>" + dictionary.view_bundles + "</div>");
             titleShown = true;
           }
           if (visibleBundles == 0) {
             visibleBundles = 1;
           }
           var $newBundle = buildBundleRow(p, bundleRevision, null, null, null, true);
           $bundleContainer.append($newBundle.show());
           hideConfigureAndSubscribe();
         }
         titleShown = false;
       }
     }
     $("#bundle-hourly-count").text("(" + oneTimeBundlesCount + ")");
     $("#bundle-monthly-count").text("(" + monthlyBundlesCount + ")");
     $("#bundle-quaterly-count").text("(" + quaterlyBundlesCount + ")");
     $("#bundle-annual-count").text("(" + annualBundlesCount + ")");
   }
    
   function getActionUrlForListBundle(){
     var actionUrl;
     var tenantParam = $("#tenantParam").val();
     var serviceInstaceUuid = $("#serviceInstanceUuid").val();
     var resourceType = $("#resourceType").val();
     var SERVICE_RESOURCE_TYPE = "__SERVICE__";
     if (resourceType === SERVICE_RESOURCE_TYPE) {
       actionUrl = "/portal/portal/productBundles/list.json?&tenant="
           + tenantParam + "&serviceInstaceUuid=" + serviceInstaceUuid;
     } else {
       actionUrl = "/portal/portal/productBundles/list.json?&tenant="
           + tenantParam + "&serviceInstaceUuid=" + serviceInstaceUuid
           + "&resourceType=" + resourceType + "&filters="
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
       actionUrl = actionUrl + "&currencyCode="+currencyCode;
     }
     return actionUrl;
   }
   
  function refreshBundle(pricingFilter) {   
    $("#no_screen_wrapper_div").hide();
    $("#spinning_wheel").show();
    monthlyBundlesCount = 0;
    oneTimeBundlesCount = 0;
    annualBundlesCount = 0;
    quaterlyBundlesCount = 0;
    var actionUrl = getActionUrlForListBundle();
    if(pricingFilter == null) {
      pricingFilter = $("#pricingFilter").val();
    }
    $.ajax({
      type : "GET",
      url : actionUrl,  
      async: false,
      dataType : "json",
      success : function(bundleRevisions) {   
        showBundleRevisions(pricingFilter, bundleRevisions);
      }
    }); 
    $("#spinning_wheel").hide();
    if(visibleBundles == 0 ){
      $("#no_screen_wrapper_div").show();
    }
  }  
  
  function buildBundleRow(index, bundleRevision, buttonLabel, buttonClass, subscriptionId, isCompatible){
    var bundle = bundleRevision.productBundle;
    var entitlements = bundleRevision.entitlements;
    var entitlementHtml = "";
    var totalentitlementhtml = "";
    var link_added = false;
    if (entitlements != null && entitlements.length > 0) {
      for (var a = 0; a < entitlements.length; a++) {
        var entitlement = entitlements[a];
        var usage = entitlement.includedUnits;
        if (entitlement.includedUnits == -1) {
          usage = dictionary.unlimited;
        }
        if (a >= 3) {
          if (link_added == false) {
            entitlementHtml = entitlementHtml + '<li>' + '<a id="more_link' + bundle.id + '" value="' + dictionary.more + '" href="javascript:void(0);">' + dictionary.more + '</a>' + '</li>';
            link_added = true;
          }
          totalentitlementhtml = totalentitlementhtml + '<li style="color: #000"><span class="navicon ' + entitlement.product.name + '">' + '</span><span class="text" style="margin-top: 14px;"><strong>' + usage + "&nbsp;" + i18nUomText(entitlement.product.uom) + "</strong>&nbsp;" + dictionary.of + "&nbsp;" + entitlement.product.name + "</span></li>";
          continue;
        }
        entitlementHtml = entitlementHtml + '<li style="color: #000"><span class="navicon ' + entitlement.product.name + '">' + '</span><span class="text" style="margin-top: 14px;"><strong>' + usage + "&nbsp;" + i18nUomText(entitlement.product.uom) + "</strong>&nbsp;" + dictionary.of + "&nbsp;" + entitlement.product.name + "</span></li>";
        totalentitlementhtml = entitlementHtml;
      }
    }
    if (bundle.description != null) {
      if (!link_added) {
        entitlementHtml = entitlementHtml + '<li>' + '<a id="more_link' + bundle.id + '" value="' + dictionary.more + '" href="javascript:void(0);">' + dictionary.more + '</a>' + '</li>';
        link_added = true;
      }
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
    $newBundle.find("#totalentitlments").html(totalentitlementhtml);
    $newBundle.attr("id", "bundle_" + bundle.id + "vmproduct_" + bundle.id);
    $newBundle.data("jsonObj", bundle);
    $newBundle.data("bundleRevisionObj", bundleRevision);
    if(subscriptionId!=null){
      $newBundle.data("subscriptionId", subscriptionId);
    }
    $newBundle.data("link_added", link_added);
    $newBundle.find("#name").text(bundle.name);
    $newBundle.find("#name").attr("title", bundle.name);
    $newBundle.find("#currencySign").text($("#selectedCurrencySign").val());
    $newBundle.find("#recurrenceType").text("/" + i18ChargeTypeText(bundle.rateCard.chargeType.displayName));
    $newBundle.data("pricingFilterDisplay", bundle.rateCard.chargeType.displayName);
    $newBundle.find("#more_link" + bundle.id).attr("id", "more_link" + "bundle_" + bundle.id + "vmproduct_" + bundle.id);
    $newBundle.find("#totalentitlmentsdiv").attr("id", "totalentitlmentsdiv" + "bundle_" + bundle.id + "vmproduct_" + bundle.id);
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
    if (recurringCharge == 0) {
      if (bundle.rateCard.chargeType.name == 'NONE') {
        $newBundle.find("#recurringCharges").text(0);
        recurringCharge = 0;
      } else {
        $newBundle.find("#recurringCharges").text("0");
      }
    } else {
      $newBundle.find("#recurringCharges").text(recurringCharge);
    }
    $newBundle.data('recurringCharges', recurringCharge);
    $newBundle.data('activationCharges', activationCharge);
    if (index == 0) {
      $selectedBundle = $newBundle;
    }
    $newBundle.find("#more_link" + "bundle_" + bundle.id + "vmproduct_" + bundle.id).bind("click", function (event) {
      actionMoreLink(event, $(this));
    });
    $newBundle.find("#subscribe").attr("id", "subscribe" + bundle.id);
    if(buttonLabel!=null){
      $newBundle.find("#subscribe" + bundle.id).text(buttonLabel);
    }
    if(isCompatible){
      $newBundle.find("#subscribe" + bundle.id).bind("click", function (event) {
        actionSubscribe(event, $(this).closest("div[id^='bundle_']"));
      });
    }
    if(buttonClass){
      $newBundle.find(".subscribebutton").removeClass("subscribebutton").addClass(buttonClass); 
    }
    return $newBundle;
  }
  
  function showActive(pricingFilter, bundleRevisions, subscriptionBundleInfo) {
    if(subscriptionBundleInfo != null) {
      var bundleRevision = subscriptionBundleInfo.bundleRevision;
      if (pricingFilter == 'ALL' || bundleRevision.productBundle.rateCard.chargeType.name == pricingFilter)  {
        activeBundleCount = 1;
        $bundleContainer.append("<div class='catalog_current_header'>" + l10dict['currentBundle'] + "</div>");
        if (visibleBundles == 0) {
          visibleBundles = 1;
        }
        
        var isCompatible = subscriptionBundleInfo.isCompatible;
        var $newBundle;
        if(isCompatible){
          $newBundle = buildBundleRow(0, bundleRevision, l10dict['Provision'], null, subscriptionBundleInfo.subscriptionId, true);
        }
        else{
          $newBundle = buildBundleRow(0, bundleRevision, l10dict['Incompatible'], "incompatible", null, false);
        }
        $bundleContainer.append($newBundle.show());
      }
    }
    if (!$.isEmptyObject(bundleRevisions)) {
      var titleShown = false;
      for(var subscriptionId in bundleRevisions) {
        activeBundleCount = activeBundleCount + 1;
        if (pricingFilter != 'ALL' && bundleRevisions[subscriptionId].productBundle.rateCard.chargeType.name != pricingFilter)  {
          continue;
        }
        if(!titleShown) {
          titleShown = true;
          $bundleContainer.append("<div class='catalog_current_header'>"+ l10dict['subscriptionBundles'] +"</div>");
        }
        if (visibleBundles == 0) {
          visibleBundles = 1;
        }
        var $newBundle = buildBundleRow(activeBundleCount, bundleRevisions[subscriptionId],  l10dict['Provision'], null, subscriptionId, true);
        $bundleContainer.append($newBundle.show());
      }
    }
  }
  
  function refreshActive(pricingFilter) {
    
    if(pricingFilter == null) {
      pricingFilter = $("#pricingFilter").val();
      if(pricingFilter == null) {
        pricingFilter = 'ALL';
      }
    }
    
    
    if($("#resourceTypeSelection").val() !== SERVICE_RESOURCE_TYPE) {
      $("#no_screen_wrapper_div").hide();
      $("#spinning_wheel").show();
      if($("#viewChannelCatalog").val() != 'true' && $("#anonymousBrowsing").val() != 'true' ){
      var subscriptionBundleRevisions = $.ajax({
        type: "GET",
        url:  "/portal/portal/productBundles/listValidSubscriptions.json?&tenant="+tenantParam+"&serviceInstaceUuid="+serviceInstaceUuid+"&resourceType="+$("#resourceTypeSelection").val() +"&filters="+getSelectedFilterString()+"&context="+getContextString(),
        async: false,
        dataType: "json"
      });
      }
      if($("#subscriptionId").length>0){
        var subscriptionBundle = $.ajax({
          type: "GET",
          url: "/portal/portal/productBundles/getBundleBySubscription.json?&tenant="+tenantParam+"&serviceInstaceUuid="+serviceInstaceUuid+"&resourceType="+$("#resourceTypeSelection").val() +"&filters="+getSelectedFilterString()+"&context="+ getContextStringOverrideEffective() + "&subscriptionId="+$("#subscriptionId").val(),
          async: false,
          dataType: "json"
        });
        $.when(subscriptionBundleRevisions, subscriptionBundle).done(function (subscriptionBundleRevisionsJson, subscriptionBundleJson) {
          showActive(pricingFilter, subscriptionBundleRevisionsJson[0], subscriptionBundleJson[0]);
        });
      }
      else{
        $.when(subscriptionBundleRevisions).done(function (subscriptionBundleRevisionsJson) {
          showActive(pricingFilter, subscriptionBundleRevisionsJson);
        });
      }
      $("#spinning_wheel").hide();
      if (visibleBundles == 0) {
        $("#no_screen_wrapper_div").show();
      }
    }
  } 
    
  $("#utilityrates_lightbox").find("#close_button").bind("click", function(event) {
    $("#overlay_black").hide(); 
    $("#utilityrates_lightbox").hide();       
    return false;
  });
  
  function actionMoreLink(event, current, from_page){
    var dialogId = "totalentitlmentsdiv";
    var targetId;
    if (from_page=='from_subscribe')
      targetId = $(current).attr("id");
    else
      targetId = $(current).attr("id").substr(9);
    
    dialogId = dialogId + targetId;
    initDialog(dialogId, 380);
    var $thisDialog = $("#" + dialogId);
    $thisDialog.dialog('option', 'buttons', {
      "Cancel": function () {
        $(this).dialog("close");
      }
    });
    dialogButtonsLocalizer($thisDialog, {
      'Cancel': g_dictionary.dialogCancel
    });
    $thisDialog.dialog("open");
  }

  var savedProductCustomProps = $('#subConfigurationData').val();
  if(savedProductCustomProps != null && savedProductCustomProps.trim() != ""){
    //console.log('Values.....',savedProductCustomProps);
    var propObject = $.parseJSON(savedProductCustomProps);
    //console.log('Values.....',propObject);
    for(var i=0; i < productProperties.length; i++) {
      target = $("input[name='" + productProperties[i] + "']");
      var eleName = target.attr("name");
      target.attr('value',propObject[eleName]);
    }
     var productCustomEditorUrl = $('#customEditorTag').val();
          if(productCustomEditorUrl != null && productCustomEditorUrl.trim() != ""){
        initializeIframeContents(propObject);
          }

  }

  function initializeIframeContents(propObject){
    var targetedIframe = window.frames['ifid1'];
    for(var i=0; i < productProperties.length; i++) {
      target = $("input[name='" + productProperties[i] + "']");
      
      var eleName = target.attr("name");
      if(propObject[eleName] == null || propObject[eleName] == undefined){
        //console.log('cannot assign..',eleName);
      } else{
            targetedIframe.document.getElementById(eleName).value=propObject[eleName];
      }
    }
  }

  function populateIframeContents(){
    var productCustomEditorUrl = $('#customEditorTag').val();
      if(productCustomEditorUrl != null && productCustomEditorUrl.trim() != ""){
      var targetedIframe = window.frames['ifid1'];
      for(var i=0; i < productProperties.length; i++) {
        target = $("input[name='" + productProperties[i] + "']");
        var eleName = target.attr("name");
        target.attr('value',targetedIframe.document.getElementById(eleName).value);     
      }
    }
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
	        "Go to DashBoard": function (){
	          window.location = "/portal/portal/home?tenant="+tenantParam+"&secondLevel=true";
	        }
	    });
	    dialogButtonsLocalizer($launchVmDialog, {'Go to Subscriptions':dictionary.goToSubscriptions, 'Go to Catalog': dictionary.goToCatalog, 'Go to DashBoard':dictionary.goToDashboard}); 
	    $launchVmDialog.dialog("open");
	    $("#launchingVm_template").find("#message2").html(message);
	    
	    var messageClass="success";
	    if(messageType=="error"){
	    	messageClass="error";
	    }
	    $("#launchingVm_template").addClass(messageClass);
    	$("#launchingVm_template").show();
    }
  
    $("#launchvm_button").click(function(event) {
      launchVMButtonClicked();
    });
    
    
    function launchVMButtonClicked(){
    	$("#top_message_panel").find("#msg").text("");
    	$("#top_message_panel").hide();

    	if(!checkGroupFullFillment()) {
    	  alert(dictionary.error_Group_Not_Satisfied);
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
        alert(g_dictionary.youCanNotSubscribeUntilYouAcceptTheTermsAndConditions);
        $("#accept_checkbox").focus();
        $("#spinning_wheel").hide();
        return false;
      }

      var deployVmUrl = "/portal/portal/dashboard/manageresource/deployVm?tenant="+tenantParam+"&productBundleId="+bundleObj_id;

      var propConfigs = JSON.stringify(productPropertyObj);
      //console.log('...Prop Values..',propConfigs);
      deployVmUrl+= "&configurationData="+propConfigs;
      deployVmUrl+= "&serviceInstaceUuid="+serviceInstaceUuid;
      deployVmUrl+= "&resourceType="+$("#resourceTypeSelection").val();
      deployVmUrl+= "&filters="+getSelectedFilterString();
      deployVmUrl+= "&context="+getContextString();
      if($("#provision_accept").is(':checked')) {
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
        	openSubscribeDialog(message, "success");
        	
        },
        error : function(jqXHR) {
        	if(jqXHR.status != AJAX_FORM_VALIDATION_FAILED_CODE){
        		var message = dictionary.subscriptionFailure+"<br /> "+jqXHR.responseText;
        		openSubscribeDialog(message, "error");
        	}else{
        		var message = dictionary.subscriptionFailure+": "+$.parseJSON(jqXHR.responseText)["validationResult"];
        		$("#top_message_panel").find("#msg").html(message);
                $("#top_message_panel").find("#status_icon").removeClass("successicon").addClass("erroricon");
                $("#top_message_panel").removeClass("success").addClass("error").show();
        	}
        }
      });
    }
    
    function updateMinified(){
      $('#componentselector-minified-filters .title').nextAll().remove();
      $('#componentselector-minified-components .title').nextAll().remove();
      var newComponentHTML = "<span class=\"selectedtext\">" + getSelectedFiltersAndComponents().join("</span><span class=\"text\">/</span><span class=\"selectedtext\">")  + "</span>";
      $(newComponentHTML).insertAfter("#componentselector-minified-components .title");
    }
    if($("#resourceTypeSelection").val()===SERVICE_RESOURCE_TYPE){
      $bundleContainer.empty();
      refreshBundle(null);
    }
    
    $("#get-pricing").click(function(event) {
      visibleBundles = 0;
      if(currentPage == 2) {
        refreshYourSelectionSummary();
        populateProductsForGeneratedUsage();
        return;
      }
      if(isNotBlank($("#customComponentSelector").val())) {
        updateMinified();
      }
      $bundleContainer.empty();
      refreshActive(null);
      refreshBundle(null);
      hideConfigureAndSubscribe();
      $("#bundle_container").show();
      $("#contextString").val(getContextString());
      $("#filterString").val(getSelectedFilterString());
    });
    
    $("#currency_selector").bind('mouseover',function(event)
        {
      $("#catalog_currencybox_dropdown").show();
        });

    $("#currency_selector").bind('mouseout',function(event)
        {
      $("#catalog_currencybox_dropdown").hide();
        });

    $(".currencyLi").bind('click',function(event){
      $("#spinning_wheel2").show();
      $("#selectedcurrencytext").html($(this).attr('id'));
      $("#selectedcurrencyflag").html('<img src="/portal/images/flags/'+$(this).attr('id')+'.gif"/>');
      $("#catalog_currencybox_dropdown").hide();
      $("#selectedCurrencySign").val($(this).attr('sign'));
      if(currentPage==1 && isPayAsYouGoChosen){
        populateUtilityRatesTable($("#tenantParam").val(),$("#serviceInstanceUuid").val(), $("#resourceTypeSelection").val());
      }
      else {
        if($("#resourceType").val()===SERVICE_RESOURCE_TYPE) {
          $bundleContainer.empty();
          refreshBundle(null);
        }
        else{
          $("#get-pricing").click();
        }
      }
      $("#spinning_wheel2").hide();
    });
});

function resetInputFields() {
  for(var i=0; i < allConfigurationProperties.length; i++) {
    var $inputField = $("input[name='" + allConfigurationProperties[i] + "']");
    $inputField.val(null);
    $inputField.data('valueDisplayName', null);
    $inputField.data('fieldDisplayName', null);
    $inputField.data('effectiveValue', null);
  }
}

function hideConfigureAndSubscribe(){
  if($("#anonymousBrowsing").val() == 'true' || $("#viewChannelCatalog").val() == 'true'){
    $(".configure_subscribe_button").hide();
    $(".go_button_class").hide();
    if($("#viewChannelCatalog").val() == 'true'){
      $("#tab_utility_view").hide();
    }
  } 
}

function updateProperty(visibleField) {
  var $hiddenField = $("#conf_" + $(visibleField).attr('name'));
  $hiddenField.val($(visibleField).val());
  $hiddenField.change();
}
