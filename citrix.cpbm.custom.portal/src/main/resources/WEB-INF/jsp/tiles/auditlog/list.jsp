<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>


 
<div class="members_box">
               	  <div id="maincontent_title">
                        <div class="maintitle_icon"> <img src="<%=request.getContextPath() %>/images/portal/membertitle_icons.gif" alt="dashboard title" /></div>
                        <h1>Audit Log</h1>
                    
                        
                      <div class="maintitle_actionarea">
                            
                      </div>
                	</div>

<div class="grid_container">
  <div class="grid_header">
    <div class="grid_genheader_cell" style="width:15%">
      <div class="grid_headertitles">Creation Date</div>
    </div>
    
    <div class="grid_genheader_cell" style="width:15%">
      <div class="grid_headertitles">User</div>
    </div>
    <div class="grid_genheader_cell" style="width:14%">
      <div class="grid_headertitles">Action</div>
    </div>
    <div class="grid_genheader_cell" style="width:55%">
      <div class="grid_headertitles">State</div>
    </div>
    
  </div>
  
   
  <div class="grid_content">
	<c:forEach var="auditLog" items="${auditLogs}" varStatus="status">
	  <c:choose>
	    <c:when test="${status.index % 2 == 0}">
	      <c:set var="rowClass" value="smallrow_odd"/>
	    </c:when>
	    <c:otherwise>
          <c:set var="rowClass" value="smallrow_even"/>
	    </c:otherwise>
	  </c:choose>
      <div class="<c:out value="gridrow ${rowClass}"/>" style="height:58px;">
       
        <div class="gridrow_cell" style="width:15%">
  	      <div class="grid_celltitles">
  	      <spring:message code="dateonly.format" var="dateonly_format"/>
          <fmt:formatDate value="${auditLog.creationDate}" pattern="${dateonly_format}" timeZone="${currentUser.timeZone}"/>
          </div>
  	    </div>
  	     <div class="gridrow_cell" style="width:15%">
  	      <div class="grid_celltitles">
            <c:out value="${auditLog.user.username}"/>
  	      </div>
  	    </div>
  	    
  	    <div class="gridrow_cell" style="width:14%">
  	      <div class="grid_celltitles">
  	         <c:out value="${auditLog.action}"/>
  	      </div>
  	    </div>
  	    
  	    <div class="gridrow_cell" style="width:55%">
  	      <div class="grid_celltitles" title='<c:out value="${auditLog.currentState}"/>'>
            <c:out value="${auditLog.currentState}"/>
  	      </div>
  	    </div>
  	      </div>
	</c:forEach>
  </div>
</div>
</div>
