var SHOW_MESSAGE_TIME_BEFORE_PAGE_REFRESH = 3000;
$(document).ready(function() { 

  $.validator.addClassRules("j_startDate", {
    startDate : true
  });

  $.validator
  .addMethod(
      "startDate",
      function(value, element) {
          $(element).rules("add", {
            required : true
          });
          var isTodayAllowed = $("#isTodayAllowed").val();
          if(isTodayAllowed == "true"){
            var now = Date.parse($("#date_today").val());
            return Date.parse(value) >= now; 
          }else{
            var now = Date.parse($("#date_today").val());
            return Date.parse(value) > now; 
          }
      },
      commmonmessages.startDate);

  $("#planDateForm").validate({
    errorPlacement: function(error, element) {
      var name = element.attr('id');
      name = ReplaceAll(name,".","\\."); 
      if (name != "") {
        error.appendTo("#" + name + "Error");
      }
    }
  });

$(function() {   
  $('#planstartDate').datepicker({
    duration: '',
    showOn: "button",
    buttonImage: "/portal/images/calendar_icon.png",
    buttonImageOnly: true,
    buttonText: "",
    dateFormat: g_dictionary.friendlyDate,
    showTime: false,
    //minDate:new Date(new Date().getTime() + (24 * 60 * 60 * 1000)),
    minDate: new Date(Date.parse($("#date_today").val())),
    beforeShow: function(dateText, inst){ 
        $("#dialog_set_plan_date").data("height.dialog", 370);
        $("button").each(function(){
          $(this).attr("style", "margin-top: 170px;");
        });       
        $("#ui-datepicker-div").addClass("datepicker_stlying");
        var isTodayAllowed = $("#isTodayAllowed").val();
        if(isTodayAllowed == "true"){
          $(this).datepicker("option", "minDate", new Date(Date.parse($("#date_today").val())));
        } else {
          $(this).datepicker("option", "minDate", new Date(Date.parse($("#date_today").val()) + (24 * 60 * 60 * 1000)));
        }
    },
    onSelect: function(dateText, inst) {
        $(this).attr("value", dateText);
        $("#planstartDate").each(function() {
          $(this).attr("value", dateText);
        });
        $("#dialog_set_plan_date").data("height.dialog", 200);
        $("button").each(function(){
          $(this).attr('style', 'margin-top: 5px;');
        });
  },
  onClose: function(dateText, inst) {
    $("#dialog_set_plan_date").data("height.dialog", 200);
    $("button").each(function(){
      $(this).attr("style", "margin-top: 5px;");
    });
  }
 });
  
});
  
});  
function setPlanDate(action,entityName){
  initDialog("dialog_set_plan_date", 450);  
  var $thisDialog =  $("#dialog_set_plan_date");
  $thisDialog.data("height.dialog", 200);
  var actionurl = productsUrl+"setplandate";
    $.ajax( {
      type : "GET",
      url : actionurl,
      dataType : "html",
      async : false,
      success : function(html) { 
                $thisDialog.html("");
                $thisDialog.html(html);
                $thisDialog.dialog('option', 'buttons', {
                  "OK": function () {
                    var productForm = $thisDialog.find("form");
                    if($(productForm).valid()){
                       $.ajax({
                               type : "POST",
                               url : $(productForm).attr('action'),
                               data:$(productForm).serialize(),
                               dataType : "html",
                               async:false,
                               success : function(status) {
                                 if(status == "plan_date_should_be_greater_or_equal_to_today"){
                                   $thisDialog.dialog("close");
                                   popUpDialogForAlerts("alert_dialog", i18n.errors.products.activationdategreaterorequaltotoday);
                                 } else if(status == "plan_date_should_be_greater_to_today"){
                                   $thisDialog.dialog("close");
                                   popUpDialogForAlerts("alert_dialog", i18n.errors.products.activationdategreaterthantoday);
                                 } else if(status == "no_product_added"){
                                   $thisDialog.dialog("close");
                                   popUpDialogForAlerts("alert_dialog", i18n.errors.products.noproductsadded);
                                 } else{
                                   $thisDialog.dialog("close");
                                   location.reload();
                                 }
                               },
                               error:function(XMLHttpRequest){
                                 alert(i18n.errors.products.failed_set_plan_date);
                                 $thisDialog.dialog("close");
                               }
                             });
                    }
                },
                "Cancel": function () {
                  $(this).dialog("close");
                }
                });
                dialogButtonsLocalizer($thisDialog, {'OK':g_dictionary.dialogOK, 'Cancel': g_dictionary.dialogCancel});
                $thisDialog.bind( "dialogbeforeclose", function(event, ui) {
                  $("#ui-datepicker-div").hide();
                  $thisDialog.empty();
                  });
                $thisDialog.dialog("open");
              },
        error : function(){}
        });
}

function removePlannedDate(){
  initDialog("common_dialog", 350, false);
  var $commonDialog = $("#common_dialog");
  $commonDialog.find("#helptext").empty();
  $commonDialog.find("#helptext").html(commmonmessages.lightboxremoveplanneddate1);
  $("#common_dialog").dialog('option', 'buttons', getConfirmationDialogButtons("removeplanneddate")).dialog("open");
}

