<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fmt:setLocale value="${pageContext.request.locale}" scope="request"/>
<c:choose>
  <c:when test="${currentUser != null}">
    <c:set var="home_path" value="/home"/>
  </c:when>
  <c:otherwise>
    <c:set var="home_path" value="/"/>
  </c:otherwise>
</c:choose>
<div id="co_logoheader" class="co_logoheader" style="width:724px; display:inline;">
  <div id="co_logoheader_left" class="co_logoheader">
    <a class="cloud_logo" href="<%= request.getContextPath() %>/portal<c:out value="${home_path}"/>"></a>
  </div> 
</div>
