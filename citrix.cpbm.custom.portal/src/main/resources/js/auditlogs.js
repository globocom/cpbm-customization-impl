/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
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

