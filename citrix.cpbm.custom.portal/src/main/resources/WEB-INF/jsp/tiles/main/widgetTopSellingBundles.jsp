<%-- Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<script type="text/javascript">                     

$(document).ready(function(){

  
  var chartData = '<c:out value="${report.data}" escapeXml="false"/>';
  var chartDataJSON = $.parseJSON(chartData);
  var to_be_shown=4;
  $("#non_item_list").show();
  if(chartDataJSON.usageMap.length > 0){
      $("#non_item_list").hide();
  }
  if(chartDataJSON.usageMap.length < to_be_shown){
      to_be_shown=chartDataJSON.usageMap.length;
  }
  
  for(var product=0; product < to_be_shown; product++){
    
    var product_usage = parseFloat(chartDataJSON.usageMap[product].data);
    $("#list_item"+product).show();
    $("#counter"+product).flipCounter({number:(product_usage+500), numIntegralDigits:4,numFractionalDigits:0});
    $("#counter"+product).flipCounter("startAnimation", {end_number:product_usage, easing:jQuery.easing.easeInOutCubic, duration:1000});
    $("#product"+product).text(chartDataJSON.usageMap[product].productname);
    };
  
});
  </script>
  

<ul id="counters">
  
  <li id="list_item0" style="display:none;">
    <div id="product0" class="flipcounter_row_title"></div>
    <div class="flipcounter" id="counter0">
      <input type="hidden" name="counter-value" value="0" />
    </div>
  </li>
  <li id="list_item1" style="display:none;">
    <div id="product1" class="flipcounter_row_title"></div>
    <div class="flipcounter" id="counter1">
      <input type="hidden" name="counter-value" value="0" />
    </div>
  </li>
  <li id="list_item2" style="display:none;">
    <div id="product2" class="flipcounter_row_title"></div>
    <div class="flipcounter" id="counter2">
      <input type="hidden" name="counter-value" value="0" />
    </div>
  </li>
  <li id="list_item3" style="display:none;">
    <div id="product3" class="flipcounter_row_title"></div>
    <div class="flipcounter" id="counter3">
      <input type="hidden" name="counter-value" value="0" />
    </div>
  </li>
  
</ul>