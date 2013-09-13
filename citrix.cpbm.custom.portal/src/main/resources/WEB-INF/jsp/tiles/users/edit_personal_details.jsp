<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<script language="javascript">
	var i18n = {
		      user: {
	      		delfail: '<spring:message javaScriptEscape="true" code="js.user.del.fail"/>',
	      		deactfail: '<spring:message javaScriptEscape="true" code="js.user.deact.fail"/>',
	      		actfail: '<spring:message javaScriptEscape="true" code="js.user.act.fail"/>',
	      		channel: '<spring:message javaScriptEscape="true" code="js.user.channel"/>',
	      		title: '<spring:message javaScriptEscape="true" code="js.user.title"/>',
	    		del : '<spring:message javaScriptEscape="true" code="js.user.del.confirm"/>',
	    		deact: '<spring:message javaScriptEscape="true" code="js.user.deact.confirm"/>',
	    		act: '<spring:message javaScriptEscape="true" code="js.user.act.confirm"/>',
	    		delproject : '<spring:message javaScriptEscape="true" code="js.user.del.confirmproject"/>',	      		
			    max : '<spring:message javaScriptEscape="true" code="js.user.max"/>',
			    cloudstorage: '<spring:message javaScriptEscape="true" code="js.user.cloudstorage.subscribe"/>',
			    profile: '<spring:message javaScriptEscape="true" code="js.user.profile.edit"/>',
			    firstname: '<spring:message javaScriptEscape="true" code="js.user.firstname"/>',
			    lastname: '<spring:message javaScriptEscape="true" code="js.user.lastname"/>',
			    email: '<spring:message javaScriptEscape="true" code="js.user.email"/>',
			    emailmatch: '<spring:message javaScriptEscape="true" code="js.user.email.match"/>',
			    confirmemail: '<spring:message javaScriptEscape="true" code="js.user.confirmemail"/>',
			    emailformat: '<spring:message javaScriptEscape="true" code="js.user.email.format"/>',
			    username: '<spring:message javaScriptEscape="true" code="js.user.username"/>',
			    usernameexists: '<spring:message javaScriptEscape="true" code="js.user.username.exists"/>',
			    password: '<spring:message javaScriptEscape="true" code="js.user.password"/>',
			    passwordconfirm: '<spring:message javaScriptEscape="true" code="js.user.password.confirm"/>',
			    passwordmatch: '<spring:message javaScriptEscape="true" code="js.user.password.match"/>',
			    profilerequired: '<spring:message javaScriptEscape="true" code="js.user.profile.required"/>',
			    passwordequsername: '<spring:message javaScriptEscape="true" code="js.user.passwordequsername"/>'
		      }
		};
</script>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/users.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/jquery.selectboxes.min.js"></script>
	<c:set var="countries" scope="page" value="${userForm.countryList}"/>
<jsp:include page="/WEB-INF/jsp/tiles/shared/country_states.jsp"></jsp:include>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/jquery.stateselect.js"></script>
<div class="maincontent_title" id="maincontent_title">  
  	<h1>Edit Personal Details</h1>
