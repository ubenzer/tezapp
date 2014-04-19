"use strict"
ngDefine "controllers.results.entityDetail", (module) ->
  module.controller "results.entityDetail", ($scope, SelectedItems, PrettyNaming) ->
    if(!angular.isObject($scope.entityDetail?.entityObj)) then throw new Error("We need entityDetail#entityObj to be defined in scope")
    if(!angular.isString($scope.pageParts?.entityDetailTab)) then throw new Error("We need pageParts#entityDetailTab to be defined in scope")

    $scope.removeItem = (uri) -> SelectedItems.removeItem(uri)
    $scope.addItem = (uri) -> SelectedItems.addItem(uri)
    $scope.isElementSelected = (uri) -> SelectedItems.isItemSelected(uri)

    $scope.entity = $scope.entityDetail.entityObj
    $scope.entity.title = $scope.entity.label || $scope.entity.uri
    $scope.entity.className = PrettyNaming.classNameFor($scope.entity.kind)

    $scope.columnEntityRelationTypes = []

    ### LOOKUP TYPES ###

    ## CLASS ##
    classRelations = [
      {
        name: "Sub classes"
        hierarchyCount: 3
        predicate: "http://www.w3.org/2000/01/rdf-schema#subClassOf"
        searchFor: "subject"
      }
      {
        name: "Super classes"
        hierarchyCount: 10
        predicate: "http://www.w3.org/2000/01/rdf-schema#subClassOf"
        searchFor: "object"
      }
      {
        name: "Instances"
        predicate: "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
        searchFor: "subject"
      }
      {
        name: "Disjoint with"
        predicate: "http://www.w3.org/2002/07/owl#disjointWith"
        searchFor: "object"
      }
      {
        name: "Is domain of"
        predicate: "http://www.w3.org/2000/01/rdf-schema#domain"
        searchFor: "subject"
      }
      {
        name: "Is range of"
        predicate: "http://www.w3.org/2000/01/rdf-schema#range"
        searchFor: "subject"
      }
    ]

    ## OBJECT PROPERTY + PROPERTY ##
    propertyRelations = [
      {
        name: "Sub properties"
        hierarchyCount: 5
        predicate: "http://www.w3.org/2000/01/rdf-schema#subPropertyOf"
        searchFor: "subject"
      }
      {
        name: "Super properties"
        hierarchyCount: 5
        predicate: "http://www.w3.org/2000/01/rdf-schema#subPropertyOf"
        searchFor: "object"
      }
      {
        name: "Domains"
        predicate: "http://www.w3.org/2000/01/rdf-schema#domain"
        searchFor: "object"
      }
      {
        name: "Ranges"
        predicate: "http://www.w3.org/2000/01/rdf-schema#range"
        searchFor: "object"
      }
      {
        name: "Inverse of"
        predicate: "http://www.w3.org/2002/07/owl#inverseOf"
        searchFor: "all"
      }
    ]

    relationSearches = []
    switch $scope.entity.kind
      when "http://www.w3.org/2002/07/owl#Class", "http://www.w3.org/2000/01/rdf-schema#Class"
        relationSearches = classRelations
      when "http://www.w3.org/2002/07/owl#ObjectProperty", "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property"
        relationSearches = propertyRelations
      else relationSearches = []

    $scope.relationSearches = relationSearches
    return
  return