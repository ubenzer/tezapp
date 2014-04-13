"use strict"
ngDefine "directives.genericEntityList", (module) ->

  module.directive "genericEntityWithHierarchy", ($compile) ->
    {
      restrict: "E"
      template: "
        <div>
          <small style='float:right;' ng-if='entityData.loading'><i class='fa fa-lg fa-spinner fa-spin'></i></small>
          <small style='float:right;' ng-if='entityData.failed'><i class='fa fa-lg fa-thumbs-down'></i> Failed loading hierarchy.</small>
          <generic-entity
            entity='entityData'
            selectable='selectable'
            show-navigation='showNavigation'
            on-detail-action='detailFn(entityData, $activate)'>
          </generic-entity>
        </div
      "
      scope:
        entityData: "=entity"
        selectable: "="
        showNavigation: "="
        detailFn: "&onDetailAction"
      replace: true
      link: (scope, element) ->
        template = "<div style='margin-left: 30px;'>
                      <generic-entity-with-hierarchy
                        ng-repeat='hi in entityData.hierarcy'
                        entity='hi'
                        selectable='selectable'
                        show-navigation='showNavigation'
                        on-detail-action='detailFn(hi, $activate)'>
                      </generic-entity-with-hierarchy>
                    </div>"

        hierarchyEl = null
        scope.$watch "entityData.hierarcy", () ->
          if(hierarchyEl?) then hierarchyEl.remove()
          if(scope.entityData.hierarcy? && scope.entityData.hierarcy.length > 0)
            hierarchyEl = angular.element(template)
            element.append(hierarchyEl)
            $compile(hierarchyEl)(scope)
          return

        return
    }