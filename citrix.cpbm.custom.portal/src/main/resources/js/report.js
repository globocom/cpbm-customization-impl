/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
$(document).ready(function() {
  $(function() {
    $('#start').datepicker({
      duration: '',
      showOn: "button",
      buttonImage: "/portal/images/calendar_icon.png",
      buttonImageOnly: true,
      buttonText: i18n.text.startDateButtonText,
      dateFormat: g_dictionary.friendlyDate,
      showTime: true,
      stepMinutes: 1,
      stepHours: 1,
      time24h: false,
      onClose: function(dateText, inst) {
        if (dateText) {
          var endDate = $('#end').val();
          var date = new Date(Date.parse($(this).datepicker("getDate")));
          date.setDate(date.getDate() + 1);
          $('#end').datepicker("option", "minDate", date);
          $('#end').val(endDate);
        }
      }
    });
  });

  $(function() {
    var minDate = new Date(Date.parse($("#start").val()));
    minDate.setDate(minDate.getDate() + 1);
    minDate = minDate || Date();
    $('#end').datepicker({
      duration: '',
      showOn: "button",
      buttonImage: "/portal/images/calendar_icon.png",
      buttonImageOnly: true,
      buttonText: i18n.text.endDateButtonText,
      dateFormat: g_dictionary.friendlyDate,
      showTime: true,
      stepMinutes: 1,
      stepHours: 1,
      time24h: false,
      minDate: minDate
    });
  });
  $("#newRegistrationForm").validate({
    //debug : true,
    success: "valid",
    ignoreTitle: true,
    rules: {
      "startDate": {
        required: true
      },
      "endDate": {
        required: true
      }
    },
    messages: {
      "startDate": {
        required: i18n.errors.validationStartDate
      },
      "endDate": {
        required: i18n.errors.validationEndDate
      }
    },
    errorPlacement: function(error, element) {
      var name = element.attr('id');
      name = ReplaceAll(name, ".", "\\.");
      if (error.html() != "") {
        $("#" + name + "Error").html("");
        error.appendTo("#" + name + "Error");
      }
    }
  });
  $("#reportForm").validate({
    //debug : true,
    success: "valid",
    ignoreTitle: true,
    rules: {
      "reportMonth": {
        required: true
      },
      "reportYear": {
        required: true
      }
    },
    messages: {
      "reportMonth": {
        required: i18n.errors.validationMonth
      },
      "reportYear": {
        required: i18n.errors.validationYear
      }
    },
    errorPlacement: function(error, element) {
      var name = element.attr('id');
      name = ReplaceAll(name, ".", "\\.");
      if (error.html() != "") {
        $("#" + name + "Error").html("");
        error.appendTo("#" + name + "Error");
      }
    }
  });

  $("#customreport").change(function() {
    $("#month").val("");
    $("#year").val("");
    $("#date").val("");
    var val = $("#customreport").val();
    var text = $("#customreport option[value='" + val + "']").text();
    var type = jQuery.trim(text.substr(text.lastIndexOf('-') + 1));
    if (type == 'MONTHLY') {
      $("#dateparam").hide();
      $("#monthparam").show();
    } else if (type == 'DAILY') {
      $("#monthparam").hide();
      $("#dateparam").show();
    } else {
      $("#monthparam").hide();
      $("#dateparam").hide();
    }
    $("#reportdownload").removeAttr('href');
    $("#reportdownload").addClass("commonbuttondisabled");
    $("#reportdownload").removeClass("commonbutton");
    $("#reportemail").removeAttr('onClick');
    $("#reportemail").removeAttr('name');
    $("#reportemail").addClass("commonbuttondisabled");
    $("#reportemail").removeClass("commonbutton");
  });

  $("#customReportForm").validate({
    //debug : true,
    success: "valid",
    ignoreTitle: true,
    rules: {
      "customreport": {
        required: true
      },
      "month": {
        required: function(element) {
          var val = $("#customreport").val();
          var text = $("#customreport option[value='" + val + "']").text();
          var type = jQuery.trim(text.substr(text.lastIndexOf('-') + 1));
          return (type == 'MONTHLY');
        }
      },
      "year": {
        required: function(element) {
          var val = $("#customreport").val();
          var text = $("#customreport option[value='" + val + "']").text();
          var type = jQuery.trim(text.substr(text.lastIndexOf('-') + 1));
          return (type == 'MONTHLY');
        }
      },
      "date": {
        required: function(element) {
          var val = $("#customreport").val();
          var text = $("#customreport option[value='" + val + "']").text();
          var type = jQuery.trim(text.substr(text.lastIndexOf('-') + 1));
          return (type == 'DAILY');
        }
      }
    },
    messages: {
      "customreport": {
        required: i18n.errors.validationCustomReport
      },
      "month": {
        required: i18n.errors.validationMonth
      },
      "year": {
        required: i18n.errors.validationYear
      },
      "date": {
        required: i18n.errors.validationDate
      }
    },
    errorPlacement: function(error, element) {
      var name = element.attr('id');
      name = ReplaceAll(name, ".", "\\.");
      if (error.html() != "") {
        $("#" + name + "Error").html("");
        error.appendTo("#" + name + "Error");
      }
    }
  });

  $(function() {
    $('#date').datepicker({
      duration: '',
      showOn: "button",
      buttonImage: "/portal/images/calendar_icon.png",
      buttonImageOnly: true,
      buttonText: i18n.text.startDateButtonText,
      dateFormat: g_dictionary.friendlyDate,
      showTime: true,
      stepMinutes: 1,
      stepHours: 1,
      time24h: false
    });
  });

  $(function() {
    $("#email-dialog-modal").dialog({
      modal: true,
      resizable: false,
      autoOpen: false,
      buttons: {
        "Done": function() {
          var isValid = validateEmailIds();
          if (isValid) {
            $("#emailidsError").html("");
            $("#emailidsError").hide();
            sendEmail();
            $(this).dialog("close");
          } else {
            $("#emailidsError").show();
          }
        }
      }
    });
  });

});

