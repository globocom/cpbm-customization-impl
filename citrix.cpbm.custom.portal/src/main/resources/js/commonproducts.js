/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */ 
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