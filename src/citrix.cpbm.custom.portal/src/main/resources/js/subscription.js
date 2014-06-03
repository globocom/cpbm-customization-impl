/*
*  Copyright � 2013 Citrix Systems, Inc.
*  You may not use, copy, or modify this file except pursuant to a valid license agreement from
*  Citrix Systems, Inc.
*/
$(document).ready(function() {

  activateThirdMenuItem("l3_subscriptions_tab");

  // Decide how to show 'List All' link and bind unbind accordingly
  var selectedDetails = $("#selected_subs_for_details").val();
  var filtersApplied = $("#filtersApplied").val();
  if ((filtersApplied != null && filtersApplied > 0) || (selectedDetails != null && selectedDetails != '')) {
    $("#list_all").addClass("title_listall_arrow");
    $("#list_titlebar").unbind("click").bind("click", function() {
      if ("true" == $("#usage_billing_my_usage").val()) {
        window.location = "/portal/portal/usage/subscriptions?tenant=" + $("#tenantParam").val() + "&state=All";
      } else {
        window.location = "/portal/portal/billing/subscriptions?tenant=" + $("#tenantParam").val() + "&state=All";
      }
    });
    if( selectedDetails != null && selectedDetails != ''){
      $("#search_panel").hide();
    }    
  } else {
    $("#list_all").removeClass("title_listall_arrow");
    $("#list_titlebar").unbind("click");
  }
  
  
  
  $("#advancesearchButton").click(function() {
    $("#advanceSearchDropdownDiv").toggle();
  });

  $("#advSrchCancel").click(function() {
	$("#dropdownfilter_users").val($('#useruuid').val());
	$("#dropdownfilter_states").val($('#stateSelected').val());
	$("#dropdownfilter_instances").val($('#instanceuuid').val());
	$("#dropdownfilter_bundles").val($('#productBundleID').val());
    $("#advanceSearchDropdownDiv").hide(); 
  });
  
  $("#selected_filters").text();
  $("#dropdownfilter_instances").change(function() {
    var selected_instance = $("#dropdownfilter_instances option:selected").attr('id');
    $("#dropdownfilter_bundles").empty();
    $("#dropdownfilter_bundles").append($("#hidden_bundles option").first().clone());
    $("#dropdownfilter_bundles").append($("#hidden_bundles option[instance="+ selected_instance +"]").clone());
    if($("#selectedBundleID").val() != null && $("#selectedBundleID").val() !=""){
      $("#dropdownfilter_bundles option[id="+ $("#selectedBundleID").val() +"]").attr('selected','selected');
    }
  });
  
  $("#dropdownfilter_instances").change();
  
  $("#advSrchSubmit").click(function() {
    
	  var useruuid = $("#dropdownfilter_users option:selected").val();
	  var state = $("#dropdownfilter_states option:selected").val();
	  var instanceParam = $("#dropdownfilter_instances option:selected").val();
	  var bundleId = $("#dropdownfilter_bundles option:selected").val();
	  
    var $currentPage = 1;
    
    if ("true" == $("#usage_billing_my_usage").val()) {
      filterurl = "/portal/portal/usage/subscriptions?tenant=" + $("#tenantParam").val()  + "&state=" + state;
    } else {
      filterurl = "/portal/portal/billing/subscriptions?tenant=" + $("#tenantParam").val() + "&state=" + state;
    }
    
    if (useruuid != null && useruuid != 'ALL') {
      filterurl = filterurl + "&useruuid=" + useruuid;
    }
    if (bundleId != null && bundleId != 'ALL') {
      filterurl = filterurl + "&productBundleID=" + bundleId;
    }
    if (instanceParam != null && instanceParam != 'ALL') {
      filterurl = filterurl + "&instanceuuid=" + instanceParam;
    }
    
    window.location = filterurl + "&page=" + (parseInt($currentPage));
  });  
  
  function refreshGridRow(jsonObj, $template) {
    if (jsonObj.state == "EXPIRED")
      $template.find(".widget_statusicon").removeClass().addClass("widget_statusicon stopped");
    else if (jsonObj.state == "ACTIVE")
      $template.find(".widget_statusicon").removeClass().addClass("widget_statusicon running");
    else
      $template.find(".widget_statusicon").removeClass().addClass("widget_statusicon nostate");

  }

  initDialog("dialog_confirmation", 350, false);
  var topActionMap = {
    terminatesubscription: {
      label: dictionary.terminatesubscription,
      elementIdPrefix: "terminatesubscription",
      inProcessText: dictionary.terminatingSubscription,
      type: "POST",
      afterActionSeccessFn: function(resultObj) {
        $("#subscriptionState").html(resultObj.state);
        refreshGridRow(resultObj, $("li[id^='sub'].selected.subscriptions"));
        viewSubscription($("li[id^='sub'].selected.subscriptions"));
      },
      afterActionFailureFn : function(xhr, status) {
        if(isNotBlank(xhr.responseText)) {
          return dictionary.cloudServiceException + " : " + xhr.responseText;
        }
        return null;
      },
      afterActionCompleteFn: function(jqXHR, textStatus){
        // adding this to override global ajaxSetup complete event handler
        return;
      }
    },
    cancelsubscription: {
      label: dictionary.cancelsubscription,
      elementIdPrefix: "cancelsubscription",
      inProcessText: dictionary.cancellingSubscription,
      type: "POST",
      afterActionSeccessFn: function(resultObj) {
        $("#subscriptionState").html(resultObj.state);
        refreshGridRow(resultObj, $("li[id^='sub'].selected.subscriptions"));
        viewSubscription($("li[id^='sub'].selected.subscriptions"));
      },
      afterActionCompleteFn: function(jqXHR, textStatus){
        // adding this to override global ajaxSetup complete event handler
        return;
      }
    }
  };

  function getConfirmationDialogButtons(command) {

    var buttonCallBacks = {};
    var actionMapItem;
    if (command == "terminatesubscription") {
      actionMapItem = topActionMap.terminatesubscription;
    } else if (command == "cancelsubscription") {
      actionMapItem = topActionMap.cancelsubscription;
    }

    buttonCallBacks[dictionary.lightboxbuttonconfirm] = function() {
      $(this).dialog("close");

      var apiCommand;
      if (command == "terminatesubscription") {
        var subscriptionParam = $('#current_subscription_param').val();
        apiCommand = billingPath + "subscriptions/terminate/" + subscriptionParam;

      }
      if (command == "cancelsubscription") {
        var subscriptionParam = $('#current_subscription_param').val();
        apiCommand = billingPath + "subscriptions/cancel/" + subscriptionParam;
      }

      doActionButton(actionMapItem, apiCommand);

    };

    buttonCallBacks[dictionary.lightboxbuttoncancel] = function() {
      $(this).dialog("close");
    };

    return buttonCallBacks;
  }

  $(".terminatesubscription_link").live("click", function(event) {
    $("#dialog_confirmation").text(dictionary.lightboxterminatesubscription).dialog('option', 'buttons',
      getConfirmationDialogButtons("terminatesubscription")).dialog("open");
      viewSubscription($("li[id^='sub'].selected.subscriptions"));
  });
  $(".cancelsubscription_link").live("click", function(event) {
    $("#dialog_confirmation").text(dictionary.lightboxcancelsubscription).dialog('option', 'buttons',
      getConfirmationDialogButtons("cancelsubscription")).dialog("open");
  });
  timerFunction($("li[id^='sub'].selected.subscriptions"));
});

