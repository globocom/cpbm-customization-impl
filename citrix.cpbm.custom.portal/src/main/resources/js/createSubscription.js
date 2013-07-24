/* Copyright (C) 2013 Citrix Systems, Inc. All rights reserved */

var currentBundleTemplate;
var isPayAsYouGoChosen = false;
var currentPage = 1;
var section2Top ;
var SERVICE_RESOURCE_TYPE = "__SERVICE__";

function getActionUrlForListBundle() {
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
  if($("#anonymousBrowsing").val() == 'true'){
    locationPrefix = "/portal/portal/catalog/browse_catalog";
    var currencyCode = $("#selectedcurrencytext").html();
    locationPrefix = locationPrefix + "?&currencyCode="+currencyCode
  }
  else if($("#viewChannelCatalog").val() == 'true'){
    var channelId = $("#channelId").val();
    var revision = $("#revision").val();
    var dateFormat = $("#dateFormat").val();
    var revisionDate = $("#revisionDate").val();
    var currencyCode = $("#selectedcurrencytext").html();
    actionUrl = actionUrl + "&viewCatalog=true&revision="+revision+"&channelParam="+channelId+"&currencyCode="+currencyCode+"&revisionDate="+revisionDate+"&dateFormat="+dateFormat
  }
  return actionUrl;
}

function getPrefixedLocation(selectedResourceType){
  var locationPrefix = "/portal/portal/subscription/createsubscription";
  if($("#anonymousBrowsing").val() == 'true'){
    locationPrefix = "/portal/portal/catalog/browse_catalog";
    var currencyCode = $("#selectedcurrencytext").html();
    locationPrefix = locationPrefix + "?&currencyCode="+currencyCode
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
    var compInputVal = $("input[name='" + uniqueResourceComponents[i] + "']").data('effectiveValue');
    if(isBlank(compInputVal)) {
      compInputVal = $("input[name='" + uniqueResourceComponents[i] + "']").val();
    }
    if (isNotBlank(compInputVal)) {
      if (contextString !== "") {
        contextString += ",";
      }
      contextString += uniqueResourceComponents[i] + "=" + compInputVal;
    }
  }
  return contextString;
}

function isNotBlank(str) {
  if(str != null && str != "" && typeof(str) != "undefined") {
    return true;
  }
  return false;
}

function isBlank(str) {
  return !isNotBlank(str);
}

