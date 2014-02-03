<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/channels.js"></script>
<jsp:include page="/WEB-INF/jsp/tiles/shared/js_messages.jsp"></jsp:include>
<script type="text/javascript">
    var planned_word = '<spring:message javaScriptEscape="true" code="label.next.planned.version"/>';
    var invalid_date_msg = '<spring:message javaScriptEscape="true" code="ui.datepicker.date.value.invalid"/>';
    var date_before_or_today_msg = '<spring:message javaScriptEscape="true" code="ui.datepicker.date.value.before.or.today"/>';
    var dialogProceed = '<spring:message javaScriptEscape="true" code="label.proceed"/>';
    var notYetSet= '<spring:message javaScriptEscape="true" code="ui.label.plan.date.not.yet.set"/>';
    var channelServiceSettingDialogTitle= '<spring:message javaScriptEscape="true" code="ui.label.channel.service.settings.dialog.title"/>';

   // Unbind the previous bindings with element clicks before binding the same. The problem is like ::
   // JQuery evals the script code and puts that in an Eval Array (in firebug it's
   // located at jquery-1.7.1.min.js/eval/seq/array number).
   // So the js is never unloaded from the eval tab and if we have some live events in the 
   // page, the code is executed as many times as the pages loaded.
   $("#details_tab").unbind("click").bind("click", function (event) {
      $('#details_tab').removeClass('nonactive').addClass("active");
      $('#currencies_tab').removeClass('active').addClass("nonactive");
      $('#catalog_tab').removeClass('active').addClass("nonactive");
          
      $('#details_content').show();
  
      $('#currencies_content').hide();
      $('#catalog_links').hide();
      $("#catalog_content").hide();
      $("#second_line_under_planned").hide();
      $('#action_currency').hide();
      $("#main_action_box").attr("style", "height: 30px");
      $('#service_controls_tab').removeClass('active').addClass("nonactive");
      $('#action_service_settings').hide();
      $('#service_controls_content').hide();
   });

   $("#currencies_tab").unbind("click").bind("click", function (event) {
       $('#details_tab').removeClass('active').addClass("nonactive");
       $('#currencies_tab').removeClass('nonactive').addClass("active"); 
       $('#catalog_tab').removeClass('active').addClass("nonactive");
  
       $('#details_content').hide();
       $('#currencies_content').show();
       $('#action_currency').show();
       $("#catalog_content").hide();
       $('#catalog_links').hide();
       $("#second_line_under_planned").hide();
       $("#main_action_box").attr("style", "height: 30px");
       $('#service_controls_tab').removeClass('active').addClass("nonactive");
       $('#action_service_settings').hide();
       $('#service_controls_content').hide();
   });

   $("#catalog_tab").unbind("click").bind("click", function (event) {
     preViewCatalogPlanned(null, $("#planned_or_not"));
     });

   $("#action_currency").unbind("mouseover").bind("mouseover", function (event) {
       $('#currency_action_menu').show();
     });

   $("#action_currency").unbind("mouseout").bind("mouseout", function (event) {
       $('#currency_action_menu').hide();
     });
  
   function preViewCatalogPlanned(event){
       $('#details_tab').removeClass('active').addClass("nonactive");
       $('#currencies_tab').removeClass('active').addClass("nonactive");
       $('#catalog_tab').removeClass('nonactive').addClass("active");
       $('#details_content').hide();
       $('#currencies_content').hide();

       $('#catalog_links').show();
       $("#catalog_content").show();
       $('#action_currency').hide();
       $('#plan_date_picker').show();

       $("#catalog_current_tab").find("a").attr("style", "color: #2C8BBC");
       $("#catalog_planned_tab").find("a").attr("style", "color: #000");
       $("#catalog_history_tab").find("a").attr("style", "color: #2C8BBC");
       $("#second_line_under_planned").closest(".widget_details_actionbox").attr("style", "height: auto");
       $("#second_line_under_planned").show();
       viewCatalogPlanned();
       $('#service_controls_tab').removeClass('active').addClass("nonactive");
       $('#action_service_settings').hide();
       $('#service_controls_content').hide();
     }

   function popup_date_picker(event){
     var channelId = $("li[id^='channel'].selected.channels").attr('id').substr(7);
     initDialog("plan_date_div", 500);
     
     var $thisDialog = $("#plan_date_div");
     $thisDialog.data("height.dialog", 210);
     $thisDialog.bind('dialogclose', function(event) {
       $("#ui-datepicker-div").hide();
     });
     $.ajax( {
         type : "GET",
         url : "/portal/portal/channels/showdatepicker",
         data:{channelId: channelId},
         cache: false,
         async: false,
         dataType : "html",
         success : function(html) {
             $thisDialog.html(html);
             $thisDialog.dialog('option', 'buttons', {
                   "Proceed": function () {
                        // Start the backend call
                        var prevValue = $thisDialog.find("#planstartDate").attr("prevvalue");
                        var nowVal = $thisDialog.find("#planstartDate").attr("value");
                        var format = $thisDialog.find("#planstartDate").attr("dateformat");
                        // Checks if the date entered is of form M/D/YYYY or MM/DD/YYYY or M/DD/YYYY or MM/D/YYYY
                        if(!/^\s*\d{1,2}\/\d{1,2}\/\d{4}\s*$/.test(nowVal)){
                          popUpDialogForAlerts("alert_dialog", invalid_date_msg);
                          return;
                        }
                        // Check if the date entered, which is done manually, obviously, is earleir than today
                        var now = Date.parse($("#date_today").val());                       
                        var dateSplt = nowVal.split("/");
                        var newDate = new Date(dateSplt[2], parseInt(dateSplt[0]-1), dateSplt[1]);
                        var isTodayAllowed = $("#isTodayAllowed").val();
                        if((now == newDate.getTime() && isTodayAllowed =="false") ||
                            (now > newDate.getTime())){
                          popUpDialogForAlerts("alert_dialog", date_before_or_today_msg);
                          return;
                        }

                        $.ajax( {
                              type : "POST",
                              url : "/portal/portal/channels/changeplandate",
                              data: {"channelId": channelId,
                                     "newDate": nowVal,
                                     "dateFormat": format
                                     },
                              dataType : "html",
                              success : function(html) {
                                  //$thisDialog.find("#planstartDate").attr("prevvalue", nowVal);
                                  $thisDialog.find("#planstartDate").attr("value", nowVal);
                                  $("#second_line_under_planned").closest(".widget_details_actionbox").attr("style", "height: 45px");
                                  $("#second_line_under_planned").show();
                                  $("#planned_or_not").html(planned_word);
                                  $("#planned_or_not").attr("planned", "1");
                                  if(now < newDate.getTime()){
                                    var formatted_nowVal = dateFormat(nowVal, g_dictionary.dateonlyFormat, false);
                                    $("#effective_date").text(formatted_nowVal);
                                  } else {
                                    $("#effective_date").text('<spring:message javaScriptEscape="true" code="ui.label.plan.date.not.yet.set"/>');
                                  }
                                  if(isTodayAllowed == "true"){
                                     $("li[id^='channel'].selected.channels").click(); 
                                     $("#catalog_tab").click();
                                  }
                                  },
                              error: function(XMLHttpRequest){
                                 //TODO
                              }
                            });
                      $(this).dialog("close");
                      $(this).dialog("destroy");
                     },
                     "Cancel": function () {
                       
                       $thisDialog.dialog("close");
                       $(this).dialog("destroy");
                         }
                   });
                   dialogButtonsLocalizer($thisDialog, {'Proceed': dialogProceed, 'Cancel': g_dictionary.dialogCancel});
                   $thisDialog.dialog("open");
         },error:function(){
           // need to handle TO-DO
         }
      });
    }

   $("#service_controls_tab").unbind("click").bind("click", function (event) {
       $('#details_tab').removeClass('active').addClass("nonactive");
       $('#currencies_tab').removeClass('active').addClass("nonactive"); 
       $('#catalog_tab').removeClass('active').addClass("nonactive");
  
       $('#details_content').hide();
       $('#currencies_content').hide();
       $('#action_currency').hide();
       $("#catalog_content").hide();
       $('#catalog_links').hide();
       $("#second_line_under_planned").hide();
       $("#main_action_box").attr("style", "height: 35px");
       $('#service_controls_tab').removeClass('nonactive').addClass("active");
       $('#action_service_settings').show();
       $('#service_controls_content').show();
   });
