<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. --> 
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<c:set var="optional_field_available" value="false" />
<c:forEach var="service_config_property" items="${service_account_config_properties}" varStatus="status">
<c:if test="${(!service_config_property.validations.required)}">
<c:set var="optional_field_available" value="true" />
</c:if>
<c:choose>
  <c:when test="${accountConfigurationData ne null and accountConfigurationData[service_config_property.name] ne null}">
    <c:set var="service_config_value" value="${accountConfigurationData[service_config_property.name]}" />
  </c:when>
  <c:otherwise>
    <c:set var="service_config_value" value="${service_config_property.defaultVal}" />
  </c:otherwise>
</c:choose>

<c:if test="${(service_config_property.validations.required)
            || (!service_config_property.validations.required)}">
  <c:choose>
    <c:when test="${status.index % 2 == 0}">
      <c:set var="rowClass" value="odd" />
    </c:when>
    <c:otherwise>
      <c:set var="rowClass" value="even" />
    </c:otherwise>
  </c:choose>
  <li>
    <span class="label"><spring:message  code="${service.serviceName}.${service_config_property.name}.name"/></span> 
      <c:choose>
        <c:when test="${service_config_property.type=='Boolean'}">
          <div>
            <ul>
              <li style="padding:0;margin:20px 0 0 3px;width:300px;">
                <span><input type="radio" style="width:25px;" id="configbooleantrue<c:out value="${service_config_property.id}"/>" name="${service_config_property.name}"   <c:if test="${service_config_value eq null or service_config_value eq 'true'}"><c:out value="checked=\"checked\""></c:out></c:if>
 value="true"/><spring:message  code="label.true"/></span>
                <span><input type="radio" style="width:25px;" id="configbooleanfalse<c:out value="${service_config_property.id}"/>" name="${service_config_property.name}"   <c:if test="${service_config_value ne null and service_config_value eq 'false'}"><c:out value="checked=\"checked\""></c:out></c:if>
                value="false"/><spring:message  code="label.false"/></span>
              </li>
            </ul>
          </div>
        </c:when>
        <c:otherwise>
          <c:choose>
            <c:when test="${service_config_property.validations.required}">
              <div class="mandatory_wrapper">
            </c:when>
            <c:otherwise>
              <div style="float:left;">
            </c:otherwise>
          </c:choose>
          <c:choose>
            <c:when test="${service_config_property.isEncrypted}">
              <ul>
                <li style="padding:0;margin:10px 0 0 10px;">
                  <input type="password"  id="configproperty<c:out value="${service_config_property.id}"/>" name="${service_config_property.name}" 
                  data-toggle="tooltip" data-placement="right" data-original-title="<spring:message  code="${service.serviceName}.${service_config_property.name}.tooltip"/>" style="margin:0;"
                  data-input-error="<spring:message  code='${service.serviceName}.${service_config_property.name}.errortext'/>" class="text js_input_help ${service_config_property.validations.classValidations}" ${service_config_property.validations.validations} onkeyup="showHideUnmaskedLink(this)"
                  value="${service_config_property.defaultVal}"/>
                </li>
                <!--[if !(IE 8)]><!-->
                <li style="padding:0;margin:0 0 0 10px;">
                  <a <c:if test="${not empty service_config_property.defaultVal}">style="cursor: pointer; visibility: 'visible'; opacity: 1.0;" </c:if>
                    <c:if test="${empty service_config_property.defaultVal}">disabled=true style="cursor: pointer; visibility: 'visible'; opacity: 0.5;" </c:if>
                    id="configproperty<c:out value="${service_config_property.id}"/>_show_unmasked" onclick="showHideUnmaskedField(this)"><spring:message  code="label.show"/></a>
                </li>
                <!--<![endif]-->
              </ul>
            </c:when>
            <c:otherwise>
              <input type="text"  id="configproperty<c:out value="${service_config_property.id}"/>" name="${service_config_property.name}" 
              data-toggle="tooltip" data-placement="right" data-original-title="<spring:message  code="${service.serviceName}.${service_config_property.name}.tooltip"/>"
              data-input-error="<spring:message  code='${service.serviceName}.${service_config_property.name}.errortext'/>" class="text js_input_help ${service_config_property.validations.classValidations}" ${service_config_property.validations.validations} 
              value="${service_config_value}"/>
            </c:otherwise>
          </c:choose>
          </div>
          <div class="main_addnew_formbox_errormsg_popup" id="configproperty<c:out value='${service_config_property.id}'/>Error" ></div>
        </c:otherwise>
      </c:choose>
  </li>
  </c:if>
</c:forEach>
<input type="hidden" id="isOptionalFieldAvailable" name="isOptionalFieldAvailable" value="${optional_field_available}" />

<script type="text/javascript">
$(function () {
    $(".js_input_help").tooltip({
      trigger: 'focus'
    });
  });
</script>
