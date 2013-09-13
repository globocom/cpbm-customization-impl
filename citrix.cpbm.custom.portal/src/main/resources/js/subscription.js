/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
$(document).ready(function() {

  activateThirdMenuItem("l3_subscriptions_tab");
  
	function refreshGridRow(jsonObj, $template) {
	  if(jsonObj.state=="EXPIRED")
	    $template.find(".widget_statusicon").removeClass().addClass("widget_statusicon stopped");
	  else if(jsonObj.state=="ACTIVE")
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
        type:"POST",
        afterActionSeccessFn: function (resultObj) {
	        $("#subscriptionState").html(resultObj.state);
	        refreshGridRow(resultObj, $("li[id^='sub'].selected.subscriptions"));
	        viewSubscription($("li[id^='sub'].selected.subscriptions"));
        }
      },
  cancelsubscription: {
    label: dictionary.cancelsubscription,
    elementIdPrefix: "cancelsubscription",
    inProcessText: dictionary.cancellingSubscription,
    type:"POST",
    afterActionSeccessFn: function (resultObj) {
      $("#subscriptionState").html(resultObj.state);
      refreshGridRow(resultObj, $("li[id^='sub'].selected.subscriptions"));
      viewSubscription($("li[id^='sub'].selected.subscriptions"));
    }
  }
	};
	
  function getConfirmationDialogButtons(command) {

    var buttonCallBacks = {};
    var actionMapItem;
    if (command == "terminatesubscription") {
      actionMapItem = topActionMap.terminatesubscription;
    }
    else if (command == "cancelsubscription") {
      actionMapItem = topActionMap.cancelsubscription;
    }

    buttonCallBacks[dictionary.lightboxbuttonconfirm] = function () {
      $(this).dialog("close");
      
      
      
      var apiCommand;
      if (command == "terminatesubscription") {
        var subscriptionParam=$('#current_subscription_param').val();
        apiCommand = billingPath + "subscriptions/terminate/"+subscriptionParam; 
        
      }
      if (command == "cancelsubscription") {
        var subscriptionParam=$('#current_subscription_param').val();
        apiCommand = billingPath + "subscriptions/cancel/"+subscriptionParam; 
        
      }

      doActionButton(actionMapItem, apiCommand);

    };

    buttonCallBacks[dictionary.lightboxbuttoncancel] = function () {
      $(this).dialog("close");
    };

    return buttonCallBacks;
  }
  
  $(".terminatesubscription_link").live("click", function (event) {
    $("#dialog_confirmation").text(dictionary.lightboxterminatesubscription).dialog('option', 'buttons', getConfirmationDialogButtons("terminatesubscription")).dialog("open");
  });
  $(".cancelsubscription_link").live("click", function (event) {
    $("#dialog_confirmation").text(dictionary.lightboxcancelsubscription).dialog('option', 'buttons', getConfirmationDialogButtons("cancelsubscription")).dialog("open");
  }); 
	viewSubscription($("li[id^='sub'].selected.subscriptions"));

});

/**
 * Update subscription row
 */
$.editSubscription = function(jsonResponse){
	 	
	 	if(jsonResponse == null ){
	 		alert(i18n.errors.subscription.editSubscription);
	 	}else{
			$("#viewDetailsDiv").html("");
			var content = "";
			content=content+"<div class='db_gridbox_columns' style='width:33%;'>";
			content=content+"<div class='db_gridbox_celltitles'>";
			content=content+jsonResponse.id;
			content=content+"</div>";
			content=content+"</div>";
			content=content+"<div class='db_gridbox_columns' style='width:33%;'>";
			content=content+"<div class='db_gridbox_celltitles'>";
			content=content+jsonResponse.state;
			content=content+"</div>";
			content=content+"</div>";
			content=content+"<div class='db_gridbox_columns' style='width:33%;'>";
			content=content+"<div class='db_gridbox_celltitles'>";
			content=content+jsonResponse.productBundle.name;
			content=content+"</div>";
			content=content+"</div>";
			$("#row"+jsonResponse.param).html(content);
			viewSubscription($("#row"+jsonResponse.id));
		}
};

