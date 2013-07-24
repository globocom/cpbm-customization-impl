/* Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. */
  var getTicketAjaxReq;
  var getTicketCountAjaxReq;
  var ajaxAborted = false;
  
$(document).ready(function() {

    if (typeof(fetchTicketCount) != 'undefined' && fetchTicketCount){
      getTicketsCount();
    }
    
    if(typeof(loadGravtars) != 'undefined' && loadGravtars == true){
		viewUsersGravtars();
  }
  
 
  
  $("#spendByTypeTab").click(function() {
     $("#spendByTypeTab").attr("class", "last current");
     $("#spendBudgetTab").attr("class", "first");
     $("#spendByTypeChart").show();
     $("#spendBudgetChart").hide();
  });

  $("#spendBudgetTab").click(function() {
     $("#spendByTypeTab").attr("class", "last");
     $("#spendBudgetTab").attr("class", "first current");
     $("#spendByTypeChart").hide();
     $("#spendBudgetChart").show();
  });

  $("#custRankTab").click(function(event) {
      event.preventDefault();
      $("#newRegTab").attr("class", "first");
      $("#custRankTab").attr("class", "last current");
      $("#newRegTab").find("a").removeClass("current");
      $("#custRankTab").find("a").addClass("current");
      $("#newRegChart").hide();
      $("#custRankChart").show();
  });

  $("#newRegTab").click(function(event) {
    event.preventDefault();
    $("#newRegTab").attr("class", "first current");
    $("#custRankTab").attr("class", "last");
    $("#custRankTab").find("a").removeClass("current");
    $("#newRegTab").find("a").addClass("current");
    $("#newRegChart").show();
    $("#custRankChart").hide();
  });
 
  
  $(".utility_rates_link").unbind("click").bind("click", function(event) {        
	  viewUtilitRates(tenantParam,"utilityrates_lightbox");
  });  
  
  
});
  
  
function go_to_notifications(current){
  var notifications_for = $(current).attr('ref');
  if(notifications_for=="today"){
    window.location="/portal/portal/tenants/notifications?filterBy=Today&tenant="+effectiveTenantParam;
  }
  else if(notifications_for=="yesterday"){
    window.location="/portal/portal/tenants/notifications?filterBy=This Week&tenant="+effectiveTenantParam;
  }
  else{
    window.location="/portal/portal/tenants/notifications?filterBy=All&tenant="+effectiveTenantParam;
  }

}

function getTicketsCount(element){
  $("#spinnerDiv").show();
  $("#spinnerDiv").addClass("topgrid_loader");
  
  getTicketCountAjaxReq = $.ajax({
    type: "GET",
    url: "/portal/portal/support/homeTicketsCount",
    dataType: "html",
    data:{tenantParam:effectiveTenantParam},
    cache:false,
    success: function(html){
      $("#spinnerDiv").removeClass("topgrid_loader");
      $("#ticketsCountChart").empty();
      $("#ticketsCountChart").html(html);
    },
    error: function(xhr, ajaxOptions, thrownError){
      if (!ajaxAborted){
        $('#TicketError').removeClass('errorinvisible').find("#msg").removeClass('errorinvisible').addClass('errorinfo').html( xhr.status + ': ' + thrownError + "\n" + g_dictionary.jsGetTicketError);
      }
    }
  });
 }

function go_to_new_tickets(){
  tickets_page_path="/portal/portal/support/tickets?statusFilter=New&tenant="+effectiveTenantParam;
  window.open(tickets_page_path, '_blank');
}
function go_to_working_tickets(){
  tickets_page_path="/portal/portal/support/tickets?statusFilter=Working&tenant="+effectiveTenantParam;
  window.open(tickets_page_path, '_blank');
}
function go_to_closed_tickets(){
  tickets_page_path="/portal/portal/support/tickets?statusFilter=Closed&tenant="+effectiveTenantParam;
  window.open(tickets_page_path, '_blank');
}
function go_to_escalated_tickets(){
  tickets_page_path="/portal/portal/support/tickets?statusFilter=Escalated&tenant="+effectiveTenantParam;
  window.open(tickets_page_path, '_blank');
}

function viewUsersGravtars() {
  $("#users_gravtars_spinner_div").addClass("topgrid_loader");
  $("#users_gravtars_spinner_div").css( { marginLeft : "50%", marginTop : "0px" } );
  $.ajax({
    type : "GET",
    url : "/portal/portal/home/getGravtars?tenant=" + effectiveTenantParam,
    async: true,
    dataType : "html",
    cache : false,
    success : function(html) {
     $("#users_gravtars_spinner_div").removeClass("topgrid_loader");
     $("#users_gravtars").html(html);
    }
  });
  return false;
}


function getServiceInstanceStatus(current) {
	  var serviceInstanceUuid = null;
	  if (current == null) {
		  serviceInstanceUuid = $('#serviceInstanceHealthSelect').val();
	  } else{
		  serviceInstanceUuid = current.value;
	  }
	  
	   var url = healthUrl + "/getHealthStatusForServiceInstance";
	   if(serviceInstanceUuid!=null){
		   $.ajax( {
		      type : "GET",
		      url : url,
		      async: true,
		      data:{serviceInstanceUUID:serviceInstanceUuid},
		      dataType : "html",
		      success : function(html) {    
		    	  $("#serviceHealthChart").empty();
		          $("#serviceHealthChart").html(html);
		        
		      },error:function(){ 
		        //need to handle TO-DO
		      }
		   });
	   }
	}

$(window).bind('beforeunload',function() {
  if(getTicketAjaxReq && getTicketAjaxReq.readystate != 4){
    ajaxAborted = true; 
    getTicketAjaxReq.abort();
  }
  if(getTicketCountAjaxReq && getTicketCountAjaxReq.readystate != 4){
    ajaxAborted = true; 
    getTicketCountAjaxReq.abort();
  }
});