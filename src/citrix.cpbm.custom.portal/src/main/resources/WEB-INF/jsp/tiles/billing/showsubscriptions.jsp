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
      notApplicable:'<spring:message javaScriptEscape="true" code="ui.label.na"/>'
  };
  
</script>
<div class="widget_box">
  <div class="widget_leftpanel">
    <div class="widget_titlebar">
      <h2 id="list_titlebar"><span id="list_all"><spring:message code="label.list.all"/> </span></h2>
    </div>
      <sec:authorize access="hasAnyRole('ROLE_ACCOUNT_ADMIN','ROLE_ACCOUNT_BILLING_ADMIN','ROLE_FINANCE_CRUD')">
    <div class="widget_searchpanel">
      <div id="search_panel" class="widget_searchcontentarea">
       <spring:message code="page.level2.allusers" var="all_users"/>
        <span class="label fixed_width"><spring:message code="label.filter.by"/>:</span>
        <select id="userfilterdropdownforinvoices" onchange="filter_subscriptions(this);" class="select">
        <option id="ALL_USERS" value="ALL_USERS"><c:out value="${all_users}"></c:out></option>
        <c:forEach var="user" items="${tenant.users}" varStatus="usersStatus">
              <option id="${user.uuid}"  value="${user.uuid}" <c:if test="${user.uuid eq useruuid}">selected</c:if>><c:out value="${user.username}"></c:out> </option>
        </c:forEach>
        </select>
        
           
      </div>
    </div>
    </sec:authorize>
    <div class="widget_searchpanel">
      <div id="search_panel" class="widget_searchcontentarea">
          <span class="label fixed_width"><spring:message code="label.state" />:</span> <select class="filterby select"
            id="filter_dropdown" onchange="filter_subscriptions(this);" >
            <option value="all">
              <spring:message code="ui.product.category.all" />
            </option>
            <c:forEach items="${states}" var="choice" varStatus="status">
              <option value='<c:out value="${choice.name}" />' <c:if test="${choice.name eq state}">selected</c:if>>
               <spring:message code="subscription.state.${fn:toLowerCase(choice.name)}" />
              </option>
            </c:forEach>
          </select>
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
            
          <li class='<c:out value="widget_navigationlist ${selected} subscriptions"/>' id="sub<c:out value="${subscription.uuid}" />" onclick="viewSubscription(this)" onmouseover="showInfoBubble(this)" onmouseout="hideInfoBubble(this)">
                            <span class="navicon subscription" id="nav_icon"></span>
                            
                            <div class="widget_navtitlebox">
                              <span class="title">
                               <c:if test="${subscription.productBundle == null}">
                                 <spring:message code="launchvm.utility.bundle.name" />
                               </c:if>
                               <c:if test="${subscription.productBundle != null}">
                                  <c:out value="${subscription.productBundle.name}"/>
                                </c:if>
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
                                  <c:when test="${subscription.handle == null}">
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
                                        <div class="raw_contents_value">
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
  <div class="widget_rightpanel" id="viewDetailsDiv">
  <c:if test="${(empty subscriptions || subscriptions == null)&& current_page == 1}">
    <jsp:include page="/WEB-INF/jsp/tiles/billing/viewsubscription.jsp"></jsp:include>
  </c:if>
  </div>
</div>

    
<input type="hidden" id="current_page"  value="<c:out value="${current_page}"/>"/>
<input type="hidden" id="selected_subs_for_details"  value="<c:out value="${idForDetails}"/>"/>
<input type="hidden" id="tenantParam"  value="<c:out value="${tenant.param}"/>"/>
<input type="hidden" id="states"  value="<c:out value="${states}"/>"/>
