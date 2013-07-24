<%-- Copyright (C) 2013 Citrix Systems, Inc. All rights reserved --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles"%>

<link rel="stylesheet" href="<%=request.getContextPath() %>/css/jslider.css" type="text/css">
<link rel="stylesheet" href="<%=request.getContextPath() %>/css/jslider.plastic.css" type="text/css">
<link rel="stylesheet" href="<%=request.getContextPath() %>/css/customproperties.css" type="text/css">
<link rel="stylesheet" href="<%=request.getContextPath() %>/css/bootstrap.min.css" type="text/css">
<script type="text/javascript" src="<%=request.getContextPath() %>/js/jquery.easing.1.3.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/bootstrap.min.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/populatebundles.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/createSubscription.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/jquery.slider.min.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/resources/app.js"></script>

<jsp:include page="/WEB-INF/jsp/tiles/shared/js_messages.jsp"></jsp:include>

<script type="text/javascript">
    var l10dict = new Array();
    
    var uniqueResourceComponents = [];
    <c:if test="${not empty uniqueResourceComponentNames}">
      <c:forEach items="${uniqueResourceComponentNames}" var="uniqueResourceComponentName">
        l10dict['${uniqueResourceComponentName}' + '-name']='<spring:message javaScriptEscape="true" code="${service.serviceName}.ResourceType.${resourceType}.${uniqueResourceComponentName}.name"/>';
        l10dict['${uniqueResourceComponentName}' + '-desc']='<spring:message javaScriptEscape="true" code="${service.serviceName}.ResourceType.${resourceType}.${uniqueResourceComponentName}.desc"/>';
        uniqueResourceComponents.push('${uniqueResourceComponentName}');
      </c:forEach>
    </c:if>
    
    var serviceFilterNames = [];
    var serviceFilters = [];
    var serviceFilter;
    <c:if test="${not empty filter_list}">
      <c:forEach items="${filter_list}" var="filter" varStatus="status">
        l10dict['${filter.key}' + '-name']='<spring:message javaScriptEscape="true" code="${service.serviceName}.Filter.${filter.key}.name"/>';
        l10dict['${filter.key}' + '-desc']='<spring:message javaScriptEscape="true" code="${service.serviceName}.Filter.${filter.key}.description"/>';
        serviceFilterNames.push('${filter.key}');
        serviceFilter = [];
        <c:forEach items="${filter_list[filter.key]}" var="filter_values" varStatus="status">
          serviceFilter.push('${filter_values}');
        </c:forEach>
        serviceFilters.push(serviceFilter);
      </c:forEach>
    </c:if>
    
    var reconfigurableMap = {};
    var groups = [];
    var group;
    <c:forEach items="${groups}" var="group">
      group = [];
      <c:forEach items="${group.serviceResourceGroupComponents}" var="serviceResourceGroupComponent">
        reconfigurableMap['${serviceResourceGroupComponent.resourceComponentName}'] = '${serviceResourceGroupComponent.reconfigurable}';
        group.push('<c:out value="${serviceResourceGroupComponent.resourceComponentName}" escapeXml="false"/>');
      </c:forEach>
      groups.push(group);
    </c:forEach>
    
    var productProperties = [];
    <c:if test="${not empty productProperties}">
      <c:forEach items="${productProperties}" var="productProperty">
        l10dict['${productProperty.name}' + '-name'] = '<spring:message  code="${service.serviceName}.ResourceType.${resourceType}.${productProperty.name}.name"/>';
        l10dict['${productProperty.name}' + '-desc'] = '<spring:message code="${service.serviceName}.ResourceType.${resourceType}.${productProperty.name}.description"/>';
        productProperties.push('${productProperty.name}');
      </c:forEach>
    </c:if>
    
    var allConfigurationProperties = [];
    $.merge(allConfigurationProperties, uniqueResourceComponents);
    $.merge(allConfigurationProperties, serviceFilterNames);
    $.merge(allConfigurationProperties, productProperties);
    
    l10dict['currentBundle']='<spring:message javaScriptEscape="true" code="subscription.current.bundle"/>';
    l10dict['subscriptionBundles']='<spring:message javaScriptEscape="true" code="subscription.current.subscriptions"/>';
    l10dict['Provision']='<spring:message javaScriptEscape="true" code="ui.label.provision"/>';
    l10dict['Incompatible']='<spring:message javaScriptEscape="true" code="ui.label.incompatible"/>';
    l10dict['subscription_bundle_match_count_singular']='<spring:message javaScriptEscape="true" code="subscription.bundle.match.count.singular" arguments="1" />';
    l10dict['subscription_bundle_match_count_plural']='<spring:message javaScriptEscape="true" code="subscription.bundle.match.count.plural" arguments="__bundleCount__" />';