</div>
<div class="mainbox  ui-corner-top ui-corner-bottom" style="display: block;">
	
	<div class="text_container"> 
	<div class="required_fields"><span class="required">*</span> indicates mandatory field(s)</div><div class="clearboth"></div>  
  		<spring:url value="/portal/users/personal_details_edit" var="update_user_path" htmlEscape="false">        
      </spring:url> 
    	<form:form commandName="user" id="userForm" cssClass="formPanel" action="${update_user_path}" method="put">
  		<div class="globaledit_form" >  		
    	<div class="form_element">
			<table><tr><td>
				<div class="label">
					 <form:label path="user.firstName">First Name<span class="required">* </span> </form:label>
				</div></td><td>
   				<div class="text">
   					 <form:input tabindex="201" path="user.firstName" title="First Name" />
          			 <form:errors cssClass="servererror" path="user.firstName"/>
   				</div>
   			</td></tr></table>              
	    </div>
	    <div class="form_element">
			<table><tr><td>
				<div class="label">
					<form:label path="user.lastName">Family Name<span class="required">* </span> </form:label>
				</div></td><td>
   				<div class="text">
   					  <form:input tabindex="202" path="user.lastName" title="Family name" />
          			  <form:errors cssClass="servererror" path="user.lastName"/>
   				</div>
   			</td></tr></table>              
	    </div>
	     
	    <div class="form_element">
			<table><tr><td>
				<div class="label">
					<label for="user_username">Username<span class="required"> </span></label>
				</div></td><td>
   				<div class="text">
   					<div class="noneditable"><c:out value="${user.user.username}"/></div>
   				</div>
   			</td></tr></table>              
	    </div>
	    
	    
	    <div class="form_element">
			<table><tr><td>
				<div class="label">
					<form:label path="user.email">Email <span class="required">* </span></form:label>
				</div></td><td>
   				<div class="text">
   					 <form:input tabindex="208" path="user.email" title="Email Address" />
          		     <form:errors path="user.email" cssClass="servererror"/>
   				</div>
   			</td></tr></table>              
	    </div>
	    
	    <div class="form_element">
			<table><tr><td>
                 	<div class="label">
                		<label  for="confirmEmail">Confirm Email<span class="required">* </span></label>
                	</div></td><td>
                	<div class="text">
                		<div class="required"></div>
                		<input type="text" value="<c:out value="${user.user.email}" />" tabindex="209" title="Confirm Email" name="confirmEmail" id="confirmEmail">
                	</div>
                </td></tr></table>               
	    </div>
	    <div class="form_element">
			<table><tr><td>
				<div class="label">
					 <form:label path="clearPassword">Password <span class="required"></span></form:label>
				</div></td><td>
   				<div class="text">
   					   <form:password tabindex="206" path="clearPassword" autocomplete="off" title="Password" />
          			   <form:errors cssClass="servererror" path="clearPassword"/>
   				</div>
   			</td></tr></table>              
	    </div>
	     <div class="form_element">
              	 <table><tr><td>
              	 	<div class="label">
                		<label for="password_confirm" >Confirm Password <span class="required"></span></label>
                	</div></td><td>
                	<div class="text">
                		<div class="required"></div>
                		<input  tabindex="207" type="password" name="clearPassword_confirm" autocomplete="off" title="Password"/>
                	</div>
                	</td></tr></table>                   	
          </div> 
          <sec:authorize access="hasAnyRole('ROLE_ACCOUNT_ADMIN','ROLE_ACCOUNT_CRUD')">
            <div class="form_element">
              	 <table><tr><td>
              	 	<div class="label">
                		<form:label path="user.phone" >Phone Number<span class="required">*</span> </form:label>
                	</div></td><td>
                	<div class="text">                		
                		<form:input  tabindex="208" path="user.phone" title="Phone Number"/>
                	</div>
               	 	</td></tr></table>   
              	 </div> 
           </sec:authorize>
	    
	     <div class="form_element">
			<table><tr><td>
				<div class="label">
					 <form:label path="user.timeZone">Select a time zone<span class="required"> </span></form:label>
				</div></td><td>
   				<div class="text">
   					  <form:select tabindex="210" path="user.timeZone" title="Time zone" cssStyle="width:auto;">
            			<form:option value="" label="Choose..."/>
           				 <c:forEach items="${user.timeZones}" var="choice" varStatus="status">
              				<form:option value="${choice.value}">
                			<spring:message code="${choice.key}"/>
             			 	</form:option> 
            			</c:forEach>
          			</form:select>
   				</div>
   			</td></tr></table>              
	    </div>	    
    </div>   
    <form:hidden path="action"/>
    <div class="clearboth"></div>
	<div class="buttons" id="buttons" >
 				<table><tr><td>
 					<div class="second">
 						<a  tabindex="215" href="javascript:history.back();" >Cancel</a>
             		</div>
 					</td>
 					<td><div class="first">
 						<input  tabindex="216" type="submit" value="Apply changes"/>        		
 					</div>					
             	</td></tr></table>
			</div>	
  	</form:form>
  </div> 
</div>
    
