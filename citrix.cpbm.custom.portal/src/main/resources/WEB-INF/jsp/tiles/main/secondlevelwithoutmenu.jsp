<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
 <%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
 <div class="secondlevel_withoutsubmenu">
 <c:choose>
 <c:when test="${isprofilepage == 'true' && (user.user.id != currentUser.id)}">
  <div class="secondlevel_breadcrumb_panel" style="width:950px;">
     <div class="secondlevel_breadcrumbbox">
       <p><c:out value="${user.user.tenant.name}"/></p>
     </div>
     <div class="secondlevel_breadcrumbbox">
      <p><c:out value="${user.user.firstName} ${user.user.lastName}"/></p>
    </div>   
    <div class="secondlevel_breadcrumbbox">
      <p><spring:message code="page.level2.viewprofile"/></p>
    </div>  
  </div>
  </c:when>
  <c:when test="${isprofilepage == 'true' && (user.user.id == currentUser.id)}">
  <div class="secondlevel_breadcrumb_panel">
     <div class="secondlevel_breadcrumbbox">
       <p><spring:message code="page.level2.myprofile"/></p>
     </div>
  </div>
  </c:when>
  <c:otherwise>
  <div class="secondlevel_breadcrumb_panel">
           <div class="secondlevel_breadcrumbbox">
               <p><c:out value="${tenant.name}"/></p>
           </div>
                
      </div>
  </c:otherwise>
 </c:choose>
 
</div>

  <tiles:insertDefinition name="warnings"></tiles:insertDefinition>
