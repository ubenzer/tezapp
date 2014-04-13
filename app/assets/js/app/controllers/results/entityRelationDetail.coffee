"use strict"
ngDefine "controllers.results.entityRelationDetail", (module) ->
  module.controller "results.entityRelationDetail", ($scope, $http, Utils) ->
    if(!angular.isObject($scope.entityRelationType)) then throw new Error("We need entityRelationType to be defined in scope")
    if(!angular.isString($scope.entity?.uri)) then throw new Error("We need entity#uri to be defined in scope")

    relationSize = 0
    $scope.showMoreResults = () -> $scope.resultLimit += 5
    $scope.hasMoreResults = () -> $scope.resultLimit < relationSize

    $scope.load = () ->
      $scope.resultLimit = 5
      $scope.loading = true
      $scope.error = false
      $scope.columnedRelations = []

      # Do search!
      $http.post("/relation/" + $scope.entityRelationType.searchFor, {
        uri: $scope.entity.uri
        predicate: $scope.entityRelationType.predicate
      })
      .success (data) ->
        relationSize = data.length
        $scope.columnedRelations = Utils.splitArrayIntoEqualChunks(1, data)
      .error () ->
        $scope.error = true
      .finally () ->
        $scope.loading = false
    $scope.load()

    return
  return