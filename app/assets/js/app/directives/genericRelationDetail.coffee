"use strict"
ngDefine "directives.genericRelationDetail", (module) ->

  module.controller "directives.genericRelationDetail", ($scope, $http) ->
    relationSize = 0
    $scope.showMoreResults = () -> $scope.resultLimit += 5
    $scope.hasMoreResults = () -> $scope.resultLimit < relationSize

    $scope.load = () ->
      $scope.resultLimit = 5
      $scope.loading = true
      $scope.error = false
      $scope.relations = []

      # Do search!
      $http.post("/relation/" + $scope.relationData.searchFor, {
        uri: $scope.entityUri
        predicate: $scope.relationData.predicate
      })
      .success (data) ->
        relationSize = data.length
        $scope.relations = data
        return
      .error () ->
        $scope.error = true
        return
      .finally () ->
        $scope.loading = false
        return
    $scope.load()

    $scope.detailFnWrapper = (relation, activate) ->
      $scope.detailFn({
        $relationEntity: relation
        $activate: activate
      })
      return
    return

  module.directive "genericRelationDetail", (UrlConfig) ->
    {
      restrict: "E"
      templateUrl: UrlConfig.htmlBaseUrl + "/directives/genericRelationDetail.html"
      replace: true
      scope:
        entityUri: "="
        relationData: "="
        showNavigation: "="
        detailFn: "&onDetailAction"
      controller: "directives.genericRelationDetail"
    }