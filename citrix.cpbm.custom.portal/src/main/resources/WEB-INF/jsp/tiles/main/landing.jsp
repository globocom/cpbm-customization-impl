<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<div class="login_midcontentbox">
  <h2>
    Welcome to the Cloud.com CloudPortal. 
  </h2>
  <div class="login_formcontent loginbutton_box">
    <label>New to the cloud?</label>
    <a href="<%= request.getContextPath() %>/portal/register">Signup</a>
  </div>
  <div class="login_formcontent loginbutton_box">
    <label>Already have an account?</label>
    <a href="<%= request.getContextPath() %>/portal/login">Signin</a>
  </div>
  <div class="clear_both"></div>
</div>
