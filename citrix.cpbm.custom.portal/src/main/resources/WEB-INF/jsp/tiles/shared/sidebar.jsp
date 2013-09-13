<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<script type="text/javascript" src="<%=request.getContextPath() %>/resources/app.js"></script>

  <script>
  $(document).ready(function() {	
    $("#home_side_accordion").accordion();  
    $("#home_side_accordion" ).accordion({ autoHeight: false }); 
    $("#home_side_accordion").accordion({ collapsible: true });   
    $("#home_side_accordion").accordion({ active: false});  
  });

  </script>

<spring:url value="/billing/history" var="tenant_billing_history_path" htmlEscape="false">
   <spring:param name="tenant"><c:out value="${currentUser.tenant.param}"/></spring:param>
</spring:url>
<div id="home_side_accordion">
	<h3><a href="/portal/portal/home">Home</a></h3>
	<div>
		<a href="/portal/portal/home">Dashboard</a>
	</div>
	<h3><a href="<%=request.getContextPath() %>/portal/users/projectsconsole">My Cloud Resources</a></h3>
	<div>
		<a href="<%=request.getContextPath() %>/portal/users/projectsconsole">View Cloud Console</a><br></br>
	</div>
	<h3><a href="#">My Account</a></h3>
	<div>
		<spring:url value="/tenants/{tenant}" var="tenant_path" htmlEscape="false">
   			<spring:param name="tenant"><c:out value="${currentUser.tenant.param}"/></spring:param>
		</spring:url>
		<div><a href="<%=request.getContextPath() %><c:out value="${tenant_path}"/>">View/Edit Details</a></div>
		<div><a href="#">View API Keys</a></div>
		<sec:authorize access="hasAnyRole('ROLE_PROJECT_CRUD','ROLE_ACCOUNT_PROJECT_CRUD','ROLE_ACCOUNT_PROJECT_ADMIN')">   
          <c:choose>
			<c:when test="${!currentUser.profile.operationsProfile}">
				<div><a href="<%=request.getContextPath() %>/portal/projects">Manage Projects</a></div>                
			</c:when>
		</c:choose>
        </sec:authorize>
		<sec:authorize access="hasAnyRole('ROLE_USER_CRUD','ROLE_ACCOUNT_USER_CRUD')">
		<div><a href="<%=request.getContextPath() %>/portal/users">Manage Users</a></div>
		</sec:authorize>
		<div><a href="#">View My Subscriptions</a></div>				
	</div>
	<h3><a href="#">My Usage</a></h3>
	<div>
		<div><a href="#">View by Billing Period</a></div>
	</div>
	<sec:authorize access="hasAnyRole('ROLE_ACCOUNT_ADMIN','ROLE_ACCOUNT_BILLING_ADMIN')">
	<h3><a href="#">My Billing</a></h3>
	<div>
	 <sec:authorize access="hasAnyRole('ROLE_ACCOUNT_ADMIN','ROLE_ACCOUNT_BILLING_ADMIN')">
		<div><a href="<%=request.getContextPath() %>/portal/billing/">View/Edit Details</a></div>
	</sec:authorize>
		<div><a href="<%=request.getContextPath() %><c:out value="${tenant_billing_history_path}"/>">View History</a></div>
		<div><a href="#">Manage Spend Budget</a></div>
		<div><a href="#">View Usage</a>	</div>			
	</div>
	</sec:authorize>
	<h3><a href="#">My Alerts</a></h3>
	<div>
		<div><a href="<%=request.getContextPath() %>/portal/users/alerts_prefs">View/Edit Preferences</a></div>
		<div><a href="<%=request.getContextPath() %>/portal/users/alerts">View Alerts</a></div>
		<c:choose>
			<c:when test="${!currentUser.profile.operationsProfile && currentUser.profile.name != 'Billing Admin'}">		
               <div><a href="<%=request.getContextPath() %>/portal/users/subscribe">Manage Custom Spend Alerts</a></div>	
            </c:when>
       </c:choose>
	</div>
	<h3><a href="#">Support</a></h3>
	<div style="height:auto;">
		<div><a href="<%=request.getContextPath() %>/portal/help">Help Desk</a></div>
		<div><a href="<%=request.getContextPath() %>/portal/health">Service Health</a></div>
		<div><a href="#">Forums</a></div>
		<div><a href="#">Knowledge Base</a></div>
		<div><a href="#">Developer APIs</a></div>
	</div>
	<h3><a href="#">Community</a></h3>	
	<div>
		<div><a href="#">Blogs</a></div>
		<div><a href="#">Wikis</a></div>
		<div><a href="#">Forums</a></div>		
	</div>
	<sec:authorize access="hasAnyRole('ROLE_ACCOUNT_CRUD','ROLE_ACCOUNT_MGMT')">
	<h3><a href="#">CRM</a></h3>	
	<div>
		<div><a href="<%=request.getContextPath() %>/portal/tenants">Accounts</a></div>
		<!-- 
		<spring:url value="/portal/projects" var="projects_all_path" htmlEscape="false">
        <spring:param name="showAll" value="true"/>
      </spring:url>
		<div><a href="<c:out value="${projects_all_path}"/>">Projects</a></div>
		
		<spring:url value="/portal/users" var="users_path" htmlEscape="false">
        <spring:param name="showAll" value="true"/>
      </spring:url>
		<div><a href="<c:out value="${users_path}"/>" >Users</a></div> 
		-->		
	</div>
	</sec:authorize>
	<h3><a href="#">Reports</a></h3>	
	<div>
		<div><a href="#">Report 1</a></div>
		<div><a href="#">Export</a></div>
	</div>
	<h3><a href="#">Product Management</a></h3>	
	<div>
		<div><a href="#">Manage Catalog</a></div>
		<div><a href="#">Manage Campaigns</a></div>
		<sec:authorize access="hasRole('ROLE_CONFIGURATION_CRUD')">		
			<div><a href="<%=request.getContextPath() %>/portal/rpc/show">Manage Rate Plans</a></div>
		</sec:authorize>
	</div>
	
	<h3><a href="#">Admin</a></h3>	
	<div>
		<sec:authorize access="hasRole('ROLE_CONFIGURATION_CRUD')">
		<div><a href="<%=request.getContextPath() %>/portal/home/config/show_configuration">Configuration</a></div>
		</sec:authorize>
		<div><a href="#">Jobs</a></div>
		<div><a href="#">Cloud Administration</a></div>
		 <sec:authorize access="hasRole('ROLE_PROFILE_CRUD')">	
		<div><a href="<%=request.getContextPath() %>/portal/profiles/show"><spring:message code="page.level3.profilemanagement"/></a></div>		
		</sec:authorize>
		 <sec:authorize access="hasAnyRole('ROLE_ACCOUNT_CRUD','ROLE_ACCOUNT_MGMT')">
		<div><a href="<%=request.getContextPath() %>/portal/promotions/list" >Campaigns</a></div>
		</sec:authorize>
	</div>			
</div>
<div class="clearboth">
</div>
