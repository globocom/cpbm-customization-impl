<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/subscribe.js"></script>
<div class="maincontent_title" id="maincontent_title">  
  <div>
  	<h1>Custom Spend Alerts</h1>
  </div>  
</div>

<c:if test="${! empty maxalerts}">
<div id="status_message" class="status_message">
<c:choose>
	<c:when test="${return_message_type=='info'}">
		<img alt="Information" style="padding: 5px 0px 0px 0px;" title="Info" src="/portal/images/checked.png"/>	
	</c:when>
	<c:when test="${return_message_type=='error'}">
		<img alt="Error" style="padding: 5px 0px 0px 0px;" title="Error" src="/portal/images/exclamation.png"/>		
	</c:when>
</c:choose>
<c:out value="${maxalerts}"></c:out>
</div>

</c:if>
<div class="mainbox  ui-corner-top ui-corner-bottom" style="display: block;">
	<div class="main_titlebox ui-corner-top">
		<h2><spring:message code="user.custom.spend.alert.create" /></h2>
	</div>
	<div class="text_container">
		<div class="required_fields"><span class="required">*</span> indicates mandatory field(s)</div><div class="clearboth"></div> 
		<spring:url value="/portal/users/subscribe/new" var="subscribe_path_add" htmlEscape="false"></spring:url>       
			<form class="subscriptionForm" id = "subscriptionForm" name="subscriptionForm" method ="POST" action="<c:out value="${subscribe_path_add}"/>" >
				<div class="globaledit_form" >
					 <div class="form_element">
						<table><tr><td>
							<div class="label">
					 			<form:label path="subscriptionForm.projectMembershipId" >Select Project<span class="required">* </span></form:label>
							</div></td><td>
   							<div class="text">
   							<div class="required"></div> 
   					 		 <form:select tabindex="1" path="subscriptionForm.projectMembershipId" >
	  							<option value="" >Choose...</option>             
            	 				<c:forEach items="${subscriptionForm.user.projectMemberships}" var="projectMembership">             
             	 					<option value="membership<c:out value="${projectMembership.id}"/>" ><c:out value="${projectMembership.project.name}"/></option>  
            					</c:forEach>
            	 				<c:forEach items="${ownerProjectList}" var="project">             
             	 					<option value="project<c:out value="${project.id}"/>" ><c:out value="${project.name}"/></option>  
            					</c:forEach>
          					</form:select>
   						</div>
   					</td></tr></table>              
	   			 	</div>
	   			 	<div class="form_element" id="thresholdtypes" style="display: none;">
						<table><tr><td>
						<div class="label">
							<form:label  path="subscriptionForm.thresholdType">Threshold Type<span class="required"> </span></form:label>
						</div></td><td>
   						<div class="radio" style="float: left;">
   						<div class="required"></div>
   					 		<form:radiobutton  path="subscriptionForm.thresholdType"  tabindex="2" value="percentage"  />
					 		<form:label  path="subscriptionForm.thresholdType">Percentage<span class="required"> </span></form:label>
   					  		<form:radiobutton   path="subscriptionForm.thresholdType"  tabindex="3" value="amount"  />
   					 		<form:label path="subscriptionForm.thresholdType">Amount<span class="required"> </span></form:label>
   						</div>
   							<div id="thresholdtypeError" style="float:left;padding-top: 6px;margin: 2px;">
				 		</div>
   						</td></tr></table>              
	    			</div>
	   			 		<div class="form_element" id="membershipDataDiv" style="display: none;">
						<table><tr><td>
							<div class="label">
					 			<form:label path="subscriptionForm.membershipData" >Alert when spend reaches <c:out value="${curency}"></c:out><span class="required"> </span></form:label>
							</div></td><td>
   							<div class="text">
   							<div class="required"></div> 
   					 		 <form:input tabindex="2" path="subscriptionForm.membershipData" title="Data value" />
   						</div>
   						</td></tr></table>              
	   			 		</div>	
	   			 		<div class="form_element" id="membershipPercentageDiv" style="display: none;">
							<table><tr><td>
								<div class="label">
					 				<form:label path="subscriptionForm.membershipPercentage" >Alert when spend reaches % <span class="required"> </span></form:label>
								</div></td><td>
   								<div class="text">
   								<div class="required"></div> 
   					 		 	<form:input tabindex="2" path="subscriptionForm.membershipPercentage" title="Percentage Value" />
   								</div>
   								</td></tr></table>              
	   			 		</div>
	   			 					 	
	   			 		<div class="form_element" id="projectPercentageDiv" style="display: none;">
						<table><tr><td>
							<div class="label">
					 			<form:label path="subscriptionForm.projectPercentage" >Alert when overall spend of this project reaches % <span class="required"> </span></form:label>
							</div></td><td>
   							<div class="text">
   							<div class="required"></div> 
   					 			<form:input tabindex="2" path="subscriptionForm.projectPercentage" title="Percentage Value" />
   							</div>
   						</td></tr></table>              
	   			 		</div>
	   			 		<div class="form_element" id="projectDataDiv" style="display: none;">
							<table><tr><td>
								<div class="label">
					 				<form:label path="subscriptionForm.projectData" >Alert when overall spend of this project reaches <c:out value="${curency}"></c:out> <span class="required"> </span></form:label>
								</div></td><td>
   								<div class="text">
   								<div class="required"></div> 
   					 				<form:input tabindex="2" path="subscriptionForm.projectData" title="Data value" />
   								</div>
   							</td></tr></table>              
	   			 		</div>   	
	   			 	
				</div>
				 <div class="clearboth"></div>
				<div class="buttons" id="buttons" >
					<table><tr><td>
						<div class="first">
							<input class="anchor_button ui-corner-bottom ui-corner-top" style="padding: 3px 12px;font-size: 12px;background: none;" tabindex="100" type="submit" value="Create"/>        		
						</div></td><td>
						<div class="second">
						<a class="anchor_button ui-corner-bottom ui-corner-top" tabindex="101" style="padding: 4px 12px;font-size: 12px;" href="javascript:history.back();">Cancel</a>
          				</div>
        			 </td></tr></table>
					</div>	
			</form>
	</div>
</div>
