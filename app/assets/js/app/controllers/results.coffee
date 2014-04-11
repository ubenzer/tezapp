"use strict"
ngDefine "controllers.results", [
  "module:controllers.results.header"
  "module:controllers.results.export"
  "module:controllers.results.entityDetail"
], (module) ->

  module.controller "results", ($scope, $state, $stateParams, SearchSerializer, SelectedItems, UrlConfig, $http, exportFormats, PrettyNaming, $filter) ->

    $scope.searchConfig = SearchSerializer.deserialize($stateParams.searchParams)
    if(!$scope.searchConfig)
      $state.go("search")
      return

    SelectedItems.clear() # We don't want to keep items from previous one

    $scope.export = {}
    $scope.export.exportFormats = exportFormats

    # Initialize page parts
    resultsPageBaseUrl = UrlConfig.htmlBaseUrl + "/results"
    # Basic configuration for subpages.
    $scope.pageParts = {
      header: resultsPageBaseUrl + "/header.html"
      exportTab: resultsPageBaseUrl + "/export.html"
      entityDetailTab: resultsPageBaseUrl + "/entityDetail.html"
      entityRelationTab: resultsPageBaseUrl + "/entityRelation.html"
      entityRelationDetail: resultsPageBaseUrl + "/entityRelationDetail.html"
    }
    # Data for page parts
    $scope.pageControls = {
      searchInProgress: true
    }

    $scope.resultList = []
    $scope.resultListVisible = []
    $scope.resultLimit = 30
    $scope.showMoreResults = () -> $scope.resultLimit += 50
    $scope.hasMoreResults = () -> $scope.resultLimit < $scope.resultListVisible.length

    # Do search!
    $http.post("/search", $scope.searchConfig)
      .success (data) ->
        $scope.resultList = processResults(data.searchResults)
        return
      .error () ->
        alert("Some error occurred. Please resubmit your search. Sorry. :/")
        return
      .finally () ->
        $scope.pageControls.searchInProgress = false
        return

    $scope.getSelectedElementCount = () -> SelectedItems.getSelectedCount()

    processResults = (results) ->
      tbReturned = []
      for result in results
        tbReturned.push(result.element)
      tbReturned

    $scope.entityDetails = []
    entityDetailsUriLookup = {}
    $scope.showEntityDetail = (entityObj, activeTab = true) ->

      if(entityDetailsUriLookup[entityObj.uri]?)
        # If already open, switch to that tab.
        entityDetailsUriLookup[entityObj.uri].active = true
        return

      entity = {
        title: entityObj.label || entityObj.uri
        className: PrettyNaming.classNameFor(entityObj.kind)
        active: activeTab
        close: () ->
          $scope.entityDetails.splice($scope.entityDetails.indexOf(entity), 1)
          delete entityDetailsUriLookup[entityObj.uri]
          return
        entityObj: entityObj
      }
      $scope.entityDetails.push(entity)
      entityDetailsUriLookup[entityObj.uri] = entity
      return

    $scope.entityTypes = PrettyNaming.list()
    $scope.filter = {}
    Object.keys($scope.entityTypes).forEach (entity) -> $scope.filter[entity] = true
    typeFilter = (item) -> ($scope.filter[item.kind]? && $scope.filter[item.kind])
    refreshFilter = () ->
      $scope.resultListVisible = $filter('filter')($scope.resultList, typeFilter)
      $scope.resultLimit = 30
    $scope.$watchCollection("resultList", refreshFilter)
    $scope.$watch("filter", refreshFilter, true)

    return

  module.factory "SelectedItems", () ->
    selectedItems = {}

    api = {
      clear: () ->
        selectedItems = {}
        return
      addItem: (uri) ->
        selectedItems[uri] = true
        return
      removeItem: (uri) ->
        delete selectedItems[uri]
        return
      isItemSelected: (uri) ->
        selectedItems[uri]?
      getSelectedCount: () ->
        Object.keys(selectedItems).length
      getAllItems: () ->
        angular.copy(selectedItems)
    }
    api

