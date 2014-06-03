<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ page import="net.tanesha.recaptcha.ReCaptcha"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles"%>
<tiles:insertAttribute name="pageHeader" />
<script type="text/javascript">
$(function(){
  $("#user\\.firstName").focus();
});
</script>
<div class="login_headerarea">
<div class="login_headerarea_left">
<div class="loginlogo"><img src="/portal/portal/splogo" /></div>
</div>
<!--     Commenting out for now - TODO Get an appropriate mock in
                    <div class="login_headerarea_right">
                      <c:choose>
                        <c:when test="${registration.phoneVerificationEnabled}">
                          <div class="login_wizard step1of4"></div>
                        </c:when>
                        <c:otherwise>
                          <div class="login_wizard step1of3"></div>
                        </c:otherwise>
                      </c:choose>                       
                    </div>
                      --></div>
<div class="login_maincontentarea">
<c:choose>
  <c:when test="${! empty signupwarningmessage}">
     <div class="common_messagebox success">
          <p><c:out value="${signupwarningmessage}"></c:out> </p>
      </div>
  </c:when>
  <c:otherwise>
    <div class="login_maincontentarea_titlepanel">
		<h1><spring:message code="label.userInfo.account_type_selection" /></h1>
		</div>
		
		<spring:url value="/portal/signup" var="signup" htmlEscape="false" /> 
		<form:form method="get" name="registration" commandName="registration" id="accountTypeForm" cssClass="registration formPanel" action="${signup}">
			<c:forEach items="${registration.selfRegistrationAccountTypes}" var="choice" varStatus="status">
			    <div class="registration_formbox">
			      <div class="accountype_box"> 
			                <div class="accountype_box_left">
			                            <div class="accountype_icon <c:out value="${choice.nameLower}" />"></div>
			                            <a id="<c:out value="${choice.id}" />" class="accountype_selectbutton" href="#"><spring:message  code="select" /></a>
			                </div>
			        <div class="accountype_box_right">
			             <h3><spring:message code="registration.accounttype.${choice.nameLower}" /></h3>
			             <p><spring:message code="registration.accounttype.description.${choice.nameLower}"/></p>
			          </div>
			      </div>
			     </div>
			  </c:forEach>

			<form:hidden id="account_type_select" path="accountTypeId" cssClass="text"/>
		</form:form>
  </c:otherwise>
</c:choose>

</div>
