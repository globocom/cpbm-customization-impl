$(document).ready(function() {
  currentPage = parseInt(currentPage);
  perPageValue = parseInt(perPageValue);
  batchjobListLen = parseInt(batchjobListLen);
  
  function nextClick(event) {
    
    $("#click_next").unbind("click", nextClick);
    $("#click_next").addClass("nonactive");
    
    currentPage = currentPage + 1;
    
    $("#click_previous").unbind("click").bind("click", previousClick);
    $("#click_previous").removeClass("nonactive");
    
    window.location=batchJobUrl+"?currentPage=" + currentPage;
  }

  function previousClick(event) {
    $("#click_previous").unbind("click", previousClick);
    $("#click_previous").addClass("nonactive");

    currentPage = currentPage - 1;
    
    $("#click_next").removeClass("nonactive");
    $("#click_next").unbind("click").bind("click", nextClick);
    
    window.location=batchJobUrl+"?currentPage=" + currentPage;
  }
  
  if (currentPage > 1) {
    $("#click_previous").removeClass("nonactive");
    $("#click_previous").unbind("click").bind("click", previousClick);
  }
  
  if (batchjobListLen < perPageValue) {
    $("#click_next").unbind("click");
    $("#click_next").addClass("nonactive");

  } else if (batchjobListLen == perPageValue) {
    
    if (currentPage < totalpages) {
      
      $("#click_next").removeClass("nonactive");
      $("#click_next").unbind("click").bind("click", nextClick);
    } else {
      $("#click_next").unbind("click");
      $("#click_next").addClass("nonactive");
    }
  }
});
function viewbatchjob(current){
	 var divId = $(current).attr('id');
	 var ID=divId.substr(3);
	 resetGridRowStyle();
	 $(current).addClass("selected active");
	 var url = "/portal/portal/admin/viewbatchjob";
	 $.ajax( {
			type : "GET",
			url : url,
			data:{Id:ID},
			dataType : "html",
			success : function(html) {				
				$("#viewjobstatusDiv").html(html);
				//fixupTooltipZIndex();
			},error:function(){	
				//need to handle TO-DO
			}
	 });
}
	/**
	 * Reset data row style
	 * @return
	 */
	function resetGridRowStyle(){
	  $(".widget_navigationlist").each(function(){
	    $(this).removeClass("selected active");        
	  });
	}

	function viewFirstBatchJob(divId){
	  var ID=divId.substr(3);
	  var url = "/portal/portal/admin/viewbatchjob";
	  $.ajax( {
	     type : "GET",
	     url : url,
	     data:{Id:ID},
	     dataType : "html",
	     success : function(html) {        
	       $("#viewjobstatusDiv").html(html);
	       //fixupTooltipZIndex();
	     },error:function(){ 
	       //need to handle TO-DO
	     }
	  });
	}
	function onBatchJobMouseover(current){
	  if($(current).hasClass('active')) return 
	  $(current).find("#info_bubble").show(); 
	  return false; 
	}
	function onBatchJobMouseout(current){
	  $(current).find("#info_bubble").hide(); 
	  return false; 
	}
	