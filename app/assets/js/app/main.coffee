"use strict"
ngDefine "main", [
  # 3rd party
  "module:ngSanitize:angular-sanitize"
  "module:ngAnimate:angular-animate"
  "module:ui.bootstrap:ui-bootstrap"
  "module:ui.router:ui-router"
  "module:ngTagsInput:ng-tags-input"

  # My app

  # Controllers
  "module:controllers.search"
  "module:controllers.results"

  # Services
  "module:services.SearchSerializer"
  "module:services.PrettyNaming"
  "module:services.Utils"

  # Directives
  "module:directives.genericEntity"
  "module:directives.genericRelationDetail"

  # Filters
  "module:filters.cut"

], (module) ->

  module.constant("UrlConfig", {
    htmlBaseUrl: "/assets/html"
  })

  module
  .config ($urlRouterProvider, $stateProvider, UrlConfig) ->
    $urlRouterProvider.otherwise('/')
    $stateProvider
    .state 'search',
      url: "/"
      templateUrl: UrlConfig.htmlBaseUrl + "/search.html"
      controller: "search"

    .state 'results',
      url: "/s/*searchParams"
      templateUrl: UrlConfig.htmlBaseUrl + "/results.html"
      controller: "results"
      resolve:
        exportFormats: ($http) ->
          $http.get("/exportFormats")
          .then (response) ->
            response.data

    return
  .controller "app", ($rootScope, $state) ->
      $rootScope.$state = $state
      return
