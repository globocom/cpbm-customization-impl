<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<jsp:include page="/WEB-INF/jsp/tiles/accounttypes/js_messages.jsp"></jsp:include>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/accounttype.js"></script>
<style>
.dialog_formcontent label.error {
   margin-left: -75px;
}
</style>
<!--  Edit Initial Deposit starts here-->
<div class="dialog_formcontent" >
  <div class="details_lightboxtitlebox">
  </div>
  <div class="details_lightboxformbox">
    <spring:url value="/portal/admin/editinitialdeposit" var="edit_credt_exposure_path" htmlEscape="false" /> 
    <form:form commandName="accountTypeForm" cssClass="ajaxform" id="accountTypeInitialDepositForm" action="${edit_credt_exposure_path}" onsubmit="editinitialdepositPost(event,this)">
    <ul>
      <c:forEach items="${accountTypeForm.accountType.accountTypeCreditExposureList}" varStatus="cestatus"  var="accountTypeCreditExposure">
          <li> 
            <label><c:out value="${accountTypeCreditExposure.currencyValue.currencyName}"></c:out>(<c:out value="${accountTypeCreditExposure.currencyValue.currencyCode}"></c:out>)</label>
            <div class="nonmandatory_wrapper">
              <input id="accountType.accountTypeCreditExposureList<c:out value='${cestatus.index}' />.initialDeposit" value='<c:out value="${accountTypeCreditExposure.initialDeposit}"/>' name="accountType.accountTypeCreditExposureList[<c:out value='${cestatus.index}' />].initialDeposit" class="text initialdepositclass"/>
            </div>                   
            <div class="main_addnew_formbox_errormsg edit_error_msg" id="accountType.accountTypeCreditExposureList<c:out value='${cestatus.index}' />.initialDepositError" ></div>
       </c:forEach>         
    </ul>
    </form:form>
  </div>
</div>
<!--  Edit Initial Deposit starts here-->

