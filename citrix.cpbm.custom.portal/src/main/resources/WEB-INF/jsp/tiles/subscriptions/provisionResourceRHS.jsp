<%-- Copyright (C) 2013 Citrix Systems, Inc. All rights reserved --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles"%>

<div class="widgetcatalog_contentarea rightside" id="provision_page_RHS_div">
<div class="biggerback_container">
<span id="back_to_catalog" class="title_listall_arrow biggerback" style="float:right;"><spring:message code="label.subscribe.backtocatalog"/></span>
</div>
  <div style="width:254px;float:left;" class="filterlistbox">
    <div class="bs_selectbox title user"><h2><spring:message code="label.catalog.Your.Selection"/></h2></div>
    <div class="widgetcatalog_contentarea sectionbox bigborders catalog_rightmenu" style="height:300px; overflow-y:auto">
      <div id="RHS_your_selection_contentArea"></div>
    </div>
  </div>

  <div id="one_time_charges_content_area" style="width:254px;float:left;" class="filterlistbox">
    <div class="bs_selectbox title user"><h2><spring:message code="label.one.time"/> <spring:message code="label.charges"/></h2></div>         
    <div class="widgetcatalog_contentarea sectionbox bigborders catalog_rightmenu" >
      <span class="title" style="width:60%;"><spring:message code="ui.task.transaction.type.subscriptionActivation"/></span>
      <span class="description price" id="one_time_charges"></span><br />
    </div>
  </div>
   
  <div id="recurring_charges_content_area" style="width:254px;float:left;" class="filterlistbox">
    <div class="bs_selectbox title user"><h2><spring:message code="label.recurring"/> <spring:message code="label.charges"/></h2></div>
    <div class="widgetcatalog_contentarea sectionbox bigborders catalog_rightmenu" >
      <span id="pricing_reccurence_frequency" class="title" style="width:60%;"></span>
      <span class="description price" id="recurring_charges"></span><br />
    </div>
  </div>
  
  <div style="width:254px;float:left;" class="filterlistbox"> 
    <div class="bs_selectbox title user"><h2><spring:message code="label.usage.billing.utility.charges"/></h2></div>
    <div class="widgetcatalog_contentarea sectionbox bigborders catalog_rightmenu">
      <div>
        <p class="title" id="msg_RHS_entitlements" style="text-align:justify; text-justify:inter-word; width:90%;"><spring:message javaScriptEscape="true" code="message.create.subscription.utility.rates.above.entitlements"/>:</p>
        <ul id="included_usage" class="pull-left"></ul>
      </div>
      <div>
        <p class="title" id="msg_RHS_generated_usage" style="text-align:justify; text-justify:inter-word; width:90%;"><spring:message javaScriptEscape="true" code="message.create.subscription.generated.usage.charges"/>:</p>
        <div><ul id="generated_products_div" style="margin-left:0px;"></ul></div>
      </div>
      <div>
        <p class="title" id="msg_RHS_rate_card_table" style="text-align:justify; text-justify:inter-word; width:90%;"><spring:message code="message.view.rate.card.table" htmlEscape="false"/></p>
      </div>
    </div>
  </div>
    
  <div style="width:254px;float:left;" class="filterlistbox">
    <div class="bs_selectbox title user"><div class="launchvm_cart_termsnconditionbox titlearea"><spring:message code="label.subscribe.notice.title" /></div></div>
    <div class="widgetcatalog_contentarea sectionbox bigborders catalog_rightmenu" >
      <div>
        <span style="margin:10px 0px 0px 12px;width:225px;float:left;">
          <tiles:insertDefinition name="tnc.virtual.machine" />
        </span>
      </div>
    </div>
  </div>
  <input class="checkbox" type="checkbox" id="accept_checkbox"/>
  <span class="tnctext" id="tncLink"><spring:message code="label.subscribe.notice.terms" htmlEscape="false"/></span>
   
  <div id="subtotal_content_area" class="launchvm_sub-totalbox"><span class="sublabel"><spring:message code="label.subscribe.summary.subtotal" /></span>&nbsp;&nbsp;&nbsp;<span class="subamount" id="sub_total_amount"></span></div>
  <div id="tax_content_area" class="launchvm_sub-totalbox"><span class="sublabel"><spring:message code="label.subscribe.summary.tax" /></span>&nbsp;&nbsp;&nbsp;<span class="subamount" id="subscription_tax_amount"></span></div>
  <div id="total_content_area" class="launchvm_sub-totalbox total"><span class="label"><spring:message code="label.subscribe.summary.total" /></span>&nbsp;&nbsp;&nbsp;<span class="amount" id="subscription_total_amount"></span></div>
   
  <div class="widgetcatalog_contentarea sectionbox" style="border:none; background:none;margin-top:10px;">
    <c:if test="${not provision}">
      <div id="provision_acceptBoxAndText"><input type="checkbox" class="checkbox" id="provision_accept" checked="true"/><text style="font-size:14px">&nbsp;&nbsp;<spring:message code="ui.label.provision.resource"/></text></div>
    </c:if>
    <a class="launchvm_button" id="launchvm_button" href="javascript:void(0);">
      <c:choose>
        <c:when test="${provision}">
          <spring:message code="ui.label.provision.resource"/>
        </c:when>
        <c:otherwise>
          <spring:message code="label.subscribe.summary.subscribe" />
        </c:otherwise>
      </c:choose>
    </a>
  </div>
</div>

<!-- Template to clone RHS section 1 from -->
<div id="RHS_your_selection" style="display:none;">
  <span id="RHS_your_selection_header" class="title vertical"></span><span class="description vertical" id="RHS_your_selection_value"></span>
</div>