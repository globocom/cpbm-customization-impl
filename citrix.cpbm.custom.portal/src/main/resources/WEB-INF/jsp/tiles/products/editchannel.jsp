<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<jsp:include page="js_messages.jsp"></jsp:include>

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
                        <li class="widgetwizard_stepscenterbar fourstepswizard first"><span class="steps active"><span class="stepsnumbers active">1</span></span><span class="stepstitle active"><spring:message htmlEscape="false" code="ui.channel.create.step1.title" /></span></li>
                        <li class="widgetwizard_stepscenterbar fourstepswizard"><span class="steps"><span class="stepsnumbers">2</span></span><span class="stepstitle"><spring:message htmlEscape="false" code="ui.channel.edit.step2.title" /></span></li>
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
                      <span><spring:message code="ui.channel.edit.deteailsstep.desc"/></span>
             </div>
             <div class="widgetwizard_detailsbox">
               <ul>
                 <li>
                    <span class="label"><spring:message code="label.name"/></span>
                    <div class="mandatory_wrapper">
                      <input class="text" tabindex="1" id="channelName" onchange="validate_channelname(event,this)"
                             value="<c:out  value="${channel.name}" />" />
                   </div>

                    <div class="main_addnew_formbox_errormsg_popup" id="name_errormsg"></div>
                </li>
                <li>
                    <c:set var="if_mandatory_class" value="nonmandatory_wrapper"/>
                    <c:if test="${channel.code != null}">
                        <c:set var="if_mandatory_class" value="mandatory_wrapper"/>
                    </c:if>
                    <span class="label"><spring:message code="label.channel.code"/></span>
                    <div class="<c:out value='${if_mandatory_class}' />">
                      <input class="text" id="channelCode" onchange="validate_channelcode(event,this)" tabindex="2"
                             value="<c:out  value="${channel.code}" />"
                             prevValue="<c:out  value="${channel.code}" />" />
                    </div>

                    <div class="main_addnew_formbox_errormsg_popup" id="code_errormsg"></div>
                </li>
                <li>
                    
                    <span class="label"><spring:message code="label.channel.description"/></span>
                    <div class="nonmandatory_wrapper">
                      <textarea class="textarea" id="channelDescription" onchange="validate_channeldesc(event,this)"
                                style="width: 300px; height: 50px; margin-left: 10px;" tabindex="3"><c:out  value="${channel.description}" /></textarea>
                    </div>

                   <div class="main_addnew_formbox_errormsg_popup" id="description_errormsg"></div>
                </li>
              </ul>
            </div>
         </div>
       </div>

        <div id="buttons" class="widgetwizard_nextprevpanel">
            <input class="widgetwizard_nextprevpanel nextbutton" type="button" onclick="editChannelNext(this)" value="<spring:message code="label.next.step"/>" name="<spring:message code="label.next.step"/>">
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
                     <li class="widgetwizard_stepscenterbar fourstepswizard"><span class="steps active"><span class="stepsnumbers active">2</span></span><span class="stepstitle active"><spring:message htmlEscape="false" code="ui.channel.edit.step2.title" /></span></li>
                     <li class="widgetwizard_stepscenterbar fourstepswizard"><span class="steps"><span class="stepsnumbers">3</span></span><span class="stepstitle"><spring:message htmlEscape="false" code="ui.channel.create.step3.title" /></span></li>
                     <li class="widgetwizard_stepscenterbar fourstepswizard last"><span class="steps last"><span class="stepsnumbers last">4</span></span><span class="stepstitle last"><spring:message htmlEscape="false" code="ui.channel.create.step4.title" /></span></li>
                 </ul>
             </div>
         </div>
       </div>

        <div class="widgetwizard_contentarea">
          <div class="widgetwizard_boxes fullheight">
            <div class="widgetwizard_titleboxes">
                <h2><spring:message code="ui.channel.edit.step2.title" htmlEscape="false"></spring:message></h2>
                <span><spring:message code="ui.channel.supportedcurrencies.desc"/></span>
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
                                 <span class="checked" style="margin-left: 0px"></span>
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
            <input class="widgetwizard_nextprevpanel nextbutton" type="button" onclick="editChannelNext(this)" value="<spring:message code="label.next.step"/>" name="<spring:message code="label.add"/>">
            <a href="javascript:void(0);" class="cancel close_channel_wizard" ><spring:message code="label.cancel" /></a>
        </div>
    </div>
<!--  Step 2 ends here  -->

