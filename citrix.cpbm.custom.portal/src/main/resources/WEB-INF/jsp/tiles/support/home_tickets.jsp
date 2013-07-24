<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>

<c:if test="${totalTickets <= 5}">
      <c:set var="ticketsShown" value="${totalTickets}"/>
    </c:if>
    <c:if test="${totalTickets > 5}">
      <c:set var="ticketsShown" value="5"/>
    </c:if>
    
<div class="dbboxes_maintitlebox">
          <h2><spring:message code="ui.home.page.title.tickets"/></h2>
          <span>(
            <spring:message code="ui.home.page.tickets.number" arguments="${ticketsShown}, ${totalTickets}"/>
          )</span>
        </div>
        <div class="dbboxes ticketsbox">
          <div class="db_gridbox">
            <div class="db_gridbox_rows header">
              <div class="db_gridbox_columns" style="width:35%;">
                <div class="db_gridbox_celltitles header"><spring:message code="ui.home.page.title.tickets.hash"/></div>
              </div>
              <div class="db_gridbox_columns" style="width:60%;">
                <div class="db_gridbox_celltitles header"><spring:message code="ui.home.page.title.subject"/></div>
              </div>
            </div>
            <c:if test="${ error != null }">
	            <div class="error">
	              <div class="icon">
	                <div class="db_gridbox_typeicons alerts"></div>
	              </div>
	              <div class="message">
	              	<spring:message code="tickets.${error}"/>
	              </div>
	            </div>
            </c:if>
            <c:forEach var="ticketsObj" items="${tickets}" varStatus="status">
              <c:choose>
                <c:when test="${status.count % 2 == 0}">
                  <c:set var="rowStyle" value="even"/>
                </c:when>
                <c:otherwise>
                  <c:set var="rowStyle" value="odd"/>
                </c:otherwise>
              </c:choose>
              <div class="<c:out value="db_gridbox_rows ${rowStyle}"/>" style="height:20px;line-height:20px;overflow:hidden;">
                <div class="db_gridbox_columns" style="width:35%;">
                <spring:url value="/portal/support/tickets" var="all_tickets_path" htmlEscape="false">
                	<spring:param name="tenant"><c:out value="${tenant.param}"/></spring:param>
                  <spring:param name="ticketNumber"><c:out value="${ticketsObj.caseNumber}"/></spring:param>
             	 	</spring:url> 
                  <div class="db_gridbox_celltitles"><a href="<c:out value="${all_tickets_path}"/>"><c:out value="${ticketsObj.caseNumber}"/></a></div>
                </div>
                <div class="db_gridbox_columns" style="width:60%;">
                  <div class="db_gridbox_celltitles">
                    <c:out value="${ticketsObj.subject}"/> 
                  </div>
                </div>
              </div>
            </c:forEach>
          </div>
          <div class="dbboxes_footerlinksbox">
            <spring:url value="/portal/support/tickets" var="all_tickets_path" htmlEscape="false">
            <spring:param name="tenant"><c:out value="${tenant.param}"/></spring:param>
          </spring:url> 
            <p><a href="<c:out value="${all_tickets_path}"/>"><spring:message code="ui.home.page.title.view.all.tickets"/></a></p>
            <sec:authorize access="hasAnyRole('ROLE_USER_TICKET_MANAGEMENT', 'ROLE_TICKET_MANAGEMENT')">
            	<p>|</p> <p><a href="<c:out value="${all_tickets_path}&showNewTicket=1"/>"><spring:message code="ui.home.page.title.submit.ticket"/></a></p>
            </sec:authorize>
          </div>
        </div>