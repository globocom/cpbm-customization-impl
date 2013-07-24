<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>

<input type="hidden" id="l3_tenant_param" value='<c:out value="${tenant.param}"/>'/>
<c:if test="${!provision}">
	<div class="thirdlevel_subsubmenu">
	  <div class="thirdlevel_subsubmenu left"></div>
	    <div class="thirdlevel_subsubmenu mid">
	          <div class="thirdlevel_subtab big off" id="l3_compute_bundles_tab">
	            <div class="thirdlevel_menuicons computebundles"></div>
	            <p><spring:message code="page.level3.subscription.compute.bundles"/>
	            </p>
	          </div>
	          <div class="thirdlevel_subtab big off" id="l3_service_bundles_tab">
	            <div class="thirdlevel_menuicons servicebundles"></div>
	            <p><spring:message code="page.level3.subscription.service.bundles"/>
	            </p>
	          </div>
	          
	        </div> 
	        <div class="thirdlevel_subsubmenu right"></div>
	</div>
</c:if>