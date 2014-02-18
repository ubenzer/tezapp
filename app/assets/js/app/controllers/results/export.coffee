"use strict"
ngDefine "controllers.results.export", ["Blob", "FileSaver"], (module) ->
  module.controller "results.export", ($scope, $http, SelectedItems, $filter) ->
    if(!angular.isObject($scope.export)) then throw new Error("We need export to be defined in scope")
    if(!angular.isArray($scope.export.exportFormats)) then throw new Error("We need exportFormats to be defined in scope")

    $scope.properties = {
      fileName: "TOK Export " + $filter('date')(new Date())
      format: $scope.export.exportFormats[0].name
    }

    $scope.export = $scope.export || {}
    $scope.export.exportFormats = $scope.export.exportFormats || []
    $scope.export.exportOngoing = false
    $scope.export.exportOntology = () ->
      if($scope.export.exportOngoing) then return
      $scope.export.exportOngoing = true
      # Flattern selected items into array
      tripleIds = []
      (tripleIds.push(k)) for own k of SelectedItems.getAllItems()

      $http.post("/export", {
        elements: tripleIds
        properties:
          format: "doesn't matter for now"
      })
      .success (data) ->
        blob = new Blob([data], {type: "text/plain;charset=" + document.characterSet})
        saveAs(blob, $scope.properties.fileName + ".owl")
        return
      .finally () ->
        $scope.export.exportOngoing = false
        return
      return
  return