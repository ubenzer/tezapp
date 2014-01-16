"use strict"
ngDefine "controllers.results", (module) ->

  module.controller "controllers.results", ($scope, $state, $stateParams, searchSerializer) ->
    $scope.searchConfig = searchSerializer.deserialize($stateParams.searchParams)
    if(!$scope.searchConfig)
      $state.go("search")
      return

    return