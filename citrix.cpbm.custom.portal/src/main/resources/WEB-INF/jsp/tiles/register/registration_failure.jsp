<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<div class="login_headerarea">
  <div class="login_headerarea_left">
        <div class="loginlogo"><img src="/portal/portal/splogo"/></div>
      </div>     
  </div>
  <div class="login_maincontentarea">
    <div class="login_maincontentarea_titlepanel">
      <h1><spring:message code="label.registration.failure"/></h1>
    </div> 
    <div class="login_errorbox">                
      <p><spring:message code="message.registration.failure"/></p> 
      <div class="clearboth"></div>
      <br/>
      <a href="<%= request.getContextPath() %>/portal/"><spring:message code="message.reset.password.login"/></a>
      <br/>
    </div>
</div>  

