  <%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<script type="text/javascript">
var accountPath = "<%=request.getContextPath() %>/portal/tenants/";
var userPath = "<%=request.getContextPath() %>/portal/users/account_userpopup";
var productsPath = "<%=request.getContextPath() %>/portal/products";
var help_link_path = "<c:out value="${helpUrl}"/>";
var notifications_link_path = "<%=request.getContextPath() %>/portal/tenants/notifications?tenant=<c:out value='${tenant.param}'/>&filterBy=Today";
</script>

<c:if test="${currentTenant.id != 1}">
  <sec:authorize access="hasAnyRole('ROLE_ACCOUNT_ADMIN','ROLE_ACCOUNT_BILLING_ADMIN','ROLE_FINANCE_CRUD')">
    <script type="text/javascript">
      var balance_link_path="<%=request.getContextPath() %>/portal/billing/usageBilling?tenant=<c:out value='${tenant.param}'/>";
    </script>
  </sec:authorize>
</c:if>

<script type="text/javascript">
  var health_link_path = "<%=request.getContextPath() %>/portal/health";
  var health_maintenance_link_path = "<%=request.getContextPath() %>/portal/health/healthmaintainance";
</script>

<script type="text/javascript">
  var billingPath = "<%=request.getContextPath() %>/portal/billing/";
  var effectiveTenantParam = '<c:out value="${effectiveTenant.param}"/>';
</script>
<c:set var="auth" value="n" />
<sec:authorize access="!hasAnyRole('ROLE_ACCOUNT_ADMIN','ROLE_ACCOUNT_BILLING_ADMIN', 'ROLE_FINANCE_CRUD')">
  <script type="text/javascript">
    var billingPath = "<%=request.getContextPath() %>/portal/usage/";
  </script>
</sec:authorize>


<div id="header">
  <div class="header_left">
    <tiles:insertAttribute name="logo" />
  </div>
  <div class="header_right">
    <div class="userprofile_button">
      <div class="userprofile_buttonlink"><c:out value="${currentUser.firstName}"/>&nbsp;&nbsp;<c:out value="${currentUser.lastName}"/></div>
      <div class="userprofile_arrows"><img src="<%= request.getContextPath() %>/images/userprofile_downarrow.png" /></div>
      <!--dropdown starts here-->
      <div class="userprofile_dropdownbox" style="display:none;" id="userprofile_dropdownbox">
        <div class="userprofile_dropdownbox_top"></div>
        <div class="userprofile_dropdownbox_bot">
          <div class="userprofile_dropdownlist">
            <ul>
      
              <li><a href="<%=request.getContextPath() %>/portal/users/<c:out value="${currentUser.param}"/>/myprofile"><spring:message code="ui.header.page.title.myprofile"/></a></li>
              <sec:authorize access="hasRole('ROLE_CONFIGURATION_CRUD') or hasRole('ROLE_ACCOUNT_CRUD')">
                 <sec:authorize access="hasRole('ROLE_CONFIGURATION_CRUD')">
                   <li><a href="<%=request.getContextPath() %>/portal/admin/config/showconfiguration"><spring:message code="page.level1.admin"/></a></li>
                 </sec:authorize>
                 <sec:authorize access="hasRole('ROLE_ACCOUNT_CRUD') and hasRole('ROLE_USER_CRUD') and !hasRole('ROLE_CONFIGURATION_CRUD')">
                   <li><a href="<%=request.getContextPath() %>/portal/users/listusersforaccount"><spring:message code="page.level1.admin"/></a></li>
                 </sec:authorize>
                 <sec:authorize access="hasRole('ROLE_ACCOUNT_CRUD') and !hasRole('ROLE_USER_CRUD') and !hasRole('ROLE_CONFIGURATION_CRUD')">
                   <li><a href="<%=request.getContextPath() %>/portal/admin/batch/status"><spring:message code="page.level1.admin"/></a></li>
                 </sec:authorize>
              </sec:authorize>
              <sec:authorize access="hasAnyRole('ROLE_ACCOUNT_ADMIN','ROLE_ACCOUNT_CRUD','ROLE_ACCOUNT_BILLING_ADMIN')">
                <li><a href="<%=request.getContextPath() %>/portal/tenants/editcurrent"><spring:message code="ui.header.page.title.companysetup"/></a></li>
              </sec:authorize>
              <sec:authorize access="hasAnyRole('ROLE_ACCOUNT_CRUD')">
                <c:if test="${isAdmin}">
                  <li><a href="<%=request.getContextPath() %>/portal/tenants/editcurrentlogo"><spring:message code="ui.header.page.title.customizecompany"/></a></li>
                </c:if>
              </sec:authorize>
              <li><a href="<%= request.getContextPath() %>/portal/<c:out value="${currentUser.param}"/>/loggedout"><spring:message code="ui.header.page.title.logout"/></a></li>
            </ul>
          </div>
        </div>
      </div>
      <!--dropdown ends here-->
    </div>
  </div>
