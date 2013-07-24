<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<div class="dialog_formcontent" style="margin-top:0px">  
	<div class="details_lightboxformbox" style="border-bottom-style:none;padding-bottom:0px;width:auto">
		<ul>
			<li>
				<span style="width:auto;margin-left:0px"><spring:message code="label.myprofile.apicredentials.password"/></span>
			</li>
			<li>
				<label><spring:message code="label.password"/>:</label>
				<input id="password" type="password" class="text" name="password" tabindex="1"/>
			</li>
			<li>
				<div class="main_addnew_formbox_errormsg" id="wrongPasswordError" style="min-height: 15px;"></div>
			</li>
		</ul>
	</div>

	<div class="main_addnew_submitbuttonpanel" style="padding-bottom:0px;">
		<div class="main_addnew_submitbuttonbox">
			<a style="cursor:pointer;" onclick="closePasswordDialog();" id="dialogcancel"><spring:message code="label.cancel"/></a>
			<input type="submit" id="verifyPassword"  value="<spring:message code="label.proceed"/>" class="commonbutton submitmsg" tabindex="100">
		</div>
	</div>
</div>