<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<script type="text/javascript">
  $(document).ready(function() {

    $("#paymentForm").validate({
      //debug : false,
      success : "valid",
      ignoreTitle : true,
      rules : {
        "payAmount" : {
          required : true,
          twoDecimal : true
        }
      },
      messages : {
        "payAmount" : {
          twoDecimal : i18n.errors.priceRequired,
          required : i18n.errors.invoicePayAmount 
        }

      },
      errorPlacement : function(error, element) {
        var name = element.attr('id');
        name = ReplaceAll(name, ".", "\\.");
        if (error.html() != "") {
          error.appendTo("#" + name + "Error");
        }
      }
    });
  });
</script>

<div class="dialog_formcontent">
	<form:form id="paymentForm">
		<div class="db_gridbox_columns" style="width: 30%;">
			<div class="db_gridbox_celltitles details">
				<strong><spring:message code="label.record.payment.invoice.payment.amount" /></strong>
			</div>
		</div>
		<div class="db_gridbox_columns" style="width: 45%;">
			<p style="float: left; margin-top: 10px; color: black">
				<c:out value="${salesLedgerRecord.tenant.currency.sign}" />
			</p>
			<input class="text" id="payAmount" name="payAmount"/>
		</div>
		<div class="main_addnew_formbox_errormsg" id="payAmountError" style="margin: 10px 0px 0 120px; width: 60%"></div>
	</form:form>
</div>