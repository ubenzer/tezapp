$("div.ontology-search-element").each(function() {
  var searchElement = $(this);
  var endpointName = searchElement.data("endpoint");
  var idPrefix = searchElement.data("idPrefix");
  
  var submitButton = searchElement.find("#" + idPrefix + "-submit");
  var input = searchElement.find("#" + idPrefix + "-input");
  var loading = searchElement.find("#" + idPrefix + "-busy");
  var chartArea = searchElement.find("#" + idPrefix + "-chart");
  var chartDescArea = searchElement.find("#" + idPrefix + "-chart-desc");
  
  var submitFunction = function(e) {
    if(e) { e.preventDefault(); };
    
    if(loading.is(":visible")) {
      alert("Şu an zaten bir arama yapılıyor!");
      // TODO BEAUTIFY IT!
    }
    
    var keyword = $.trim(input.val());
    if(keyword.length == 0) {
      return;
    }
    // TODO ADD SIMPLE VALIDATION!
        
    loading.fadeIn();
    routes.controllers[endpointName].submit(keyword).ajax()
    
    .always(function(data) {
      loading.fadeOut();
    })
    .success(function(data) {
      input.val("");
      console.debug(data);
      var totalCount = data.total;
      searchElement.data("stats", data);
      alert("====STATS FOR " + idPrefix + "====\n" +
          "Total: "        + data.total  + "\n" +
          "Failed: "       + data.failed + "\n" +
          "Not 200: "      + data.not200 + "\n" +
          "Duplicate: "    + data.duplicate + "\n" +
          "Not ontology: " + data.notOntology + "\n" +
          (data.liveJournal ? "Livejournal count: " + data.liveJournal + "\n" : "") +
          (data.deadJournal ? "Deadjournal count: " + data.deadJournal + "\n" : "") +
          "Ok: "           + data.ok);
      
     var data = [
       { label: "Failed",  data: data.failed },
       { label: "Not 200",  data: data.not200 },
       { label: "Duplicate",  data: data.duplicate },
       { label: "Not ontology",  data: data.notOntology },
       { label: "Ok: ",  data: data.ok }
     ];
     
     if(data.liveJournal) {
       data.push({label: "Livejournal",  data: data.liveJournal})
     };
     if(data.deadJournal) {
       data.push({label: "DeadJournal",  data: data.deadJournal})
     };
     
     var labelFormatter = function(label, series) {
       return "<div style='font-size:8pt; text-align:center; padding:2px; color:white;'>" + label + "<br/>" + Math.round(series.percent) + "%</div>";
     }
     
     chartArea.css("height", chartArea.width() * 3 / 4).css("margin-top", "8px");
     $.plot(chartArea, data, {
        series: {
          pie: {
            show: true,
            radius: 1,
            label: {
              show: true,
              radius: 3/4,
              formatter: labelFormatter,
              background: {
                opacity: 0.5
              }
            }
          }
        },
        legend: {
          show: false
        },
        grid: {
          hoverable: true,
          clickable: true
        }
     });
     chartArea.unbind("plothover").bind("plothover", function(event, pos, obj) {

       if (!obj) {
         return;
       }
       //var percent = parseFloat(obj.series.percent).toFixed(2);
       var percent = obj.datapoint[1][0][1];
       chartDescArea.html("<span style='font-weight:bold; color:" + obj.series.color + "'>" + obj.series.label + " (" + percent + " / " + totalCount +")</span>");
     });
     
      
    }).error(function(data) {
      // TODO MAKE FLOW BETTER!
      alert("Bir hata oluştu!");
      console.debug("error in " + idPrefix);
      console.debug(data);
    });
  };
  
  input.keypress(function(e) {
    if (e.which == 13) {
      submitFuntion(e);
    }
  });
  submitButton.click(submitFunction);
});;