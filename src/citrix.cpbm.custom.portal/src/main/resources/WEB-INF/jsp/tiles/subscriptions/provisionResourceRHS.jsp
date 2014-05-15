<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles"%>

<div class="widgetcatalog_contentarea rightside" id="provision_page_RHS_div">
  <div class="biggerback_container">
    <span id="back_to_catalog" class="title_listall_arrow biggerback" style="float: right;">
      <c:choose>
      <c:when test="${not empty subscription && subscription.activeHandle == null}">
        <spring:message code="label.subscribe.go.to.subscriptions" />
      </c:when>
      <c:otherwise>
        <spring:message code="label.subscribe.backtocatalog" />
      </c:otherwise>
      </c:choose>
    </span>
  </div>
  <div style="width: 254px; float: left;" class="filterlistbox">
    <div class="bs_selectbox title user">
      <h2>
        <spring:message code="label.catalog.Your.Selection" />
      </h2>
    </div>
    <div class="widgetcatalog_contentarea sectionbox bigborders catalog_rightmenu"
      style="height: auto; max-height:300px; overflow-y: auto">
      <div id="RHS_your_selection_contentArea"></div>
    </div>
  </div>

  <div id="one_time_charges_content_area" style="width: 254px; float: left;" class="filterlistbox">
    <div class="bs_selectbox title user">
      <h2>
        <spring:message code="label.one.time" />
        <spring:message code="label.charges" />
      </h2>
    </div>
    <div class="widgetcatalog_contentarea sectionbox bigborders catalog_rightmenu">
      <span class="title" style="width: 60%;"><spring:message
          code="ui.task.transaction.type.subscriptionActivation" /></span> <span class="description price"
        id="one_time_charges"></span><br />
    </div>
  </div>

  <div id="recurring_charges_content_area" style="width: 254px; float: left;" class="filterlistbox">
    <div class="bs_selectbox title user">
      <h2>
        <spring:message code="label.recurring" />
        <spring:message code="label.charges" />
      </h2>
    </div>
    <div class="widgetcatalog_contentarea sectionbox bigborders catalog_rightmenu">
      <span id="pricing_reccurence_frequency" class="title" style="width: 60%;"></span> <span class="description price"
        id="recurring_charges"></span><br />
    </div>
  </div>

  <div style="width: 254px; float: left;" class="filterlistbox">
    <div class="bs_selectbox title user">
      <h2>
        <spring:message code="label.usage.billing.utility.charges" />
      </h2>
    </div>
    <div class="widgetcatalog_contentarea sectionbox bigborders catalog_rightmenu" style="min-height:100px;max-height:400px;overflow-y:auto;overflow-x:hidden;">
      <div>
        <p class="title" id="msg_RHS_entitlements" style="text-align: justify; text-justify: inter-word; width: 90%;">
          <spring:message code="message.create.subscription.utility.rates.above.entitlements" />
          :
        </p>
        <ul id="included_usage" class="pull-left"></ul>
      </div>
      <div>
        <p class="title" id="msg_RHS_generated_usage" style="text-align: justify; text-justify: inter-word; width: 90%;">
          <spring:message code="message.create.subscription.generated.usage.charges" />
          :
        </p>
        <div>
          <ul id="generated_products_div" style="margin-left: 0px;"></ul>
        </div>
      </div>
      <div>
        <p class="title" id="msg_RHS_rate_card_table" style="text-align: justify; text-justify: inter-word; width: 90%;">
          <spring:message code="message.view.rate.card.table" htmlEscape="false" />
        </p>
      </div>
    </div>
  </div>

  <div style="width: 254px; float: left; margin-bottom:7px;" class="filterlistbox">
    <div class="bs_selectbox title user">
      <div class="launchvm_cart_termsnconditionbox titlearea">
        <spring:message code="label.subscribe.notice.title" />
      </div>
    </div>
    <div class="widgetcatalog_contentarea sectionbox bigborders catalog_rightmenu">
      <div>
        <span style="margin: 10px 0px 0px 12px; width: 225px; float: left;"> <tiles:insertDefinition
            name="tnc.virtual.machine" />
        </span>
      </div>
    </div>
  </div>
  <div class="subscribe_tnc_box">
    <span class="checkbox_container"><input class="checkbox" type="checkbox" id="accept_checkbox" /></span> 
    <span class="checkbox_description" ><spring:message code="label.subscribe.notice.terms" htmlEscape="false"/><img src="../../images/open_new_window.png" id="tncLink" alt="<spring:message code='terms.and.conditions' />" class="new_window" ></span>
  </div>

  <div id="subtotal_content_area" class="launchvm_sub-totalbox">
    <span class="sublabel"><spring:message code="label.subscribe.summary.subtotal" /></span>&nbsp;&nbsp;&nbsp;<span
      class="subamount" id="sub_total_amount"></span>
  </div>
  <div id="tax_content_area" class="launchvm_sub-totalbox">
    <span class="sublabel"><spring:message code="label.subscribe.summary.tax" /></span>&nbsp;&nbsp;&nbsp;<span
      class="subamount" id="subscription_tax_amount"></span>
  </div>
  <div id="total_content_area" class="launchvm_sub-totalbox total">
    <span class="label"><spring:message code="label.subscribe.summary.total" /></span>&nbsp;&nbsp;&nbsp;<span
      class="amount" id="subscription_total_amount"></span>
  </div>

  <div class="widgetcatalog_contentarea sectionbox" style="border: none; background: none; margin-top: 10px;">
    <c:choose>
      <c:when test="${isReconfigure || isReprovision || resourceType == serviceBundleResourceType || isPayAsYouGoChosen}">
        <div class="btn-group">
          <button class="btn btn-info" style="width: 240px;" id="launchResource">
              <c:if test="${isReprovision || isPayAsYouGoChosen}">
                <spring:message code="ui.label.provision" />
              </c:if>
              <c:if test="${isReconfigure}">
                <spring:message code="ui.label.subscription.Reconfigure" />
              </c:if>
              <c:if test="${resourceType == serviceBundleResourceType}">
                <spring:message code="label.subscribe.confirm" />
              </c:if>
          </button>
        </div>
      </c:when>
      <c:otherwise>
        <div class="btn-group">
          <button class="btn btn-info" style="width:210px;" data-hover="dropdown" data-delay="1000" data-close-others="false" id="launchResourcePrimary"><spring:message code="label.subscribe.summary.subscribe" /></button>
          <button class="btn btn-info dropdown-toggle" data-hover="dropdown" data-delay="1000" data-close-others="false" data-toggle="dropdown" style="padding-bottom:12px;padding-top:4px;">
          <span class="caret"></span>
          </button>
          <ul class="dropdown-menu dropdown-menu-blue" style="z-index:10000;width:235px;">
            <li><a href="javascript:void(0);" id="launchResourcePrimaryMenu"><spring:message code="label.subscribe.summary.subscribe.provision" /></a></li>
            <li><a href="javascript:void(0);" id="launchResourceSecondaryMenu"><spring:message code="label.subscribe.summary.subscribe.provision.later" /></a></li>
          </ul>
        </div>
      </c:otherwise>
     </c:choose>
  </div>
</div>

<!-- Template to clone RHS section 1 from -->
<div id="RHS_your_selection" style="display: none;">
  <span id="RHS_your_selection_header" class="title vertical"></span><span class="description vertical"
    id="RHS_your_selection_value"></span>
</div>