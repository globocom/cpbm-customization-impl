<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>

<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>
      <spring:message code="company.name"/> -      
      <spring:message code="webapp.tagline"/> 
    </title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1"/>
    <script type="text/javascript" src="<%=request.getContextPath() %>/resources/all.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath() %>/csrf_js_servlet"></script>   
    <script type="text/javascript" src="<%=request.getContextPath() %>/resources/app.js"></script>
    <link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/resources/all.css"/>
    <link rel="stylesheet" type="text/css" media="all" href="<%=request.getContextPath()%>/css/main.css"/>
    <link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/custom/css/custom.css"/>   
    <link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/<spring:theme code="css" />"/> 
    <tiles:insertAttribute name="customHeader" ignore="true"/>
    <tiles:insertAttribute name="captcha_strings" ignore="true"/>
    
    <c:if test="${isGoogleAnalyticsEnabled}">
    	<script type="text/javascript">

  			var _gaq = _gaq || [];
 			_gaq.push(['_setAccount', '<c:out value="${googleAnalyticsAccount}"/>']);
  			_gaq.push(['_setDomainName', '<c:out value="${googleAnalyticsDomain}"/>']);
  			_gaq.push(['_trackPageview']);

  			(function() {
    			var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    			ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    			var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  			})();
		</script>
	</c:if>
    
  </head>
  <body>
   <c:if test="${directoryServiceAuthenticationEnabled || useSmallCss}">
    <div id="login_main_smaller_ver">
   </c:if>
    <c:if test="${!directoryServiceAuthenticationEnabled}">
    <div id="login_main">
    </c:if>
    <div id="catalog_signupmenubox_container"> 
    <div class="catalog_signupmenubox">
    
    <c:if test="${showLanguageSelection=='true' || showAnonymousCatalogBrowsing=='true'}">
      
      <c:choose>
        <c:when test="${showAnonymousCatalogBrowsing=='true'}">
        <div class="catalog_signupmenubox_left"></div>
                <div class="catalog_signupmenubox_mid">
                  <ul>
                      <li class="catalog_signupmenubox_mid"><span class="icon catalog"></span><span id="browse_catalogs_li" class="link"><spring:message code='label.catalog.browse'/></span></li>
                        <li class="catalog_signupmenubox_mid last"  id="language_selector"><span class="icon language"></span><span class="link"><spring:message code="page.title.select.language"/></span>
                         <div class="catalog_signupmenubox_popover" id="language_selector_dropdown" style="left: 162px;display: None">
                            <ul class="catalog_signuppopoverlist">
                      <c:forEach items="${supportedLocaleList}" var="locale" varStatus="status">
                        <li class="language_select_option" id="<c:out value="${locale.key}"></c:out>"><span class="flagicons <c:out value="${locale.key}"></c:out>"></span><span class="languagelist"><c:out value="${locale.value}"></c:out></span></li>
                          </c:forEach>
                        </ul>
                </div>
                        </li>
                    </ul>
                </div>
                <div class="catalog_signupmenubox_right"></div>
        </c:when>
        <c:otherwise>
        <div class="catalog_signupmenubox_left"></div>
                <div class="catalog_signupmenubox_mid">
                  <ul>
                        <li class="catalog_signupmenubox_mid last"  id="language_selector"><span class="icon language"></span><span class="link"><spring:message code="page.title.select.language"/></span>
                         <div class="catalog_signupmenubox_popover" id="language_selector_dropdown" style="left: 30px;display: None">
                            <ul class="catalog_signuppopoverlist">
                      <c:forEach items="${supportedLocaleList}" var="locale" varStatus="status">
                        <li class="language_select_option" id="<c:out value="${locale.key}"></c:out>"><span class="flagicons <c:out value="${locale.key}"></c:out>"></span><span class="languagelist"><c:out value="${locale.value}"></c:out></span></li>
                          </c:forEach>
                        </ul>
                </div>
                        </li>
                    </ul>
                </div>
                <div class="catalog_signupmenubox_right"></div>
        </c:otherwise>
</c:choose>
    
    </c:if>
   </div>
    </div>
      <div class="login_contentpanel  <c:if test="${directoryServiceAuthenticationEnabled || useSmallCss}">smaller_ver</c:if>">
        <tiles:insertAttribute name="header" ignore="true" />
        <div class="login_container  <c:if test="${directoryServiceAuthenticationEnabled || useSmallCss}">smaller_ver</c:if>">
          <tiles:insertAttribute name="body"/>
        </div>
        <div id="footer_login">
          <tiles:insertAttribute name="footer" ignore="true" />
        </div>
      </div>
      <div class="clearboth">
      </div>
      </div>
  </body>
</html>

