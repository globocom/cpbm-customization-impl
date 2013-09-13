<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<div class="menu_topquicklinks_container">
  <div class="menu_topquicklinks_box">
    <p><strong><spring:message code="ui.accounts.popup.label.quicklinks"/></strong></p>
    <p style="padding-left:0px;">
    <a href="/portal/portal/tenants/list"><spring:message code="page.level2.allaccounts"/></a>
    <sec:authorize access="hasAnyRole('ROLE_ACCOUNT_CRUD','ROLE_ACCOUNT_MGMT')">
      <spring:url value="/portal/tenants/list?accountType=" var="tenants_list" htmlEscape="false">
        <spring:param name="tenant"><c:out value="${tenant.param}"/></spring:param>
      </spring:url>
      <c:forEach items="${accountTypes}" var="choice" varStatus="status">
        <c:if test="${choice.id != 1}">
          <spring:url value="/portal/tenants/list" var="tenants_list" htmlEscape="false">
            <spring:param name="tenantUUId"><c:out value="${tenant.param}"/></spring:param>
            <spring:param name="accountType"><c:out value="${choice.id}"/></spring:param>
          </spring:url>
          <a href="<c:out value="${tenants_list}"/>"><spring:message code="page.level2.${choice.nameLower}"/></a>
        </c:if>
      </c:forEach>
      
    </sec:authorize>
    </p>
  </div>
  <sec:authorize access="hasRole('ROLE_ACCOUNT_CRUD')">
  <div class="menu_topaddnew_box">
    <spring:url value="/portal/tenants/list" var="tenants_list" htmlEscape="false">
      <spring:param name="accountType"><c:out value=""/></spring:param>
      <spring:param name="showAddAccountWizard"><c:out value="true"/></spring:param>
    </spring:url>
    <a class="selection_commonbutton" href="${tenants_list}"><spring:message code="ui.accounts.all.newaccount"/></a>
  </div>
  </sec:authorize>
