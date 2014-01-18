"use strict"
ngDefine "controllers.results.header", (module) ->

  module.controller "results.header", ($scope, SearchSerializer, $state) ->
    $scope.isCollapsed = true

    # This is a local copy of search config. Since we
    # keep changes 'unsearched' until user presses 'search'
    # we can't change the original config (and uri)
    $scope.formData = angular.copy($scope.searchConfig) # keywords, offline
    $scope.doSearch = () ->
      if($scope.formData.keywords.length == 0) then return
      serializedSearch = SearchSerializer.serialize($scope.formData.keywords, $scope.formData.offline)
      $state.go("results", {searchParams: serializedSearch})
    return