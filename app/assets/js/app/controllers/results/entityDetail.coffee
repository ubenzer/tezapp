"use strict"
ngDefine "controllers.results.entityDetail", [
  "module:controllers.results.entityRelationDetail"
], (module) ->
  module.controller "results.entityDetail", ($scope, SelectedItems, Utils, PrettyNaming) ->
    if(!angular.isObject($scope.entityDetail?.entityObj)) then throw new Error("We need entityDetail#entityObj to be defined in scope")
    if(!angular.isString($scope.pageParts?.entityDetailTab)) then throw new Error("We need pageParts#entityDetailTab to be defined in scope")

    $scope.removeItem = (uri) -> SelectedItems.removeItem(uri)
    $scope.addItem = (uri) -> SelectedItems.addItem(uri)
    $scope.isElementSelected = (uri) -> SelectedItems.isItemSelected(uri)

    $scope.entity = $scope.entityDetail.entityObj
    $scope.entity.title = $scope.entity.label || $scope.entity.uri
    $scope.entity.className = PrettyNaming.classNameFor($scope.entity.kind)

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

    $scope.columnEntityRelationTypes = Utils.splitArrayIntoChunks(2, entityRelationTypes)
    return
  return