/* Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. */
function getAuditLog(url) {
	var form = $(this).children('form');

	$.ajax( {

		type : "POST",
		url : url,
		data : form.serialize(),
		dataType : "html",
		success : function(html) {
			$("#auditLogDiv").html(html);

		}

	});
}   

