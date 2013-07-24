<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<div class="secondlevel_withsubmenu">

    <div class="secondlevel_breadcrumb_panel">
        <div class="secondlevel_breadcrumbbox">
        	<p><c:out value="${tenant.name}"/></p>
        </div>
             
    </div>        
     <div class="secondlevel_menupanel">
   			<a  class="secondlevel_menutabs <c:out value="${NewRegistration}"/>"href="<%=request.getContextPath() %>/portal/reports/newRegistrations">
        		<spring:message code="ui.label.report.newregistrations.title"/>
        	</a>        	
        	<a   class="secondlevel_menutabs <c:out value="${CustomerRank}"/>" href="<%=request.getContextPath() %>/portal/reports/customerRank">
        		<spring:message code="ui.label.report.customerrank.title"/>
        	</a>
        	<a   class="secondlevel_menutabs <c:out value="${ProductUsage}"/>" href="<%=request.getContextPath() %>/portal/reports/productUsage">
        		<spring:message code="ui.label.report.productusage.title"/>
        	</a>        	
        	<a   class="secondlevel_menutabs <c:out value="${ProductBundleUsage}"/>" href="<%=request.getContextPath() %>/portal/reports/productbundleUsage">
        		<spring:message code="ui.label.report.bundleusage.title"/>
        	</a>  
        	<a   class="secondlevel_menutabs <c:out value="${CustomReports}"/>" href="<%=request.getContextPath() %>/portal/reports/customreports">
        		<spring:message code="ui.label.report.customreport.title"/>
        	</a>          	      	        	
	 </div>

  <tiles:insertDefinition name="warnings"></tiles:insertDefinition>

</div>
                     
