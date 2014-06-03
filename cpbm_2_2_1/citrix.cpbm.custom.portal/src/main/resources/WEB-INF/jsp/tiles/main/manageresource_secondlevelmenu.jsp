<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<div class="secondlevel_withoutsubmenu">
  <div class="secondlevel_breadcrumb_panel">
    <div class="secondlevel_breadcrumbbox">
      <p><c:out value="${tenant.name}"/></p>
    </div>
    <div class="doc_help_link"></div>
  </div>
  <div class="secondlevel_menupanel" id="iframe_tabs">
  <a href="javascript:void(0);" id="iframe_tab_all_services" class="secondlevel_menutabs js_iframe_tabs on"><span class="navicon loading js_loading"  style="display:none;"></span><spring:message code="page.level2.allservices"/></a>
  <c:if test="${userHasCloudServiceAccount}">
    <c:forEach items="${serviceInstanceMap}" var="serviceInstanceEntry" varStatus="status">
      <c:if test="${(serviceInstanceEntry.value == true)}">
        <a href="javascript:void(0);" id="iframe_tab_${serviceInstanceEntry.key.uuid}" class="secondlevel_menutabs js_iframe_tabs"><span class="navicon loading js_loading"  style="display:none;"></span><c:out value="${serviceInstanceEntry.key.name}"/></a>
      </c:if>
    </c:forEach>
  </c:if>
  </div>
  <div class="clearboth"></div>
</div>
<tiles:insertDefinition name="warnings"></tiles:insertDefinition>  
  