</script>

<style>
.ui-datepicker-trigger {
    margin-left : 5px;
    margin-top : 3px;
}

.datepicker_stlying{
  z-index: 9999
}
</style>

<spring:message code="date.format" var="ddMMMyyyy_format"/>
<input id="planning_for_first_time" type="hidden" name="planning_for_first_time" value="<c:out value="${futureRevisionDate}"/>"/>

<div class="widget_actionbar">
 <div class="widget_actionarea" id="top_actions" >

   <div id="spinning_wheel" style="display:none;">
         <div class="widget_blackoverlay widget_rightpanel">
         </div>
         <div class="widget_loadingbox widget_rightpanel">
           <div class="widget_loaderbox">
             <span class="bigloader"></span>
           </div>
           <div class="widget_loadertext">
             <p id="in_process_text"><spring:message code='label.loading.withdots'/></p>
           </div>
         </div>
   </div>

   <div class="widget_moreactions action_menu_container" title="<spring:message code='manage'/>" id="action_menu_container">
      <!--Actions popover starts here-->
      <div class="widget_actionpopover" id="action_menu" style="display:none">
          <div class="widget_actionpopover_top"></div>
            <div class="widget_actionpopover_mid">
              <ul class="widget_actionpoplist">
                <li id="editchannel_action" ><a href="javascript:void(0);"><spring:message code="label.channel.edit"/></a></li>
                <c:if test="${isChannelDeletionAllowed eq true}">  
                  <li id="deletechannel_action" ><a href="javascript:void(0);"><spring:message code="label.channel.delete"/></a></li>
                </c:if>
              </ul>
            </div>
            <div class="widget_actionpopover_bot"></div>
        </div>
        <!--Actions popover ends here-->
      </div>
    </div>
