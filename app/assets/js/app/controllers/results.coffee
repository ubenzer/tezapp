"use strict"
ngDefine "controllers.results", (module) ->

  module.controller "controllers.results", ($scope, $stateParams) ->
    console.debug($stateParams)
    $scope.bar = $stateParams
    return