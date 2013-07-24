/* Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. */
$(document).ready(function() {
            
      $("#createTicketForm").validate( {
  		// debug : true,
  		
  		rules : {
  			"comments" : {
  				required : true
  			},
  			"subject" : {
  				required : true
  			}
  			
  		},
  		messages : {
  			"comments" : {
  				required : "Please provide description."
  			},
  			"subject" : {
  				required : "Please provide subject."
  			}
  			
  		}
  	});
	$("#createTicketCommentForm").validate( {
		// debug : true,
		
		rules : {
			"commentBody" : {
				required : true
			}			
		},
		messages : {
			"commentBody" : {
				required : "Please provide comments."
			}			
		}
	});
  });
function refreshList(){
	$(".gridrow").each( function() {
		$(this).hide();
	});
	var status = document.getElementById("status");
	var value = status.options[status.selectedIndex].value;
	if(value == 'All'){
	$(".gridrow").each( function() {
		$(this).show();
	});
	}
	if(value == 'Closed'){
		$("."+value).each( function() {
			$(this).show();
		});
	}
	if(value == 'Open'){
		$(".gridrow").each( function() {
			$(this).show();
		});
		$(".Closed").each( function() {
			$(this).hide();
		});
	}
}
