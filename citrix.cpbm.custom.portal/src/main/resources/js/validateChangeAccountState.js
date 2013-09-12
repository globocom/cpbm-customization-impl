/*
*  Copyright © 2013 Citrix Systems, Inc.
*  You may not use, copy, or modify this file except pursuant to a valid license agreement from
*  Citrix Systems, Inc.
*/
$(document).ready(function() {

  $("#changeStateForm").validate({
    //debug : true,
    success: "valid",
    ignoreTitle: true,
    submitHandler: function(form) {},
    rules: {
      "memo": {
        required: true
      }
    },
    messages: {
      "memo": {
        required: i18n.change.state.memo
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
});
