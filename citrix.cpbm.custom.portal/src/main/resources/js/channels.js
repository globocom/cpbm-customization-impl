/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
var DEFAULT_PER_PAGE_SIZE = 5;
var DEFAULT_CURRENT_PAGE = 1;
var KEY_VALUE_ITEM_SEPERATOR = "::";

$(document).ready(function() {

  $("#catalog_history_dates").change(function() {
    if ($("#productsOrBundle").val() == "product") {
      $("#product_utility_history").find("a").attr("style", "color: #000");
      $("#product_bundle_history").find("a").attr("style", "color: #2C8BBC");
      viewCatalogHistory($("#catalog_history_dates").val(), $("#historyDateFormat").val(), "showProductHistory");
    } else {
      $("#product_utility_history").find("a").attr("style", "color: #2C8BBC");
      $("#product_bundle_history").find("a").attr("style", "color: #000");
      viewCatalogHistory($("#catalog_history_dates").val(), $("#historyDateFormat").val());
    }
  });

  // Edit channel action click event handler  ( in viewchannel)
  $("#editchannel_action").unbind("click").bind("click", function(event) {
    editChannel(event, $("li[id^='channel'].selected.channels"));
  });

  // Add channel action click event handler  ( in viewchannel)
  $("#add_channel_link").unbind("click").bind("click", function(event) {
    addChannel(event);
  });

  // Mouse-over event of action menu container ( in viewchannel)
  $("#action_menu_container").unbind("click").bind("mouseover", function(event) {
    showActionMenu(event);
  });

  // Mouse-out event of action menu container ( in viewchannel)
  $("#action_menu_container").unbind("click").bind("mouseout", function(event) {
    hideActionMenu(event);
  });

  // Delete channel action click event handler  ( in viewchannel)
  $("#deletechannel_action").unbind("click").bind("click", function(event) {
    deleteChannel(event, $("li[id^='channel'].selected.channels"));
  });
  $("#view_catalog_action").unbind("click").bind("click", function(event) {
    var channelParam = $("li[id^='channel'].selected.channels").attr('id').substr(7);
    var revisionDate = $("#catalog_history_dates").val();
    var revision = $("#currentHistoryPlanned").val();
    var dateFormat = $("#historyDateFormat").val();
    window.open("/portal/portal/channel/catalog/view_catalog?channelParam=" + channelParam + "&revision=" +
      revision + "&revisionDate=" + revisionDate + "&dateFormat=" + dateFormat, "_blank",
      "width=1000,height=850,resizable=yes,menubar=no,status=no,scrollbars=yes,toolbar=no,location=no");

  });

  // Delete channel dialog
  initDialog("dialog_delete_channel", 390);

  function deleteChannel(event, current) {
    var divId = $(current).attr('id');
    var id = divId.substr(7);
    initDialog("dialog_delete_channel", 390);
    var $thisDialog = $("#dialog_delete_channel");
    $thisDialog.data("height.dialog", 100);
    $thisDialog.dialog('option', 'buttons', {
      "OK": function() {
        var url = "/portal/portal/channels/deletechannel";
        $.ajax({
          type: "POST",
          url: url,
          async: false,
          cache: false,
          data: {
            "Id": id
          },
          dataType: "html",
          success: function(result) {
            $thisDialog.dialog("close");
            if (result == "failure") {
              popUpDialogForAlerts("alert_dialog", i18n.errors.channels.failed_delete_channel);
            } else {
              location.reload(true);
            }
          },
          error: function(XMLHttpRequest) {
            popUpDialogForAlerts("alert_dialog", i18n.errors.channels.failed_delete_channel);
          }
        });
        $(this).dialog("close");
      },
      "Cancel": function() {
        $(this).dialog("close");
      }
    });
    dialogButtonsLocalizer($thisDialog, {
      'OK': g_dictionary.dialogOK,
      'Cancel': g_dictionary.dialogCancel
    });
    $thisDialog.dialog("open");
  }

  // Edit channel dialog
  initDialog("dialog_edit_channel", 785);

  function editChannel(event, current) {
    var id = $(current).attr('id').substr(7);
    var url = "/portal/portal/channels/editchannel";
    $.ajax({
      type: "GET",
      url: url,
      cache: false,
      data: {
        Id: id
      },
      dataType: "html",
      success: function(html) {
        var $thisDialog = $("#dialog_edit_channel");
        $thisDialog.html("");
        $thisDialog.html(html);
        $thisDialog.dialog("open");
      },
      error: function() {
        // need to handle TO-DO
      }
    });
  }

  // Add Channel Dialog
  initDialog("dialog_add_channel", 785);

  function addChannel() {
    var url = "/portal/portal/channels/createchannel";
    $.ajax({
      type: "GET",
      url: url,
      cache: false,
      dataType: "html",
      success: function(html) {
        var $thisDialog = $("#dialog_add_channel");
        $thisDialog.html("");
        $thisDialog.html(html);
        $thisDialog.dialog("open");
        $('#channelName').focus();
      },
      error: function(XMLHttpResponse) {
        if (XMLHttpResponse.status === PRECONDITION_FAILED) {
          popUpDialogForAlerts("alert_dialog", i18n.errors.channels.failed_create_channel_precondition);
        }
        // need to handle TO-DO
      }
    });
  }

  // Show action menu.

  function showActionMenu(event) {
    $("#action_menu").show();
  }

  // Hide action menu.

  function hideActionMenu(event) {
    $("#action_menu").hide();
  }

  // Logo required validator
  $.validator.addClassRules("logorequired", {
    logorequired: true
  });

  // logo required validator function definition
  $.validator.addMethod(
    "logorequired",
    function(value, element) {
      if (value == "") {
        return false;
      }
      return true;
    },
    i18n.errors.channels.edit_image_path_invalid_message);

  // Logo form validator
  $("#channelLogoForm").validate({
    success: "valid",
    ignoreTitle: true,
    rules: {
      "logo": {
        required: true
      }
    },
    messages: {
      "logo": {
        required: i18n.errors.channels.edit_image_path_invalid_message
      }
    },
    errorPlacement: function(error, element) {
      var name = element.attr('id');
      name = ReplaceAll(name, ".", "\\.");
      if (name != "") {
        error.appendTo("#" + name + "Error");
      }
    }
  });

  scrollUrlHitMap = {};

});

// Reset the styling of all the items

function resetGridRowStyle() {
  $(".widget_navigationlist").each(function() {
    $(this).removeClass("selected");
    $(this).removeClass("active");
  });
}

// View Channel info bubble

function viewInfoBubble(current) {
  if ($(current).hasClass('active')) return
  $(current).find("#info_bubble").show();
}

// Clear Channel info bubble

function clearInfoBubble(current) {
  $(current).find("#info_bubble").hide();
}

// View Channel details

