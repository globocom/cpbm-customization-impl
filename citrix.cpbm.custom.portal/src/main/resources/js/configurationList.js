/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */

$(document).ready(function() {

  var current_tab_string = $('#module').val().toLowerCase() || 'configaccountmanagement';
  activateThirdMenuItem("l3_config_" + current_tab_string + "_tab");
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
    success: function(html) {
      $thisPanel.find(".dialog_formcontent").html(html);
      $thisPanel.dialog({
        height: "auto",
        width: 750,
        closeText: 'hide',
        autoOpen: false,

      });
      $thisPanel.dialog('option', 'buttons', {
        "Close": function() {
          $(".ui-dialog-buttonpane button:contains('Save')").hide();
          $thisPanel.find(".dialog_formcontent").html("");
          $(this).dialog("close");

        },
        "Edit": function() {
          $('div[id^="valuenotedit"]').hide();
          $('div[id^="valueedit"]').show();
          $(".ui-dialog-buttonpane button:contains('Edit')").hide();
          $(".ui-dialog-buttonpane button:contains('Save')").show();
        },
        "Save": function() {
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

    error: function(request) {
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
    type: "POST",
    url: "edit",
    data: {
      "configProperties": JSON.stringify(configKeyValues),
    },
    dataType: 'text',
    success: function(data) {
      if (data == "success") {
        $('input[id^="value"]').each(function() {
          var configProperty = new Object();
          configProperty.name = $(this).attr("id").substr(5);
          configProperty.value = $(this).attr("value");
          $("#valuenotedit" + configProperty.name).html(configProperty.value);
        });
        $('div[id^="valuenotedit"]').show();
        $('div[id^="valueedit"]').hide();
      } else {
        $('div[id^="valueerror"]').show();
      }
    },
    error: function(request) {

    }
  });
}
