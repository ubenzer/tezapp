"use strict"
ngDefine "services.searchSerializer", (module) ->

  module.factory "SearchSerializer", ($log) ->

    api = {
      serialize: (keywords, offline = false) ->
        return JSON.stringify({k: keywords, o: offline})
      deserialize: (string) ->
        try
          obj = JSON.parse(string)
        catch error
          $log.error("Unparsable search value!")
          $log.error(string)
          return undefined

        if(!angular.isArray(obj.k))
          $log.error("Invalid search value!")
          $log.error(string)
          return undefined

        return {
          keywords: obj.k
          offline: obj.o == true
        }
    }
    api
  return