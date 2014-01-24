"use strict"
ngDefine "controllers.results", [
  "module:controllers.results.header"
], (module) ->

  module.controller "results", ($scope, $state, $stateParams, SearchSerializer, UrlConfig, $timeout) ->
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


    # Mock loading
    $timeout(
      () ->
        $scope.pageControls.searchInProgress = false
    , 50)


    return