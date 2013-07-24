<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<div class="login_midcontentbox">
  <h2>
    You have logged out.  
  </h2>
  <div class="loginbutton_box">
    <a class="button" href="<%= request.getContextPath() %>">Signin</a>
  </div>
  <div class="clear"></div>
</div>
