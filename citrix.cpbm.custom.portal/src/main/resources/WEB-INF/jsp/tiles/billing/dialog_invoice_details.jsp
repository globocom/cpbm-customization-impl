<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="netInvoiceItem" value="null"></c:set>
<spring:message code="dateonly.format" var="date_only_format"/>
<c:set var="invoiceUuid" value="${invoice.uuid}"></c:set>
<c:set var="invoiceType" value="${invoice.type}"></c:set>
<c:set var="invoiceitems" value="${invoice.invoiceItems}"></c:set>
<fmt:formatDate var="invoiceStartDate" pattern="${date_only_format}" value="${invoice.serviceStartDate}" timeZone="${currentUser.timeZone}"/>
<fmt:formatDate var="invoiceEndDate" pattern="${date_only_format}" value="${invoice.serviceEndDate}" timeZone="${currentUser.timeZone}"/>

<spring:message code="label.invoice.details" var="invoiceDetailsString"/>
<c:set var="invoiceDetailTitle" value="${invoiceDetailsString} (${invoiceStartDate} - ${invoiceEndDate})"></c:set>
<div  id="dialog_invoice_details_<c:out value='${invoiceUuid}'/>" title="${invoiceDetailTitle}" style="display: none; overflow: hidden;">
    <div class="more_details_grid_lightbox" >
		
		  <div class="mainbox">
		       <div class="morecontentarea"> 
		       <div class="db_gridbox_rows" style="border-bottom-color: #E1E1E1;border-bottom-style: solid;border-bottom-width: .5px">    
              <c:choose>
                <c:when test="${invoiceType eq 'CloseOut'}">
                  <div class="db_gridbox_columns" style="width:20%;">
                    <div class="db_gridbox_celltitles header more_details_grid"><spring:message code="label.bundle.edit.urc.product"/></div>
                  </div>
                   <div class="db_gridbox_columns" style="width:20%;">
                <div class="db_gridbox_celltitles header more_details_grid"><spring:message code="ui.campaigns.label.create.title"/></div>
              </div>
                </c:when>
                <c:otherwise>
                  <div class="db_gridbox_columns" style="width:20%;">
                    <div class="db_gridbox_celltitles header more_details_grid"><spring:message code="ui.label.emptylist.billing.leftnav.subscription"/></div>
                  </div>
                   <div class="db_gridbox_columns" style="width:20%;">
                <div class="db_gridbox_celltitles header more_details_grid"><spring:message code="ui.campaigns.label.create.title"/></div>
              </div>
                </c:otherwise>
              </c:choose>    
              <div class="db_gridbox_columns" style="width:20%;">
		            <div class="db_gridbox_celltitles header more_details_grid"><spring:message code="label.usage.billing.subscription.charge.type"/></div>
		          </div>
		           <div class="db_gridbox_columns" style="width:20%;">
		            <div class="db_gridbox_celltitles header more_details_grid"><spring:message code="label.invoice.details.quantity"/></div>
		          </div>
		           <div class="db_gridbox_columns" style="width:20%;">
		            <div class="db_gridbox_celltitles header more_details_grid"><spring:message code="label.usage.billing.subscription.charge.amount"/></div>
		          </div>
		     </div>
                 <spring:message code="dateonly.format" var="date_only_format"/>
		         <c:forEach items="${invoiceitems}" var="invoiceitem" varStatus="invoiceitemLoopStatus">
	               <c:if test="${invoiceitem.chargeType ne 'NET_TOTAL_CHARGES'}">
                 <div class="db_gridbox_rows" 
                    <c:if test="${invoiceitem.chargeType eq 'TOTAL_CHARGES' or invoiceitem.chargeType eq 'TOTAL_DISCOUNTS' or invoiceitem.chargeType eq 'TOTAL_TAX'}">
                      style="border-top-color: #E1E1E1;border-top-style: solid;border-top-width: .5px;border-bottom-color: #E1E1E1;border-bottom-style: solid;border-bottom-width: .5px"
                    </c:if>
                  >    
	                 <c:choose>
                      <c:when test="${invoiceType eq 'CloseOut'}">
                        <div class="db_gridbox_columns" style="width:20%;">
                          <div class="db_gridbox_celltitles" title="${invoiceitem.product.name}">
                             <c:if test="${invoiceitem.chargeType eq 'CHARGE'}"><c:out value="${invoiceitem.product.name}"></c:out></c:if>
                          </div>
                        </div>
                        <div class="db_gridbox_columns" style="width:20%;">
                     <div class="db_gridbox_celltitles ellipsis" title="<c:if test="${invoiceitem.chargeType eq 'CHARGE'}"><c:out value="${invoiceitem.product.description}"></c:out></c:if>">
                        <c:if test="${invoiceitem.chargeType eq 'CHARGE'}"><c:out value="${invoiceitem.product.description}"></c:out></c:if>
                      </div>
                   </div>
                      </c:when>
                      <c:otherwise>
                        <div class="db_gridbox_columns" style="width:20%;">
                          <div class="db_gridbox_celltitles">
                            <c:if test="${invoiceitem.chargeType eq 'CHARGE'}"><c:out value="${invoiceitem.subscription.uuid}"></c:out></c:if>
                          </div>
                        </div>
                        <div class="db_gridbox_columns" style="width:20%;">
                     <div class="db_gridbox_celltitles">
                        <c:if test="${invoiceitem.chargeType eq 'CHARGE'}"><c:out value="${invoiceitem.description}"></c:out></c:if>
                      </div>
                   </div>
                      </c:otherwise>
                  </c:choose>
                   
                   <div class="db_gridbox_columns" style="width:20%;">
                      <spring:message code="label.usage.billing.invoiceitem.chargetype.${invoiceitem.chargeType}" var="chargeTypeLabel"/>
	                   <div class="db_gridbox_celltitles"><c:out value="${chargeTypeLabel}"></c:out></div>
	                 </div>
                  <c:choose>
                      <c:when test="${invoiceitem.chargeType eq 'CHARGE' and invoiceitem.product ne 'null'}">
                        <div class="db_gridbox_columns" style="width:20%;">
                          <div class="db_gridbox_celltitles">
                              <c:out value="${invoiceitem.quantity} ${invoiceitem.product.uom}"></c:out>
                          </div>
                        </div>
                      </c:when>
                  </c:choose>
	                  <div class="db_gridbox_columns" style="width:20%;">
	                   <div class="db_gridbox_celltitles">
	                     <c:out value="${tenant.currency.sign}" /><fmt:formatNumber pattern="${currencyFormat}"  value="${invoiceitem.amount}" minFractionDigits="${minFractionDigits}" />
	                   </div>
	                 </div>
                  </div>
                  </c:if>
                  <c:if test="${invoiceitem.chargeType eq 'NET_TOTAL_CHARGES'}">
                    <c:set var="netInvoiceItem" value="${invoiceitem}"></c:set>
                  </c:if>
	           </c:forEach>

		     </div> 
		    
		  </div>
		</div>
    <c:if test="${netInvoiceItem ne 'null'}">
       <div class="db_gridbox_columns" style="width:60%;">
                      <div class="db_gridbox_celltitles header more_details_grid"></div>
        </div>
        <div class="db_gridbox_columns" style="width:18%;">
                      <div class="db_gridbox_celltitles header more_details_grid"><b><spring:message code="label.usage.billing.invoiceitem.chargetype.NET_TOTAL_CHARGES"/></b></div>
        </div>
        <div class="db_gridbox_columns" style="width:22%;">
                      <div class="db_gridbox_celltitles header"><b><c:out value="${tenant.currency.sign}" /><fmt:formatNumber pattern="${currencyFormat}"  value="${netInvoiceItem.amount}" minFractionDigits="${minFractionDigits}" /></b></div>
        </div>
     </c:if>
</div>