<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

 <div class="widget_actionbar">
  <div class="widget_actionarea" id="top_actions">
    <div id="spinning_wheel" style="display:none">
      <div class="maindetails_footer_loadingpanel">
      </div>
      <div class="maindetails_footer_loadingbox first">
        <div class="maindetails_footer_loadingicon"></div>
        <p id="in_process_text"></p>
      </div>
    </div>
      <c:if test="${(not empty subscription || subscription != null)&& current_page != 1}">
       <div class="widget_moreactions action_menu_container" title="<spring:message code='manage'/>">
          <!--Actions popover starts here-->
          <div class="widget_actionpopover" id="action_menu" style="display:none;">
              <div class="widget_actionpopover_top"></div>
                <div class="widget_actionpopover_mid">
                  <ul class="widget_actionpoplist">
                    <c:choose>
                      <c:when test="${subscription.state=='ACTIVE' or subscription.state=='NEW'}">
                        <c:if test="${allowTermination && hasCloudAccess && cloudStackCallFailed == false}">
                        <li class="terminatesubscription_link" id="<c:out value="term_subscription_1_${subscription.param}"/>" title='<spring:message code="label.subscription.details.terminate"/>'><spring:message code="label.subscription.details.terminate"/></li>
                       </c:if>
                       <c:if test="${subscription.state=='NEW'}">
                        <li class="cancelsubscription_link" id="<c:out value="cancel_subscription_1_${subscription.param}"/>" title='<spring:message code="label.subscription.details.cancel"/>'><spring:message code="label.subscription.details.cancel"/></li>
                        </c:if>
                        <c:if test="${!isPayAsYouGoChosen}">
                          <c:choose>
                            <c:when test="${subscription.state=='ACTIVE' and subscription.resourceType != null and toProvision}">
                              <li class="provision_subscription_link" onclick="provisionSubscription(<c:out value="${subscription.id}"/>);" id="provision_subscription_1_<c:out value="${subscription.param}"/>" title='<spring:message code="ui.label.provision.resource"/>'><spring:message code="ui.label.provision.resource"/></li>
                            </c:when>
                            <c:when test="${subscription.state=='ACTIVE' and subscription.resourceType != null and !toProvision}">
                              <li class="provision_subscription_link" onclick="provisionSubscription(<c:out value="${subscription.id}"/>);" id="provision_subscription_1_<c:out value="${subscription.param}"/>" title='<spring:message code="ui.label.edit.resource"/>'><spring:message code="ui.label.edit.resource"/></li>
                            </c:when>
                          </c:choose>
                        </c:if>
                      </c:when>
                      <c:otherwise>
                              <li id="no_actions_available_volume" title='<spring:message code="label.no.actions.available"/>'><spring:message code="label.no.actions.available"/></li>
                      </c:otherwise>
                    </c:choose>
                    </ul>
                </div>
                <div class="widget_actionpopover_bot"></div>
            </div>
            <!--Actions popover ends here-->
         </div>
      </c:if>  
      </div>
  </div>
  
   
