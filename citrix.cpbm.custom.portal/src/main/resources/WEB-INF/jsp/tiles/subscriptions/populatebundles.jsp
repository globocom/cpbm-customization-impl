<%-- Copyright (C) 2013 Citrix Systems, Inc. All rights reserved --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles"%>

<c:set var="provision" value="false" scope="request"></c:set>
<c:set var="subConfigurationData" value="${subConfigurationData}" scope="request"></c:set>
<c:set var="customEditorTag" value="${customEditorTag}" scope="request"></c:set>
            <div id="top_message_panel" class="common_messagebox widget" style="display:none;"><span id="status_icon"></span><p id="msg"></p></div>
            
            <div class="widgetcatalog_contentarea leftside" >
                <div id="headerSectionProvisionPage" style="display:none">
                  <h3 style="width:400px">
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
                  <c:if test="${not empty customComponentSelector}">
                    <div class="row">
                      <div class="catalog_filterbox" id="filter">
                        <c:if test="${resourceType ne serviceBundleResourceType}">
                          <div id="narrow-your-search" onclick="narrowYourSearchClickEventListener()" class="botarea catalog_search_filter_box">
                            <div class="contentarea">
                              <div class="selectedbox">
                                <span class="icon filter"></span><span id="narrow_your_search_link" class="selectedtext expand"><spring:message code="label.catalog.narrow.your.search"/></span>
                              </div>
                            </div>
                          </div>
                        </c:if>
                      </div>
                    </div>
                  </c:if>
                  <div id="componentsForSelectedBundle">
                    <div id="filterSectionProvisionPage"></div>
                    <div id="componentsSectionProvisionPage"></div>
                  </div>
                  <!-- Custom Component Selector -->
                  <c:if test="${not empty customComponentSelector}">
                  
                    <div id="customComponentSelectorContent" class="row"><tiles:insertDefinition name="${customComponentSelector}"></tiles:insertDefinition></div>
                  </c:if>
                  <!-- /Custom Component Selector -->
                  
                  <jsp:include page="../productBundles/commonproductbundlediv.jsp"></jsp:include>
                  </div>
                  <div id="configure_subscribe" style="display:none;">
                    <tiles:insertDefinition name="subscription.custom.fields"></tiles:insertDefinition>
                    <c:choose>
                      <c:when test="${not empty customEditorTag}">
                        <div id="customEditorContent"><tiles:insertDefinition name="${customEditorTag}"></tiles:insertDefinition></div>
                      </c:when>
                      <c:otherwise>
                      <c:choose>
                      <c:when test="${resourceType == serviceBundleResourceType}">
                      <div class="widgetcatalog_contentarea sectionbox compute">
                          <div class="catalogconfigure_sectionox_header">
                              <h4><spring:message code="label.subscribe.servicebundle.info" /></h4>
                          </div>
                          <div class="catalogconfigure_sectionox_contentarea">
                              <div id="bundle_entitlements_ribbon" style="margin:7px 0px 0px 0px;"></div>
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
                                  <h4><spring:message  code="${service.serviceName}.ResourceType.${resourceType}.${productProperty.name}.name"/></h4>
                                </div>
                                <div class="catalogconfigure_sectionox_contentarea">
                                  <div class="widgetconfigure_default_propertybox">
                                    <div class="widgetconfigure_default_propertyicon"></div>
                                  </div>
                                  <div class="catalogconfigure_sectionox_contentarea compute">
                                    <p><spring:message code="${service.serviceName}.ResourceType.${resourceType}.${productProperty.name}.description"/></p>
                                    <br>
                                    <c:set var="propValue" value="" />
                                    <c:if test="${not empty subscription}">
                                      <c:set var="propValue" value="${configurationData[productProperty.name]}" />
                                    </c:if>
                                    <c:choose>
                                      <c:when test="${productProperty.validations.required }">
                                        <br><span style="color: rgb(255, 0, 0); padding-left:2px;"><sup>*</sup></span>&nbsp;<input type="text" name="prop_${productProperty.name}" class="text ${productProperty.validations.classValidations}" ${productProperty.validations.validations} value="<c:out value="${propValue}" />" onChange="updateProperty(this)"/>
                                      </c:when>
                                      <c:otherwise>
                                        <br><span style="color: rgb(255, 0, 0); padding-left:6px;"></span><input type="text" name="prop_${productProperty.name}" class="text ${productProperty.validations.classValidations}" ${productProperty.validations.validations} value="<c:out value="${propValue}" />" onChange="updateProperty(this)"/>
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
    <c:set var="title_arg_1">
     <spring:message code="${service.serviceName}.ResourceType.${resourceType}.name"/>
    </c:set>
     <div id="launch_vm_dialog" title='<spring:message code="label.subscribing.to" arguments="${title_arg_1}"/>' style="display:none">
       
          <div class="catalogconfigure_sectionox_contentarea" >
              <div class="widgetconfigure_softwarebox" >                
              </div>
              <div class="widgetconfigure_softwarebox textarea" id="launch_vm_progress" style="width:70%;margin-left:30px;">
                  <div id="launchingVm_template" class="subscribe_dialog_message_box" style="display:none;">  
                    
                      <p id="message2" style="margin:8px 0px 0px 5px;"></p>  
                         
                  </div>
                  <p id="in_progress_message2"><spring:message code="launching.subscribe.launch.vm.in.process.part2" htmlEscape="false"/></p>
                
               
              </div>
              
          </div>
     
       
    </div>  

<!--  dialogs (begin) -->
<div id="tncDialog" title='<spring:message code="js.errors.register.tncDialog.title"/>' style="display:none">
      <c:out value="${tnc}" escapeXml="false" />
</div>
<!--  dialogs (end) -->  
 

<!-- hidden templates (begin) -->
<input type="hidden" id="channelParam" name="channelParam" value="<c:out value="${channel.param}"/>"/>
<input type="hidden" id="showServiceBundles" name="showServiceBundles" value="<c:out value="${showServiceBundles}"/>"/>
<input type="hidden" id="subConfigurationData"  value="<c:out value="${subConfigurationData}"/>"/>
<input type="hidden" name="customEditorTag" id="customEditorTag" value="${customEditorTag}"/>
<input type="hidden" id="tenantCurrencyCode" name="tenantCurrencyCode" value="<c:out value="${tenant.currency.currencyCode}" />" />
<input type="hidden" id="minFractionDigits" name="minFractionDigits" value="<c:out value="${minFractionDigits}" />" />

<!-- Template to clone the component selectors for bundle from Start -->
<div class="widgetcatalog_contentarea sectionbox" id="componentSelectionContainer" style="display:none;">
  <div class="catalogconfigure_sectionox_header">
    <h4 id="componentSelectionHeader"></h4>
  </div>
  <div class="catalogconfigure_sectionox_contentarea" style="max-height:150px; overflow:auto; width:98%;">
    <p id="componentSelectionDescription"></p>
    <div class="catalogconfigure_sectionox_contentarea formbox">
      <form id="component_values_container">
        <div id="component_value_template" style="display: none;">
          <li><input name="resourceComponentName" type="radio" class="radio"/>
            &nbsp;&nbsp;<span id="name" class="label"></span></li>
        </div>
      </form>
    </div>
  </div>
</div>
<!-- Template to clone the component selectors for bundle from End -->
