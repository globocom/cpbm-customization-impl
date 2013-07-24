<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>

	
<spring:url value="/portal/profiles/create" var="create_profile_path" htmlEscape="false" />    
<form:form commandName="profileForm" id="profileForm" cssClass="formPanel" action="${create_profile_path}">
<form:errors path="*" /> 
<table>	
	<tr>			
		<td><form:label path="profile.name" cssClass="label">Profile Name:</form:label></td>
		<td><form:input  path="profile.name" title="PROFILE NAME"/>
		<form:errors cssClass="error" path="profile.name"/> 
		</td>
		<td></td>
	</tr>
	<tr></tr>	
	<tr></tr>		
	<tr> 	
	<td></td>		
 		<c:forEach items="${authorityTypes}" var="authority_choice"> 
 			<c:if test="${authority_choice != 'ROLE_PROFILE_CRUD'}">	
 				<td><B><spring:message code="${authority_choice}" /></B></td>
 			</c:if>
 		</c:forEach> 
	</tr> 		
	<tr>	
	<td></td>	
		<c:forEach items="${authorityTypes}" var="authority_choice" varStatus="status">		
			<c:if test="${authority_choice != 'ROLE_PROFILE_CRUD'}">			     
			   <td>
			     <form:checkbox path="authorityNames" value="${authority_choice}"/>
			   	 <form:errors cssClass="error" path="authorityNames"/>
			   </td>
			</c:if>				      
		 </c:forEach>
		 
	</tr>	
	<tr></tr>	
	<tr></tr>
	<tr>
		<td><form:label path="profile.operationsProfile" cssClass="label">Ops Profile:</form:label></td>
		<td><form:checkbox path="profile.operationsProfile"/></td>		
	</tr>
	
	<tr>
	 <td>
		<input tabindex="100" type="submit" class="submit" value="create" />
		<div class="clear"></div>
	</td>
	</tr>
</table>
</form:form>

