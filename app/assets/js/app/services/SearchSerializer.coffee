"use strict"
ngDefine "services.SearchSerializer", (module) ->

  module.factory "SearchSerializer", ($log) ->

    api = {
      serialize: (keywords, offline = false) ->
        theArray = []
        theArray.push(v.text) for own k,v of keywords
        return JSON.stringify({k: theArray, o: offline})
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