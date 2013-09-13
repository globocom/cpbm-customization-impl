<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<div class="widget_actionbar">
	<div id="top_actions" class="widget_actionarea">
		<div style="display: none" id="spinning_wheel">
			<div class="maindetails_footer_loadingpanel"></div>
			<div class="maindetails_footer_loadingbox first">
				<div class="maindetails_footer_loadingicon"></div>
				<p id="in_process_text"></p>
			</div>
		</div>
	</div>
</div>
<div id="top_message_panel" class="common_messagebox widget" style="display:none;"><span id="status_icon"></span><p id="msg"></p></div>
<div id="action_result_panel" class="common_messagebox widget" style="display:none;"><span id="status_icon"></span><p id="msg">${errorMsg}</p></div>

<div class="widget_browser">
 	<!-- <div id="spinning_wheel" style="display: none">
		<div class="widget_loadingpanel"></div>
		<div class="maindetails_footer_loadingbox first">
			<div class="maindetails_footer_loadingicon"></div>
			<p id="in_process_text"></p>
		</div>
	</div> -->
	<div class="widget_browsermaster">
		<div class="widget_browser_contentarea">
			<div class="widget_browsergrid_wrapper master">
				<div class="widget_grid master even first">
					<div class="widget_grid_labels">
						<span><spring:message code="billing_history.list.header.date" /></span>
					</div>
					<div class="widget_grid_description">
						<span> 
							<spring:message code="dateonly.format" var="dateonly_format" /> <fmt:formatDate
								value="${salesLedgerRecord.createdAt}" pattern="${dateonly_format}" timeZone="${currentUser.timeZone}" />
						</span>
					</div>
				</div>

			 
			     <div class="widget_grid master even">
					<div class="widget_grid_labels">
						<span><spring:message code="label.billing.history.billingUUId.payment" /></span>
					</div>
					<div class="widget_grid_description">
						<span class="uuid"> <c:out value="${salesLedgerRecord.uuid}" /></span>
					</div>
				</div>
				<div class="widget_grid master even">
					<div class="widget_grid_labels">
						<span><spring:message code="billing_history.list.header.amount" /></span>
					</div>
					<div class="widget_grid_description">
						<span> 
								<c:out value="${salesLedgerRecord.tenant.currency.sign}" />
								<fmt:formatNumber pattern="${currencyFormat}" value="${salesLedgerRecord.transactionAmount}" minFractionDigits="${minFractionDigits}" />
						</span>
					</div>
				</div>
			</div>
			<div class="widget_masterbigicons payments"></div>
		</div>
	</div>

	<div class="widget_browser_contentarea">
		<ul class="widgets_detailstab">
			<li class="widgets_detailstab active" id="details_tab"><spring:message code="label.details" /></li>
		</ul>
		<div class="widget_details_actionbox">
			<ul class="widget_detail_actionpanel">
				<sec:authorize access="hasRole('ROLE_FINANCE_CRUD')">
					<c:if test="${salesLedgerRecord.cancellationReferenceId eq null}">
						<c:if test="${salesLedgerRecord.type eq 'AUTO' ||salesLedgerRecord.type eq 'MANUAL'}"> 
							<li class="widget_detail_actionpanel cancelCreditOrPayment">
								<a href="#" id="<c:out value="cancelCreditOrPayment${salesLedgerRecord.param}"/>" onclick="chargeBack(this);"><spring:message code="label.billing.history.issue.charge.back" /></a>
							</li>
						</c:if>
						<c:if test="${salesLedgerRecord.type eq 'NOTIONAL' || salesLedgerRecord.type eq 'RECORD'}"> 
							<li class="widget_detail_actionpanel cancelCreditOrPayment">
								<a href="#" id="<c:out value="cancelCreditOrPayment${salesLedgerRecord.param}"/>" onclick="cancelRecordedPayment(this);"><spring:message code="label.billing.history.cancel.payment" /></a>
							</li>
						</c:if>
						<c:if test="${salesLedgerRecord.type eq 'SERVICE_CREDIT'}">
							<li class="widget_detail_actionpanel cancelCreditOrPayment">
								<a href="#" id="<c:out value="cancelCreditOrPayment${salesLedgerRecord.param}"/>" onclick="cancelCredit(this);"><spring:message code="label.billing.history.cancel.credit" /></a>
							</li>
						</c:if>
					</c:if>
				</sec:authorize>
			</ul>
		</div>

		<div class="widget_browsergrid_wrapper details">
			<div class="widget_grid details even">
				<div class="widget_grid_labels">
					<span><spring:message code="label.billing.history.description" /></span>
				</div>
				<div class="widget_grid_description">
					<span><c:out value="${salesLedgerRecord.memo}" /></span>
				</div>
			</div>
			<sec:authorize access="hasRole('ROLE_FINANCE_CRUD')">
			<div class="widget_grid details even">
				<div class="widget_grid_labels">
					<span><spring:message code="label.type" /></span>
				</div>
				<div class="widget_grid_description">
					<span><c:out value="${salesLedgerRecord.type}" /></span>
				</div>
			</div>
			</sec:authorize>
			<div class="widget_grid details odd">
				<div class="widget_grid_labels">
					<span><spring:message code="label.state" /></span>
				</div>
				<c:if test="${salesLedgerRecord != null}">
					<div class="widget_grid_description">
						<span>${salesLedgerRecord.paymentTransaction.state}</span>
					</div>
				</c:if>
			</div>
			<div class="widget_grid details even">
				<div class="widget_grid_labels">
					<span><spring:message code="label.billing.history.billingId.payment" /></span>
				</div>
				<c:if test="${salesLedgerRecord != null}">
					<div class="widget_grid_description">
						<span>${salesLedgerRecord.paymentTransaction.transactionId}</span>
					</div>
				</c:if>
			</div>
		</div>
	</div>
</div>
 
<div id="updateBillingActivityDiv" style="display:none"> <!-- UNUSED REMOVE ? -->
</div>

<input id="invoice_id" type="hidden" value='<c:out value="${salesLedgerRecord.paymentTransaction.transactionId}" />'/>
