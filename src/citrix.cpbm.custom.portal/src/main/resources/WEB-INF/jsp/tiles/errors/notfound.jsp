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
    <h2 style="width:auto;"><spring:message code="ui.error.page.not.found"/></h2>
    <br /> <br />
    <h3 style="float:left;"><a href="<%= request.getContextPath() %>/portal<c:out value="${home_path}"/>"><spring:message code="ui.error.page.not.found.return.home"/></a></h3>
  </div>
</div>
