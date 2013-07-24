<%-- Copyright (C) 2013 Citrix Systems, Inc. All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>  
    <div class="left_filtermenu">
      <span class="title"><spring:message  code="label.category"/></span>
      <div class="filterlist">
        <ul>
          <li>
            <span class="filters">
              <a href="javascript:void(0);" class="filters selected" id="All"><spring:message  code="label.all"/> (${countPerCategory['All']})</a>
            </span>
          </li>
          <c:forEach var="category" items="${categories}" varStatus="status">
            <li>
              <span class="filters">
                <a href="javascript:void(0);" class="filters" id="${category}"><spring:message  code="${category}.category.text"/> 
                <c:choose>
                    <c:when test="${!empty countPerCategory[category]}">
                        (${countPerCategory[category]})
                    </c:when>
                    <c:otherwise>
                        (0)
                    </c:otherwise>
                </c:choose>
                
                </a>
              </span>
            </li>
          </c:forEach>
        </ul>
      </div>
    </div>