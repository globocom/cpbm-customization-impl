<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<script type="text/javascript">
if( typeof accountTypeDictionary === 'undefined' ) {
  var dictionary = {};
}
var dictionary = {  
    lightboxbuttoncancel: '<spring:message javaScriptEscape="true" code="label.cancel"/>',  
    lightboxbuttonconfirm: '<spring:message javaScriptEscape="true" code="label.confirm"/>', 
    lightboxrequestPasswordReset : '<spring:message javaScriptEscape="true" code="message.myprofile.reset.password.request"/>', 
    deleteconfirm:  '<spring:message javaScriptEscape="true" code="js.alert.preference.delete.confirm.message"/>',
    makeprimaryconfirm:  '<spring:message javaScriptEscape="true" code="js.alert.preference.make.primary.confirm.message"/>',
    verifyconfirm:  '<spring:message javaScriptEscape="true" code="js.alert.preference.verify.confirm.message"/>'      
};

</script>
<input type="hidden" id="effectiveUserParam" value="<c:out value="${user.param}"/>"/>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/notifications.js"></script>
<jsp:include page="js_messages.jsp"></jsp:include>
  <!--  Add new alert delivery option starts here-->
      <!-- Start List of Notifications -->	
      	<div class="widget_grid" style="margin-top:10px; border:none;">
        	<!-- Header -->
        	<div class="widget_grid header">
            <div class="widget_grid_cell" style="width:49%;"><span class="header"><spring:message code="ui.notification.delivery.email.address" /></span></div>
            <div class="widget_grid_cell" style="width:25%;"><span class="header"><spring:message code="ui.notification.delivery.isverified" /></span></div>
            <div class="widget_grid_cell" style="width:25%;"><span class="header"><spring:message code="ui.notification.delivery.action" /></span></div>
          </div>
          <!-- Header end -->
            <c:choose>
              <c:when test="${empty alertsPrefs || alertsPrefs == null}">
                <spring:message var="notificationsMsg" code="ui.label.emptylist.notificationsemails" ></spring:message>
                <spring:message code="ui.label.emptylist.notavailable" arguments="${notificationsMsg}" htmlEscape="false"/>
              </c:when>
              <c:otherwise>
              <c:forEach items="${alertsPrefs}" var="preference" varStatus="status">
              <c:choose>
                <c:when test="${status.index % 2 == 0}">
                  <c:set var="rowClass" value="odd"/>
                </c:when>
                <c:otherwise>
                  <c:set var="rowClass" value="even"/>
                </c:otherwise>
              </c:choose> 
              <div class="widget_grid <c:out value="${rowClass}"/>" id="div<c:out value="${preference.id}"/>">
                <div class="widget_grid_cell" style="width:49%;"><span class="celltext" id="email-<c:out value="${preference.id}"/>"><c:out value="${preference.emailAddress}"/></span> </div>
                
                <div class="widget_grid_cell" id = "emailVerifiedIconDiv" style="width:26%;">
                  <c:choose>
                    <c:when test="${!preference.emailVerified}">
                      <span class="unverifiedicon"  >
                      </span>
                      <span class="celltext" ><a href="javascript:void(0)" class="verifyAlertPref" name="<c:out value="${preference.id}"/>"><spring:message code="ui.notification.delivery.label.verify" /></a>
                      </span> 
                    </c:when>
                    <c:otherwise>
                      <span class="verifiedicon"  ></span>
                      <span class="celltext" ><spring:message code="ui.notification.delivery.isverified" /></span> 
                    </c:otherwise>
                  </c:choose>
                </div>
                    
                <div class="widget_grid_cell" id = "emailVerifiedIconDivTemplate" style="width:26%; display:none">
                   <span class="unverifiedicon" id = "unverifyIcon" style="display: none"></span>
                   <span class="celltext" id = "unverifyText" style="display: none"><a href="javascript:void(0)" class="verifyAlertPref"><spring:message code="ui.notification.delivery.label.verify" /></a></span> 
                   <span class="verifiedicon" id = "verifyIcon" style="display: none"></span>
                   <span class="celltext" id = "verifyText" style="display: none"><spring:message code="ui.notification.delivery.isverified" /></span> 
                </div> 
                    
                <div class="widget_grid_cell" style="width:auto;"> 
                    <span class="celltext">
                    <a href="javascript:void(0)" class="deleteAlertPref" name="<c:out value="${preference.id}"/>"><spring:message code="ui.notification.delivery.label.delete" /></a>
                    </span>
                  <c:if test="${preference.emailVerified}">
                      <span class="celltext" id ="makePrimaryDiv">
                      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="javascript:void(0)" class="makePrimary" name="<c:out value="${preference.id}"/>"><spring:message code="ui.notification.delivery.label.changePrimary" /></a>
                      </span>
                  </c:if>
                 </div>
                
              </div>                
            </c:forEach>
            </c:otherwise>
            </c:choose>
            
        </div>
         <div class="main_addnew_formbox_errormsg" style="margin:2px 0 0 1px" id="additional_emailError"></div>
           <div class="clearboth"></div>
          <div style="margin: 20px 0 0;">
        <div id="emailErrorMessage" style="background: #F5A9A9;color: #190707;padding:5px 0 5px 5px;display:none;"></div>
        <div id="emailSuccessMessage" style="background-color:#FFF8D6;padding:5px 0 5px 5px;display:none;"></div>
        </div>
    <div class="" id="addEmailTextDiv"
           <c:if test="${alertsPrefsSize >= addAlertEmailLimit}">
           style="display:none;"
           </c:if>
          >    
      <div class="addsecemailbox" style="margin-top:15px;">
        <div class="addsecemail_highlighttitlebox">
          <spring:url value="/portal/tenants/alert_prefs" var="add_email_path" htmlEscape="false" />
          <form:form commandName="userAlertEmailForm" id="userAlertEmailForm" cssClass="ajaxform" action="${add_email_path}">
            <h4><spring:message code="ui.notification.delivery.label.additional.email"/></h4>
            <div style="float:left;width:256px;">
            <div class="mandatory_wrapper">
              <form:input cssClass="text" path="email"/>
            </div>
            <div class="main_addnew_formbox_errormsg" id="emailError" style="width:480px;margin:10px 0 0 15px">
            </div>
            </div>
            <div style="float:right;padding-right:20px;">
            <div class="commonbox_submitbuttonbox">
              <input id="addsecalert" class="commonbutton" type="submit" value="<spring:message code="ui.notification.delivery.label.addEmail"/>"/>
            </div>
            </div>
          </form:form>
        </div>
      </div>
    </div>  
     
      
