"use strict"
ngDefine "controllers.results", [
  "module:controllers.results.header"
], (module) ->

  module.controller "results", ($scope, $state, $stateParams, searchSerializer, UrlConfig) ->
    $scope.searchConfig = searchSerializer.deserialize($stateParams.searchParams)
    if(!$scope.searchConfig)
      $state.go("search")
      return

    # Initianize page parts
    resultsPageBaseUrl = UrlConfig.htmlBaseUrl + "/results"
    $scope.pageParts = {
      header: resultsPageBaseUrl + "/header.html"
    }


    return