"use strict"
ngDefine "controllers.results", [
  "module:controllers.results.header"
], (module) ->

  module.controller "results", ($scope, $state, $stateParams, SearchSerializer, UrlConfig, $http) ->
    $scope.searchConfig = SearchSerializer.deserialize($stateParams.searchParams)
    if(!$scope.searchConfig)
      $state.go("search")
      return

    # Initianize page parts
    resultsPageBaseUrl = UrlConfig.htmlBaseUrl + "/results"
    # Basic configuration for subpages.
    $scope.pageParts = {
      header: resultsPageBaseUrl + "/header.html"
    }
    # Data for page parts
    $scope.pageControls = {
      searchInProgress: true
    }


    console.debug($scope.searchConfig)
    $http.post("/search", $scope.searchConfig)

    .success (data) ->
      console.debug(data)
    .error () ->
      alert("Some error occurred. Please resubmit your search. Sorry. :/")
    .finally () ->
        $scope.pageControls.searchInProgress = false


    return