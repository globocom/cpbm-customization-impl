<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>                
<script type="text/javascript" src="<%=request.getContextPath() %>/js/subscription.js"></script>
<jsp:include page="js_messages.jsp"></jsp:include>

<script language="javascript">

  var dictionary = {  
      terminatesubscription: '<spring:message javaScriptEscape="true" code="label.subscription.details.terminate"/>',
      lightboxterminatesubscription:'<spring:message javaScriptEscape="true" code="message.confirm.terminate.subscription"/>',
      cancelsubscription: '<spring:message javaScriptEscape="true" code="label.subscription.details.cancel"/>',
      lightboxcancelsubscription:'<spring:message javaScriptEscape="true" code="message.confirm.cancel.subscription"/>',
      lightboxbuttoncancel: '<spring:message javaScriptEscape="true" code="label.cancel"/>',  
      lightboxbuttonconfirm: '<spring:message javaScriptEscape="true" code="label.confirm"/>',
      terminatingSubscription:'<spring:message javaScriptEscape="true" code="message.terminating.subscription"/>',
      cancellingSubscription:'<spring:message javaScriptEscape="true" code="message.cancelling.subscription"/>',
      showingdetails: '<spring:message javaScriptEscape="true" code="message.showing.details"/>',
      notApplicable:'<spring:message javaScriptEscape="true" code="ui.label.na"/>',
      subscriptionEndDate:'<spring:message code="label.usage.billing.subscription.charge.service.end"/>',
      cloudServiceException:'<spring:message code="exception.cloud.service"/>'
  };

  
