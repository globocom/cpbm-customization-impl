<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page import="com.vmops.web.forms.UserForm"  %>
<%@ page import="java.util.Map.Entry"  %>
<%@ page import="java.io.File"  %>
<%@ page import="java.net.URI"  %>
<%
UserForm form = (UserForm)(request.getAttribute("user"));
for (Entry<String, String> cf: form.getUser().getCustomFieldMap().entrySet()) {
  String filename = "../../../custom/jsp/tiles/users/myprofile.customfield." + cf.getKey() + ".jsp";
  %>
  <c:catch var="e">
    <jsp:include page="<%=filename%>"></jsp:include>  
  </c:catch>
  <%--   
  <c:if test="${!empty e}">
    Error: <c:out value="${e.message}"/>
  </c:if>
  --%>  
  <%
}
%>
<!-- 
<c:forEach items="${userObj.customFieldMap}" var="customField">
..................
  <c:set var="fieldName" value="<c:out value='${customField.key}'/>"></c:set>
  <c:catch var="e">
    <c:set var="userObj" value="${userObj}" scope="request"></c:set>
    <jsp:include page="${fieldName}"></jsp:include>  
  </c:catch>
  <c:if test="${!empty e}">
    Error: <c:out value="${e.message}"/>
</c:if>

</c:forEach>
 -->
