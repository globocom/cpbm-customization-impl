<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">


<script type="text/javascript" src="<%=request.getContextPath()%>/resources/all.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/common.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/csrf_js_servlet"></script>
<link rel="stylesheet" type="text/css" media="all" href="<%=request.getContextPath()%>/resources/all.css"/>
<link rel="stylesheet" type="text/css" media="all" href="<%=request.getContextPath()%>/css/main.css"/>
<link rel="stylesheet" type="text/css" media="all" href="<%=request.getContextPath()%>/css/resourcesView.css"/>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/serviceAccountConfig.js"></script>
<script type="text/javascript">
var effectiveTenantParam = '<c:out value="${effectiveTenant.param}"/>';
var currentLoggedinTenant = '<c:out value="${currentTenant.param}"/>';
var jspProvidedByService = '<c:out value="${jspProvidedByService}"/>';
var foundJspProvidedByService = '<c:out value="${not empty jspProvidedByService}"/>';
var currentServiceInstanceUUID = '<c:out value="${serviceInstanceUUID}"/>';
var emptyServiceAccountConfigProperties = '<c:out value="${empty service_account_config_properties}"/>';
var serviceAccountConfigPropertiesArray = '<c:out value="${service_account_config_properties_list}"/>';
var service_config_message_dict={
		enableServiceError:'<spring:message javaScriptEscape="true" code="message.enable.service.error"/>'
};
</script>
<form id="serviceAccountConfigParamForm" style="margin:0; display:none;">


<div id = "mainContentForServiceAccConfigParam" class="service_detail_subsection">
<div class="contentarea">
<c:choose>
<c:when test="${service_account_config_properties != null && empty jspProvidedByService}">
<c:forEach var="service_account_config_property" items="${service_account_config_properties}" varStatus="status">
	<c:choose>
		<c:when test="${status.index % 2 == 0}">
			<c:set var="rowClass" value="odd" />
		</c:when>
		<c:otherwise>
			<c:set var="rowClass" value="even" />
		</c:otherwise>
	</c:choose>
	<li class="row ${rowClass}">
		<span class="label"><spring:message  code="${service.serviceName}.${service_account_config_property.name}.name"/></span> 
		<span class="description">
			<c:choose>
				<c:when test="${service_account_config_property.type=='Boolean'}">
					<span style="padding-left:6px;"></span><span><input type="radio" id="configbooleantrue<c:out value="${service_account_config_property.id}"/>" name="${service_account_config_property.name}" checked /><spring:message  code="label.true"/></span>
					<span><input type="radio" id=sacp_"<c:out value="${service_account_config_property.name}"/>" name="${service_account_config_property.name}"/><spring:message  code="label.false"/></span>
				</c:when>
				<c:otherwise>
					<c:choose>
						<c:when test="${service_account_config_property.validations.required}">
							<span style="color: rgb(255, 0, 0); padding-left:2px; float: left">*</span>
						</c:when>
						<c:otherwise>
							<span style="padding-left:9px;"></span> 
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${service_account_config_property.isEncrypted}">
							<input type="password"  id=sacp_"<c:out value="${service_account_config_property.name}"/>" name="${service_account_config_property.name}"  class="text ${service_account_config_property.validations.classValidations}" ${service_account_config_property.validations.validations} />
						</c:when>
						<c:otherwise>
							<input type="text"  id=sacp_"<c:out value="${service_account_config_property.name}"/>" name="${service_account_config_property.name}" 
							title="<spring:message  code="${service.serviceName}.${service_account_config_property.name}.tooltip"/>"
							class="text ${service_account_config_property.validations.classValidations}" ${service_account_config_property.validations.validations}/>
						</c:otherwise>
					</c:choose>							
				</c:otherwise>
			</c:choose>
		</span>
	</li>
</c:forEach>
</c:when>
<c:otherwise>
<div id="accountConfigEditorJsp"></div>
</c:otherwise>
</c:choose>
<div class="submitpanel">
	<a id="submitbutton" uuid="${service.uuid}" action="save" class="button active" href="javascript:void(0);"><spring:message  code="label.save"/></a>
	<a id="cancelButton" class="button active" href="javascript:void(0);"><spring:message  code="label.cancel"/></a>
</div>
</div>
</div>
</form>
  <div id="spinning_wheel" style="display: none;">
    <div class="widget_blackoverlay widget_rightpanel"
      style="position: fixed; height: 100%"></div>
    <div class="widget_loadingbox fullpage" style="position: fixed;">
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
<div id="resultForServiceConfigParams" style="margin:0; display:none;">
<div class="service_detail_subsection" class="service_detail_subsection">
<div class="contentarea">
    <div class="subscribe_dialog_message_box" id="ajax_result_display" style="display:none;">
        <p id="ajax_result_text"></p>
    </div>
<div id="printResultsForConfigParams"></div>

<div class="submitpanel">
	<a id="resultButton" class="button active" href="javascript:void(0);"><spring:message code="connector.account.settings.gotoservices" /></a>
</div>

</div>
</div>
</div> 
