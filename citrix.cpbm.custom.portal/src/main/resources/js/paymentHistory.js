/* Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. */
$(document).ready(function() {
  activateThirdMenuItem("l3_billing_payments_tab");
  
  bindActionMenuContainers();
});



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
  window.location = "/portal/portal/billing/paymenthistory?tenant="+$('#tenantParam').val()+"&page="+(parseInt($currentPage)+1);
}
function previousClick() {
  var $currentPage=$('#current_page').val();
  window.location = "/portal/portal/billing/paymenthistory?tenant="+$('#tenantParam').val()+"&page="+(parseInt($currentPage)-1);
}