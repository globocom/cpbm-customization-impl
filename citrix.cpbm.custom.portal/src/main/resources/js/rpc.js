/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
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
