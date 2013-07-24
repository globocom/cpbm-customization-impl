<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!-- Start Grid -->
       <div class="main_listbox" >
          <div class="main_gridpanel" style="border:1px solid #CCCCCC;margin:2px 0 0 13px;">
            <!-- Header -->
            <div class="widget_grid inline header" >
                  <div class="widget_grid_cell" style="width:30%;">
                      <span class="header"><spring:message code="label.name"/></span>
                     </div>
                     <div class="widget_grid_cell" style="width:30%;">
                      <span class="header"><spring:message code="label.value"/></span>
                     </div>
                     <div class="widget_grid_cell" style="width:25%;">
                      <span class="header"><spring:message code="ui.configuration.list.page.description"/></span>
                     </div>
                 </div>
                 <!-- Header end -->
                 <!-- Data Start-->
                 
                 <div class="main_gridlistbox" style="height:315px;">
                 <c:forEach var="configuration" items="${configurationList}" varStatus="status">
                   
                  <c:choose>
                  <c:when test="${status.index % 2 == 0}">
                      <c:set var="rowClass" value="odd"/>
                  </c:when>
                  <c:otherwise>
                      <c:set var="rowClass" value="even"/>
                  </c:otherwise>
                  </c:choose> 
                  <c:set var="enable_service_config" value="${fn:toLowerCase(configuration.module)}.${fn:toLowerCase(fn:replace(configuration.component, ' ', ''))}.enabled" />
                  <c:if  test='${configuration.name != enable_service_config}'>            
                  <div class="<c:out value="widget_grid ${rowClass}"/>">
                    <div class="widget_grid_cell" style="width:30%;" title="<c:out value="${configuration.name}"/>" >
                       <span class="celltext ellipsis" style="margin-left:10px;">
                        <c:set var="propertyName" value="${fn:split(configuration.name,'.')}"/>
                          <c:out value="${propertyName[fn:length(propertyName)-2]}.${propertyName[fn:length(propertyName)-1]}"/>
                        </span>
                    </div>
                    <div class="widget_grid_cell" style="width:30%;">
                       <div class="values">
                          <div id="valuenotedit<c:out value="${configuration.id}"/>">
                            <c:choose>
                                 <c:when test="${configuration.isEncryptionRequired == true}">
                               <c:out value="****"/>
                              </c:when>
                                 <c:otherwise>
                                  <c:out value="${configuration.value}" />
                                 </c:otherwise>
                            </c:choose> 
                          </div>  
                                <div id="valueedit<c:out value="${configuration.id}"/>" style="display: none;">
                                  <input type="text" id="value<c:out value="${configuration.id}"/>" class="configtext" value="<c:out value="${configuration.value}"/>" name="value" />
                                  <div class="main_addnew_formbox_errormsg" style="margin:0;width:180px;"  id="valueerror<c:out value="${configuration.id}"/>"></div>  
                                </div> 
                        </div>
                    </div>
                    <div class="widget_grid_cell" style="width:25%;">
                       <span class="celltext" style="margin-left:10px;">
                          <c:catch var ="catchException"> 
                           <spring:message code="config.${configuration.name}.description"/>     
                          </c:catch>
                          <c:if test = "${catchException!=null}">
                          </c:if>                                      
                        </span>
                    </div>
                  
                   </div>
                  </c:if>
                  </c:forEach>
                    
                 <!-- Data end -->
          </div>
       </div>
       <!-- End Grid -->