function viewChannel(current) {
  $("div[id^='plan_date_div']").each(function() {
    $(this).remove();
  });
  var divId = $(current).attr('id');
  var id = divId.substr(7);
  resetGridRowStyle();
  $(current).addClass("selected");
  $(current).addClass("active");
  var url = "/portal/portal/channels/viewchannel";
  $("#spinning_wheel").show();
  $.ajax({
    type: "GET",
    url: url,
    data: {
      Id: id
    },
    cache: false,
    async: false,
    dataType: "html",
    success: function(html) {
      $("#main_details_content").html("");
      $("#main_details_content").html(html);
    },
    error: function() {
      // need to handle TO-DO
    }
  });
  $("#spinning_wheel").hide();
}

// Checks if the channel name is unique or not

function validate_channelname(event, input) {
  clearErrorsBeforeValidate("channelName", "name_errormsg");
  var channelName = $(input).val().trim();
  if (input.defaultValue.trim() == channelName) {
    return;
  }
  var err_msg = "";
  if (channelName.length >= 255) {
    err_msg = i18n.errors.channels.max_length_exceeded + " 255";
  }
  if (err_msg.trim().length > 0) {
    $("#name_errormsg").text(err_msg);
    $("#name_errormsg").show();
    return;
  }
  $.ajax({
    type: "GET",
    url: "/portal/portal/channels/validate_channelname",
    data: {
      "channelName": channelName
    },
    dataType: "html",
    async: false,
    cache: false,
    success: function(result) {
      if (result == "false") {
        setErrorClassesAndShowError("channelName", "name_errormsg",
          getErrorLabel(i18n.errors.channels.channel_name_not_unique));
      }
    },
    error: function(html) {
      setErrorClassesAndShowError("channelName", "name_errormsg",
        getErrorLabel(html));
    }
  });
}

// Checks if the channel code is unique or not

function validate_channelcode(event, input) {
  clearErrorsBeforeValidate("channelCode", "code_errormsg");
  var channelCode = $(input).val().trim();
  if (input.defaultValue.trim() == channelCode) {
    return;
  }
  var err_msg = "";
  if (channelCode.length >= 64) {
    err_msg = i18n.errors.channels.max_length_exceeded + " 64";
  }

  if (channelCode.length > 0 && !/^[a-zA-Z0-9_:\[\]-]+$/.test(channelCode)) {
    err_msg = i18n.errors.channels.code_invalid;
  }
  if (channelCode.trim().length == 0) {
    return;
  }

  if (err_msg.trim().length > 0) {
    setErrorClassesAndShowError("channelCode", "code_errormsg",
      getErrorLabel(err_msg));
    return;
  }
  $.ajax({
    type: "GET",
    url: "/portal/portal/products/validateCode",
    data: {
      "channelCode": channelCode
    },
    dataType: "html",
    async: false,
    cache: false,
    success: function(result) {
      if (result == "false") {
        setErrorClassesAndShowError("channelCode", "code_errormsg",
          getErrorLabel(i18n.errors.channels.channel_code_not_unique));
      }
    },
    error: function(html) {
      setErrorClassesAndShowError("channelCode", "code_errormsg",
        getErrorLabel(html));
    }
  });
}

// When next is clicked on the left side panel "Next". We are right now showing only 14 entries

function nextClick() {
  var $currentPage = $('#current_page').val();
  window.location = "/portal/portal/channels/list?page=" + (parseInt($currentPage) + 1);
}

// When Previous is clicked on the left side panel "Prev". We are right now showing only 14 entries

function previousClick() {
  var $currentPage = $('#current_page').val();
  window.location = "/portal/portal/channels/list?page=" + (parseInt($currentPage) - 1);
}

// Channel description validator. We limit description to 255 characters.

function validate_channeldesc(event, input) {
  clearErrorsBeforeValidate("channelDescription", "description_errormsg");
  var channelDesc = $(input).val().trim();
  if (channelDesc.length > 255) {
    $("#description_errormsg").text(i18n.errors.channels.max_length_exceeded + " 255");
    $("#description_errormsg").show();
  }
}

// Checks if the value is a valid non-negative number

function isValidNonNegativeNo(value) {
  return value != "" && isNaN(value) == false && Number(value) >= 0 && Number(value) <= 99999999.9999;
}

function isMorethanFourDecimalDigits(value) {
  return !/^(?:\d*\.\d{1,4}|\d+)$/.test(value);
}

// Edit the bundle charges, as in over-riding bundle charges at the catalog level

function editBundleCharges(event, current) {
  initDialog("dialog_edit_bundle_charges", 850, 200);
  var $thisDialog = $("#dialog_edit_bundle_charges");
  $thisDialog.html("");
  var channelId = $("li[id^='channel'].selected.channels").attr('id').substr(7);
  var bundleId = $(current).attr('bundleId');
  var url = "/portal/portal/channels/editcatalogproductbundlepricing";
  $.ajax({
    type: "GET",
    url: url,
    data: {
      "channelId": channelId,
      "bundleId": bundleId
    },
    cache: false,
    dataType: "html",
    success: function(html) {
      $thisDialog.html(html);
      $thisDialog.find("#error_div").hide();
      $thisDialog.find("#priceError").html("");
      $thisDialog.dialog('option', 'buttons', {
        "OK": function() {
          var currencyVals = new Array();
          var inError = false;
          $('input[id^="currencyValsWeNeed"][class^="text"]').each(function() {
            if (!isValidNonNegativeNo($(this).attr("value").trim())) {
              $thisDialog.find("#error_div").show();
              $thisDialog.find("#priceError").html(i18n.errors.channels.validPriceRequired);
              $(this).addClass("error");
              $(this).css("background-color", "#FFE4E4");
              $(this).css("background-image", "none");
              inError = true;
              return;
            }
            if (isMorethanFourDecimalDigits($(this).attr("value").trim())) {
              $thisDialog.find("#error_div").show();
              $thisDialog.find("#priceError").html(i18n.errors.channels.max_four_decimal_value);
              $(this).addClass("error");
              $(this).css("background-color", "#FFE4E4");
              $(this).css("background-image", "none");
              inError = true;
              return;
            }
            var currencyObj = new Object();
            currencyObj.previousvalue = $(this).attr("previousvalue");
            currencyObj.value = $(this).attr("value");
            currencyObj.currencycode = $(this).attr("currencycode");
            currencyObj.currencyId = $(this).attr("currencyId");
            currencyObj.isRecurring = $(this).attr("isRecurring");
            currencyVals.push(currencyObj);
          });
          if (inError) {
            return;
          }
          $("#spinning_wheel").show();
          $.ajax({
            type: "POST",
            url: url,
            data: {
              "currencyValData": JSON.stringify(currencyVals),
              "channelId": channelId,
              "bundleId": bundleId
            },
            dataType: "json",
            async: false,
            cache: false,
            success: function(productCharges) {
              $thisDialog.find("#priceError").html("");
              $thisDialog.find("#error_div").hide();
              $thisDialog.html("");
              $thisDialog.dialog("close");
              viewCatalogPlanned();
            },
            error: function(XMLHttpRequest) {
              if (XMLHttpRequest.status === CODE_NOT_UNIQUE_ERROR_CODE) {
                popUpDialogForAlerts("alert_dialog", i18n.errors.common.codeNotUnique);
              } else {
                // TODO
              }
            }
          });
          $("#spinning_wheel").hide();
        },
        "Cancel": function() {
          $thisDialog.dialog("close");
        }
      });
      dialogButtonsLocalizer($thisDialog, {
        'OK': g_dictionary.dialogOK,
        'Cancel': g_dictionary.dialogCancel
      });
      $thisDialog.dialog("open");
    },
    error: function() {
      // need to handle TO-DO
    }
  });
}

