<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>


<script type="text/javascript" src="<%=request.getContextPath() %>/js/lib/HighCharts/highcharts.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/lib/HighCharts/no-data-to-display.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/lib/HighCharts/modules/exporting.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/util/HighChartsUtil.js"></script>

<div class="secondlevel_withsubmenu">

    <div class="secondlevel_breadcrumb_panel">
        <div class="secondlevel_breadcrumbbox_tenant">
        	<p title="${tenant.name}"><c:out value="${tenant.name}"/></p>
        </div>
        <div class="doc_help_link"></div>
    </div>        
     <div class="secondlevel_menupanel">
   			<a  class="secondlevel_menutabs <c:out value="${NewRegistration}"/>"href="<%=request.getContextPath() %>/portal/reports/new_registrations">
        		<spring:message code="ui.label.report.newregistrations.title"/>
        	</a>        	
        	<a   class="secondlevel_menutabs <c:out value="${CustomerRank}"/>" href="<%=request.getContextPath() %>/portal/reports/customer_rank">
        		<spring:message code="ui.label.report.customerrank.title"/>
        	</a>
        	<a   class="secondlevel_menutabs <c:out value="${ProductUsage}"/>" href="<%=request.getContextPath() %>/portal/reports/product_usage">
        		<spring:message code="ui.label.report.productusage.title"/>
        	</a>        	
        	<a   class="secondlevel_menutabs <c:out value="${ProductBundleUsage}"/>" href="<%=request.getContextPath() %>/portal/reports/productbundle_usage">
        		<spring:message code="ui.label.report.bundleusage.title"/>
        	</a>  
        	<a   class="secondlevel_menutabs <c:out value="${CustomReports}"/>" href="<%=request.getContextPath() %>/portal/reports/custom_reports">
        		<spring:message code="ui.label.report.customreport.title"/>
        	</a>          	      	        	
	 </div>
   <div class="clearboth"></div>

  <tiles:insertDefinition name="warnings"></tiles:insertDefinition>

</div>
                     
