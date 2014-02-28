"use strict"
ngDefine "controllers.results", [
  "module:controllers.results.header"
  "module:controllers.results.export"
  "module:controllers.results.entityDetail"
  "module:controllers.results.genericEntity"
], (module) ->

  module.controller "results", ($scope, $state, $stateParams, SearchSerializer, SelectedItems, UrlConfig, $http, exportFormats, PrettyNaming) ->

    $scope.searchConfig = SearchSerializer.deserialize($stateParams.searchParams)
    if(!$scope.searchConfig)
      $state.go("search")
      return

    SelectedItems.clear() # We don't want to keep items from previous one

    $scope.export = {}
    $scope.export.exportFormats = exportFormats

    # Initianize page parts
    resultsPageBaseUrl = UrlConfig.htmlBaseUrl + "/results"
    # Basic configuration for subpages.
    $scope.pageParts = {
      header: resultsPageBaseUrl + "/header.html"
      exportTab: resultsPageBaseUrl + "/export.html"
      entityDetailTab: resultsPageBaseUrl + "/entityDetail.html"
      entityRelationTab: resultsPageBaseUrl + "/entityRelation.html"
      entityRelationDetail: resultsPageBaseUrl + "/entityRelationDetail.html"
      genericEntity: resultsPageBaseUrl + "/genericEntity.html"
    }
    # Data for page parts
    $scope.pageControls = {
      searchInProgress: true
    }

    $scope.resultList = []
    $scope.resultLimit = 20
    $scope.showMoreResults = () -> $scope.resultLimit += 50
    $scope.hasMoreResults = () -> $scope.resultLimit < $scope.resultList.length

    # Do search!
    $http.post("/search", $scope.searchConfig)
      .success (data) ->
        $scope.resultList = processResults(data.searchResults)
      .error () ->
        alert("Some error occurred. Please resubmit your search. Sorry. :/")
      .finally () ->
        $scope.pageControls.searchInProgress = false

    $scope.getSelectedElementCount = () -> SelectedItems.getSelectedCount()

    processResults = (results) ->
      tbReturned = []
      for result in results
        tbReturned.push(result.element)
      tbReturned

    $scope.entityDetails = []
    entityDetailsUriLookup = {}
    $scope.showEntityDetail = (maybeEvent, entityObj) ->
      if(maybeEvent?) then maybeEvent.stopPropagation()

      if(entityDetailsUriLookup[entityObj.uri]?)
        # If already open, switch to that tab.
        entityDetailsUriLookup[entityObj.uri].active = true
        return

      entity = {
        title: entityObj.label || entityObj.uri
        className: PrettyNaming.classNameFor(entityObj.kind)
        active: (!maybeEvent? || maybeEvent.button != 1)
        close: () ->
          $scope.entityDetails.splice($scope.entityDetails.indexOf(entity), 1)
          delete entityDetailsUriLookup[entityObj.uri]
          return
        entityObj: entityObj
      }
      $scope.entityDetails.push(entity)
      entityDetailsUriLookup[entityObj.uri] = entity
      return

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

