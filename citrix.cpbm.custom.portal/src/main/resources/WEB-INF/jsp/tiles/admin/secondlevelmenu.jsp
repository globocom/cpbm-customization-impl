<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<c:if test="${isAdmin}">
	<div class="secondlevel_withsubmenu">
		<div class="secondlevel_breadcrumb_panel">
   		<div class="secondlevel_breadcrumbbox" style="border-right:none;">
        <p><spring:message code="ui.menu.admin.secondlevel.page.detail"/></p>       		
     	</div>       
    </div>   
	     
	     <div class="secondlevel_menupanel">
	     	<sec:authorize access="hasRole('ROLE_CONFIGURATION_CRUD')">
	   			<a  class="secondlevel_menutabs <c:out value="${Configuiration}"/>" href="<%=request.getContextPath() %>/portal/admin/config/showconfiguration">
	        		<spring:message code="page.level2.configuration"/>
	        	</a>
	   		</sec:authorize>
	   		<sec:authorize access="hasRole('ROLE_PROFILE_CRUD')">
	        	<a  class="secondlevel_menutabs <c:out value="${Profiles}"/>" href="<%=request.getContextPath() %>/portal/profiles/show">
	        		 <spring:message code="page.level2.profiles"/>
	        	</a>
	       </sec:authorize>
	       <sec:authorize access="hasRole('ROLE_USER_CRUD')">
            <a  class="secondlevel_menutabs <c:out value="${Users}"/>" href="<%=request.getContextPath() %>/portal/users/listusersforaccount">
		        		<spring:message code="page.level2.adminusers"/>
		        	</a>
	        </sec:authorize>
	       <sec:authorize access="hasRole('ROLE_CONFIGURATION_CRUD')">
	        	<a  class="secondlevel_menutabs <c:out value="${BatchJobs}"/>"href="<%=request.getContextPath() %>/portal/admin/batch/status">
	        		<spring:message code="page.level2.batchjobs"/>
	        	</a>	
	        	</sec:authorize>
          <sec:authorize access="hasRole('ROLE_CONFIGURATION_CRUD')">
            <a  class="secondlevel_menutabs <c:out value="${AccountTypes}"/>"href="<%=request.getContextPath() %>/portal/admin/accounttypes/list">
              <spring:message code="page.level2.accounttypes"/>
            </a>  
            </sec:authorize>
            <sec:authorize access="hasRole('ROLE_CONFIGURATION_CRUD')">
            <a  class="secondlevel_menutabs <c:out value="${ContentTemplates}"/>"href="<%=request.getContextPath() %>/portal/admin/emailtemplates">
              <spring:message code="page.level2.contenttemplates"/>
            </a>  
            </sec:authorize>
		 </div>
     
 <tiles:insertDefinition name="warnings"></tiles:insertDefinition>  

	</div>
</c:if>