/**
 * Update subscription row
 */
$.editSubscription = function(jsonResponse) {

  if (jsonResponse == null) {
    popUpDialogForAlerts("dialog_info", i18n.errors.subscription.editSubscription);
  } else {
    $("#viewDetailsDiv").html("");
    var content = "";
    content = content + "<div class='db_gridbox_columns' style='width:33%;'>";
    content = content + "<div class='db_gridbox_celltitles'>";
    content = content + jsonResponse.id;
    content = content + "</div>";
    content = content + "</div>";
    content = content + "<div class='db_gridbox_columns' style='width:33%;'>";
    content = content + "<div class='db_gridbox_celltitles'>";
    content = content + jsonResponse.state;
    content = content + "</div>";
    content = content + "</div>";
    content = content + "<div class='db_gridbox_columns' style='width:33%;'>";
    content = content + "<div class='db_gridbox_celltitles'>";
    content = content + jsonResponse.productBundle.name;
    content = content + "</div>";
    content = content + "</div>";
    $("#row" + jsonResponse.param).html(content);
    timerFunction($("#row" + jsonResponse.id));
  }
};

function refreshRow(subState, $template) {
  if (subState == "EXPIRED")
    $template.find(".widget_statusicon").removeClass().addClass("widget_statusicon stopped");
  else if (subState == "ACTIVE")
    $template.find(".widget_statusicon").removeClass().addClass("widget_statusicon running");
  else
    $template.find(".widget_statusicon").removeClass().addClass("widget_statusicon nostate");
}

