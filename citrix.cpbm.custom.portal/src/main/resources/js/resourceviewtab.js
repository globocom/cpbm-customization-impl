function initializeResourceTab(tenantParam, serviceInstanceUUID) {
    

    if(!checkDelinquent(isDelinquent,redirectToBilling,redirectToDashBoard,showMakePaymentMessage,tenantParam)){
      return false;
    }
    
    
  
    	var url=$('#items_container li').first().attr("resourceUrl");
        var mode=$('#items_container li').first().attr("mode");
        var tenantParam = $("#tenantParam").val();
        var listItems = $("#items_container li");
        singleSignOn(tenantParam, serviceInstanceUUID);
        loadbody(url, mode);
      listItems.each(function(){
          $(this).unbind("click").bind("click", function(){
          $(".thirdlevel_subtab").removeClass("on").addClass("off");
          $(this).removeClass("off").addClass("on");
          url = $(this).attr("resourceUrl");
          mode = $(this).attr("mode");
          singleSignOn(tenantParam, serviceInstanceUUID);
          loadbody(url, mode);
        });
      });
  
}

function loadbody(url, mode) {
    if (mode == "IFRAME") {
      var frame=document.getElementById("serviceInstanceResourceView");
      frame.src= url;
    } else if (mode == "WINDOW") {
      window.open(url, "_blank");
    }
}