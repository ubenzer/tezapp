"use strict"
ngDefine "directives.genericRelationDetail", (module) ->

  module.controller "directives.genericRelationDetail", ($scope, $http) ->
    $scope.showBlock = true

    $scope.load = () ->
      $scope.relations = []

      _load = (uri, predicate, whatToSearchFor, hierarchyLeft) ->
        # Do search!
        $http.post("/relation/" + whatToSearchFor, {
          uri: uri
          predicate: predicate
        })
        .then (rawData) ->
          data = rawData.data
          if(hierarchyLeft > 0)
            data.forEach (relation) ->
              relation.hierarcy = []
              relation.loading = true
              relation.error = false
              _load(relation.uri, predicate, whatToSearchFor, hierarchyLeft--)
                .then (data) ->
                  relation.hierarcy = data
                  relation.loading = false
                  return
                , () ->
                  relation.loading = false
                  relation.error = true
                  return
              return

          return data

      if(!$scope.relationData.hierarchyCount?) then $scope.relationData.hierarchyCount = 0
      _load($scope.entityUri, $scope.relationData.predicate, $scope.relationData.searchFor, $scope.relationData.hierarchyCount)

    $scope.allLoading = true
    $scope.allFailed = false
    $scope.load().then (data) ->
      $scope.relations = data
      $scope.allLoading = false
      return
    , () ->
      $scope.allLoading = false
      $scope.allFailed = true

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