</script>

<div class="sliding_statswrapper" >
  <jsp:include page="/WEB-INF/jsp/tiles/main/serviceCategoryList.jsp"/>        
</div>

<!--catalog list will starts here-->
<c:choose>
  <c:when test="${not empty cloudServiceException && cloudServiceException}">
    <c:if test="${not empty cloudServiceExceptionStr}">
      <div style="width:99%; margin-bottom:0px; padding:5px;" class="common_messagebox error">
        <c:out value="${cloudServiceExceptionStr}"/>
      </div>
    </c:if>
  </c:when>
  <c:otherwise>
    <div  class="widgetcatalog_contentarea">
      <div class="row" id="subscribe_payg_choice">
        
          <ul class="nav nav-pills pull-right" id="tab_options">
            <li class="nav-header title" style="text-transform:none;"><spring:message code="label.catalog.would.you.like.to"/>:</li>
            <li id="tab_bundles_view" class="active" >
              <a class="small" href="#"><spring:message code="label.catalog.subscribe.to.bundles"/></a>
            </li>
            <li id="tab_utility_view" >
              <a class="small" href="#"><spring:message code="label.catalog.pay.as.you.go"/></a></li>
          </ul>
        
      </div>
      <div id="componentselector" class="row">
        
              
              <div id="SECTION_1" style="display:none;">
              <div class="full_width_box">
                <div class="page_heading">
                    <h3><spring:message code="label.catalog.utilityrate.title"/></h3>
                    <span id="effective_date_box" class="sub_title"></span>
                </div>
                <div class="page_heading_description">
                    <p><spring:message code="label.catalog.pay.as.you.go.description"/></p>
                </div>
              </div>
              <div class="full_width_box">
                <div class="utility_view_launch_form pull-right">
                <span class="go_button_class">
                      <label class="pull-left">
                          <spring:message code="label.launch"/>:
                      </label>
                      
                      <span class="pull-left">
                          <select id="utility_resource_selection_1" class="js_utility_resource_selection" onChange="changeResourceType(this);">
                            <c:forEach items="${resourceTypes}" var="resourceTypeObject">
                              <option value="<c:out value="${resourceTypeObject.resourceTypeName}"/>" <c:if test="${resourceTypeObject.resourceTypeName == resourceType}">selected="selected"</c:if>>
                                <spring:message code="${service.serviceName}.ResourceType.${resourceTypeObject.resourceTypeName}.name"/>
                              </option>
                            </c:forEach>
                          </select>
                      </span>
                          <button id="go_button_1" class="btn btn-primary js_pay_as_you_go_action"><spring:message code="label.button.go"/>&nbsp;&gt;&gt;</button>
                      </span>
                </div>
              </div>
              <div id="utilityrate_table" class="full_width_box" ></div> 
              <div class="full_width_box" id="pay_as_you_go_action_container_2" style="display:none;">
                <div class="utility_view_launch_form pull-right">
                <span class="go_button_class">
                      <label class="pull-left">
                          <spring:message code="label.launch"/>:
                      </label>
                      
                      <span class="pull-left">
                          <select id="utility_resource_selection_2" class="js_utility_resource_selection" onChange="changeResourceType(this);">
                            <c:forEach items="${resourceTypes}" var="resourceTypeObject">
                              <option value="<c:out value="${resourceTypeObject.resourceTypeName}"/>" <c:if test="${resourceTypeObject.resourceTypeName == resourceType}">selected="selected"</c:if>>
                                <spring:message code="${service.serviceName}.ResourceType.${resourceTypeObject.resourceTypeName}.name"/>
                              </option>
                            </c:forEach>
                          </select>
                      </span>
                          <button id="go_button_2" class="btn btn-primary js_pay_as_you_go_action"><spring:message code="label.button.go"/>&nbsp;&gt;&gt;</button>
                      </span>
                      
                </div>
              </div>
              </div>
            
            <div id="SECTION_2" class="catalog_span_narrow pull-left">
                <div class="filterlistbox">
                  <div class="bs_selectbox title user"><h2><spring:message code="label.catalog.browse.by"/></h2></div>
                  <div>
                        <select size="5"  id="resourceTypeSelection" class="catalog_select_boxes" onchange="changeResourceType(this);">
                            <c:forEach items="${resourceTypes}" var="resourceTypeObject">
                              <option title="<spring:message code="${service.serviceName}.ResourceType.${resourceTypeObject.resourceTypeName}.name"/>" value="<c:out value="${resourceTypeObject.resourceTypeName}"/>" <c:if test="${resourceTypeObject.resourceTypeName == resourceType}">selected="selected"</c:if>>
                                <spring:message code="${service.serviceName}.ResourceType.${resourceTypeObject.resourceTypeName}.name"/>
                              </option>
                            </c:forEach>
                            <option value="<c:out value="${serviceBundleResourceType}" />" <c:if test="${resourceType == serviceBundleResourceType}">selected="selected" </c:if>><spring:message code="catalog.resource.type.selection.service.bundle"/></option>
                        </select>
                  </div>
                </div>
                <div class="service_filters_label" style="display:none;"><span class="title"><spring:message code="filter.by"/>:</span></div>
                <div id="filters_SECTION_2"></div>
                <div id="components_SECTION_2"></div>
              </div>
              <div class="catalog_span_wide pull-right" id="SECTION_3">
                <div class="row">
                  <div class="navbar pull-right" id="pricing">
                  <div class="pull-left" style="padding:10px;"><spring:message code="label.subscribe.pricing"/>:</div>
                    <div class="navbar-inner pull-right bs_catalog_pricing_nav_bar">
                      <ul id="pricing_filters" class="nav">
                        <li class="active" id="ALL"><a href="#"><spring:message code="label.all"/></a></li>
                        <li id="NONE"><a href="#"><spring:message code="launchvm.chargetype.text.onetime"/></a></li>
                        <li id="MONTHLY"><a href="#"><spring:message code="charge.type.MONTHLY"/></a></li>
                        <li id="QUARTERLY"><a href="#"><spring:message code="charge.type.QUARTERLY"/></a></li>
                        <li id="ANNUAL"><a href="#"><spring:message code="charge.type.ANNUAL"/></a></li>
                      </ul>
                    </div>
                  </div>
                </div>
                <jsp:include page="populatebundles.jsp"></jsp:include>
              </div>
              <div id="SECTION_4" style="display:none;">
                <jsp:include page="provisionResourceRHS.jsp"></jsp:include>
              </div>
          </div>
        </div>
  </c:otherwise>