function prepareSelectedCategory() {
  
  var refreshPage = function rePopulateCreateSubscription(serviceInstanceUuid, tenantParam) {
    window.location = getPrefixedLocation();
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
}

function loadServiceFilters() {
  var $container = $("#filters_SECTION_2").empty();
  for(i=0; i<serviceFilterNames.length; i++) {
    var html = $("#row-clone-new").clone();
    html.find("#filterBoxTitle").text(l10dict[serviceFilterNames[i] + "-name"]);
    var $listBox = html.find("#filterBoxSelection").empty();
    $listBox.attr("name", serviceFilterNames[i]);
    var serviceFilterValues = serviceFilters[i];
    for(j=0; j<serviceFilterValues.length; j++) {
      var serviceFilterValue = serviceFilterValues[j];
      var name = serviceFilterValue.split("=")[0];
      var value = serviceFilterValue.split("=")[1];
      $listBox.append($('<option>', {
        id : value,
        value : value,
        text : name,
        title: name,
        fieldDisplayName : l10dict[serviceFilterNames[i] + "-name"]
      }));
    }
    $listBox.unbind("change").bind("change", function() {
      var $target = $(this).find(':selected');
      refreshFilterInputField($target);
      refreshRCsListing();
    });
    
    var currentInput = $("input[name='" + serviceFilterNames[i] + "']").val();
    var found = false;
    if(isNotBlank(currentInput)) {
      html.find('option').each(function(){
        if($(this).attr('value') == currentInput) {
          found = true;
          $(this).prop('selected', true);
          refreshFilterInputField($(this));
        }
      });
    } 
    
    if(!found) {
      var target = html.find('#filterBoxSelection option:first'); 
      target.prop('selected', true);
      refreshFilterInputField(target);
    }
    $container.append(html.show());
  }
}

function refreshInputField(target) {
  var $inputField = $("input[name='" + target.parent('select').attr('name') + "']");
  $inputField.val(target.attr("value"));
  $inputField.data('valueDisplayName', target.attr('valueDisplayName'));
  $inputField.data('fieldDisplayName', target.attr('fieldDisplayName'));
  $inputField.data('effectiveValue', target.attr("effectiveValue"));
}

function refreshFilterInputField($target) {
  var $inputField = $("input[name='" + $target.parent('select').attr('name') + "']");
  $inputField.val($target.attr("value"));
  $inputField.data('valueDisplayName', $target.text());
  $inputField.data('fieldDisplayName', $target.attr("fieldDisplayName"));
}


function loadServiceResourceComponents() {
  var $leftFilterPanel = $("#components_SECTION_2").empty();
  for(var i=0; i < uniqueResourceComponents.length; i++) {
    var html = $("#row-clone-new").clone();
    html.find("#filterBoxTitle").text(l10dict[uniqueResourceComponents[i] + "-name"]);
    var $listBox = html.find("#filterBoxSelection").empty();
    $listBox.attr("name", uniqueResourceComponents[i]);
    $listBox.append($('<option>', {
        fieldDisplayName : l10dict[uniqueResourceComponents[i] + "-name"],
        value : "",
        text : dictionary.label_Any,
        effectiveValue : "",
        valueDisplayName : "",
        selected : 'selected'
      }));
    var data = getValuesForComponent(uniqueResourceComponents[i]);
    
    for (var component in data) {
      var effectiveValue = data[component]["value"];
      if(data[component]["parent"] != null){
        effectiveValue = data[component]["parent"]["value"];
      }
      $listBox.append($('<option>', {
        fieldDisplayName : l10dict[uniqueResourceComponents[i] + "-name"],
        value : data[component]["value"],
        text : data[component]["name"],
        effectiveValue : effectiveValue,
        valueDisplayName : data[component]["name"],
        title: data[component]["name"]
      }));
    }
    $listBox.unbind("change").bind("change", function() {
      var target = $(this).find(':selected');
      refreshInputField(target);
      refreshRCsListing();
    });
    
    var currentInput = $("input[name='" + uniqueResourceComponents[i] + "']").val();
    var found = false;
    
    if(isNotBlank(currentInput)) {
      html.find('option').each(function() {
        if($(this).attr('value') == currentInput) {
          found = true;
          $(this).prop('selected', true).parent('select');
          refreshInputField($(this));
        }
      });
    } 
    
    if(!found) {
      var target = html.find("#filterBoxSelection option:first"); 
      target.prop('selected', true);
      refreshInputField(target);
    }
    
    $leftFilterPanel.append(html.show());
  }
}

function getValuesForComponent(componentTypeName, effComponentType) {
  var url;
  if(currentBundleTemplate != null) {
    var bundleRevisionObj = $(currentBundleTemplate).data("bundleRevisionObj");
  }
  if(bundleRevisionObj != null && bundleRevisionObj != undefined && !isPayAsYouGoChosen) {
    url = "/portal/portal/subscription/getResourceComponentsForBundle?bundleId="+bundleRevisionObj.productBundle.id+"&revisionId="+bundleRevisionObj.revision.id;
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
      resourceType : $("#resourceTypeSelection").val(),
      componentType : componentTypeName,
      effComponentType : effComponentType,
      filters : getSelectedFilterString(),
      contextString : getContextString()
    },
    dataType : "json",
    success : function(data) {
      returnValues = data;
    }
  });
  return returnValues;
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
	
	//$(".js_utility_resource_selection").val($(selectedResourceType).val());
  var location = getPrefixedLocation($(selectedResourceType).val());
  
  window.location = location;
}

function refreshRCsListing() {
  $("#spinning_wheel").show();
  if(isNotBlank($("#customComponentSelector").val())) {
    initializeComponentSelector(false, 1);
  } else {
    loadServiceResourceComponents();
  }
  $("#get-pricing").click();
  $("#spinning_wheel").hide();
}

function narrowYourSearchClickEventListener() {
	$("#narrow_your_search_link").toggleClass("expand");
	$("#narrow_your_search_link").toggleClass("collapse");
    $("#filter_options").slideToggle("slow");
    $("#filter").removeClass("selected");
    if (!$("#filter_options").is(":hidden")){
      $("#filter").addClass("selected");
    }
}

