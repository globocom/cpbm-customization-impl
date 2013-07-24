<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/validator.js"></script>

<style type="text/css">
.tooltip {
	background-color: #CCCCCC;
	border: 1px solid #fff;
	padding: 10px 15px;
	width: 240px;
	display: none;
	color: #fff;
	text-align: left;
	font-size: 12px;
	z-index: 10000;
}

.dialog_formcontent label.error {
	margin-left: 265px;
}
</style>
<script type="text/javascript">
$("#accountControlsForm").validate({
	submitHandler : function() {
		var noOfErrors = $("#accountControlsForm").find('label.error:visible').length;
		if (noOfErrors == 0) {
			saveAcccountControls();
		}
		return false;
	}
});

$('input[id^="configproperty"]').tooltip({
	position : "center right",
	offset : [ 0, 0 ],
	effect : "fade",
	opacity : 0.7
});

$("#submitbutton").unbind("click").bind("click", function(event) {
	if ($("#submitbutton").hasClass('active')) {
		$('#accountControlsForm').submit();
	}
});
</script>

<div class="dialog_formcontent">
	<div class="details_lightboxtitlebox"></div>
	<div class="details_lightboxformbox">
		<form id="accountControlsForm">
			<input type="hidden" id="instanceParam" value="${instance.uuid}" />
			<input type="hidden" id="tenantParam" value="${tenantuuid}" />
			<c:choose>
				<c:when test="${account_control_edit_properties!=null}">
					<input type="hidden" id="action" value="save" />
					<ul>
						<c:forEach var="account_control_property" items="${account_control_edit_properties}" varStatus="status">
							<li style="height: 28px">
								<label style="width: 260px">
									<spring:message code="${service.serviceName}.${account_control_property.name}.name" />
								</label>
								<div class="mandatory_wrapper" style="float: none">
									<c:choose>
										<c:when test="${account_control_property.type=='Boolean'}">
											<span style="padding-left: 6px;" />
											<span>
												<input type="radio" id="configbooleantrue<c:out value="${account_control_property.id}"/>"
													name="${account_control_property.name}" checked />
												<spring:message code="label.true" />
											</span>
											<span>
												<input type="radio" id="configbooleanfalse<c:out value="${account_control_property.id}"/>"
													name="${account_control_property.name}" />
												<spring:message code="label.false" />
											</span>
										</c:when>
										<c:otherwise>
											<input type="text" id="configproperty<c:out value="${account_control_property.id}"/>"
												name="${account_control_property.name}"
												title="<spring:message  code="${service.serviceName}.${account_control_property.name}.tooltip"/> "
												class="text ${account_control_property.validations.classValidations}"
												${account_control_property.validations.validations} value="${resLimitMap[account_control_property.name]}" />
										</c:otherwise>
									</c:choose>
								</div>
							</li>
						</c:forEach>
						<li style="width: 530px">
							<div class="servicelist sections" style="height: 25px; float: right">
								<a class="active add_button" id="closedialog" href="javascript:void(0);"
									onclick="closeAccountTypeControlsDialog()">
									<spring:message code="label.cancel" />
								</a>
								<a class="active add_button" id="submitbutton" href="javascript:void(0);">
									<spring:message code="label.save" />
								</a>
							</div>
						</li>
					</ul>
				</c:when>
			</c:choose>
		</form>
	</div>
</div>
