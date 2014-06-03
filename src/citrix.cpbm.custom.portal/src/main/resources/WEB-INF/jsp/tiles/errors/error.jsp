<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<c:set var="home_path" value="/home"/>
<div class="error_box" style="height:200px">
  <div class="login_headerarea">
    <div class="login_headerarea_left">
      <div class="loginlogo"><img src="/portal/portal/splogo"/></div>
    </div>
  </div>
  <div style="padding:20px;">
    <h2 style="width:auto;"><spring:message code="ui.error.page.server.error.message"/>
    <c:if test="${supportPhone ne null}">
      <c:out value="${supportPhone}"/>,
    </c:if>  
    <c:if test="${supportEmail ne null}">
      <a href="mailto:<c:out value="${supportEmail}"/>"><c:out value="${supportEmail}"/></a>, or our
    </c:if>      
    <a href="<c:out value="${supportUrl}"/>"><spring:message code="ui.error.page.support.website"/></a>.</h2>
    <br /> <br />
    <c:if test="${not empty verifyAlertEmail}">
      <div class="login_errorbox" style="display:block;">
        <p style="color:red"><spring:message code="${verifyAlertEmail}"/></p>
      </div>
    </c:if>
    <br /> <br />
    <h3 style="float:left;"><a href="<%= request.getContextPath() %>/portal<c:out value="${home_path}"/>"><spring:message code="ui.error.page.support.return.home"/></a></h3>
  </div>
</div>

