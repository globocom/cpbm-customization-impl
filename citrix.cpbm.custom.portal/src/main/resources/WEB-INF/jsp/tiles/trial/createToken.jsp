<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/css/jquery/themes/base/jquery-ui-1.7.2.custom.css"/>
<div id="maincontent_title">
  <h1>Generate Token</h1>
</div>
<div class="text_container">
 	<c:choose>
	<c:when test ="${isTokenAvailable != NULL && isTokenAvailable == 'Y'}">
	<div class="confirmation_messagebox">Email sent to customer with token and further instructions</div>      	
	</c:when>
	<c:otherwise>
		<c:if test ="${isTokenAvailable != NULL && isTokenAvailable == 'N'}">
			<div class="error_messagebox">No tokens available at this time</div>      		
     	</c:if>	
   	</c:otherwise>
   	</c:choose>
   	 <c:if test="${!empty emailValidationFailed}">
      <div class="error_messagebox"><c:out value="${emailValidationFailed}"/></div>
     </c:if>   
 <h3 style="margin-left:27px;">All fields marked with an asterisk (*) are required.</h3>
  <spring:url value="/portal/promotions/createToken" var="create_token_path" htmlEscape="false"/>
  <form:form commandName="tokenRequest" id="tokenRequestForm" cssClass="formPanel" action="${create_token_path}">
    <form:errors path="*" cssClass="errormsg"  />
    <div class="globaledit_form" style="width:100%;">
     
      <ol>
        <li>
			<form:label path="" >Campaign code:*</form:label>
            <form:select cssClass="select" tabindex="1" path="campaignCode" title="Country">
              <form:option value="" label="Choose..."/>
              <c:forEach items="${campaignPromotions}" var="choice" varStatus="status">
                <form:option value="${choice.code}">
                  <spring:message code="${choice.code}"/>
                </form:option> 
              </c:forEach>
            </form:select>
  		</li>
  		<li>
               <form:label path="promotionSignup.name">Name *:</form:label>
               <form:input cssClass="text" tabindex="2" path="promotionSignup.name" title="Name"/>
               <form:errors element="label" cssClass="error serverError" path=""/>
  		</li>
  		<li>
               <form:label path="promotionSignup.company" >Company Name *:</form:label>
               <form:input cssClass="text" tabindex="3" path="promotionSignup.company" title="Company Name"/>
               <form:errors  element="label" path="" cssClass="error serverError"/>
  		</li>
  		<li >
               <form:label path="promotionSignup.email">Email Address *:</form:label>
               <form:input cssClass="text" tabindex="4" path="promotionSignup.email" title="Email Address"/>
               <form:errors  element="label" path="" cssClass="error serverError"/>
  		</li>
  		<li >
               <form:label path="promotionSignup.phone">Phone Number *:</form:label>
               <form:input cssClass="text" tabindex="5" path="promotionSignup.phone" title="Phone NUmber"/>
               <form:errors  element="label" path="" cssClass="error serverError"/>
  		</li>
  			
	  </ol>
     </div>  
    
            <div class="globaledit_form" style="width:100%;">
            <table>
            	<tr>
            	<td>
            	 <form:label path="" >Preferred Currency *:</form:label>
            	</td>
            	<c:forEach items="${tokenRequest.currencyList}" var="curr_choice" varStatus="status"><td>
		           	<form:radiobutton  cssStyle="margin-left:0px;" cssClass="inputCurrency" path="promotionSignup.currency" tabindex="6" value="${curr_choice.currencyCode}"/>
		           	<spring:message code="currency.${curr_choice.currencyCode}" /> </td>
		        </c:forEach>
            	</tr>            	
            </table> 
            <div id="currencyErrorLoc" ></div><form:label path="" cssStyle="display:none;" cssClass="error">Select a currency!</form:label>	
		     </div>
    <div class="clear"></div> <br>   
    <div>
      <input tabindex="100" type="submit" id="submitbutton"class="selected_button" value="Send Email"/>
      <a class="grey_button" tabindex="101" href="javascript:history.back();">Cancel</a>
      <div class="clear"></div>
    </div>
  </form:form>
</div>
