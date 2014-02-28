"use strict"
ngDefine "controllers.results.entityDetail", (module) ->
  module.controller "results.entityDetail", ($scope) ->
    if(!angular.isObject($scope.entityDetail?.searchResultObj)) then throw new Error("We need entityDetail to be defined in scope")
    if(!angular.isFunction($scope.isElementSelected)) then throw new Error("We need isElementSelected to be defined in scope")
    if(!angular.isFunction($scope.removeItem)) then throw new Error("We need removeItem to be defined in scope")
    if(!angular.isFunction($scope.addItem)) then throw new Error("We need addItem to be defined in scope")
    $scope.entity = $scope.entityDetail.searchResultObj.element
    $scope.entity.title = $scope.entity.label || $scope.entity.uri

    console.debug($scope.entityDetail)



    return
  return