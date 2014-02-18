"use strict"
ngDefine "controllers.results", [
  "module:controllers.results.header"
  "module:controllers.results.export"
], (module) ->

  module.controller "results", ($scope, $state, $stateParams, SearchSerializer, SelectedItems, UrlConfig, $http, exportFormats) ->

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
    }
    # Data for page parts
    $scope.pageControls = {
      searchInProgress: true
    }

    $scope.resultList = {}

    # Do search!
    $http.post("/search", $scope.searchConfig)
      .success (data) ->
        console.debug(data)
        $scope.resultList = data.searchResults;
      .error () ->
        alert("Some error occurred. Please resubmit your search. Sorry. :/")
      .finally () ->
        $scope.pageControls.searchInProgress = false


    $scope.isElementSelected = (uri) -> SelectedItems.isItemSelected(uri)
    $scope.toggleElement = (uri) ->
      if(SelectedItems.isItemSelected(uri))
        SelectedItems.removeItem(uri)
      else
        SelectedItems.addItem(uri)
      return
    $scope.getSelectedElementCount = () -> SelectedItems.getSelectedCount()

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