</c:choose>

<!-- Component UI Template to clone from -->
<div  style="display: none" id="row-clone-new">
  <div class="filterlistbox">
    <div class="bs_selectbox title user"><h2 id="filterBoxTitle"></h2></div>
    <div>
       <select size="5" id="filterBoxSelection" class="catalog_select_boxes"></select>
    </div>
  </div>
</div>
<!-- Component UI Template to clone from -->
                  
<!-- Hidden fields for filters, components and properties Start -->

<c:if test="${not empty service.serviceFilters}">
  <c:forEach items="${service.serviceFilters}" var="service_filter" varStatus="status">
    <input type="hidden" id="conf_prop_${service_filter.discriminatorName}" name="${service_filter.discriminatorName}"/>
  </c:forEach>
</c:if>

<c:if test="${not empty uniqueResourceComponentNames}">
  <c:forEach items="${uniqueResourceComponentNames}" var="uniqueResourceComponentName">
    <input type="hidden" id="conf_prop_${uniqueResourceComponentName}" name="${uniqueResourceComponentName}"/>
  </c:forEach>
</c:if>

<c:if test="${not empty productProperties}">
  <c:forEach items="${productProperties}" var="productProperty">
    <input type="hidden" id="conf_prop_${productProperty.name}" name="${productProperty.name}"/>
  </c:forEach>
