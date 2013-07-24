<%-- Copyright (C) 2013 Citrix Systems, Inc. All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<jsp:include page="/WEB-INF/jsp/tiles/shared/js_messages.jsp"></jsp:include>
<script type="text/javascript">
  var connectorPath = "<%=request.getContextPath()%>/portal/connector";
  var type ="cs";
</script>
<link rel="stylesheet" href="<%=request.getContextPath() %>/css/jquery.ibutton.css" type="text/css">
<script type="text/javascript" src="<%=request.getContextPath() %>/js/jquery.ibutton.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/csconnector.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/validator.js"></script>
<script type="text/javascript">
if( typeof i18n === 'undefined' ) {
  var i18n = {};
}
if( typeof i18n.errors === 'undefined' ) {
  i18n.errors = {};
}

i18n.errors.config = {
  valueRequired: '<spring:message javaScriptEscape="true" code="js.errors.config.value.required"/>'
};
var dictionary = {
    toggleButtonEnable:'<spring:message javaScriptEscape="true" code="label.configuration.integration.togglebutton.enable"/>',
    noSettingsFound:'<spring:message javaScriptEscape="true" code="message.no.settings.found"/>'
    
};
</script>
<c:set var="is_admin" value="false"/>
<sec:authorize  access="hasRole('ROLE_ACCOUNT_ADMIN')">
    <c:set var="is_admin" value="true"/>
</sec:authorize>
<c:set var="auth_enable_service" value="false"/>
<sec:authorize  access="hasAnyRole('ROLE_ACCOUNT_MGMT','ROLE_ACCOUNT_USER_CRUD')">
    <c:set var="auth_enable_service" value="true"/>