// Handler to view the catalog's current bundle / utility rate card charges. By default, bundle is selected

function viewCatalogCurrent() {
  $('#details_tab').removeClass('active').addClass("nonactive");
  $('#currencies_tab').removeClass('active').addClass("nonactive");
  $('#catalog_tab').removeClass('nonactive').addClass("active");
  $('#details_content').hide();
  $('#currencies_content').hide();

  $('#catalog_links').show();
  $("#second_line_under_planned").hide();
  $("#catalog_content").show();
  $('#action_currency').hide();
  $('#plan_date_picker').hide();

  $("#catalog_current_tab").find("a").attr("style", "color: #000");
  $("#catalog_planned_tab").find("a").attr("style", "color: #2C8BBC");
  $("#catalog_history_tab").find("a").attr("style", "color: #2C8BBC");
  $("#catalog_current_tab").closest(".widget_details_actionbox").attr("style", "height: auto");
  $("#catalog_content").html("");
  var channelId = $("li[id^='channel'].selected.channels").attr('id').substr(7);
  var url = "/portal/portal/channels/viewcatalogcurrent";
  $("#spinning_wheel").show();
  $.ajax({
    type: "GET",
    url: url,
    data: {
      channelId: channelId,
      page: DEFAULT_CURRENT_PAGE,
      perPage: DEFAULT_PER_PAGE_SIZE
    },
    cache: false,
    async: false,
    dataType: "html",
    success: function(html) {
      $("#catalog_content").html(html);
      $("#second_line_under_planned").show();
      if ($("#currentEffectiveDate").val() != "") {
        $("#second_line_under_planned").find("#effective_date").text($("#currentEffectiveDate").val());
      }
    },
    error: function() {
      // need to handle TO-DO
    }
  });
  $("#spinning_wheel").hide();
}

// Handler to edit ( add ) the currencies that the channel is going to support

function editCurrencies(event, current) {
  initDialog("dialog_edit_currencies", 665, 600);
  var $thisDialog = $("#dialog_edit_currencies");
  var id = $("li[id^='channel'].selected.channels").attr('id').substr(7);
  var url = "/portal/portal/channels/editchannelcurrency";
  $.ajax({
    type: "GET",
    url: url,
    data: {
      "channelId": id
    },
    cache: false,
    dataType: "html",
    success: function(html) {
      $thisDialog.html("");
      $thisDialog.html(html);
      $thisDialog.dialog('option', 'buttons', {
        "OK": function() {
          var id = $("li[id^='channel'].selected.channels").attr('id').substr(7);
          var currencyCodeArray = new Array();
          var currencySignArray = new Array();
          $($("#currencies_add").find('.widget_checkbox')).each(function() {
            if ($(this).find("span").attr("class") == "checked") {
              currencyCodeArray.push($(this).attr('currCode'));
              currencySignArray.push([$(this).attr("currCode"), $(this).attr("currSign"), $(this).attr('currName')]);
            }
          });
          if (currencyCodeArray.length == 0) {
            $(this).dialog("close");
            return;
          }
          $.ajax({
            type: "POST",
            url: url,
            data: {
              "channelId": id,
              "currencyCodeArray": JSON.stringify(currencyCodeArray)
            },
            cache: false,
            async: false,
            dataType: "html",
            success: function(html) {
              var htmlStr = "";
              var alreadySuportedCurrencies = $("#currency_row_container").find('.widget_currencyflag').length;
              for (var i = 0; i < currencySignArray.length; i++) {
                var thisCurrArr = currencySignArray[i];
                htmlStr += '<div class="widget_grid details ';
                if ((i + alreadySuportedCurrencies - 1) % 2) {
                  htmlStr += "odd";
                } else {
                  htmlStr += "even";
                }
                htmlStr += '"><div class="widget_checkbox"><span class="checked"></span></div>';
                htmlStr += '<div class="widget_grid_description" style="border:none;"><span><strong>';
                htmlStr += thisCurrArr[1] + ' - ' + thisCurrArr[2] + '</strong></span>';
                htmlStr += '</div><div class="widget_flagbox"><div class="widget_currencyflag">';
                htmlStr += '<img src="../../images/flags/' + thisCurrArr[0] + '.gif" alt="" /></div></div></div>';
                if ($("#channeCurrencies" + id).html().length > 0) {
                  $("#channeCurrencies" + id).html($("#channeCurrencies" + id).html() + ", " + thisCurrArr[0]);
                }
                if ($("li[id^='channel'].selected.channels").find("#channel_currencies").html().length > 0) {
                  $("li[id^='channel'].selected.channels").find("#channel_currencies").html(
                    $("li[id^='channel'].selected.channels").find("#channel_currencies").html() + ", " +
                    thisCurrArr[0]);
                }
              }
              $("#currency_row_container").html($("#currency_row_container").html() + htmlStr);
            },
            error: function() {}
          });
          $(this).dialog("close");
        },
        "Cancel": function() {
          $(this).dialog("close");
        }
      });
      dialogButtonsLocalizer($thisDialog, {
        'OK': g_dictionary.dialogOK,
        'Cancel': g_dictionary.dialogCancel
      });
      $thisDialog.dialog("open");
    },
    error: function() {
      // need to handle TO-DO
    }
  });
}

// Handler to view the catalog's current bundle / utility rate planned charges. By default, bundle is selected

