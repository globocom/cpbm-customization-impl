<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<spring:message code="label.catalog.utilityrate.generated.by" var="generatedBy"/>
<spring:message code="label.catalog.utilityrate.closing.bracket" var="bracket"/>
  <!-- Utility Charges -->
     <c:forEach items="${mapToDisplay}" var="serviceMap" varStatus="serviceLoopStatus">
      <div class="utilityrateslist_panel" style="border:none; margin:0 0 15px 0;">
        <div class="utilityrateslist_header on" style="background:#c2e6f8;" id="service_<c:out value="${serviceMap.key.uuid}"/>">
          <div class="utilityrateslist_header_content">
            <div class="utilityrateslist_title on" id="title_service_<c:out value="${serviceMap.key.uuid}"/>"><a name="service_<c:out value="${serviceMap.key.uuid}"/>" > <c:out value="${serviceMap.key.category}"></c:out> </a></div>
          </div>
        </div>
          <c:set var="instanceMap" value="${serviceMap.value}"></c:set>
          <c:forEach items="${instanceMap}" var="instanceMapVar" varStatus="instanceLoopStatus">
            <div class="usagelist_gridbox" id="instance<c:out value="${instanceMapVar.key.uuid}"/>data" style="width: 100%; margin:0; left:0; top:0;">
              <div class="db_gridbox_rows header" style="background:#ebf5fc;">
                 <div class="db_gridbox_columns" style="width:55%;">
                   <div class="db_gridbox_celltitles header" style="margin-left: 30px;"> <c:out value="${instanceMapVar.key.name}"></c:out> </div>
                 </div>
              </div>
                  <c:set var="serviceUsagetypeMap" value="${instanceMapVar.value}"></c:set>
                  <c:forEach items="${serviceUsagetypeMap}" var="serviceUsagetypeMapVar" varStatus="serviceUsageTypeLoopStatus">
                        <c:set var="ifGenerated" value="false"></c:set>
                        <c:forEach items="${generatedUsageListForServiceResourceType}" var="generatedUsageListForServiceResourceTypeVar" varStatus="generatedUsageListForServiceResourceTypeLoopStatus">
                          <c:if test="${serviceUsagetypeMapVar.key.usageTypeName eq generatedUsageListForServiceResourceTypeVar.usageTypeName}">
                            <c:set var="ifGenerated" value="true"></c:set>
                          </c:if>
                        </c:forEach>
                        <div class="db_gridbox_rows header" <c:if test="${ifGenerated == true && showGenerated == true}"> style="background:#efefb0;" </c:if> <c:if test="${ifGenerated == false }"> style="background:#c0c0c0;" </c:if> >
                          <div class="db_gridbox_columns" style="width:45%;">
                           <div class="db_gridbox_celltitles header" style="margin-left: 60px;"> <c:out value="${serviceUsagetypeMapVar.key.usageTypeName}"></c:out> <c:if test="${ifGenerated == true && showGenerated == true }"><c:out value="${generatedBy}"></c:out><c:out value="${resourceTypeName}"></c:out><c:out value="${bracket}"></c:out></c:if>
                           </div>
                          </div>
                         
                        </div>
                           <div class="db_gridbox_rows header">
                            <div class="db_gridbox_columns" style="width:22%;">
                               <div class="db_gridbox_celltitles header" style="margin-left: 30px;"><spring:message code="label.bundle.edit.urc.product"/></div>
                            </div>
                            <div class="db_gridbox_columns" style="width:40%;">
                               <div class="db_gridbox_celltitles header" style="width:99%;"><spring:message code="label.bundle.edit.urc.description"/></div>
                            </div>              
                            <div class="db_gridbox_columns" style="width:10%;">
                               <div class="db_gridbox_celltitles header"><spring:message code="label.bundle.edit.urc.units"/></div>
                            </div>       
                            <div class="db_gridbox_columns last" style="width:28%;">
                               <div style="width:90%; margin-left: 50px;" class="db_gridbox_celltitles header">
                               <spring:message code="ui.products.label.create.product.list.price"/><c:out value="${currency.sign}"></c:out><spring:message code="ui.products.label.create.product.right.brace"/>
                               </div>
                            </div>               
                            </div>                        
                            <c:set var="productsMap" value="${serviceUsagetypeMapVar.value}"></c:set>
                            <c:forEach items="${productsMap}" var="productsMapVar" varStatus="productMapLoopStatus">
                                   <c:choose>
                                    <c:when test="${status.index % 2 == 0}">
                                      <c:set var="rowClass" value="odd"/>
                                    </c:when>
                                    <c:otherwise>
                                      <c:set var="rowClass" value="even"/>
                                    </c:otherwise>
                                  </c:choose>                                          
                                
                                  <div class="db_gridbox_rows rowClass">
                                    <div class="db_gridbox_columns" style="width:22%;">
                                      <div class="db_gridbox_celltitles" style="margin-left: 30px;"><c:out value="${productsMapVar.key.name}"/></div>
                                    </div>
                                    <div class="db_gridbox_columns" style="width:40%;">
                                      <div class="db_gridbox_celltitles ellipsis"  title="<c:out value="${productsMapVar.key.description}"/>" style="width:99%;"><c:out value="${productsMapVar.key.description}"/></div>
                                    </div>
                                    <div class="db_gridbox_columns" style="width:10%;">
                                      <div class="db_gridbox_celltitles">
                                      <spring:message code="${productsMapVar.key.uom}"/></div>
                                      </div>                   
                                    <div class="db_gridbox_columns last" style="width:28%;">
                                      <c:if test="${not empty productsMapVar.value}">
                                        <div style="width:80%; margin-left: 50px;" class="db_gridbox_celltitles"><c:out value="${currency.sign}" />&nbsp;<fmt:formatNumber pattern="${currencyFormat}"  minFractionDigits="${minFractionDigits}" value="${productsMapVar.value}"  /></div>
                                      </c:if>
                                    </div>
                                  </div>
                            </c:forEach>
                  </c:forEach>
              </div>
          </c:forEach>       
      </div>
   </c:forEach>