/* Copyright 2013 Citrix Systems, Inc. Licensed under the BSD 2 license. See LICENSE for more details. */

$(document).ready(function() {
  $("#capacity_carousel_container").show();
  $("#foo2_pag").show();
  $(function() {
    $("#capacity_carousel_container").carouFredSel({
      circular: false,
      infinite: false,
      auto: false,
      prev: {
        button: "#foo2_prev",
        key: "left"
      },
      next: {
        button: "#foo2_next",
        key: "right"
      },
      pagination: "#foo2_pag"
    });
  });

});
