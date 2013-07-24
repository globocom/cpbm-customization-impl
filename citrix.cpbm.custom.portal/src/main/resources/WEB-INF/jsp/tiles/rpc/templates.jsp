<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<div class="maincontent_title" id="maincontent_title">  
  <div>
  	<h1>Rate Plan Charges Mapping</h1>   
  </div>
   <div class="title_boxlinks">
    	<div class="title_boxlinks_leftpointed"></div>    	
        <div class="title_boxlinks_tab highlighted">
        <spring:url value="/portal/rpc/show_service_offerings" var="service_offerings_path" htmlEscape="false" />		
        <a href="<c:out value="${service_offerings_path}"/>">Service Offerings</a></div>
        <div class="title_boxlinks_tab highlighted">
        <spring:url value="/portal/rpc/show_disk_offerings" var="disk_offerings_path" htmlEscape="false" />		
        <a href="<c:out value="${disk_offerings_path}"/>">Disk Offerings</a></div>
        <div class="title_boxlinks_tab highlighted">
        <spring:url value="/portal/rpc/show_templates" var="templates_path" htmlEscape="false" />		
        <a href="<c:out value="${templates_path}"/>">Templates</a></div>
    </div>
</div>
<spring:url value="/portal/rpc/update_templates" var="rpc_update_path" htmlEscape="false"/>
  <form:form commandName="rpcForm" id="rpcForm"  action="${rpc_update_path}">
<div class="mainbox " style="display: block;">
	<div class="main_titlebox">
		<h2>Templates</h2>
	</div>
<div class="grid_container">
	<hr></hr>
  <div class="grid_header" > 
  <div class="grid_genheader_cell" style="width:10%">
      <div class="grid_headertitles"><spring:message code="rpc.page.ui.cloud.service.offerings.id"></spring:message></div>
    </div>   
    <div class="grid_genheader_cell" style="width:30%">
      <div class="grid_headertitles"><spring:message code="rpc.page.ui.cloud.service.offerings.header"></spring:message></div>
    </div>
    <div class="grid_genheader_cell" style="width:10%">
      <div class="grid_headertitles">Enabled?</div>
    </div>
    <div class="grid_genheader_cell" style="width:49%">
      <div class="grid_headertitles"><spring:message code="rpc.page.ui.billing.rpc.ci.header"></spring:message></div>
    </div>          
  </div>
  <div class="grid_content">  
	<c:forEach var="offering" items="${rpcForm.offeringList}" varStatus="offeringStatus">
	  <c:choose>
	    <c:when test="${offeringStatus.index % 2 == 0}">
	      <c:set var="rowClass" value="smallrow_odd"/>
	    </c:when>
	    <c:otherwise>
          <c:set var="rowClass" value="smallrow_even"/>
	    </c:otherwise>
	  </c:choose>	 
	  <div class="<c:out value="gridrow ${rowClass}"/>" >
	   <div class="gridrow_cell" style="width:10%">
  	      <div class="grid_celltitles">
            <c:out value="${offering.offeringId}"/>
  	      </div>
  	    </div>	  
	   <div class="gridrow_cell" style="width:30%">
  	      <div class="grid_celltitles">
            <c:out value="${offering.offeringName}"/>
            <input type="hidden" name="offeringList[<c:out value='${offeringStatus.index}'/>].offeringName" value="<c:out value='${offering.offeringName}'/>">
            <input type="hidden" name="offeringList[<c:out value='${offeringStatus.index}'/>].offeringId" value="<c:out value='${offering.offeringId}'/>">
  	      </div>
  	    </div>
  	     <div class="gridrow_cell" style="width:10%">
  	      <div class="grid_celltitles">
            <c:out value="${offering.enable}"/>
  	      </div>
  	    </div>
  	    <div class="gridrow_cell" style="width:49%">
  	      <div class="grid_celltitles">
           <select name="offeringList[<c:out value='${offeringStatus.index}'/>].billingId"   >
           <option value="-1" ><spring:message code="label.choose"/></option>
           	<c:forEach var="billingRPC" items="${rpcForm.rpcList}" varStatus="status">
           		
           		 <option value='<c:out value="${billingRPC.billingId}"/>'  <c:if test="${billingRPC.billingId == offering.billingId}">selected='selected'</c:if>
           		 > <c:out value="${billingRPC.billingName}"/></option>
           		
           	</c:forEach>
           </select>
  	      </div>
  	    </div>  	       	    	    
	  </div>
	 </c:forEach>
  </div>
</div>
<div class="clearboth"></div>
<div class="buttons" id="buttons" style="margin:0 0 0 10px;">
 <table><tr><td>
 	<div class="second">
		<a  tabindex="220" href="javascript:history.back();" >Cancel</a>
        		</div>
	</td>
	<td><div class="first">
		<input  tabindex="221" type="submit" value="Apply Changes"/>        		
	</div>					
 </td></tr></table>
</div>
</div>

</form:form>