function getTopActionMap(){
  var topActionMap = {    
    removeplanneddate: {
      label: commmonmessages.removeplanneddate ,
      inProcessText: commmonmessages.removingplanneddate,
      type : "GET",
      dataType : "html",
      afterActionSeccessFn  : function(html) {
      //alert(commmonmessages.removedplanneddate_successfully);
      window.location.reload();
      },error:function(){ 
        alert(commmonmessages.removeplanneddate + " " + g_dictionary.actionFailed);
      }  
      },
      activatenow: {
        label: commmonmessages.activatenow ,
        inProcessText: commmonmessages.activating,
        type : "GET",
        dataType : "html",
        afterActionSeccessFn  : function(html) {
          //alert(commmonmessages.activated_successfully);
          
          window.setInterval(function reload() {
              window.location.reload();
            }, SHOW_MESSAGE_TIME_BEFORE_PAGE_REFRESH); 
        },
        error:function(){
            alert(commmonmessages.activatenow + " " + g_dictionary.actionFailed);
        },
        processXMLHttpResponse: function(XMLHttpResponse){
          if(XMLHttpResponse.status === PRECONDITION_FAILED){
            if(i18n.errors.products != undefined){
              popUpDialogForAlerts("alert_dialog", i18n.errors.products.pricesNotSetForAllProducts);
            }
            else if(i18n.errors.pricesNotSetForAllProducts != undefined){
              popUpDialogForAlerts("alert_dialog", i18n.errors.pricesNotSetForAllProducts);
            }
          }
        }
      }
  };
  return topActionMap;
}

function getConfirmationDialogButtons(command) {

    var buttonCallBacks = {};
    var actionMapItem;
    if (command == "removeproductplannedcharges") {
      actionMapItem = getTopActionMap().removeproductplannedcharges;
    }else if(command == "removebundlesplannedcharges"){
      actionMapItem = getTopActionMap().removebundlesplannedcharges;
    }else if(command == "removeplanneddate"){
      actionMapItem = getTopActionMap().removeplanneddate;
    }else if(command == "activatenow"){
      actionMapItem = getTopActionMap().activatenow;
    }
    
    buttonCallBacks[commmonmessages.lightboxbuttonconfirm] = function () {
      $(this).dialog("close");
      var apiCommand;
      if(command == "removeplanneddate"){
        apiCommand = "/portal/portal/products/removeplanneddate";
      }else if(command == "activatenow"){
        apiCommand = "/portal/portal/products/activatenow";
      }
      doActionButton(actionMapItem, apiCommand);

    };

    buttonCallBacks[commmonmessages.lightboxbuttoncancel] = function () {
      $(this).dialog("close");
    };

    return buttonCallBacks;
  }

function planCharges(entityName){
  initDialog("dialog_plan_charges", 905,600);
  var actionurl;  
  if(entityName == "products"){
    actionurl = productsUrl+"/plancharges";  
  }else{
    actionurl = productBundlesUrl+"/plancharges";  
  }
   
    $.ajax( {
      type : "GET",
      url : actionurl,
      dataType : "html",
      async:false,
      success : function(html) {      
                var $thisDialog = $("#dialog_plan_charges");
                $thisDialog.html("");
                $thisDialog.html(html);
                $thisDialog.dialog('option', 'buttons', {
                  "OK": function () {
                    var form = $thisDialog.find("form");
                    var isFormValid =  $(form).valid();
                    var hasError =  $(form).find(".priceRequired").hasClass('error');
                    if(hasError == false){
                      $(".common_messagebox").hide();
                    }else{
                      $(".common_messagebox").show();
                    }
                    if(isFormValid){
                       $.ajax( {
                               type : "POST",
                               url : $(form).attr('action'),
                               data:$(form).serialize(),
                               dataType : "html",
                               async:false,
                               success : function(status) {
                                 if(status == "success"){
                                  window.location.reload();                                  
                                 }
                                
                               },error:function(XMLHttpRequest){
                                 $thisDialog.dialog("close");                                
                                 
                               }
                             });
                    }
                 
                },
                "Cancel": function () {
                  $(this).dialog("close");
                  $("#dialog_plan_charges").empty();
                }
                });
                dialogButtonsLocalizer($thisDialog, {'OK':g_dictionary.dialogOK, 'Cancel': g_dictionary.dialogCancel});
                $thisDialog.bind( "dialogbeforeclose", function(event, ui) {
                  $thisDialog.empty();
                  });
                $thisDialog.dialog("open");
              },error:function(){                 
              }
        });
}
function activatenow(current){
  initDialog("common_dialog", 350, false);
  var $commonDialog = $("#common_dialog");
  $commonDialog.find("#helptext").empty();
  $commonDialog.find("#helptext").html(commmonmessages.lightboxactivenowconfirmmessage);
  $("#common_dialog").dialog('option', 'buttons', getConfirmationDialogButtons("activatenow")).dialog("open");
}