</div>

<div class="top_notifications">
  <div id="top_message_panel" class="common_messagebox widget" style="display:none;">
    <button type="button" class="close js_close_parent" >&times;</button>
    <span id="status_icon"></span><p id="msg"></p>
  </div>
  <div id="action_result_panel" class="common_messagebox widget" style="display:none;">
    <button type="button" class="close js_close_parent" >&times;</button>
    <span id="status_icon"></span><p id="msg"></p>
  </div>
</div>


<div class="widget_browser">
<!-- Start View Channel Details -->
<div class="widget_browsermaster">
  <div class="widget_browser_contentarea">
      <div class="widget_browsergrid_wrapper master">
            <div class="widget_grid master even first">
                <div class="widget_grid_labels">
                    <span><spring:message code="label.name"/></span>
              </div>
              <div class="widget_grid_description">
                  <span class = "ellipsis"  title = "<c:out value="${channel.name}"/>"> <c:out value="${channel.name}"/></span>
              </div>
          </div>
          <div class="widget_grid master odd">
               <div class="widget_grid_labels">
                    <span><spring:message code="ui.label.code"/></span>
              </div>
              <div class="widget_grid_description">
                  <span class = "ellipsis"  title = "<c:out value="${channel.code}"/>" ><c:out value="${channel.code}"/></span>
              </div>
          </div>
          <div class="widget_grid master even">
               <div class="widget_grid_labels">
                    <span><spring:message code="ui.products.label.create.catalog.select.currency"/></span>
              </div>
              <div class="widget_grid_description">
                  <span id="channeCurrencies<c:out value="${channel.id}"/>">
                    <c:forEach var="supported_currency" items="${supportedCurrencies}" varStatus="status">

                       <c:if test="${status.index > 0}">
                       ,
                       </c:if>

                       <c:out value="${supported_currency.currencyCode}"/>
                     </c:forEach>
                  </span>
              </div>
          </div>
      </div>
      <div class="widget_masterbigicons defaultbox">
          <div class="thumbnail_defaultcontainer">
              <div class="thumbnail_defaulticon channels">
                <c:choose>
                 <c:when test="${not empty channel.imagePath}">
                   <img src="/portal/portal/logo/channel/<c:out value="${channel.id}"/>" id="channelimage<c:out value="${channel.id}"/>" style="height:99px;width:99px;" />
                 </c:when>
                 <c:otherwise>
                   <img src="" id="channelimage<c:out value="${channel.id}"/>" style="height:99px;width:99px;" />
                 </c:otherwise>
                 </c:choose>
              </div>
           </div>
           <div class="widget_masterbigicons_linkbox default">
              <a href="javascript:void(0);"
                onclick="editChannelImage(this, <c:out value="${channel.id}" />)"
                class=" default editChannelLogo">
               <spring:message code="ui.channel.label.view.editimage"/>
              </a>
           </div>
        </div>
  </div>
