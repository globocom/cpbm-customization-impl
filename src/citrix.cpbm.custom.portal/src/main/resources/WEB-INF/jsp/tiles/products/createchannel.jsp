<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<jsp:include page="js_messages.jsp"></jsp:include>

<script>
  $(".widget_checkbox").off('click');
  $(".widget_checkbox").on('click', function() {
    if($(this).find("span").attr("class") == "unchecked") {
         $(this).find("span").removeClass('unchecked').addClass('checked').css("margin-left", "0px");
       } else {
         operation = "remove";
         $(this).find("span").removeClass('checked').addClass('unchecked');
       }
  });
</script>

<div class="dialog_formcontent wizard">
  <form>
  <div class="widget_wizardcontainer">

<!--  Step 1 starts here  -->
    <div id="step1"  class="j_channelspopup">
      <input type="hidden" id="nextstep" name="nextstep" value="step2" >
      <input type="hidden" id="prevstep" name="prevstep" value="" >

       <div class="widgetwizard_stepsbox">
            <div class="widgetwizard_steps_contentcontainer">
                <div class="widgetwizard_stepscenterbar">
                    <ul>
                        <li class="widgetwizard_stepscenterbar  first"><span class="steps active"><span class="stepsnumbers active">1</span></span><span class="stepstitle active"><spring:message htmlEscape="false" code="ui.channel.create.step1.title" /></span></li>
                        <li class="widgetwizard_stepscenterbar fourstepswizard"><span class="steps"><span class="stepsnumbers">2</span></span><span class="stepstitle"><spring:message htmlEscape="false" code="ui.channel.create.step2.title" /></span></li>
                        <li class="widgetwizard_stepscenterbar fourstepswizard"><span class="steps"><span class="stepsnumbers">3</span></span><span class="stepstitle"><spring:message htmlEscape="false" code="ui.channel.create.step3.title" /></span></li>
                        <li class="widgetwizard_stepscenterbar fourstepswizard last"><span class="steps last"><span class="stepsnumbers last">4</span></span><span class="stepstitle last"><spring:message htmlEscape="false" code="ui.channel.create.step4.title" /></span></li>
                   </ul>
                </div>
            </div>
        </div>

       <div class="widgetwizard_contentarea">
         <div class="widgetwizard_boxes fullheight">
	          <div class="widgetwizard_titleboxes">
	                    <h2><spring:message code="ui.channel.create.step1.title"/></h2>
	                    <span><spring:message code="ui.channel.deteailsstep.desc"/></span>
	           </div>
	           <div class="widgetwizard_detailsbox">
	             <ul>
	               <li>
				            <span class="label"><spring:message code="label.name"/></span>
				            <div class="mandatory_wrapper"> 
				              <input class="text" tabindex="1" id="channelName" onchange="validate_channelname(event,this)"/>
				            </div>
				
				            <div class="main_addnew_formbox_errormsg_popup" id="name_errormsg"></div>
				        </li>
				        <li>
				            <span class="label"><spring:message code="label.channel.code"/></span>
				            <div class="mandatory_wrapper">
				              <input class="text" id="channelCode" onchange="validate_channelcode(event,this)" tabindex="2"/>
				            </div>
				
				            <div class="main_addnew_formbox_errormsg_popup" id="code_errormsg"></div>
				        </li>
				        <li>
                    <span class="label"><spring:message code="label.channel.description"/></span>
                    <div class="nonmandatory_wrapper">
                       <textarea class="textarea" id="channelDescription" onchange="validate_channeldesc(event,this)"
                                 style="width: 300px; height: 50px; margin-left: 10px;" tabindex="3"></textarea>
                    </div>

                   <div class="main_addnew_formbox_errormsg_popup" id="description_errormsg"></div>
                </li>
	             </ul>
	           </div>
         </div>
       </div>

        <div id="buttons" class="widgetwizard_nextprevpanel">
            <input class="widgetwizard_nextprevpanel nextbutton" type="button" onclick="addChannelNext(this)" value="<spring:message code="label.next.step"/>" name="<spring:message code="label.next.step"/>">
            <a href="javascript:void(0);" class="cancel close_channel_wizard" ><spring:message code="label.cancel" /></a>
        </div>

    </div>
