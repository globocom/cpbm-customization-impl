<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="net.tanesha.recaptcha.ReCaptcha" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<spring:url	value="/portal/users/deployVm" var="deployVm">
	<spring:param name="tenant">
		<c:out value="${tenant.param}" />
	</spring:param>
</spring:url>

<c:if test="${empty jobId}">
	<div style="padding: 10px 12px; font-size: 12px; background: none repeat scroll 0% 0% transparent;"
	  class="submit_button ui-corner-bottom ui-corner-top">
	  <a href="<c:out value="${deployVm}"/>">Launch VM</a>
	</div>
</c:if>
  
<c:if test="${not empty jobId}">
   VM launch in progress, jobId = <c:out value="${jobId}"/>
</c:if>

