"use strict"
ngDefine "app", [
  # 3rd party
  "angular-animate"
  "module:ui.bootstrap:ui-bootstrap"
  "module:ui.router:ui-router"
  "module:ngTagsInput:ng-tags-input"

  # My app
  "module:app.search"
  "module:app.results"
], (module) ->

  module.constant("UrlConfig", {
    htmlBaseUrl: "/assets/html"
  })

  module.config ($urlRouterProvider, $stateProvider, UrlConfig) ->

    $urlRouterProvider.otherwise('')
    $stateProvider
    .state 'search',
      url: ""
      templateUrl: UrlConfig.htmlBaseUrl + "/search.html"
      controller: "app.search"

    .state 'results',
      url: "/s/:offline/:keywords"
      templateUrl: UrlConfig.htmlBaseUrl + "/esults.html"
      controller: "app.results"
    return