</div>
<div id="mainmenu_panel">
  <div class="mainmenu_panel_left">
    <div class="mainmenu_button <c:out value="${Home}"/>">
      <div class="mainmenu_button_linkbox">
        <a href="<%=request.getContextPath() %>/portal/home">
          <spring:message code="page.level1.home"/>
        </a>
      </div>
    </div>
    
    <c:if test="${currentTenant.id != 1}">
        <div class="mainmenu_button <c:out value="${MyServices}"/>"> 
            <div class="mainmenu_button_linkbox">
                <spring:url value="/portal/connector/csinstances" var="connectors_list" htmlEscape="false">
                  <spring:param name="tenant"><c:out value="${tenant.param}"/></spring:param>
                </spring:url>
                <a  href="<c:out value="${connectors_list}"/>"><spring:message code="page.level2.myservices"/></a> 
            </div>
        </div>
    </c:if>
    <c:if test="${currentTenant.id != 1}">
    <sec:authorize access="hasAnyRole('ROLE_ACCOUNT_USER_CRUD')">
      <div class="mainmenu_button <c:out value="${Users}"/>" id="userstab" >
        <div class="mainmenu_button_linkbox" id="accountuserstab">
            <a href="<%=request.getContextPath() %>/portal/users/listusersforaccount" >
            <spring:message code="page.level1.users"/>          
          	</a>
        </div>
      </div>
      </sec:authorize>
    </c:if>

    <c:if test="${currentTenant.id != 1 }">
    <div class="mainmenu_button <c:out value="${Catalog}"/>" id="catalogtab">
      <div class="mainmenu_button_linkbox">
		      <spring:url value="/portal/subscription/createsubscription" var="catalog_path" htmlEscape="false">
		        <spring:param name="tenant"><c:out value="${tenant.param}"/></spring:param>
		      </spring:url>   
	        <a  href="<c:out value="${catalog_path}"/>"><spring:message code="page.level1.catalog"/></a>
      </div>
    </div>
    </c:if>
    
    <sec:authorize access="hasAnyRole('ROLE_ACCOUNT_CRUD','ROLE_ACCOUNT_MGMT')">
      
      <div class="mainmenu_button <c:out value="${Crm}"/>" id="crmtab" >
        <div class="mainmenu_button_linkbox">
          <a href="<%=request.getContextPath() %>/portal/tenants/list" >
            <spring:message code="page.level1.accounts"/>
          </a>
        </div>
      </div>
    </sec:authorize>
    <sec:authorize access="hasRole('ROLE_PRODUCT_CRUD')">
      <div class="mainmenu_button <c:out value="${Products}"/>" id="productstab" onclick="listProductsTabItems(this)">
        <div class="mainmenu_button_linkbox">
          <a href="#">
            <spring:message code="page.level1.products"/>
          </a>
        </div>
        <div class="menu_dropdown" id="menu_dropdwon_for_products" style="display:none">
          <ul>
              <li id="plannedDropdown"><a href="<%=request.getContextPath() %>/portal/products/getServiceCategories?whichPlan=planned"><spring:message code="ui.label.plan.next"/></a></li>
              <li id="currentDropdown"><a href="<%=request.getContextPath() %>/portal/products/getServiceCategories?whichPlan=current"><spring:message code="ui.label.view.current"/></a></li>
              <li id="historyDropdown"><a href="<%=request.getContextPath() %>/portal/products/getServiceCategories?whichPlan=history"><spring:message code="ui.label.view.history"/></a></li>
            </ul>
        </div>
      </div>
    </sec:authorize>
    <sec:authorize access="hasRole('ROLE_PRODUCT_CRUD')">
      <div class="mainmenu_button <c:out value="${Channels}"/>" id="channelstab">
        <div class="mainmenu_button_linkbox">
          <a href="<%=request.getContextPath() %>/portal/channels/list">
            <spring:message code="page.level1.channels"/>
          </a>
        </div>
      </div>
    </sec:authorize>
    
    <c:choose>
      <c:when test="${effectiveTenant.state eq 'ACTIVE'}">
        <spring:url value="/portal/health" var="healthURL" htmlEscape="false">
        </spring:url>   
      </c:when>
      <c:otherwise>
        <spring:url value="/portal/support/tickets" var="healthURL" htmlEscape="false">
        </spring:url>   
      </c:otherwise>
    </c:choose>
     <div class="mainmenu_button <c:out value="${Support}"/>" id="supporttab">
       <div class="mainmenu_button_linkbox">
         <a  href="<c:out value="${healthURL}"/>"><spring:message code="page.level1.support"/></a>
       </div>
     </div>
    <sec:authorize access="hasAnyRole('ROLE_REPORTING_ADMIN')">
      <div class="mainmenu_button <c:out value="${Reports}"/>" id="reportstab" >
        <div class="mainmenu_button_linkbox">
          <a href="<%=request.getContextPath() %>/portal/reports/newRegistrations">
            <spring:message code="page.level1.reports"/>
          </a>
        </div>
      </div>
    </sec:authorize>
  </div>
  <div class="mainmenu_panel_right">
  	<!--new top statistics starts here-->
  	<div class="mainmenu_infolinks">
      <li class="mainmenu_infolinks" id="top_right_nav_notifications_link" title="<spring:message code="label.notifications.for.today"/>">
        <c:if test="${!empty notifications_count_for_today}">
          <span class="icon notifications_numbersbg"><span class="noticationsnumber"><c:out value="${notifications_count_for_today}"/></span></span>
        </c:if> 
        <span class="icon notifications"></span>
        <span class="title"><spring:message code="label.today"/></span>
        <!-- dropdown starts here-->
          <div class="widget_actionpopover notificationsdropdown" id="top_nav_notifications_dropdown" style="display:none;">
          		<div class="widget_actionpopover_top notificationsdropdown"></div>
                <div class="widget_actionpopover_mid notificationsdropdown">
                	<ul class="widget_actionpoplist notificationsdropdown">
                    <c:forEach var="latest_notification" items="${latest_notifications}" varStatus="status">
                      <li>
                      <span class="icon <c:out value="${fn:toLowerCase(latest_notification.severity)}"></c:out>"></span>
                        <p>
                          <c:choose>
                            <c:when test="${latest_notification.useMessageKey eq true}">
                              <span class="title"><spring:message code="${latest_notification.message}" arguments="${latest_notification.messageArguments}"/></span>
                              <span class="description"><spring:message code="${latest_notification.description}" arguments="${latest_notification.messageArguments}"/></span>
                            </c:when>
                            <c:otherwise>
                              <span class="title"><c:out value="${latest_notification.message}"></c:out></span>
                              <!--span class="description"><c:out value="${latest_notification.attributes}"></c:out></span-->
                            </c:otherwise>
                          </c:choose>
                        </p>
                      </li>
                    </c:forEach>
                   </ul>
                    <div class="widget_actionpopover_mid notificationsdropdown_footer">
                    	<a href="javascript:void(0)" id="top_right_nav_notifications_more_link"><spring:message code="label.top.right.nav.notifications.more"/></a>
                    </div>
                </div>
                <div class="widget_actionpopover_bot notificationsdropdown"></div>
          </div>
         <!-- dropdown ends here-->
        
        
        </li>
      <c:if test="${!empty top_nav_health_status}">
        <li class="mainmenu_infolinks" id="top_right_nav_health_link" title="<spring:message code="ui.header.page.title.servicehealth"/>">
          <c:if test="${top_nav_health_status=='NORMAL'}">
            <span class="icon servicehealth normal"></span>
          </c:if>
          <c:if test="${top_nav_health_status=='ISSUE'}">
            <span class="icon servicehealth performanceissue"></span>
          </c:if> 
          <c:if test="${top_nav_health_status=='DOWN'}">
            <span class="icon servicehealth down"></span>
          </c:if>
           <c:if test="${top_nav_health_status=='MAINTENANCE'}">
            <span class="icon servicehealth maintenance"></span>
          </c:if>
          <span class="title"><spring:message code="label.top.right.nav.service.health"/></span>
         <!-- dropdown starts here-->
          <div class="widget_actionpopover servicehealth_dropdown" id="top_nav_servicehealth_dropdown" style="display:none;">
          		<div class="widget_actionpopover_top OSdropdown"></div>
                <div class="widget_actionpopover_mid OSdropdown">
                	<ul class="widget_actionpoplist servicehealthdropdown" id="servicehealthdropdown">
                    <c:forEach items="${top_nav_zones}" var="zone">
                      <li id="zone_<c:out value="${zone.id}" />" onclick="view_service_health(this)"> <span class="icon servicehealth <c:out value="${zone.health_status}" />"></span><span class="servicename"><c:out value="${zone.name}" /></span></li>
                    </c:forEach>
                    </ul>
                    <div class="widget_actionpopover_mid healthdropdown_footer">
                      <a href="javascript:void(0)" id="top_right_nav_view_maintenance_link"><spring:message code="label.top.right.nav.health.view.maintenance"/></a>
                    </div>
                </div>
                <div class="widget_actionpopover_bot OSdropdown"></div>
          </div>
         <!-- dropdown ends here-->
        </li>
      </c:if>
      <c:if test="${currentTenant.id != 1}">
        <sec:authorize access="hasAnyRole('ROLE_ACCOUNT_ADMIN','ROLE_ACCOUNT_BILLING_ADMIN','ROLE_FINANCE_CRUD')">
        <li class="mainmenu_infolinks" id="top_right_nav_balance_link" title="<spring:message code="label.usage.billing.net.balance"/>"><span class="icon currencyflags"><img src="/portal/images/flags/<c:out value="${tenant.currency.flag}" />" alt="" /></span><span class="title"><c:out value="${tenant.currency.sign}" /><fmt:formatNumber pattern="${currencyFormat}" minFractionDigits="${minFractionDigits}" maxFractionDigits="${minFractionDigits}" value="${currentBalanceTopRightNav}" /></span></li>
        </sec:authorize>
      </c:if>
      <li class="mainmenu_infolinks last" id="top_right_nav_help_link" title="<spring:message code="label.help.documentation"/>"><span class="title"><spring:message code="ui.header.page.title.help"/></span></li>
    </div>
    <!--new top statistics ends here-->
    
  </div>
<div class="menu_dropdown_selectionbox" id="crm_drop_down" style="display:none;"></div>

</div>