function viewCatalogPlanned() {
  $("#catalog_content").html("");
  $("#second_line_under_planned").hide();
  var channelId = $("li[id^='channel'].selected.channels").attr('id').substr(7);
  var editpriceisvalid = "0";
  if ($("#planning_for_first_time").attr("value") == "") {
    editpriceisvalid = "1";
  }
  var url = "/portal/portal/channels/viewcatalogplanned";
  $("#spinning_wheel").show();
  $.ajax({
    type: "GET",
    url: url,
    data: {
      channelId: channelId,
      page: DEFAULT_CURRENT_PAGE,
      perPage: DEFAULT_PER_PAGE_SIZE,
      editpriceisvalid: editpriceisvalid
    },
    cache: false,
    async: false,
    dataType: "html",
    success: function(html) {
      $("#catalog_content").html(html);
      $("#second_line_under_planned").show();
      if ($("#currentEffectiveDate").val() != "") {
        $("#second_line_under_planned").find("#effective_date").text($("#currentEffectiveDate").val());
      }
    },
    error: function() {
      // need to handle TO-DO
    }
  });
  $("#spinning_wheel").hide();
}

// Handler to view the catalog's history of bundle / utility rate card charges. By default, bundle is selected

function viewCatalogHistory(historyDate, format, showProductHistory) {
  $('#details_tab').removeClass('active').addClass("nonactive");
  $('#currencies_tab').removeClass('active').addClass("nonactive");
  $('#catalog_tab').removeClass('nonactive').addClass("active");
  $('#details_content').hide();
  $('#currencies_content').hide();

  $('#catalog_links').show();
  $("#catalog_content").show();
  $("#second_line_under_planned").hide();
  $('#action_currency').hide();
  $('#plan_date_picker').hide();

  $("#catalog_current_tab").find("a").attr("style", "color: #2C8BBC");
  $("#catalog_planned_tab").find("a").attr("style", "color: #2C8BBC");
  $("#catalog_history_tab").find("a").attr("style", "color: #000");
  $("#catalog_history_tab").closest(".widget_details_actionbox").attr("style", "height: 30px");

  $("#catalog_content").html("");
  var channelId = $("li[id^='channel'].selected.channels").attr('id').substr(7);
  var inputData = {
    channelId: channelId
  };
  if (historyDate != null) {
    inputData = {
      channelId: channelId,
      historyDate: historyDate,
      dateFormat: format,
      showProductHistory: showProductHistory
    };
  }
  var url = "/portal/portal/channels/viewcataloghistory";
  $("#spinning_wheel").show();
  $.ajax({
    type: "GET",
    url: url,
    data: inputData,
    cache: false,
    dataType: "html",
    async: false,
    success: function(html) {
      $("#catalog_content").html(html);
    },
    error: function() {
      // need to handle TO-DO
    }
  });
  $("#spinning_wheel").hide();
}

// Handler to edit the catalog's utility rate card charges

function editCatalogProductCharges() {
  initDialog("dialog_product_pricing_edit", 850, 600);
  var $thisDialog = $("#dialog_product_pricing_edit");
  $thisDialog.html("");
  $thisDialog.find("#error_div").hide();
  $thisDialog.find("#priceError").html("");

  var channelId = $("li[id^='channel'].selected.channels").attr('id').substr(7);
  var actionurl = "/portal/portal/channels/editcatalogproductpricing";
  $.ajax({
    type: "GET",
    url: actionurl,
    dataType: "html",
    async: false,
    cache: false,
    data: {
      "channelId": channelId
    },
    dataType: "html",
    success: function(html) {
      $thisDialog.html(html);
      $thisDialog.find("#priceError").html("");
      $thisDialog.dialog('option', 'buttons', {
        "OK": function() {
          var currencyVals = new Array();
          var inError = false;
          $('input[id^="currencyValsWeNeed"][class^="text"]').each(function() {
            if (!isValidNonNegativeNo($(this).attr("value").trim())) {
              $thisDialog.find("#error_div").show();
              $thisDialog.find("#priceError").html(i18n.errors.channels.validPriceRequired);
              $(this).addClass("error");
              $(this).css("background-color", "#FFE4E4");
              $(this).css("background-image", "none");
              inError = true;
              return;
            }
            if (isMorethanFourDecimalDigits($(this).attr("value").trim())) {
              $thisDialog.find("#error_div").show();
              $thisDialog.find("#priceError").html(i18n.errors.channels.max_four_decimal_value);
              $(this).addClass("error");
              $(this).css("background-color", "#FFE4E4");
              $(this).css("background-image", "none");
              inError = true;
              return;
            }
            var currencyObj = new Object();
            currencyObj.previousvalue = $(this).attr("previousvalue");
            currencyObj.value = $(this).attr("value");
            currencyObj.currencycode = $(this).attr("currencycode");
            currencyObj.currencyId = $(this).attr("currencyId");
            currencyObj.productId = $(this).attr("productId");
            currencyVals.push(currencyObj);
          });
          if (inError) {
            return;
          }
          $("#spinning_wheel").show();
          $.ajax({
            type: "POST",
            url: actionurl,
            data: {
              "currencyValData": JSON.stringify(currencyVals),
              "channelId": channelId
            },
            dataType: "json",
            async: false,
            cache: false,
            success: function(productCharges) {
              $thisDialog.find("#priceError").html("");
              $thisDialog.html("");
              $thisDialog.dialog("close");
              viewCatalogPlanned();
            },
            error: function(XMLHttpRequest) {
              if (XMLHttpRequest.status === CODE_NOT_UNIQUE_ERROR_CODE) {
                popUpDialogForAlerts("alert_dialog", i18n.errors.common.codeNotUnique);
              } else {
                // To do
              }
            }
          });
          $("#spinning_wheel").hide();
        },
        "Cancel": function() {
          $thisDialog.html("");
          $(this).dialog("close");
        }
      });
      dialogButtonsLocalizer($thisDialog, {
        'OK': g_dictionary.dialogOK,
        'Cancel': g_dictionary.dialogCancel
      });
      $thisDialog.dialog("open");
    },
    error: function() {}
  });
}

// Handler to attach ( add ) a bundle to the catalog. Only published bundles will be listed. 

function attachProductBundle() {
  initDialog("attach_product_bundle", 500, 200);
  var $thisDialog = $("#attach_product_bundle");
  //$thisDialog.data("height.dialog", 400);
  $thisDialog.html("");

  var id = $("li[id^='channel'].selected.channels").attr('id').substr(7);
  var url = "/portal/portal/channels/listbundles";
  $.ajax({
    type: "GET",
    url: url,
    data: {
      "channelId": id
    },
    cache: false,
    dataType: "html",
    success: function(html) {
      $thisDialog.html(html);
      $thisDialog.dialog('option', 'buttons', {
        "OK": function() {
          var selectProductBundles = new Array();
          $($thisDialog.find('.widget_checkbox')).each(function() {
            if ($(this).find("span").attr("class") == "checked") {
              selectProductBundles.push($(this).attr('bundleId'));
            }
          });
          $("#spinning_wheel").show();
          $.ajax({
            type: "POST",
            url: "/portal/portal/channels/attachproductbundles",
            data: {
              "channelId": id,
              "selectProductBundles": JSON.stringify(selectProductBundles)
            },
            cache: false,
            dataType: "html",
            success: function(html) {
              viewCatalogPlanned();
            },
            error: function() {}
          });
          $("#spinning_wheel").hide();
          $(this).dialog("close");
        },
        "Cancel": function() {
          $thisDialog.dialog("close");
        }
      });
      dialogButtonsLocalizer($thisDialog, {
        'OK': g_dictionary.dialogOK,
        'Cancel': g_dictionary.dialogCancel
      });
      $thisDialog.dialog("open");
    },
    error: function() {
      // need to handle TO-DO
    }
  });
}

