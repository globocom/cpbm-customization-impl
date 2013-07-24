<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<script type="text/javascript" src="<%=request.getContextPath() %>/js/resourceviewtab.js"></script>

  
    
  <%-- <input type="hidden" id="l3_tenant_param" value='<c:out value="${tenant.param}"/>'/>
   --%>
  <c:choose> 
      <c:when test="${fn:length(resourceViews) gt 1}">
          <script type="text/javascript" src="<%=request.getContextPath() %>/js/thirdlevelmenuSlide.js"></script> 
          <div class='slider thirdlevel_subsubmenu' >

      </c:when> 
      <c:otherwise> 
          
          <div class='slider thirdlevel_subsubmenu' style="display:none;">
      </c:otherwise> 
  </c:choose>
  
    <div class="thirdlevel_subsubmenu left"></div>
      <div class="thirdlevel_subsubmenu mid">
      <div class='thirdlevel_slidingbutton prev' style="display:none;"></div>
      <div class='thirdlevel_slidingbutton next' style="display:none;"></div>
        <div id="items_container"> 
          <ul>
              <c:forEach items="${resourceViews}" var="resourceViewItem" varStatus="status">
                  <c:choose> 
                      <c:when test="${status.index == 0}"> 
                          <c:set var="tabStatus" value="on"/> 
                      </c:when> 
                      <c:otherwise> 
                          <c:set var="tabStatus" value="off"/> 
                      </c:otherwise> 
                  </c:choose>
              <li class="thirdlevel_subtab big ${tabStatus}" id="l3_tab_<c:out value='${resourceViewItem.name}'/>" resourceUrl="<c:out value='${resourceViewItem.URL}'/>" mode="<c:out value='${resourceViewItem.mode}'/>" >
                <div><c:if test="${not empty resourceViewItem.icon}"><img src="${resourceViewItem.icon}"/></c:if></div>
                <p><c:out value="${resourceViewItem.name}" /></p>
              </li>
            </c:forEach>
           </ul>
       </div>
    </div>
    <div class="thirdlevel_subsubmenu right"></div>
  </div>
