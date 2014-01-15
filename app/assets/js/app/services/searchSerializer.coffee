"use strict"
ngDefine "services.searchSerializer", (module) ->

  module.factory "searchSerializer", () ->

    api = {
      serialize: (keywords, offline = []) ->

        return "mock"
      deserialize: (string) ->

        return "mock"
    }
    api
  return