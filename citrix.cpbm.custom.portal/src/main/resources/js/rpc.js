/* Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. */
$(document).ready(function() {
	
	$(".filterbyservicedomain").change(function() {
		var domainId = $(this).val();		
		window.location ="/portal/portal/rpc/show_service_offerings?domainId="+domainId;
			return true;
	});
	
	$(".filterbydiskdomain").change(function() {
		var domainId = $(this).val();		
		window.location ="/portal/portal/rpc/show_disk_offerings?domainId="+domainId;
			return true;
	});
	
	
});