<!--  Step 3 starts here -->

  <div id="step3" class="j_channelspopup" style="display:none">
    <input type="hidden" id="nextstep" name="nextstep" value="step4" >
    <input type="hidden" id="prevstep" name="prevstep" value="step2" >
     <div class="widgetwizard_stepsbox">
         <div class="widgetwizard_steps_contentcontainer">
             <div class="widgetwizard_stepscenterbar">
              <ul>
                  <li class="widgetwizard_stepscenterbar  first"><span class="steps completedsteps"><span class="stepsnumbers ">1</span></span><span class="stepstitle "><spring:message htmlEscape="false" code="ui.channel.create.step1.title" /></span></li>
                  <li class="widgetwizard_stepscenterbar fourstepswizard"><span class="steps completedsteps"><span class="stepsnumbers">2</span></span><span class="stepstitle"><spring:message htmlEscape="false" code="ui.channel.edit.step2.title" /></span></li>
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
                    <span><spring:message code="ui.channel.review.edit.confirm.title.desc"/></span>
                </div>
                <div class="widgetwizard_reviewbox">
                  <ul>
                      <li style="padding:0;" id="confirmChannelEditDetails">
                         <span class="label"><spring:message code="ui.channel.create.step1.title"/>:</span>
                          <span class="edit" style="margin-right:60px">
                            <a class="confirm_edit_link" onclick="backToChannelDetails(this);" href="javascript:void(0);"><spring:message code="label.edit"/></a>
                          </span>
                          <ul>
                           <li class="subselection" >
                                 <span class="label sublabel"><spring:message code="label.name"/></span>
                                 <span class="description subdescription ellipsis" id="conf_edit_name"></span>
                            </li>
                            <li class="subselection" >
                                 <span class="label sublabel"><spring:message code="label.channel.code"/></span>
                                 <span class="description subdescription ellipsis" id="conf_edit_code"></span>
                            </li>
                            <li class="subselection" >
                                 <span class="label sublabel"><spring:message code="label.channel.description"/></span>
                                 <span class="description subdescription ellipsis" id="conf_edit_channel_description"></span>
                            </li>
                          </ul>
                      </li>
                      <li>
                         <span class="label"><spring:message code="ui.channel.supported.currencies"/>:</span>
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
        </div>

        <div id="buttons" class="widgetwizard_nextprevpanel">
            <input class="widgetwizard_nextprevpanel prevbutton" type="button" onclick="addEditChannelPrevious(this)" value="<spring:message code="label.previous.step"/>" name="<spring:message code="label.previous.step"/>">
            <input class="widgetwizard_nextprevpanel nextbutton" type="button" onclick="editChannelNext(this)" value="<spring:message code="label.save"/>" name="<spring:message code="label.add"/>">
            <a href="javascript:void(0);" class="cancel close_channel_wizard" ><spring:message code="label.cancel" /></a>
        </div>

    </div>
<!--  Step 3 ends here -->

<!--  Step 4 starts here  -->
  <div id="step4" class="j_channelspopup" style="display:none;">
    <input type="hidden" id="nextstep" name="nextstep" value="" >
    <input type="hidden" id="prevstep" name="prevstep" value="step3" >

    <div class="widgetwizard_stepsbox">
       <div class="widgetwizard_steps_contentcontainer">
           <div class="widgetwizard_stepscenterbar">
               <ul>
                   <li class="widgetwizard_stepscenterbar  first"><span class="steps completedsteps"><span class="stepsnumbers ">1</span></span><span class="stepstitle "><spring:message htmlEscape="false" code="ui.channel.create.step1.title" /></span></li>
                  <li class="widgetwizard_stepscenterbar fourstepswizard"><span class="steps completedsteps"><span class="stepsnumbers">2</span></span><span class="stepstitle"><spring:message htmlEscape="false" code="ui.channel.edit.step2.title" /></span></li>
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
                      <p id="successmessage"><spring:message htmlEscape="false" code="ui.channel.successfully.edited.text"/>&nbsp;</p>
                      <a href="#" id="viewchanneldetails_configure"><spring:message htmlEscape="false" code="ui.channel.view.details.configure.text"/></a>
                  </div>
              </div>
          </div>
      </div>

      <div id="buttons" class="widgetwizard_nextprevpanel">
          <input class="widgetwizard_nextprevpanel submitbutton" type="button" onclick="editChannelNext(this)" value="<spring:message code="label.close"/>" name="Close">
      </div>

  </div>

<!--  Step 4 ends here  -->

  </div>
  </form>
</div>