</div>
<!-- End view Channel Details -->

  <!-- The Details and other tabs' content area  -->
  <div class="widget_browser_contentarea">
  
      <ul class="widgets_detailstab">
          <li id="details_tab" class="widgets_detailstab active"><spring:message code="label.details"/></li>
          <li id="currencies_tab" class="widgets_detailstab nonactive"><spring:message code="label.channel.currencies"/></li>
          <li id="catalog_tab" class="widgets_detailstab nonactive"><spring:message code="label.channel.catalog"/></li>
          <li id="service_controls_tab" class="widgets_detailstab nonactive"><spring:message code="label.channel.service.controls"/></li>
      </ul>
  
      <div class="widget_details_actionbox" id="main_action_box" style="height: 30px">

        <c:set var="plannedIsLast" value="false"/>
        <c:set var="currentIsLast" value="false"/>
        <c:if test="${!isCurrentThere && !isHistoryThere}">
          <c:set var="plannedIsLast" value="true"/>
        </c:if>
        <c:if test="${isCurrentThere && !isHistoryThere}">
          <c:set var="currentIsLast" value="true"/>
        </c:if>
        <ul class="widget_detail_navpanel" id="catalog_links" style="display:None">
           <li class="widget_detail_navpanel <c:if test="${plannedIsLast}">last</c:if>"  style="float:left; color:#000;" id="catalog_planned_tab">
              <a href="javascript:void(0);" onclick="preViewCatalogPlanned(event);"
                         planned="1" channelid="<c:out value="${channel.id}"/>"
                         id="planned_or_not">
                 <spring:message code="label.next.planned.version"/>
              </a>
           </li>

           <c:if test="${isCurrentThere}">
             <li class="widget_detail_navpanel <c:if test="${currentIsLast}">last</c:if>"  style="float:left;" id="catalog_current_tab">
                  <a href="javascript:void(0);" onclick="viewCatalogCurrent();"><spring:message code="label.current"/>
                  </a>
              </li>
            </c:if>

            <c:if test="${isHistoryThere}">
             <li class="widget_detail_navpanel last"  style="float:left;" id="catalog_history_tab">
                <a href="javascript:void(0);" onclick="viewCatalogHistory();"><spring:message code="label.history"/>
                </a>
             </li>
           </c:if>
        </ul>
  
        <div class="widget_subactions grid action_menu_container" id="action_currency" style="display:None;float:right;">
          <div class="widget_actionpopover_grid" id="currency_action_menu" style="display:None;">
            <div class="widget_actionpopover_top grid"></div>
                <div class="widget_actionpopover_mid">
                  <ul class="widget_actionpoplist">
                    <c:choose>
                      <c:when test="${currenciestoadd}">
                        <li class="view_volume_details_link" id="add_currencies" onclick="editCurrencies(event,this)" style="display: block;">
                          <spring:message code="label.channel.currencies.Add"/>
                        </li>
                      </c:when>
                      <c:otherwise>
                        <li id="no_actions_available"><spring:message code="label.no.actions.available"/></li>
                      </c:otherwise>
                    </c:choose>
                  </ul>
                </div>
            <div class="widget_actionpopover_bot"></div>
         </div>
       </div>

      <div id="second_line_under_planned" class="widget_detail_navpanel" style="width: 100%; margin-bottom:2px; display:none">
          
          
          <span class="widget_detail_navpanel" style="margin: 0 0 0 10px;" id="second_line_under_planned_span">
          <spring:message code="label.effective.date"/>
            <span id="effective_date">
                <c:choose>
                  <c:when test="${effectiveDate != null}">
                      <fmt:formatDate value="${effectiveDate}" pattern="${ddMMMyyyy_format}" />
                  </c:when>
                  <c:otherwise>
                      <spring:message code="ui.label.plan.date.not.yet.set"/>
                  </c:otherwise>
               </c:choose>
            </span>
          </span>
      </div>
      
       <div id="action_service_settings" style="display:None;">
          <div style="height:35px;">
			    <div style="padding:8px;">
				    <c:if test="${services}">
				   		<span>
	     					 <spring:message code="ui.label.service.sub.title" />
	     				</span>
						 <select  id="selectedInstance" class="select" style="margin: 0 0px; width:20px" onchange="refreshChannelServiceSettings();"  >
						      <option value=""><spring:message code="label.view.channel.choose.service"/></option>
							    <c:forEach items="${cloudService}" var="service">
							    	<optgroup label="${service.servicename}" class="highlight">
						                       <c:forEach items="${service.instances}" var="instance">
						                        	<option value=<c:out value="${instance.instanceuuid}"/>>${instance.instancename}</option>
						        			   </c:forEach> 
						        	</optgroup>		   
						        </c:forEach>
				         </select>
				    </c:if>   
				</div>
          </div>
       </div>
    </div>
  
      <!-- The channel details starts here -->
      <div id="details_content" class="widget_browsergrid_wrapper details">
            <div class="widget_grid details even">
                  <div class="widget_grid_labels">
                      <span><spring:message code="label.channel.description"/></span>
                  </div>
                  <div class="widget_grid_description" >
                      <span class = "ellipsis"  title = "<c:out value="${channel.description}"/>"> <c:out value="${channel.description}"/></span>
                  </div>
            </div>
            <%-- <div class="widget_grid details odd">
                  <div class="widget_grid_labels">
                      <span><spring:message code="label.cloudstack.domain.id"/></span>
                  </div>
                  <div class="widget_grid_description" >
                      <span><c:out value="${channel.domainId}"/></span>
                  </div>
            </div> --%>

  
      </div>
      <!-- The channel details ends here -->
  
      <!-- The channel currencies' details starts here -->
      <div class="widget_browsergrid_wrapper details" id="currencies_content" style="display:none;overflow-x: hidden;overflow-y: auto;">
        <div id="currency_row_container">
            <c:forEach var="currency" items="${supportedCurrencies}" varStatus="status">

              <c:choose>
                <c:when test="${status.index % 2 == 0}">
                  <c:set var="rowClass" value="odd"/>
                </c:when>
                <c:otherwise>
                    <c:set var="rowClass" value="even"/>
                </c:otherwise>
              </c:choose>

              <div class="<c:out value="widget_grid details ${rowClass}"/>">
                  <div class="widget_checkbox">
                    <span class="checked"></span> 
                  </div>
                  <div class="widget_grid_description" style="border:none;margin:0;">
                    <span><strong><c:out value="${currency.sign}"/> - <spring:message code="currency.longname.${currency.currencyCode}"/></strong></span>
                  </div>
                  <div class="widget_flagbox">
                    <div class="widget_currencyflag">
                        <img src="../../images/flags/<c:out value="${currency.currencyCode}"/>.gif" alt="" />
                    </div>
                  </div>
              </div>
            </c:forEach>
        </div>
      </div> 
      <!-- The channel currencies' details ends here -->
  
      <div id="dialog_edit_currencies" title='<spring:message code="label.channel.currencies.Add"/>' style="overflow-x: hidden; overflow-y: auto;">
      </div>
  
      <div id="catalog_content">
      </div>
    
      
    <div class="widget_browsergrid_wrapper details" id="service_controls_content" style="display:none;overflow-x: hidden;overflow-y: auto;">
    <div id="service_controls_content_row_container">
    <div id="channelServiceSettingsDiv" class="widget_details_actionbox">
    </div>      
    </div>
    </div>
  </div>
</div>

<div id="plan_date_div" title='<spring:message code="label.set.plan.date"/>'>
</div>

<div  id="dialog_edit_channel_image" title='<spring:message code="title.custom.channel.image"/>' style="display: none">
</div>

<div id="dialog_bundle_pricing" title='<spring:message code="ui.label.bundle.pricing"/>'>
</div>

<div id="dialog_utility_pricing" title='<spring:message code="ui.label.utility.pricing"/>'>
</div>