function bindEvents() {
  $(window).scroll(function() {
    var payg_choice = $('#subscribe_payg_choice');
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
      var stopHeight = payg_choice.offset().top + payg_choice.height();
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
  
  $('#tab_utility_view a').click(function (e) {
    e.preventDefault();
    $("#spinning_wheel2").show();
    isPayAsYouGoChosen = true;
    $("#SECTION_1").show();
    $("#narrow-your-search").hide();
    $("#SECTION_2").hide();
    $("#SECTION_3").hide();
    $("#filter_options").hide();
    $("#SECTION_4").hide();
    $(this).parent().addClass('active').siblings().removeClass('active');
    $("#effective_date_box").text('');
    populateUtilityRatesTable($("#tenantParam").val(),$("#serviceInstanceUuid").val(), $("#resourceTypeSelection").val());
    $("#effective_date_box").text(effective_date_str);
    $("#pay_as_you_go_action_container_2").show();
    $("#spinning_wheel2").hide();
  });
  $('#tab_bundles_view a').click(function (e) {
    e.preventDefault();
    $("#spinning_wheel2").show();
    isPayAsYouGoChosen = false;
    $("#resourceTypeSelection").val($("#resourceType").val());
    $("#pay_as_you_go_action_container_2").hide();
    $("#SECTION_1").hide();
    $("#narrow_your_search_link").removeClass("collapse").addClass("expand");
    $("#narrow-your-search").show();
    $("#filter_options").hide();
    $("#SECTION_2").show();
    $("#SECTION_3").show();
    $("#SECTION_4").hide();
    $(this).parent().addClass('active').siblings().removeClass('active');
    $("#spinning_wheel2").hide();
  });
}

$(document).ready(function() {
  currentPage = 1;
  if(isNotBlank($("#cloudServiceException").val())) {
    return;
  }
  $("#spinning_wheel").show();
  prepareSelectedCategory();
  bindEvents();
  
  if(isNotBlank($("#isPayAsYouGoChosen").val()) && $("#isPayAsYouGoChosen").val() == "true") {
    $('#tab_utility_view a').click();
  }
  
  if($("#resourceTypeSelection").val() != SERVICE_RESOURCE_TYPE) {
    $("#tab_utility_view").show();
    $(".service_filters_label").show();
    subscriptionId = $("#subscriptionId").val();
    isReconfigure = $("#isReconfigure").val();
    
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
    
    if(isNotBlank($("#isReconfigure").val())) {
      $("#SECTION_2").find('.catalog_select_boxes').each(function() {
        
        var componentId = $(this).attr('name');
        if(isBlank(componentId) || reconfigurableMap[componentId] == "false" || reconfigurableMap[componentId] == undefined) {
          $(this).prop('disabled', true);
        }
      });
    }
    
    for(var i=0; i < uniqueResourceComponents.length; i++) {
      $("input[name='" + uniqueResourceComponents[i] + "']").unbind("change").bind("change", function() {$("#get-pricing").click();});
    }
  } else {
    $("#tab_utility_view").hide();
    $(".service_filters_label").hide();
  }
  hideConfigureAndSubscribe();

  $("#spinning_wheel").hide();

});

function populateUtilityRatesTable(tenant, serviceInstanceUuid, resourceType){
  var currencyCode = $("#selectedcurrencytext").html();
  $.ajax({
  	    type : "GET",
  	    async : false,
  	    url : "/portal/portal/subscription/utilityrates_table",
  	    data : {
  	    	tenant:tenant,
  	    	serviceInstanceUuid :serviceInstanceUuid,
  	    	resourceTypeName :resourceType,
  	    	currencyCode:currencyCode
  	    },
  	    dataType : "html",
  	    success : function(html) {
  	    	$("#utilityrate_table").empty();
  	    	$("#utilityrate_table").html(html);
  	    }
  	  });
}

function checkGroupFullFillment() {
  
  if($("#resourceTypeSelection").val() === SERVICE_RESOURCE_TYPE) {
    return true;
  }
  
  var currentComponents = [];
  for(var i=0; i < uniqueResourceComponents.length; i++) {
    var compInputVal = $("input[name='" + uniqueResourceComponents[i] + "']").val();
    if(isNotBlank(compInputVal) && compInputVal != dictionary.all) {
      currentComponents.push(uniqueResourceComponents[i]);
    }
  }
  var isFulfilled = false;
  for (var i = 0; i < groups.length; i++) {
    isMatched = true;
    for (var currentComponent in currentComponents) {
      if (groups[i][currentComponent] !== currentComponents[currentComponent]) {
        isMatched = false;
        break;
      }
    }
    if (isMatched) {
      if (groups[i].length === currentComponents.length) {
        isFulfilled = true;
        break;
      }
    }
  }
  return isFulfilled;
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