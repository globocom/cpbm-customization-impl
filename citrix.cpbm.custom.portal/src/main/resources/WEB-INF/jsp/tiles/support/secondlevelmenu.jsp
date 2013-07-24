<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<tiles:insertDefinition name="support.tickets.js_messages"/>

<div class="secondlevel_withsubmenu">
  <div class="secondlevel_breadcrumb_panel">
    <div class="secondlevel_breadcrumbbox">
      <p><c:out value="${tenant.name}"/></p>
    </div>
  </div>        
  <div class="secondlevel_menupanel">
  <c:choose>
  <c:when test="${isAdmin && tenant.id == 1}">
  <a  class="secondlevel_menutabs <c:out value="${Health}"/>" href="<%=request.getContextPath() %>/portal/health">
		<spring:message code="page.level2.systemhealth"/>
	</a>
	<c:if test="${ticketCapabilities == 'CRUD'}">
    <sec:authorize access="hasRole('ROLE_TICKET_MANAGEMENT')">
      <spring:url value="/portal/support/tickets" var="tickets_path" htmlEscape="false">
        <spring:param name="tenant"><c:out value="${tenant.param}"/></spring:param>
      </spring:url>   
      <a class="secondlevel_menutabs <c:out value="${Tickets}"/>" href="<c:out value="${tickets_path}"/>">
        <spring:message code="page.level2.tickets"/>
      </a> 
    </sec:authorize>
  </c:if>
  </c:when>
  <c:otherwise>
  <c:if test="${effectiveTenant.state eq 'ACTIVE'}">
    <a class="secondlevel_menutabs <c:out value="${Health}"/>" href="<%=request.getContextPath() %>/portal/health">
        <spring:message code="page.level2.health"/>
    </a>
    </c:if>
  	<sec:authorize access="hasAnyRole('ROLE_TICKET_MANAGEMENT', 'ROLE_USER_TICKET_MANAGEMENT', 'ROLE_SPL_USER_TICKET_MANAGEMENT', 'ROLE_TENANT_TICKET_MANAGEMENT')">
  		<c:set var="hasTicketCreateRole" value="true"/>
    </sec:authorize>
    <c:if test="${hasTicketCreateRole && ticketCapabilities == 'CRUD'}">
  		<spring:url value="/portal/support/tickets" var="tickets_path" htmlEscape="false">
    		<spring:param name="tenant"><c:out value="${tenant.param}"/></spring:param>
    	</spring:url>   
    	<a class="secondlevel_menutabs <c:out value="${Tickets}"/>" href="<c:out value="${tickets_path}"/>">
      		<spring:message code="page.level2.tickets"/>
    	</a>
 	</c:if>
  </c:otherwise>
</c:choose>        
    
  </div>
  
  <tiles:insertDefinition name="warnings"></tiles:insertDefinition> 
  
</div>  