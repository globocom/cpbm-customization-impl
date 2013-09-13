<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/subscribe.js"></script>
<div class="maincontent_title" id="maincontent_title">  
  <div>
  	<h1>Custom Spend Alerts</h1>
  </div>  
</div>
<div class="mainbox  ui-corner-top ui-corner-bottom" style="display: block;">
	<div class="main_titlebox ui-corner-top">
		<h2><spring:message code="user.custom.spend.alert.list" /></h2>
		<spring:url value="/portal/users/subscribe/new" var="new_custom_alert_path" htmlEscape="false"></spring:url>
  		<div class="create_anchor">
  			<a class="anchor_button ui-corner-bottom ui-corner-top" href="<c:out value="${new_custom_alert_path}"/>" >Create Custom Alert</a>  
  		</div>
	</div>
	<div class="grid_container">
  <div class="grid_header">
    <div class="grid_genheader_cell" style="width:30%">
      <div class="grid_headertitles">Type</div>
    </div>
    <div class="grid_genheader_cell" style="width:30%">
      <div class="grid_headertitles">Name</div>
    </div>
    <div class="grid_genheader_cell" style="width:15%">
      <div class="grid_headertitles">Amount</div>
    </div>
    <div class="grid_genheader_cell" style="width:15%">
      <div class="grid_headertitles">Percentage</div>
    </div>
    <div class="grid_genheader_cell" style="width:9%">
      <div class="grid_headertitles">Delete</div>
    </div>   
  </div>
  <div class="grid_content">    
       <c:forEach var="subscriptionObj" items="${subscriptions}" varStatus="status">
	     <c:choose>
	       <c:when test="${status.index % 2 == 0}">
	         <c:set var="rowClass" value="smallrow_odd"/>
	       </c:when>
	       <c:otherwise>
             <c:set var="rowClass" value="smallrow_even"/>
	       </c:otherwise>
	     </c:choose>
	     <div class="<c:out value="gridrow ${rowClass}"/>" >	       
  	       <div class="gridrow_cell" style="width:30%">
  	         <div class="grid_celltitles">
               <c:out value="${subscriptionObj.type}"/>
  	         </div>
  	       </div>
  	       <div class="gridrow_cell" style="width:30%">
  	         <div class="grid_celltitles">
               <c:out value="${subscriptionObj.accountHolder.accontHolderName}"/>
  	         </div>
  	       </div>
  	       <div class="gridrow_cell" style="width:15%">
  	         <div class="grid_celltitles">
               <c:out value="${subscriptionObj.data}"/>
  	         </div>
  	       </div> 
  	       <div class="gridrow_cell" style="width:15%">
  	         <div class="grid_celltitles">
               <c:out value="${subscriptionObj.percentage}"/>
  	         </div>
  	       </div> 
  	       <div class="gridrow_cell" style="width:9%">
  	         <div class="grid_celltitles">
               <spring:url value="/portal/users/{subscriptionId}/subscribe/delete"	var="delete_subscribe_path" htmlEscape="false">
					<spring:param name="subscriptionId"><c:out value="${subscriptionObj.id}" /></spring:param>
				</spring:url>
				<form 	action="<c:out value="${delete_subscribe_path}"/>" method="post">
					<input type="submit" id="nonopsDelete" value="" title="delete" class="delete_icon"  />
				</form>
  	         </div>
  	       </div>    	    
	     </div>
	   </c:forEach>     
  </div>    
</div> 
	
</div>

<div id="pagination_panel" class="pagination_panel" style="display:block;">
 <p id="grid_rows_total"><c:out value="${size} "></c:out><c:out value=" items"></c:out></p>
   <div class="pagination_actionbox">
    <div class="pagination_actions">
      <div class="pagination_actionicon"><img src="/portal/images/portal/pagination_refresh.gif" title="refresh" /></div>
      <a id="refresh" href="<%=request.getContextPath() %>/portal/users/subscribe"> Refresh</a>
    </div>
    
  </div>
</div>
