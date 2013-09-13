/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */
$(document).ready(function() { 
$(".tree_links").click(function() {
      var id = $(this).attr("id");
      id = id + "_details";
      $(".details_content").hide();
      $("#" + id).show();
    });

    $(".inner_links").click(function() {
      var id = $(this).attr("id");
      id = id.replace(/inner_/, '') + "_details";
      $(".details_content").hide();
      $("#" + id).show();
    });
    
});