</div>
  <div class="menu_dropdown_contentarea">
       <div class="menu_dropdown_selectioncontainer">
         <div class="menu_dropdown_selectioncontainer_top">
          <div class="menu_dropdown_searchbox">
            <div class="menu_dropdown_searchicon"></div>
              <input class="text" value='<c:out value="${pattern}" />' id="searchaccount" type="text"  onkeyup="searchaccounts(event,this,'<c:out value="${charRange}" />');"/>
            </div>
            <div class="menu_dropdown_tabtext" ><spring:message code="ui.accounts.popup.title.${charRange}" /></div>
        </div>
        <div id="accountslist">
        <div class="menu_dropdown_selectioncontainer_mid">
          <div class="menu_dropdown_selectionlistbox">
              <c:forEach items="${tenants}" var="tenant" varStatus="status">
                   <div class="menu_dropdown_selectionlist">
                    <spring:url value="/portal/tenants/list" var="account_path" htmlEscape="false">
                      <spring:param name="tenantUUId"><c:out value="${tenant.param}"/></spring:param>
                    </spring:url>
                    <a class="ellipsis" href="<c:out value="${account_path}"/>" title="<c:out value="${tenant.name}"/>"><c:out value="${tenant.name}"/> </a>
                   </div>
              </c:forEach>
        </div>
        </div>
        <div class="menu_dropdown_selectioncontainer_bot">
          <div class="menu_dropdown_prevnxtbox">
          
          
          <c:choose>
            <c:when test="${currentPage==1}">
            <div class="menu_dropdown_previcon" ></div>
            </c:when>
           <c:otherwise>
              <a href="#" class="menu_dropdown_previcon"  
              onclick="getCurrentPageAccounts(<c:out value="${prevPage}" />,'<c:out value="${size}" />','<c:out value="${charRange}" />','<c:out value="${pattern}" />');">
               </a>
          </c:otherwise>
          </c:choose>
          <p><spring:message code="ui.accounts.popup.pageofpages" arguments="${currentPage}, ${totalpages}"/></p>
                
        <c:choose>
          <c:when test="${currentPage==totalpages}">
            <div class="menu_dropdown_nexticon"></div>
          </c:when>
        <c:otherwise>
            <a href="#" class="menu_dropdown_nexticon"  
            onclick="getCurrentPageAccounts(<c:out value="${nextPage}" />,'<c:out value="${size}" />','<c:out value="${charRange}" />','<c:out value="${pattern}" />');">
            </a>
          </c:otherwise>
        </c:choose>
                
            </div>
        </div>
      </div>
    </div> 
            
    <div class="menu_dropdown_sidefiltersbox">
      <ol>
        <li <c:if test="${charRange!='All'}">style='background:none;'</c:if>>
          <a href="#" <c:if test="${charRange=='All'}">class=selected</c:if>
            onclick="getCurrentPageAccounts(1,null,'All','<c:out value="${pattern}"/>');"><spring:message code="ui.accounts.popup.title.All"/></a>
        </li>   
        <li <c:if test="${charRange!='A-D'}">style='background:none;'</c:if>  >
          <a href="#" <c:if test="${charRange=='A-D'}">class=selected</c:if>
            onclick="getCurrentPageAccounts(1,null,'A-D','<c:out value="${pattern}"/>');"><spring:message code="ui.accounts.popup.title.A-D"/></a>
        </li>
        <li <c:if test="${charRange!='E-H'}">style='background:none;'</c:if> >
          <a href="#"  <c:if test="${charRange=='E-H'}">class=selected</c:if>
            onclick="getCurrentPageAccounts(1,null,'E-H','<c:out value="${pattern}"/>');"><spring:message code="ui.accounts.popup.title.E-H"/></a>
        </li>
        <li <c:if test="${charRange!='I-L'}">style='background:none;'</c:if>>
          <a href="#"  <c:if test="${charRange=='I-L'}">class=selected</c:if>
            onclick="getCurrentPageAccounts(1,null,'I-L','<c:out value="${pattern}"/>');"><spring:message code="ui.accounts.popup.title.I-L"/></a>
        </li>
        <li <c:if test="${charRange!='M-P'}">style='background:none;'</c:if> >
          <a href="#" <c:if test="${charRange=='M-P'}">class=selected</c:if>
            onclick="getCurrentPageAccounts(1,null,'M-P','<c:out value="${pattern}"/>');"><spring:message code="ui.accounts.popup.title.M-P"/></a>
        </li>
        <li <c:if test="${charRange!='Q-T'}">style='background:none;'</c:if>>
          <a href="#" <c:if test="${charRange=='Q-T'}">class=selected</c:if>
            onclick="getCurrentPageAccounts(1,null,'Q-T','<c:out value="${pattern}"/>');"><spring:message code="ui.accounts.popup.title.Q-T"/></a>
        </li>
        <li <c:if test="${charRange!='U-Z'}">style='background:none;'</c:if>>
          <a href="#"  <c:if test="${charRange=='U-Z'}">class=selected</c:if>
            onclick="getCurrentPageAccounts(1,null,'U-Z','<c:out value="${pattern}"/>');"><spring:message code="ui.accounts.popup.title.U-Z"/></a>
        </li>
        <li <c:if test="${charRange!='0-9'}">style='background:none;'</c:if>>
          <a href="#" <c:if test="${charRange=='0-9'}">class=selected</c:if>
            onclick="getCurrentPageAccounts(1,null,'0-9','<c:out value="${pattern}"/>');"><spring:message code="ui.accounts.popup.title.0-9"/></a>
        </li>
        <li <c:if test="${charRange!='Other'}">style='background:none;'</c:if>>
          <a href="#" <c:if test="${charRange=='Other'}">class=selected</c:if>
            onclick="getCurrentPageAccounts(1,null,'Other','<c:out value="${pattern}"/>');"><spring:message code="ui.accounts.popup.title.Other"/></a>
        </li>
      </ol> 
    </div>
  </div>
