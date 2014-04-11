"use strict"
ngDefine "directives.genericEntity", (module) ->

  module.controller "directives.genericEntity", ($scope, $filter, SelectedItems, PrettyNaming) ->
    $scope.isElementSelected = (uri) -> SelectedItems.isItemSelected(uri)
    $scope.toggleElement = (uri) ->
      if(!$scope.selectable) then return

      if(SelectedItems.isItemSelected(uri))
        SelectedItems.removeItem(uri)
      else
        SelectedItems.addItem(uri)
      return

    $scope.entityData.kindPretty = PrettyNaming.for($scope.entityData.kind)
    $scope.entityData.className = PrettyNaming.classNameFor($scope.entityData.kind)
    $scope.entityData.popover =
        "<p><strong>Type:</strong> " + $scope.entityData.kindPretty + "</p>" +
        "<p><small>" + ($filter("cut")(($scope.entityData.comment || "") , 150)) + "</small></p>"

    $scope.showEntityDetail = (event) ->
      event.stopPropagation()
      $scope.detailFn({$activate: (event.button != 1)})
      return

    return

  module.directive "genericEntity", (UrlConfig) ->

    {
      restrict: "E"
      templateUrl: UrlConfig.htmlBaseUrl + "/directives/genericEntity.html"
      replace: true
      scope:
        entityData: "=entity"
        selectable: "="
        showNavigation: "="
        detailFn: "&onDetailAction"
      controller: "directives.genericEntity"
    }