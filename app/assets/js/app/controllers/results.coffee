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

    $scope.resultList = {}

    $scope.selectedElements = {} # We use instead of array due to performance reasons.
    $scope.isElementSelected = (uri) -> $scope.selectedElements[uri]?
    $scope.selectElement = (uri) -> $scope.selectedElements[uri] = true;
    $scope.deselectElement = (uri) ->  delete $scope.selectedElements[uri];
    $scope.toggleElement = (uri) ->
      if($scope.selectedElements[uri]?)
        delete $scope.selectedElements[uri];
      else
        $scope.selectedElements[uri] = true;
    $scope.getSelectedElementCount = () -> Object.keys($scope.selectedElements).length


    console.debug($scope.searchConfig)
    $http.post("/search", $scope.searchConfig)
      .success (data) ->
        console.debug(data)
        $scope.resultList = data.searchResults;
      .error () ->
        alert("Some error occurred. Please resubmit your search. Sorry. :/")
      .finally () ->
          $scope.pageControls.searchInProgress = false

    return