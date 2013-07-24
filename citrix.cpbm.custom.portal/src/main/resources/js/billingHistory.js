/* Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. */
$(document).ready(function() {
  activateThirdMenuItem("l3_billing_invoices_tab");
  if (isDelinquent == true) {
    if (showMakePaymentMessage != "") {
      alert(showMakePaymentMessage);
    }
  }

  $(".invoiceDetailsLink").live('click',function (event) {
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
});

function viewInvoiceDetailsGet(element, invoiceId) {
  
    var url = billingUrl + "invoice/" + invoiceId + "/invoiceDetails";
    $.ajax({
      type : "GET",
      url : url,
      dataType : "html",
      success : function(html) {
        $("#invoiceItemsView").html(html);
        $('#details_tab').removeClass('active').addClass("nonactive");
        $('#viewCharges_tab').removeClass('nonactive').addClass("active");
        $('#invoice_details_content').show();
        $('#details_content').hide();
      },
      error : function() {
        alert(i18n.errors.billingHistory.showDetails);
      }
    });
}

function details_tab_click(current) {

  $('#viewCharges_tab').removeClass('active').addClass("nonactive");
  $(current).removeClass('nonactive').addClass("active");
  $('#invoice_details_content').hide();
  
  $('#details_content').show();

};

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
  window.location = "/portal/portal/billing/history?tenant="+$('#tenantParam').val()+"&page="+(parseInt($currentPage)+1);
}
function previousClick() {
  var $currentPage=$('#current_page').val();
  window.location = "/portal/portal/billing/history?tenant="+$('#tenantParam').val()+"&page="+(parseInt($currentPage)-1);
}