// Handler to search the channel on text entered. Search right now is text*
var searchRequest;

function searchChannelByName(event) {
  $("#click_previous").unbind("click", previousClick);
  $("#click_previous").addClass("nonactive");

  currentPage = 1;
  var searchPattern = $("#channelSearchPanel").val();

  $("#click_next").removeClass("nonactive");
  $("#click_next").unbind("click").bind("click", nextClick);

  var url = "/portal/portal/channels/searchchannel?currentPage=" + currentPage + "&namePattern=" + todb(searchPattern);
  if (searchRequest && searchRequest.readyState != 4) {
    searchRequest.abort();
  }
  searchRequest = $.ajax({
    type: "GET",
    url: url,
    dataType: "html",
    async: true,
    cache: false,
    success: function(html) {
      $("#channellistdiv").html(html);
      $("#channelgridcontent").find(".channel:first").click();
    },
    error: function() {
      //need to handle
    }
  });
}

// Handler of mouse over on " Included Usage" in bundle view 
$("a[id^='moreOf']").bind("mouseover", function(event) {
  var count = $(this).attr("count");
  $("#moreEntitlements" + count).show();
});

// Handler of mouse out on " Included Usage" in bundle view
$("a[id^='moreOf']").bind("mouseout", function(event) {
  var count = $(this).attr("count");
  $("#moreEntitlements" + count).hide();
});

// When "Bundles" under current is clicked
$("#product_bundle_current").unbind("click").bind("click", function(event) {
  $("#product_bundle_current").find("a").attr("style", "color: #000");
  $("#product_utility_current").find("a").attr("style", "color: #2C8BBC");

  $('#catalog_productbundle_current').show();
  $('#catalog_products_current').hide();
});

// When "Utility rate card" under current is clicked
$("#product_utility_current").unbind("click").bind("click", function(event) {
  $("#product_utility_current").find("a").attr("style", "color: #000");
  $("#product_bundle_current").find("a").attr("style", "color: #2C8BBC");

  $('#catalog_products_current').show();
  $('#catalog_productbundle_current').hide();
});

// When action icon mouse-over on a bundle ( under planned ) is done 
$("div[id^='action_per_product_bundle']").bind("mouseover", function(event) {
  $(this).find('#per_bundle_action_menu').show();
});

// When action icon mouse-out on a bundle ( under planned ) is done 
$("div[id^='action_per_product_bundle']").bind("mouseout", function(event) {
  $(this).find('#per_bundle_action_menu').hide();
});

// When action icon mouse-over on utility rate card ( under planned ) view is done 
$("#plan_product_charges").bind("mouseover", function(event) {
  $(this).find('#product_action_menu').show();
});

// When action icon mouse-out on utility rate card ( under planned ) view is done 
$("#plan_product_charges").bind("mouseout", function(event) {
  $(this).find('#product_action_menu').hide();
});

// When action icon mouse-over is done in area before listing of bundles ( under planned ) is done 
$("#bundle_level_menu").bind("mouseover", function(event) {
  $(this).find('#bundle_add_menu').show();
});

// When action icon mouse-out is done in area before listing of bundles ( under planned ) is done
$("#bundle_level_menu").bind("mouseout", function(event) {
  $(this).find('#bundle_add_menu').hide();
});

// Dialog box to show the entitlements of a bundle

function viewEntitlements(event, current) {
  var bundleId = $(current).attr("productbundleid");

  initDialog("entitlements_dialog_" + bundleId, 400);
  var $thisDialog = $("#entitlements_dialog_" + bundleId);
  $thisDialog.data("height.dialog", 400);
  $thisDialog.show();
  $thisDialog.bind('dialogclose', function(event) {
    $("#entitlements_dialog_" + bundleId).hide();
  });
  $thisDialog.dialog('option', 'buttons', {
    "Cancel": function() {
      $thisDialog.dialog("close");
    }
  });
  dialogButtonsLocalizer($thisDialog, {
    'Cancel': g_dictionary.dialogCancel
  });
  $thisDialog.dialog("open");
}

// Handler to update the logo details of a channel when an image is successfully set for the same

function updatechannellogodetails(current) {
  response = $(current).contents().find('body');
  if (response == null || response == "null" || response == "") {
    popUpDialogForAlerts("alert_dialog", i18n.errors.channels.failed_upload_image);
    return;
  }
  try {
    var pre = response.children('pre');
    if (pre.length) response = pre.eq(0);
    returnReponse = $.parseJSON(response.html());
    var date = new Date();
    $("#channelimage" + returnReponse.id).attr('src', "/portal/portal/logo/channel/" + returnReponse.id + "?t=" + date.getMilliseconds());
  } catch (e) {
    popUpDialogForAlerts("alert_dialog", response.html());
  }

}

// Handler to change the logo of a channel

function editChannelImage(current, channelId) {
  initDialog("dialog_edit_channel_image", 550);
  $.ajax({
    type: "GET",
    url: "/portal/portal/channels/editlogo",
    data: {
      channelId: channelId
    },
    async: false,
    dataType: "html",
    success: function(html) {
      var $thisDialog = $("#dialog_edit_channel_image");
      $thisDialog.empty();
      $thisDialog.html(html);
      $thisDialog.dialog('option', 'buttons', {
        "OK": function() {
          if ($('#channelLogoForm').valid()) {
            $('#channelLogoForm').iframePostForm({
              iframeID: 'channelLogoForm-iframe-post-form',
              json: true,
              post: function() {
                $("#channelLogoForm-iframe-post-form").hide();
                return true;
              },
              complete: function() {
                updatechannellogodetails($("#channelLogoForm-iframe-post-form"));
              }
            });
            $('#channelLogoForm').submit();
            $thisDialog.dialog('close');
          }
        },
        "Cancel": function() {
          $("#dialog_edit_channel_image").empty();
          $thisDialog.dialog('close');
        }
      });
      dialogButtonsLocalizer($thisDialog, {
        'OK': g_dictionary.dialogOK,
        'Cancel': g_dictionary.dialogCancel
      });
      $thisDialog.bind("dialogbeforeclose", function(event, ui) {
        $thisDialog.empty();
      });
      $thisDialog.dialog("open");
    },
    error: function() {}
  });
}

