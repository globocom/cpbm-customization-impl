<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<script language="javascript">
$(document).ready(function(){
  $("#advSrchCancel").click(function() { $("#advancedSearch").slideUp(); }); 
});
</script> 
<div class="clearboth"></div>
<div class="main_addnewbox" style="display:block;">
	 <div class="main_addnewbox_contentbox">
	    <div class="main_addnewbox_titlebox">
	      <h2><spring:message code="ui.label.report.parameters"/></h2>
	    </div>
	        <spring:url value="/portal/tenants" var="create_tenant_path" htmlEscape="false" /> 
   
	    <spring:url value="/portal/tenants/search" var="search_tenant_path" htmlEscape="false" /> 
	    <form:form commandName="searchForm" id="searchForm" action="${search_tenant_path}" method="POST">
		<div class="main_addnew_formbox" id="params">
		  <c:forEach items="${searchForm.fieldList}" var="choice" varStatus="status">
			<div class="main_addnew_formpanels" style="border: none">
				<ol>
					<li style="margin-left: 15px; display: inline;">
					  <form:label path="fieldList[${status.index}].value"><c:out value="${choice.dispalyName}"/></form:label>             
                      <form:input cssClass="text" tabindex="${status.index}" path="fieldList[${status.index}].value" title="${choice.fieldName}"/>
                    </li>
				</ol>
			</div>
			</c:forEach>
		</div>
      <div class="main_addnew_submitbuttonpanel">
        <div class="main_addnew_submitbuttonbox">
          <input tabindex="100"  class="commonbutton submitmsg" rel="<spring:message code="ui.label.report.rel.generating"/>" type="submit" value="<spring:message code="ui.label.report.submit"/>"/>
         <a id="advSrchCancel" class="selection_commonbutton"  href="javascript:void(0);" >Cancel</a>
        </div>
      </div>
    </form:form>
	</div>
</div>
<div class="clearboth"></div>