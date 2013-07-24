<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
 <%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
 <div class="secondlevel_withoutsubmenu">
  <div class="secondlevel_breadcrumb_panel">
     <div class="secondlevel_breadcrumbbox">
       <p><c:out value="${tenantLogoForm.tenant.name}"/></p>
     </div>     
  </div>
</div>

<jsp:include page="../shared/warnings.jsp"></jsp:include>  
