<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles"%>

<div id="top_message_panel" class="common_messagebox widget" style="display: none;">
  <span id="status_icon"></span>
  <p id="msg"></p>
</div>

<div class="widgetcatalog_contentarea leftside">
  <div id="headerSectionProvisionPage" style="display: none">
    <h3 style="width: 400px">
      <c:choose>
        <c:when test="${resourceType == serviceBundleResourceType}">
          <spring:message code="label.subscribe.subscribe" />
        </c:when>
        <c:otherwise>
          <spring:message code="label.subscribe.configure.subscribe" />
        </c:otherwise>
      </c:choose>
    </h3>
  </div>
  <div id="filterSectionProvisionPage"></div>
  <!-- Custom Component Selector -->
  <c:choose>
    <c:when test="${not empty customComponentSelector}">
      <div id="customComponentSelectorContent" class="row">
        <tiles:insertDefinition name="${customComponentSelector}" />
      </div>
    </c:when>
    <c:otherwise>
      <div id="componentsSectionProvisionPage">
      <div id="defaultRCsContent" style="display:none"></div>
      </div>
    </c:otherwise>
  </c:choose>
  <!-- /Custom Component Selector -->
</div>
<div id="configure_subscribe" style="display: none;">
  <tiles:insertDefinition name="subscription.custom.fields" />
  <c:choose>
    <c:when test="${not empty customEditorTag}">
      <div id="customEditorContent">
        <tiles:insertDefinition name="${customEditorTag}" />
      </div>
    </c:when>
    <c:otherwise>
      <c:choose>
        <c:when test="${resourceType == serviceBundleResourceType}">
          <div class="widgetcatalog_contentarea sectionbox compute">
            <div class="catalogconfigure_sectionox_header">
              <h4>
                <spring:message code="label.subscribe.servicebundle.info" />
              </h4>
            </div>
            <div class="catalogconfigure_sectionox_contentarea">
              <div id="bundle_entitlements_ribbon" style="margin: 7px 0px 0px 0px;"></div>
              <div class="catalogconfigure_sectionox_contentarea compute">
                <p id="servicebundle-info-description"></p>
              </div>
            </div>
          </div>
        </c:when>
        <c:otherwise>

          <c:if test="${not empty productProperties}">
            <c:forEach items="${productProperties}" var="productProperty" varStatus="productPropertyIndex">
              <div class="widgetcatalog_contentarea sectionbox">
                <div class="catalogconfigure_sectionox_header">
                  <h4>
                    <spring:message
                      code="${service.serviceName}.ResourceType.${resourceType}.${productProperty.name}.name" />
                  </h4>
                </div>
                <div class="catalogconfigure_sectionox_contentarea">
                  <div class="widgetconfigure_default_propertybox">
                    <div class="widgetconfigure_default_propertyicon"></div>
                  </div>
                  <div class="catalogconfigure_sectionox_contentarea compute">
                    <p>
                      <spring:message
                        code="${service.serviceName}.ResourceType.${resourceType}.${productProperty.name}.description" />
                    </p>
                    <br>
                    <c:set var="propValue" value="" />
                    <c:if test="${not empty subscription}">
                      <c:set var="propValue" value="${configurationData[productProperty.name]}" />
                    </c:if>
                    <c:choose>
                      <c:when test="${productProperty.validations.required }">
                        <br>
                        <span style="color: rgb(255, 0, 0); padding-left: 2px;"><sup>*</sup></span>&nbsp;<input
                          type="text" name="prop_${productProperty.name}"
                          class="text ${productProperty.validations.classValidations}"
                          ${productProperty.validations.validations} value="<c:out value="${propValue}" />"/>
                      </c:when>
                      <c:otherwise>
                        <br>
                        <span style="color: rgb(255, 0, 0); padding-left: 6px;"></span>
                        <input type="text" name="prop_${productProperty.name}"
                          class="text ${productProperty.validations.classValidations}"
                          ${productProperty.validations.validations} value="<c:out value="${propValue}" />"/>
                      </c:otherwise>
                    </c:choose>
                  </div>
                </div>
              </div>
            </c:forEach>
          </c:if>
        </c:otherwise>
      </c:choose>
    </c:otherwise>
  </c:choose>
</div>

<!-- Template to clone the component selectors for bundle from Start -->
<div class="widgetcatalog_contentarea sectionbox" id="componentSelectionContainer" style="display: none;">
  <div class="catalogconfigure_sectionox_header">
    <h4 id="componentSelectionHeader"></h4>
  </div>
  <div class="catalogconfigure_sectionox_contentarea" style="max-height: 150px; overflow-y: auto;overflow-x: hidden; width: 98%;">
    <p id="componentSelectionDescription"></p>
    <div class="catalogconfigure_sectionox_contentarea formbox">
      <form id="component_values_container">
        <div id="component_value_template" style="display: none;">
          <li><input name="resourceComponentName" type="radio" class="radio" /> &nbsp;&nbsp;<span id="name"
            class="label" style="width:580px"></span><span id="attributes_description" class="ellipsis catalog_step2_filter_description"></span></li>
        </div>
      </form>
    </div>
  </div>
</div>
<!-- Template to clone the component selectors for bundle from End -->
