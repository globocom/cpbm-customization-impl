<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>	
<c:if test="${size > 10 }">
<div class="pagination_actionbox">  
   	
   	<c:choose>
  	<c:when test="${currentPage==1}">
  		<span>&lt;&lt;</span>
  	</c:when>
  	<c:otherwise>
  	<a href="#" onclick="return getCurrentPageData(this,1,'<c:out value="${size}" />','<c:out value="${url}" />');" style="text-decoration: none;">
   		<span>&lt;&lt;</span>
   	</a>
  	</c:otherwise>
  	</c:choose>
   	<c:choose>
  	<c:when test="${currentPage==1}">
  		<span>Prev</span>
  	</c:when>
  	<c:otherwise>
  	<a href="#" onclick="return getCurrentPageData(this,<c:out value="${prevPage}" />,'<c:out value="${size}" />','<c:out value="${url}" />');">
   		<span>Prev</span>
   	</a>  
  	</c:otherwise>
  	</c:choose>
   	 	
  	<c:forEach begin="${lowerBound}" end="${upperBound}"  var="status">
  		<a href="#" onclick="return getCurrentPageData(this,<c:out value="${status}" />,'<c:out value="${size}" />','<c:out value="${url}" />');">  		
  		<span <c:if test="${status==currentPage }">class='current'</c:if>><c:out value="${status}"></c:out></span>
  		</a>
  	</c:forEach>
  	<c:choose>
  	<c:when test="${currentPage==totalpages}">
  		<span>Next</span>
  	</c:when>
  	<c:otherwise>
  	<a href="#" onclick="return getCurrentPageData(this,<c:out value="${nextPage}" />,'<c:out value="${size}" />','<c:out value="${url}" />');">
  		<span>Next</span>
  	</a>
  	</c:otherwise>
  	</c:choose>
  	<c:choose>
  	<c:when test="${currentPage==totalpages}">
  		<span>&gt;&gt;</span>
  	</c:when>
  	<c:otherwise>
  		<a href="#" onclick="return getCurrentPageData(this,<c:out value="${totalpages}" />,'<c:out value="${size}" />','<c:out value="${url}" />');" style="text-decoration: none;">
  		<span>&gt;&gt;</span>
  	</a>
  	</c:otherwise>
  	</c:choose>
  	
  
</div>
<div class="noofpages">
	<label for="perPage" >Show</label>
	<select name="perPage" id="perPage" style="width: 50px;" >
		<c:forEach begin="5" end="55" step="5" var="optionValue">
			<option value="<c:out value="${optionValue}" />"
			<c:if test="${perPage == optionValue}">selected='selected'</c:if> 
			>
			<c:out value="${optionValue}" /></option>	
		</c:forEach>
	</select>
	<span>per page</span>
</div>
</c:if>
