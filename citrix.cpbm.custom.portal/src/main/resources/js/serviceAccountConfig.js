$(document).ready(function() {
	//console.log(jspProvidedByService+'...serviceAccountConfigProperties...',emptyServiceAccountConfigProperties);
	if(emptyServiceAccountConfigProperties == 'true'){
			enableService(1);
	}else{
		$("#serviceAccountConfigParamForm").show();
		if(foundJspProvidedByService == 'true'){
			handleCustomJsp();
		}
	}

	$("#serviceAccountConfigParamForm").validate({
	    submitHandler : function() {
	      var noOfErrors = $('label.error:visible').length;
	      if (noOfErrors == 0) {
		if(foundJspProvidedByService == 'true'){
			enableService(2);
		}else{
	    	  enableService(0);
		}
	      }
	      return false;
	    }
	  });
	
	
	 $("#submitbutton").bind("click", function(event) {
		    if($("#submitbutton").hasClass('active')){
		      $('#serviceAccountConfigParamForm').submit();
		    }
	});

 	$("#cancelButton").bind("click", function(event) {
		window.open('/portal/portal/connector/csinstances?tenant='+effectiveTenantParam,'_parent');
	});

	$("#resultButton").bind("click", function(event) {
		window.open('/portal/portal/connector/csinstances?tenant='+effectiveTenantParam,'_parent');
	});
	
	function handleCustomJsp(){		
		//console.log('Custom Jsp Found');
		$("#serviceAccountConfigParamForm").show();
		$.ajax({
		      url : "/portal/portal/connector/loadPackagedJsp?serviceInstanceUUID="+currentServiceInstanceUUID,
		      dataType : "html",
		      async : false,
		      success : function(html) {
			$("#accountConfigEditorJsp").html(html);
		      }
		    });
	}
	
	function enableService(enableServiceType){
	var propObject = new Object();
	if(enableServiceType == 0){
		$("input[id^=sacp_]").each(function(index) {
		       	var eleName = $(this).attr("name");
			var eleValue = $(this).attr('value');	
		      	propObject[eleName] = eleValue;
		});   
	}else if(enableServiceType == 2){
		//console.log('....serviceAccountConfigProperties Values...',serviceAccountConfigPropertiesArray);		
		$.each( serviceAccountConfigPropertiesArray.split(','), function(index,value ) {			
			//console.log(index+":ssss:: " + value);
			if(value != null && value != ""){
				var elementId = "#"+value;
				var elementValue = $(elementId).attr('value');
				propObject[value] = elementValue;
			}
		});
	}

	 var propConfigs = JSON.stringify(propObject);
		//console.log('..enableServiceType..',enableServiceType,'....',propObject,'.......',currentLoggedinTenant,'...',propConfigs,'....',jspProvidedByService,'...',currentServiceInstanceUUID);
		 $("#spinning_wheel").show();
	var ajaxUrl = "/portal/portal/tenants/enableServiceForTenant";
		$.ajax({
			type : "POST",
			data : {
				"tenantparam" : effectiveTenantParam,
				"instanceUuid" : currentServiceInstanceUUID,
				"instanceProperty" : propConfigs
			},		
			url : ajaxUrl,
			success : function(status) {
				 $("#spinning_wheel").hide();
				prepareSuccessResultView(status);
			},
			error : function(XMLHttpResponse) {
				 $("#spinning_wheel").hide();
				 $("#resultForServiceConfigParams").show();
				 $("#ajax_result_text").text(service_config_message_dict.enableServiceError);
				 $("#ajax_result_display").addClass("error").show();
			}
		});
	}

	function prepareSuccessResultView(status){
//console.log('status ...', status);
	/*	var object = new Object();
		var successArray = new Array();
		successArray.push('tom');
		successArray.push('mickey');
		var failArray = new Array();
		failArray.push('donald');
		failArray.push('ravi');
		object.failedUsers = failArray;
		object.registeredUser = successArray;*/
		var current = 1;
		var liStyle ="";
		var final = "";
		if(status.registeredUser != undefined){
			$.each( status.registeredUser, function( key, value ) {
				var temp ="";
				//console.log( key + ": " + value );
				if(current%2 ==0){
					liStyle = "even";
				}else{
					liStyle = "odd";			
				}
				current = current +1;	
				temp = temp + '<li class="row '+ liStyle + '">';
				temp = temp + '<span class="label">' + value + '</span> ';
				temp = temp + '<span class="description"> Configured </span> </li>';	
				final = final + temp; 
			});
		}
		if(status.failedUsers != undefined){
			$.each( status.failedUsers, function( key, value ) {
				var temp ="";
				//console.log( key + ": " + value );
				if(current%2 ==0){
					liStyle = "even";
				}else{
					liStyle = "odd";			
				}
				current = current +1;	
				temp = temp + '<li class="row '+ liStyle + '">';
				temp = temp + '<span class="label">' + value + '</span> ';
				temp = temp + '<span class="description"> Failed </span> </li>';	
				final = final + temp;
			});
		}
		//console.log(final);
		$("#resultForServiceConfigParams").show();
		$("#printResultsForConfigParams").append(final);
		$("#mainContentForServiceAccConfigParam").hide();
	}
	 
});