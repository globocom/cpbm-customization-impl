<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<div class="error_box" style="height:200px">
  <div class="login_headerarea">
    <div class="login_headerarea_left">
      <div class="loginlogo"><img src="/portal/portal/splogo"/></div>
    </div>
  </div>
  <div style="padding:20px;">
    <h2 style="width:auto;"><spring:message code="generate.accountStatement.pdf.error"/></h2>
    <br /> <br />
    <h3 style="float:left;"><a href="javascript:history.back()"><spring:message code="ui.home.page.title.go.to.billing.usage"/></a></h3>
  </div>
</div>
