"use strict"
ngDefine "controllers.search", (module) ->

  module.controller "search", ($scope, $state, SearchSerializer) ->
    $scope.formData = {
      keywords: []
      offline: true
    }
    $scope.doSearch = () ->
      if($scope.formData.keywords.length == 0) then return
      serializedSearch = SearchSerializer.serialize($scope.formData.keywords, $scope.formData.offline)
      $state.go("results", {searchParams: serializedSearch})
    return
  return