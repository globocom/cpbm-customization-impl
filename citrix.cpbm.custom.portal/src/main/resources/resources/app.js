
var CODE_NOT_UNIQUE_ERROR_CODE=601;var AJAX_FORM_VALIDATION_FAILED_CODE=420;var INVALID_AJAX_REQUEST_ERROR_CODE=420;var PAGINATION_PAGE_SIZE=13;var PAGINATION_CURRENT_PAGE_NUMBER=1;var INTERNAL_SERVER_FAILED_CODE=500;var PRECONDITION_FAILED=412;$(document).ready(function(){if($("#from_cas_login_page").val()=='true'){$.ajax({type:"GET",url:"/portal/portal/getSupportedLanguages",async:false,dataType:"json",success:function(response){var htmlToPush='';for(var key in response){htmlToPush=htmlToPush
+'<li class="language_select_option" id='
+key+'><span class="flagicons '+key
+'"></span><span class="languagelist">'
+response[key]+'</span></li>';}
$(".catalog_signuppopoverlist").html(htmlToPush);$(".language_select_option").bind('click',function(event){reload_in_another_language($(this));});}});$.ajax({type:"GET",url:"/portal/portal/isShowAnnonymousCatalog",async:false,dataType:"json",success:function(shouldShow){if(shouldShow==true){$("#language_selector_dropdown").css("left","162px");$("#cas_login_view_anonymous_browsing").show();}else{$("#language_selector_dropdown").css("left","30px");$("#cas_login_view_anonymous_browsing").hide();}}});$.ajax({type:"GET",url:"/portal/portal/getLoginPageUIRelatedConfigs",async:false,dataType:"json",success:function(configs){if(configs.isDirectoryServiceAuthenticationON=="true"){$("#login_maintabsarea_div_id").hide();$("#login_main").attr('id','login_main_smaller_ver');$("#login_contentpanel_div_id").addClass('smaller_ver');$("#login_container_div_id").addClass('smaller_ver');$("#login_headerarea_div_id").addClass('smaller_ver');$("#login_maincontentarea_div_id").addClass('smaller_ver');$("#login_formbox_div_id").addClass('smaller_ver');$("#login_info_box").hide();$("#loginbg_bot_div_id").addClass('smaller_ver');$(".login_messages").addClass('smaller_ver');}
if(configs.showSuffix=="true"){$("#login_maintabsarea_div_id").hide();$("#dontHaveAnAccount_p_id").hide();$("#dontHaveAnAccount_a_id").hide();$("#showSuffixControlVar").val("true");$("#suffix_li").show();}
if(configs.showSuffixDropBox=="true"){var suffixList=configs.suffixList;var htmlToPush='';for(var index in suffixList){if(index==0){$("#suffix").val(suffixList[index]);}
htmlToPush=htmlToPush+'<option value="'+suffixList[index]+'">'+suffixList[index]+'</option>';}
$("#suffixDropdown").html(htmlToPush);$("#suffixDropdown").show();}
else{$("#suffix").show();}}});$.ajax({type:"GET",url:"/portal/portal/isSystemActive",async:false,dataType:"json",success:function(shouldShow){if(shouldShow==true){$("#login_info_box").show();$("#signup_tab").show();}else{$("#login_info_box").hide();$("#signup_tab").hide();}}});$("#login_main").show();$("#login_main_smaller_ver").show();}
if($("#from_login_page").val()=='true'){$.ajax({type:"GET",url:"/portal/portal/isSystemActive",async:false,dataType:"json",success:function(shouldShow){if(shouldShow==true){$("#login_info_box").show();$("#signup_tab").show();}else{$("#login_info_box").hide();$("#signup_tab").hide();}}});$("#login_main").show();$("#login_main_smaller_ver").show();}});$("#language_selector").live('mouseover',function(event){$("#language_selector_dropdown").show();});$("#language_selector").live('mouseout',function(event){$("#language_selector_dropdown").hide();});$("#browse_catalogs_li").live('click',function(event){var searchq=window.location.search;var lang="";if(searchq.indexOf("lang=")!=-1){lang=searchq.substring(searchq.indexOf("lang="));window.location="/portal/portal/catalog/browse_catalog?"+lang;}else{window.location="/portal/portal/catalog/browse_catalog";}});$(".language_select_option").live('click',function(event){reload_in_another_language($(this));});function reload_in_another_language(obj){if($("#from_cas_login_page").val()=='true'){$.ajax({type:"GET",url:"/portal/portal/setLocale?lang="+obj.attr('id'),async:false,dataType:"json",success:function(response){}});}
var searchq=window.location.search;var addr=window.location.href;var trimmedsearchq="";var loc="/portal/";if(addr.indexOf("loggedout")!=-1){window.location=loc+"?lang="+obj.attr('id');}else{if(searchq.indexOf("lang=")==-1){if(searchq=="")
window.location=window.location+"?lang="+obj.attr('id');else
window.location=window.location+"&lang="+obj.attr('id');}else{trimmedsearchq=searchq.substring(0,searchq.indexOf("lang="));if(searchq!="")
loc=window.location.href.substring(0,window.location.href.indexOf(searchq));window.location=loc+trimmedsearchq+"lang="+obj.attr('id');}}};$("#signup_login_li").live('click',function(event){var searchq=window.location.search;var lang="";if(searchq.indexOf("lang=")!=-1){lang=searchq.substring(searchq.indexOf("lang="));window.location="/portal/?"+lang;}else
window.location="/portal";});if(typeof String.prototype.trim!=='function'){String.prototype.trim=function(){return this.replace(/^\s+|\s+$/g,'');};}
jQuery.fn.extend({inputAutoTitles:function(options){this.find('input[type=text]').each(function(i){var title=$(this).attr('title');if($(this).val()==''){$(this).val(title).addClass('waiting').blur(function(){$(this).removeClass('active');if($(this).val()==''||$(this).val()==title)
$(this).addClass('waiting').val(title);}).focus(function(){$(this).removeClass('waiting').addClass('active');if($(this).val()==title)
$(this).val('');});}else{$(this).unbind('click');}});$(this).submit(function(){$(this).find('[type=text]').each(function(i){if($(this).attr('title')==$(this).val())
$(this).val('');});});}});jQuery.validator.addMethod("password",function(value,element){if(element.className=="text j_credit_card_cvv"||element.className=="text j_credit_card_cvv error"||element.className=="text j_credit_card_cvv valid")
return true;return this.optional(element)||value.length>=8&&/\d/.test(value)&&/[A-Z]/.test(value);},"8 characters, one number and one uppercase character required.");jQuery.validator.addMethod("notEqualTo",function(value,element,param){return(value!=$(param).val());},"equal");jQuery.validator.addMethod('noSpacesAllowed',function(value,element){return value.length>0&&/^[a-zA-Z0-9_:\[\]-]+$/.test(value);},"Only characters, digits and special symbols : - _ ] [ are allowed.");$.validator.addMethod("xRemote",function(value,element){var rule=this.settings.rules[element.name].xRemote;if(rule.condition&&$.isFunction(rule.condition)&&!rule.condition.call(this,element))
return"dependency-mismatch";return $.validator.methods.remote.apply(this,arguments);},"Code already in use");jQuery.validator.addMethod("phone",function(value,element){return this.optional(element)||value.length>0&&/^[\d-]+$/.test(value)&&value.replace(/[^\d]/g,"").length<13&&value.replace(/[^\d]/g,"").length>4;},"Invalid phone number.");jQuery.validator.addMethod("countryCode",function(value,element){return this.optional(element)||value.length>0&&/^\d+$/.test(value)&&value.length<4;},"Invalid country code.");jQuery.validator.addMethod("flname",function(value,element){var myRegexp=/([[\.,\/#@!$%\^&\*;:{}=_`?~(\\)\d]+)/;var match=myRegexp.exec(value);return this.optional(element)||value.length>0&&match==null;},"Only characters and special symbols ' - are allowed.");jQuery.validator.setDefaults({submitHandler:function(form){var submit=$(form).find(".submitmsg");if($(submit).attr("rel")!=null&&$(submit).attr("rel")!=""){$(submit).attr("value",$(submit).attr("rel"));$(submit).attr("disabled",true);}
if($(form).hasClass("ajaxform")==false){form.submit();}}});$.ajaxSetup({contentType:"application/x-www-form-urlencoded;charset=utf8",complete:function(xhr,status){if(xhr.status==401){window.location.reload(false);}
if(xhr.status==404){window.location="/portal/portal/errors/notfound";}
if(xhr.status==503){alert(xhr.responseText);}}});if(!Array.prototype.indexOf){Array.prototype.indexOf=function(elt){var len=this.length>>>0;var from=Number(arguments[1])||0;from=(from<0)?Math.ceil(from):Math.floor(from);if(from<0)
from+=len;for(;from<len;from++){if(from in this&&this[from]===elt)
return from;}
return-1;};}
function formUploadBeforeSubmit(form){var thisForm=$(form);var csrfToken=$('input[name$="OWASP_CSRFTOKEN"]',thisForm).val();if(csrfToken!=null){thisForm.attr('action',thisForm.attr('action')+"?OWASP_CSRFTOKEN="
+csrfToken);}}
$(document).ready(function(){$(".welcomeuser_panel").hover(function(){$("#welcome_menu").parent().removeClass("welcomebox_sideicon");$("#welcome_menu").parent().addClass("welcomebox_dropdown_arrow");$("#welcome_menu").show();},function(){$("#welcome_menu").parent().addClass("welcomebox_sideicon");$("#welcome_menu").parent().removeClass("welcomebox_dropdown_arrow");$("#welcome_menu").hide();});if($('.grid_content').length>0){$(".smallrow_odd, .smallrow_even").hover(function(){$(this).find('.grid_links_container').show();},function(){$(this).find('.grid_links_container').hide();});}});if(top.location!=location){top.location.href=document.location.href;}
function getCurrentPageData(current,currentPage,size,url){var perPage=$("#perPage").val();var url=url+"currentPage="+currentPage+"&perPage="+perPage
+"&size="+size;$(current).attr("href",url);return true;}
function ReplaceAll(Source,stringToFind,stringToReplace){var tempArr=Source.split(stringToFind);var dest="";for(var i=0;i<tempArr.length;i++){if(dest==""){dest=tempArr[i];}else{dest=dest.concat(stringToReplace);dest=dest.concat(tempArr[i]);}}
return dest;}
function sanitizeXSS(val){if(val==null||typeof(val)!="string")
return val;val=val.replace(/</g,"&lt;");val=val.replace(/>/g,"&gt;");return val;}
function noNull(val){if(val==null)
return"";else
return val;}
function validatePositiveInteger(value){return(Number(value)%1)==0&&Number(value)>=0&&value.length<10;}
function validatePositiveJavaInteger(value){return(Number(value)%1)==0&&Number(value)>=0&&Number(value)<=2147483647;}
function fromdb(val){return sanitizeXSS(noNull(val));}
function todb(val){return encodeURIComponent(val);}
function getVmName(p_vmName,p_vmDisplayname){if(p_vmDisplayname==null)
return fromdb(p_vmName);var vmName=null;if(p_vmDisplayname!=p_vmName){vmName=fromdb(p_vmDisplayname)+"("+p_vmName+")";}else{vmName=p_vmName;}
return vmName;}
function setBooleanReadField(value,$field){if(value=="true"||value==true)
$field.text(g_dictionary.yes).show();else if(value=="false"||value==false)
$field.text(g_dictionary.no).show();else
$field.hide();}
function setBooleanEditField(value,$field){if(value==true)
$field.val("true");else
$field.val("false");}
function convertBytes(bytes){if(bytes<1024*1024){return formatNumber((bytes/1024).toFixed(2))+" "+g_dictionary.KB;}else if(bytes<1024*1024*1024){return formatNumber((bytes/1024/1024).toFixed(2))+" "+g_dictionary.MB;}else if(bytes<1024*1024*1024*1024){return formatNumber((bytes/1024/1024/1024).toFixed(2))+" "+g_dictionary.GB;}else{return formatNumber((bytes/1024/1024/1024/1024).toFixed(2))+" "+g_dictionary.TB;}}
function convertHz(hz){if(hz==null)
return"";if(hz<1000){return formatNumber(hz)+" "+g_dictionary.MHZ;}else{return formatNumber((hz/1000).toFixed(2))+g_dictionary.GHZ;}}
function showError(isValid,field,errMsgField,errMsg){if(isValid){errMsgField.text("").hide();field.addClass("text").removeClass("error_text");}else{errMsgField.text(errMsg).show();field.removeClass("text").addClass("error_text");}}
function showError2(isValid,field,errMsgField,errMsg,appendErrMsg){if(isValid){errMsgField.text("").hide();field.addClass("text2").removeClass("error_text2");}else{if(appendErrMsg)
errMsgField.text(errMsgField.text()+errMsg).show();else
errMsgField.text(errMsg).show();field.removeClass("text2").addClass("error_text2");}}
function showErrorInDropdown(isValid,field,errMsgField,errMsg,appendErrMsg){if(isValid){errMsgField.text("").hide();field.addClass("select").removeClass("error_select");}else{if(appendErrMsg)
errMsgField.text(errMsgField.text()+errMsg).show();else
errMsgField.text(errMsg).show();field.removeClass("select").addClass("error_select");}}
function validateDropDownBox(label,field,errMsgField,appendErrMsg){var isValid=true;var errMsg="";var value=field.val();if(value==null||value.length==0){errMsg=g_dictionary.required;isValid=false;}
showErrorInDropdown(isValid,field,errMsgField,errMsg,appendErrMsg);return isValid;}
function validateInteger(label,field,errMsgField,min,max,isOptional){return validateNumber(label,field,errMsgField,min,max,isOptional,"integer");}
function validateNumber(label,field,errMsgField,min,max,isOptional,type){var isValid=true;var errMsg="";var value=field.val();if(value!=null&&value.length!=0){if(isNaN(value)){errMsg=g_dictionary.invalidNumber;isValid=false;}else{if(type=="integer"&&(value%1)!=0){errMsg=g_dictionary.invalidInteger;isValid=false;}
if(min!=null&&value<min){errMsg=g_dictionary.minimum+": "+min;isValid=false;}
if(max!=null&&value>max){errMsg=g_dictionary.maximum+": "+max;isValid=false;}}}else if(isOptional!=true){errMsg=g_dictionary.required;isValid=false;}
showError(isValid,field,errMsgField,errMsg);return isValid;}
function validateString(label,field,errMsgField,isOptional,maxLength){var isValid=true;var errMsg="";var value=field.val();if(isOptional!=true&&(value==null||value.length==0)){errMsg=g_dictionary.required;isValid=false;}else if(value!=null&&value.length>=maxLength){errMsg=g_dictionary.maximum+": "+max+" character";isValid=false;}else if(value!=null&&value.indexOf('"')!=-1){errMsg=g_dictionary.doubleQuotesNotAllowed;isValid=false;}
showError(isValid,field,errMsgField,errMsg);return isValid;}
function validateEmail(label,field,errMsgField,isOptional){if(validateString(label,field,errMsgField,isOptional)==false)
return;var isValid=true;var errMsg="";var value=field.val();if(value!=null&&value.length>0){myregexp=/^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$/;var isMatch=myregexp.test(value);if(!isMatch){errMsg=g_dictionary.example+": "+"xxxxxxx@hotmail.com";isValid=false;}}
showError(isValid,field,errMsgField,errMsg);return isValid;}
function validateNetmask(label,field,errMsgField,isOptional){if(validateString(label,field,errMsgField,isOptional)==false)
return;var isValid=true;var errMsg="";var value=field.val();if(value!=null&&value.length>0){myregexp=/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/;var isMatch=myregexp.test(value);if(!isMatch){errMsg=g_dictionary.example+": 255.255.255.0";isValid=false;}}
showError(isValid,field,errMsgField,errMsg);return isValid;}
function validateGateway(label,field,errMsgField,isOptional){if(validateString(label,field,errMsgField,isOptional)==false)
return;var isValid=true;var errMsg="";var value=field.val();if(value!=null&&value.length>0){myregexp=/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/;var isMatch=myregexp.test(value);if(!isMatch){errMsg=g_dictionary.example+": 192.168.100.1";isValid=false;}}
showError(isValid,field,errMsgField,errMsg);return isValid;}
function validateIp(label,field,errMsgField,isOptional){if(validateString(label,field,errMsgField,isOptional)==false)
return;var isValid=true;var errMsg="";var value=field.val();if(value!=null&&value.length>0){myregexp=/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/;var isMatch=myregexp.test(value);if(!isMatch){errMsg=g_dictionary.example+": "+"75.52.126.11";isValid=false;}}
showError(isValid,field,errMsgField,errMsg);return isValid;}
function validateCIDR(label,field,errMsgField,isOptional){if(validateString(label,field,errMsgField,isOptional)==false)
return;var isValid=true;var errMsg="";var value=field.val();if(value!=null&&value.length>0){myregexp=/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/;var isMatch=myregexp.test(value);if(!isMatch){errMsg=g_dictionary.example+": "+"75.52.126.11/24";isValid=false;}}
showError(isValid,field,errMsgField,errMsg);return isValid;}
function validateCIDRList(label,field,errMsgField,isOptional){if(validateString(label,field,errMsgField,isOptional)==false)
return;var isValid=true;var errMsg="";var cidrList=field.val();var array1=cidrList.split(",");for(var i=0;i<array1.length;i++){var value=array1[i];if(value!=null&&value.length>0){myregexp=/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\/\d{1,2}$/;var isMatch=myregexp.test(value);if(!isMatch){isValid=false;}}}
if(isValid==false)
errMsg=g_dictionary.example+": 10.1.1.1/24,10.1.1.2/24";showError(isValid,field,errMsgField,errMsg);return isValid;}
function validateAlphanumeric(label,field,errMsgField,isOptional,allowedSplCharacters){if(validateString(label,field,errMsgField,isOptional)==false)
return;var isValid=true;var errMsg="";var value=field.val();if(value!=null&&value.length>0){if(allowedSplCharacters!=undefined&&allowedSplCharacters.length>0){value=value.replace(new RegExp("["
+allowedSplCharacters.join('')+"]","g"),"");}
if(alphanumericRegexp.test(value)==false){errMsg=dictionary.onlyAlphanumericCharactersAreAllowed;isValid=false;}}
showError(isValid,field,errMsgField,errMsg);return isValid;}
function validatePath(label,field,errMsgField,isOptional){if(validateString(label,field,errMsgField,isOptional)==false)
return;var isValid=true;var errMsg="";var value=field.val();if(value!=null&&value.length>0){myregexp=/^\//;var isMatch=myregexp.test(value);if(!isMatch){errMsg=label+g_dictionary.example+": "+"/aaa/bbb/ccc";isValid=false;}}
showError(isValid,field,errMsgField,errMsg);return isValid;}
function cleanErrMsg(field,errMsgField){showError(true,field,errMsgField);}
var keycode_Enter=13;var cloudStackLoginResponse,cloudStackSessionKey,userTimeZoneOffset,cloudStackSupportELB,cloudStackFirewallRuleUiEnabled;var cloudStackVersion="";var isCloudStack22X=false;var isCloudStackActonOrAbove=false;var cloudStackDirectAttachSecurityGroupsEnabled="false";function getDirectAttachSecurityGroupsEnabled(){return cloudStackDirectAttachSecurityGroupsEnabled;}
var $currentGridRow;function clickAnotherGridRow($thisGridRow){if($currentGridRow!=null&&$currentGridRow!=$thisGridRow)
$currentGridRow.removeClass("selected active");if($thisGridRow.hasClass("selected")==false)
$thisGridRow.addClass("selected active");$currentGridRow=$thisGridRow;$("body").stopTime();$("#top_actions").find("#spinning_wheel").hide();$("#action_result_panel").find("#msg").text("");$("#action_result_panel").removeClass("error").addClass("success").hide();cancelActionPanel(currentSelectedActionLink);var $spinningWheel=$("#top_actions").find("#spinning_wheel");$spinningWheel.find("#in_process_text").text("");$spinningWheel.hide();$("#top_message_panel").find("#msg").text("");$("#top_message_panel").removeClass("error").addClass("success").hide();}
function clickTopActionLink(){if($currentGridRow==null||($currentGridRow.hasClass("selected")==true&&$currentGridRow.hasClass("active")==true))
$currentGridRow.removeClass("selected active");}
function cancelTopActionLink(){if($currentGridRow!=null&&$currentGridRow.hasClass("selected")==false&&$currentGridRow.hasClass("active")==false)
$currentGridRow.addClass("selected active");}
function initActionLinks(actionMap,refreshGridRowFn,refreshDetailsPanelFn){for(var i=0;i<actionMap.length;i++){bindToActionLink(actionMap[i],refreshGridRowFn,refreshDetailsPanelFn);}}
var currentSelectedActionLink=null;function clickActionLink(elementIdPrefix,actionLinkOnClickFn){if(actionLinkOnClickFn!=null)
actionLinkOnClickFn();cancelActionPanel(currentSelectedActionLink);currentSelectedActionLink=elementIdPrefix;$("#"+elementIdPrefix+"_link").hide();$("#"+elementIdPrefix+"_text").show();$("#action_result_panel").hide();$("#top_message_panel").hide();$("#"+elementIdPrefix+"_panel").show();}
function cancelActionPanel(elementIdPrefix,actionLinkOnCancelFn){if(elementIdPrefix==null)
return;if(actionLinkOnCancelFn!=null)
actionLinkOnCancelFn();$("#"+elementIdPrefix+"_text").hide();$("."+elementIdPrefix+"_link").show();$("#"+elementIdPrefix+"_panel").hide();if(currentSelectedActionLink==elementIdPrefix)
currentSelectedActionLink=null;}
function bindToActionLink(mapObj,refreshGridRowFn,refreshDetailsPanelFn){$("."+mapObj.elementIdPrefix+"_link").unbind("click").bind("click",function(event){clickActionLink(mapObj.elementIdPrefix,mapObj.actionLinkOnClickFn);return false;});$("#"+mapObj.elementIdPrefix+"_panel").find("#cancel_button").unbind("click").bind("click",function(event){$("#"+mapObj.elementIdPrefix+"_panel").find(".agree input[type=checkbox]#accept_checkbox").removeAttr("checked");cancelActionPanel(mapObj.elementIdPrefix,mapObj.actionLinkOnCancelFn);return false;});$("#"+mapObj.elementIdPrefix+"_panel").find("#confirm_button").unbind("click").bind("click",function(event){if($currentGridRow.data("jsonObj")==null)
return false;var actionUrl1=mapObj.getActionUrlFn();if(actionUrl1==null)
return false;$("#"+mapObj.elementIdPrefix+"_text").hide();$("."+mapObj.elementIdPrefix+"_link").show();$("#"+mapObj.elementIdPrefix+"_panel").hide();$("#action_link_container").find("#spinning_wheel").show();if(mapObj.isAsync==true){$.ajax({type:"GET",url:actionUrl1,async:false,dataType:"json",success:function(obj){var property;for(property in obj){};var jobId=obj[property].jobid;if(jobId==null)
return;var timerKey="vmAction"+jobId;$("body").everyTime(5000,timerKey,function(){var actionUrl2=cloudStackURL("command=queryAsyncJobResult&jobId="
+jobId);$.ajax({type:"GET",url:actionUrl2,dataType:"json",success:function(json){var result=json.queryasyncjobresultresponse;if(result.jobstatus==0){return;}else{$("body").stopTime(timerKey);$("#action_link_container").find("#spinning_wheel").hide();if(result.jobstatus==1){var property;for(property in result.jobresult){};var item4=result.jobresult[property];if(typeof(item4)!="object"){if((property=="success")&&((typeof(item4)=="string"&&item4=="true")||(typeof(item4)=="boolean"&&item4==true))){$currentGridRow.click();var msg;if(mapObj.afterActionSeccessFn!=null){msg=mapObj.afterActionSeccessFn(item4);}
if(msg==null){var actionlabel=$("."
+mapObj.elementIdPrefix
+"_link").find("a").first().text();msg=actionlabel
+" "
+g_dictionary.succeeded;}
$("#action_result_panel").find("#msg").html(msg);$("#action_result_panel").find("#status_icon").removeClass("erroricon").addClass("successicon");$("#action_result_panel").removeClass("error").addClass("success").show();}
return;}
if($currentGridRow.data("jsonObj")==null)
return;var idPropertyName=null;if(mapObj.returnedObjectId!=null){if(mapObj.returnedObjectId!="NoReturnedObjectId")
idPropertyName=mapObj.returnedObjectId;else
idPropertyName=null;}else{idPropertyName="id";}
if((idPropertyName==null)||(item4[idPropertyName]==$currentGridRow.data("jsonObj").id)){var msg;if(mapObj.afterActionSeccessFn!=null){msg=mapObj.afterActionSeccessFn(item4);}
if(msg==null){var actionlabel=$("."
+mapObj.elementIdPrefix
+"_link").find("a").first().text();msg=actionlabel
+" "
+g_dictionary.succeeded;}
$("#action_result_panel").find("#msg").html(msg);$("#action_result_panel").find("#status_icon").removeClass("erroricon").addClass("successicon");$("#action_result_panel").removeClass("error").addClass("success").show();refreshGridRowFn(item4,$currentGridRow);refreshDetailsPanelFn($currentGridRow);}}else if(result.jobstatus==2){var actionlabel=$("."
+mapObj.elementIdPrefix
+"_link").find("a").first().text();var msg=actionlabel
+" "
+g_dictionary.actionFailed;if(result.jobresult.errortext!=null&&result.jobresult.errortext.length>0)
msg+=(" - "+localizeCloudStackMsg(fromdb(result.jobresult.errortext)));$("#action_result_panel").find("#msg").text(msg);$("#action_result_panel").find("#status_icon").removeClass("successicon").addClass("erroricon");$("#action_result_panel").removeClass("success").addClass("error").show();}}},error:function(response){$("body").stopTime(timerKey);$("#action_link_container").find("#spinning_wheel").hide();$("#action_result_panel").find("#msg").text(g_dictionary.errorFromAPICall
+": "
+actionUrl2);$("#action_result_panel").find("#status_icon").removeClass("successicon").addClass("erroricon");$("#action_result_panel").removeClass("success").addClass("error").show();}});},0);},error:function(XMLHttpResponse){$("#action_link_container").find("#spinning_wheel").hide();var errorMsg="";if(XMLHttpResponse.responseText!=null&XMLHttpResponse.responseText.length>0)
errorMsg=parseXMLHttpResponse(XMLHttpResponse);var actionlabel=$("."
+mapObj.elementIdPrefix
+"_link").find("a").first().text();var msg=actionlabel+" "
+g_dictionary.actionFailed;if(errorMsg.length>0)
msg+=(" "+localizeCloudStackMsg(errorMsg));else
msg+=(" "
+g_dictionary.errorFromAPICall
+": "+actionUrl1);$("#action_result_panel").find("#msg").text(msg);$("#action_result_panel").find("#status_icon").removeClass("successicon").addClass("erroricon");$("#action_result_panel").removeClass("success").addClass("error").show();}});}else{$.ajax({type:"GET",url:actionUrl1,async:false,dataType:"json",success:function(obj1){if(typeof(obj1)!="object")
return;var propertyInObj1;for(propertyInObj1 in obj1){};var obj2=obj1[propertyInObj1];if(typeof(obj2)!="object")
return;var propertyInObj2;for(propertyInObj2 in obj2){};var item3=obj2[propertyInObj2];$("#action_link_container").find("#spinning_wheel").hide();var msg;if(mapObj.afterActionSeccessFn!=null){msg=mapObj.afterActionSeccessFn(item3);}
if(msg==null){var actionlabel=$("."
+mapObj.elementIdPrefix
+"_link").find("a").first().text();msg=actionlabel
+" "
+g_dictionary.succeeded;}
$("#action_result_panel").find("#msg").text(msg);$("#action_result_panel").find("#status_icon").removeClass("successicon").addClass("erroricon");$("#action_result_panel").removeClass("error").addClass("success").show();if(typeof(item3)=="object"){refreshGridRowFn(item3,$currentGridRow);refreshDetailsPanelFn($currentGridRow);}
if((propertyInObj2=="success")&&((typeof(item3)=="string"&&item3=="true")||(typeof(item3)=="boolean"&&item3==true))){$currentGridRow.click();$("#action_result_panel").find("#msg").text(msg);$("#action_result_panel").find("#status_icon").removeClass("successicon").addClass("erroricon");$("#action_result_panel").removeClass("error").addClass("success").show();}},error:function(obj1){$("#action_link_container").find("#spinning_wheel").hide();var actionlabel=$("."
+mapObj.elementIdPrefix
+"_link").find("a").first().text();var msg=actionlabel+" "
+g_dictionary.actionFailed;$("#action_result_panel").find("#msg").text(msg);$("#action_result_panel").find("#status_icon").removeClass("successicon").addClass("erroricon");$("#action_result_panel").removeClass("success").addClass("error").show();}});}
return false;});}
function doActionButton(actionMapItem,apiCommand){var label=actionMapItem.label;var inProcessText=actionMapItem.inProcessText;var protocol_type=actionMapItem.type;if(protocol_type==null)
protocol_type="GET";var data_type=actionMapItem.dataType;if(data_type==null)
data_type="json";var isAsyncJob=actionMapItem.isAsyncJob;var asyncJobResponse=actionMapItem.asyncJobResponse;var afterActionSeccessFn=actionMapItem.afterActionSeccessFn;var $spinningWheel=$("#top_actions").find("#spinning_wheel");$spinningWheel.find("#in_process_text").text(inProcessText);$spinningWheel.show();$("#top_message_panel").find("#msg").text("");$("#top_message_panel").hide();$("#action_result_panel").find("#msg").text("");$("#action_result_panel").hide();if(isAsyncJob==true){$.ajax({cache:false,type:protocol_type,url:apiCommand,dataType:data_type,success:function(json){var jobId=json[asyncJobResponse].jobid;var timerKey="asyncJob_"+jobId;$("body").everyTime(10000,timerKey,function(){$.ajax({cache:false,url:cloudStackURL("command=queryAsyncJobResult&jobId="
+jobId),dataType:"json",success:function(json){var result=json.queryasyncjobresultresponse;if(result.jobstatus==0){return;}else{$("body").stopTime(timerKey);$spinningWheel.hide();if(result.jobstatus==1){var msg;if(actionMapItem.afterActionSeccessFn!=null)
msg=actionMapItem.afterActionSeccessFn(json);if(msg==null)
msg=label
+" "
+g_dictionary.succeeded;if(actionMapItem.showResultMessage!=false){$("#top_message_panel").find("#msg").text(msg);$("#top_message_panel").find("#status_icon").removeClass("erroricon").addClass("successicon");$("#top_message_panel").removeClass("error").addClass("success").show();}}else if(result.jobstatus==2){$("#top_message_panel").find("#msg").text(label
+" "
+g_dictionary.actionFailed
+" - "
+localizeCloudStackMsg(fromdb(result.jobresult.errortext)));$("#top_message_panel").find("#status_icon").removeClass("successicon").addClass("erroricon");$("#top_message_panel").removeClass("success").addClass("error").show();}}},error:function(XMLHttpResponse){$("body").stopTime(timerKey);$spinningWheel.hide();$("#top_message_panel").find("#msg").text(label
+" "
+g_dictionary.actionFailed);$("#top_message_panel").find("#status_icon").removeClass("successicon").addClass("erroricon");$("#top_message_panel").removeClass("success").addClass("error").show();if(actionMapItem.afterActionFailureFn!=null){actionMapItem.afterActionFailureFn(label
+" "
+g_dictionary.actionFailed);}}});},0);},error:function(XMLHttpResponse){$spinningWheel.hide();$("#top_message_panel").find("#msg").text(label+" "+g_dictionary.actionFailed);$("#top_message_panel").find("#status_icon").removeClass("successicon").addClass("erroricon");$("#top_message_panel").removeClass("success").addClass("error").show();if(actionMapItem.afterActionFailureFn!=null){actionMapItem.afterActionFailureFn(label+" "
+g_dictionary.actionFailed);}}});}
else{$.ajax({type:protocol_type,cache:false,url:apiCommand,dataType:data_type,async:false,success:function(json){var msg;if(actionMapItem.afterActionSeccessFn!=null)
msg=actionMapItem.afterActionSeccessFn(json);if(msg==null)
msg=label+" "+g_dictionary.succeeded;$spinningWheel.hide();if(actionMapItem.showResultMessage!=false){$("#top_message_panel").find("#msg").text(msg);$("#top_message_panel").find("#status_icon").removeClass("erroricon").addClass("successicon");$("#top_message_panel").removeClass("error").addClass("success").show();}},error:function(XMLHttpResponse){$spinningWheel.hide();$("#top_message_panel").find("#msg").text(label+" "+g_dictionary.actionFailed);$("#top_message_panel").find("#status_icon").removeClass("successicon").addClass("erroricon");$("#top_message_panel").removeClass("success").addClass("error").show();if(actionMapItem.afterActionFailureFn!=null){actionMapItem.afterActionFailureFn(label+" "
+g_dictionary.actionFailed);}
if(actionMapItem.processXMLHttpResponse!=null){actionMapItem.processXMLHttpResponse(XMLHttpResponse);}}});}}
function bindActionMenuContainers(){$(".action_menu_container").each(function(i,ele){var $ele=$(ele);$ele.bind("mouseover",function(event){$(this).find("#action_menu").show();return false;});$ele.bind("mouseout",function(event){var $thisElement=$(this)[0];var relatedTarget1=event.relatedTarget;while(relatedTarget1!=null&&relatedTarget1.nodeName!="BODY"&&relatedTarget1!=$thisElement){relatedTarget1=relatedTarget1.parentNode;}
if(relatedTarget1==$thisElement){return;}
$(this).find("#action_menu").hide();return false;});});}
var ERROR_ACCESS_DENIED_DUE_TO_UNAUTHORIZED=401;var ERROR_INTERNET_NAME_NOT_RESOLVED=12007;var ERROR_INTERNET_CANNOT_CONNECT=12029;var ERROR_VMOPS_ACCOUNT_ERROR=531;function handleError(XMLHttpResponse,handleErrorCallback){if(XMLHttpResponse.status==ERROR_ACCESS_DENIED_DUE_TO_UNAUTHORIZED){alert("CloudStack - Your session has expired.");}else if(XMLHttpResponse.status==ERROR_INTERNET_NAME_NOT_RESOLVED){alert("CloudStack - Your internet name cannot be resolved.");}else if(XMLHttpResponse.status==ERROR_INTERNET_CANNOT_CONNECT){alert("CloudStack - The Management Server is unaccessible.  Please try again later.");}else if(XMLHttpResponse.status==ERROR_VMOPS_ACCOUNT_ERROR&&handleErrorCallback!=undefined){handleErrorCallback();}else if(handleErrorCallback!=undefined){handleErrorCallback();}else{var errorMsg=localizeCloudStackMsg(fromdb(parseXMLHttpResponse(XMLHttpResponse)));if(errorMsg!=""){alert(errorMsg);}}}
function parseXMLHttpResponse(XMLHttpResponse){if(isBrowserIE7()==false){if(isValidJsonString(XMLHttpResponse.responseText)==false){return"";}}
if(isBrowserIE7()==true){try{var json=jQuery.parseJSON(XMLHttpResponse.responseText);}catch(e){return"";}}else{var json=JSON.parse(XMLHttpResponse.responseText);}
if(json!=null){var property;for(property in json){}
var errorObj=json[property];return fromdb(errorObj.errortext);}else{return"";}}
function isValidJsonString(str){try{JSON.parse(str);}catch(e){return false;}
return true;}
var $readonlyFields,$editFields;function cancelEditMode($tab){if($editFields!=null)
$editFields.hide();if($readonlyFields!=null)
$readonlyFields.show();$tab.find("#save_button, #cancel_button").hide();}
function switchBetweenDifferentTabs(tabArray,tabContentArray,afterSwitchFnArray){for(var tabIndex=0;tabIndex<tabArray.length;tabIndex++){switchToTab(tabIndex,tabArray,tabContentArray,afterSwitchFnArray);}}
function switchToTab(tabIndex,tabArray,tabContentArray,afterSwitchFnArray){tabArray[tabIndex].bind("click",function(event){tabArray[tabIndex].removeClass("off").addClass("on");for(var k=0;k<tabArray.length;k++){if(k!=tabIndex)
tabArray[k].removeClass("on").addClass("off");}
tabContentArray[tabIndex].show();for(var k=0;k<tabContentArray.length;k++){if(k!=tabIndex)
tabContentArray[k].hide();}
if(afterSwitchFnArray!=null){if(afterSwitchFnArray[tabIndex]!=null)
afterSwitchFnArray[tabIndex]();}
return false;});}
function switchBetweenDifferentTabsTwo(tabArray,tabContentArray,afterSwitchFnArray){for(var tabIndex=0;tabIndex<tabArray.length;tabIndex++){switchToTabTwo(tabIndex,tabArray,tabContentArray,afterSwitchFnArray);}}
function switchToTabTwo(tabIndex,tabArray,tabContentArray,afterSwitchFnArray){tabArray[tabIndex].bind("click",function(event){tabArray[tabIndex].removeClass("nonactive").addClass("active");for(var k=0;k<tabArray.length;k++){if(k!=tabIndex)
tabArray[k].removeClass("active").addClass("nonactive");}
tabContentArray[tabIndex].show();for(var k=0;k<tabContentArray.length;k++){if(k!=tabIndex)
tabContentArray[k].hide();}
if(afterSwitchFnArray!=null){if(afterSwitchFnArray[tabIndex]!=null)
afterSwitchFnArray[tabIndex]();}
return false;});}
var alphanumericRegexp=/^[a-zA-Z0-9_]*$/;function initDialog(elementId,width1,addToActive){if(width1==null){activateDialog($("#"+elementId).dialog({autoOpen:false,modal:true,zIndex:2000,open:function(event){$(".action_menu_container").find("#action_menu").hide();}}),addToActive);}else{activateDialog($("#"+elementId).dialog({width:width1,autoOpen:false,modal:true,zIndex:2000,open:function(event){$(".action_menu_container").find("#action_menu").hide();}}),addToActive);}}
function initDialogWithOK(elementId,width1,addToActive){var dialog;if(width1==null){dialog=$("#"+elementId).dialog({autoOpen:false,modal:true,zIndex:2000,buttons:{"OK":function(){$(this).dialog("close");}}});}else{dialog=$("#"+elementId).dialog({width:width1,height:500,autoOpen:false,modal:true,zIndex:2000,buttons:{"OK":function(){$(this).dialog("close");}}});}
activateDialog(dialog,addToActive);return dialog;}
function setTemplateStateInRightPanel(stateValue,$stateField){$stateField.text(stateValue);if(stateValue=="Ready")
$stateField.text(stateValue);else if(stateValue!=null&&stateValue.indexOf("%")!=-1)
$stateField.text(stateValue);else
$stateField.text(stateValue);}
var activeDialogs=new Array();function activateDialog(dialog,addToActive){if(addToActive==undefined||addToActive){activeDialogs[activeDialogs.length]=dialog;}
dialog.keypress(function(event){if(event.keyCode==keycode_Enter){var activeElementTag=document.activeElement.tagName;if(activeElementTag=="TEXTAREA"){return true;}
$buttons=$('[aria-labelledby$='+dialog.attr("id")+']').find(":button");var activeElementFound=false;$buttons.each(function(index,element){if(element==document.activeElement){activeElementFound=true;element.click();return false;}});if(!activeElementFound){var primaryButton=$('[aria-labelledby$='+dialog.attr("id")+']').find(":button[data-primary]:visible:first");if(primaryButton.length===0){primaryButton=$('[aria-labelledby$='+dialog.attr("id")+']').find(":button:first");}
primaryButton.click();}
return false;}});}
function removeDialogs(){for(var i=0;i<activeDialogs.length;i++){activeDialogs[i].remove();}
activeDialogs=new Array();}
function roundNumber(number,decimal){var floatValue=parseFloat(number);if((floatValue+'').indexOf('.')!=-1&&decimal=='0'){decimal=2;var array1=(floatValue+'').split('.');var decimalValue=array1[1];if(decimalValue.length==1){decimal=1;}}
return(Math.round(floatValue*Math.pow(10,decimal))/Math.pow(10,decimal)).toFixed(decimal);}
function stripTrailingZeros(number,minFractionalPart){var strNumber=number.toString();var pointPos=strNumber.indexOf('.');var lenNumber=strNumber.length;if(pointPos==-1||(lenNumber-pointPos)<(minFractionalPart+2))
return strNumber;return strNumber.slice(0,pointPos+minFractionalPart+1)+
strNumber.slice(pointPos+minFractionalPart+1).replace(/0*$/,'');}
function formatNumber(number){return number.replace(".",g_dictionary.decPoint);}
function setDateField(dateValue,dateField){if(dateValue!=null&&dateValue.length>0){var disconnected=new Date();disconnected.setISO8601(dateValue);var showDate;var milliseconds=disconnected.getTime();if(userTimeZoneOffset!=null)
disconnected=new Date(milliseconds
+(userTimeZoneOffset*60*60*1000));showDate=dateFormat(disconnected,g_dictionary.jsDateFormat,true);dateField.text(showDate);}}
function displayAjaxFormError(XMLHttpRequest,formId,fieldErrorClass){var json=$.parseJSON(XMLHttpRequest.responseText);$("#"+formId+" ."+fieldErrorClass+" label").html("");for(var fieldError in json.Error){var html="<label class=\"error\" for=\""+fieldError
+"\" generated=\"true\">"+json.Error[fieldError].Message
+"</label>";$("div[id='"+fieldError+"Error"+"']").html(html);}}
function getOnlyNosFromThePhoneNoString(phoneNumber){var nosInPhoneNoArray=phoneNumber.match(/\d+/g);var phoneNo="";for(var i=0;i<=nosInPhoneNoArray.length-1;i++){phoneNo+=nosInPhoneNoArray[i];}
return phoneNo;}
function initActionLinks2(actionMap,refreshGridRowFn,refreshDetailsPanelFn){for(var i=0;i<actionMap.length;i++){bindToActionLink2(actionMap[i],refreshGridRowFn,refreshDetailsPanelFn);}}
var currentSelectedActionLink=null;function clickActionLink2(elementIdPrefix,actionLinkOnClickFn,refreshGridRowFn,refreshDetailsPanelFn,mapObj){if(actionLinkOnClickFn!=null)
actionLinkOnClickFn();currentSelectedActionLink=elementIdPrefix;$("#action_result_panel").hide();$("#top_message_panel").hide();$("#"+mapObj.elementIdPrefix+"_panel").find(".agree input[type=checkbox]#accept_checkbox").removeAttr("checked");var dialog=$("#"+elementIdPrefix+"_panel").dialog({width:675,autoOpen:false,modal:true,zIndex:2000,buttons:{"OK":function(){if($currentGridRow.data("jsonObj")==null)
return false;var actionUrl1=mapObj.getActionUrlFn(this);if(actionUrl1==null)
return false;$(this).dialog("close");var $spinningWheel=$("#top_actions").find("#spinning_wheel");$spinningWheel.find("#in_process_text").text(mapObj.actionText);$spinningWheel.show();if(mapObj.isAsync==true){$.ajax({type:"GET",url:actionUrl1,async:false,cache:false,dataType:"json",success:function(obj){var property;for(property in obj){};var jobId=obj[property].jobid;if(jobId==null)
return;var timerKey="vmAction"
+jobId;$("body").everyTime(5000,timerKey,function(){var actionUrl2=cloudStackURL("command=queryAsyncJobResult&jobId="
+jobId);$.ajax({type:"GET",url:actionUrl2,dataType:"json",cache:false,success:function(json){var result=json.queryasyncjobresultresponse;if(result.jobstatus==0){return;}else{$("body").stopTime(timerKey);$("#top_actions").find("#spinning_wheel").hide();if(result.jobstatus==1){var property;for(property in result.jobresult){};var item4=result.jobresult[property];if(typeof(item4)!="object"){if((property=="success")&&((typeof(item4)=="string"&&item4=="true")||(typeof(item4)=="boolean"&&item4==true))){$currentGridRow.click();var msg;if(mapObj.afterActionSeccessFn!=null){msg=mapObj.afterActionSeccessFn(item4);}
if(msg==null){var actionlabel=$("."
+mapObj.elementIdPrefix
+"_link").find("a").first().text();msg=actionlabel
+" "
+g_dictionary.succeeded;}
$("#action_result_panel").find("#msg").html(msg);$("#action_result_panel").find("#status_icon").removeClass("erroricon").addClass("successicon");$("#action_result_panel").removeClass("error").addClass("success").show();}
return;}
if($currentGridRow.data("jsonObj")==null)
return;var idPropertyName=null;if(mapObj.returnedObjectId!=null){if(mapObj.returnedObjectId!="NoReturnedObjectId")
idPropertyName=mapObj.returnedObjectId;else
idPropertyName=null;}else{idPropertyName="id";}
if((idPropertyName==null)||(item4[idPropertyName]==$currentGridRow.data("jsonObj").id)){var msg;if(mapObj.afterActionSeccessFn!=null){msg=mapObj.afterActionSeccessFn(item4);}
if(msg==null){var actionlabel=$("."
+mapObj.elementIdPrefix
+"_link").find("a").first().text();msg=actionlabel
+" "
+g_dictionary.succeeded;}
$("#action_result_panel").find("#msg").html(msg);$("#action_result_panel").find("#status_icon").removeClass("erroricon").addClass("successicon");$("#action_result_panel").removeClass("error").addClass("success").show();$("#"
+mapObj.elementIdPrefix
+"_panel").find(".agree input[type=checkbox]#accept_checkbox").removeAttr("checked");if(mapObj.detailsLevelAction!=true){refreshGridRowFn(item4,$currentGridRow);refreshDetailsPanelFn($currentGridRow);}}}else if(result.jobstatus==2){var actionlabel=$("."
+mapObj.elementIdPrefix
+"_link").find("a").first().text();var msg=actionlabel
+" "
+g_dictionary.actionFailed;if(result.jobresult.errortext!=null&&result.jobresult.errortext.length>0)
msg+=(" - "+localizeCloudStackMsg(fromdb(result.jobresult.errortext)));$("#action_result_panel").find("#msg").text(msg);$("#action_result_panel").find("#status_icon").removeClass("successicon").addClass("erroricon");$("#action_result_panel").removeClass("success").addClass("error").show();$("#"
+mapObj.elementIdPrefix
+"_panel").find(".agree input[type=checkbox]#accept_checkbox").removeAttr("checked");}}},error:function(response){$("body").stopTime(timerKey);$("#top_actions").find("#spinning_wheel").hide();$("#action_result_panel").find("#msg").text(g_dictionary.errorFromAPICall
+": queryAsyncJobResult");$("#action_result_panel").find("#status_icon").removeClass("successicon").addClass("erroricon");$("#action_result_panel").removeClass("success").addClass("error").show();$("#"
+mapObj.elementIdPrefix
+"_panel").find(".agree input[type=checkbox]#accept_checkbox").removeAttr("checked");}});},0);},error:function(XMLHttpResponse){$("#top_actions").find("#spinning_wheel").hide();var errorMsg="";if(XMLHttpResponse.responseText!=null&XMLHttpResponse.responseText.length>0)
errorMsg=parseXMLHttpResponse(XMLHttpResponse);var actionlabel=$("."
+mapObj.elementIdPrefix
+"_link").find("a").first().text();var msg=actionlabel
+" "
+g_dictionary.actionFailed;if(errorMsg.length>0)
msg+=(" "+localizeCloudStackMsg(errorMsg));else
msg+=(" "
+g_dictionary.errorFromAPICall
+": "+mapObj.apiName);$("#action_result_panel").find("#msg").text(msg);$("#action_result_panel").find("#status_icon").removeClass("successicon").addClass("erroricon");$("#action_result_panel").removeClass("success").addClass("error").show();$("#"
+mapObj.elementIdPrefix
+"_panel").find(".agree input[type=checkbox]#accept_checkbox").removeAttr("checked");}});}else{$.ajax({type:"GET",url:actionUrl1,async:false,cache:false,dataType:"json",success:function(obj1){if(typeof(obj1)!="object")
return;var propertyInObj1;for(propertyInObj1 in obj1){};var obj2=obj1[propertyInObj1];if(typeof(obj2)!="object")
return;var propertyInObj2;for(propertyInObj2 in obj2){};var item3=obj2[propertyInObj2];$("#top_actions").find("#spinning_wheel").hide();var msg;if(mapObj.afterActionSeccessFn!=null){msg=mapObj.afterActionSeccessFn(item3);}
if(msg==null){var actionlabel=$("."
+mapObj.elementIdPrefix
+"_link").find("a").first().text();msg=actionlabel
+" "
+g_dictionary.succeeded;}
$("#action_result_panel").find("#msg").text(msg);$("#action_result_panel").find("#status_icon").removeClass("erroricon").addClass("successicon");$("#action_result_panel").removeClass("error").addClass("success").show();if(typeof(item3)=="object"){refreshGridRowFn(item3,$currentGridRow);refreshDetailsPanelFn($currentGridRow);}
if((propertyInObj2=="success")&&((typeof(item3)=="string"&&item3=="true")||(typeof(item3)=="boolean"&&item3==true))){$currentGridRow.click();$("#action_result_panel").find("#msg").text(msg);$("#action_result_panel").find("#status_icon").removeClass("erroricon").addClass("successicon");$("#action_result_panel").removeClass("error").addClass("success").show();}},error:function(XMLHttpResponse){$("#top_actions").find("#spinning_wheel").hide();var errorMsg="";if(XMLHttpResponse.responseText!=null&XMLHttpResponse.responseText.length>0)
errorMsg=parseXMLHttpResponse(XMLHttpResponse);var actionlabel=$("."
+mapObj.elementIdPrefix
+"_link").find("a").first().text();var msg=actionlabel
+" "
+g_dictionary.actionFailed;if(errorMsg.length>0)
msg+=(" "+localizeCloudStackMsg(errorMsg));else
msg+=(" "
+g_dictionary.errorFromAPICall
+": "+mapObj.apiName);$("#action_result_panel").find("#msg").text(msg);$("#action_result_panel").find("#status_icon").removeClass("successicon").addClass("erroricon");$("#action_result_panel").removeClass("success").addClass("error").show();$("#"
+mapObj.elementIdPrefix
+"_panel").find(".agree input[type=checkbox]#accept_checkbox").removeAttr("checked");}});}},"Cancel":function(){$("#"+mapObj.elementIdPrefix+"_panel").find(".agree input[type=checkbox]#accept_checkbox").removeAttr("checked");if(currentSelectedActionLink==elementIdPrefix)
currentSelectedActionLink=null;$(this).dialog("close");}}});activateDialog(dialog);dialogButtonsLocalizer(dialog,{'OK':g_dictionary.dialogOK,'Cancel':g_dictionary.dialogCancel});dialog.dialog("open");}
var $currentSelectedVolume=null;var $currentSelectedSnapshot=null;function bindToActionLink2(mapObj,refreshGridRowFn,refreshDetailsPanelFn){$("."+mapObj.elementIdPrefix+"_link").unbind("click").bind("click",function(event){$currentSelectedVolume=$(this).attr('ref_volume');$currentSelectedSnapshot=$(this).parents('#action_menu').attr('ref_snapshot');clickActionLink2(mapObj.elementIdPrefix,mapObj.actionLinkOnClickFn,refreshGridRowFn,refreshDetailsPanelFn,mapObj);});}
function activateThirdMenuItem(menuItem){$("#"+menuItem).removeClass("off").addClass("on");}
var l3menuTenantParam=$("#l3_tenant_param").val();$("#l3_usage_details_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/billing/usageBilling?tenant="
+effectiveTenantParam;});$("#l3_subscriptions_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");if($("#usage_billing_my_usage").val()=="true"){window.location="/portal/portal/usage/subscriptions?tenant="
+effectiveTenantParam+"&perPage=14&page=1";}else{window.location="/portal/portal/billing/subscriptions?tenant="
+effectiveTenantParam+"&perPage=14&page=1";}});$("#l3_billing_invoices_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/billing/history?tenant="
+effectiveTenantParam+"&perPage=14&page=1";});$("#l3_billing_payments_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/billing/paymenthistory?tenant="
+effectiveTenantParam+"&perPage=14&page=1";});$("#l3_payment_info_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/billing/showcreditcarddetails?tenant="
+effectiveTenantParam;});$("#13_health_status_tab").bind("click",function(event){var selectedServiceInstanceUUID=$("#selectedServiceInstance").find(".downarrow").attr("id");$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");if(typeof selectedServiceInstanceUUID=='undefined'||selectedServiceInstanceUUID==null||selectedServiceInstanceUUID==""){window.location="/portal/portal/health";}else{window.location="/portal/portal/health?serviceInstanceUUID="+selectedServiceInstanceUUID;}});$("#13_health_scheduled_maintainence_tab").bind("click",function(event){var selectedServiceInstanceUUID=$("#selectedServiceInstance").find(".downarrow").attr("id");$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");if(typeof selectedServiceInstanceUUID=='undefined'||selectedServiceInstanceUUID==null||selectedServiceInstanceUUID==""){window.location="/portal/portal/health/healthmaintainance";}else{window.location="/portal/portal/health/healthmaintainance?serviceInstanceUUID="+selectedServiceInstanceUUID;}});$("#l3_compute_bundles_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/subscription/createsubscription?tenant="
+l3menuTenantParam;});$("#l3_compute_bundles_tab_browse_catalog").bind("click",function(event){var searchq=window.location.search;var lang="";if(searchq.indexOf("lang=")!=-1){lang=searchq.substring(searchq.indexOf("lang="));window.location="/portal/portal/browse_catalogs?"+lang;}else
window.location="/portal/portal/browse_catalogs";});$("#l3_account_All_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/tenants/list?accountType="
+selectedAccountType+"&filterBy=All";});$("#l3_service_bundles_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/subscription/subscribeNonVmBundle?tenant="
+l3menuTenantParam;});$("#l3_service_bundles_tab_browse_catalog").bind("click",function(event){var searchq=window.location.search;var lang="";if(searchq.indexOf("lang=")!=-1){lang=searchq.substring(searchq.indexOf("lang="));window.location="/portal/portal/browse_catalogs?t="+$(".secondlevel_menutabs.on").attr("id")+"&showServiceBundles=true&"
+lang;}else
window.location="/portal/portal/browse_catalogs?t="+$(".secondlevel_menutabs.on").attr("id")+"&showServiceBundles=true";});$("#l3_account_0_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/tenants/list?accountType="
+selectedAccountType+"&filterBy=0";});$("#l3_account_1_tab").bind("click",function(event){$(this).removeClass("off").addClass("on");window.location="/portal/portal/tenants/list?accountType="
+selectedAccountType+"&filterBy=1";});$("#l3_account_2_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/tenants/list?accountType="
+selectedAccountType+"&filterBy=2";});$("#l3_account_3_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/tenants/list?accountType="
+selectedAccountType+"&filterBy=3";});$("#l3_account_4_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/tenants/list?accountType="
+selectedAccountType+"&filterBy=4";});$("#l3_profile_serviceprovider_tab").bind("click",function(event){initialiseProfilesPage("spProfiles");});$("#l3_profile_customer_tab").bind("click",function(event){initialiseProfilesPage("customerProfiles");});$("#l3_profile_partner_tab").bind("click",function(event){initialiseProfilesPage("");});$("#12_content_0_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/admin/emailtemplates?filterBy=0";});$("#12_content_1_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/admin/emailtemplates?filterBy=1";});$("#12_content_2_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/admin/emailtemplates?filterBy=2";});$("#12_content_3_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/admin/emailtemplates?filterBy=3";});$("#l3_billing_record_deposit_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/billing/show_record_deposit?tenant="
+l3menuTenantParam;});$("#l3_config_configaccountmanagement_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/admin/config/showconfiguration?module=ConfigAccountManagement";});$("#l3_config_accountprovisioning_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/admin/config/showconfiguration?module=AccountProvisioning";});$("#l3_config_billing_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/admin/config/showconfiguration?module=Billing";});$("#l3_config_crm_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/admin/config/showconfiguration?module=CRM";});$("#l3_config_helpdesk_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/admin/config/showconfiguration?module=HelpDesk";});$("#l3_config_integration_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/admin/config/showconfiguration?module=Integration";});$("#l3_home_connectors_cs_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/connector/cs";});$("#l3_home_connectors_oss_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/connector/oss";});$("#l3_config_server_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/admin/config/showconfiguration?module=Server";});$("#l3_config_portal_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/admin/config/showconfiguration?module=Portal";});$("#l3_config_reports_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/admin/config/showconfiguration?module=Reports";});$("#l3_config_marketing_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/admin/config/showconfiguration?module=Marketing";});$("#l3_config_trialmanagement_tab").bind("click",function(event){$(".thirdlevel_subtab").removeClass("on").addClass("off");$(this).removeClass("off").addClass("on");window.location="/portal/portal/admin/config/showconfiguration?module=TrialManagement";});function isBrowserIE7(){if(navigator.appVersion.indexOf('MSIE 7.')==-1)
return false;return true;}
function viewUtilitRates(tenantParam,id,url){var target_url="/portal/portal/subscription/utilityrates_lightbox?tenant="+tenantParam;if(url!=null){target_url=url;}
$.ajax({type:"GET",url:target_url,dataType:"html",cache:false,success:function(html){initDialog(id,800);var $thisDialog=$("#"+id);$thisDialog.dialog('option','height',700);$thisDialog.dialog('option','minWidth',750);$thisDialog.dialog('option','minHeight',400);$thisDialog.dialog('option','buttons',{"OK":function(){$(this).dialog("close");}});dialogButtonsLocalizer($thisDialog,{'Cancel':g_dictionary.dialogOK});$thisDialog.html(html);$thisDialog.dialog("open");}});return false;}
function dialogButtonsLocalizer(dialogPanel,localizedButtonLabels){var buttons=dialogPanel.dialog("option","buttons");var localizedButtons={};$.each(buttons,function(key,value){if(localizedButtonLabels[key]){localizedButtons[localizedButtonLabels[key]]=value;}else{localizedButtons[key]=value;}});dialogPanel.dialog("option","buttons",localizedButtons);}
function popUpDialogForAlerts(dialog_div_id,message){initDialog(dialog_div_id,390);$thisDialog=$("#"+dialog_div_id);$thisDialog.html(message);$thisDialog.dialog('option','buttons',{"OK":function(){$(this).dialog("close");}});dialogButtonsLocalizer($thisDialog,{'OK':g_dictionary.dialogOK});$thisDialog.bind("dialogbeforeclose",function(event,ui){$thisDialog.empty();});$thisDialog.dialog("open");}
function getTaxableAmount(amount){var taxableAmount=amount;$.ajax({type:"GET",url:"/portal/portal/subscription/taxable_amount?amount="+amount,async:false,dataType:"html",success:function(json){taxableAmount=json;}});return taxableAmount;}
function i18nUomText(uom){switch(uom){case"GB-Months":if(g_dictionary.GB_Months!=null&&g_dictionary.GB_Months!="undefined"){uom=g_dictionary.GB_Months;}
break;case"GB":if(g_dictionary.GB!=null&&g_dictionary.GB!="undefined"){uom=g_dictionary.GB;}
break;case"IP-Month":if(g_dictionary.IP_Month!=null&&g_dictionary.IP_Month!="undefined"){uom=g_dictionary.IP_Month;}
break;case"Rules":if(g_dictionary.Rules!=null&&g_dictionary.Rules!="undefined"){uom=g_dictionary.Rules;}
break;case"Hours":if(g_dictionary.Hours!=null&&g_dictionary.Hours!="undefined"){uom=g_dictionary.Hours;}
break;}
return uom;}
function i18nBooleanString(field){switch(field){case true:if(g_dictionary.labelTrue!=null&&g_dictionary.labelTrue!="undefined"){field=g_dictionary.labelTrue;}
break;case false:if(g_dictionary.labelFalse!=null&&g_dictionary.labelFalse!="undefined"){field=g_dictionary.labelFalse;}
break;}
return field;}
function focusFirstItemInGivenContainer(container_id){var firstInput=$("#"+container_id).find('input[type=text],input[type=password],input[type=radio],input[type=checkbox],textarea,select').filter(':visible:first');if(firstInput!=null){firstInput.focus();}}
function localizeCloudStackMsg(msg){var localizedMsg="";if((msg)&&(language!="en")){for(var i=0;i<CloudStack_Message_Regexp.length;i++){var re=new RegExp("");re.compile(CloudStack_Message_Regexp[i][0]);if(re.test(msg)){var execResult=re.exec(msg);if(execResult[0]==msg){if(CloudStack_Message_Regexp[i].length<=2){localizedMsg=msg.replace(re,CloudStack_Message_Regexp[i][1]);}else{var key=subKey="";if(CloudStack_Message_Regexp[i].length==3){localizedMsg=msg.replace(re,CloudStack_Message_Regexp[i][1]);for(var j=0;(j<CloudStack_Message_Regexp[i][2].length)||(j<execResult.length-1);j++){if(CloudStack_Message_Regexp[i][2][j]!=null){subKey=execResult[j+1];re.compile(subKey);key=CloudStack_Message_Regexp[i][2][j]+"_"+subKey;if(CloudStack_Message_Args[key]){localizedMsg=localizedMsg.replace(re,CloudStack_Message_Args[key]);}}}}else if(CloudStack_Message_Regexp[i].length==4){for(var j=0;(j<CloudStack_Message_Regexp[i][2].length)||(j<execResult.length-1);j++){if(CloudStack_Message_Regexp[i][2][j]){if(key!="")key+="_";key+=execResult[j+1];}}
if(CloudStack_Message_Regexp[i][3][key]){localizedMsg=msg.replace(re,CloudStack_Message_Regexp[i][3][key]);}}}
break;}}}
if(localizedMsg=="")localizedMsg=msg;}
else{localizedMsg=msg;}
return localizedMsg;}
function checkDelinquent(isDelinquent,redirectToBilling,redirectToDashBoard,showMakePaymentMessage,tenantParam){if(isDelinquent==true){if(redirectToBilling==true){window.location="/portal/portal/billing/history?tenant="+tenantParam+"&action=launchvm";}
else if(redirectToDashBoard==true){alert(showMakePaymentMessage);window.location="/portal/portal/home?tenant="+tenantParam+"&secondLevel=true";}
return false;}
return true;}
function addParametersToUrl(url,key,value){if(url.indexOf("?")>=0){url=url+"&";}else{url=url+"?";}
url=url+key+"="+value;return url;}
function singleSignOn(tenantParam,serviceInstanceUUID){if(serviceInstanceUUID==null||serviceInstanceUUID==""){return false;}
var returnVal=true;var url="/portal/portal/dashboard/manageresource/getSSOCmdString";if(tenantParam!=null&&tenantParam!=""){url=addParametersToUrl(url,"tenant",tenantParam);}
url=addParametersToUrl(url,"serviceInstanceUUID",serviceInstanceUUID);$.ajax({type:"POST",url:url,async:false,dataType:"json",success:function(responseMap){if(responseMap.status=='fail'){if(responseMap.error_message!=null){alert(responseMap.error_message);}
returnVal=responseMap.url;}else{if(responseMap.callback!=undefined){}}},error:function(XMLHttpResponse){alert('Error while getting SSO command string');}});return returnVal;}
function updateServiceInstanceItems(current,fn,tenantParam){var serviceInstanceUUID=$(current).attr("id");$selectedServiceInstance=$("#selectedServiceInstance");$("#serviceInstanceUuid").val(serviceInstanceUUID);$selectedServiceInstance.html($(current).text()+"<div class='downarrow' id='"+$(current).attr("id")+"'></div>");$("#serviceInstanceDropdownlist").css('display','none');fn(serviceInstanceUUID,tenantParam);}
function populateServiceInstances(serviceCategory,tenantParam,fn,firstTimefn){if(typeof serviceCategory=='undefined'){return;}
actionUrl="/portal/portal/connector/getServiceInstanceList"
if($("#viewChannelCatalog").val()=='true'){actionUrl=addParametersToUrl(actionUrl,"viewCatalog",true);}
$.ajax({type:"GET",url:actionUrl,async:true,data:{category:serviceCategory,tenant:tenantParam},dataType:"json",success:function(items){var serviceInstanceSelect=$("#serviceInstanceList");serviceInstanceSelect.empty();var selectedCloudServiceInstance=$("#selectedCloudServiceInstance").val();if(typeof selectedCloudServiceInstance=='undefined'||selectedCloudServiceInstance==null||selectedCloudServiceInstance==""){selectedCloudServiceInstance=null;}
if(items!=null&&items.length>0){$("#serviceInstanceListContainer").show();var serviceInstanceUUID=items[0].uuid;if(selectedCloudServiceInstance==null){var $selectedServiceInstance=$("#selectedServiceInstance");$selectedServiceInstance.html(items[0].name+"<div class='downarrow' id='"+items[0].uuid+"'></div>");}
var serviceInstanceItem;var select_option;for(var i=0;i<items.length;i++){serviceInstanceItem=items[i];if(serviceInstanceItem.uuid==selectedCloudServiceInstance){var $selectedServiceInstance=$("#selectedServiceInstance");$selectedServiceInstance.html(serviceInstanceItem.name+"<div class='downarrow' id='"+serviceInstanceItem.uuid+"'></div>");serviceInstanceUUID=selectedCloudServiceInstance;}
select_option="<li id='"+serviceInstanceItem.uuid+"' onclick='updateServiceInstanceItems(this, "+fn+", \""+tenantParam+"\");' >"+serviceInstanceItem.name+"</li>";serviceInstanceSelect.append(select_option);}
$("#serviceInstanceListContainer").bind("mouseover",function(event){$(this).find("#serviceInstanceDropdownlist").show();return false;});$("#serviceInstanceListContainer").bind("mouseout",function(event){var $thisElement=$(this)[0];var relatedTarget1=event.relatedTarget;while(relatedTarget1!=null&&relatedTarget1.nodeName!="BODY"&&relatedTarget1!=$thisElement){relatedTarget1=relatedTarget1.parentNode;}
if(relatedTarget1==$thisElement){return;}
$(this).find("#serviceInstanceDropdownlist").hide();return false;});if(typeof firstTimefn!='undefined'){firstTimefn(serviceInstanceUUID,tenantParam);}}else{$("#serviceInstanceListContainer").hide();if(typeof firstTimefn!='undefined'){firstTimefn("",tenantParam);}}},error:function(){}});}
function displayErrorDialog(XMLHttpResponse){var textToDisplay="Error";if(XMLHttpResponse.status===INTERNAL_SERVER_FAILED_CODE){textToDisplay=XMLHttpResponse.statusText+"\n"+XMLHttpResponse.responseText;}
initDialogWithOK("dialog_info",350,false);$("#dialog_info").dialog("option","height",150);$("#dialog_info").text(textToDisplay).dialog("open");}
$(".workflowDetailsPopup").live("click",function(){var $opener=$(this);var workflowUUID=$opener.attr('id').replace("workflowdetails","");var workflowDetailsurl="/portal/portal/workflow/"+workflowUUID;var workflowDetailsGet=$.ajax({type:"GET",url:workflowDetailsurl,dataType:"html"});$(".workflow_details_popup").each(function(index){if(index!=0){$(this).remove();}});var $workflowDetailsDialog=$(".workflow_details_popup");$workflowDetailsDialog.dialog({width:700,modal:true,resizable:false,autoOpen:false,buttons:{"OK":function(){$workflowDetailsDialog.dialog("close");}},close:function(event,ui)
{$workflowDetailsDialog.dialog("destroy").html("");}});dialogButtonsLocalizer($workflowDetailsDialog,{'OK':g_dictionary.dialogOK});workflowDetailsGet.done(function(html){$workflowDetailsDialog.html(html);$workflowDetailsDialog.data('opener',$opener).dialog("open");});});$(".workflow_details_popup .workflow_activitycontainer .statusearea a").die("click").live("click",function(){$(this).closest(".workflow_activitycontainer").find(".error").slideToggle('slow');});function expandActivityDetails(bucketNumber){var $divToExpand=$("#activitypanel"+bucketNumber);$('.activitypanel').each(function(){if(!$(this).hasClass("hide")&&$(this).attr('id').replace("activitypanel","")!=$divToExpand.attr('id').replace("activitypanel","")){$(this).addClass("hide");}});$divToExpand.toggleClass("hide");}
$(".workflow_resetbutton").live("click",function(){var workflowUUID=$(this).closest(".workflow_details_popup").data('opener').attr('id').replace("workflowdetails","")
var resetUrl="/portal/portal/workflow/"+workflowUUID+"/reset";var resetWorkflowGet=$.ajax({type:"POST",url:resetUrl,dataType:"text"});resetWorkflowGet.done(function(text){if(text=='true'){$("#workflowdetails"+workflowUUID).click();}});});function isNotBlank(str){if(str!=null&&str!=""&&typeof(str)!="undefined"){return true;}
return false;}
function isBlank(str){return!isNotBlank(str);};