<!--  Step 1 ends here  -->

<!--  Step 2 starts here  -->
    <div id="step2" class="j_channelspopup" style="display:none;">
      <input type="hidden" id="nextstep" name="nextstep" value="step3" >
      <input type="hidden"  id="prevstep"name="prevstep" value="step1" >

      <div class="widgetwizard_stepsbox">
         <div class="widgetwizard_steps_contentcontainer">
             <div class="widgetwizard_stepscenterbar">
                 <ul>
                     <li class="widgetwizard_stepscenterbar  first"><span class="steps completedsteps"><span class="stepsnumbers ">1</span></span><span class="stepstitle"><spring:message htmlEscape="false" code="ui.channel.create.step1.title" /></span></li>
                     <li class="widgetwizard_stepscenterbar fourstepswizard"><span class="steps active"><span class="stepsnumbers active">2</span></span><span class="stepstitle active"><spring:message htmlEscape="false" code="ui.channel.create.step2.title" /></span></li>
                     <li class="widgetwizard_stepscenterbar fourstepswizard"><span class="steps"><span class="stepsnumbers">3</span></span><span class="stepstitle"><spring:message htmlEscape="false" code="ui.channel.create.step3.title" /></span></li>
                     <li class="widgetwizard_stepscenterbar fourstepswizard last"><span class="steps last"><span class="stepsnumbers last">4</span></span><span class="stepstitle last"><spring:message htmlEscape="false" code="ui.channel.create.step4.title" /></span></li>
                 </ul>
             </div>
         </div>
       </div>

        <div class="widgetwizard_contentarea">
          <div class="widgetwizard_boxes fullheight">
            <div class="widgetwizard_titleboxes">
                <h2><spring:message code="ui.channel.create.step2.title" htmlEscape="false"></spring:message></h2>
                <span><spring:message code="ui.channel.currencyselect.desc"/></span>
            </div>
            <div class="widgetwizard_reviewbox">
              <ul>
                <li>
                  <span class="label" style="margin-bottom: 6px;"><spring:message code="label.channel.currencies" /></span>

				            <div class="mandatory_wrapper">
				              <div id="currency_row_container">
									      <c:forEach var="currency" items="${currencies}" varStatus="status">
									        <c:choose>
									          <c:when test="${(status.index) % 2 == 0}">
									            <c:set var="rowClass" value="odd"/>
									          </c:when>
									          <c:otherwise>
									              <c:set var="rowClass" value="even"/>
									          </c:otherwise>
									        </c:choose>
									        <div class="<c:out value="widget_grid details ${rowClass}"/>">
									            <div class="widget_checkbox widget_checkbox_wide"
									                currCode="<c:out value="${currency.currencyCode}"/>"
									                currSign="<c:out value="${currency.sign}"/>"
									                currName="<spring:message javaScriptEscape="true" code="currency.longname.${currency.currencyCode}"/>">
									              <span class="unchecked"></span> 
									            </div>
									            <div class="widget_grid_description" style="margin:0;">
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
		            <div class="main_addnew_formbox_errormsg_popup" id="currency_errormsg"></div>
	            </li>
	          </ul>
	         </div>
	       </div>
	     </div>

        <div id="buttons" class="widgetwizard_nextprevpanel">
            <input class="widgetwizard_nextprevpanel prevbutton" type="button" onclick="addEditChannelPrevious(this)" value="<spring:message code="label.previous.step"/>" name="<spring:message code="label.previous.step"/>">
            <input class="widgetwizard_nextprevpanel nextbutton" type="button" onclick="addChannelNext(this)" value="<spring:message code="label.next.step"/>" name="<spring:message code="label.add"/>">
            <a href="javascript:void(0);" class="cancel close_channel_wizard" ><spring:message code="label.cancel" /></a>
        </div>
    </div>
