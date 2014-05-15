<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<script type="text/javascript">
$(document).ready(function() {
  $("#depositForm").inputAutoTitles();
  $("#receiveOn").datepicker(
      {
        duration: '',
        showOn: "button",
        buttonImage: "/portal/images/calendar_icon.png",
        buttonImageOnly: true,
        buttonText: "",
        showTime: false,
        beforeShow: function(dateText, inst){ 
            
        },
        onSelect: function(dateText, inst) {
            $(this).attr("value", dateText);
            $("#receiveOn").each(function() {
              $(this).attr("value", dateText);
            });
            
      },
      onClose: function(dateText, inst) {
        
      }});
  $("#depositForm").validate( {
    
    //debug : true,
    success : "valid",
    ignoreTitle : true,
    rules : {
      "receiveOn" : {
        required: true,
        date: true
      },
      "amt": {
        required: true
      }
    },
    messages : {
      "receiveOn" : {
        required: "Enter date when deposit was received"
      },
      "amt" : {
        required: "Enter amount of deposit received"
      }
    }
  });
  $("#receiveOn").attr( 'readOnly' , 'true' );
});
$("#ui-datepicker-div").css("z-index", "9999" );
</script>
<div class="dialog_formcontent">
 <div id="changeAccountTypeDiv">
  
      <form:form commandName="depositRecordForm" id="depositForm" action="${record_deposit_path}" >
      
          <ul>
            <li>
              <label for="receivedOn"><spring:message code="label.initial.deposit.record.received.on"/></label>
              <div class="mandatory_wrapper">                                            
                <form:input autocomplete="off" tabindex="102" id="receiveOn" path="receivedOn" title="MM/dd/yyyy" cssClass="text" />
              </div>
              <div class="main_addnew_formbox_errormsg" id="receivedOnError">
                <label for="receivedOn" generated="true" class="error" style="width:200px;padding-left:6px;"></label>
              </div>
            </li>                                                                                                     
            <li>
              <label for=""><spring:message code="label.initial.deposit.record.amount"/></label>
              <div class="mandatory_wrapper">                                            
                <form:input tabindex="102" id="amt" path="amount" readonly="true" cssClass="text"/>
              </div>
            </li>            
          </ul>
    
              
      </form:form>
    
</div>
</div>
