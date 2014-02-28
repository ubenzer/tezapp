"use strict"
ngDefine "controllers.results.entityRelationDetail", (module) ->
  module.controller "results.entityRelationDetail", ($scope, $http) ->
    if(!angular.isObject($scope.entityRelationType)) then throw new Error("We need entityRelationType to be defined in scope")
    if(!angular.isString($scope.entity?.uri)) then throw new Error("We need entity#uri to be defined in scope")
    if(!angular.isFunction($scope.isElementSelected)) then throw new Error("We need isElementSelected to be defined in scope")
    if(!angular.isFunction($scope.removeItem)) then throw new Error("We need removeItem to be defined in scope")
    if(!angular.isFunction($scope.addItem)) then throw new Error("We need addItem to be defined in scope")

    $scope.showMoreResults = () -> $scope.resultLimit += 50
    $scope.hasMoreResults = () -> $scope.resultLimit < $scope.relations.length

    $scope.load = () ->
      $scope.resultLimit = 20
      $scope.loading = true
      $scope.error = false
      $scope.relations = []

      # Do search!
      $http.post("/relation", {
        uri: $scope.entity.uri
        relationType: $scope.entityRelationType.type
      })
      .success (data) ->
        $scope.relations = processResults(data.searchResults)
      .error () ->
        $scope.error = true
      .finally () ->
        $scope.loading = false
    $scope.load()

    return
  return