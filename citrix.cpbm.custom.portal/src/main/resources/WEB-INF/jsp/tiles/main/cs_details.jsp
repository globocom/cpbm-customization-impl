<%-- Copyright (C) 2013 Citrix Systems, Inc. All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<script language="javascript">
  var dictionary = {viewMasked: '<spring:message javaScriptEscape="true" code="label.show"/>',
        hideMasked: '<spring:message javaScriptEscape="true" code="label.hide"/>'};
</script>

<script type="text/javascript" src="<%=request.getContextPath() %>/js/csInstanceAdd.js"></script>
<style type="text/css">
.tooltip {
    background-color:#CCCCCC;
    border:1px solid #fff;
    padding:10px 15px;
    width:280px;
    display:none;
    color:#fff;
    text-align:left;
    font-size:12px;
}
</style>
<div class="dialog_formcontent ">
  <div class="dialog_detailsbox" style="padding:0px;">
    <div class="mainbox">
      <div class="logobox"><img src="/portal/portal/logo/connector/${service.uuid}/logo"/></div>
      <div class="descriptionbox">
        <h2><spring:message  code="${service.serviceName}.service.name"/></h2>
        <span class="detail1"><spring:message  code="label.category"/>: <spring:message code="${service.serviceName}.service.category"/></span>
        <p><spring:message  code="${service.serviceName}.service.description"/></p>
      </div>
    </div>
  </div>
</div>