function timerFunction(current) {
  hideInfoBubble(current);
  $("#spinning_wheel2").show();
  var timerKey = "timerKey";
  $("body").stopTime(timerKey);
  var promise = viewSubscription(current);
  
  if(promise == null) {
    $("#spinning_wheel2").hide();
    return;
  }
  
  promise.done(function(){
    $("#spinning_wheel2").hide();
    var subscriptionState = $("#viewDetailsDiv").find("#subscription_state").val();
    var handleState = $("#viewDetailsDiv").find("#subscription_handle_state").val();
    if(handleState == "PROVISIONING") {
      $("body").everyTime(5000, timerKey, function() {
        var promise = viewSubscription(current);
        if(promise == null) {
          $("body").stopTime(timerKey);
          return;
        }
        promise.done(function(){
          var subscriptionState = $("#viewDetailsDiv").find("#subscription_state").val();
          refreshRow(subscriptionState, current);
          var handleState = $("#viewDetailsDiv").find("#subscription_handle_state").val();
          if(handleState != "PROVISIONING") {
            $("body").stopTime(timerKey);
          }
        });
      }, 0);
    }
  });
}
  
/**
 * View Subscription details
 * 
 * @param current
 * @return
 */

function viewSubscription(current) {
  var deferred = $.Deferred();
  var divId = $(current).attr('id');
  if (divId == null) {
    return null;
  }
  var id = divId.substr(3);
  resetGridRowStyle();
  $(current).addClass("selected active");
  var url = billingPath + "subscriptions/showDetails?tenant=" + $("#tenantParam").val();
  $.ajax({
    type: "POST",
    url: url,
    async: false,
    data: {
      id: id
    },
    dataType: "html",
    success: function(html) {
      $("#viewDetailsDiv").html("");
      $("#viewDetailsDiv").html(html);
      
      $("#viewDetailsDiv").find("#js_resource_error").popover();
      
      bindActionMenuContainers();
      
      var subscriptionState = $("#viewDetailsDiv").find("#subscriptionState").text();
      subscriptionState = jQuery.trim(subscriptionState);
      var $divId = $("#sub" + id); 
      var oldSubscriptionState = jQuery.trim($divId.find("#subscriptionStateDivId").text());
      var endDate = $("#viewDetailsDiv").find("#endDate").val();
      if (subscriptionState != oldSubscriptionState && endDate != null){
        $divId.find("#subscriptionStateDivId").text(subscriptionState);
        if(subscriptionState == "EXPIRED"){
        
          var endDateDiv = ""+
          "<div class=\"raw_content_row\">" + 
          "<div class=\"raw_contents_title\">" + 
            dictionary.subscriptionEndDate + ":"+ 
          "</div>" +
          "<div class=\"raw_contents_value\">" + 
            "<span>"+
              endDate + 
            "</span>" + 
          "</div>" +
        "</div>";
          
          $divId.find(".raw_contents").append (endDateDiv);
        }
      }
      
      $("#details_tab").bind("click", function(event) {

        $('#configurations_tab').removeClass('active').addClass("nonactive");
        $('#resource_details_tab').removeClass('active').addClass("nonactive");
        $('#entitlements_tab').removeClass('active').addClass("nonactive");
        $('#details_tab').removeClass('nonactive').addClass("active");
        $('#entitlements').hide();
        $('#configurations').hide();
        $('#resource_details').hide();
        $('#subscription_charges').show();

      });

      $("#entitlements_tab").bind("click", function(event) {
        $('#details_tab').removeClass('active').addClass("nonactive");
        $('#configurations_tab').removeClass('active').addClass("nonactive");
        $('#resource_details_tab').removeClass('active').addClass("nonactive");
        $('#entitlements_tab').removeClass('nonactive').addClass("active");
        $('#resource_details').hide();
        $('#subscription_charges').hide();
        $('#configurations').hide();
        $('#entitlements').show();
      });

      $("#configurations_tab").bind("click", function(event) {
        $('#details_tab').removeClass('active').addClass("nonactive");
        $('#entitlements_tab').removeClass('active').addClass("nonactive");
        $('#resource_details_tab').removeClass('active').addClass("nonactive");
        $('#configurations_tab').removeClass('nonactive').addClass("active");
        $('#entitlements').hide();
        $('#subscription_charges').hide();
        $('#resource_details').hide();
        $('#configurations').show();
      });

      $("#resource_details_tab").bind("click", function(event) {
        $('#details_tab').removeClass('active').addClass("nonactive");
        $('#entitlements_tab').removeClass('active').addClass("nonactive");
        $('#configurations_tab').removeClass('active').addClass("nonactive");
        $('#resource_details_tab').removeClass('nonactive').addClass("active");
        $('#entitlements').hide();
        $('#subscription_charges').hide();
        $('#configurations').hide();
        $('#resource_details').show();
      });
      deferred.resolve();
    },
    error: function(xhr) {
      $("#spinning_wheel2").hide();
    },
    complete: function(xhr, status) {
      // Just added to prevent it from going to generic handler
    }
  });
  return deferred.promise();
}


