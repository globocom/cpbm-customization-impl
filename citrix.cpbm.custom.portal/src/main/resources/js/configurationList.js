/*Copyright (C) 2012 Citrix Systems, Inc.� All rights reserved*/

$(document).ready(function () {
  
    var current_tab_string = $('#module').val().toLowerCase() || 'configaccountmanagement';
    activateThirdMenuItem("l3_config_" + current_tab_string + "_tab");
    
    $(".toggleButton").iButton({
        labelOn: dictionary.toggleButtonEnable,
        labelOff: dictionary.toggleButtonDisable,
        change: function ($input) {
            var configId = $input.attr('id').substr(4);
            var component = $input.attr('component');
            if ($input.is(":checked")) {
                //alert($input.attr('id'));
                newvalue = true;
                var msg = dictionary.configurationEnabled;
                msg = msg.replace("{0}", component);
                var errormsg = dictionary.configurationEnableFailure;
                errormsg = errormsg.replace("{0}", component);
            } else {
                newvalue = false;
                var msg = dictionary.configurationDisabled;
                msg = msg.replace("{0}", component);
                var errormsg = dictionary.configurationDisableFailure;
                errormsg = errormsg.replace("{0}", component);
            }
            initDialogWithOK("dialog_info", 350, false);
            $("#dialog_info").dialog("option", "height", 150);
            $.ajax({

                type: "POST",
                url: "edit",
                data: {
                    id: configId,
                    value: newvalue
                },
                dataType: 'text',
                success: function (data) {
                    if (data == "success") {
                        $("#dialog_info").text(msg).dialog("open");
                    } else {

                        $("#save" + configId).attr('checked', !newvalue);
                        $("#save" + configId).iButton("repaint");
                        $("#dialog_info").text(errormsg).dialog("open");
                    }
                },

                error: function (request) {
                    $("#save" + configId).attr('checked', !newvalue);
                    $("#save" + configId).iButton("repaint");
                    $("#dialog_info").text(errormsg).dialog("open");

                }

            });
        }
    });
    
   
});

function configuration_edit_dialog(module, component) {
    $("#spinning_wheel").show();
  
    initDialog("configuration_edit_panel", 750);
    var $thisPanel = $("#configuration_edit_panel");

    $.ajax({

        type: "GET",
        url: configPath + "?component=" + component + "&module=" + module,
        data: "",
        dataType: 'html',
        success: function (html) {
          $thisPanel.find(".dialog_formcontent").html(html);
          $thisPanel.dialog({
              height: "auto",
              width: 750,
              closeText: 'hide',
              autoOpen: false,
              
          });
          $thisPanel.dialog('option', 'buttons', {
              "Close": function () {
                $(".ui-dialog-buttonpane button:contains('Save')").hide();
                  $thisPanel.find(".dialog_formcontent").html("");
                  $(this).dialog("close");

              },
              "Edit" : function (){
                $('div[id^="valuenotedit"]').hide();
                $('div[id^="valueedit"]').show();
                $(".ui-dialog-buttonpane button:contains('Edit')").hide();
                $(".ui-dialog-buttonpane button:contains('Save')").show();
              },
              "Save" : function (){
                saveAllConfigValues();
                $(".ui-dialog-buttonpane button:contains('Save')").hide();
                $(".ui-dialog-buttonpane button:contains('Edit')").show();
                },
          });
            
            dialogButtonsLocalizer($thisPanel, {
                'OK': g_dictionary.dialogClose
            });
            $(".ui-dialog-buttonpane button:contains('Save')").hide();
            $("#spinning_wheel").hide();
            $thisPanel.dialog("open");
        },

        error: function (request) {
          $("#spinning_wheel").hide();
          initDialogWithOK("dialog_info", 350, false);
          $("#dialog_info").dialog("option", "height", 150);
          $("#dialog_info").text(dictionary.configurationFetchError).dialog("open");
            
        }
    });
    
    
}
function saveAllConfigValues() {
  var configKeyValues = new Array();
  $('input[id^="value"]').each(function() {
    var configProperty = new Object();
    configProperty.name = $(this).attr("id").substr(5);
    configProperty.value = $(this).attr("value");
    configKeyValues.push(configProperty);
  }),
  // send the configKeyValues to controller
  $.ajax({
    type : "POST",
    url : "edit",
    data : {
      "configProperties" : JSON.stringify(configKeyValues),
    },
    dataType : 'text',
    success : function(data) {
      if (data == "success") {
        $('input[id^="value"]').each(function() {
          var configProperty = new Object();
          configProperty.name = $(this).attr("id").substr(5);
          configProperty.value = $(this).attr("value");
          $("#valuenotedit"+configProperty.name).html(configProperty.value);  
        });
        $('div[id^="valuenotedit"]').show();
        $('div[id^="valueedit"]').hide();
      } else {
        $('div[id^="valueerror"]').show();
      }
    },
    error : function(request) {
      
    }
  });
}