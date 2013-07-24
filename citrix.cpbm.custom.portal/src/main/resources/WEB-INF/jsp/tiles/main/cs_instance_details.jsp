<%-- Copyright (C) 2013 Citrix Systems, Inc. All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<script language="javascript">
	var dictionary = {viewMasked: '<spring:message javaScriptEscape="true" code="label.show"/>',
		    hideMasked: '<spring:message javaScriptEscape="true" code="label.hide"/>'};
</script>

<script type="text/javascript" src="<%=request.getContextPath() %>/js/csInstanceAdd.js"></script>
<style type="text/css">
.tooltip {
    background-color:#CCCCCC;
    border:1px solid #fff;
    padding:10px 15px;
    width:280px;
    display:none;
    color:#fff;
    text-align:left;
    font-size:12px;
}
</style>
eweer<c:out value="${viewServiceDetails}" />
<c:choose>
	<c:when test="${viewServiceDetails}">
		<div class="dialog_formcontent ">
			<div class="dialog_detailsbox" style="padding:0px;">
				<div class="mainbox">
					<div class="logobox"><img src="/portal/portal/logo/connector/${service.uuid}/logo"/></div>
					<div class="descriptionbox">
						<h2><spring:message  code="${service.serviceName}.service.name"/></h2>
						<span class="detail1"><spring:message  code="label.category"/>: <spring:message code="${service.serviceName}.service.category"/></span>
						<p><spring:message  code="${service.serviceName}.service.description"/></p>
					</div>
				</div>
		
				<!-- <div class="morecontentarea">
					<h3>Features</h3>
		            <h4>Saves Admin Time</h4>
		            <p>Manage you applications and infrastructure more efficiently, from DevOps shops to multi-department organizations.</p>
		            
		            <h4>Achieve High Availability</h4>
		            <p>RightScale provides the tools to easily operate highly available autoscaling application tiers, master/slave databases, and multi-region and multi-cloud environments.</p>
		        </div> -->
		    </div>
		</div>
	</c:when>
	<c:otherwise>
		<div class="service_descriptionbox">
			<div class="service_descriptionbox logobox" style="padding:5px"><img src="/portal/portal/logo/connector/${service.uuid}/banner"/></div>
			<div class="service_descriptionbox textbox" style="width:400px;float:left">
				<h3><spring:message  code="${service.serviceName}.service.name"/></h3>
				<br><br><h5><spring:message  code="label.category"/>: <spring:message  code="${service.serviceName}.service.category"/></h5>
				<p><span class="label"><spring:message  code="${service.serviceName}.service.description"/></p>
			</div>
		</div>
		
		<div class="service_detail_subsection" style="display:none;border:none;background:none;">
			<div class="contentarea" style="padding:0px">
				<li class="row" style="border:none">
					<span class="description" id="resultstring" style="width: 100%; text-align: center; color: red;"/>
				</li>
			</div>
		</div>
	
		<form id="serviceInstanceForm">
			<c:choose>
				<c:when test="${service_config_properties!=null}">
					<div class="service_detail_subsection">
						<div class="header">
							<h4><spring:message  code="label.general"/></h4>
						</div>
						<div class="contentarea">
							<li class="row" style="display:none;"><span class="description" id="resultstring"
								style="width: 900px; text-align: center; color: red;"></span></li>
							<li class="row odd"><span class="label"><spring:message  code="instance.name"/></span><span
								class="description"><span style="color: rgb(255, 0, 0); padding-left:4px;float: left;">*</span><input id="configproperty_instance_name" name="instancename" title="<spring:message  code="instance.name"/>" type="text" class="text required"  maxlen="128"/></span></li>
							<li class="row even"><span class="label"><spring:message  code="instance.description"/></span></span> <span
								class="description"><span style="padding-left:9px;"/><input id="configproperty_instance_description" name="instancedescription" title="<spring:message  code="instance.description"/>" type="text" class="text" maxlen="128"/></span></li>
						</div>
					</div>
				
					<div class="service_detail_subsection">
						<div class="header">
							<h4><spring:message  code="label.configuration"/></h4>
						</div>
						<div class="contentarea">
							<c:forEach var="service_config_property" items="${service_config_properties}" varStatus="status">
								<c:choose>
									<c:when test="${status.index % 2 == 0}">
										<c:set var="rowClass" value="odd" />
									</c:when>
									<c:otherwise>
										<c:set var="rowClass" value="even" />
									</c:otherwise>
								</c:choose>
								<li class="row ${rowClass}">
									<span class="label"><spring:message  code="${service.serviceName}.${service_config_property.name}.name"/></span> 
									<span class="description">
										<c:choose>
											<c:when test="${service_config_property.type=='Boolean'}">
												<span style="padding-left:6px;"/><span><input type="radio" id="configbooleantrue<c:out value="${service_config_property.id}"/>" name="${service_config_property.name}" checked /><spring:message  code="label.true"/></span>
												<span><input type="radio" id="configbooleanfalse<c:out value="${service_config_property.id}"/>" name="${service_config_property.name}"/><spring:message  code="label.false"/></span>
											</c:when>
											<c:otherwise>
												<c:choose>
													<c:when test="${service_config_property.validations.required}">
														<span style="color: rgb(255, 0, 0); padding-left:2px; float: left">*   </span>
													</c:when>
													<c:otherwise>
														<span style="padding-left:9px;"/> 
													</c:otherwise>
												</c:choose>
												<c:choose>
													<c:when test="${service_config_property.isEncrypted}">
														<input type="password"  id="configproperty<c:out value="${service_config_property.id}"/>" name="${service_config_property.name}"  
                            title="<spring:message  code="${service.serviceName}.${service_config_property.name}.tooltip"/>"
                            class="text ${service_config_property.validations.classValidations}" ${service_config_property.validations.validations} onkeyup="showHideUnmaskedLink(this)"/>
                            <span style="padding-left:9px;"/> 
                            <a disabled=true style="cursor: pointer; opacity: 0.5;" id="configproperty<c:out value="${service_config_property.id}"/>_show_unmasked" onclick="showHideUnmaskedField(this)"><spring:message  code="label.show"/></a>
													</c:when>
													<c:otherwise>
														<input type="text"  id="configproperty<c:out value="${service_config_property.id}"/>" name="${service_config_property.name}" 
														title="<spring:message  code="${service.serviceName}.${service_config_property.name}.tooltip"/>"
														class="text ${service_config_property.validations.classValidations}" ${service_config_property.validations.validations} 
														value="${service_config_property.defaultVal}"/>
													</c:otherwise>
												</c:choose>							
											</c:otherwise>
										</c:choose>
									</span>
								</li>
							</c:forEach>
						</div>
						<div class="submitpanel">
							<a id="submitbutton" uuid="${service.uuid}" action="save" class="button active" href="javascript:void(0);"><spring:message  code="label.add"/></a>
						</div>
					</div>
				</c:when>
				<c:otherwise>
					<div class="service_detail_subsection">
						<div class="header">
							<h4><spring:message  code="label.general"/></h4>
						</div>
						<div class="contentarea">
							<li class="row" style="display:none;"><span class="description" id="resultstring"
								style="width: 900px; text-align: center; color: red;"></span></li>
							<li class="row odd"><span class="label"><spring:message  code="instance.name"/></span><span
								class="description"><span style="color: rgb(255, 0, 0); padding-left:4px;float:left">*</span><input id="configproperty_instance_name" name="instancename" title="<spring:message  code="instance.name"/>" value="${instance.name}" type="text" class="text required"  maxlen="128"/></span></li>
							<li class="row even"><span class="label"><spring:message  code="instance.description"/></span></span> <span
								class="description"><span style="padding-left:9px;"/><textarea id="configproperty_instance_description" name="instancedescription" title="<spring:message  code="instance.description"/>" value="" class="text_area" maxlength="4000">${instance.description}</textarea></span></li>
						</div>
					</div>
				
					<div class="service_detail_subsection">
						<div class="header">
							<h4><spring:message  code="label.configuration"/></h4>
						</div>
						<div class="contentarea">
							<c:forEach var="instance_property" items="${instance_properties}" varStatus="status">
								<c:choose>
									<c:when test="${status.index % 2 == 0}">
										<c:set var="rowClass" value="odd" />
									</c:when>
									<c:otherwise>
										<c:set var="rowClass" value="even" />
									</c:otherwise>
								</c:choose>
								<li class="row ${rowClass}">
									<span class="label"><spring:message  code="${service.serviceName}.${instance_property.serviceConfigMetadata.name}.name"/></span> 
									<span class="description">
										<c:choose>
											<c:when test="${instance_property.serviceConfigMetadata.type=='Boolean'}">
												<span style="padding-left: 6px;" /></span>
												<span>
													<input type="radio" id="configbooleantrue<c:out value="${instance_property.serviceConfigMetadata.id}"/>"
														name="${instance_property.serviceConfigMetadata.name}" checked />
													<spring:message code="label.true" />
												</span>
												<span>
													<input type="radio" id="configbooleanfalse<c:out value="${instance_property.serviceConfigMetadata.id}"/>"
														name="${instance_property.serviceConfigMetadata.name}" />
													<spring:message code="label.false" />
												</span>
											</c:when>
											<c:otherwise>
												<c:choose>
													<c:when test="${instance_property.serviceConfigMetadata.validations.required}">
														<span style="color: rgb(255, 0, 0); padding-left:2px;float:left;">*</span>
													</c:when>
													<c:otherwise>
														<span style="padding-left:7px;"/>
													</c:otherwise>
												</c:choose>
												<c:set var="textFieldType" value="text"/>
												<c:if test="${instance_property.serviceConfigMetadata.isEncrypted}">
													<c:set var="textFieldType" value="password"/>
												</c:if>
												<input  type="${textFieldType}" id="configproperty<c:out value="${instance_property.serviceConfigMetadata.id}"/>" 
														name="${instance_property.serviceConfigMetadata.name}" 
														title="<spring:message  code="${service.serviceName}.${instance_property.serviceConfigMetadata.name}.tooltip"/>"
														class="text ${instance_property.serviceConfigMetadata.validations.classValidations}" ${instance_property.serviceConfigMetadata.validations.validations} 
														value="${instance_property.value}"/>
											</c:otherwise>
										</c:choose>
									</span>
								</li>
							</c:forEach>
						</div>
						<div class="submitpanel">
							<a id="submitbutton" uuid="${instance.uuid}" action="update" class="button active" href="javascript:void(0);"><spring:message  code="label.save"/></a>
						</div>
					</div>
				</c:otherwise>
			</c:choose>
		</form>
	</c:otherwise>
</c:choose>