/* Copyright (C) 2011 Cloud.com, Inc.  All rights reserved. */
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
