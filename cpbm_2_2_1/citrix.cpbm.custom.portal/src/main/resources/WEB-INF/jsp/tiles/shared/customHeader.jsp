<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. --> 
   <%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
   <link href="/portal/portal/spfavicon" type="image/x-icon" rel="shortcut icon" />
   <script language="javascript">
   if( typeof unsupportedDictionary === 'undefined' ) {
		    var unsupportedDictionary = {};
	 }
   unsupportedDictionary = {    
    header: '<spring:message javaScriptEscape="true" code="browser.unsupported.header"/>',
    paragraph1: '<spring:message javaScriptEscape="true" code="browser.unsupported.paragraph1"/>',
    paragraph2: '<spring:message javaScriptEscape="true" code="browser.unsupported.paragraph2"/>'
   };
   </script>
   <script type="text/javascript" src="/portal/js/appIE6reject.js"></script>
