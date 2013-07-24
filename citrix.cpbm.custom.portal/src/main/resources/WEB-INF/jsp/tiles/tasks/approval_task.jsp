<%-- Copyright (C) 2012 Citrix, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<spring:message code="date.format" var="date_format"/>
<spring:url var="actionURL" value="/portal/tasks/approval-task"></spring:url>

<div class="dialog_formcontent wizard">
 <div class="j_actionForm">
  <div class="widgetwizard_contentarea sixstepswizard">
    <div class="widgetwizard_boxes fullheight sixstepswizard">
      <div class="widgetwizard_titleboxes">
        <h2><spring:message code="task.approval.dialog.review.title" /></h2>
        <span style="width:100%"><spring:message code="task.approval.dialog.review.description" /></span>
      </div>
      <div class="widgetwizard_reviewbox sixstepswizard ">
        <ul>
          <li>
            <span class="label"><spring:message code="ui.task.transaction" />:</span>
            <span class="description j_description"><spring:message code="ui.task.transaction.type.${task.businessTransaction.type}" /></span>
			<c:out value="${taskDetails}" escapeXml="false"></c:out>
            <c:choose>
            <c:when test="${task.businessTransaction.type == 'tenantStateChange' }">
	            <c:set var="transactionMessage">
					<spring:message
						code="ui.task.businesstransaction.${task.businessTransaction.type}"
						arguments="${task.businessTransaction.tenantInitialState},${task.businessTransaction.tenantTargetState},${task.tenant.name}"></spring:message>
				</c:set>
            </c:when>
            <c:when test="${task.businessTransaction.type == 'tenantAccountTypeConversion' }">
				<c:set var="transactionMessage">
					<spring:message
						code="ui.task.businesstransaction.${task.businessTransaction.type}"
						arguments="${task.businessTransaction.accountTypeInitial.name},${task.businessTransaction.accountType.name},${task.tenant.name}"></spring:message>
				</c:set>           
            </c:when>
            <c:when test="${task.businessTransaction.type == 'paymentInfoChange' }">
				<c:set var="transactionMessage">
					<spring:message
						code="ui.task.businesstransaction.${task.businessTransaction.type}"
						arguments="null,null,${task.tenant.name}"></spring:message>
				</c:set>
            </c:when>
            <c:when test="${task.businessTransaction.type == 'subscriptionActivation' }">
            	<c:set var="transactionMessage">
					<spring:message code="ui.task.businesstransaction.${task.businessTransaction.type}"
						arguments="${task.businessTransaction.subscriptionInitialState},${task.businessTransaction.subscriptionTargetState},${task.businessTransaction.subscription.user.lastName}"></spring:message>
				</c:set>
            </c:when>
            <c:otherwise></c:otherwise>
            </c:choose> 
          </li>
          <li>
            <span class="label"><spring:message code="ui.task"/>:</span>
            <span class="description j_description"></span>
            <ul> 
              <li class="subselection">
                <span class="label sublabel"><spring:message code="ui.task.type" />  </span>
                <span class="description subdescription ellipsis" ><spring:message code="ui.task.type.${task.type}.name" /></span>
              </li>
              <li class="subselection">
                <span class="label sublabel"><spring:message code="ui.task.state" />  </span>
                <span class="description subdescription ellipsis" id="taskState">
                  <spring:message code="ui.task.state.${task.state}" />
                </span>
              </li>
              <li class="subselection">
                <span class="label sublabel"><spring:message code="ui.task.createdAt" />  </span>
                <span class="description subdescription ellipsis"><fmt:formatDate value="${task.createdAt}" pattern="${date_format}" type="date" dateStyle="MEDIUM" timeZone="${currentUser.timeZone}"/></span>
              </li>
              <c:if test="${task.state != 'SUCCESS' && task.state != 'FAILURE'}">
                <form:form action="${actionURL}" method="post" id="approval_task_form" commandName="task">
                  <input type="hidden" value="${task.uuid}" id="approval_task_uuid" />
                    <li class="subselection">
                      <span class="label sublabel"><spring:message code="ui.accounts.all.pending.changes.memo" /></span>
                      <div class="mandatory_wrapper">
                        <form:textarea path="memo" class="commonboxes_formbox_withouttextbox equalwidth"></form:textarea>
                      </div>
                      <div id="memo_errormsg" class="dialog_formcontent_errormsg"></div>
                    </li>
                </form:form>
              </c:if>
            </ul>
          </li>
        </ul>
      </div>
    </div>

		<div id="spinning_wheel_rhs" style="display: none;">
			<div class="widget_blackoverlay widget_rightpanel"
				style="height: 100%"></div>
			<div class="widget_loadingbox tasks">
				<div class="widget_loaderbox">
					<span class="bigloader"></span>
				</div>
				<div class="widget_loadertext">
					<p id="in_process_text">
						<spring:message code="label.loading" />
					</p>
				</div>
			</div>
		</div>
	</div>
  <div id="buttons" class="widgetwizard_nextprevpanel sixstepswizard">
    <c:if test="${task.state != 'SUCCESS' && task.state != 'FAILURE'}">
      <input type="button" id="approval_task_success" name="<spring:message code="ui.accounts.all.pending.changes.approve"/>" value="<spring:message code="ui.accounts.all.pending.changes.approve"/>"
        class="widgetwizard_nextprevpanel submitbutton">
      <input type="button" id="approval_task_failure" name="<spring:message code="ui.accounts.all.pending.changes.reject"/>" value="<spring:message code="ui.accounts.all.pending.changes.reject"/>" 
        class="widgetwizard_nextprevpanel submitbutton">
    </c:if>
    <a class="cancel close_enable_service_wizard" href="javascript:void(0);" id="approval_task_close"><spring:message code="label.cancel"/></a>
  </div>
  </div>
 <div class="j_success" style="display:none">
	 <div class="widgetwizard_contentarea sixstepswizard">
	    <div class="widgetwizard_boxes fullheight sixstepswizard">
	        <c:set var="taskMessage">
					<spring:message code="ui.widget.task.type.${task.type}.name"/>
			</c:set>
			<div class="widgetwizard_successbox tasks">
				<div class="widget_resulticon success"></div>
				<p class="hidden j_approved"><spring:message code="ui.widget.task.approved" arguments="${taskMessage},${transactionMessage}" /></p>
				<p class="hidden j_rejected"><spring:message code="ui.widget.task.rejected" arguments="${taskMessage},${transactionMessage}" /></p>
			</div>      
	    </div>
		</div>
	  <div id="okbuttons" class="widgetwizard_nextprevpanel sixstepswizard">
	    <input type="button" id="okbutton" name="<spring:message code="label.ok"/>" value="<spring:message code="label.ok"/>"  class="widgetwizard_nextprevpanel submitbutton">
	  </div>
  </div> 
</div>
