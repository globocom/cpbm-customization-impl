/* Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. */
$(document).ready(function() {

	$("#searchFormLookupDiv").dialog( {		
		autoOpen : false,
		width : 600,
		height : 300,
		modal : true,
		title : 'Lookup',
		buttons : {
			"Cancel" : function() {
				$(this).dialog('close');
			},
			"Lookup" : function() {

				$('#searchFormLookup').submit();
			}
		}
	});

	$("#tenantsearchbutton").click(function() {
		var id = $(this).attr('id');
		id = id.replace('tenantsearchbutton', 'searchFormAdvancedDiv');
		$("#" + id).dialog('open');
	});
	
	$("#searchFormAdvancedDiv").dialog( {		
		autoOpen : false,
		width : 600,
		height : 300,
		modal : true,
		title : 'Search',
		buttons : {
			"Cancel" : function() {
				$(this).dialog('close');
			},
			"Search" : function() {
                if($('#fieldName').val()=="" && $('#name').val()=="" && $('#country').val()==""){
                	$("#atleastonefield").html("Atleast one field value is required.");
                	return false;
                }else{                	
				   $('#searchFormAdvanced').submit();
                }
			}
		}
	});

	$("#tenantlookupbutton").click(function() {
		var id = $(this).attr('id');
		id = id.replace('tenantlookupbutton', 'searchFormLookupDiv');
		$("#" + id).dialog('open');
	});
	
});