</c:if>

<!-- Hidden fields for filters, components and properties End -->

<!-- /Hidden fields for component names -->
<input type="hidden" id="selectedCurrencySign" value="${currentTenant.currency.sign}">
<input type="hidden" name="resourceType" id="resourceType" value="${resourceType}" />
<input type="hidden" name="serviceInstanceUuid" id="serviceInstanceUuid" value="${serviceInstanceUuid}" />
<input type="hidden" name="isPayAsYouGoChosen" id="isPayAsYouGoChosen" value="${isPayAsYouGoChosen}" />
<input type="hidden" name="tenantParam" id="tenantParam" value='<c:out value="${tenant.param}"/>' />
<input type="hidden" name="userParam" id="userParam" value='<c:out value="${userParam}"/>' />
<input type="hidden" name="customComponentSelector" id="customComponentSelector" value="${customComponentSelector}" />
<input type="hidden" name="customEditorTag" id="customEditorTag" value="${customEditorTag}" />
<input type="hidden" name="tenantDataJsonStr" id="tenantDataJsonStr" value='${tenantDataJsonStr}' />
<input type="hidden" name="contextString" id="contextString" value="" />
<input type="hidden" name="filterString" id="filterString" value="" />
<input type="hidden" name="uniqueResourceComponentNames" id="uniqueResourceComponentNames" value="<c:out value="${uniqueResourceComponentNames}" />" />
<input type="hidden" name="cloudServiceException" id="cloudServiceException" value="${cloudServiceException}" />
<input type="hidden" name="serviceFilterNames" id="serviceFilterNames" value="<c:out value="${serviceFilterNames}" />" />
<input type="hidden" name="pricingFilter" id="pricingFilter" value="ALL" />
<c:if test="${not empty subscription}">
  <input type="hidden" name="subscriptionId" id="subscriptionId" value="<c:out value="${subscription.id}" />" />
  <input type="hidden" name="subscriptionConfJson" id="subscriptionConfJson" value="<c:out value="${configurationData}" />" />
  <c:if test="${subscription.activeHandle != null}">
    <input type="hidden" name="isReconfigure" id="isReconfigure"  value="true"  />
  </c:if>
</c:if>
<input type="hidden" id="selectedCategory" value="<c:out value="${selectedCategory}"/>" />

<a class="submit_button" href="javascript:void(0);" id="get-pricing" style="display:none;"><spring:message code="label.subscription.pricing.getpricing" /></a>
<input type="hidden" id="viewChannelCatalog" value="${viewChannelCatalog}">

<div id="spinning_wheel2" style="display:none;">
   <div class="widget_blackoverlay widget_full_page">
   </div>
   <div class="widget_loadingbox fullpage">
     <div class="widget_loaderbox">
       <span class="bigloader"></span>
     </div>
     <div class="widget_loadertext">
       <p ><spring:message code="label.loading"/> &hellip;</p>
     </div>
   </div>
</div>