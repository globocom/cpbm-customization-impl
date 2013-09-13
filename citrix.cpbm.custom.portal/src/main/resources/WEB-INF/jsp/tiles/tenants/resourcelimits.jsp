<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="csrf" uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld"%>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/tenants.js"></script>
<div class="maincontent_title" id="maincontent_title">  
  	<h2>Resource Limits</h2>
</div>
<div class="mainbox" style="display: block;">		
<div class="text_container">
<spring:url value="/portal/tenants/{tenant}/resource_limit" var="resource_path" htmlEscape="false">
	<spring:param name="tenant"><c:out value="${tenant.param}"/></spring:param>
</spring:url>
<form:form commandName="resourceLimitForm" cssClass="formPanel" action="${resource_path}" method="post" id="resourceLimitForm"	name="resourceLimitForm">
<div class="globaledit_form" >
		<div class="form_element">
              	 <table><tr><td>
              	 	<div class="label">
                		<label for="account">Account</label>
                	</div></td><td>
                	<div class="noneditable" style="width:200px;">      		
                		Max # of Projects
                	</div></td><td>
                	<div class="noneditable" style="width:200px;">      		
                		Max # of Users
                	</div>
                	</td></tr></table>   
         </div>
         <div class="form_element">
              	 <table><tr><td>
              	 	<div class="label">
                		<label for="account">Default</label>
                	</div></td><td>
                	<div class="noneditable" style="width:200px;">      		
                		<c:out value="${resourceLimitForm.defaultMaxProjects}"/>
                	</div></td><td>
                	<div class=noneditable style="width:200px;">      		
                		<c:out value="${resourceLimitForm.defaultMaxUsers}"/>
                	</div>
                	</td></tr></table>   
         </div>
         <div class="form_element">              	 
              	 	<div class="label" style="float: left;margin:5px;">
                		<label for="account"><c:out value="${tenant.name}"/></label>
                	</div>
                	<div class="text" style="width:200px;float: left;">      		
                		<form:input tabindex="1" path="maxProjects" title="maxrojects"/>
					<div id="maxProjectsError"></div>
                	</div>
                	<div class="text" style="width:200px;float: left;">      		
                		<form:input tabindex="2" path="maxUsers" title="maxusers"/>
					<div id="maxUsersError"></div>
                	</div>
         </div>
	</div>
	<form:hidden path="id"/>
	<div class="clearboth"></div>
	<div class="buttons" id="buttons" >
 				<table><tr><td>
 					<div class="second">
 						<a  tabindex="220" href="#" id="resourcelimitcancel">Cancel</a>
             		</div>
 					</td>
 					<td><div class="first">
                        <input type="hidden" name="<csrf:token-name/>" value="<csrf:token-value uri="portal/tenants/${tenant.param}/resource_limit"/>"/>
 						<input  tabindex="221" type="submit" value="Apply Changes"/>        		
 					</div>					
             	</td></tr></table>
			</div>	

 </form:form>
 </div>
 </div>