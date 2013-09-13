<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<script language="javascript">
if( typeof i18n === 'undefined' ) {
  var i18n = {};
}
if( typeof i18n.errors === 'undefined' ) {
  i18n.errors = {};
}

i18n.errors.trialtoken = {
	compaignCodeReq : '<spring:message javaScriptEscape="true" code="js.errors.trialtoken.compaign.code.required"/>',
	nameReq : '<spring:message javaScriptEscape="true" code="js.errors.trialtoken.name.required"/>',
	companyNameReq : '<spring:message javaScriptEscape="true" code="js.errors.trialtoken.company.name.required"/>',
	emailAddressNeed : '<spring:message javaScriptEscape="true" code="js.errors.trialtoken.email.address.need"/>',
	emailAddressInvalid : '<spring:message javaScriptEscape="true" code="js.errors.trialtoken.email.address.invalid"/>',
	phoneNumReq : '<spring:message javaScriptEscape="true" code="js.errors.trialtoken.phone.number"/>',
	currencySel : '<spring:message javaScriptEscape="true" code="js.errors.trialtoken.currency.select"/>',
	
	
};
</script>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/campaign.js"></script>
<script type="text/javascript" src="<%=request.getContextPath() %>/js/trialtoken.js"></script>
