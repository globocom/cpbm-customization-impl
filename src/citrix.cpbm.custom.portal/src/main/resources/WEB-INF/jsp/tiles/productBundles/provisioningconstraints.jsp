<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
 <%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<script type="text/javascript">
    var productBundlesUrl = "<%=request.getContextPath() %>/portal/productBundles/";
</script>

<div class="widget_details_actionbox">
    <ul class="widget_detail_actionpanel" style="float:left;">
    </ul>
</div>

<div class="widget_inline_chargesbox">
    <div class="widget_grid inline subheader">
       <div class="widget_grid_cell header borders" style="width: 200px;">
           <span class="subheader"><spring:message code="label.component.name"/></span>
       </div>
        <div class="widget_grid_cell borders" style="width: 150px;">
               <span class="subheader"><spring:message code="label.component.association"/></span>
         </div>
         <div class="widget_grid_cell borders" style="width: 150px;">
             <span class="subheader"><spring:message code="label.component.value"/></span>
         </div>
    </div>
</div>

<c:forEach items="${constraints}" var="constraint" varStatus="status">

   <c:choose>
      <c:when test="${status.index % 2 == 0}">
        <c:set var="rowClass" value="odd"/>
      </c:when>
      <c:otherwise>
        <c:set var="rowClass" value="even"/>
      </c:otherwise>
    </c:choose>

    <div class="<c:out value="widget_grid inline ${rowClass}"/>">

        <div class="widget_grid_cell borders" style="height:27px; width:200px" title="<spring:message code="${constraint.productBundle.serviceInstanceId.service.serviceName}.ResourceType.${constraint.productBundle.resourceType.resourceTypeName}.${constraint.componentName}.name"/>">
           <span class="celltext right ellipsis" style="width: 80%;"><spring:message code="${constraint.productBundle.serviceInstanceId.service.serviceName}.ResourceType.${constraint.productBundle.resourceType.resourceTypeName}.${constraint.componentName}.name"/></span>
        </div>

         <div class="widget_grid_cell borders" style="height:27px; width: 150px;">
            <span class="celltext">
               <spring:message code="ui.label.${fn:toLowerCase(constraint.association)}" />
            </span>
         </div>

         <div class="widget_grid_cell" style="height:27px; width: auto;">
            <span title='<c:out value="${constraint.componentValueDisplayName}" />' class="celltext ellipsis" style="width: 200px;">
               <c:out value="${constraint.componentValueDisplayName}" />
            </span>
         </div>
    </div>
</c:forEach>