<!--  Step 2 ends here  -->

<!--  Step 3 starts here  -->
  <div id="step3" class="j_channelspopup" style="display:none">
    <input type="hidden" id="nextstep" name="nextstep" value="step4" >
    <input type="hidden" id="prevstep" name="prevstep" value="step2" >
     <div class="widgetwizard_stepsbox">
         <div class="widgetwizard_steps_contentcontainer">
             <div class="widgetwizard_stepscenterbar">
              <ul>
                  <li class="widgetwizard_stepscenterbar  first"><span class="steps completedsteps"><span class="stepsnumbers ">1</span></span><span class="stepstitle "><spring:message htmlEscape="false" code="ui.channel.create.step1.title" /></span></li>
                  <li class="widgetwizard_stepscenterbar fourstepswizard"><span class="steps completedsteps"><span class="stepsnumbers">2</span></span><span class="stepstitle"><spring:message htmlEscape="false" code="ui.channel.create.step2.title" /></span></li>
                  <li class="widgetwizard_stepscenterbar fourstepswizard"><span class="steps active"><span class="stepsnumbers active">3</span></span><span class="stepstitle active"><spring:message htmlEscape="false" code="ui.channel.create.step3.title" /></span></li>
                  <li class="widgetwizard_stepscenterbar fourstepswizard last"><span class="steps last"><span class="stepsnumbers last">4</span></span><span class="stepstitle last"><spring:message htmlEscape="false" code="ui.channel.create.step4.title" /></span></li>
                </ul>
             </div>
         </div>
     </div>
      <div class="widgetwizard_contentarea">
            <div class="widgetwizard_boxes fullheight">
                <div class="widgetwizard_titleboxes">
                    <h2><spring:message code="ui.channel.review.confirm.title" htmlEscape="false"></spring:message></h2>
                    <span>
                      <c:if test="">
                        <spring:message code="ui.channel.review.confirm.title.desc"/>
                      </c:if>
                      <spring:message code="ui.channel.review.confirm.title.desc"/>
                    </span>
                </div>
                <div class="widgetwizard_reviewbox">
                  <ul>
                      <li style="padding:0;" id="confirmChannelDetails">
                         <span class="label"><spring:message code="ui.channel.create.step1.title"/>:</span>
                          <span class="edit" style="margin-right:60px">
                            <a class="confirm_edit_link" onclick="backToChannelDetails(this);" href="javascript:void(0);"><spring:message code="label.edit"/></a>
                          </span>
                          <ul>
                           <li class="subselection" >
                                 <span class="label sublabel"><spring:message code="label.name"/></span>
                                 <span class="description subdescription ellipsis" id="conf_name"></span>
                            </li>
                            <li class="subselection" >
                                 <span class="label sublabel"><spring:message code="label.channel.code"/></span>
                                 <span class="description subdescription ellipsis" id="conf_code"></span>
                            </li>
                            <li class="subselection" >
                                 <span class="label sublabel"><spring:message code="label.channel.description"/></span>
                                 <span class="description subdescription ellipsis" id="conf_channel_description"></span>
                            </li>
                          </ul>
                      </li>
                      <li id="confirmChannelCurrencies">
                         <span class="label"><spring:message code="ui.channel.supported.currencies"/>:</span>
                          <span class="edit" style="margin-right:60px">
                            <a class="confirm_edit_link" onclick="backTourrencySelection(this);" href="javascript:void(0);"><spring:message code="label.edit"/></a>
                          </span>
                          <ul>
                            <li class="subselection" >
                                 <span class="label sublabel"><spring:message code="label.channel.currencies"/></span>
                                 <span class="description subdescription" id="conf_currencies"></span>
                            </li>
                          </ul>
                      </li>
                  </ul>
                </div>
            </div>
                        <div id="spinning_wheel5" style="display:none;">
                                  <div class="widget_blackoverlay widget_wizard">
                                  </div>
                                  <div class="widget_loadingbox widget_wizard">
                                    <div class="widget_loaderbox">
                                    	<span class="bigloader"></span>
                                    </div>
                                    <div class="widget_loadertext">
                                    	<p id="in_process_text"><spring:message code="label.loading.in.process"/></p>
                                    </div>
                                  </div>
                            </div>
        </div>

        <div id="buttons" class="widgetwizard_nextprevpanel">
            <input class="widgetwizard_nextprevpanel prevbutton" type="button" onclick="addEditChannelPrevious(this)" value="<spring:message code="label.previous.step"/>" name="<spring:message code="label.previous.step"/>">
            <input class="widgetwizard_nextprevpanel nextbutton" type="button" onclick="addChannelNext(this)" value="<spring:message code="label.add"/>" name="<spring:message code="label.add"/>">
            <a href="javascript:void(0);" class="cancel close_channel_wizard" ><spring:message code="label.cancel" /></a>
        </div>

    </div>
