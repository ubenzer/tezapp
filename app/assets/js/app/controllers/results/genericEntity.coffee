"use strict"
ngDefine "controllers.results.genericEntity", [
  "module:controllers.results.genericEntity"
], (module) ->
  module.controller "results.genericEntity", ($scope, SelectedItems, PrettyNaming) ->
    if(!angular.isObject($scope.result)) then throw new Error("We need result to be defined in scope")
    if(!angular.isFunction($scope.showEntityDetail)) then throw new Error("We need showEntityDetail to be defined in scope")

    $scope.isElementSelected = (uri) -> SelectedItems.isItemSelected(uri)
    $scope.toggleElement = (uri) ->
      if(SelectedItems.isItemSelected(uri))
        SelectedItems.removeItem(uri)
      else
        SelectedItems.addItem(uri)
      return

    $scope.result.kindPretty = PrettyNaming.for($scope.result.kind)
    $scope.result.className = PrettyNaming.classNameFor($scope.result.kind)
    $scope.result.popover =
      "<p><strong>Type:</strong> " + $scope.result.kindPretty + "</p>" +
      "<p><small>" + ($scope.result.comment || "") + "</small></p>"

    return
  return