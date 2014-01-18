"use strict"
ngDefine "controllers.results.header", (module) ->

  module.controller "results.header", ($scope, SearchSerializer, $state) ->
    $scope.isCollapsed = true

    return