</script>
<div class="widget_box">
  <div class="widget_leftpanel">
    <div class="widget_titlebar">
      <h2 id="list_titlebar"><span id="list_all" class="title_listall_arrow"><spring:message code="label.list.all"/></span></h2>
    </div>
     
    <div class="widget_searchpanel">
      <div id="search_panel" class="widget_searchcontentarea">
        
        <span class="label" id="filters_applied">
          <c:if test="${filtersApplied==0}"><spring:message code="label.no"/> </c:if> 
          <c:if test="${filtersApplied>0}">${filtersApplied} </c:if>
        </span>
        <span class="label js_filter_details_popover" style="text-decoration: underline;margin-left:5px;"><spring:message code="label.filter"/></span>
        <span class="label ellipsis" style="margin-left:5px;width: 100px" title='<spring:message code="label.applied"/>'><spring:message code="label.applied"/></span>
        
        <div id="js_filter_details_popover" style="display: none;">
          <div class="popover_content_container" style="margin-top:50px;">
            <div class="popover_rows">
              <div class="row_contents_title">
                <span>
                  <spring:message code="ui.label.user" />:
                </span>
              </div>
              <div class="row_contents_value">
                <span id="_filter_user"></span>
              </div>
            </div>
            <div class="popover_rows">
              <div class="row_contents_title">
                <span>
                  <spring:message code="ui.label.state" />:
                </span>
              </div>
              <div class="row_contents_value">
                <span id="_filter_state"></span>
              </div>
            </div>
            <div class="popover_rows">
              <div class="row_contents_title">
                <span>
                  <spring:message code="ui.label.instance" />:
                </span>
              </div>
              <div class="row_contents_value">
                <span id="_filter_instance"></span>
              </div>
            </div>
            <div class="popover_rows">
              <div class="row_contents_title">
                <span>
                  <spring:message code="ui.label.product.bundle" />:</span>
                </span>
              </div>
              <div class="row_contents_value">
                <span id="_filter_bundle"></span>
              </div>
            </div>
          </div>
        </div>
                              
        <a class="advancesearch_button" id="advancesearchButton" style="float:right"></a>
        <div class="widget_actionpopover advancesearch_dropdown" id="advanceSearchDropdownDiv" style="display: none;">
          <jsp:include page="search.jsp"></jsp:include>
        </div>
        
      </div>
    </div>
    <div class="widget_navigation">
      <ul class="widget_navigationlist" id="grid_row_container">
                          
         <c:choose>
           <c:when test="${empty subscriptions || subscriptions == null}">
            <!--look when there is no list starts here-->
            <li class="widget_navigationlist nonlist" id="non_list">
                <span class="navicon volumedata"></span>
                  <div class="widget_navtitlebox">
                    <span class="newlist"><spring:message code="message.no.subscriptions.available"/></span>
                  </div>
                  
              </li>
              <!--look when there is no list ends here-->
      
           </c:when>
          <c:otherwise> 
          <c:forEach items="${subscriptions}" var="subscription" varStatus="status">
            <c:choose>
              <c:when test="${status.index == 0}">
                  <c:set var="firstSubscription" value="${subscription}"/>
                  <c:set var="selected" value="selected"/>
              </c:when>
              <c:otherwise>
                  <c:set var="selected" value=""/>
              </c:otherwise>
            </c:choose> 
            
          <li class='<c:out value="widget_navigationlist ${selected} subscriptions"/>' id="sub<c:out value="${subscription.uuid}" />" onclick="timerFunction($(this))" onmouseover="showInfoBubble(this)" onmouseout="hideInfoBubble(this)">
                            <span class="navicon subscription" id="nav_icon"></span>
                            <div class="widget_navtitlebox">
                              <span class="title">
                               <c:choose>
                                <c:when test="${not empty subscription.handle.resourceName}">
                                  <c:out value="${subscription.handle.resourceName}"/>
                                </c:when>
                                <c:otherwise>
                                  <c:out value="${subscription.uuid}"/>
                                </c:otherwise>
                              </c:choose>
                              </span>
                              
                              <span class="subtitle">
                                <fmt:timeZone value="${currentUser.timeZone}">
                                <spring:message code="dateonly.format" var="dateonly_format"/>  
                                  <fmt:formatDate value="${subscription.activationDate}" pattern="${dateonly_format}"/>                      
                                </fmt:timeZone>
                                <c:if test="${subscription.state == 'EXPIRED'}">
                                   -
                                   <fmt:timeZone value="${currentUser.timeZone}">
                                      <spring:message code="dateonly.format" var="dateonly_format"/>  
                                    <fmt:formatDate value="${subscription.terminationDate}" pattern="${dateonly_format}"/>                      
                                    </fmt:timeZone>
                                </c:if>
                              </span>
                            </div>
                            <c:choose>
                              <c:when test="${subscription.state == 'ACTIVE'}">
                                <c:choose>
                                  <c:when test="${subscription.resourceType != null && (subscription.handle == null || subscription.handle.state == 'ERROR' || subscription.handle.state == 'TERMINATED')}">
                                    <c:set var="status_icon" value="interrupted"/>
                                  </c:when>
                                  <c:otherwise>
                                    <c:set var="status_icon" value="running"/>
                                 </c:otherwise>
                               </c:choose>
                             </c:when>
                             <c:when test="${subscription.state == 'EXPIRED'}">
                                  <c:set var="status_icon" value="stopped"/>
                              </c:when>
                              <c:otherwise>
                                  <c:set var="status_icon" value="nostate"/>
                              </c:otherwise>
                            </c:choose> 
                              <div class="<c:out value="widget_statusicon ${status_icon}" />"></div>
                              <!--Info popover starts here-->
                              <div class="widget_info_popover" id="info_bubble" style="display:none">
                              <div class="popover_wrapper" >
                              <div class="popover_shadow"></div>
                              <div class="popover_contents">
                              <div class="raw_contents">
                                      <div class="raw_content_row">
                                        <div class="raw_contents_title">
                                          <span><spring:message code="label.subscription.details.bundle"/>:</span>
                                        </div>
                                        <div class="raw_contents_value">
                                          <c:if test="${subscription.productBundle == null}">
                                            <spring:message code="launchvm.utility.bundle.name" />
                                          </c:if>
                                          <c:if test="${subscription.productBundle != null}">
                                            <span><c:out value="${subscription.productBundle.name}"/></span>
                                          </c:if>
                                        </div>
                                      </div>
                                      <div class="raw_content_row">
                                        <div class="raw_contents_title">
                                          <span><spring:message code="label.state"/>:</span>
                                        </div>
                                        <div class="raw_contents_value" id = "subscriptionStateDivId">
                                          <span><spring:message code="${subscription.state.code}" /></span>
                                        </div>
                                      </div>
                                      <div class="raw_content_row">
                                        <div class="raw_contents_title">
                                          <span><spring:message code="label.usage.billing.subscription.charge.service.start"/>:</span>
                                        </div>
                                        <div class="raw_contents_value">
                                          <span>
                                            <fmt:timeZone value="${currentUser.timeZone}">
                                              <spring:message code="dateonly.format" var="dateonly_format"/>  
                                              <fmt:formatDate value="${subscription.activationDate}" pattern="${dateonly_format}"/>                      
                                            </fmt:timeZone>
                                          </span>
                                        </div>
                                      </div>
                                      <c:if test="${subscription.state == 'EXPIRED'}">
                                          <div class="raw_content_row">
                                          <div class="raw_contents_title">
                                            <span><spring:message code="label.usage.billing.subscription.charge.service.end"/>:</span>
                                          </div>
                                          <div class="raw_contents_value">
                                            <span>
                                              <fmt:timeZone value="${currentUser.timeZone}">
                                                <spring:message code="dateonly.format" var="dateonly_format"/>  
                                                <fmt:formatDate value="${subscription.terminationDate}" pattern="${dateonly_format}"/>                      
                                              </fmt:timeZone>
                                            </span>
                                          </div>
                                        </div>
                                      </c:if>
                                      
                                </div>
                                  </div>
                                </div>
                                </div>
                                <!--Info popover ends here-->
                            
                           </li>
        </c:forEach>
        
        </c:otherwise>
        </c:choose>
                          

        </ul>
      </div>
      <div class="widget_panelnext">
        <div class="widget_navnextbox">
            <c:choose>
              <c:when test="${current_page <= 1}">
                  <a class="widget_navnext_buttons prev nonactive" href="javascript:void(0);" id="click_previous"><spring:message code="label.previous.short"/></a>
              </c:when>
              <c:otherwise>
                  <a class="widget_navnext_buttons prev" href="javascript:void(0);" id="click_previous" onclick="previousClick()"><spring:message code="label.previous.short"/></a>
              </c:otherwise>
            </c:choose> 
            
            <c:choose>
              <c:when test="${enable_next == true}">
              <a class="widget_navnext_buttons next" href="javascript:void(0);" id="click_next" onclick="nextClick()"><spring:message code="label.next"/></a>
          </c:when>
              <c:otherwise>
              <a class="widget_navnext_buttons next nonactive" href="javascript:void(0);" id="click_next"><spring:message code="label.next"/></a>
                </c:otherwise>
            </c:choose> 
          </div>
      </div>
  </div>
  <div class="widget_rightpanel">
  <div id="viewDetailsDiv">
  <c:if test="${(empty subscriptions || subscriptions == null)&& current_page == 1}">
    <jsp:include page="/WEB-INF/jsp/tiles/billing/viewsubscription.jsp"></jsp:include>
  </c:if>
  </div>
  <div id="spinning_wheel2" style="display: none;">
  <div class="widget_blackoverlay widget_full_page" style="position:absolute;"></div>
  <div class="widget_loadingbox fullpage" style="position:absolute;">
    <div class="widget_loaderbox">
      <span class="bigloader"></span>
    </div>
    <div class="widget_loadertext">
      <p>
        <spring:message code="label.loading" />
        &hellip;
      </p>
    </div>
  </div>
</div>
  </div>
</div>

<input type="hidden" id="filtersApplied"  value="<c:out value="${filtersApplied}"/>"/>
<input type="hidden" id="useruuid"  value="<c:out value="${useruuid}"/>"/>
<input type="hidden" id="instanceuuid"  value="<c:out value="${instanceuuid}"/>"/>
<input type="hidden" id="productBundleID"  value="<c:out value="${productBundleID}"/>"/>
<input type="hidden" id="stateSelected"  value="<c:out value="${stateSelected}"/>"/>
<input type="hidden" id="current_page"  value="<c:out value="${current_page}"/>"/>
<input type="hidden" id="selected_subs_for_details"  value="<c:out value="${idForDetails}"/>"/>
<input type="hidden" id="tenantParam"  value="<c:out value="${tenant.param}"/>"/>
<input type="hidden" id="states"  value="<c:out value="${states}"/>"/>
