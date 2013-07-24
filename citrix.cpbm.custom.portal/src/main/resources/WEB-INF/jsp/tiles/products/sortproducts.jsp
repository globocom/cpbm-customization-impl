<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/products.js"></script>
<div class="dialog_formcontent wizard">
   <span class="helptext">
   <spring:message code="ui.product.sort.help.text1"></spring:message>&nbsp;&nbsp;&nbsp;<span class="moveicon_description"></span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
   <spring:message code="ui.product.sort.help.text2"></spring:message>
    </span>
   <form id="productOrder">
            <input type="hidden" name="productOrderData" id="productOrderData"/>
   </form> 
   <div class="widgetwizard_selectionbox sortproduct" style="margin-bottom: 20px;"  >
       <ul class="ui-sortable" id="sortproductslist">
           <c:forEach items="${productsList}" var="product" varStatus="productstatus">
           
             <li class="widgetwizard_selectionbox" style="cursor: pointer;" id="<c:out value="sort${product.id}"/>" >
                 <span class="description"><c:out value="${product.name}" /> </span>
                 <span class="movebox">
                     <span class="moveicon"></span>
                 </span>
             </li>
           </c:forEach>
          
         </ul>
     </div>
</div>