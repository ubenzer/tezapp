$("#swoogle-submit").click(function() {
  var keyword = $("#swoogle-input").val();
  
  $("#common-busy").fadeIn();
  routes.controllers.Swoogle.submit(keyword).ajax()
  
  .always(function(data) {
    $("#common-busy").fadeOut();
  })
  .success(function(data) { 
    console.debug(data);
    alert("Total: " + data.total);
    alert("Failed: " + data.failed);
    alert("Not 200: " + data.not200);
    alert("Duplicate: " + data.duplicate);
    alert("Not ontology: " + data.notOntology);
    alert("Ok: " + data.ok);
    
  }).error(function(data) {
    
    

    console.debug("error");
    console.debug(data);
  });
});