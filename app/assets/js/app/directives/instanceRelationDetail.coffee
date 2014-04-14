"use strict"
ngDefine "directives.instanceRelationDetail", (module) ->

  module.controller "directives.instanceRelationDetail", ($scope, $http) ->
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


      promise = _load($scope.entityUri, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "object", 0).then (data) ->
        data.forEach (relation) ->
          relation.hierarcy = []
          relation.loading = true
          relation.error = false
          _load(relation.uri, "http://www.w3.org/2000/01/rdf-schema#subClassOf", "object", 5)
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
      return promise

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

  module.directive "instanceRelationDetail", (UrlConfig) ->
    {
      restrict: "E"
      templateUrl: UrlConfig.htmlBaseUrl + "/directives/instanceRelationDetail.html"
      replace: true
      scope:
        entityUri: "="
        showNavigation: "="
        detailFn: "&onDetailAction"
      controller: "directives.instanceRelationDetail"
    }