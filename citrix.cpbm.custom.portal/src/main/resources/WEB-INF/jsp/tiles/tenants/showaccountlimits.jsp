<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/tenants.js"></script>

<div class="widget_details_actionbox">
	<ul class="widget_detail_actionpanel">
		<spring:url value="/portal/home" var="tenant_dashboard" htmlEscape="false">
			<spring:param name="tenant">
				<c:out value="${tenantuuid}" />
			</spring:param>
			<spring:param name="secondLevel">
				<c:out value="true" />
			</spring:param>
		</spring:url>

		<sec:authorize access="hasAnyRole('ROLE_ACCOUNT_CRUD')">
			<c:if test="${tenantid != 1}">
				<li>
					<a href="javascript:void(0);" onclick="editAccountLimits(this);" class="editAccountLimits"
						tenantuuid="${tenantuuid}" instanceparam="${instance.uuid}">
						<spring:message code="ui.label.tenant.view.edit" />
					</a>
				</li>
			</c:if>
		</sec:authorize>
	</ul>
</div>
<div class="widget_browsergrid_wrapper details" id="instanceparam_${instance.uuid}">
	<c:forEach var="entry" items="${resourceLimitsMap}" varStatus="status">
		<div class="widget_grid details even">
			<div class="widget_grid_labels" style="width:200px">
				<span style="width:auto">
					<spring:message code="${instance.service.serviceName}.${entry.key}.name" />
				</span>
			</div>
			<div class="widget_grid_description">
				<span id="${entry.key}">
					<c:out value="${entry.value}"></c:out>
				</span>
			</div>
		</div>
	</c:forEach>
	<%-- <div class="widget_grid details even">
		<div class="widget_grid_labels" style="width: 350px">
			<span>
				<spring:message code="label.accountcontrol.maxuser.limit" />
			</span>
		</div>
		<div class="widget_grid_description">
			<span>
				<c:out value="${userLimit}"></c:out>
			</span>
		</div>
	</div> --%>
</div>

<!-- Title -->
<div id='messageBox' class='common_messagebox success successMessage' style="margin: 0; display: none"></div>


