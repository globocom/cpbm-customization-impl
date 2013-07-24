<%-- Copyright (C) 2013 Citrix Systems, Inc. All rights reserved --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<spring:message code="dateonly.format" var="date_format"/> 
<script type="text/javascript">
  
  var effective_date_str = '<spring:message code="label.catalog.utilityrate.effective.date"/> <fmt:formatDate value="${startDate}" pattern="${date_format}" timeZone="${currentUser.timeZone}"/>';
  
</script>
<table class="table table-condensed table-hover table-bordered">  
  <thead class="utility_rate_table_header">  
    <tr>  
      <th><p><spring:message code="label.catalog.utility.card.table.header.product"/></p></th>  
      <th><p><spring:message code="label.catalog.utility.card.table.header.category"/></p></th>  
      <th><p><spring:message code="label.catalog.utility.card.table.header.units"/></p></th>  
      <th><p><spring:message code="label.catalog.utility.card.table.header.price"/>&nbsp;(<c:out value="${currency.currencyCode}" />)</p></th>  
    </tr>  
  </thead>  
  <tbody>
  <c:forEach items="${retMap}" var="serviceMap" varStatus="serviceLoopStatus"> 
    <c:set var="instanceMap" value="${serviceMap.value}"></c:set> 
    <c:forEach items="${instanceMap}" var="instanceMapVar" varStatus="instanceLoopStatus">
    <c:set var="serviceUsagetypeMap" value="${instanceMapVar.value}"></c:set>
    <c:forEach items="${serviceUsagetypeMap}" var="serviceUsagetypeMapVar" varStatus="serviceUsageTypeLoopStatus">
    <c:set var="productsMap" value="${serviceUsagetypeMapVar.value}"></c:set>
    
    <c:forEach items="${productsMap}" var="productsMapVar" varStatus="productMapLoopStatus">
    <tr>  
      <td><p class="title"><c:out value="${productsMapVar.key.name}"/></p><p class="description"><c:out value="${productsMapVar.key.description}"/></p></td>  
      <td><c:out value="${productsMapVar.key.category.name}"/></td>  
      <td><spring:message code="${productsMapVar.key.uom}"/></td>  
      <td><c:out value="${currency.sign}" /><fmt:formatNumber pattern="${currencyFormat}"  minFractionDigits="${minFractionDigits}" value="${productsMapVar.value}"  /></td>  
    </tr>  
    </c:forEach>
    </c:forEach>
    </c:forEach>
  </c:forEach>
        
  </tbody>  
</table>
    