// Function to check if a string is valid, as in length doesn't exceed maximum, and length is not 0 and quotes aren't there

function isValidString(value, maxLength) {
  if (maxLength == undefined) {
    maxLength = 65537;
  }
  var isValid = true;
  if (value == null || value.length == 0) { //required field   
    errMsg = g_dictionary.required;
    isValid = false;
  } else if (value != null && value.length >= maxLength) {
    errMsg = g_dictionary.maximum + ": " + max + " character";
    isValid = false;
  } else if (value != null && value.indexOf('"') != -1) {
    errMsg = g_dictionary.doubleQuotesNotAllowed;
    isValid = false;
  }
  return isValid;
}

// Sets the error classes in the channel add / edit wizard and show the errors

function setErrorClassesAndShowError(entityDivId, errorDivId, message) {
  $("#" + entityDivId).addClass("error");
  if ($("#" + errorDivId).find("label.error").length == 0) {
    $("#" + errorDivId).append(message);
  }
  $("#" + errorDivId).show();
}

// The error message label to be appended

function getErrorLabel(errorMsg) {
  return '<label class="error" style="display: block;">' + errorMsg + '</label>';
}

// Clears the errors and removes error class just before validation is done so that we are not left
// with errors even if validation succeeds. If it doesn't get validated again, the setErrorClassesAndShowError
// above will set the error again.

function clearErrorsBeforeValidate(entityDivId, errorDivId) {
  $("#" + errorDivId).find(".error").remove();
  $("#" + errorDivId).text("");
  $("#" + entityDivId).removeClass("error");
}

// Checks if the channel form is valid. Checks for the correctness of name, code and currency selection. Atleast
// one currency needs to be selected.

function isChannelFormValid(channelForm, currentstep) {
  var name_err_msg = $(channelForm).find("#name_errormsg").text().trim();
  var code_err_msg = $(channelForm).find("#code_errormsg").text().trim();

  var isValidName = true;
  isValidName &= isValidString($(channelForm).find("#channelName").val());
  if ($(channelForm).find("#channelName").val().trim() == "") {
    setErrorClassesAndShowError("channelName", "name_errormsg", getErrorLabel(i18n.errors.channels.name));
  } else if (name_err_msg.length > 0) {
    setErrorClassesAndShowError("channelName", "name_errormsg", "");
  }

  var isValidCode = isValidString($(channelForm).find("#channelCode").val());
  if ($(channelForm).find("#channelCode").val().trim() == "") {
    var setCodeError = true;
    if ($(channelForm).find("#channelCode").attr("prevValue") != undefined) {
      if ($(channelForm).find("#channelCode").attr("prevValue") == "") {
        setCodeError = false;
        isValidCode = true;
      }
    }
    if (setCodeError) {
      setErrorClassesAndShowError("channelCode", "code_errormsg", getErrorLabel(i18n.errors.channels.code));
    }
  } else if (code_err_msg.length > 0) {
    setErrorClassesAndShowError("channelCode", "code_errormsg", "");
  }


  var isValidCurrency = true;
  var isChannelCurrErrMsgThere = false;
  clearErrorsBeforeValidate("currencyList", "currency_errormsg");
  if (currentstep == 'step2') {
    if ($(channelForm).find("#currency_row_container").find('.widget_checkbox').find(".checked").length == 0) {
      isValidCurrency = false;
      setErrorClassesAndShowError("currencyList", "currency_errormsg", getErrorLabel(i18n.errors.channels.channel_currency_required));
    }
    if ($(channelForm).find("#currency_errormsg").text().trim().length > 0) {
      isChannelCurrErrMsgThere = true;
    }
  }
  if (!isValidName || !isValidCode || !isValidCurrency || $("#name_errormsg").text().trim().length > 0 ||
    $(channelForm).find("#code_errormsg").text().trim().length > 0 ||
    $(channelForm).find("#description_errormsg").text().trim().length > 0 ||
    isChannelCurrErrMsgThere) {
    return false;
  }
  return true;
}

// When "Prev" is clicked in edit channel dialog

function addEditChannelPrevious(current) {
  var prevStep = $(current).parents(".j_channelspopup").find('#prevstep').val();
  if (prevStep != "") {
    $(".j_channelspopup").hide();
    $("#" + prevStep).show();
  }
}

// Get back to channel details' edit ( in both channel add / edit wizard ) 

function backToChannelDetails(current) {
  $(current).parents(".j_channelspopup").hide();
  $("#step1").show();
}

// Get back to currency selection ( in create dialog )

function backTourrencySelection(current) {
  $(current).parents(".j_channelspopup").hide();
  $("#step2").show();
}

// Adds channel details in left panel when a new channel is created. We make the channel created the very first entry in the view

function addChannelDetailsInListView(channel, currencies) {
  var $channelListTemplate = $("#channelleftviewtemplate").clone();
  $channelListTemplate.attr('id', "channel" + channel.id);
  var isOdd = $("#channelgridcontent").find(".widget_navigationlist.channels:first").hasClass('odd');
  if (isOdd) {
    $channelListTemplate.addClass('even');
  } else {
    $channelListTemplate.addClass('odd');
  }
  $channelListTemplate.addClass('selected');
  $channelListTemplate.addClass('active');
  $channelListTemplate.find(".widget_navtitlebox").find('.title').text(channel.name);

  $channelListTemplate.find(".widget_navtitlebox").find('.subtitle').text(currencies);
  $channelListTemplate.find(".widget_info_popover").find('.raw_contents').find("#info_bubble_displayname").find(
    '.raw_contents_value').find("#value").text(channel.name);
  $channelListTemplate.find(".widget_info_popover").find('.raw_contents').find("#info_bubble_code").find(
    '.raw_contents_value').find("#value").text(channel.code);
  $channelListTemplate.find(".widget_info_popover").find('.raw_contents').find("#info_bubble_code").find(
    '.raw_contents_value').find("#value").text(channel.code);

  $channelListTemplate.show();
  $("#channelgridcontent").prepend($channelListTemplate);
  var channelsCount = $("#channelgridcontent").find(".channels.widget_navigationlist").size();
  //remove last element if count grater than pagination value
  if (channelsCount > 14) {
    $("#channelgridcontent").find(".widget_navigationlist.channels:last").remove();
  }
  //reset styling
  resetGridRowStyle();
  $("#channelgridcontent").find(
    ".channels:first").click();
}

// Gets currencies selected

function getCurrencyArray(channelForm) {
  var currencyCodeArray = new Array();
  $(channelForm).find("#currency_row_container").find('.widget_checkbox').each(function() {
    if ($(this).find("span").attr("class") == "checked") {
      currencyCodeArray.push($(this).attr('currCode'));
    }
  });
  return currencyCodeArray;
}

// Gets a comma separated supported currency codes' string, as in like "INR, USD, EUR" 

