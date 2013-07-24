<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:choose>
<c:when test ="${isTokenAvailable != NULL && isTokenAvailable == 'Y'}">
<div style="margin-left:15px;">
<h3>Congratulations!
</h3>
<p>
A trial code is available for use and has been sent to your email address. 
Please open your email now and click on the link to activate your code within 24 hours. 
The trial code is worth $20 and will expire within one week of activation
</p>
</div>
</c:when>
<c:otherwise>
<div style="margin-left:15px;">
<h3>Sorry - No Codes Available

</h3>
<p>
At this time, all trial codes have been taken. 
We will send you an email as soon as a code becomes available for your use. 
Thank you for your interest.

</p>
</div>
</c:otherwise>
</c:choose>
