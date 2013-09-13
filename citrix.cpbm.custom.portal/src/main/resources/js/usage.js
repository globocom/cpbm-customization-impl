/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
$(document).ready(function() {
/**
 * Localize the Product Bundle Charge Name
 */
  var bundleId = ["VMBundle", "NonVMBundle", "RenewalVMBundle", "RenewalNonVMBundle"];
  for (var i = 0; i < bundleId.length; i ++) {
    var bundleCount = $("#count" + bundleId[i]).val();
    for (var j = 0; j < bundleCount; j ++) {
      var bundleChargeId = "#charge" + bundleId[i] + j;
      var bundleCharge = $(bundleChargeId + "_val").val();
      var localized_bundleCharge = localizeCloudStackMsg(bundleCharge);
      $(bundleChargeId).text(localized_bundleCharge);
      if ($(bundleChargeId).attr("title") != undefined) {
        $(bundleChargeId).attr("title", localized_bundleCharge);
      }
    }
  }

  $(".netUsageObjectDiv").bind('click',function (event) {
	  var divId = $(this).attr("id");
	  var Id=divId.substr(9);
    var dialogId = "dialog_invoice_details_"+Id;
    initDialog(dialogId, 700);
    var $thisDialog = $("#" + dialogId);
    $thisDialog.dialog('option', 'buttons', {
              "Cancel": function () {
               $(this).dialog("close");
              }
            });
    dialogButtonsLocalizer($thisDialog, {'Cancel': g_dictionary.dialogClose}); 
    $thisDialog.dialog("open");
    });

  activateThirdMenuItem("l3_usage_details_tab");
	$("#selectViewBy").change(function(){
		var projectId= $("#projectId").val();
		var viewBy = $("#selectViewBy").val();
		window.location="/portal/portal/projects/"+projectId+"/usageBilling?viewBy="+viewBy;
	});	

	  $(".showUsgDetails").click(function(){
		  var id = $(this).attr("id").substring(14);
		  $(this).hide();
		  $("#hideUsgDetails"+id).show();
		  $("#desc"+id).slideToggle();
	  });
	  $(".hideUsgDetails").click(function(){
		  var id = $(this).attr("id").substring(14);
		  $(this).hide();
		  $("#showUsgDetails"+id).show();
		  $("#desc"+id).slideToggle();
	  });
	  $(".showMonthUsgDetails").click(function(){
		  var id = $(this).attr("id").substring(19);
		  $(this).hide();
		  $("#hideMonthUsgDetails"+id).show();
		  $("#desc"+id).slideToggle();
	  });
	  $(".hideMonthUsgDetails").click(function(){
		  var id = $(this).attr("id").substring(19);
		  $(this).hide();
		  $("#showMonthUsgDetails"+id).show();
		  $("#desc"+id).slideToggle();
	  });	  
	  $("#exportUsage").click(function(){
		  var url = $(this).attr("name");		  
		  $.ajax( {
			type : "PUT",
			url : url,
			data: {},				
			dataType : "html",
			success : function(html) {
				if(html=='Success'){
					alert(i18n.errors.usage.usageReportExport);
				}
				else{
					alert(i18n.errors.usage.usageReportUnable);
				}
			},
			error : function (html) {
				alert(i18n.errors.usage.usageReportError);
			}				
		});			
		  
	  });
	  
	  $(".usagelist_header").click(function(){
		  var div_id = $(this).attr('id');
		  $("#"+div_id+"data").slideToggle();
		  var id = div_id.substring(6); //Opening the tab
		  if($("#arrow"+id).hasClass('closed')){
			  $("#arrow"+id).removeClass('closed');
			  $("#arrow"+id).addClass('opened');
			  $("#title"+id).removeClass('off');			  
			  $("#title"+id).addClass('on');
			  $("#total"+id).removeClass('off');			  
			  $("#total"+id).addClass('on');
			  $(this).removeClass('off');			  
			  $(this).addClass('on');
		  }else{						// Closing the tab
			  $("#arrow"+id).removeClass('opened');
			  $("#arrow"+id).addClass('closed');
			  $("#title"+id).removeClass('on');
			  $("#title"+id).addClass('off');
			  $("#total"+id).removeClass('on');
			  $("#total"+id).addClass('off');
			  $(this).removeClass('on');
			  $(this).addClass('off');
			  
		  }
	  });
	  
	  
	$("#accountStatementUuid").change(function(){
		var option = $("#accountStatementUuid").val();
		window.location=usageBillingUrl + "&accountStatementUuid="+option;
	});	
	
	
	$("#generateurd").click(function(){
		var option = $("#accountStatementUuid").val();
		var newUrl = generateURDUrl + "&accountStatementUuid="+option;
		top.location.href=newUrl;
		$("#generateurd").attr('href', newUrl);		
	});	
	
	$("#generatePdfInvoice").click(function(){
		var option = $("#accountStatementUuid").val();
		var newUrl = generatePdfUrl + "&accountStatementUuid="+option ;
		$("#generatePdfInvoice").attr('href', newUrl);		
	});	

  initDialog("dialog_confirmation", 350, false);
  var topActionMap = {
      
      sendEmailInvoice: {
       label: dictionary.sendInvoiceEmail,
       inProcessText: dictionary.sendingInvoiceEmail,
       type : "GET",
       dataType : "html",
       afterActionSeccessFn  : function(html) {
       }   
      }  };

  function getConfirmationDialogButtons(command) {

      var buttonCallBacks = {};
      var actionMapItem;
      if (command == "sendEmailInvoice") {
        actionMapItem = topActionMap.sendEmailInvoice;
      }
      buttonCallBacks[dictionary.lightboxbuttonconfirm] = function () {
        $(this).dialog("close");

        var apiCommand = null;
        if (command == "sendEmailInvoice") {
          var accountStatementUuid = $("#accountStatementUuid").val();
          var currentPage = $("#current_page").val();
          apiCommand = sendEmailInvoicePDF + "&accountStatementUuid=" + accountStatementUuid + "&page=" + currentPage;
        }
        doActionButton(actionMapItem, apiCommand);
      };

      buttonCallBacks[dictionary.lightboxbuttoncancel] = function () {
        $(this).dialog("close");
      };

      return buttonCallBacks;
    }

	$("#sendEmailInvoicePdf").click(function(){
	    $("#dialog_confirmation").text(dictionary.lightboxSendInvoiceEmail).dialog('option', 'buttons', getConfirmationDialogButtons("sendEmailInvoice")).dialog("open");
	  }); 
		
	bindActionMenuContainers();
});

