<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

 <div class="main_detailsbox" style="width:98%;">
  <!-- Starting of entitlement data -->
        <div id="entitlementsDiv"> 
         
         <div class="main_details_titlebox" >
           <h2><spring:message code="label.bundle.list.entitlement.title"/></h2>
          </div>          
  <div class="db_gridbox_rows detailsheader" >
    <div class="db_gridbox_columns" style="width:35%;">
       <div class="db_gridbox_celltitles header"><spring:message code="label.bundle.edit.urc.product"/></div>
    </div>
    <div class="db_gridbox_columns" style="width:35%;">
       <div class="db_gridbox_celltitles header"><spring:message code="label.bundle.list.entitlement.included.units"/></div>
    </div>     
    <div class="db_gridbox_columns last">
    </div>               
  </div>
<c:forEach var="entitlementComponent" items="${entitlements}" varStatus="status">               
                  <c:choose>
            <c:when test="${status.index % 2 == 0}">
              <c:set var="rowClass" value="odd"/>
            </c:when>
            <c:otherwise>
                <c:set var="rowClass" value="even"/>
            </c:otherwise>
          </c:choose>
                      
           <div class="<c:out value="db_gridbox_rows ${selected}  ${rowClass}"/>"
            id="entitlementrootdiv<c:out value='${entitlementComponent.id}' />" >
            <div class="db_gridbox_columns" style="width:35%;">
                <div class="db_gridbox_celltitles">
                  <c:out value="${entitlementComponent.product.name}"/></div>
              </div>
              <div class="db_gridbox_columns" style="width:35%;">
                <div class="db_gridbox_celltitles">
                
                
                <div id="valuenotedit<c:out value="${entitlementComponent.id}"/>">
                  <c:choose>                
                    <c:when test="${entitlementComponent.includedUnits == -1}">
                     <c:set var="includedunitsvalue" value="0"/>
                    <span style="font-weight: normal;"><spring:message code="label.bundle.list.entitlement.unlimited"/></span>
                    <span style="margin-left:5px;font-weight: normal;"> <spring:message code="${entitlementComponent.product.uom}"/></span>
                    </c:when>
                    <c:otherwise>
                     <c:set var="includedunitsvalue" value="${entitlementComponent.includedUnits}"/>
                    <c:out value="${entitlementComponent.includedUnits}"/>
                    <span style="margin-left:5px;font-weight: normal;"> <spring:message code="${entitlementComponent.product.uom}"/></span>
                    </c:otherwise>
                  </c:choose>
                </div>                    
                </div>
              </div> 
             <div class="db_gridbox_columns last" >
              <div class="db_gridbox_celltitles" style="float: right;">              
              </div>
            </div>   
            </div>
       </c:forEach> 
        </div>  
       
  </div>
         
             