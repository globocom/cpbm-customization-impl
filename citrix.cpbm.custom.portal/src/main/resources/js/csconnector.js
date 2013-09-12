/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */

var filledActiveCurrencies = new Array();

$(document).ready(function() {

  $(".js_iframe_tabs").live("click", function() {
    var serviceInstanceUUID = $(this).attr('id').substr(11);
    if (serviceInstanceUUID == "all_services") {
      window.location = "/portal/portal/connector/csinstances?tenant=" + effectiveTenantParam;
    } else {
      showResourcesIFrameWithServiceInstanceUUID(serviceInstanceUUID);
    }
  });
  if (typeof iframe_view != "undefined" && iframe_view && typeof service_instance_uuid_for_iframe != "undefined") {
    showResourcesIFrameWithServiceInstanceUUID(service_instance_uuid_for_iframe);
  }
  $("#backToServiceInstanceDetails").live("click", function(event) {
    $(".j_cloudservicepopup").hide();
    currentstep = "step1";
    $("#step1").show();
  });

  $("#backToProductSelection").live("click", function(event) {
    $(".j_cloudservicepopup").hide();
    currentstep = "step3";
    $("#step3").show();
  });

  $("#backToProductCharges").live("click", function(event) {
    $(".j_cloudservicepopup").hide();
    currentstep = "step4";
    $("#step4").show();
  });


  $.validator.addClassRules("logorequired", {
    logorequired: true
  });
  $.validator
    .addMethod(
      "logorequired",
      function(value, element) {
        if (value == "") {
          return false;
        }
        return true;
      },
      dictionary.editImagePathInvalidMessage);
  $("#serviceInstanceLogoForm").validate({
    // debug : true,
    success: "valid",
    ignoreTitle: true,
    rules: {
      "logo": {
        required: true
      }
    },
    messages: {
      "logo": {
        required: dictionary.editImagePathInvalidMessage
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

  activateThirdMenuItem("l3_home_connectors_cs_tab");

  $(".cloud_button.active").bind("click", function(event) {
    var id = $(this).attr('id');
    $('div.servicelist_extended[serviceid=' + id + ']').toggle();
  });

  $("a.filters").bind("click", function(event) {
    var category = $(this).attr('id');
    showSelectedCategory(category);
  });

  $(".servicedetails").bind("click", function(event) {
    initDialog("dialog_service_details", 720);
    var id = $(this).attr('id');
    $.ajax({
      type: "GET",
      url: connectorPath + "/" + type + "?id=" + id + "&action=view",
      dataType: "html",
      success: function(html) {
        var $thisDialog = $("#dialog_service_details");
        $thisDialog.html(html);
        $thisDialog.bind("dialogbeforeclose", function(event, ui) {
          $thisDialog.empty();
        });
        dialogButtonsLocalizer($thisDialog, {
          'OK': g_dictionary.dialogOK,
          'Cancel': g_dictionary.dialogCancel
        });
        $thisDialog.dialog('open');
      },
      error: function() {

      }
    });
  });

  $('.actionbutton').on('mouseenter', function() {
    $(this).parent().find("#action_menu").show();
    $(this).parent().find(".widget_moreactions").show();

  }).on('mouseleave', function() {
    $(this).parent().find(".widget_moreactions").hide();
  });


  $(".termsandconditions").bind("click", function(event) {
    var id = $(this).parents('.servicelist.mainbox').attr('serviceid');
    dialog_enable_service(id);
  });

  $("#tncAccept").live("click", function(event) {
    if ($(this).is(":checked")) {
      $("#tncAcceptError").text("");
    }
  });

  $("a.close_enable_service_wizard").live("click", function(event) {
    closeDialog();
  });

  $("a.close_service_instance_wizard").live("click", function(event) {
    closeAddServiceInstanceDialog();
  });

  $("a.optional_settings").live("click", function(event) {
    $("#optional_settings_div").toggle();
  });

  $("a.close_edit_service_instance_wizard").live("click", function(event) {
    closeEditServiceInstanceDialog();
  });

  $("li.uploadLogo").live("click", function(event) {
    var id = $(this).parents('li').attr('serviceid');
    uploadServiceInstanceImageGet(id);
  });



  $("li.reload").live("click", function(event) {
    var id = $(this).parents('li').attr('serviceid');
    var $currentRow = $(this).parents('li');
    $currentRow.find(".widget_loaderbox").show();
    $.ajax({
      type: "GET",
      url: connectorPath + "/status?id=" + id,
      async: false,
      dataType: 'json',
      success: function(running) {
        if (running) {
          $currentRow.find("#instance_icon").removeClass('stopped_listicon').addClass('running_listicon'); //remove existing class
        } else {
          $currentRow.find("#instance_icon").removeClass('running_listicon').addClass('stopped_listicon');
        }
        $currentRow.find(".widget_loaderbox").hide();
      },
      error: function(error) {
        $currentRow.find(".widget_loaderbox").hide();
      }
    });
  });

  $("li.edit").live("click", function(event) {
    var id = $(this).parents('li').attr('serviceid');
    filledActiveCurrencies = new Array();
    initDialog("dialog_edit_service_instance", 900);
    var actionurl = connectorPath + "/" + type + "?instanceId=" + id;
    $("#spinning_wheel").show();
    $.ajax({
      type: "GET",
      url: actionurl,
      dataType: 'html',
      success: function(html) {
        var $thisDialog = $("#dialog_edit_service_instance");
        $thisDialog.html(html);
        $thisDialog.bind("dialogbeforeclose", function(event, ui) {
          $thisDialog.empty();
        });
        $currentDialog = $thisDialog;
        dialogButtonsLocalizer($thisDialog, {
          'OK': g_dictionary.dialogOK,
          'Cancel': g_dictionary.dialogCancel
        });
        $currentDialog.dialog('open');
        $("#spinning_wheel").hide();
      },
      error: function(error) {
        $("#spinning_wheel").hide();
      }
    });
  });

  $(".add_button.active.add_service").live("click", function(event) {
    var id = $(this).attr('id');
    filledActiveCurrencies = new Array();
    initDialog("dialog_add_service_instance", 900);
    var actionurl = connectorPath + "/" + type + "?id=" + id;
    $("#spinning_wheel").show();
    $.ajax({
      type: "GET",
      url: actionurl,
      dataType: "html",
      success: function(html) {
        var $thisDialog = $("#dialog_add_service_instance");
        $thisDialog.html(html);
        $thisDialog.bind("dialogbeforeclose", function(event, ui) {
          $thisDialog.empty();
        });
        $currentDialog = $thisDialog;
        dialogButtonsLocalizer($thisDialog, {
          'OK': g_dictionary.dialogOK,
          'Cancel': g_dictionary.dialogCancel
        });
        $currentDialog.dialog('open');
        $("#spinning_wheel").hide();
      },
      error: function() {
        $("#spinning_wheel").hide();
      }
    });
  });

  $("li.widget_navigationlist").live("click", function(event) {
    var id = $(this).attr('id');
    var $currentStep = $(this).parents('div.j_cloudservicepopup');
    $currentStep.find('div.griddescriptionbox').hide();
    $currentStep.find('#profile_' + id).show();
    $currentStep.find("li.widget_navigationlist").removeClass("active");
    $(this).addClass("active");
  });
  $(".button_manage_service").live("click", showResourcesIFrame);

  $(".utility_rates_link").unbind("click").bind("click", function() {
    var serviceInstanceUUID = $(this).attr('id').substr(7);
    viewUtilitRates(effectiveTenantParam, "utilityrates_lightbox", null, serviceInstanceUUID);
  });
  $(".subscibe_to_bundles_link").unbind("click").bind("click", function() {
    var serviceInstanceUUID = $(this).attr('id').substr(10);
    window.location = "/portal/portal/subscription/createsubscription?tenant=" + effectiveTenantParam +
      "&serviceInstanceUUID=" + serviceInstanceUUID;
  });

  $("#all_selected_usage_type").live("click", function(event) {
    var isChecked = $(event.target).attr("checked");
    var checkboxList = $("#step3").find("#productsList input:checkbox");
    checkboxList.each(function(idx, i) {
      var checkboxItem = $(i);
      if (isChecked == "checked") {
        checkboxItem.attr("checked", isChecked);
      } else {
        checkboxItem.prop("checked", false);
      }
    });
  });

  $(".learn_more_link").live("click", function(event) {
    var si_id = $(this).attr('id').substr(16);
    $(this).toggleClass("more_down");
    $(this).toggleClass("more_up");
    $("#stripped_content_" + si_id).toggle();
    $("#learn_more_content_" + si_id).toggle();
  });

  function uploadServiceInstanceImageGet(ID) {
    initDialog("dialog_upload_service_instance_image", 550);
    var actionurl = connectorPath + "/upload_logo";
    $.ajax({
      type: "GET",
      url: actionurl,
      data: {
        "Id": ID
      },
      async: false,
      dataType: "html",
      success: function(html) {
        var $thisDialog = $("#dialog_upload_service_instance_image");
        $thisDialog.empty();
        $thisDialog.html(html);
        $thisDialog.dialog('option', 'buttons', {
          "OK": function() {
            if ($('#serviceInstanceLogoForm').valid()) {
              $('#serviceInstanceLogoForm').iframePostForm({
                iframeID: 'serviceInstanceLogoForm-iframe-post-form',
                dataType: 'html',
                post: function() {
                  $("#serviceInstanceLogoForm-iframe-post-form").hide();
                  return true;
                },
                complete: function(text) {
                  if (text == 'success') {
                    $thisDialog.dialog('close');
                    alert(dictionary.imageUploadedSuccessfully);
                  } else {
                    $("#logoError").text(text);
                  }
                }
              });
              $('#serviceInstanceLogoForm').submit();
            }

          },
          "Cancel": function() {
            $("#dialog_upload_service_instance_image").empty();
            $thisDialog.dialog('close');
          }
        });
        dialogButtonsLocalizer($thisDialog, {
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
});

function showSelectedCategory(category) {
  $('div.servicelist_extended').hide();
  if (category != "All") {
    $('div.servicelist.mainbox').hide();
    $('div.servicelist.mainbox[category=' + category + ']').show();
  } else {
    $("div.servicelist.mainbox").show();
  }
  $("#selectedcategory").val(category);
  $("a.filters").removeClass('selected');
  $('a.filters[id=' + category + ']').addClass('selected');
}

function dialog_enable_service(id) {
  initDialog("dialog_enable_service");
  var actionurl = connectorPath + "/enable_service?id=" + id;
  $("#spinning_wheel").show();
  $.ajax({
    type: "GET",
    url: actionurl,
    dataType: "html",
    success: function(html) {
      var $thisDialog = $("#dialog_enable_service");
      $thisDialog.dialog("option", {
        height: "auto",
        width: 785
      });
      $thisDialog.html(html);
      $thisDialog.bind("dialogbeforeclose", function(event, ui) {
        $thisDialog.empty();
      });
      $currentDialog = $thisDialog;
      dialogButtonsLocalizer($thisDialog, {
        'OK': g_dictionary.dialogOK,
        'Cancel': g_dictionary.dialogCancel
      });
      $currentDialog.dialog('open');
      $("#spinning_wheel").hide();
    },
    error: function() {
      $("#spinning_wheel").hide();
    }
  });
}


function goToNextStep(current) {
  var currentstep = $(current).parents(".j_cloudservicepopup").attr('id');
  var $currentstep = $("#" + currentstep);
  var nextstep = $currentstep.find("#nextstep").val();


  if (nextstep != "step4") {
    if (currentstep == "step1" && $("#tncAccept").is(':checked') == false) {
      $("#tncAcceptError").text(dictionary.tncAcceptMessage);
    } else {
      $currentstep.hide();
      $("#" + nextstep).show();
    }
  } else {
    //submit
    $("#spinning_wheel").show();
    var profiledetails = new Array();

    $('div[id^="profile_"]').each(function() {
      var roles = new Array();
      var profileid = $(this).attr('id').substr(8);
      $(this).find('input[id^="role_"]:checked').each(function() {
        var rolename = $(this).attr("id").substr(5); //Remove role_
        roles.push(rolename);
      });
      var profiledetail = new Object();
      profiledetail.profileid = profileid;
      profiledetail.roles = roles;
      profiledetails.push(profiledetail);
    });

    $.ajax({
      type: "POST",
      url: connectorPath + "/enable_service",
      data: {
        "profiledetails": JSON.stringify(profiledetails),
        "id": $("#serviceParam").val()
      },
      dataType: "text",
      success: function(status) {
        if (status == 'success') {
          $("#step4").show();
          $currentstep.hide();
        }
        $("#spinning_wheel").hide();
      },
      error: function(status) {
        $("#spinning_wheel").hide();
      }
    });
  }
}

function goToPreviousStep(current) {
  var currentstep = $(current).parents(".j_cloudservicepopup").attr('id');
  var $currentstep = $("#" + currentstep);
  var prevstep = $currentstep.find("#prevstep").val();

  if (prevstep != "") {
    $currentstep.hide();
    $("#" + prevstep).show();
  }
}

function closeDialog() {
  $("#dialog_enable_service").dialog("close");
  window.location = "/portal/portal/connector/cs";
}

function closeAddServiceInstanceDialog() {
  $("#dialog_add_service_instance").dialog("close");
  window.location = "/portal/portal/connector/cs";
}

function closeEditServiceInstanceDialog() {
  $("#dialog_edit_service_instance").dialog("close");
  window.location = "/portal/portal/connector/cs";
}

function resolveViewForSettingFromServiceInstance(serviceInstanceUUID, currentTenantParam, serviceInstanceName) {
  $("#selectedInstanceH1").append(serviceInstanceName);
  $("#serviceAccountConfigDiv").show();
  $("#myServicesDiv").hide();
  $("#serviceAccountConfigViewFrame").attr("src",
    "/portal/portal/connector/account_config_params/?serviceInstanceUUID=" + serviceInstanceUUID + "&tenant=" +
    effectiveTenantParam);
}

function resolveViewForSettingFromServiceInstance2(instanceUuid) {
  $("#manage_services_info").hide();
  $("#myServicesDiv").hide();
  $(".left_filtermenu").hide();
  $("#userSubscribedServiceDetails").show();
  var actionurl = "/portal/portal/users/resolve_view_for_Settings?instanceUuid=" + instanceUuid;
  $("#full_page_spinning_wheel").show();
  $.ajax({
    type: "GET",
    url: actionurl,
    dataType: "json",
    success: function(json) {
      $("#full_page_spinning_wheel").hide();
      if (json != null && json.url != null) {
        $("#userOrAccountSettingsViewFrame").attr("src", json.url);
      }
    },
    error: function(e) {
      $("#full_page_spinning_wheel").hide();
    }
  });
}

$("#backToSubscribedServiceListing").live("click", function(event) {
  $("#userOrAccountSettingsViewFrame").attr("src", "");
  $("#userSubscribedServiceDetails").hide();
  $("#manage_services_info").show();
  $("#myServicesDiv").show();
  $(".left_filtermenu").show();

});


function resolveViewForAccountSettingFromServiceInstance(instanceUuid, tenantParam, serviceInstanceName) {
  var actionurl = "/portal/portal/users/resolve_view_for_account_settings?instanceUuid=" + instanceUuid +
    "&tenantParam=" + tenantParam;
  $.ajax({
    type: "GET",
    url: actionurl,
    dataType: "json",
    success: function(json) {
      if (json != null && json.url != null) {
        $("#selectedInstanceH1").append(serviceInstanceName);
        $("#serviceAccountConfigDiv").show();
        $("#myServicesDiv").hide();
        $("#serviceAccountConfigViewFrame").attr("src", json.url);
      } else {
        popUpDialogForAlerts("dialog_info", dictionary.noSettingsFound);
      }
    },
    error: function(e) {
      // TODO pop up (?) message for no account specific settings are found
    }
  });
}

//Checks if the ServiceInstance/Product code is unique or not

function validate_code(event, input, codeType) {
  clearCodeError(input);
  var code = $(input).val().trim();
  if (input.defaultValue != null && input.defaultValue.trim() != "" && input.defaultValue.trim() == code) {
    return true;
  }
  var err_msg = "";
  if (code.length >= 255) {
    err_msg = dictionary.max_length_exceeded + " 64";
  }

  if (code.length > 0 && !/^[a-zA-Z0-9_:\[\]-]+$/.test(code)) {
    err_msg = dictionary.code_invalid;
  }

  if (err_msg.trim().length > 0) {
    codeErrorPlacement(input, err_msg);
    return false;
  }
  var urlData = {};
  if (codeType == "serviceInstanceCode") {
    urlData = {
      "serviceInstanceCode": code
    };
  } else if (codeType == "productCode") {
    urlData = {
      "product.code": code
    };
  }
  var returnVal = false;
  $.ajax({
    type: "GET",
    url: "/portal/portal/products/validateCode",
    data: urlData,
    dataType: "html",
    async: false,
    cache: false,
    success: function(result) {
      if (result == "false") {
        codeErrorPlacement(input, dictionary.code_not_unique);
      } else {
        returnVal = true;
        clearCodeError(input);
      }
    },
    error: function(html) {
      codeErrorPlacement(input, html);
    }
  });
  return returnVal;
}

function clearCodeError(element) {
  var name = $(element).attr('id');
  name = ReplaceAll(name, ".", "\\.");
  if (name != "") {
    $("#" + name + "Error").html("");
  }
}

function codeErrorPlacement(element, errmsg) {
  var name = $(element).attr('id');
  name = ReplaceAll(name, ".", "\\.");
  if (name != "") {
    $("#" + name + "Error").html('<label for="' + name + '" generated="true" class="error">' + errmsg + '</label>');
  }
}

function createInstance(nextstep) {
  $("#step5").find("#spinning_wheel").show();
  var uuid = $("#step5").find("#add_service_instance_next").attr('uuid');
  var action = $("#step5").find("#add_service_instance_next").attr('action');
  var configProperties = new Array();
  $('input[id^="configproperty"]').each(function() {
    var configProperty = new Object();
    configProperty.name = $(this).attr("name");
    configProperty.value = $(this).attr("value");
    configProperties.push(configProperty);
  });
  $('textarea[id^="configproperty"]').each(function() {
    var configProperty = new Object();
    configProperty.name = $(this).attr("name");
    configProperty.value = $(this).val();
    configProperties.push(configProperty);
  });

  $('input[id^="configbooleantrue"]:checked').each(function() {
    var configProperty = new Object();
    configProperty.name = $(this).attr("name");
    configProperty.value = "true";
    configProperties.push(configProperty);
  });

  $('input[id^="configbooleanfalse"]:checked').each(function() {
    var configProperty = new Object();
    configProperty.name = $(this).attr("name");
    configProperty.value = "false";
    configProperties.push(configProperty);
  });

  var quickProducts = new Array();
  var checkboxList = $("#productsList input:checked");
  checkboxList.each(function(idx, i) {
    var checkboxItem = $(i);
    var usageTypeName = checkboxItem.attr("name");
    var parentDiv = checkboxItem.parent().parent();
    var quickProduct = new Object();
    quickProduct.name = parentDiv.find("#product\\.name\\." + usageTypeName).val();
    quickProduct.code = parentDiv.find("#product\\.code\\." + usageTypeName).val().trim();
    quickProduct.scale = parentDiv.find("#product\\.scale\\." + usageTypeName).val();
    quickProduct.uom = parentDiv.find("#product\\.scale\\." + usageTypeName + " :selected").text().trim();
    quickProduct.category = parentDiv.find("#product\\.category\\." + usageTypeName).val();
    quickProduct.usageTypeId = checkboxItem.val();

    quickProduct.price = new Array();
    var activeCurrencies = $("#step4").find("#productItem\\." + usageTypeName).find(".j_pricerequired");
    activeCurrencies.each(function(idxx, index) {
      var price = new Object();
      var currency = $(index);
      price.currencyCode = currency.attr("id");
      price.currencyVal = currency.val();
      quickProduct.price.push(price);
    });
    quickProducts.push(quickProduct);

  });

  var $resultDisplayBanner = $("#validationError");
  $.ajax({
    type: "POST",
    url: connectorPath + "/create_instance",
    data: {
      "configProperties": JSON.stringify(configProperties),
      "quickProducts": JSON.stringify(quickProducts),
      "id": uuid,
      "action": action
    },
    dataType: "json",
    async: false,
    success: function(data) {
      $("#step5").find("#spinning_wheel").hide();

      if (data.validationResult == "SUCCESS") {
        if (data.result == "SUCCESS") {
          $resultDisplayBanner.css('color', 'green');
          if ($("#" + uuid).find(".add_button").attr('singleton') == "true") {
            $("#" + uuid).find(".add_button").removeClass("active").addClass("nonactive");
          }
          $(".j_cloudservicepopup").hide();
          $("#" + nextstep).show();
        }
        $resultDisplayBanner.text(data.message);
        $resultDisplayBanner.parent("#serviceInstanceError").show();
      } else {
        $resultDisplayBanner.text(data.validationResult);
        $resultDisplayBanner.parent("#serviceInstanceError").show();
      }
    },
    error: function(data) {
      $resultDisplayBanner.text(i18n.errors.connector.createfailed);
      $resultDisplayBanner.parent("#serviceInstanceError").show();
      $("#step5").find("#spinning_wheel").hide();
    }
  });
}

function addServiceInstancePrevious(current) {
  var currentstep = $(current).parents(".j_cloudservicepopup").attr('id');
  var $currentstep = $("#" + currentstep);
  if (currentstep == "step4") {
    var checkboxList = $("#productsList input:checked");
    filledActiveCurrencies = new Array();
    checkboxList.each(function(idx, i) {
      var checkboxItem = $(i);
      var usageTypeName = checkboxItem.attr("name");
      filledActiveCurrencies[usageTypeName] = new Array();
      var activeCurrencies = $("#step4").find("#productItem\\." + usageTypeName).find(".j_pricerequired");
      activeCurrencies.each(function(idxx, index) {
        var price = new Object();
        var currency = $(index);
        price.currencyCode = currency.attr("id");
        price.currencyVal = currency.val();
        filledActiveCurrencies[usageTypeName][price.currencyCode] = price.currencyVal;
      });
    });
  }
  if (currentstep == "step5") {
    $("#serviceInstanceError").hide();
    var checkboxList = $("#step3").find("#productsList input:checkbox");
    if (checkboxList.length == 0) {
      $(".j_cloudservicepopup").hide();
      $("#step2").show();
      return;
    }
  }

  var prevStep = $currentstep.find("#prevstep").val();
  if (prevStep != "") {
    $(".j_cloudservicepopup").hide();
    $("#" + prevStep).show();
  }
}

function addServiceInstanceNext(current) {

  var currentstep = $(current).parents(".j_cloudservicepopup").attr('id');
  var $step2 = $("#step2");
  var $step3 = $("#step3");
  var $step4 = $("#step4");
  var $step5 = $("#step5");
  var $step6 = $("#step6");
  var $currentstep = $("#" + currentstep);
  var nextstep = $currentstep.find("#nextstep").val();
  var serviceInstanceForm = $(current).closest("form");

  $(serviceInstanceForm).validate({
    errorPlacement: function(error, element) {
      var name = element.attr('id');
      name = ReplaceAll(name, ".", "\\.");
      if (name != "") {
        error.appendTo("#" + name + "Error");
      }
    }
  });

  if (currentstep == "step3") {
    var checkboxList = $("#productsList input:checked");
    $step4.find("#productPriceDiv").find("#productPriceListDiv").empty();
    var isProductCodeValid = true;
    var productCodeMap = new Array();
    checkboxList.each(function(idx, i) {
      var checkboxItem = $(i);
      var usageTypeName = checkboxItem.attr("name");
      var parentDiv = checkboxItem.parent().parent();
      var returnVal = validate_code(null, parentDiv.find("#product\\.code\\." + usageTypeName)[0], "productCode");
      if (!returnVal) {
        isProductCodeValid = false;
      }
      var prodCode = $(parentDiv.find("#product\\.code\\." + usageTypeName)[0]).val().trim();
      if (productCodeMap[prodCode] != undefined) {
        codeErrorPlacement(parentDiv.find("#product\\.code\\." + usageTypeName)[0], dictionary.code_not_unique);
        isProductCodeValid = false;
      } else {
        productCodeMap[prodCode] = "";
      }
      var selectedProductName = parentDiv.find("#product\\.name\\." + usageTypeName).val();
      var selectedUOM = parentDiv.find("#product\\.scale\\." + usageTypeName + " :selected").text().trim();
      var selectedCategory = parentDiv.find("#product\\.category\\." + usageTypeName + " :selected").text().trim();
      var selectedProductCode = parentDiv.find("#product\\.code\\." + usageTypeName).val();
      var productItem = $step4.find("#productPriceDiv").find("#productItem").clone();
      productItem.find("#selectedProductName").text(selectedProductName);
      productItem.find("#selectedUOM").text(selectedUOM);
      productItem.find("#selectedProductCategory").text(selectedCategory);
      productItem.find("#selectedProductCode").text(selectedProductCode);
      productItem.attr("id", "productItem." + usageTypeName);
      var usageTypeCurVals = filledActiveCurrencies[usageTypeName];
      if (usageTypeCurVals != undefined) {
        var activeCurrencies = productItem.find(".j_pricerequired");
        activeCurrencies.each(function(idxx, index) {
          var currency = $(index);
          var currencyCode = currency.attr("id");
          currency.val(usageTypeCurVals[currencyCode]);
        });
      }
      $step4.find("#productPriceDiv").find("#productPriceListDiv").append(productItem);
      productItem.show();
    });
    if (!isProductCodeValid) {
      return;
    }
  }
  if ($(serviceInstanceForm).valid()) {
    if (currentstep == "step1") {
      var returnVal = validate_code(null, $("#configproperty_instance_code")[0], "serviceInstanceCode");
      if (!returnVal)
        return;

      if ($("#isOptionalFieldAvailable").val() == "true") {
        $step2.find("#optionalSettings").show();
      }

      $step5.find("#confirmServiceInstanceDetails").find("#name").text($("#configproperty_instance_name").val());
      $step5.find("#confirmServiceInstanceDetails").find("#name").attr("title", $("#configproperty_instance_name").val());
      $step5.find("#confirmServiceInstanceDetails").find("#code").text($("#configproperty_instance_code").val());
      $step5.find("#confirmServiceInstanceDetails").find("#code").attr("title", $("#configproperty_instance_code").val());
      $step5.find("#confirmServiceInstanceDetails").find("#service_description").text($(
        "#configproperty_instance_description").val());
      $step5.find("#confirmServiceInstanceDetails").find("#service_description").attr("title", $(
        "#configproperty_instance_description").val());

      var serviceInstanceName = $("#configproperty_instance_name").val();
      var serviceInstanceNameToDisplay = "<br>";
      var size = serviceInstanceName.length;
      var maxsize = 50;
      var count = 0;
      while (size > 50) {
        serviceInstanceNameToDisplay += serviceInstanceName.substring(count, count + maxsize) + "<br>";
        count = count + maxsize;
        size = size - 50;
      }
      serviceInstanceNameToDisplay += serviceInstanceName.substring(count) + "<br>";
      $step6.find("#successmessage").append(serviceInstanceNameToDisplay);
    }
    if (currentstep == "step2") {
      var checkboxList = $step3.find("#productsList input:checkbox");
      if (checkboxList.length == 0) {
        $(".j_cloudservicepopup").hide();
        $step5.find("#confirmProductDetails").hide();
        $step5.find("#confirmCharges").hide();
        $step5.show();
        return;
      }
      checkboxList.each(function(idx, i) {
        var checkboxItem = $(i);
        var usageTypeName = checkboxItem.attr("name");
        var parentDiv = checkboxItem.parent().parent();
        parentDiv.find("#product\\.code\\." + usageTypeName).val($("#configproperty_instance_code").val() + "_" +
          usageTypeName);
      });
    }
    if (currentstep == "step3") {
      $(".j_cloudservicepopup").hide();
      $step4.show();
      fixupTooltipZIndex($step4.find("#productPriceDiv").find("#productPriceListDiv"));
      return;
    }
    if (currentstep == "step4") {
      var checkboxList = $("#productsList input:checked");
      filledActiveCurrencies = new Array();
      checkboxList.each(function(idx, i) {
        var checkboxItem = $(i);
        var usageTypeName = checkboxItem.attr("name");
        filledActiveCurrencies[usageTypeName] = new Array();
        var activeCurrencies = $("#step4").find("#productItem\\." + usageTypeName).find(".j_pricerequired");
        activeCurrencies.each(function(idxx, index) {
          var price = new Object();
          var currency = $(index);
          price.currencyCode = currency.attr("id");
          price.currencyVal = currency.val();
          filledActiveCurrencies[usageTypeName][price.currencyCode] = price.currencyVal;
        });
      });
    }
    if ((currentstep == "step5")) {
      //call submit
      createInstance(nextstep);
    } else if (currentstep == "step6") {
      $currentDialog.dialog("close");
      $("#dialog_add_service_instance").find(".dialog_formcontent").empty();
      $("#dialog_edit_service_instance").find(".dialog_formcontent").empty();
      window.location = "/portal/portal/connector/cs";
    } else {
      $(".j_cloudservicepopup").hide();
      $("#" + nextstep).show();
    }
  }
}

function fixupTooltipZIndex(current) {
  var initialIndex = 200;
  $(current).find('.widget_grid').each(function() {
    var style = $(this).attr('style');
    if (style) {
      $(this).attr('style', style + ';z-index:' + initialIndex + ";position:relative;");
    } else {
      $(this).attr('style', 'z-index:' + initialIndex + ";position:relative;");
    }
    initialIndex -= 5;
  });
  initialIndex = 400;
  $(current).find('.widget_grid_cell').each(function() {
    var style = $(this).attr('style');
    if (style) {
      $(this).attr('style', style + ';z-index:' + initialIndex + ";position:relative;");
    } else {
      $(this).attr('style', 'z-index:' + initialIndex + ";position:relative;");
    }
    initialIndex -= 5;
  });
  initialIndex = 700;
  $(current).find('.subheader').each(function() {
    var style = $(this).attr('style');
    if (style) {
      $(this).attr('style', style + ';z-index:' + initialIndex + ";position:relative;");
    } else {
      $(this).attr('style', 'z-index:' + initialIndex + ";position:relative;");
    }
    initialIndex -= 5;
  });
  initialIndex = 1000;
  $(current).find('.widget_details_popover').each(function() {
    var style = $(this).attr('style');
    var position = $(this).parent().find(".levelicon").position();
    var left = position.left + 43;
    var top = position.top + 5;
    if (style) {
      $(this).attr('style', style + ';z-index:' + initialIndex + ";position:absolute;left:" + left + "px; top:" + top +
        "px;");
    } else {
      $(this).attr('style', 'z-index:' + initialIndex + ";position:absolute;left:" + left + "px; top:" + top + "px;");
    }
    initialIndex -= 5;
  });
}

function onProductDetailMouseover(current) {
  var currentItem = $(current);
  if ($(current).hasClass('active'))
    return;
  var productPriceItem = currentItem.parent().parent();
  productPriceItem.find("#info_bubble").show();
  return false;
}

function onProductDetailMouseout(current) {
  var currentItem = $(current);
  var productPriceItem = currentItem.parent().parent();
  productPriceItem.find("#info_bubble").hide();
  return false;
}

function showHideUnmaskedField(show_unmasked_link) {
  var selected_field_id = $(show_unmasked_link).attr("id").replace("_show_unmasked", "");
  var masked_field = $("#" + selected_field_id).get(0);
  if ($(show_unmasked_link).attr('disabled') == 'disabled') {
    return;
  }
  if (masked_field.getAttribute('type') == 'text') {
    masked_field.setAttribute('type', 'password');
    $(show_unmasked_link).text(dictionary.viewMasked);
  } else {
    masked_field.setAttribute('type', 'text');
    $(show_unmasked_link).text(dictionary.hideMasked);
  }
}

function showHideUnmaskedLink(masked_field) {
  var value = $("#" + $(masked_field).attr("id")).val();
  var $link = $("#" + $(masked_field).attr("id") + "_show_unmasked");

  if (value != "") {
    $link.css({
      opacity: 1.0,
      visibility: "visible"
    });
    $link.attr('disabled', false);
  } else {
    $link.css({
      opacity: 0.5,
      visibility: "visible"
    });
    $link.attr('disabled', true);
  }
}