function viewActivityBill(current){
  var $currentPage=$('#current_page').val();
  var divId = $(current).attr('id');
  if(divId==null) return;
  var ID=divId.substr(3);
  resetGridRowStyle();
  $(current).addClass("selected active");
  window.location=usageBillingUrl + "&accountStatementUuid="+ID +"&page="+(parseInt($currentPage));
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

function generateURD(){
  
}

function showInfoBubble(current) {
  var authorize = $('#authorizeForPopUp').val();
  if(authorize == 'y'){
    if($(current).hasClass('active')) return;
    $(current).find("#info_bubble").show();
  }
  return false;
};
function hideInfoBubble(current) {
  var authorize = $('#authorizeForPopUp').val();
  if(authorize == 'y'){
    $(current).find("#info_bubble").hide();
  }
  return false;
};



function nextClick() {
  var $currentPage=$('#current_page').val();
  window.location = usageBillingUrl+"&page="+(parseInt($currentPage)+1);
}
function previousClick() {
  var $currentPage=$('#current_page').val();
  window.location = usageBillingUrl+"&page="+(parseInt($currentPage)-1);
}

function changeUser(current){
  var useruuid = $(current).val();
  var ID = $("#accountStatementUuid").val();
  var filterurl = usageBillingUrl;
  var $currentPage=$('#current_page').val();
  if(ID != null){
    filterurl = filterurl+"&accountStatementUuid="+ID;
  }
  if($currentPage != null){
    filterurl = filterurl+"&page="+(parseInt($currentPage));
  }
  if(useruuid != null){
    filterurl = filterurl+"&useruuid="+useruuid;
  }
  window.location=filterurl;
}
