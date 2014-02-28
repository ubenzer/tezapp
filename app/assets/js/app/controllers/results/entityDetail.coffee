"use strict"
ngDefine "controllers.results.entityDetail", [
  "module:controllers.results.entityRelationDetail"
], (module) ->
  module.controller "results.entityDetail", ($scope) ->
    if(!angular.isObject($scope.entityDetail?.searchResultObj)) then throw new Error("We need entityDetail#searchResultObj to be defined in scope")
    if(!angular.isString($scope.pageParts?.entityDetailTab)) then throw new Error("We need pageParts#entityDetailTab to be defined in scope")
    if(!angular.isFunction($scope.isElementSelected)) then throw new Error("We need isElementSelected to be defined in scope")
    if(!angular.isFunction($scope.removeItem)) then throw new Error("We need removeItem to be defined in scope")
    if(!angular.isFunction($scope.addItem)) then throw new Error("We need addItem to be defined in scope")
    $scope.entity = $scope.entityDetail.searchResultObj.element
    $scope.entity.title = $scope.entity.label || $scope.entity.uri

    $scope.columnEntityRelationTypes = []

    entityRelationTypes = [
      {
        name: "has domains"
        type: "hasDomains"
      }
      {
        name: "has ranges"
        type: "hasRanges"
      }
    ]
    itemCountInEachColumn = Math.ceil(entityRelationTypes.length / 2)
    for i in [0..1]
      $scope.columnEntityRelationTypes.push(entityRelationTypes.slice(i * itemCountInEachColumn, (if(i == 0) then itemCountInEachColumn else undefined)))

    return
  return