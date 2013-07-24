<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles"%>


<script language="javascript">
var dictionary = {    userNoBilling: '<spring:message javaScriptEscape="true" code="message.user.no.billing"/>',  
    noThanks: '<spring:message javaScriptEscape="true" code="label.no.thanks"/>',
    selectKey: '<spring:message javaScriptEscape="true" code="select.key"/>', 
    addSSHKey: '<spring:message javaScriptEscape="true" code="add.SSH.key"/>',  
    publicKeyShouldBeginWith: '<spring:message javaScriptEscape="true" code="public.key.should.begin.with"/>',
    keyNameIsRequired: '<spring:message javaScriptEscape="true" code="key.name.is.required"/>',
    keyNameMustBeUnique: '<spring:message javaScriptEscape="true" code="key.name.must.be.unique"/>',
    publicKeyIsRequiredWhenUploadingSSHKey: '<spring:message javaScriptEscape="true" code="public.key.is.required.when.uploading.SSH.key"/>',    
    failedToGenerateSSHKey: '<spring:message javaScriptEscape="true" code="failed.to.generate.SSH.key"/>',
    uploadSSHKeySucceeded: '<spring:message javaScriptEscape="true" code="upload.SSH.key.succeeded"/>',
    failedToUploadSSHKey: '<spring:message javaScriptEscape="true" code="failed.to.upload.SSH.key"/>',
    ratePerGBHour: '<spring:message javaScriptEscape="true" code="rate.per.GB.Hour"/>',
    ratePerGBMonth: '<spring:message javaScriptEscape="true" code="rate.per.GB.Month"/>',
    featuredTemplates: '<spring:message javaScriptEscape="true" code="label.featured"/>',
    myTemplates: '<spring:message javaScriptEscape="true" code="my.templates"/>',
    communityTemplates: '<spring:message javaScriptEscape="true" code="community.templates"/>',
    ISOs: '<spring:message javaScriptEscape="true" code="ISOs"/>',
    rootVolume: '<spring:message javaScriptEscape="true" code="root.volume"/>', 
    allChargeTypes: '<spring:message javaScriptEscape="true" code="all.charge.types"/>',  
    hourly: '<spring:message javaScriptEscape="true" code="label.hourly"/>',
    vmName: '<spring:message javaScriptEscape="true" code="label.VM.name"/>', 
    vmDisplayName: '<spring:message javaScriptEscape="true" code="vm.display.name"/>',  
    password: '<spring:message javaScriptEscape="true" code="label.password"/>',  
    diskSizeIsRequired: '<spring:message javaScriptEscape="true" code="disk.size.is.required"/>',
    diskSizeShouldBeNumeric: '<spring:message javaScriptEscape="true" code="disk.size.should.be.numeric"/>',  
    onlyAlphanumericCharactersAreAllowed: '<spring:message javaScriptEscape="true" code="only.alphanumeric.characters.are.allowed"/>' ,
    customDiskSizeShouldBeLessThan: '<spring:message javaScriptEscape="true" code="error.custom.disk.size"/>',
    noPrimaryNetwork: '<spring:message javaScriptEscape="true" code="no.primary.network"/>',
    networkCreationFailed: '<spring:message javaScriptEscape="true" code="primary.network.creation.failed"/>',
    noValidNetworkOfferingFound: '<spring:message javaScriptEscape="true" code="no.valid.network.offering.found"/>',
    noGuestNetworksAvailable: '<spring:message javaScriptEscape="true" code="no.guest.networks.available.please.contact.support"/>',
    NoOfVMLaunchedBasedonaboveselectionstext: '<spring:message javaScriptEscape="true" code="virtual.machines.will.be.launched.based.on.the.above.selections"/>',
    hoursChargeType: '<spring:message javaScriptEscape="true" code="launchvm.chargetype.text.hours"/>',
    usageBasedChargeType: '<spring:message javaScriptEscape="true" code="launchvm.chargetype.text.usagebase"/>',
    oneTimeChargeType: '<spring:message javaScriptEscape="true" code="launchvm.chargetype.text.onetime"/>',
    annualChargeType: '<spring:message javaScriptEscape="true" code="label.subscribe.year"/>',
    hourlyChargeType: '<spring:message javaScriptEscape="true" code="label.subscribe.hour"/>',
    monthlyChargeType: '<spring:message javaScriptEscape="true" code="label.subscribe.month"/>', 
    quarterlyChargeType: '<spring:message javaScriptEscape="true" code="label.subscribe.quarter"/>',
    category: '<spring:message javaScriptEscape="true" code="label.subscription.category"/>',
    hypervisor: '<spring:message javaScriptEscape="true" code="label.hypervisor"/>',
    OperatingSystem: '<spring:message javaScriptEscape="true" code="label.subscription.operating.system"/>',
    unlimited:'<spring:message javaScriptEscape="true" code="unlimited"/>',
    more:'<spring:message javaScriptEscape="true" code="label.more"/>',
    subscribeSoftwareInfo:'<spring:message javaScriptEscape="true" code="message.subscribe.os.info"/>',
    subscribeSoftwareInfoLimitedEntitlement:'<spring:message javaScriptEscape="true" code="message.subscribe.os.info.with.limited.entitlement"/>',
    subscribeSoftwareInfoUnlimitedEntitlement:'<spring:message javaScriptEscape="true" code="message.subscribe.os.info.with.unlimited.entitlement"/>',
    subscribeComputeInfo:'<spring:message javaScriptEscape="true" code="message.subscribe.compute.info"/>',
    vcpu:'<spring:message javaScriptEscape="true" code="label.subscribe.vcpu"/>',
    vram:'<spring:message javaScriptEscape="true" code="label.subscribe.vram"/>',
    diskSelectionWarning:'<spring:message javaScriptEscape="true" code="message.subscribe.select.disk.warning"/>',
    createNetworkError:'<spring:message javaScriptEscape="true" code="message.subscribe.create.network.error"/>',
    templateSelectionWarning:'<spring:message javaScriptEscape="true" code="message.subscribe.template.selection.warning"/>',
    goToMyResources:'<spring:message javaScriptEscape="true" code="go.to.my.resources"/>',
    goToSubscriptions:'<spring:message javaScriptEscape="true" code="label.go.to.subscriptions"/>',
    subscribingTo:'<spring:message javaScriptEscape="true" code="label.subscribing.to"/>',
    goToCatalog:'<spring:message javaScriptEscape="true" code="go.to.catalog"/>',
    subscriptionSuccess:'<spring:message javaScriptEscape="true" code="message.subscription.creation.success"/>',
    subscriptionFailure:'<spring:message javaScriptEscape="true" code="message.subscription.creation.failure"/>',
    goToDashboard:'<spring:message javaScriptEscape="true" code="go.to.dashboard"/>',
    of:'<spring:message javaScriptEscape="true" code="label.subscribe.select.of"/>',
    addVolumeInfoLine1withEntitlement:'<spring:message javaScriptEscape="true" code="message.subscribe.add.disk.line1.withEntitlement"/>',
    addVolumeInfoLine1withoutEntitlement:'<spring:message javaScriptEscape="true" code="message.subscribe.add.disk.line1.withoutEntitlement"/>',
    addVolumeInfoLine2:'<spring:message javaScriptEscape="true" code="message.subscribe.add.disk.line2"/>',
    noSoftwareSelected:'<spring:message javaScriptEscape="true" code="message.subscribe.select.os"/>',
    goToSubscriptions:'<spring:message javaScriptEscape="true" code="label.subscribe.go.to.subscriptions"/>',
    activationCharges:'<spring:message javaScriptEscape="true" code="label.subscribe.summary.activation.charges"/>',
    all:'<spring:message javaScriptEscape="true" code="label.all"/>',
    badVmNameWarning:'<spring:message javaScriptEscape="true" code="js.errors.launchVm.badName"/>',
    label_Any:'<spring:message javaScriptEscape="true" code="label.Any"/>',
    label_Bundle:'<spring:message javaScriptEscape="true" code="label.bundle"/>',
    label_None:'<spring:message javaScriptEscape="true" code="label.none"/>',
    error_Group_Not_Satisfied:'<spring:message javaScriptEscape="true" code="error.Resource.Provision.Group.Not.Satisfied"/>',
    msg_utility_charges_applicable:'<spring:message javaScriptEscape="true" code="message.create.subscription.utility.rates.above.entitlements"/>',
    view_bundles:'<spring:message javaScriptEscape="true" code="label.subscribe.view.bundles"/>',
    label_charged_at:'<spring:message javaScriptEscape="true" code="label.charged.at"/>',
    msg_utility_rates:'<spring:message javaScriptEscape="true" code="message.view.rate.card.table"/>',
    msg_no_extra_charge_upto:'<spring:message javaScriptEscape="true" code="label.no.extra.charge.up.to"/>',
    msg_overages_charged_at:'<spring:message javaScriptEscape="true" code="label.overages.charged.at"/>',
    msg_overages_charges:'<spring:message javaScriptEscape="true" code="label.overages.charges"/>',
    label_Not_Applicable:'<spring:message javaScriptEscape="true" code="label.Not.Applicable"/>',
    label_at:'<spring:message javaScriptEscape="true" code="label.at"/>'
};
</script>
<style>
.widgetcatalog_cataloglist.sections.bundles {
  border-right:none;

}
.widgetcatalog_cataloglist.sections {
  border-right:none;
}
.catalog_filterpanel{
   width:auto;
}
.catalog_listpanel{
   width:auto;
}
.widgetcatalog_cataloglist{
 width: 650px;
}
.widgetcatalog_cataloglistpanel{
  width: 650px; 
  float:left;
  margin:10px 0px 10px 0px;
}
.left_filtermenu{
  margin-top:10px;
}
.widgetcatalog_filterpanel{
  width:auto;
}
.no-listscreen_wrapper {
    margin: 20px 0 0;
    padding: 0;
    width: auto;
}
</style>

              
                   <%-- <div class="widgetcatalog_filterpanel" style="margin-top:5px;<c:if test='${provision}'>display:None;</c:if>">
                        <div class="left_filtermenu" id="pricing">
								            <span class="title"><spring:message code="label.subscribe.pricing"/></span>
                              <a href="javascript:void(0);" style="float: right;" class="utility_rates_link" ><spring:message code="page.level2.utilityrates"/></a>
								              <c:if test="${resourceType!=serviceBundleResourceType}">
								              <div class="widgetcatalog_contentarea sectionbox bigborders leftmenu">
                                  <span class="icon active_subscription"></span>
                                    <span class="title leftmenu" style="width:75%;"><spring:message code="label.subscription.view.active" htmlEscape="false" /></span>
                              </div>
                              </c:if>
								         </div>
                    </div> --%>
                      
                    <div id="bundle_with_selection_summary_div">
                    <c:if test="${not empty customComponentSelector}">
                    <div class="widgetcatalog_cataloglistpanel" id="componentselector-minified">
                        <c:if test="${resourceType ne serviceBundleResourceType}">
                            <div class="selected_filters_box">
                              <div class="selectedbox" id="componentselector-minified-components">
                                <span class="title"><spring:message code="label.catalog.Your.Selection"/>:</span>
                              </div>
                            </div>
                        </c:if>
                    </div>
                    </c:if>
                    
                    <div id="no_screen_wrapper_div" class="no-listscreen_wrapper" style="display:None;">
                     <div class="no-listbox">
                        <div class="widgetcatalog_bundlesbox blank"></div>
                        <div class="no-listbox textbox">
                          <h2><spring:message code="label.bundle.no.product.bundle" /></h2>
                          <p><spring:message code="label.bundle.contact.administrator" /></p>
                        </div>
                      </div>
                </div>
                    <div id="bundle_container">
                      <div class="widgetcatalog_cataloglist" id="bundle_template" style="display:none;">
                          <div class="widgetcatalog_cataloglist sections bundles">
                              <div id="entitlmentsribbon" class="widgetcatalog_bundlesbox">
                                <div class="widgetcatalog_bundlesbox iconbox">
                                  <span class="compute"></span>
                                </div>
                                
                                <div id="offering" class="widgetcatalog_bundlesbox offerings">
                                </div>
                              </div>
                                <h3 class = "ellipsis" id="name"></h3>
                                <ul id="entitlements">
                                </ul>
                                <div  id="totalentitlmentsdiv" title='<spring:message code="label.bundle.list.entitlement.title"/>' style="display: none; overflow: hidden;">
                                <div class="dialog_formcontent_entitlement_selectedbox">
                                  <div id="totalentitlmentsribbon" class="widgetcatalog_bundlesbox" style="margin-top:0;">
                                        <div class="widgetcatalog_bundlesbox iconbox">
                                            <span class="compute"></span>
                                        </div>
                                        
                                        <div id="total_offering" class="widgetcatalog_bundlesbox offerings">
                                        </div>
                                      </div>
                                      
                                      <h2 id="bundle_name"></h2>
                                      <p id="bundle_description" style="width: 255px; margin-top: 5px;"></p>
                                </div>
                                
                                <div class="dialog_formcontent entitlementlightbox">
                                    <div class="details_lightboxformbox">
                                         <ul id="totalentitlments"> 
                                        </ul>
                                     </div>
                                    </div>
                                   </div>
                            </div>
                        
                        <div class="widgetcatalog_cataloglist sections subscription">
                              <div class="widgetcatalog_cataloglist sections subscription contentarea">
                                  <h4><spring:message code="label.subscribe.price.message"/>:</h4>
                                    <div class="widgetcatalog_cataloglist_rateswrapper">
                                      <div class="widgetcatalog_cataloglist_ratesbox">
                                        <span class="currency" id="currencySign"></span><span class="rate" id="recurringCharges"></span><span class="pertime" id="recurrenceType"></span>
                                        </div>
                                    </div>
                                     <span class="charges" id="activationCharges"></span>
                                    <div class="configure_subscribe_button">
                                        <a class="subscribebutton" id="subscribe" href="#">
                                       <c:choose>
                                          <c:when test="${buttonMessage}">
                                            <spring:message code="label.subscribe.login.subscribe"/>
                                          </c:when>
                                          <c:otherwise>
                                            <c:choose>
                                              <c:when test="${provision}">
                                                <spring:message code="label.configure"/> &amp; <spring:message code="ui.label.provision"/>
                                              </c:when>
                                              <c:otherwise>
                                                <spring:message code="label.configure"/> &amp; <spring:message code="label.subscribe.confirm"/>
                                              </c:otherwise>
                                            </c:choose>
                                          </c:otherwise>
                                       </c:choose>
                                       </a>
                                       </div>
                                </div>
                            </div>
                    </div>
                </div>
                </div>
                <div class="infinite_scrollbarbox" style="display:none; cursor: pointer;">
	               	<p id="show_more"> <spring:message code="label.show.more"/> &hellip;</p>
                    <!--for loading-->
                	<div class="infinite_loading" style="display:none;"></div>
                </div>
              

<!-- lightbox (begin) -->
<div id="utilityrates_lightbox" title='<spring:message code="view.utility.rates"/>' style="display:none;">
  <p> 
  </p>
  <div class="dialog_formcontent">  
    <div class="utilityrates_container" id="container">
    </div>
  </div>
</div>
<!-- lightbox (end) -->

<div id="spinning_wheel" style="display:none;">
     <div class="widget_blackoverlay widget_full_page">
     </div>
     <div class="widget_loadingbox fullpage">
       <div class="widget_loaderbox">
         <span class="bigloader"></span>
       </div>
       <div class="widget_loadertext">
         <p id="in_process_text"><spring:message code="label.loading"/> &hellip;</p>
       </div>
     </div>
</div>
