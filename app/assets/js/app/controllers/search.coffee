"use strict"
ngDefine "controllers.search", (module) ->

  module.controller "search", ($scope, $state, searchSerializer) ->
    $scope.formData = {
      searchTerms: []
      offline: false
    }
    $scope.doSearch = () ->
      if($scope.formData.searchTerms.length == 0) then return
      serializedSearch = searchSerializer.serialize($scope.formData.searchTerms, $scope.formData.offline)
      $state.go("results", {searchParams: serializedSearch})
    return
  return