<!-- Copyright (C) 2013 Citrix Systems Inc.  All rights reserved. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<c:choose>
<c:when test="${not empty serviceCategoryList}">
<div class="navigation">
    <c:choose> 
      <c:when test="${currentUser.profile.operationsProfile && currentUser == effectiveTenant.owner}">
        <c:set var="userStyle" value=""/>
      </c:when> 
      <c:otherwise> 
        <c:set var="userStyle" value="user"/> 
      </c:otherwise>
    </c:choose> 
    <div class="flevel">
        <div class="menutab" id="service_category_list_container">
            <ul>
               <c:forEach items="${serviceCategoryList}" var="serviceCategory" varStatus="status">
                     <c:choose> 
                         <c:when test="${(selectedCategory!=null && selectedCategory==serviceCategory) || (selectedCategory == null && status.index == 0)}">
                             <c:set var="tabStatus" value="current ${userStyle}"/> 
                         </c:when> 
                         <c:otherwise> 
                             <c:set var="tabStatus" value="off"/> 
                         </c:otherwise> 
                     </c:choose>
                     <li class="categorytabs ${tabStatus}" id="l3_tab_<c:out value='${serviceCategory}'/>" category="<c:out value='${serviceCategory}'/>">
                       <spring:message code='label.service.category.${serviceCategory}'/>
                     </li>
                </c:forEach>
            </ul>
        </div>
    </div>
    <div class="slevel ${userStyle}">
      <div class="selectedservice ${userStyle}" id="serviceInstanceListContainer">
        <a class="selecteddropdown ${userStyle}" href="#" id="selectedServiceInstance"> </a>
        <div  id="serviceInstanceDropdownlist" class="dropdownlist">
          <ul id="serviceInstanceList">
          </ul>
        </div>
      </div>
      <c:if test="${userHasCloudServiceAccount && showCloudConsoleLink}">
        <input type="hidden" name="currentTenantParam" id="currentTenantParam" value="<c:out value="${currentTenant.param}"/>">  
        <div id="cloudServiceConsoleID" style="float:right;margin:8px 10px 0 0;display:none">
          <a onclick="launchCloudServiceConsole(this);" target="cloudconsole" style="cursor:pointer;"><spring:message code="ui.home_service.page.title.launch.cloud.console"/></a>
        </div>
      </c:if>
      <c:if test="${not empty selectedCloudServiceInstance}">
        <input type="hidden" name="selectedCloudServiceInstance" id="selectedCloudServiceInstance" value="<c:out value="${selectedCloudServiceInstance}"/>">  
      </c:if>
    </div>
</div>
</c:when>
<c:otherwise>
  <div class="subscribe_dialog_message_box error" style="margin-left:20px;">
            <p><spring:message code='no.cloud.service.account'/></p>
        </div>
</c:otherwise>
</c:choose>