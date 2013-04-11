$("#swoogle-submit").click(function() {
  var keyword = $("#swoogle-input").val();
  
  $("#common-busy").fadeIn();
  routes.controllers.Swoogle.submit(keyword).ajax()
  
  .always(function(data) {
    $("#common-busy").fadeOut();
  })
  .success(function(data) { 
    console.debug(data);
    alert("====STATS FOR SWOOGLE====\n" +
        "Total: "        + data.total  + "\n" +
        "Failed: "       + data.failed + "\n" +
        "Not 200: "      + data.not200 + "\n" +
        "Duplicate: "    + data.duplicate + "\n" +
        "Not ontology: " + data.notOntology + "\n" +
        "Ok: "           + data.ok);
    
  }).error(function(data) {
    
    

    console.debug("error");
    console.debug(data);
  });

});

$("#sindice-submit").click(function() {
  var keyword = $("#sindice-input").val();
  
  $("#common-busy").fadeIn();
  routes.controllers.Sindice.submit(keyword).ajax()
  
  .always(function(data) {
    $("#common-busy").fadeOut();
  })
  .success(function(data) { 
    console.debug(data);
    alert("====STATS FOR SINDICE====\n" +
        "Total: "        + data.total  + "\n" +
        "Failed: "       + data.failed + "\n" +
        "Not 200: "      + data.not200 + "\n" +
        "Duplicate: "    + data.duplicate + "\n" +
        "Not ontology: " + data.notOntology + "\n" +
        "Ok: "           + data.ok);
    
  }).error(function(data) {
    
    
    
    console.debug("error");
    console.debug(data);
  });
  
});


$("#watson-submit").click(function() {
  var keyword = $("#watson-input").val();
  
  $("#common-busy").fadeIn();
  routes.controllers.Watson.submit(keyword).ajax()
  
  .always(function(data) {
    $("#common-busy").fadeOut();
  })
  .success(function(data) { 
    console.debug(data);
    alert("====STATS FOR WATSON====\n" +
        "Total: "             + data.total  + "\n" +
        "Failed: "            + data.failed + "\n" +
        "Not 200: "           + data.not200 + "\n" +
        "Duplicate: "         + data.duplicate + "\n" +
        "Not ontology: "      + data.notOntology + "\n" +
        "Livejournal count: " + data.liveJournal + "\n" +
        "Deadjournal count: " + data.deadJournal + "\n" +
        "Ok: "                + data.ok);
    
  }).error(function(data) {
    
    
    
    console.debug("error");
    console.debug(data);
  });
  
});