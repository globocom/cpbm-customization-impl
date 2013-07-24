 /* Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. */
$(document).ready(function() { 
  activateThirdMenuItem("l3_billing_record_deposit_tab");
  initDialog("recordDepositDiv", 420);
  $('#record_initial_deposit_button').unbind('click').bind('click', function(event){
    
    var actionurl = "/portal/portal/billing/record_deposit?tenant="+$("#tenantId").val();   
    
    $.ajax( {
      type : "GET",
      url : actionurl,
      
      dataType : "html",
      success : function(html) {
        
        $("#recordDepositDiv").html("");
        $("#recordDepositDiv").html(html);
        
        var $thisDialog = $("#recordDepositDiv");

        $thisDialog.dialog({ height: 100, width : 420 });
        $thisDialog.dialog('option', 'buttons', {
          "OK": function () {
          
          
          if($("#depositForm").valid()) {
            
            
            var newVal = $("#tenantPercentage").val();
             $.ajax( {
                type : "POST",
                url : "/portal/portal/billing/record_deposit?tenant="+$('#tenantId').val(),
                data: $('#depositForm').serialize(),
                dataType : "json",
                success : function() {
                   $thisDialog.dialog("close");
                   window.location.href="/portal/portal/billing/show_record_deposit?tenant="+$('#tenantId').val();
                  
               
                  
                },error:function(XMLHttpRequest){
                  if(XMLHttpRequest.status === INVALID_AJAX_REQUEST_ERROR_CODE){
                    alert(XMLHttpRequest.responseText);
                  }
                    displayAjaxFormError(XMLHttpRequest, "depositForm", "main_addnew_formbox_errormsg");
                }
              });
             
           }
           
          },
          "Cancel": function () {
            $(this).dialog("close");
          }
        });
        dialogButtonsLocalizer($thisDialog, {'OK':g_dictionary.dialogOK, 'Cancel': g_dictionary.dialogCancel}); 
        $thisDialog.dialog("open");
        
        
      },error:function(){ 
      }
    });
    
    
  });
  
});