<div id="top_message_panel" class="common_messagebox widget" style="display:none;"><span id="status_icon"></span><p id="msg"></p></div>
<div id="action_result_panel" class="common_messagebox widget" style="display:none;"><span id="status_icon"></span><p id="msg"></p></div>
  
  <div class="widget_browser">
    <div id="spinning_wheel" style="display:none">
        <div class="widget_loadingpanel">
        </div>
        <div class="maindetails_footer_loadingbox first">
          <div class="maindetails_footer_loadingicon"></div>
          <p id="in_process_text"></p>
        </div>
      </div>
    <div class="widget_browsermaster">
        <div class="widget_browser_contentarea">
            <div class="widget_browsergrid_wrapper master">
                  <div class="widget_grid master even first">
                      <div class="widget_grid_labels">
                          <span><spring:message code="label.subscription.details.bundle"/></span>
                    </div>
                    <spring:message code="label.subscription.details.parent.subscription" var="subsciptions_parent_label"/>
                    <spring:message code="label.subscription.details.ongoing.subscription" var="subsciptions_ongoing_label"/>
                    <div class="widget_grid_description">
                        <span>
                          <c:if test="${subscription.productBundle != null}">
                            <c:out value="${subscription.productBundle.name}"/>
                          </c:if>
                          <c:if test="${not empty subscription && subscription.productBundle == null}">
                            <spring:message code="launchvm.utility.bundle.name" />
                          </c:if>
                          <c:if test="${subscription.derivedFrom != null && subscription.derivedFrom != ''}">
                            (<a href='/portal/portal/billing/subscriptions?id=<c:out value="${subscription.derivedFrom.uuid}"></c:out>'><c:out value="${subsciptions_parent_label}"></c:out></a>)
                          </c:if>                                   
                            
                          <c:if test="${subscription.state == 'EXPIRED'}">
                            <c:if test="${subscription.newSubscription != null}">
                              (<a href='/portal/portal/billing/subscriptions?id=<c:out value="${subscription.newSubscription.uuid}"></c:out>'><c:out value="${subsciptions_ongoing_label}"></c:out></a>)
                            </c:if>
                          </c:if>
                        </span>
                    </div>
                </div>
                
                <div class="widget_grid master even">
                    <div class="widget_grid_labels">
                        <span><spring:message code="ui.label.serviceinstance"/></span>
                    </div>
                    <c:if test="${(not empty subscription || subscription != null)&& current_page != 1}">
                   	 <div class="widget_grid_description">
                        <span><spring:message code="${subscription.serviceInstance.name}" /></span>
                   	 </div>
                    </c:if>
                </div>
               <c:if test="${(not empty subscription || subscription != null)&& current_page != 1}"> 
               	<div class="widget_grid master even">
                    <div class="widget_grid_labels">
                        <span><spring:message code="label.state"/></span>
                    </div>  
                    <div class="widget_grid_description">
                      
                      <span id="subscriptionState"><spring:message code="${subscription.state.code}" />
                      <c:if test="${toProvision and subscription.resourceType!=null}"> ( <spring:message code="label.no.active.subscription.handle"/> ) </c:if>
                      <c:if test="${not empty workflowUuid }">
                      	<a href="javascript:void(0);" id="workflowdetails${workflowUuid}" class="workflowDetailsPopup sub_wf"><spring:message code="message.view.workflow" /></a>
                      </c:if>
                      </span>
                    </div>
                </div>
              </c:if>  
            </div>
            <div class="widget_masterbigicons subscription">
            </div>
        </div>
    </div>
    
    <div class="widget_browser_contentarea">
      <ul class="widgets_detailstab">
          <li class="widgets_detailstab active" id="details_tab">
            <spring:message code="usage.billing.subscription.title.subscription.charges" />
          </li>
         
           <li class="widgets_detailstab nonactive" id="entitlements_tab">
             <spring:message code="usage.billing.view.subscription.entitlement.utility.rate.title" />
           </li>
            <c:if test="${subscription.resourceType != null && hasCloudAccess && cloudStackCallFailed == false}">
           <li class="widgets_detailstab nonactive" id="resource_details_tab">
             <spring:message code="ui.label.resource.details" />
           </li>
           <li class="widgets_detailstab nonactive" id="configurations_tab">
             <spring:message code="label.configuration.data" />
           </li>
          </c:if>
            
        </ul>
        
        <div class="widget_details_actionbox">
          <ul class="widget_detail_actionpanel">
            <c:if test="${allowTermination && hasCloudAccess && cloudStackCallFailed == false}">
              <li class="widget_detail_actionpanel terminatesubscription_link"><a href="#"><spring:message code="label.subscription.details.terminate"/></a> </li>
             </c:if>
          </ul>
        </div>
        
        <div class="widget_browsergrid_wrapper fixed" id="subscription_charges">
         
            <div class="widget_grid details header">
              <div class="widget_grid_cell" style="padding:1px;width:60%;">
                <span class="header"><spring:message code="label.charges" /></span>
              </div>            
              <div class="widget_grid_cell last" style="padding:1px;">
                <span class="header"><spring:message code="usage.billing.view.subscription.price.title" /></span>
              </div>
              
            </div>
             <c:if test="${not empty subscription}">
                <div class="<c:out value="grid_rows odd"/>">                   
                  <div class="grid_row_cell" style="padding:1px;width:60%;">              
                    <div class="row_celltitles"> <spring:message code="label.one.time"></spring:message></div>
                  </div>               
                  <div class="grid_row_cell last" style="padding:1px;">
                  <div class="row_celltitles">
                  <c:out value="${subscription.tenant.currency.sign}" />
                  <c:if test="${subscription.productBundle !=null}">
                    <fmt:formatNumber pattern="${currencyFormat}"  minFractionDigits="${minFractionDigits}" value="${subscription.nonRecurringCharge.price}"  />
                  </c:if>
                  <c:if test="${subscription.productBundle == null}">
                    <fmt:formatNumber pattern="${currencyFormat}"  minFractionDigits="${minFractionDigits}" value="0" />
                  </c:if>
                  </div>
                  </div>     
                 </div>
              <c:choose>
                <c:when test="${subscription.productBundle.rateCard.chargeType.frequencyInMonths != 0 }">
                  <div class="<c:out value="grid_rows even"/>">                   
                <div class="grid_row_cell" style="padding:1px;width:60%;">              
                  <div class="row_celltitles"><spring:message code="label.recurring">&nbsp;:&nbsp;</spring:message>
                 	 <c:if test="${current_page != 1 && subscription.productBundle !=null}">
                  		<spring:message code="charge.type.${subscription.productBundle.rateCard.chargeType.name}"/>
                  	 </c:if>
                  </div>
                </div>
                <div class="grid_row_cell last" style="padding:1px;">
                  <div class="row_celltitles">
                  	<c:if test="${current_page != 1 && subscription.productBundle !=null}"> 
                 		 <c:out value="${subscription.tenant.currency.sign}" />
                  		 <fmt:formatNumber pattern="${currencyFormat}"  minFractionDigits="${minFractionDigits}" value="${subscription.recurringCharge.price}"  />
                    </c:if>
                    <c:if test="${subscription.productBundle == null}">
                      <spring:message code="ui.label.na"/>
                    </c:if>
                  </div>
                </div>     
              </div>
                </c:when>
                <c:otherwise>
                <div class="<c:out value="grid_rows even"/>">                   
                <div class="grid_row_cell" style="padding:1px;width:60%;">              
                  <div class="row_celltitles"><spring:message code="label.recurring"></spring:message>&nbsp;:&nbsp;<spring:message code="ui.label.na"/></div>
                </div>               
                <div class="grid_row_cell last" style="padding:1px;">
                  <div class="row_celltitles">
                  </div>
                </div>     
              </div>
                </c:otherwise>
              </c:choose>
            </c:if>            
        </div>
        <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display: none;">
            <div class="rightpanel_mainloaderbox">
                <div class="rightpanel_mainloader_animatedicon">
                </div>
                <p>
                    <spring:message code="label.loading"/> &hellip;</p>
            </div>
          </div> 
        
          
              <div class="widget_browsergrid_wrapper fixed" id="entitlements" style="display:none">

                    <div class="widget_grid details header">
                      <div class="widget_grid_cell" style="padding:1px;width:30%;">
                        <span class="header"><spring:message code="usage.billing.view.subscription.item" /></span>
                      </div>
                       <div class="widget_grid_cell " style="padding:1px;width:20%;">
                        <span class="header"><spring:message code="usage.billing.view.subscription.unit.price.title" /></span>
                      </div>
                      <div class="widget_grid_cell" style="padding:1px;" >
                        <span class="header"><spring:message code="usage.billing.view.subscription.included.units" /></span>
                      </div>
                    </div>
                    <c:forEach  items="${subscription.effectiveEntitlements}" var="entitlement" varStatus="entitlementStatus">       
                    <c:choose>
                      <c:when test="${entitlementStatus.index % 2 == 0}">
                        <c:set var="rowClass" value="odd"/>
                      </c:when>
                      <c:otherwise>
                          <c:set var="rowClass" value="even"/>
                      </c:otherwise>
                    </c:choose> 
                      <c:set var="chargePrice" value="0"></c:set>
                      <div class="<c:out value="grid_rows ${rowClass}"/>">                   
	                      <div class="grid_row_cell" style="padding:1px;width:30%;">
	                        <div class="row_celltitles" title = "<c:out value="${entitlement.product.name}"/>" ><c:out value="${entitlement.product.name}"/></div>
	                      </div>
	                       <c:forEach items="${subscription.utilityCharges}" var="rcc" varStatus="rccStatus">
                          <c:if test="${entitlement.product.code == rcc.product.code }"> 
                            <c:set var="chargePrice" value="${rcc.price}"></c:set>
                          </c:if>
                          </c:forEach>
                    <div class="grid_row_cell " style="padding:1px;width:25%;">           
                        <div class="row_celltitles">
                        <c:out value="${subscription.tenant.currency.sign}" />
                        <fmt:formatNumber pattern="${currencyFormat}" minIntegerDigits="1"  minFractionDigits="${minFractionDigits}" value="${chargePrice}"  />
                        </div>
                      </div>    
                      <c:choose>
                        <c:when test="${entitlement.includedUnits < 0}">
                         <div class="grid_row_cell" style="padding:1px;">
                            <div class="row_celltitles"><spring:message code="label.entitlement.unlimited" /></div>
                          </div> 
                        </c:when>                  
                        <c:otherwise>                  
                          <div class="grid_row_cell" style="padding:1px;" >
                            <div class="row_celltitles"><c:out value="${entitlement.includedUnits}"/> <spring:message code="${entitlement.product.uom}"/></div>
                          </div> 
                        </c:otherwise>
                      </c:choose>

                  </div>
                    
                 </c:forEach>   
                 
               
                
              </div>

                <div class="widget_browsergrid_wrapper fixed" id="configurations" style="display:none;">
                  <div class="widget_grid details header">
                    <div class="widget_grid_cell" style="padding: 1px; width: 40%;">
                      <span class="header">
                        <spring:message code="label.name"/>
                      </span>
                    </div>                
                    <div class="widget_grid_cell" style="padding: 1px; width: 30%;">
                      <span class="header">
                        <spring:message code="label.value"/>
                      </span>
                    </div>
                  </div>

                  <c:forEach items="${subscription.configurationMap}" var="entry" varStatus="configurationCounter">
                    <c:choose>
                      <c:when test="${configurationCounter.count % 2 == 0}">
                        <c:set var="rowClass" value="odd"/>
                      </c:when>
                      <c:otherwise>
                        <c:set var="rowClass" value="even"/>
                      </c:otherwise>
                    </c:choose>

                    <div class="<c:out value="grid_rows ${rowClass}"/>">
                      <div class="grid_row_cell" style="padding: 1px;width: 40%;">
                        <div class="row_celltitles" style="padding: 1px;">
                          <spring:message code="${subscription.serviceInstance.service.serviceName}.ResourceType.${subscription.resourceType.resourceTypeName}.${entry.key}.name" var="key" />
                          <c:out value="${key}"></c:out>
                        </div>
                      </div>
                      <div class="grid_row_cell"  style="padding:1px;width: 58%;">
                        <div class="row_celltitles" title="<c:out value='${entry.value}'></c:out>" style="padding: 1px;">
                            <c:choose>
                                <c:when test="${entry.value ne null && entry.value!=''}">
                                    <c:out value="${entry.value}" />
                                </c:when>
                                <c:otherwise>
                                    -
                                </c:otherwise>
                            </c:choose>
                          
                        </div>
                      </div>
                    </div>
                  </c:forEach>
                </div>

                <div class="widget_browsergrid_wrapper fixed" id="resource_details" style="display:none;">
                  <div class="widget_grid details header">
                    <div class="widget_grid_cell" style="padding: 1px; width: 40%;">
                      <span class="header">
                        <spring:message code="label.name"/>
                      </span>
                    </div>                
                    <div class="widget_grid_cell" style="padding: 1px; width: 30%;">
                      <span class="header">
                        <spring:message code="label.value"/>
                      </span>
                    </div>
                  </div>

                  <div class="<c:out value="grid_rows even"/>">
                    <div class="grid_row_cell" style="padding: 1px;width: 40%;">
                      <div class="row_celltitles" style="padding: 1px;">
                        <spring:message code="label.resource.type"/>
                      </div>
                    </div>
                    <div class="grid_row_cell"  style="padding:1px;width: 30%;">
                      <div class="row_celltitles" style="padding: 1px;">
                        <spring:message code="${subscription.serviceInstance.service.serviceName}.ResourceType.${subscription.resourceType.resourceTypeName}.name"/>
                      </div>
                    </div>
                  </div>

                  <div class="<c:out value="grid_rows odd"/>">
                    <div class="grid_row_cell" style="padding: 1px;width: 40%;">
                      <div class="row_celltitles" style="padding: 1px;">
                        <spring:message code="label.resource.handle"/>
                      </div>
                    </div>
                    <div class="grid_row_cell"  style="padding:1px;width: 30%;">
                      <div class="row_celltitles" title="<c:out value='${subscription.handle.resourceHandle}'></c:out>" style="padding: 1px;">
                        <c:out value="${subscription.handle.resourceHandle}"></c:out>
                      </div>
                    </div>
                  </div>

                  <div class="<c:out value="grid_rows even"/>">
                    <div class="grid_row_cell" style="padding: 1px;width: 40%;">
                      <div class="row_celltitles" style="padding: 1px;">
                        <spring:message code="ui.label.serviceinstance"/>
                      </div>
                    </div>
                    <div class="grid_row_cell"  style="padding:1px;width: 30%;">
                      <div class="row_celltitles" style="padding: 1px;">
                        <c:out value="${subscription.serviceInstance.name}"></c:out>
                      </div>
                    </div>
                  </div>
                  
                </div>
    </div>
</div>
<input type="hidden" id="current_subscription_param"  value="<c:out value="${subscription.param}"/>"/>
<c:if test="${!(toProvision and subscription.productBundle.resourceType != null)}">
	<input type="hidden" id="vmId"  value="<c:out value="${vmId}"/>"/>
	<input type="hidden" id="vmDisplayName"  value="<c:out value="${vmDisplayName}"/>"/>
	<input type="hidden" id="vmGroupName"  value="<c:out value="${vmGroupName}"/>"/>
</c:if>

<div class="workflow_details_popup" title="<spring:message code="dialogue.title.workflow" />" style="display:none"></div>