/**
 * Reset data row style
 * @return
 */

function resetGridRowStyle() {
  $(".widget_navigationlist").each(function() {
    $(this).removeClass("selected active");
  });
}

function showInfoBubble(current) {
  if ($(current).hasClass('active')) return
  $(current).find("#info_bubble").show();
  return false;
};

function hideInfoBubble(current) {
  $(current).find("#info_bubble").hide();
  return false;
};

function nextClick() {
  var $currentPage = $('#current_page').val();
  var useruuid = $("#dropdownfilter_users option:selected").val();
  var state = $("#dropdownfilter_states option:selected").val();
  var instanceParam = $("#dropdownfilter_instances option:selected").val();
  var bundleId = $("#dropdownfilter_bundles option:selected").val();

  var filterurl = "";
  if (useruuid != null && useruuid != 'ALL') {
    filterurl = filterurl + "&useruuid=" + useruuid;
  }
  if (bundleId != null && bundleId != 'ALL') {
    filterurl = filterurl + "&productBundleID=" + bundleId;
  }
  if (instanceParam != null && instanceParam != 'ALL') {
    filterurl = filterurl + "&instanceuuid=" + instanceParam;
  }
  
  filterurl = filterurl + "&state=" + state + "&page=" + (parseInt($currentPage) + 1);
  
  if ("true" == $("#usage_billing_my_usage").val()) {
    window.location = "/portal/portal/usage/subscriptions?tenant=" + $("#tenantParam").val() + filterurl;
  } else {
    window.location = "/portal/portal/billing/subscriptions?tenant=" + $("#tenantParam").val() + filterurl;
  }
}

function previousClick() {
  var $currentPage = $('#current_page').val();
  var useruuid = $("#dropdownfilter_users option:selected").val();
  var state = $("#dropdownfilter_states option:selected").val();
  var instanceParam = $("#dropdownfilter_instances option:selected").val();
  var bundleId = $("#dropdownfilter_bundles option:selected").val();

  var filterurl = "";
  if (useruuid != null && useruuid != 'ALL') {
    filterurl = filterurl + "&useruuid=" + useruuid;
  }
  if (bundleId != null && bundleId != 'ALL') {
    filterurl = filterurl + "&productBundleID=" + bundleId;
  }
  if (instanceParam != null && instanceParam != 'ALL') {
    filterurl = filterurl + "&instanceuuid=" + instanceParam;
  }
  filterurl = filterurl + "&state=" + state + "&page=" + (parseInt($currentPage) - 1);
  
  if ("true" == $("#usage_billing_my_usage").val()) {
    window.location = "/portal/portal/usage/subscriptions?tenant=" + $("#tenantParam").val() + filterurl;
  } else {
    window.location = "/portal/portal/billing/subscriptions?tenant=" + $("#tenantParam").val() + filterurl;
  }
}
/*
function filter_subscriptions(current) {
  var useruuid = $("#userfilterdropdownforinvoices option:selected").val();
  var state = $("#filter_dropdown option:selected").val();
  var $currentPage = $('#current_page').val();
  var filterurl = "/portal/portal/usage/subscriptions?tenant=" + $("#tenantParam").val() + "&page=" + (parseInt(
    $currentPage));
  if (useruuid != null && useruuid != 'ALL_USERS') {
    filterurl = filterurl + "&useruuid=" + useruuid;
  }
  filterurl = filterurl + "&state=" + state;
  window.location = filterurl;
}
*/

function provisionSubscription(subscriptionId) {
  window.location = "/portal/portal/subscription/createsubscription?tenant=" + $("#tenantParam").val() +
    "&subscriptionId=" + subscriptionId;
}

$(function (){
  $(".js_filter_details_popover").popover({trigger:"hover",html : true, content: function() {

    var filterUser = $("#dropdownfilter_users option:selected").text();
    var filterState = $("#dropdownfilter_states option:selected").text();
    var filterInstance = $("#dropdownfilter_instances  option:selected").text();
    var filterBundle = $("#dropdownfilter_bundles  option:selected").text();

    $('#js_filter_details_popover').find("#_filter_state").text(filterState);
    $('#js_filter_details_popover').find("#_filter_instance").text(filterInstance);
    $('#js_filter_details_popover').find("#_filter_bundle").text(filterBundle);
    
    if (filterUser !="" && filterUser.length > 0) {
      $('#js_filter_details_popover').find("#_filter_user").text(filterUser);
    }else{
      $('#js_filter_details_popover .popover_rows:first').hide();
    }

    return $('#js_filter_details_popover').html();
  }});
});