function getCurrencyString(currencyArray) {
  var currencies = "";
  var first = 1;

  for (var i = 0; i < currencyArray.length; i++) {
    if (first == 1) {
      first = 0;
    } else {
      if (i != currencyArray.length) {
        currencies += ", ";
      } else {
        currencies;
      }
    }
    currencies += currencyArray[i];
  }
  return currencies;
}

// When "Next" is clicked in channel create wizard

function addChannelNext(current) {
  var currentstep = $(current).parents(".j_channelspopup").attr('id');
  var $currentstep = $("#" + currentstep);
  var nextstep = $currentstep.find("#nextstep").val();
  var channelForm = $(current).closest("form");
  var url = "/portal/portal/channels/createchannel";
  $thisDialog = $("#dialog_add_channel");
  $thisDialog.bind("dialogbeforeclose", function(event, ui) {
    $thisDialog.empty();
  });

  if (isChannelFormValid(channelForm, currentstep)) {
    if (currentstep == "step2") {
      $(channelForm).find("#conf_name").text($("#channelName").val());
      $(channelForm).find("#conf_name").attr("title", $("#channelName").val());
      $(channelForm).find("#conf_code").text($("#channelCode").val());
      $(channelForm).find("#conf_code").attr("title", $("#channelCode").val());
      $(channelForm).find("#conf_channel_description").text($("#channelDescription").val());
      $(channelForm).find("#conf_channel_description").attr("title", $("#channelDescription").val());
      $(channelForm).find("#conf_currencies").text(getCurrencyString(getCurrencyArray(channelForm)));
    }
    if (currentstep == "step3") {

      var channelName = $("#channelName").val();
      var channelNameToDisplay = "<br>";
      var size = channelName.length;
      var maxsize = 50;
      var count = 0;
      while (size > 50) {
        channelNameToDisplay += channelName.substring(count, count + maxsize) + "<br>";
        count = count + maxsize;
        size = size - 50;
      }

      channelNameToDisplay += channelName.substring(count) + "<br>";

      $("#step4").find("#successmessage").append(channelNameToDisplay);
      $("#spinning_wheel5").show();
      $.ajax({
        type: "POST",
        url: url,
        cache: false,
        data: {
          "channelName": $thisDialog.find("#channelName").val(),
          "description": $thisDialog.find("#channelDescription").val(),
          "code": $thisDialog.find("#channelCode").val(),
          "currencyList": getCurrencyArray(channelForm)
        },
        success: function(channel) {
          addChannelDetailsInListView(channel, getCurrencyString(getCurrencyArray(channelForm)));
          $(".j_channelspopup").hide();
          $("#non_list").hide();
          $("#" + nextstep).show();

        },
        error: function(XMLHttpRequest) {
          if (XMLHttpRequest.status === AJAX_FORM_VALIDATION_FAILED_CODE) {
            displayAjaxFormError(XMLHttpRequest,
              "channelForm",
              "main_addnew_formbox_errormsg");
          } else if (XMLHttpRequest.status === CODE_NOT_UNIQUE_ERROR_CODE) {
            popUpDialogForAlerts("alert_dialog", i18n.errors.channels.channel_name_not_unique);
          } else {
            popUpDialogForAlerts("alert_dialog", i18n.errors.channels.failed_create_channel);
          }
        },
        complete: function() {
          $("#spinning_wheel5").hide();
        }
      });

    } else if (currentstep == "step4") {
      $thisDialog.dialog("close");
      $thisDialog.find(".dialog_formcontent").empty();
      $thisDialog.find(".dialog_formcontent").empty();
    } else {
      $(".j_channelspopup").hide();
      $("#" + nextstep).show();
    }
  }
}

// Updates the channel listing in the left hand panel of view channel

function editChannelDetailsInListViiew(channelId, channelName, channelCode) {
  var $chanelEdited = $("#channelgridcontent").find("#channel" + channelId);
  $chanelEdited.find(".widget_navtitlebox").find(".title").text(channelName);
  $chanelEdited.find(".widget_info_popover").find('.raw_contents').find("#info_bubble_displayname").find(
    '.raw_contents_value').find("#value").text(channelName);
  $chanelEdited.find(".widget_info_popover").find('.raw_contents').find("#info_bubble_code").find('.raw_contents_value')
    .find("#value").text(channelCode);
  $("#channelgridcontent").find("#channel" + channelId).click();
}

// When "Next" is clcked in the channel "edit' wizard 

function editChannelNext(current) {
  var currentstep = $(current).parents(".j_channelspopup").attr('id');
  var $currentstep = $("#" + currentstep);
  var nextstep = $currentstep.find("#nextstep").val();
  var channelForm = $(current).closest("form");
  var url = "/portal/portal/channels/editchannel";
  var channelId = $("li[id^='channel'].selected.channels").attr('id').substr(7);
  $thisDialog = $("#dialog_edit_channel");
  $thisDialog.bind("dialogbeforeclose", function(event, ui) {
    $thisDialog.empty();
  });

  if (isChannelFormValid(channelForm, "")) {
    if (currentstep == "step2") {
      $(channelForm).find("#conf_edit_name").text($("#channelName").val());
      $(channelForm).find("#conf_edit_name").attr("title", $("#channelName").val());
      $(channelForm).find("#conf_edit_code").text($("#channelCode").val());
      $(channelForm).find("#conf_edit_code").attr("title", $("#channelCode").val());
      $(channelForm).find("#conf_edit_channel_description").text($("#channelDescription").val());
      $(channelForm).find("#conf_edit_channel_description").attr("title", $("#channelDescription").val());
      $(channelForm).find("#conf_currencies").text(getCurrencyString(getCurrencyArray(channelForm)));
    }
    if (currentstep == "step3") {

      var channelName = $("#channelName").val();
      var channelNameToDisplay = "<br>";
      var size = channelName.length;
      var maxsize = 50;
      var count = 0;
      while (size > 50) {
        channelNameToDisplay += channelName.substring(count, count + maxsize) + "<br>";
        count = count + maxsize;
        size = size - 50;
      }
      channelNameToDisplay += channelName.substring(count) + "<br>";

      $("#step4").find("#successmessage").append(channelNameToDisplay);
      $.ajax({
        type: "POST",
        url: url,
        cache: false,
        data: {
          "Id": channelId,
          "channelName": $thisDialog.find("#channelName").val(),
          "description": $thisDialog.find("#channelDescription").val(),
          "code": $thisDialog.find("#channelCode").val()
        },
        success: function(channel) {
          editChannelDetailsInListViiew(channelId, $thisDialog.find("#channelName").val(), $thisDialog.find(
            "#channelCode").val());
          $(".j_channelspopup").hide();
          $("#" + nextstep).show();
        },
        error: function(XMLHttpRequest) {
          if (XMLHttpRequest.status === AJAX_FORM_VALIDATION_FAILED_CODE) {
            displayAjaxFormError(XMLHttpRequest,
              "channelForm",
              "main_addnew_formbox_errormsg");
          } else if (XMLHttpRequest.status === CODE_NOT_UNIQUE_ERROR_CODE) {
            popUpDialogForAlerts("alert_dialog", i18n.errors.common.codeNotUnique);
          } else {
            popUpDialogForAlerts("alert_dialog", i18n.errors.products.failed_create_product);
          }
        }
      });

    } else if (currentstep == "step4") {
      $thisDialog.dialog("close");
      $thisDialog.find(".dialog_formcontent").empty();
      $thisDialog.find(".dialog_formcontent").empty();
    } else {
      $(".j_channelspopup").hide();
      $("#" + nextstep).show();
    }
  }
}


