/* Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. */
$(document).ready(function() {	
	
$("#changeStateForm").validate( {
    //debug : true,
    success : "valid",
    ignoreTitle : true,
    submitHandler: function(form) {
    },
    rules : {
	  "memo" : {
	  	required: true
    }
  },
  messages : {
	"memo" : {
		required : i18n.change.state.memo
	}
},
    errorPlacement: function(error, element) {
	var name = element.attr('id');
	name =ReplaceAll(name,".","\\."); 
	if (name != "") {
		error.appendTo("#" + name + "Error");
	}
   }
  });
});
