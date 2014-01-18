"use strict"
ngDefine "controllers.results", [
  "module:controllers.results.header"
], (module) ->

  module.controller "results", ($scope, $state, $stateParams, SearchSerializer, UrlConfig) ->
    $scope.searchConfig = SearchSerializer.deserialize($stateParams.searchParams)
    if(!$scope.searchConfig)
      $state.go("search")
      return

    # Initianize page parts
    resultsPageBaseUrl = UrlConfig.htmlBaseUrl + "/results"
    $scope.pageParts = {
      header: resultsPageBaseUrl + "/header.html"
    }


    return