<!--  Step 3 starts here  -->

<!--  Step 4 starts here  -->
  <div id="step4" class="j_channelspopup" style="display:none;">
	  <input type="hidden" id="nextstep" name="nextstep" value="" >
	  <input type="hidden" id="prevstep" name="prevstep" value="step3" >

    <div class="widgetwizard_stepsbox">
       <div class="widgetwizard_steps_contentcontainer">
           <div class="widgetwizard_stepscenterbar">
               <ul>
                   <li class="widgetwizard_stepscenterbar  first"><span class="steps completedsteps"><span class="stepsnumbers ">1</span></span><span class="stepstitle "><spring:message htmlEscape="false" code="ui.channel.create.step1.title" /></span></li>
                  <li class="widgetwizard_stepscenterbar fourstepswizard"><span class="steps completedsteps"><span class="stepsnumbers">2</span></span><span class="stepstitle"><spring:message htmlEscape="false" code="ui.channel.create.step2.title" /></span></li>
                  <li class="widgetwizard_stepscenterbar fourstepswizard"><span class="steps completedsteps"><span class="stepsnumbers">3</span></span><span class="stepstitle"><spring:message htmlEscape="false" code="ui.channel.create.step3.title" /></span></li>
                  <li class="widgetwizard_stepscenterbar fourstepswizard last"><span class="steps last active"><span class="stepsnumbers last">4</span></span><span class="stepstitle last"><spring:message htmlEscape="false" code="ui.channel.create.step4.title" /></span></li>
               </ul>
           </div>
       </div>
    </div>

    <div class="widgetwizard_contentarea">
      <div class="widgetwizard_boxes fullheight">
           <div class="widgetwizard_successbox">
               <div class="widgetwizard_successbox">
                    <div class="widget_resulticon success"></div>
                      <p id="successmessage"><spring:message htmlEscape="false" code="ui.channel.successfully.completed.text"/>&nbsp;</p>
                      <c:if test="${fn:length(channels) == 0}">
                        <p id="defaultchannelmessage" style="font-weight:bold"><spring:message htmlEscape="false" code="ui.first.channel.created.as.default.message"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</p>
                      </c:if>
                      <a href="#" id="viewchanneldetails_configure"><spring:message htmlEscape="false" code="ui.channel.view.details.configure.text"/></a>
                  </div>
              </div>
          </div>
      </div>

      <div id="buttons" class="widgetwizard_nextprevpanel">
          <input class="widgetwizard_nextprevpanel submitbutton" type="button" onclick="addChannelNext(this)" value="<spring:message code="label.close"/>" name="Close">
      </div>

  </div>

<!--  Step 4 ends here  -->

  </div>
  </form>
</div>
