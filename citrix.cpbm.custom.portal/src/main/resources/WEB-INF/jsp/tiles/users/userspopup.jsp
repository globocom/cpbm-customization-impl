<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ page import="com.vmops.model.Tenant" %>
<script language="javascript">
  var i18n = {
          user: {
            delfail: '<spring:message javaScriptEscape="true" code="js.user.del.fail"/>',
            deactfail: '<spring:message javaScriptEscape="true" code="js.user.deact.fail"/>',
            actfail: '<spring:message javaScriptEscape="true" code="js.user.act.fail"/>',
            channel: '<spring:message javaScriptEscape="true" code="js.user.channel"/>',
            title: '<spring:message javaScriptEscape="true" code="js.user.title"/>',
          del : '<spring:message javaScriptEscape="true" code="js.user.del.confirm"/>',
          deact: '<spring:message javaScriptEscape="true" code="js.user.deact.confirm"/>',
          act: '<spring:message javaScriptEscape="true" code="js.user.act.confirm"/>',
          delproject : '<spring:message javaScriptEscape="true" code="js.user.del.confirmproject"/>',           
          max : '<spring:message javaScriptEscape="true" code="js.user.max"/>',
          cloudstorage: '<spring:message javaScriptEscape="true" code="js.user.cloudstorage.subscribe"/>',
          profile: '<spring:message javaScriptEscape="true" code="js.user.profile.edit"/>',
          firstname: '<spring:message javaScriptEscape="true" code="js.user.firstname"/>',
          lastname: '<spring:message javaScriptEscape="true" code="js.user.lastname"/>',
          email: '<spring:message javaScriptEscape="true" code="js.user.email"/>',
          emailmatch: '<spring:message javaScriptEscape="true" code="js.user.email.match"/>',
          confirmemail: '<spring:message javaScriptEscape="true" code="js.user.confirmemail"/>',
          emailformat: '<spring:message javaScriptEscape="true" code="js.user.email.format"/>',
          username: '<spring:message javaScriptEscape="true" code="js.user.username"/>',
          usernameexists: '<spring:message javaScriptEscape="true" code="js.user.username.exists"/>',
          password: '<spring:message javaScriptEscape="true" code="js.user.password"/>',
          passwordconfirm: '<spring:message javaScriptEscape="true" code="js.user.password.confirm"/>',
          passwordmatch: '<spring:message javaScriptEscape="true" code="js.user.password.match"/>',
          profilerequired: '<spring:message javaScriptEscape="true" code="js.user.profile.required"/>',
          passwordequsername: '<spring:message javaScriptEscape="true" code="js.user.passwordequsername"/>'
          }
    };
</script>
<input id="tenantId" type="hidden" name="tenant" value="<c:out value="${tenant.param}"/>"/>

<script type="text/javascript">
 var usersUrl = "<%=request.getContextPath() %>/portal/users/";
<%String addNewUserUrll = (String)request.getContextPath()+"/portal/users/new/step1?tenant="+(String)(((Tenant)request.getAttribute("tenant")).getParam());%>
var addNewUserUrl="<%=addNewUserUrll %>";
</script>
<script type="text/javascript" src="<%=request.getContextPath() %>/resources/app.js"></script>

