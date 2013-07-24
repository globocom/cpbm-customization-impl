<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<div class="footer_linksbox">
  <p><a href="<%=request.getContextPath() %>/portal/health"><spring:message code="ui.footer.page.title.servicehealth"/></a> | <a href="<c:out value="${footerLinks.support}"/>"><spring:message code="ui.footer.page.title.support"/></a> | <a href="<c:out value="${footerLinks.blog}"/>"><spring:message code="ui.footer.page.title.blog"/></a> | <a href="<c:out value="${footerLinks.forum}"/>"><spring:message code="ui.footer.page.title.forum"/></a> | <a href="<c:out value="${footerLinks.contactUs}"/>"><spring:message code="ui.footer.page.title.contactus"/></a></p>
  <div class="clearboth"></div>
  <p><spring:message code="footer.copyright.message" htmlEscape="false" /> | <a href="<c:out value="${footerLinks.privacyPolicy}"/>"><spring:message code="ui.footer.page.title.privacypolicy"/></a> | <a href="<c:out value="${footerLinks.termsOfUse}"/>"><spring:message code="ui.footer.page.title.termsofuse"/></a></p>
</div>
<div style="text-align:right">
  <sec:authorize access="hasRole('ROLE_ACCOUNT_CRUD')">
    <spring:message code="version.id" /> (<spring:message code="build.id" />)
  </sec:authorize>
</div>
