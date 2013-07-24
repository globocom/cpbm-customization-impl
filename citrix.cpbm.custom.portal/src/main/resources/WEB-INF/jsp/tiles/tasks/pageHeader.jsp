<%-- Copyright (C) 2012 Citrix, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<jsp:include page="/WEB-INF/jsp/tiles/billing/js_messages.jsp"></jsp:include>
<script language="javascript">
if( typeof i18n === 'undefined' ) {
  var i18n = {};
}
var tenantParam = '<c:out value="${tenant.param}"/>';
</script>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/tasks.js"></script>