/**
* View Subscription details
* @param current
* @return
*/
function viewSubscription(current){
	 var divId = $(current).attr('id');
	 if(divId==null) return;
	 var id=divId.substr(3);
	 resetGridRowStyle();
	 $(current).addClass("selected active");
	 var selectedDetails = $("#selected_subs_for_details").val();
	 if(selectedDetails != null && selectedDetails != ''){
	   $("#list_all").addClass("title_listall_arrow");
     $("#list_titlebar").unbind("click").bind("click", function(){
       if("true" == $("#usage_billing_my_usage").val()){
         window.location="/portal/portal/usage/subscriptions?tenant="+$("#tenantParam").val();
       }else{
         window.location="/portal/portal/billing/subscriptions?tenant="+$("#tenantParam").val();
       }
     });
	   $("#search_panel").empty(); 
	 }else{
     $("#list_all").removeClass("title_listall_arrow");
     $("#list_titlebar").unbind("click");
   }
	 var url = billingPath+"subscriptions/showDetails?tenant="+$("#tenantParam").val();
	 $.ajax( {
			type : "GET",
			url : url,
			data:{id:id},
			dataType : "html",
			success : function(html) {
				$("#viewDetailsDiv").html("");
				$("#viewDetailsDiv").html(html);	
				bindActionMenuContainers();
				
				$("#details_tab").bind("click", function (event) {

				  $('#configurations_tab').removeClass('active').addClass("nonactive");
          $('#resource_details_tab').removeClass('active').addClass("nonactive");
          $('#entitlements_tab').removeClass('active').addClass("nonactive");
          $('#details_tab').removeClass('nonactive').addClass("active");
          $('#entitlements').hide();
          $('#configurations').hide();
          $('#resource_details').hide();
          $('#subscription_charges').show();

		    });
				
				$("#entitlements_tab").bind("click", function (event) {  
			      $('#details_tab').removeClass('active').addClass("nonactive");
			      $('#configurations_tab').removeClass('active').addClass("nonactive");
			      $('#resource_details_tab').removeClass('active').addClass("nonactive");
			      $('#entitlements_tab').removeClass('nonactive').addClass("active");
			      $('#resource_details').hide();
			      $('#subscription_charges').hide();
			      $('#configurations').hide();
			      $('#entitlements').show();
			  });
				
        $("#configurations_tab").bind("click", function (event) {  
          $('#details_tab').removeClass('active').addClass("nonactive");
          $('#entitlements_tab').removeClass('active').addClass("nonactive");
          $('#resource_details_tab').removeClass('active').addClass("nonactive");
          $('#configurations_tab').removeClass('nonactive').addClass("active");
          $('#entitlements').hide();
          $('#subscription_charges').hide();
          $('#resource_details').hide();
          $('#configurations').show();
        });

        $("#resource_details_tab").bind("click", function (event) {  
          $('#details_tab').removeClass('active').addClass("nonactive");
          $('#entitlements_tab').removeClass('active').addClass("nonactive");
          $('#configurations_tab').removeClass('active').addClass("nonactive");
          $('#resource_details_tab').removeClass('nonactive').addClass("active");
          $('#entitlements').hide();
          $('#subscription_charges').hide();
          $('#configurations').hide();
          $('#resource_details').show();
        });
        
			},error:function(){	
				//need to handle TO-DO
			}
	 });
	 
	 
	 
	 
	 
}


/**
 * Reset data row style
 * @return
 */
function resetGridRowStyle(){
	$(".widget_navigationlist").each(function(){
		$(this).removeClass("selected active");		
	});
}

function showInfoBubble(current) {
  if($(current).hasClass('active')) return
  $(current).find("#info_bubble").show();
  return false;
};
function hideInfoBubble(current) {
  $(current).find("#info_bubble").hide();
  return false;
};

function nextClick() {
  var $currentPage=$('#current_page').val();
  
  if("true" == $("#usage_billing_my_usage").val()){
    
    window.location = "/portal/portal/usage/subscriptions?tenant="+$("#tenantParam").val()+"&page="+(parseInt($currentPage)+1);
  }else{  
    window.location = "/portal/portal/billing/subscriptions?tenant="+$("#tenantParam").val()+"&page="+(parseInt($currentPage)+1);
  } 
  
  
}
function previousClick() {
  var $currentPage=$('#current_page').val();
  if("true" == $("#usage_billing_my_usage").val()){
    window.location = "/portal/portal/usage/subscriptions?tenant="+$("#tenantParam").val()+"&page="+(parseInt($currentPage)-1);
  }else{  
  window.location = "/portal/portal/billing/subscriptions?tenant="+$("#tenantParam").val()+"&page="+(parseInt($currentPage)-1);
  }
}

function filter_subscriptions(current){
  var useruuid = $("#userfilterdropdownforinvoices option:selected").val();
  var state = $("#filter_dropdown option:selected").val();
  var $currentPage=$('#current_page').val();
  var filterurl = "/portal/portal/usage/subscriptions?tenant="+$("#tenantParam").val()+"&page="+(parseInt($currentPage));
  if(useruuid != null && useruuid != 'ALL_USERS'){
    filterurl = filterurl+"&useruuid="+useruuid;
  }
  filterurl = filterurl+"&state="+state;
  window.location=filterurl;
}


function provisionSubscription(subscriptionId){
  window.location =  "/portal/portal/subscription/createsubscription?tenant="+$("#tenantParam").val()+"&subscriptionId="+subscriptionId;
}

//function terminateSubscriptionCancle(current){
//  $("#terminateSubscription_panel").hide();
//}

/*
function terminateSubscription(current){
	var actionId = $(current).attr('id');
	var subscriptionParam=actionId.substr(21);
	var actionurl = billingPath + "subscriptions/terminate/"+subscriptionParam;	
	$.ajax( {
			type : "POST",
			url : actionurl,
			dataType :"json",
			success : function(jsonResponse) {	
	      $("#term_subscription"+subscriptionParam).remove();
				$("#terminateSubscription_panel").hide();
	      $("#sub" + jsonResponse.id + " .subscriptionState").html(jsonResponse.state);
	      $("#sub" + jsonResponse.id).click();
	      
		 	},error:function(){	
		 		$("#miscFormErrors").text(i18n.errors.subscription.termSubscription);
		 		$("#miscFormErrors").show();
		 		
			}
	 });
}
*/
