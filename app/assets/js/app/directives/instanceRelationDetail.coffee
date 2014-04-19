"use strict"
ngDefine "directives.instanceRelationDetail", (module) ->

  module.controller "directives.instanceRelationDetail", ($scope, $http) ->
    $scope.showBlock = true

    $scope.showInstanceOfBlock = true
    $scope.instanceAllLoading = true
    $scope.instanceAllFailed = false
    $scope.loadInstanceOf = () ->
      $scope.instanceRelations = []

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

    $scope.loadInstanceOf().then (data) ->
      $scope.instanceRelations = data
      $scope.instanceAllLoading = false
      return
    , () ->
      $scope.instanceAllLoading = false
      $scope.instanceAllFailed = true


    $scope.getRelationsFor = (name, key, uri) ->
      tbReturned = {
        name: name
        showBlock: true
        allLoading: true
        allFailed: false
        relations: []
      }
      rqObj = {}
      rqObj[key] = uri
      $http.post("/relation/advanced", rqObj)
        .success (data) ->
          d[key] = "__OWN__" for d in data
          tbReturned.relations = data
          tbReturned.allLoading = false
        .error () ->
          tbReturned.allLoading = false
          tbReturned.allFailed = true
      tbReturned

    $scope.instanceTriples = [
      $scope.getRelationsFor($scope.entityName + " - Predicate - Object", "subject", $scope.entityUri)
      $scope.getRelationsFor("Subject - " + $scope.entityName + " - Object", "predicate", $scope.entityUri)
      $scope.getRelationsFor("Subject - Predicate - " + $scope.entityName, "object", $scope.entityUri)
    ]

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
        entityName: "="
        entityUri: "="
        showNavigation: "="
        detailFn: "&onDetailAction"
      controller: "directives.instanceRelationDetail"
    }