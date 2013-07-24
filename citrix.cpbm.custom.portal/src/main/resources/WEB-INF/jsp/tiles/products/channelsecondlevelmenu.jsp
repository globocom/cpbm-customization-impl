<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<jsp:include page="js_messages.jsp"></jsp:include>
<div class="secondlevel_withsubmenu">
    <div class="secondlevel_breadcrumb_panel">
      <div class="secondlevel_breadcrumbbox">
          <p><spring:message code="page.level1.channels"/></p>
        </div>
    </div>

     <div class="secondlevel_menupanel">
        <a  class="secondlevel_menutabs <c:out value="${ChannelsInner}"/>" href="<%=request.getContextPath() %>/portal/channels/list">
            <spring:message code="page.level2.channels"/>
          </a>          
          <a  style="width:150px;" class="secondlevel_menutabs <c:out value="${Campaigns}"/>" href="<%=request.getContextPath() %>/portal/promotions/list">
            <spring:message code="page.level2.campaigns"/>
          </a>
   </div>

   <tiles:insertDefinition name="warnings"></tiles:insertDefinition>

</div>