<link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/css/jquery/themes/base/jquery-ui-1.7.2.custom.css"/>
<div class="menu_topquicklinks_container">
      <div class="menu_topquicklinks_box">
        <p><strong><spring:message code="ui.accounts.popup.label.quicklinks"/></strong></p>
        <p><a href="/portal/portal/users/listusersforaccount"><spring:message code="page.level2.allusers"/></a></p>
      </div>
     <c:if test="${tenant.state eq 'ACTIVE'}">
      <sec:authorize access="hasAnyRole('ROLE_USER_CRUD','ROLE_ACCOUNT_USER_CRUD')">
         <div class="menu_topaddnew_box">
            <spring:url value="/portal/users/listusersforaccount" var="userslink" htmlEscape="false">
              <spring:param name="showAddNewUserWizard"><c:out value="true"/></spring:param>
            </spring:url>
            <a class="selection_commonbutton" href="${userslink}"><spring:message code="ui.users.all.newuser"/></a>
         </div>
      </sec:authorize>
     </c:if> 
  </div>
    <div class="menu_dropdown_contentarea">
       <div class="menu_dropdown_selectioncontainer">
         <div class="menu_dropdown_selectioncontainer_top">
         
          <div class="menu_dropdown_searchbox" style="border:0px;">
            <!--  
            <div class="menu_dropdown_searchicon"></div>
              <input class="text" type="text" />
             -->
            </div>
            
            <div class="menu_dropdown_tabtext" ><spring:message code="ui.accounts.popup.title.${charRange}" /></div>
        </div>
        <div class="menu_dropdown_selectioncontainer_mid">
          <div class="menu_dropdown_selectionlistbox">
              <c:forEach items="${users}" var="user" varStatus="status">
                   <div class="menu_dropdown_selectionlist">
                    <spring:url value="/portal/users/listusersforaccount?user={userParam}" var="account_path">
                      <spring:param name="userParam"><c:out value="${user.param}"/></spring:param>
                    </spring:url>
                    <a href="<c:out value="${account_path}"/>"><c:out value="${user.firstName} ${user.lastName}"/> </a>
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
              onclick="getCurrentPageUsers(<c:out value="${prevPage}" />,'<c:out value="${size}" />','<c:out value="${charRange}" />');">
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
            onclick="getCurrentPageUsers(<c:out value="${nextPage}" />,'<c:out value="${size}" />','<c:out value="${charRange}" />');">
            </a>
          </c:otherwise>
        </c:choose>
                
            </div>
        </div>
    </div> 
            
    <div class="menu_dropdown_sidefiltersbox">
      <ol>
        <li <c:if test="${charRange!='All'}">style='background:none;'</c:if>>
          <a href="#" <c:if test="${charRange=='All'}">class=selected</c:if>
            onclick="getCurrentPageUsers(1,null,'All');"><spring:message code="ui.accounts.popup.title.All"/></a>
        </li>
        <li <c:if test="${charRange!='A-D'}">style='background:none;'</c:if>>
          <a href="#" <c:if test="${charRange=='A-D'}">class=selected</c:if>
            onclick="getCurrentPageUsers(1,null,'A-D');"><spring:message code="ui.accounts.popup.title.A-D"/></a>
        </li>
        <li <c:if test="${charRange!='E-H'}">style='background:none;'</c:if>>
          <a href="#"  <c:if test="${charRange=='E-H'}">class=selected</c:if>
            onclick="getCurrentPageUsers(1,null,'E-H');"><spring:message code="ui.accounts.popup.title.E-H"/></a>
        </li>
        <li <c:if test="${charRange!='I-L'}">style='background:none;'</c:if>>
          <a href="#"  <c:if test="${charRange=='I-L'}">class=selected</c:if>
            onclick="getCurrentPageUsers(1,null,'I-L');"><spring:message code="ui.accounts.popup.title.I-L"/></a>
        </li>
        <li <c:if test="${charRange!='M-P'}">style='background:none;'</c:if>>
          <a href="#" <c:if test="${charRange=='M-P'}">class=selected</c:if>
            onclick="getCurrentPageUsers(1,null,'M-P');"><spring:message code="ui.accounts.popup.title.M-P"/></a>
        </li>
        <li <c:if test="${charRange!='Q-T'}">style='background:none;'</c:if>>
          <a href="#" <c:if test="${charRange=='Q-T'}">class=selected</c:if>
            onclick="getCurrentPageUsers(1,null,'Q-T');"><spring:message code="ui.accounts.popup.title.Q-T"/></a>
        </li>
        <li <c:if test="${charRange!='U-Z'}">style='background:none;'</c:if>>
          <a href="#"  <c:if test="${charRange=='U-Z'}">class=selected</c:if>
            onclick="getCurrentPageUsers(1,null,'U-Z');"><spring:message code="ui.accounts.popup.title.U-Z"/></a>
        </li>
        <li <c:if test="${charRange!='Other'}">style='background:none;'</c:if>>
          <a href="#" <c:if test="${charRange=='Other'}">class=selected</c:if>
            onclick="getCurrentPageUsers(1,null,'Other');"><spring:message code="ui.accounts.popup.title.Other"/></a>
        </li>
      </ol> 
    </div>
  </div>
   <input id="isUsersMaxReached" name="isUsersMaxReached" type="hidden" value="<c:out value="${isUsersMaxReached}" />" />  
   <div id="userMaxDiv" class="userMaxDiv" style="display:none;">
    
     <div class="dialog_formcontent">
       <div style="float:left; width:320px;">
      <p style="color: #111111"><spring:message code="max.users.reached.popup.message"></spring:message></p>
     </div>
    
      </div>  
             
      </div>  
  