// Add/edit channel wizard last step link.
$("#viewchanneldetails_configure, .close_channel_wizard").live("click", function(event) {
  $("#dialog_add_channel").dialog("close");
  $("#dialog_edit_channel").dialog("close");
  $("#dialog_add_channel").empty();
  $("#dialog_edit_channel").empty();
});

var loadingBarHtml =
  '<div class="infinite_scrollbarbox" id="loading_intimation"><div class="infinite_loading"></div></div>';
var scrollUrlHitMap = {};

$("#bundles_detail_area").scroll(function() {
  if ($("#bundles_detail_area").scrollTop() + $("#bundles_detail_area").height() >= $(this)[0].scrollHeight) {
    var gotAll = $("#bundles_detail_area").attr("gotall");
    var which = $("#bundles_detail_area").attr("which");
    if (gotAll == "true") {
      return;
    }
    // Check if the url has already been hit, then skip it..
    var lastBundleNo = $("#bundles_detail_area").find("div[id^='bundleNo']:last").attr("id").substr(8);
    var url = "/portal/portal/channels/getnextsetofbundles";
    var channelId = $("li[id^='channel'].selected.channels").attr('id').substr(7);
    var editpriceisvalid = "0";
    if ($("#planning_for_first_time").attr("value") == "") {
      editpriceisvalid = "1";
    }
    var scrollUrlKey = channelId + KEY_VALUE_ITEM_SEPERATOR + lastBundleNo + KEY_VALUE_ITEM_SEPERATOR +
      which + KEY_VALUE_ITEM_SEPERATOR + editpriceisvalid;
    if (scrollUrlHitMap[scrollUrlKey] != undefined) {
      return;
    }
    scrollUrlHitMap[scrollUrlKey] = true;
    $("#bundles_detail_area").find("div[id^='bundleNo']:last").after(loadingBarHtml);
    $.ajax({
      type: "GET",
      url: url,
      async: false,
      cache: false,
      data: {
        "channelId": channelId,
        "lastBundleNo": lastBundleNo,
        "which": which,
        "editpriceisvalid": editpriceisvalid
      },
      dataType: "html",
      success: function(result) {
        $("#bundles_detail_area").find("#loading_intimation").remove();
        if (result == null || result.trim().length == 0) {
          $("#bundles_detail_area").attr("gotall", "true");
        } else {
          $("#bundles_detail_area").find("div[id^='bundleNo']:last").after(result);
          // Had to add bindings to cater to the action items..Because we are getting a static
          // html, we need to add bindings again for the newly arrived entities.
          $("div[id^='action_per_product_bundle']").bind("mouseover", function(event) {
            $(this).find('#per_bundle_action_menu').show();
          });
          $("div[id^='action_per_product_bundle']").bind("mouseout", function(event) {
            $(this).find('#per_bundle_action_menu').hide();
          });
        }
      },
      error: function(XMLHttpRequest) {}
    });
  }
});

function getFullListingOfCharges(bundleId) {
  var data = {};
  data["currentHistoryPlanned"] = $("#currentHistoryPlanned").attr("value");
  data["channelId"] = $("li[id^='channel'].selected.channels").attr('id').substr(7);
  if (bundleId !== undefined) {
    data["bundleId"] = bundleId;
  }
  if ($("#currentHistoryPlanned").attr("value") == "history") {
    data["dateFormat"] = $("#historyDateFormat").val();
    data["historyDate"] = $("#catalog_history_dates").val();
  }
  $.ajax({
    type: "GET",
    url: "/portal/portal/channels/getfulllistingofcharges",
    data: data,
    dataType: "html",
    async: false,
    success: function(html) {
      if (bundleId !== undefined) {
        initDialog("dialog_bundle_pricing", 782);
        var $thisDialog = $("#dialog_bundle_pricing");
      } else {
        initDialog("dialog_utility_pricing", 782);
        var $thisDialog = $("#dialog_utility_pricing");
      }
      $thisDialog.html("");
      $thisDialog.html(html);
      $thisDialog.dialog('option', 'buttons', {
        "OK": function() {
          $(this).dialog("close");
          $thisDialog.empty();
        }
      });
      dialogButtonsLocalizer($thisDialog, {
        'OK': g_dictionary.dialogOK
      });
      $thisDialog.bind("dialogbeforeclose", function(event, ui) {
        $thisDialog.empty();
      });
      //		       $thisDialog.find(".widget_details_actionbox").remove();
      //		       $thisDialog.find(".widget_subactions.action_menu_container").remove();
      //		       $thisDialog.find(".widget_grid_cell .moretabbutton").parent().remove();
      $thisDialog.dialog("open");
    },
    error: function() {
      // need to do
    }
  });
}

function syncChannel(event, current) {
  var divId = $("li[id^='channel'].selected.channels").attr('id');
  var channelId = divId.substr(7);
  initDialog("dialog_sync_channel", 390);
  var $thisDialog = $("#dialog_sync_channel");
  $thisDialog.data("height.dialog", 100);
  $thisDialog.dialog('option', 'buttons', {
    "OK": function() {
      var url = "/portal/portal/channels/syncchannel";
      $.ajax({
        type: "POST",
        url: url,
        async: false,
        cache: false,
        data: {
          "channelId": channelId
        },
        dataType: "html",
        success: function(result) {
          $thisDialog.dialog("close");
          if (result == "failure") {
            popUpDialogForAlerts("alert_dialog", i18n.errors.channels.failed_to_sync_channel);
          }
        },
        error: function(XMLHttpRequest) {
          popUpDialogForAlerts("alert_dialog", i18n.errors.channels.failed_to_sync_channel);
        }
      });
      $(this).dialog("close");
    },
    "Cancel": function() {
      $(this).dialog("close");
    }
  });
  dialogButtonsLocalizer($thisDialog, {
    'OK': g_dictionary.dialogOK,
    'Cancel': g_dictionary.dialogCancel
  });
  $thisDialog.dialog("open");
}
