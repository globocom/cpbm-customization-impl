<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<script type="text/javascript">
<!--
$(document).ready(function() {
  setTimeout('$("#loginForm input:first").focus();', 1000);
  $('#login_button').click(function() {
    doLogin();
  });
  $(':input').keypress(function(e) {
    if (e.keyCode == 13) {
      doLogin();
    }
  });
  
  $('#suffixDropdown').change(function(e) {
    $("#suffix").val($("#suffixDropdown").val());
  });

  function doLogin(){
    if($("#showSuffixControlVar").val()=='true'){
      if($("#username").val()=="root"){
        if($("#suffix").val()=="root"){
          $("#j_username").val($("#username").val());
         }
        else{
          $("#j_username").val($("#username").val()+"@"+$("#suffix").val());
         }
      }
      else{
        $("#j_username").val($("#username").val()+"@"+$("#suffix").val());
      }
    }else{
      $("#j_username").val($("#username").val());
      }
    $("#loginForm").submit();
  }
});

//-->
</script>
            	<div class="login_headerarea  <c:if test="${directoryServiceAuthenticationEnabled}">smaller_ver</c:if>">
                	<div class="login_headerarea_left">
                    	<div class="loginlogo"><img src="/portal/portal/splogo"/></div>
                    </div>
                   
                </div>
                <div class="login_maincontentarea  <c:if test="${directoryServiceAuthenticationEnabled}">smaller_ver</c:if>">
                	<div class="login_maincontentarea_titlepanel">
                    	<h1><spring:message code="label.login.welcome"/></h1>
                    </div>  
                    <div class="login_formbox  <c:if test="${directoryServiceAuthenticationEnabled}">smaller_ver</c:if>">
				      <form id="loginForm" method="post" action="<%= request.getContextPath() %>/j_spring_security_check" name="login">
				        <input type="hidden" id="j_username" name="j_username" />
                <input type="hidden" id="showSuffixControlVar" value="<c:out value='${showSuffixControl}'/>" />
                <ol>
				          <li>
				            <label><spring:message code="label.login.username"/></label>
				            <input class="text" tabindex="1" id="username" name="username" value="<c:out value='${lastUser}' escapeXml="false"/>"/>
				          </li>  
				          <li>
				            <label><spring:message code="label.login.password"/></label>
				            <input class="text" type="password" tabindex="2" name="j_password"  autocomplete="off" />
				          </li>
                  <c:if test="${showSuffixControl=='true'}">
                    <li>
                      <label><spring:message code="label.login.page.suffix"/></label>
                      <c:if test="${suffixControlType!='dropdown'}">
                        <input class="text" tabindex="3" id="suffix" autocomplete="off"/>
                      </c:if>
                      <c:if test="${suffixControlType=='dropdown'}">
                        <select id="suffixDropdown" tabindex="3" style="margin-top: 30px; margin-left: -160px; height: 25px; width: 252px;">
                           <c:forEach var="suffixVar" items="${suffixList}" varStatus="status">
                            <c:if test="${status.index==0}">
                              <c:set var="tempSuffix" value="${suffixVar}"></c:set> 
                            </c:if>
                            <option value="${suffixVar}"><c:out value="${suffixVar}"></c:out></option>
                          </c:forEach>
                        </select>
                        <input type="hidden" id="suffix" value="${tempSuffix}"/>
                      </c:if>
                    </li>
                  </c:if>
					        <c:if test="${showCaptcha}">
                  <li style="width: 475px;">
	                   <%@include file="captcha.jsp" %>
	                </li>
	                </c:if>
                </ol>
				      </form> 
                        <div class="login_formbox_submitpanel">
                        	<div class="login_buttonscontainer">
                            	<a id="login_button" tabindex="3" class="logincommonbutton" href="#"><spring:message code="label.login.login"/></a>
                            </div>
                        </div>				          
			    </div> 
          <c:if test="${!directoryServiceAuthenticationEnabled}">  
  	        <div class="login_infobox" id="login_info_box" style="display: None">
              	<div class="login_infobox_top"></div>
              	<div class="login_infobox_mid">
              	
  		              <div class="login_infocontentbox">
                      <c:if test="${showSuffixControl!='true'}">                    
  		                <p><spring:message code="label.login.dontHaveAnAccount"/></p>
  		                <a href="/portal/portal/account_type"><spring:message code="label.login.signUpNow"/></a>
                       </c:if>               
                      <p><spring:message code="label.login.forgotPassword"/></p>
                      <a href="/portal/portal/reset_password"><spring:message code="label.login.requestReset"/></a>
  		              </div>
  		        </div>
  		          <div class="login_infobox_bot"></div>
              </div> 
          </c:if>      
                
			  </div>

<c:set var="loginreturn" value="success" scope="session" />
<c:if test="${loginFailed}">
  <div class="login_messages  <c:if test="${directoryServiceAuthenticationEnabled}">smaller_ver</c:if> error">
    <p><strong><spring:message code="label.login.loginFailed"/><c:out value="${error}"/></strong></p>
    <p><spring:message code="label.login.reenterCredentials"/></p>
    <c:if test="${!directoryServiceAuthenticationEnabled}"> 
      <p><spring:message code="label.login.forgotPassword"/> <a href="<%= request.getContextPath() %>/portal/reset_password"><spring:message code="label.login.requestReset"/></a></p>
    </c:if>
  </div>
</c:if>
<c:choose>
	<c:when test="${logout}">
	  <div class="login_messages <c:if test="${directoryServiceAuthenticationEnabled}">smaller_ver</c:if> success">
	    <p><spring:message code="label.login.youSignedOut"/></p>
	  </div>
	</c:when>
	<c:when test="${verify}">
	  <div class="login_messages <c:if test="${directoryServiceAuthenticationEnabled}">smaller_ver</c:if> success">
	    <p><spring:message code="label.login.passwordsetnemailVerified"/></p>
	  </div>
	</c:when>
	<c:when test="${timeout}">
	  <div class="login_messages <c:if test="${directoryServiceAuthenticationEnabled}">smaller_ver</c:if> success">
	    <p><spring:message code="label.login.sessionTimeout"/></p>
	  </div>
	</c:when>
</c:choose>

<input id="from_login_page" value="true" style="display: None"></input>