function validateEmailIds() {
  var errorMsg;
  var result = true;
  var emailIds = $("#emailids").val().split(';');
  if (emailIds == null || emailIds == "") {
    errorMsg = i18n.errors.emptyEmailField;
    $("#emailidsError").html(errorMsg);
    return false;
  }
  for (var i = 0; i < emailIds.length; i++) {
    result = validate_email(emailIds[i]);
    if (!result) {
      errorMsg = emailIds[i] + ' ' + i18n.errors.invalidEmailIds;
      $("#emailidsError").html(errorMsg);
      break;
    }
  }
  return result;
}

function validate_email(newemail) {
  var apos = newemail.indexOf("@");
  var dotpos = newemail.lastIndexOf(".");
  if (apos < 1 || dotpos - apos < 2) {
    return false;
  } else {
    return true;
  }
}

function generateCustReport(form, event) {
  if (event.preventDefault) {
    event.preventDefault();
  } else {
    event.returnValue = false;
  }
  if ($("#customReportForm").valid()) {
    $.ajax({
      type: "GET",
      url: $(form).attr("action"),
      data: $(form).serialize(),
      dataType: "html",
      success: function(result) {
        $("#reportgenerate").val(i18n.text.generateButtonText);
        $("#reportgenerate").attr("disabled", false);
        if (result == 'none') {
          alert(i18n.alerts.noData);
        } else if (result == 'failure') {
          alert(i18n.errors.generateFailure);
        } else {
          alert(i18n.alerts.generateSuccess);
          var url = $(form).attr("action");
          var downloadURL = url.substring(0, url.lastIndexOf("/")) + "/download_custom_report/" + result;
          var emailURL = url.substring(0, url.lastIndexOf("/")) + "/email_custom_report/" + result;
          $("#reportdownload").attr('href', downloadURL);
          $("#reportdownload").removeClass("commonbuttondisabled");
          $("#reportdownload").addClass("commonbutton");
          $("#reportemail").attr('onClick', 'provideEmailIds()');
          $("#reportemail").attr('name', emailURL);
          $("#reportemail").removeClass("commonbuttondisabled");
          $("#reportemail").addClass("commonbutton");
        }
      },
      error: function(result) {
        alert(i18n.errors.generateFailure);
      }
    });
  }
}

function provideEmailIds(emailURL) {
  $("#email-dialog-modal").dialog('open');

}

function sendEmail() {
  var emailURL = $("#reportemail").attr('name');
  var emailLinkText = $("#reportemail").text();
  $("#reportemail").text($("#reportemail").attr('rel'));
  $.ajax({
    type: "GET",
    url: emailURL,
    data: {
      'emailIds': $("#emailids").val()
    },
    dataType: "html",
    success: function(result) {
      $("#reportemail").text(emailLinkText);
      if (result == 'failure') {
        alert(i18n.errors.sendemailFailed);
      } else {
        alert(i18n.alerts.sendemailSuccess);
      }
    },
    error: function(result) {
      $("#reportemail").text(emailLinkText);
      alert(i18n.errors.sendemailfailed);
    }
  });
}
