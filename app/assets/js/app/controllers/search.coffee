"use strict"
ngDefine "controllers.search", (module) ->

  module.controller "controllers.search", ($scope, $state, searchSerializer) ->
    $scope.formData = {
      searchTerms: []
      offline: false
    }
    $scope.doSearch = () ->
      if($scope.formData.searchTerms.length == 0) then return

      serializedSearch = searchSerializer.serialize($scope.formData.searchTerms)

      $state.go("results", {search: serializedSearch})
    return
  return