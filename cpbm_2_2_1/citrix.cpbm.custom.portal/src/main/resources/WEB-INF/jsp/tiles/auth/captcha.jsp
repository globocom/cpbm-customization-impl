<!-- Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. -->
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<script> 
  var RecaptchaOptions = {
    custom_translations : Recaptcha_Strings,
    theme : 'custom',
    custom_theme_widget: 'recaptcha_widget'
  };
</script>


<div id="recaptcha_widget" style="display: none"> 

  <label for="recaptcha_response_field" class="recaptcha_only_if_image"><spring:message code="label.captcha.type"/></label> 
  <label for="recaptcha_response_field" class="recaptcha_only_if_audio"><spring:message code="label.captcha.hear"/></label>
  <div class="clearboth"></div>              
  <div class="captchaInput">
    <input type="text" id="recaptcha_response_field" class="text" name="recaptcha_response_field" tabindex="3" /> 
    <div id="recaptcha_image" style="margin: 5px 0px 0px 0px;"></div>
    <div style="float: left;"><p id="recaptcha_disclaimer" style="margin-left: 0px;"><spring:message code="label.captcha.poweredBy"/><a href="http://www.google.com/recaptcha" target="_blank"><spring:message code="label.captcha.recaptcha"/></a></p></div> 
  </div>
  
  <div id="recaptcha_response_field_status" class="message" style="margin: 6px 0px 0px 0px;"> 
    <spring:message code="label.captcha.trouble"/>    
    <ul class="recaptcha_options"> 
      <li class="recaptcha_option_refresh"><a href="javascript:Recaptcha.reload()"><spring:message code="label.captcha.refresh"/></a></li> 
      <li class="recaptcha_only_if_image recaptcha_option_audio"><a href="javascript:Recaptcha.switch_type('audio')"><spring:message code="label.captcha.listen"/></a></li> 
      <li class="recaptcha_only_if_audio recaptcha_option_image"><a href="javascript:Recaptcha.switch_type('image')"><spring:message code="label.captcha.image"/></a></li> 
      <li class="recaptcha_option_help"><a href="javascript:Recaptcha.showhelp()"><spring:message code="label.captcha.help"/></a></li> 
    </ul> 
  </div>

</div>
<script src="https://www.google.com/recaptcha/api/challenge?k=<c:out value="${recaptchaPublicKey}"/>"></script>

<noscript> 
  <iframe src="https://www.google.com/recaptcha/api/noscript?k=<c:out value="${recaptchaPublicKey}"/>" height="300" width="500" frameborder="0"></iframe><br>
    <textarea name="recaptcha_challenge_field" rows="3" cols="40"></textarea>
    <input type="hidden" name="recaptcha_response_field" value="manual_challenge" />
</noscript> 