</sec:authorize>
<c:set var="apostropy" value="'"/>
<c:set var="escapedeapostropy" value="\\'"/>
<c:choose>
    <c:when test="${is_admin || userHasCloudServiceAccount}">
    <div id="manage_services_info">
        <div class="page_heading">
            <h3><spring:message code="message.myservices.header"/></h3>
        </div>
        <div class="page_heading_description" >
            <c:choose>
                <c:when test="${is_admin}">
                    <spring:message code="message.myservices.admin.user.header.description"/>
                </c:when>
                <c:otherwise>
                    <spring:message code="message.myservices.normal.user.header.description"/>
                </c:otherwise>
            </c:choose>
        </div>
      </div>
    <div class="widgetcatalog_contentarea" style="display: block;">
        <div id="myServicesDiv" class="service_listpanel" style="display: block;">
        <jsp:include page="/WEB-INF/jsp/tiles/main/cs_instance_left_menu.jsp"></jsp:include>
        
     
        <input type="hidden" id="selectedcategory" value="All"/>
        
        <c:forEach items="${serviceInstanceMap}" var="serviceInstanceEntry" varStatus="status">
        <c:if test="${(serviceInstanceEntry.value == true) || auth_enable_service}">
          <div class="servicelist mainbox" category="${serviceInstanceEntry.key.service.category}" serviceid="${serviceInstanceEntry.key.service.uuid}">
            <div class="servicelist sections col6" style="width:470px">
            <c:choose>
                <c:when test="${not empty serviceInstanceEntry.key.imagePath}">
                  <span class="logobox">
                    <img src="/portal/portal/logo/serviceInstance/${serviceInstanceEntry.key.uuid}"/>
                  </span>
                </c:when>
                <c:otherwise>
                  <span class="logobox">
                    <img src="/portal/portal/logo/connector/${serviceInstanceEntry.key.service.uuid}/logo"/>
                  </span>
                </c:otherwise>
              </c:choose>              

              <div class="servicelist sections col7">
                <ul>
                  <li>
                    <h3>
                      <c:out value="${serviceInstanceEntry.key.name}"/>
                    </h3>
                  </li>
                  <li>
                      <c:choose>
                          <c:when test="${fn:length(serviceInstanceEntry.key.description) > 120}">
                              <div id="stripped_content_${serviceInstanceEntry.key.uuid}">
                                  <c:out value="${fn:substring(serviceInstanceEntry.key.description, 0, 116)}"/> &hellip;
                              </div>
                              <div class="hidden_content">
                                  <div id="learn_more_content_${serviceInstanceEntry.key.uuid}" style="display:none;">
                                      <c:out value="${serviceInstanceEntry.key.description}"/>
                                  </div>
                                  <a class="more_info more_down learn_more_link" id="learn_more_link_${serviceInstanceEntry.key.uuid}" href="javascript:void(0);">Learn more</a>
                              </div>
                          </c:when>
                          <c:otherwise>
                              <c:out value="${serviceInstanceEntry.key.description}"/>
                          </c:otherwise>
                      </c:choose>
                  </li>  
                                           
                </ul>
              </div>
            </div>
            <div class="servicelist sections col3 no_border" style="width: 195px;">
             
              <c:set var="instanceName" value="${serviceInstanceEntry.key.name}"/>
              <c:set var="escapedInstanceName" value="${fn:replace(instanceName,apostropy,escapedeapostropy)}" /> 
             <c:choose>
                  <c:when test="${(serviceInstanceEntry.value == true)}">
                      <div class="link_wrapper" >
                        <c:if test="${userHasCloudServiceAccount}">
                            <a id="${serviceInstanceEntry.key.uuid}" class="vertical_stacked_links button_manage_service"  href="javascript:void(0);">
                             <spring:message  code="service.connectorlist.manage" />
                            </a>
                        </c:if>
                        <c:if test="${userHasCloudServiceAccount && currentUser.id == effectiveUser.id}">
                           <a class="vertical_stacked_links" href="javascript:resolveViewForSettingFromServiceInstance2('${serviceInstanceEntry.key.uuid}');"> <spring:message code="message.myprofile.mysettings"/> </a>
                        </c:if> 
                        <c:if test="${serviceInstanceViewMap[serviceInstanceEntry.key] == true && is_admin && effectiveTenant.id == currentTenant.id}">
                            <a id="${serviceInstanceEntry.key.uuid}" class="vertical_stacked_links" href="javascript:resolveViewForAccountSettingFromServiceInstance('${serviceInstanceEntry.key.uuid}','${currentTenant.param}','${escapedInstanceName}');">
                                <spring:message  code="service.connectorlist.companysettings" />
                            </a>
                        </c:if>
                      
                      </div>
                     
                  </c:when>
                   <c:when test="${(auth_enable_service && effectiveTenant.state.name == 'ACTIVE')}">
                   <div class="button_wrapper" >
                      <a id="${serviceInstanceEntry.key.uuid}" class="add_button active" href="javascript:resolveViewForSettingFromServiceInstance('${serviceInstanceEntry.key.uuid}','${currentTenant.param}','${escapedInstanceName}');"> 
	                    <spring:message code="service.connectorlist.signup"/>
	                  </a>
                    </div>
		           </c:when>               
		            <c:when test="${auth_enable_service}">
                    <div class="button_wrapper" >
	                  <a id="${serviceInstanceEntry.key.uuid}" class="add_button nonactive" href="javascript:void(0);"> 
	                    <spring:message code="service.connectorlist.signup"/>
	                  </a>
                     </div>
		            </c:when>
                        
                </c:choose>
          
            </div>
          </div>
          </c:if>
        </c:forEach>
      </div>
      
       <div id="serviceAccountConfigDiv" style="margin:0; display:none;">     
          <div>
          <h2 id="selectedInstanceH1"></h2>
            <iframe id="serviceAccountConfigViewFrame" width="940px" height="700px" frameborder="0"></iframe>
          </div>
        </div>
      
    
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
    
      <div class="service_detailpanel" style="display: none;"></div>
      
      <div id="dialog_service_details" title='<spring:message code="label.service.details"/>' style="display: none"></div>
    
      <div id="dialog_enable_service" title='<spring:message code="label.enable.service"/>' style="display: none; overflow:hidden;"></div>
      
        <div id="userSubscribedServiceDetails" style="margin:0; display:none;">
            <span id="backToSubscribedServiceListing" class="title_listall_arrow biggerback" style="float:right;"><spring:message javaScriptEscape="true" code="label.backtolisting"/></span>
            <div>
                <br><br>
                <iframe id="userOrAccountSettingsViewFrame" width="940px" height="700px" frameborder="0"></iframe>
            </div>
        </div>
    
    </div>
</c:when>
    <c:otherwise>
        <div class="subscribe_dialog_message_box error">
            <p><spring:message code='no.cloud.service.account'/></p>
        </div>
    </c:otherwise>
</c:choose>